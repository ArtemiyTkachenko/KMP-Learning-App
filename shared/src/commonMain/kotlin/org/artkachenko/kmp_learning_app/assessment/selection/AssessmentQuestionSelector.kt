package org.artkachenko.kmp_learning_app.assessment.selection

import kotlin.time.Clock
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.history.CompletedAssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.MistakeScheduleDerivation
import org.artkachenko.kmp_learning_app.assessment.history.QuestionExposure
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningPerformanceDerivation
import org.artkachenko.kmp_learning_app.learning_progress.WeakArea

/**
 * Turns a practice or interview configuration into the Questions an assessment will ask.
 *
 * This is the only place that knows how a configuration becomes content, which is what keeps
 * targeted practice from needing an engine of its own: every source policy ends at the same
 * `Selected` list, and [org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine]
 * cannot tell how the list was produced.
 */
internal class AssessmentQuestionSelector(
    private val curriculumRepository: CurriculumRepository,
    private val completedHistory: CompletedAssessmentHistory,
    private val randomize: (List<Question>) -> List<Question> = { it.shuffled() },
    private val now: () -> Instant = { Clock.System.now() },
) {
    // Built over this selector's own repository rather than injected: the derivation is stateless,
    // and weak-area selection must attribute evidence through the same curriculum it selects from.
    private val performanceDerivation = LearningPerformanceDerivation(curriculumRepository)

    suspend fun select(config: AssessmentConfig): AssessmentSelectionResult =
        when (config) {
            is AssessmentConfig.Focused -> selectPracticeQuestions(config)
            is AssessmentConfig.Mixed -> selectMixedQuestions(config)
        }

    /**
     * Whether [source] has a selection policy, answered without reading any content.
     *
     * The Practice Builder needs this before the learner commits to a choice: an option with no
     * policy has to be visibly unavailable rather than start-then-fail. Asking [select] instead
     * would read content — and, for the history-derived policies, completed history — once per
     * option just to render a screen.
     *
     * This mirrors the source branch in [selectPracticeQuestions] and must move with it;
     * `AssessmentQuestionSelectorTest` fails if the two ever disagree.
     */
    fun isSourceSupported(source: PracticeQuestionSource): Boolean =
        when (source) {
            PracticeQuestionSource.ALL,
            PracticeQuestionSource.UNSEEN,
            PracticeQuestionSource.WEAK_AREAS,
            PracticeQuestionSource.UNRESOLVED_MISTAKES,
            -> true
        }

    private suspend fun selectPracticeQuestions(
        config: AssessmentConfig.Focused,
    ): AssessmentSelectionResult {
        if (config.levels.isEmpty()) return AssessmentSelectionResult.NoContent.NoLevelsSelected

        val eligible = when (config.source) {
            PracticeQuestionSource.ALL -> loadScopedQuestions(config.scope, config.levels)
            PracticeQuestionSource.UNSEEN -> loadUnseenQuestions(config.scope, config.levels)
            PracticeQuestionSource.WEAK_AREAS ->
                loadWeakAreaQuestions(config.scope, config.levels)
            PracticeQuestionSource.UNRESOLVED_MISTAKES ->
                return selectScheduledMistakes(config)
        }

        return toResult(config.scope.narrow(randomizeUnique(eligible), config.questionCount))
    }

    /**
     * Every scheduled mistake is eligible, but the due ones are asked first.
     *
     * Only a due Question's correct answer moves it along the review ladder, so a run shorter than
     * the queue spends its questions where they count. Coming-up Questions still fill the rest:
     * practising one early is allowed and simply does not count. Each tier is randomized and
     * narrowed by the scope exactly as any other source is, so Subtopic coverage applies within the
     * due tier before it applies to the fill.
     */
    private suspend fun selectScheduledMistakes(
        config: AssessmentConfig.Focused,
    ): AssessmentSelectionResult {
        val evaluatedAt = now()
        val dueQuestionIds = mutableSetOf<String>()
        val scheduledQuestionIds = mutableSetOf<String>()
        MistakeScheduleDerivation.derive(completedHistory.completedAttempts()).forEach { mistake ->
            scheduledQuestionIds += mistake.questionId
            if (mistake.isDue(evaluatedAt)) dueQuestionIds += mistake.questionId
        }

        val (due, comingUp) = loadScheduledMistakeQuestions(config.scope, config.levels, scheduledQuestionIds)
            .let(::randomizeUnique)
            .partition { it.id in dueQuestionIds }
        val dueFirst = config.scope.narrow(due, config.questionCount)
        return toResult(dueFirst + config.scope.narrow(comingUp, config.questionCount - dueFirst.size))
    }

    /**
     * How a scope turns its randomized candidate pool into the questions actually asked.
     *
     * Only a multi-Subtopic scope spreads: it was configured as several concepts, so answering
     * about one of them and calling it practice of the unit would be wrong. Topic and single
     * Subtopic scopes keep taking the randomized prefix they always have — one scope has no
     * distinct groups to cover, and re-grouping Topic practice by Subtopic would silently change
     * shipped selection for every existing focused run.
     */
    private fun AssessmentScope.narrow(
        randomized: List<Question>,
        questionCount: Int,
    ): List<Question> =
        when (this) {
            is AssessmentScope.Topic,
            is AssessmentScope.Subtopic,
            -> randomized.take(questionCount)
            is AssessmentScope.Subtopics -> randomized.coveringDistinctSubtopics(questionCount)
        }

    /**
     * Coverage across the scoped Subtopics first, then the remainder, mirroring the round-robin
     * the Mixed interview uses across Topics.
     *
     * Grouping runs over the already-randomized pool, so first-encounter order — which group is
     * covered at all when the requested count is smaller than the number of groups — comes from
     * the injected randomization rather than from ID order. Picking by lexicographic or authored ID
     * would quietly bias every short run towards the same concepts. A scoped Subtopic that
     * contributes nothing after the scope, level, and source filters simply has no group and is
     * skipped; it never lets an unscoped Question in to represent it.
     *
     * The fill pass is drawn from an explicit remainder keyed by stable Question ID, so a Question
     * cannot be both a coverage pick and a fill pick.
     */
    private fun List<Question>.coveringDistinctSubtopics(questionCount: Int): List<Question> {
        val questionsBySubtopic = linkedMapOf<String, MutableList<Question>>()
        forEach { question ->
            questionsBySubtopic
                .getOrPut(question.subtopicId) { mutableListOf() }
                .add(question)
        }

        val covering = questionsBySubtopic.values
            .map { it.first() }
            .take(questionCount)
        val coveringIds = covering.mapTo(mutableSetOf()) { it.id }
        return covering + filterNot { it.id in coveringIds }.take(questionCount - covering.size)
    }

    private suspend fun selectMixedQuestions(
        config: AssessmentConfig.Mixed,
    ): AssessmentSelectionResult {
        val questionsByTopic = linkedMapOf<String, MutableList<Question>>()
        randomizeUnique(curriculumRepository.getActiveQuestions()).forEach { question ->
            questionsByTopic
                .getOrPut(question.topicId) { mutableListOf() }
                .add(question)
        }

        val selected = mutableListOf<Question>()
        var roundIndex = 0
        while (selected.size < config.questionCount) {
            var selectedInRound = false
            for (topicQuestions in questionsByTopic.values) {
                val question = topicQuestions.getOrNull(roundIndex) ?: continue
                selected += question
                selectedInRound = true
                if (selected.size == config.questionCount) break
            }
            if (!selectedInRound) break
            roundIndex++
        }
        return toResult(selected)
    }

    private fun randomizeUnique(questions: List<Question>): List<Question> =
        randomize(questions.distinctBy { it.id })
            .distinctBy { it.id }

    private fun toResult(questions: List<Question>): AssessmentSelectionResult =
        if (questions.isEmpty()) {
            AssessmentSelectionResult.NoContent.NoEligibleQuestions
        } else {
            AssessmentSelectionResult.Selected(questions)
        }

    /**
     * Unseen practice is the complement of curriculum coverage inside the ordinary candidate pool:
     * the same scoped, level-aware ACTIVE read every source starts from, minus the stable Question
     * IDs [QuestionExposure] found in completed history.
     *
     * Subtracting last is what keeps the two dimensions — what the learner has seen, and what the
     * curriculum currently offers — from contaminating each other. A historical ID whose Question
     * was retired cannot remove anything from a pool it is no longer in, and a newly authored
     * Question is unseen the moment it exists, with no exposure record to backfill. That also means
     * the scope and level narrowing is never widened to find unseen content: seen Questions in
     * unselected levels are not in the pool to begin with, and an exhausted level selection
     * correctly ends at no content rather than quietly practising a different level.
     */
    private suspend fun loadUnseenQuestions(
        scope: AssessmentScope,
        levels: Set<QuestionLevel>,
    ): List<Question> {
        val eligible = loadScopedQuestions(scope, levels)
        val observedQuestionIds =
            QuestionExposure.observedQuestionIds(completedHistory.completedAttempts())
        return eligible.filterNot { it.id in observedQuestionIds }
    }

    /**
     * Weak-area practice intersects current eligibility with the exact performance derivation used
     * by Learning Progress. Historical occurrences establish weak identities; the repository's
     * scoped, level-aware ACTIVE read establishes what can be asked now.
     */
    private suspend fun loadWeakAreaQuestions(
        scope: AssessmentScope,
        levels: Set<QuestionLevel>,
    ): List<Question> {
        val weakAreas = performanceDerivation
            .derive(completedHistory.completedAttempts())
            .weakAreas
        val weakTopicIds = weakAreas
            .filterIsInstance<WeakArea.Topic>()
            .mapTo(mutableSetOf()) { it.performance.topicId }
        val weakSubtopicIds = weakAreas
            .filterIsInstance<WeakArea.Subtopic>()
            .mapTo(mutableSetOf()) { it.performance.topicId to it.performance.subtopicId }

        return loadScopedQuestions(scope, levels).filter { question ->
            question.topicId in weakTopicIds ||
                question.topicId to question.subtopicId in weakSubtopicIds
        }
    }

    /**
     * Historical state decides which stable IDs are scheduled; current curriculum eligibility
     * decides which of those IDs can be asked now. This keeps missing and deprecated Questions in
     * Mistake Review history without resurrecting them into a new assessment.
     */
    private suspend fun loadScheduledMistakeQuestions(
        scope: AssessmentScope,
        levels: Set<QuestionLevel>,
        scheduledQuestionIds: Set<String>,
    ): List<Question> =
        loadScopedQuestions(scope, levels).filter { it.id in scheduledQuestionIds }

    /**
     * Level filtering belongs to the repository, not to this class or to presentation: the scoped
     * level-aware reads keep ACTIVE eligibility and level matching in one place instead of loading
     * a scope and re-filtering it here.
     */
    private suspend fun loadScopedQuestions(
        scope: AssessmentScope,
        levels: Set<QuestionLevel>,
    ): List<Question> =
        when (scope) {
            is AssessmentScope.Topic ->
                curriculumRepository.getActiveQuestionsByTopicAndLevels(scope.topicId, levels)
            is AssessmentScope.Subtopic ->
                curriculumRepository.getActiveQuestionsBySubtopicAndLevels(scope.subtopicId, levels)
            // The union is read one scoped Subtopic at a time through the same level-aware call a
            // single-Subtopic run uses, rather than through a multi-ID query added for it: these
            // scopes hold a handful of concepts, and a new DAO surface would buy nothing while
            // giving the selector a second definition of eligibility to keep in step. Sorting the
            // IDs only makes the read order deterministic; it carries no selection meaning, since
            // randomization decides encounter order immediately afterwards.
            is AssessmentScope.Subtopics ->
                scope.subtopicIds.sorted().flatMap { subtopicId ->
                    curriculumRepository.getActiveQuestionsBySubtopicAndLevels(subtopicId, levels)
                }
        }
}

package org.artkachenko.kmp_learning_app.assessment.selection

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.history.CompletedAssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.QuestionExposure
import org.artkachenko.kmp_learning_app.assessment.history.UnresolvedMistakeDerivation
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
    private val performanceDerivation: LearningPerformanceDerivation =
        LearningPerformanceDerivation(curriculumRepository),
    private val randomize: (List<Question>) -> List<Question> = { it.shuffled() },
) {
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
                loadUnresolvedMistakeQuestions(config.scope, config.levels)
        }

        return toResult(randomizeUnique(eligible).take(config.questionCount))
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
     * Historical state decides which stable IDs are unresolved; current curriculum eligibility
     * decides which of those IDs can be asked now. This keeps missing and deprecated Questions in
     * Mistake Review history without resurrecting them into a new assessment.
     */
    private suspend fun loadUnresolvedMistakeQuestions(
        scope: AssessmentScope,
        levels: Set<QuestionLevel>,
    ): List<Question> {
        val unresolvedQuestionIds = UnresolvedMistakeDerivation
            .derive(completedHistory.completedAttempts())
            .mapTo(mutableSetOf()) { it.questionId }

        return loadScopedQuestions(scope, levels).filter { it.id in unresolvedQuestionIds }
    }

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
        }
}

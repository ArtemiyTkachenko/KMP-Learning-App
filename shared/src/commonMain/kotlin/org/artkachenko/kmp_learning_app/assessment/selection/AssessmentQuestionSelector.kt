package org.artkachenko.kmp_learning_app.assessment.selection

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

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
    private val randomize: (List<Question>) -> List<Question> = { it.shuffled() },
) {
    suspend fun select(config: AssessmentConfig): AssessmentSelectionResult =
        when (config) {
            is AssessmentConfig.Focused -> selectPracticeQuestions(config)
            is AssessmentConfig.Mixed -> selectMixedQuestions(config)
        }

    private suspend fun selectPracticeQuestions(
        config: AssessmentConfig.Focused,
    ): AssessmentSelectionResult {
        if (config.levels.isEmpty()) return AssessmentSelectionResult.NoContent.NoLevelsSelected

        val eligible = when (config.source) {
            PracticeQuestionSource.ALL -> loadScopedQuestions(config.scope, config.levels)
            // The history-derived policies land here, one branch each: E16-03 for UNSEEN, E16-04
            // for WEAK_AREAS, E16-05 for UNRESOLVED_MISTAKES. Until a policy exists the request is
            // refused explicitly, because falling through to the ALL pool would answer a different
            // question than the learner asked.
            PracticeQuestionSource.UNSEEN,
            PracticeQuestionSource.WEAK_AREAS,
            PracticeQuestionSource.UNRESOLVED_MISTAKES,
            -> return AssessmentSelectionResult.NoContent.SourceNotSupported
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

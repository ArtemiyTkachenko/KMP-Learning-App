package org.artkachenko.kmp_learning_app.assessment.selection

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentQuestionSelector(
    private val curriculumRepository: CurriculumRepository,
    private val randomize: (List<Question>) -> List<Question> = { it.shuffled() },
) {
    suspend fun select(config: AssessmentConfig): List<Question> =
        when (config) {
            is AssessmentConfig.Focused ->
                randomizeUnique(loadEligibleQuestions(config))
                    .take(config.questionCount)
            is AssessmentConfig.Mixed ->
                selectMixedQuestions(
                    questions = loadEligibleQuestions(config),
                    questionCount = config.questionCount,
                )
        }

    private fun selectMixedQuestions(
        questions: List<Question>,
        questionCount: Int,
    ): List<Question> {
        val questionsByTopic = linkedMapOf<String, MutableList<Question>>()
        randomizeUnique(questions).forEach { question ->
            questionsByTopic
                .getOrPut(question.topicId) { mutableListOf() }
                .add(question)
        }

        val selected = mutableListOf<Question>()
        var roundIndex = 0
        while (selected.size < questionCount) {
            var selectedInRound = false
            for (topicQuestions in questionsByTopic.values) {
                val question = topicQuestions.getOrNull(roundIndex) ?: continue
                selected += question
                selectedInRound = true
                if (selected.size == questionCount) break
            }
            if (!selectedInRound) break
            roundIndex++
        }
        return selected
    }

    private fun randomizeUnique(questions: List<Question>): List<Question> =
        randomize(questions.distinctBy { it.id })
            .distinctBy { it.id }

    private suspend fun loadEligibleQuestions(
        config: AssessmentConfig,
    ): List<Question> =
        when (config) {
            is AssessmentConfig.Focused ->
                when (val scope = config.scope) {
                    is AssessmentScope.Topic ->
                        curriculumRepository.getActiveQuestionsByTopic(scope.topicId)
                    is AssessmentScope.Subtopic ->
                        curriculumRepository.getActiveQuestionsBySubtopic(scope.subtopicId)
                }
            is AssessmentConfig.Mixed ->
                curriculumRepository.getActiveQuestions()
        }
}

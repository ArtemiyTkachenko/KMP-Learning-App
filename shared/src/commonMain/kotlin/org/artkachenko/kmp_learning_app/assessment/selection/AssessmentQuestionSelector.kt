package org.artkachenko.kmp_learning_app.assessment.selection

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class AssessmentQuestionSelector(
    private val curriculumRepository: CurriculumRepository,
    private val randomize: (List<Question>) -> List<Question> = { it.shuffled() },
) {
    suspend fun select(config: AssessmentConfig): List<Question> {
        val eligibleQuestions = loadEligibleQuestions(config)
            .distinctBy { it.id }

        return randomize(eligibleQuestions)
            .distinctBy { it.id }
            .take(config.questionCount)
    }

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

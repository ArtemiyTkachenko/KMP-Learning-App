package org.artkachenko.kmp_learning_app.assessment.retake

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.start.StartAssessment
import org.artkachenko.kmp_learning_app.assessment.start.StartAssessmentResult

internal class AssessmentRetakeService(
    private val assessmentRepository: AssessmentRepository,
    private val startAssessment: StartAssessment,
) {
    suspend fun createRetake(sourceAttemptId: String): AssessmentRetakeResult {
        require(sourceAttemptId.isNotBlank()) {
            "sourceAttemptId must not be blank."
        }

        val sourceAttempt =
            assessmentRepository.getById(sourceAttemptId)
                ?: return AssessmentRetakeResult.SourceAttemptNotFound

        check(sourceAttempt.status == AssessmentStatus.COMPLETED) {
            "Only completed attempts can be used as retake sources."
        }

        return when (val startResult = startAssessment(sourceAttempt.config)) {
            StartAssessmentResult.NoEligibleQuestions ->
                AssessmentRetakeResult.NoEligibleQuestions
            is StartAssessmentResult.Created -> {
                check(startResult.attemptId != sourceAttempt.id) {
                    "Retake attempt ID must differ from the source attempt ID."
                }
                AssessmentRetakeResult.Created(startResult.attemptId)
            }
        }
    }
}

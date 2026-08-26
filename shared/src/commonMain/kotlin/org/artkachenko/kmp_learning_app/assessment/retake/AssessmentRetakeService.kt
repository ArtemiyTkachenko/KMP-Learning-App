package org.artkachenko.kmp_learning_app.assessment.retake

import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentStartResult

internal class AssessmentRetakeService(
    private val assessmentRepository: AssessmentRepository,
    private val assessmentEngine: AssessmentEngine,
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

        return when (val startResult = assessmentEngine.start(sourceAttempt.config)) {
            AssessmentStartResult.NoEligibleQuestions ->
                AssessmentRetakeResult.NoEligibleQuestions
            is AssessmentStartResult.Started -> {
                val session = startResult.session
                check(session.attempt.id != sourceAttempt.id) {
                    "Retake attempt ID must differ from the source attempt ID."
                }
                assessmentRepository.save(session.attempt)
                AssessmentRetakeResult.Created(session)
            }
        }
    }
}

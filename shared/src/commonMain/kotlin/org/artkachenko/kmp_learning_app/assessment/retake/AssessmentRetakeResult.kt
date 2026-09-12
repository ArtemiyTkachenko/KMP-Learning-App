package org.artkachenko.kmp_learning_app.assessment.retake

internal sealed interface AssessmentRetakeResult {
    data class Created(
        val attemptId: String,
    ) : AssessmentRetakeResult

    data object SourceAttemptNotFound : AssessmentRetakeResult

    data object NoEligibleQuestions : AssessmentRetakeResult
}

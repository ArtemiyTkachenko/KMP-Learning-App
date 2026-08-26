package org.artkachenko.kmp_learning_app.assessment.session

internal sealed interface AssessmentStartResult {
    data class Started(
        val session: AssessmentSession,
    ) : AssessmentStartResult

    data object NoEligibleQuestions : AssessmentStartResult
}

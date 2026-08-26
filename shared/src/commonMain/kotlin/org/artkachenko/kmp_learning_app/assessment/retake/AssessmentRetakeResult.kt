package org.artkachenko.kmp_learning_app.assessment.retake

import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSession

internal sealed interface AssessmentRetakeResult {
    data class Created(
        val session: AssessmentSession,
    ) : AssessmentRetakeResult

    data object SourceAttemptNotFound : AssessmentRetakeResult

    data object NoEligibleQuestions : AssessmentRetakeResult
}

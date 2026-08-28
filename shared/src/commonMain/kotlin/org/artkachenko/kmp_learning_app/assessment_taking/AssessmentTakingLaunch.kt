package org.artkachenko.kmp_learning_app.assessment_taking

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig

internal sealed interface AssessmentTakingLaunch {
    data class New(val config: AssessmentConfig) : AssessmentTakingLaunch
    data class ExistingAttempt(val attemptId: String) : AssessmentTakingLaunch
}

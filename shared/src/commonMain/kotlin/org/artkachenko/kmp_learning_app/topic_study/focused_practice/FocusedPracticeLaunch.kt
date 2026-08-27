package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig

internal sealed interface FocusedPracticeLaunch {
    data class New(val config: AssessmentConfig.Focused) : FocusedPracticeLaunch
    data class ExistingAttempt(val attemptId: String) : FocusedPracticeLaunch
}

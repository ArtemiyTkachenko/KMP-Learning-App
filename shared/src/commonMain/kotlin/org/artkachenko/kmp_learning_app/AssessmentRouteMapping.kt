package org.artkachenko.kmp_learning_app

internal fun AppNavigator.pushFocusedAttempt(attemptId: String) {
    push(AppRoute.FocusedPracticeAttempt(attemptId))
}

internal fun AppNavigator.pushMixedAttempt(attemptId: String) {
    push(AppRoute.MixedInterviewAttempt(attemptId))
}

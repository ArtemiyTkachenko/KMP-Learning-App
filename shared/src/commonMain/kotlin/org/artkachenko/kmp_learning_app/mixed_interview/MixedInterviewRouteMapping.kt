package org.artkachenko.kmp_learning_app.mixed_interview

import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment_taking.AssessmentTakingLaunch

internal fun mixedInterviewStartRoute(): AppRoute.MixedInterview =
    AppRoute.MixedInterview(
        questionCount = MixedInterviewDefaults.QuestionCount,
    )

internal fun AppRoute.MixedInterview.toAssessmentConfig(): AssessmentConfig.Mixed =
    AssessmentConfig.Mixed(
        questionCount = questionCount,
    )

internal fun AppRoute.MixedInterviewAttempt.toAssessmentTakingLaunch(): AssessmentTakingLaunch =
    AssessmentTakingLaunch.ExistingAttempt(attemptId)

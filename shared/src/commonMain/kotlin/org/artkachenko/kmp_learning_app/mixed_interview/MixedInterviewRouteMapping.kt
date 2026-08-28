package org.artkachenko.kmp_learning_app.mixed_interview

import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig

internal fun mixedInterviewStartRoute(): AppRoute.MixedInterview =
    AppRoute.MixedInterview(
        questionCount = MixedInterviewDefaults.QuestionCount,
    )

internal fun AppRoute.MixedInterview.toAssessmentConfig(): AssessmentConfig.Mixed =
    AssessmentConfig.Mixed(
        questionCount = questionCount,
    )

package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope

internal fun AppRoute.FocusedTopicPractice.toAssessmentConfig(): AssessmentConfig.Focused =
    AssessmentConfig.Focused(
        scope = AssessmentScope.Topic(topicId),
        questionCount = questionCount,
    )

internal fun AppRoute.FocusedSubtopicPractice.toAssessmentConfig(): AssessmentConfig.Focused =
    AssessmentConfig.Focused(
        scope = AssessmentScope.Subtopic(subtopicId),
        questionCount = questionCount,
    )

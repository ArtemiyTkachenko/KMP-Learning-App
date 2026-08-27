package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope

internal fun AssessmentConfig.Focused.toAppRoute(): AppRoute =
    when (val scope = scope) {
        is AssessmentScope.Topic -> AppRoute.FocusedTopicPractice(
            topicId = scope.topicId,
            questionCount = questionCount,
        )

        is AssessmentScope.Subtopic -> AppRoute.FocusedSubtopicPractice(
            subtopicId = scope.subtopicId,
            questionCount = questionCount,
        )
    }

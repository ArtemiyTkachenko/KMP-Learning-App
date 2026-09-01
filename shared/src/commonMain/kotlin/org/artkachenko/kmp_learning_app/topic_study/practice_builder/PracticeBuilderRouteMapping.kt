package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.inAuthoredOrder

/** Opening the builder carries the stable scope ID and nothing else. */
internal fun AssessmentScope.toPracticeBuilderRoute(): AppRoute =
    when (this) {
        is AssessmentScope.Topic -> AppRoute.PracticeBuilderTopic(topicId = topicId)
        is AssessmentScope.Subtopic -> AppRoute.PracticeBuilderSubtopic(subtopicId = subtopicId)
    }

internal fun AppRoute.PracticeBuilderTopic.toAssessmentScope(): AssessmentScope =
    AssessmentScope.Topic(topicId)

internal fun AppRoute.PracticeBuilderSubtopic.toAssessmentScope(): AssessmentScope =
    AssessmentScope.Subtopic(subtopicId)

/**
 * Flattens the configured run into route fields.
 *
 * Every dimension the builder can change travels, so the practice destination rebuilds the same
 * request the learner configured rather than an all-levels default that merely resembles it. The
 * level set is normalised to authored order so an identical configuration is an identical route.
 */
internal fun AssessmentConfig.Focused.toPracticeRoute(): AppRoute =
    when (val scope = scope) {
        is AssessmentScope.Topic -> AppRoute.FocusedTopicPractice(
            topicId = scope.topicId,
            questionCount = questionCount,
            levels = levels.inAuthoredOrder(),
            source = source,
        )

        is AssessmentScope.Subtopic -> AppRoute.FocusedSubtopicPractice(
            subtopicId = scope.subtopicId,
            questionCount = questionCount,
            levels = levels.inAuthoredOrder(),
            source = source,
        )
    }

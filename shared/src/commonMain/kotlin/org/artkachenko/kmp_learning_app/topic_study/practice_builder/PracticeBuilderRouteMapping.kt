package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import org.artkachenko.kmp_learning_app.AppRoute
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.inAuthoredOrder
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset

/**
 * Opening the builder from content carries the stable scope ID and nothing else, so the source
 * stays the builder's `ALL` default.
 */
internal fun AssessmentScope.toPracticeBuilderRoute(): AppRoute =
    when (this) {
        is AssessmentScope.Topic -> AppRoute.PracticeBuilderTopic(topicId = topicId)
        is AssessmentScope.Subtopic -> AppRoute.PracticeBuilderSubtopic(subtopicId = subtopicId)
    }

/**
 * Opening the builder on a semantic practice intent.
 *
 * One mapping for every source rather than a route per practice kind: `PracticePreset` already
 * models the intent as scope plus source, and the builder is the screen that turns an intent into a
 * runnable configuration. Question count and level selection are deliberately absent from the
 * preset and stay the builder's defaults — a preset is a starting point the learner can edit, not a
 * reconstruction of a previous run.
 */
internal fun PracticePreset.toPracticeBuilderRoute(): AppRoute =
    when (val scope = scope) {
        is AssessmentScope.Topic -> AppRoute.PracticeBuilderTopic(
            topicId = scope.topicId,
            source = source,
        )

        is AssessmentScope.Subtopic -> AppRoute.PracticeBuilderSubtopic(
            subtopicId = scope.subtopicId,
            source = source,
        )
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

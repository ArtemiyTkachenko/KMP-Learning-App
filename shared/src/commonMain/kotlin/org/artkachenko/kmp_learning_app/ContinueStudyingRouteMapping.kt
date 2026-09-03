package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.guided_learning.ContinueStudyingTarget
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeBuilderRoute

/**
 * Continue Studying reaches existing destinations only.
 *
 * Navigation 3 stops at this file: the resolver that produced the target knows nothing about
 * `AppRoute`, which is what lets it be tested without a back stack. A content target reuses the
 * Topic detail route the browser and search results already push, including its optional Subtopic
 * argument, and a practice target reuses the one `PracticePreset` mapping the Practice Builder
 * exposes — so neither this feature nor a later one adds a parallel destination.
 *
 * No mapping exists to `FocusedPracticeAttempt` or `MixedInterviewAttempt`, and none may be added:
 * [ContinueStudyingTarget] cannot carry an attempt ID, so returning to a learning context can never
 * become resuming an assessment.
 */
internal fun ContinueStudyingTarget.toAppRoute(): AppRoute =
    when (this) {
        is ContinueStudyingTarget.Topic -> AppRoute.Topic(
            topicId = topicId,
            subtopicId = subtopicId,
        )

        is ContinueStudyingTarget.Practice -> preset.toPracticeBuilderRoute()
    }

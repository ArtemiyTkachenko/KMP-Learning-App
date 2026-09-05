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
        is AssessmentScope.Subtopics -> noPracticeBuilderRoute()
    }

/**
 * Multi-Subtopic practice is an assessment-domain capability with no navigation entry point yet.
 *
 * Every route in this file is a *UI* entry: content the learner tapped, addressed by the stable ID
 * that screen knows. A multi-Subtopic scope is produced by resolving a teaching unit into concepts,
 * and the route that carries such a request has to identify what the learner chose — the unit —
 * rather than the derived set, so it is defined together with the screen that offers it.
 *
 * Failing loudly is deliberate. The two coercions available here — practising only the first
 * Subtopic, or widening to a parent Topic — both silently run a different assessment than the one
 * configured, and a wrong quiz is worse than an obviously missing route. Nothing reaches this
 * today: every scope in this file comes from a Topic or Subtopic route, a weak area, or completed
 * history, and none of those can produce a multi-Subtopic scope.
 */
internal fun AssessmentScope.Subtopics.noPracticeBuilderRoute(): Nothing =
    error("Multi-Subtopic practice has no Practice Builder entry point: $subtopicIds.")

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

        is AssessmentScope.Subtopics -> scope.noPracticeBuilderRoute()
    }

/**
 * The route says what the learner opened the builder on; what that means for assessment is the
 * builder's own answer. For a Topic or Subtopic the two are one lookup apart, and for a Learning
 * Unit they are a document read apart — which is exactly why the mapping stops at the target.
 */
internal fun AppRoute.PracticeBuilderTopic.toPracticeBuilderTarget(): PracticeBuilderTarget =
    PracticeBuilderTarget.Topic(topicId)

internal fun AppRoute.PracticeBuilderSubtopic.toPracticeBuilderTarget(): PracticeBuilderTarget =
    PracticeBuilderTarget.Subtopic(subtopicId)

internal fun AppRoute.PracticeBuilderLearningUnit.toPracticeBuilderTarget(): PracticeBuilderTarget =
    PracticeBuilderTarget.LearningUnit(unitId)

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

        // The derived concepts travel, not the Learning Unit they came from: this is the run, and
        // re-deriving it at the assessment would let mid-run re-authoring change what is asked.
        // Sorted for the same reason the levels are normalised — an identical configuration has to
        // be an identical back-stack entry, and a Set carries no order to preserve.
        is AssessmentScope.Subtopics -> AppRoute.FocusedSubtopicsPractice(
            subtopicIds = scope.subtopicIds.sorted(),
            questionCount = questionCount,
            levels = levels.inAuthoredOrder(),
            source = source,
        )
    }

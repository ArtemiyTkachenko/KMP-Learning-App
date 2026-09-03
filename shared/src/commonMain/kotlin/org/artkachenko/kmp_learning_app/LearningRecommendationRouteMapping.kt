package org.artkachenko.kmp_learning_app

import org.artkachenko.kmp_learning_app.guided_learning.LearningRecommendationTarget
import org.artkachenko.kmp_learning_app.topic_study.practice_builder.toPracticeBuilderRoute

/**
 * Recommended Next reaches existing capabilities only.
 *
 * Navigation 3 stops at this file, exactly as it does for Continue Studying: the policy that chose
 * the target knows nothing about `AppRoute`, which is what lets it be a testable decision tree
 * rather than a navigation concern. Every case here reuses a destination the app already has.
 *
 * A practice recommendation deliberately lands in the Practice Builder rather than starting a run:
 * the preset is an *intent* — a scope and a source — and the builder stays authoritative for count,
 * levels, availability, and its own preflight. Content may have moved on between the recommendation
 * being shown and the card being tapped, and the builder is the screen that reports that honestly.
 *
 * There is no mapping to any attempt, result, or configured-practice route, and none may be added:
 * a recommendation selects a product capability, never a running assessment.
 */
internal fun LearningRecommendationTarget.toAppRoute(): AppRoute =
    when (this) {
        LearningRecommendationTarget.Topics -> AppRoute.Topics

        // The existing Mistake Review capability, as E17-01 chose. Deliberately not
        // UNRESOLVED_MISTAKES practice: that would be a different product decision made here.
        LearningRecommendationTarget.MistakeReview -> AppRoute.MistakeReview

        is LearningRecommendationTarget.Practice -> preset.toPracticeBuilderRoute()
    }

/**
 * Opens a recommendation the way the shell opens that kind of destination.
 *
 * Two of the three targets are top-level areas rather than details, so they are *selected* instead
 * of pushed: pushing `Topics` or `MistakeReview` onto the current area's stack would render an area
 * root inside another area, with the navigation bar still highlighting where the learner came from.
 * `AppTopLevelDestination.forRoute` already draws that line, so the rule stays stated once.
 *
 * For a new learner this means selecting Topics, which is the area the card is already shown in and
 * therefore returns it to its root — deterministic starting guidance pointing at the Topic list
 * immediately below, with no Topic chosen on the learner's behalf.
 */
internal fun AppNavigator.openRecommendation(target: LearningRecommendationTarget) {
    val route = target.toAppRoute()
    when (val area = AppTopLevelDestination.forRoute(route)) {
        null -> push(route)
        else -> select(area)
    }
}

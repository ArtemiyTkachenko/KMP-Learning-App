package org.artkachenko.kmp_learning_app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigationevent.NavigationEvent
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion

/**
 * Navigation motion, declared once for every host.
 *
 * Navigation 3's defaults are platform-specific: Android fades between destinations while desktop,
 * iOS, and web get `EnterTransition.None`. Since all four are real hosts, the motion is defined
 * here instead so the app moves the same way everywhere.
 *
 * Switching areas from the navigation bar fades through, because those destinations are siblings
 * rather than one being "deeper" than the other. Pushing to and popping from a detail screen slides
 * horizontally, which carries the sense of depth.
 */
private const val SlideFraction = 6

/**
 * Movement and fade are specified separately on purpose.
 *
 * Everything previously used one `tween` on the default easing, so a screen slid in at the same
 * rate it faded — which is what made the motion read as mechanical. Material pairs an emphasised
 * curve for the thing that moves with phased fades: Material's shared-axis pattern, in which the
 * slide runs the full duration while the outgoing screen fades out first and the incoming screen
 * only starts once it has gone. See [AppMotion.NavigationReplacement] for why the entrance waits.
 */
private fun slideSpec() =
    tween<IntOffset>(
        durationMillis = AppMotion.NavigationDurationMillis,
        easing = AppMotion.EmphasizedEasing,
    )

internal fun appTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (isTopLevelSwitch()) {
        fadeThrough()
    } else {
        slideInHorizontally(slideSpec()) { it / SlideFraction } +
            fadeIn(AppMotion.NavigationReplacement.enterSpec()) togetherWith
            slideOutHorizontally(slideSpec()) { -it / SlideFraction } +
            fadeOut(AppMotion.NavigationReplacement.exitSpec())
    }
}

internal fun appPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (isTopLevelSwitch()) {
        fadeThrough()
    } else {
        slideInHorizontally(slideSpec()) { -it / SlideFraction } +
            fadeIn(AppMotion.NavigationReplacement.enterSpec()) togetherWith
            slideOutHorizontally(slideSpec()) { it / SlideFraction } +
            fadeOut(AppMotion.NavigationReplacement.exitSpec())
    }
}

/**
 * Predictive back follows the edge the gesture started from, so the outgoing screen moves the way
 * the user's finger does. A swipe from the right edge is the mirror of one from the left.
 *
 * Its fades are deliberately *not* phased like [appPopTransitionSpec]'s. Here the gesture drives
 * progress, and the point of predictive back is to show where Back leads while the finger is still
 * down. Phased, the screen underneath would stay invisible for the first third of the swipe and the
 * screen being dragged would have gone by then, so a learner holding the gesture partway would see
 * only the background. Overlap is the preview here, not a glitch.
 *
 * The top-level branch keeps the shared [fadeThrough] so the route decision stays one rule; a gesture
 * does not reach it in practice, because `NavDisplay` handles back only within one area's stack.
 */
internal fun appPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { swipeEdge ->
    if (isTopLevelSwitch()) {
        fadeThrough()
    } else {
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        slideInHorizontally(slideSpec()) { -direction * it / SlideFraction } +
            fadeIn(gestureEnterFadeSpec()) togetherWith
            slideOutHorizontally(slideSpec()) { direction * it / SlideFraction } +
            fadeOut(gestureExitFadeSpec())
    }
}

/** Predictive back's incoming fade: starts with the gesture, so the destination is previewed. */
private fun gestureEnterFadeSpec() =
    tween<Float>(
        durationMillis = AppMotion.NavigationDurationMillis,
        easing = AppMotion.EmphasizedDecelerateEasing,
    )

/** Predictive back's outgoing fade: accelerates away and finishes halfway through the gesture. */
private fun gestureExitFadeSpec() =
    tween<Float>(
        durationMillis = AppMotion.NavigationDurationMillis / 2,
        easing = AppMotion.EmphasizedAccelerateEasing,
    )

/**
 * Siblings fade through rather than slide, so neither reads as deeper than the other.
 *
 * Material's fade-through: the outgoing area fades out first and the incoming one starts only once
 * it has gone, with a brief moment of background between them. Two areas' worth of text fading
 * across each other at once read as a glitch, and the background moment does not — see
 * [AppMotion.NavigationReplacement].
 */
private fun fadeThrough(): ContentTransform =
    fadeIn(AppMotion.NavigationReplacement.enterSpec()) togetherWith
        fadeOut(AppMotion.NavigationReplacement.exitSpec())

/** True when both sides of the transition are navigation-bar areas. */
internal fun isTopLevelSwitch(from: AppRoute?, to: AppRoute?): Boolean =
    from?.let(AppTopLevelDestination::forRoute) != null &&
        to?.let(AppTopLevelDestination::forRoute) != null

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isTopLevelSwitch(): Boolean =
    isTopLevelSwitch(initialState.route(), targetState.route())

private fun Scene<NavKey>.route(): AppRoute? = entries.lastOrNull()?.contentKey as? AppRoute

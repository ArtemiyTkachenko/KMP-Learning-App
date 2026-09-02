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
 * Switching areas from the navigation bar cross-fades, because those destinations are siblings
 * rather than one being "deeper" than the other. Pushing to and popping from a detail screen slides
 * horizontally, which carries the sense of depth.
 */
private const val SlideFraction = 6

/**
 * Movement and fade are specified separately on purpose.
 *
 * Everything previously used one `tween` on the default easing, so a screen slid in at the same
 * rate it faded — which is what made the motion read as mechanical. Material pairs an emphasised
 * curve for the thing that moves with a shorter, flatter fade, so the incoming screen is legible
 * before it has finished arriving.
 */
private fun slideSpec() =
    tween<IntOffset>(
        durationMillis = AppMotion.NavigationDurationMillis,
        easing = AppMotion.EmphasizedEasing,
    )

private fun enterFadeSpec() =
    tween<Float>(
        durationMillis = AppMotion.NavigationDurationMillis,
        easing = AppMotion.EmphasizedDecelerateEasing,
    )

/** Exits accelerate away and finish early, so the incoming screen is never read through the old one. */
private fun exitFadeSpec() =
    tween<Float>(
        durationMillis = AppMotion.NavigationDurationMillis / 2,
        easing = AppMotion.EmphasizedAccelerateEasing,
    )

internal fun appTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (isTopLevelSwitch()) {
        crossFade()
    } else {
        slideInHorizontally(slideSpec()) { it / SlideFraction } +
            fadeIn(enterFadeSpec()) togetherWith
            slideOutHorizontally(slideSpec()) { -it / SlideFraction } +
            fadeOut(exitFadeSpec())
    }
}

internal fun appPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (isTopLevelSwitch()) {
        crossFade()
    } else {
        slideInHorizontally(slideSpec()) { -it / SlideFraction } +
            fadeIn(enterFadeSpec()) togetherWith
            slideOutHorizontally(slideSpec()) { it / SlideFraction } +
            fadeOut(exitFadeSpec())
    }
}

/**
 * Predictive back follows the edge the gesture started from, so the outgoing screen moves the way
 * the user's finger does. A swipe from the right edge is the mirror of one from the left.
 */
internal fun appPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { swipeEdge ->
    if (isTopLevelSwitch()) {
        crossFade()
    } else {
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        slideInHorizontally(slideSpec()) { -direction * it / SlideFraction } +
            fadeIn(enterFadeSpec()) togetherWith
            slideOutHorizontally(slideSpec()) { direction * it / SlideFraction } +
            fadeOut(exitFadeSpec())
    }
}

/**
 * Siblings cross-fade rather than slide, so neither reads as deeper than the other.
 *
 * The outgoing half is not shortened here as it is for a push: with nothing moving, an early exit
 * leaves a visible gap where neither screen is drawn.
 */
private fun crossFade(): ContentTransform =
    fadeIn(enterFadeSpec()) togetherWith
        fadeOut(
            tween(
                durationMillis = AppMotion.NavigationDurationMillis,
                easing = AppMotion.EmphasizedEasing,
            ),
        )

/** True when both sides of the transition are navigation-bar areas. */
internal fun isTopLevelSwitch(from: AppRoute?, to: AppRoute?): Boolean =
    from?.let(AppTopLevelDestination::forRoute) != null &&
        to?.let(AppTopLevelDestination::forRoute) != null

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isTopLevelSwitch(): Boolean =
    isTopLevelSwitch(initialState.route(), targetState.route())

private fun Scene<NavKey>.route(): AppRoute? = entries.lastOrNull()?.contentKey as? AppRoute

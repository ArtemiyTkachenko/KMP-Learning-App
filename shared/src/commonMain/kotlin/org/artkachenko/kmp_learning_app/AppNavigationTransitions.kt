package org.artkachenko.kmp_learning_app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigationevent.NavigationEvent
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

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
private const val TransitionDurationMillis = 260
private const val SlideFraction = 6

internal fun appTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (isTopLevelSwitch()) {
        crossFade()
    } else {
        slideInHorizontally(tween(TransitionDurationMillis)) { it / SlideFraction } +
            fadeIn(tween(TransitionDurationMillis)) togetherWith
            slideOutHorizontally(tween(TransitionDurationMillis)) { -it / SlideFraction } +
            fadeOut(tween(TransitionDurationMillis))
    }
}

internal fun appPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    if (isTopLevelSwitch()) {
        crossFade()
    } else {
        slideInHorizontally(tween(TransitionDurationMillis)) { -it / SlideFraction } +
            fadeIn(tween(TransitionDurationMillis)) togetherWith
            slideOutHorizontally(tween(TransitionDurationMillis)) { it / SlideFraction } +
            fadeOut(tween(TransitionDurationMillis))
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
        slideInHorizontally(tween(TransitionDurationMillis)) { -direction * it / SlideFraction } +
            fadeIn(tween(TransitionDurationMillis)) togetherWith
            slideOutHorizontally(tween(TransitionDurationMillis)) { direction * it / SlideFraction } +
            fadeOut(tween(TransitionDurationMillis))
    }
}

private fun crossFade(): ContentTransform =
    fadeIn(tween(TransitionDurationMillis)) togetherWith fadeOut(tween(TransitionDurationMillis))

/** True when both sides of the transition are navigation-bar areas. */
internal fun isTopLevelSwitch(from: AppRoute?, to: AppRoute?): Boolean =
    from?.let(AppTopLevelDestination::forRoute) != null &&
        to?.let(AppTopLevelDestination::forRoute) != null

private fun AnimatedContentTransitionScope<Scene<NavKey>>.isTopLevelSwitch(): Boolean =
    isTopLevelSwitch(initialState.route(), targetState.route())

private fun Scene<NavKey>.route(): AppRoute? = entries.lastOrNull()?.contentKey as? AppRoute

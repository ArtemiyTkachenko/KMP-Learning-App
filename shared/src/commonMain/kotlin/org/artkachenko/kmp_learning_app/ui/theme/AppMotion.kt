package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens for the animations the app writes itself.
 *
 * `MaterialTheme.motionScheme` covers Material's own components, but nothing else: Navigation 3
 * takes `AnimatedContent` specs for its scene transitions, and the app's `animate*AsState` calls
 * choose their own. Those previously used bare `tween(260)` and `tween(300)` — two private
 * constants in unrelated files, both on the default easing, so every motion in the product moved
 * with the same generic curve regardless of what it was expressing.
 *
 * The easing curves and durations below are the Material 3 values, not invented ones, so the app's
 * own motion and the motion inside Material components agree.
 */
internal object AppMotion {

    /** Material 3 emphasized easing. The default for movement that both starts and ends on screen. */
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** For content entering the screen: decelerates into place rather than arriving at speed. */
    val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** For content leaving the screen: accelerates away, so exits read as faster than entrances. */
    val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /**
     * Navigation between destinations. Material's `DurationMedium1`.
     *
     * This replaces a local 260ms constant. The value barely moves; the easing is the actual change.
     */
    const val NavigationDurationMillis: Int = 250

    /** State-layer scale changes: selection, emphasis, small colour shifts. `DurationShort4`. */
    const val StateChangeDurationMillis: Int = 200

    /** Determinate progress. Long enough that a jump from 40% to 60% reads as travel. */
    const val ProgressDurationMillis: Int = 300

    /**
     * Colour, border, and other non-spatial properties.
     *
     * Effects are tweened rather than sprung on purpose: overshoot is meaningless for a colour —
     * there is no "past the target" for a hue — and Material's own effect tokens are critically
     * damped for the same reason.
     */
    fun <T> effectSpec(durationMillis: Int = StateChangeDurationMillis): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EmphasizedEasing)

    /**
     * Anything that moves or resizes.
     *
     * These are the Material 3 expressive spatial-default spring values (damping 0.8, stiffness
     * 380), stated explicitly rather than read from `MotionScheme` because `MotionScheme` is
     * `@Composable`-scoped and several call sites here are not. Keeping the numbers identical is
     * what makes the app's own movement indistinguishable from a Material component's.
     */
    fun <T> spatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = SpatialDamping, stiffness = SpatialStiffness)

    /**
     * The spatial spring for offsets.
     *
     * `IntOffset` needs a visibility threshold of one whole pixel; without it a spring settles on
     * sub-pixel values and keeps animating past the point anything visibly changes.
     */
    fun offsetSpec(): FiniteAnimationSpec<IntOffset> =
        spring(
            dampingRatio = SpatialDamping,
            stiffness = SpatialStiffness,
            visibilityThreshold = IntOffset(1, 1),
        )

    private const val SpatialDamping = 0.8f
    private const val SpatialStiffness = 380.0f
}

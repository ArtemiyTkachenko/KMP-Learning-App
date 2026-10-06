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
     * A completed assessment's score arriving on the result screen.
     *
     * The one duration in the app deliberately past the ~400ms ceiling the rest of the motion keeps
     * to. Everything else here is a *state change* — something that was already on screen becoming
     * something else — where length reads as sluggishness. This is the opposite case: a figure
     * being counted out and a meter being filled, which is the app's single celebratory beat and
     * the only place where the travel itself is the content. Below about half a second a count-up
     * reads as a flicker rather than as counting.
     */
    const val ScoreRevealDurationMillis: Int = 620

    /**
     * New content arriving underneath something the learner is already reading.
     *
     * Longer than a state change because it is an arrival rather than an adjustment, and short
     * enough that nothing has to be waited for: a reveal that runs past roughly a third of a second
     * stops reading as the answer appearing and starts reading as the app thinking about it.
     */
    const val ContentRevealDurationMillis: Int = 280

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
     * One thing on screen becoming another: the arriving half.
     *
     * The app pairs a decelerating arrival with a faster accelerating departure, so what is coming
     * settles into place while what is going gets out of the way. That pairing was written out
     * longhand wherever it was needed — the screen-state fade and the compact navigation
     * bar's slide each spelled out the same two `tween`s — which agreed only because nobody had
     * retuned one of them yet.
     *
     * Decelerating rather than emphasized because the content is arriving rather than travelling
     * through, and the full [StateChangeDurationMillis] because arriving is the half the learner
     * actually watches. Distinct from [revealSpec], which is longer: that is content appearing
     * *underneath* something already being read, and this is one element showing or hiding — the
     * compact navigation bar — over content that stays put.
     *
     * Not for one whole piece of content replacing another. The two halves here start together, so
     * they overlap; a replacement holds its entrance back until the exit has finished, through
     * [PhasedReplacement].
     */
    fun <T> arriveSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = StateChangeDurationMillis, easing = EmphasizedDecelerateEasing)

    /**
     * The departing half of the same pairing.
     *
     * Half the duration by default, and accelerating, so what is going gets out of the way faster
     * than what is coming settles. [durationMillis] is open because a departure that *moves*
     * rather than fades needs the full duration to clear its own travel — the navigation bar
     * sliding off the bottom is the one case.
     */
    fun <T> departSpec(durationMillis: Int = StateChangeDurationMillis / 2): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EmphasizedAccelerateEasing)

    /** A navigation destination replacing another: area switches, pushes, and pops. */
    val NavigationReplacement: PhasedReplacement = PhasedReplacement(NavigationDurationMillis)

    /** A screen's loading, empty, error, or content state replacing another. */
    val StateReplacement: PhasedReplacement = PhasedReplacement(StateChangeDurationMillis)

    /**
     * Content entering the screen, optionally after [delayMillis].
     *
     * Decelerating rather than emphasized: this is content that was not there a moment ago, so it
     * settles into place instead of travelling through. [delayMillis] is what lets several pieces
     * of one reveal arrive in order without a coroutine orchestrating them — the stagger is part of
     * the animation spec, so it cannot leave the interaction waiting on it.
     */
    fun <T> revealSpec(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(
            durationMillis = ContentRevealDurationMillis,
            delayMillis = delayMillis,
            easing = EmphasizedDecelerateEasing,
        )

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

/**
 * One whole piece of content replacing another, in two phases: Material's fade-through.
 *
 * The outgoing content fades out over the first [exitMillis], and the incoming content does not
 * start until that has finished — [enterDelayMillis] equals the exit window — then settles over the
 * rest of [totalMillis]. A replacement whose two halves both start at zero is legible twice over:
 * a decelerating fade-in is mostly opaque within its first few frames, while the departure is still
 * on screen, and two full screens of text read through each other as a glitch rather than as one
 * thing becoming another. Shortening the exit alone does not fix that; only holding the entrance
 * back does.
 *
 * The cost is a brief moment, at the hand-over, where neither side is drawn and only the background
 * shows. That moment is intended. It is what makes the change read as a clean replacement, and at
 * these durations it is too short to read as a gap.
 *
 * The exit takes [ExitPercent] of the total, which is Material's own fade-through split.
 */
internal class PhasedReplacement(val totalMillis: Int) {

    /** How long the outgoing content takes to fade out. */
    val exitMillis: Int = totalMillis * ExitPercent / 100

    /** When the incoming content starts: exactly when the outgoing content has gone. */
    val enterDelayMillis: Int = exitMillis

    /** How long the incoming content takes to fade in, filling the rest of [totalMillis]. */
    val enterMillis: Int = totalMillis - enterDelayMillis

    /** The outgoing half: accelerates away within the exit window. */
    fun <T> exitSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = exitMillis, easing = AppMotion.EmphasizedAccelerateEasing)

    /** The incoming half: waits out the exit, then decelerates into place. */
    fun <T> enterSpec(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = enterMillis,
            delayMillis = enterDelayMillis,
            easing = AppMotion.EmphasizedDecelerateEasing,
        )

    private companion object {
        const val ExitPercent = 35
    }
}

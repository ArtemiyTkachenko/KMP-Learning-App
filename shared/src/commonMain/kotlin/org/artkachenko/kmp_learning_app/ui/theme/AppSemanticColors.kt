package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The product's colour roles that Material 3 does not model.
 *
 * Material 3 has no "success" or "warning" role, so review screens previously borrowed
 * `colorScheme.primary` for a correct answer — which rendered as the baseline purple rather than
 * anything a learner would read as correct. These are explicit product tokens with light and dark
 * values, kept beside the scheme so both stay in step.
 *
 * [partiallyCorrect] covers a multiple-answer question where the learner picked only correct
 * options but missed at least one. That distinction is derived in presentation from data the
 * review models already carry; no scoring behaviour changes. It is also the app's general *warning*
 * tone: a weak Topic wears the same amber, because "you are not strong here yet" and "you got part
 * of this right" are the same message at two scales.
 *
 * ## Semantic colours mark, they do not shout
 *
 * These are semantics, not a second brand. Each triple is a **container** a whole block can be
 * filled with, an **on-container** for text inside it, and a bare **accent** for a border, an icon,
 * or a tag beside neutral text. The containers are deliberately the quietest member: in light they
 * are pale tints a paragraph can sit on, and in dark they are deep and desaturated rather than the
 * lit blocks they used to be — `incorrectContainer` was `0xFF93000A`, a near-pure red that filled a
 * whole answer row and made a review screen look like a fault report. Saturation now lives in the
 * accent, which is applied to a 1dp border or a line of tag text, so a wall of six review cards
 * reads as six results rather than six alarms.
 *
 * Both reds are the same red as `colorScheme.error` in the matching scheme, on purpose: see
 * [AppLightColorScheme].
 *
 * ## The hero gradient
 *
 * [heroGradientStart] and [heroGradientEnd] are the one shared brand flourish the product allows: a
 * short indigo-to-violet sweep across the two hues the accent range already spans, for the rare
 * surface that is the single most important thing on its screen.
 *
 * It has three call sites, each of which had to make the argument rather than inherit it, and all
 * three stay distinguishable by **motion** rather than by a palette of their own.
 *
 * `AssessmentCompletionHero` is an *arrival*: a run has just finished and the score is counted out
 * over the app's one celebratory duration. `ProgressHero` is a *standing* answer, and it is here
 * because the Progress dashboard runs six or more containers down the page — against that many
 * neighbours, one surface-ramp step reads as "the first card" rather than as the screen's headline.
 * It settles a measured figure into place over the ordinary content-reveal duration instead.
 * `InterviewStartScreen`'s hero is an *invitation*, on a destination whose only other object is a
 * bounded record in one `ContentGroup`; it animates no figure at all, because twenty questions is
 * configuration rather than a measurement, and it is the only one of the three that holds its
 * screen's primary action.
 *
 * A fourth screen adopting this still has to make the same argument: that nothing else on it
 * competes, and that the surface is genuinely the answer the learner came for — not that a card
 * should look important.
 *
 * It carries no on-colour of its own. Both endpoints are chosen to sit within the `primaryContainer`
 * tone of their scheme, so `colorScheme.onPrimaryContainer` is legible across the whole sweep;
 * `AppColorSchemeTest` asserts that against both endpoints rather than leaving it to the eye. That
 * is also why the sweep is short — a gradient wide enough to need two different text colours is a
 * gradient text cannot safely cross.
 */
@Immutable
internal data class AppSemanticColors(
    val correct: Color,
    val onCorrectContainer: Color,
    val correctContainer: Color,
    val partiallyCorrect: Color,
    val onPartiallyCorrectContainer: Color,
    val partiallyCorrectContainer: Color,
    val incorrect: Color,
    val onIncorrectContainer: Color,
    val incorrectContainer: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
)

internal val AppLightSemanticColors = AppSemanticColors(
    correct = Color(0xFF1A6547),
    onCorrectContainer = Color(0xFF06331F),
    correctContainer = Color(0xFFD3EEDE),
    partiallyCorrect = Color(0xFF7A5416),
    onPartiallyCorrectContainer = Color(0xFF2A1B00),
    partiallyCorrectContainer = Color(0xFFF6E3C4),
    incorrect = Color(0xFFB3261E),
    onIncorrectContainer = Color(0xFF3F0906),
    incorrectContainer = Color(0xFFF7DCD9),
    // Pale periwinkle into pale violet: a tint over an off-white page, not a band of colour on it.
    heroGradientStart = Color(0xFFDFE1FF),
    heroGradientEnd = Color(0xFFEDDCFF),
)

internal val AppDarkSemanticColors = AppSemanticColors(
    correct = Color(0xFF8FD3AE),
    onCorrectContainer = Color(0xFFCDEEDC),
    correctContainer = Color(0xFF10452E),
    partiallyCorrect = Color(0xFFE7C07A),
    onPartiallyCorrectContainer = Color(0xFFF6E3C4),
    partiallyCorrectContainer = Color(0xFF4A3618),
    incorrect = Color(0xFFF2B8B2),
    onIncorrectContainer = Color(0xFFF7DCD9),
    incorrectContainer = Color(0xFF631513),
    // Rich indigo into violet. Dark is where the sweep is actually visible, so it is the one that
    // sets the direction; the light pair is the same two hues at container strength.
    heroGradientStart = Color(0xFF232C6E),
    heroGradientEnd = Color(0xFF412C77),
)

/**
 * Static because the value only changes when the whole theme changes, so reads should not
 * introduce recomposition scopes of their own.
 */
internal val LocalAppSemanticColors = staticCompositionLocalOf { AppLightSemanticColors }

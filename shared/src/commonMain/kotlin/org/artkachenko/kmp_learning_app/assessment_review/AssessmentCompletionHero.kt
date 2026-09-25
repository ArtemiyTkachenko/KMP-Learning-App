package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_accuracy_caption
import kmp_learning_app.shared.generated.resources.assessment_review_accuracy_correct
import kmp_learning_app.shared.generated.resources.assessment_review_score_figure
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
import org.artkachenko.kmp_learning_app.ui.AccuracyRing
import org.artkachenko.kmp_learning_app.ui.AccuracyRingTrackAlpha
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.CountedFigure
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

/**
 * The first thing a learner sees when an assessment ends: what they just scored.
 *
 * This used to be an ordinary `PrimarySummaryCard` — the same `surfaceContainer` fill, the same
 * corner, the same weight as the Progress dashboard's summary and as each of the twenty review
 * cards underneath it. That is a fair description of the *content*, and a poor one of the
 * **moment**: finishing a run is the one arrival in the product, and it looked like one more row in
 * a list of surfaces. Nothing on the screen said "this just happened".
 *
 * Four things make this a hero rather than a card, and nothing else does. It takes the app's hero
 * gradient, which is documented as being for "the rare surface that is the single most important
 * thing on its screen" and has this as its only call site. It carries a hairline edge and a shadow,
 * so the gradient is an object lifted off the page rather than a patch of colour on it — which
 * matters most in light, where the sweep is a pale tint over an off-white background. Its figure is
 * the score itself at display scale. And that figure is *counted out*, with the ring beside it
 * sweeping to the same value over the same movement.
 *
 * The celebration stops there. There is no confetti, no streak, no points, and no praise: the app
 * has none of those anywhere else, and a learner who scored 30% is not owed a party. What the
 * motion is actually for is that a score arriving over half a second is a score the learner
 * *watches land*, and a score they watch land is one they have read.
 *
 * ## What the figure is
 *
 * `8 / 10` is the figure and `80% correct` is the line under it. It was the other way round, with
 * the percentage at display scale and the score printed below as `Score: 8 / 10`. Two things were
 * wrong with that. The fraction is what the learner actually did — ten questions happened, eight
 * went well — and the percentage is a derivation of it, so the derivation was shouting over the
 * fact. And a display-scale figure that needs the word `Score:` in front of it is a figure whose
 * own placement is not doing its job; the label is gone and the hierarchy says it instead.
 *
 * ## Colour, and why this card does not use `accuracyColor`
 *
 * The gradient carries no on-colour of its own; `onPrimaryContainer` is legible across the whole
 * sweep in both schemes by construction, and `AppColorSchemeTest` asserts that. The figure always
 * takes it, so the largest thing on the screen is always the brand.
 *
 * The performance signal is [ResultEmphasis], applied only to the ring and the accuracy line. It is
 * deliberately *not* [org.artkachenko.kmp_learning_app.ui.accuracyColor], which every other metric
 * in the app uses: that scale bottoms out at `semanticColors.incorrect`, the same red an answer
 * gets for being wrong, and this card previously gave that red to its largest element for anything
 * under 70%. A weak Topic on the dashboard is a fact about a Topic and can be marked as a fault; a
 * finished run is a fact about a person, and the error role is for errors, not for verdicts on the
 * learner. So the low band wears the app's warning amber — the same "you are not strong here yet"
 * tone a weak Topic card takes — and red never appears on this surface at all.
 *
 * Both colours the ring can take clear WCAG body-text contrast over both ends of the sweep in both
 * themes, verified in `AssessmentCompletionHeroTest` rather than left to the eye, because a later
 * change to either palette could quietly break it.
 */
@Composable
internal fun AssessmentCompletionHero(
    correctAnswers: Int,
    totalQuestions: Int,
    percentage: Double,
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    val semantic = AppThemeExtras.semanticColors
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    val scoreFigure =
        stringResource(Res.string.assessment_review_score_figure, correctAnswers, totalQuestions)

    // Below five questions a percentage reports a precision the run does not have: four questions
    // can only ever produce 0, 25, 50, 75, or 100, and "25%" from one mistake claims an accuracy
    // the run never measured. The fraction is the figure either way, so the moment is the same
    // shape; what a short run loses is the percentage line and the ring, both of which would be
    // restating that same absent precision.
    val statesAccuracy = totalQuestions >= MeaningfulPercentageQuestionCount
    val emphasis = if (statesAccuracy) resultEmphasisColor(percentage) else onHero

    // One value drives the whole reveal — the count and the ring's sweep are the same movement
    // rather than two animations matched on duration — and it is held across the pane's scrolling
    // so the score is counted out once per visit. A result is a settled fact; counting it again
    // would suggest something had changed. The flag is claimed *before* the count rather than after
    // it, so scrolling the hero out of view mid-count and back does not start it over either.
    var alreadyRevealed by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (alreadyRevealed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadyRevealed) {
            alreadyRevealed = true
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = AppMotion.ScoreRevealDurationMillis,
                    easing = AppMotion.EmphasizedDecelerateEasing,
                ),
            )
        }
    }

    // `MutableTransitionState` seeded false and flipped to true is how an `AnimatedVisibility`
    // animates its *first* composition; visible = true would simply draw the card already there.
    val entrance = remember { MutableTransitionState(alreadyRevealed) }
    entrance.targetState = true

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        // Transparent, because the fill is a brush rather than a colour and `Surface` takes only
        // the latter. The shape still clips the gradient and still owns the elevation semantics.
        color = Color.Transparent,
        // The edge and the shadow are what separate the hero from the page. A border in the hero's
        // own on-colour works in both schemes without a token of its own — a faint light hairline
        // over the dark sweep, a faint dark one over the pale sweep — where a fixed grey would be
        // invisible in one of them. The shadow does the same job for light, where the gradient is
        // closest in value to the background it sits on.
        border = BorderStroke(HeroBorderWidth, onHero.copy(alpha = HeroBorderAlpha)),
        shadowElevation = HeroElevation,
    ) {
        AnimatedVisibility(
            visibleState = entrance,
            enter = fadeIn(AppMotion.revealSpec()) +
                slideInVertically(AppMotion.spatialSpec()) { height -> height / EntranceRiseFraction },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(semantic.heroGradientStart, semantic.heroGradientEnd),
                        ),
                    )
                    .padding(AppSpacing.Generous),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
            ) {
                if (title != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            // The word beside it already says the run is complete, so the glyph is
                            // decoration for assistive technology and is not announced twice.
                            contentDescription = null,
                            tint = onHero,
                            modifier = Modifier
                                .size(CompletionIconSize)
                                .animateEnterExit(
                                    enter = scaleIn(
                                        animationSpec = AppMotion.revealSpec(IconRevealDelayMillis),
                                        initialScale = IconInitialScale,
                                    ) + fadeIn(AppMotion.revealSpec(IconRevealDelayMillis)),
                                ),
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = onHero,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (statesAccuracy) {
                        AccuracyRing(
                            fraction = (percentage / 100.0).toFloat() * reveal.value,
                            color = emphasis,
                            // The hero's own on-colour, because a neutral track would be the only
                            // part of this card still referring to the page's surface ramp.
                            trackColor = onHero.copy(alpha = AccuracyRingTrackAlpha),
                        )
                    }
                    Column(
                        // Weighted so the figure and its line wrap inside the space left beside the
                        // ring rather than pushing the row past the window, which is what a large
                        // type scale on a narrow phone does to a fixed row.
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
                    ) {
                        CountedFigure(
                            // Only the numerator counts. The denominator is how many questions
                            // there were, which was settled before the learner answered any of
                            // them, and a count that moved it would be animating the wrong fact.
                            shownText = if (reveal.value >= 1f) {
                                scoreFigure
                            } else {
                                stringResource(
                                    Res.string.assessment_review_score_figure,
                                    (correctAnswers * reveal.value).toInt(),
                                    totalQuestions,
                                )
                            },
                            settledText = scoreFigure,
                            color = onHero,
                        )
                        Text(
                            text = if (statesAccuracy) {
                                stringResource(
                                    Res.string.assessment_review_accuracy_correct,
                                    formatAccuracy(percentage),
                                )
                            } else {
                                stringResource(Res.string.assessment_review_accuracy_caption)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = emphasis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * How strongly a finished run went, as the three bands the hero is allowed to distinguish.
 *
 * Separate from the colour so the *policy* is testable without asserting a colour value: what has
 * to hold is that a weak run is [LOW] rather than anything the app renders as a fault. The upper
 * boundary is the same 85% the dashboard calls comfortably-strong, and the lower one is the domain's
 * own [LearningProgressPolicy.WeakAccuracyThresholdPercentage], so "the hero is being encouraging"
 * and "the domain thinks this area is weak" cannot drift apart.
 */
internal enum class ResultEmphasis { STRONG, MIXED, LOW }

internal fun resultEmphasisFor(percentage: Double): ResultEmphasis = when {
    percentage >= StrongResultThresholdPercentage -> ResultEmphasis.STRONG
    percentage >= LearningProgressPolicy.WeakAccuracyThresholdPercentage -> ResultEmphasis.MIXED
    else -> ResultEmphasis.LOW
}

/**
 * The accent for the ring and the accuracy line.
 *
 * [ResultEmphasis.MIXED] resolves to the hero's own on-colour rather than to a third hue: most runs
 * land there, and a middling result is not a state the card should be tinting at all. The brand
 * stays dominant by being what "nothing in particular to say" looks like.
 */
@Composable
@ReadOnlyComposable
private fun resultEmphasisColor(percentage: Double): Color {
    val semantic = AppThemeExtras.semanticColors
    return when (resultEmphasisFor(percentage)) {
        ResultEmphasis.STRONG -> semantic.correct
        ResultEmphasis.MIXED -> MaterialTheme.colorScheme.onPrimaryContainer
        ResultEmphasis.LOW -> semantic.partiallyCorrect
    }
}

/** Below this, a percentage reports a precision the run does not have. */
private const val MeaningfulPercentageQuestionCount = 5

/** Comfortably above the domain's weakness threshold, so "good" and "only just passing" differ. */
private const val StrongResultThresholdPercentage = 85.0

/** The card rises by a fraction of its own height, so the distance suits the card rather than
 *  being a fixed offset that reads as a long slide on a short card and a twitch on a tall one. */
private const val EntranceRiseFraction = 6

private const val IconRevealDelayMillis = 90
private const val IconInitialScale = 0.6f

/** An edge, not an outline: visible where the gradient meets the page and nowhere else. */
private const val HeroBorderAlpha = 0.14f
private val HeroBorderWidth = 1.dp
private val HeroElevation = 2.dp

private val CompletionIconSize = 20.dp

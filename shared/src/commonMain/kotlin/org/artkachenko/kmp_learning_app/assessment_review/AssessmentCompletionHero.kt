package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_accuracy_caption
import kmp_learning_app.shared.generated.resources.assessment_review_score
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.accuracyColor
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
 * Two things are different here and nothing else is. The card takes the app's hero gradient, which
 * has existed as a token since the colour system landed and has had no call site until now — it is
 * documented as being for "the rare surface that is the single most important thing on its screen",
 * and this is that surface. And the figure is *counted out* rather than printed, with the meter
 * filling underneath it over the same duration.
 *
 * The celebration stops there. There is no confetti, no streak, no points, and no praise: the app
 * has none of those anywhere else, and a learner who scored 30% is not owed a party. What the
 * motion is actually for is that a score arriving over half a second is a score the learner
 * *watches land*, and a score they watch land is one they have read.
 *
 * ## Colour
 *
 * The gradient carries no on-colour of its own; `onPrimaryContainer` is legible across the whole
 * sweep in both schemes by construction, and `AppColorSchemeTest` asserts that. The percentage
 * keeps its own [accuracyColor], which is the one piece of information on this card that is not
 * decoration, and all three of those colours clear WCAG body-text contrast over the sweep in both
 * themes — verified in `AssessmentCompletionHeroTest` rather than left to the eye, because a later
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
    val scoreText = stringResource(Res.string.assessment_review_score, correctAnswers, totalQuestions)

    // One value drives the whole reveal, held across the pane's scrolling so the score is counted
    // out once per visit rather than replayed every time the card comes back into view. A result is
    // a settled fact; counting it again would suggest something had changed.
    var alreadyRevealed by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (alreadyRevealed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadyRevealed) {
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = AppMotion.ScoreRevealDurationMillis,
                    easing = AppMotion.EmphasizedDecelerateEasing,
                ),
            )
            alreadyRevealed = true
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
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
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
                if (totalQuestions < MeaningfulPercentageQuestionCount) {
                    // Too few questions for a percentage to mean anything: four questions can only
                    // ever produce 0, 25, 50, 75, or 100, and reporting "25%" from one mistake
                    // claims a precision the run does not have. The raw score is counted out
                    // instead, so the moment is the same even where the figure is not a percentage.
                    CountedFigure(
                        shownText = if (reveal.value >= 1f) {
                            scoreText
                        } else {
                            stringResource(
                                Res.string.assessment_review_score,
                                (correctAnswers * reveal.value).toInt(),
                                totalQuestions,
                            )
                        },
                        settledText = scoreText,
                        color = onHero,
                    )
                } else {
                    val accuracy = accuracyColor(percentage)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                    ) {
                        CountedFigure(
                            // Whole percent while counting, and the authored value — which may
                            // carry a decimal — only once it has landed. Truncating keeps every
                            // intermediate no wider than the figure it is heading for, which is
                            // what the width reserve below depends on.
                            shownText = if (reveal.value >= 1f) {
                                formatAccuracy(percentage)
                            } else {
                                formatAccuracy((percentage * reveal.value).toInt().toDouble())
                            },
                            settledText = formatAccuracy(percentage),
                            color = accuracy,
                        )
                        Text(
                            text = stringResource(Res.string.assessment_review_accuracy_caption),
                            style = MaterialTheme.typography.titleMedium,
                            color = onHero,
                            modifier = Modifier.padding(bottom = CaptionBaselineNudge),
                        )
                    }
                    ProgressMeter(
                        fraction = (percentage / 100.0).toFloat(),
                        color = accuracy,
                        // A neutral track would be the only part of this card still referring to
                        // the page's surface ramp. The hero's own on-colour at low opacity reads as
                        // an unfilled part of the same object in both schemes.
                        trackColor = onHero.copy(alpha = MeterTrackAlpha),
                        growFromEmptyMillis = AppMotion.ScoreRevealDurationMillis,
                    )
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHero,
                    )
                }
            }
        }
    }
}

/**
 * The score itself, mid-count and settled at once.
 *
 * [shownText] is what is drawn and changes every frame; [settledText] is what the node *says* it
 * is, from the first frame onward. Without that split a screen reader would be handed a number
 * that is merely passing through — announcing "seven percent" because that is where the tween
 * happened to be — and every test that reads this figure would depend on animation timing. The
 * count is presentation; the value is the fact.
 *
 * The figure also reserves the width of [settledText] for the whole count. A number that grows
 * from one digit to three grows *sideways* as well, and everything beside it — the caption, and on
 * a narrow window the line below — is pushed along with it for half a second. Laying the settled
 * text out invisibly underneath costs one extra measure and makes the count a change of digits
 * rather than a change of layout.
 */
@Composable
private fun CountedFigure(
    shownText: String,
    settledText: String,
    color: Color,
) {
    val style = MaterialTheme.typography.displaySmall
    Box(contentAlignment = Alignment.CenterStart) {
        Text(
            text = settledText,
            style = style,
            fontWeight = FontWeight.Bold,
            // Present for measurement only: drawn at zero alpha and carrying no semantics, so the
            // figure is announced once rather than twice.
            modifier = Modifier.alpha(0f).clearAndSetSemantics {},
        )
        Text(
            text = shownText,
            style = style,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.semantics { text = AnnotatedString(settledText) },
        )
    }
}

/** Below this, a percentage reports a precision the run does not have. */
private const val MeaningfulPercentageQuestionCount = 5

/** The card rises by a fraction of its own height, so the distance suits the card rather than
 *  being a fixed offset that reads as a long slide on a short card and a twitch on a tall one. */
private const val EntranceRiseFraction = 6

private const val IconRevealDelayMillis = 90
private const val IconInitialScale = 0.6f

/** Enough to read as the unfilled half of the same bar, not as a second colour on the card. */
private const val MeterTrackAlpha = 0.22f

private val CompletionIconSize = 20.dp

/** Sits the caption on the figure's baseline rather than on the bottom of its line box. */
private val CaptionBaselineNudge = 6.dp

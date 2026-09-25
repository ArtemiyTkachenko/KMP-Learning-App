package org.artkachenko.kmp_learning_app.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.progress_accuracy_caption
import kmp_learning_app.shared.generated.resources.progress_completed_attempts_label
import kmp_learning_app.shared.generated.resources.progress_correct_answers_label
import kmp_learning_app.shared.generated.resources.progress_coverage_count
import kmp_learning_app.shared.generated.resources.progress_coverage_title
import kmp_learning_app.shared.generated.resources.progress_coverage_unavailable
import kmp_learning_app.shared.generated.resources.progress_questions_answered_label
import org.artkachenko.kmp_learning_app.ui.AccuracyRing
import org.artkachenko.kmp_learning_app.ui.AccuracyRingTrackAlpha
import org.artkachenko.kmp_learning_app.ui.CountedFigure
import org.artkachenko.kmp_learning_app.ui.MetricRow
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

/** The plot itself, so a test can reach the hero without matching on one of its many labels. */
internal const val ProgressHeroTag = "progress_hero"

/**
 * Where the learner stands, as the one surface the dashboard leads with.
 *
 * ## Why this is not `AccuracyHeroCard`
 *
 * The shared hero card states its rank with a container step, an edge, and a shadow, and that was
 * enough when it led a screen of two quiet summaries. It is not enough here. This dashboard runs
 * six or more containers down the page — coverage, recent performance, every weak area, every
 * Topic, every completed attempt — and against that many neighbours one ramp step reads as "the
 * first card" rather than as the screen's answer. So the Progress hero takes the brand gradient,
 * and the surfaces under it stay exactly where they were: the hierarchy is made by moving the top
 * of it up, not by pushing everything else down.
 *
 * That gradient previously had one call site and documented itself as reserved for an *arrival*.
 * Adopting it here is the deliberate second decision that comment asked for, and the two uses stay
 * distinguishable by their motion rather than by their palette — the completion hero counts a score
 * out over the app's one celebratory duration, while this settles into place over the ordinary
 * content-reveal one. A learner opening Progress to check on themselves is not being congratulated.
 *
 * ## What it holds
 *
 * All-time accuracy leads, beside its ring, at the largest type on the screen. Underneath it are
 * the three lifetime counts that are the evidence for that figure, and then curriculum coverage —
 * which used to be a card of its own competing with the very number it qualifies. Coverage belongs
 * here precisely because it is *not* another accuracy: it answers how much of the bank has been
 * seen at all, so it is drawn in the primary brand family rather than in [accuracyColor], and it
 * keeps its raw counts because a percentage with an invisible denominator is not interpretable.
 *
 * Recent performance is deliberately *not* folded in. It is a different window over different
 * evidence and it routinely reads a different number, so it keeps its own labelled surface.
 */
@Composable
internal fun ProgressHero(
    percentage: Double,
    completedAttemptCount: Int,
    answeredQuestionCount: Int,
    correctAnswerCount: Int,
    coverage: ProgressCoverageUiModel,
    modifier: Modifier = Modifier,
) {
    val semantic = AppThemeExtras.semanticColors
    // The gradient carries no on-colour of its own; `onPrimaryContainer` is its documented
    // contract, asserted against both endpoints in `AppColorSchemeTest`.
    val onHero = MaterialTheme.colorScheme.onPrimaryContainer
    val onHeroMuted = onHero.copy(alpha = SupportingTextAlpha)
    val accuracy = accuracyColor(percentage)
    val settledFigure = formatAccuracy(percentage)

    // One value drives the whole entry: the ring's sweep and the figure's count are the same
    // movement rather than two animations matched by hand on duration.
    //
    // The flag is `rememberSaveable` and is claimed *before* the animation starts, which is what
    // makes this once per visit rather than once per composition. The dashboard's state is a
    // `StateFlow` that re-emits on every lifecycle resume and on every settled history refresh,
    // and the hero sits in a scrolling list; without the flag a learner would watch their lifetime
    // accuracy count up again every time either of those happened, which would be the screen
    // claiming something had changed when nothing had. The flag lives here, in presentation —
    // nothing about it belongs in the ViewModel.
    var alreadyRevealed by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (alreadyRevealed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadyRevealed) {
            alreadyRevealed = true
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = AppMotion.ContentRevealDurationMillis,
                    easing = AppMotion.EmphasizedDecelerateEasing,
                ),
            )
        }
    }

    // Seeded false and flipped to true is how an `AnimatedVisibility` animates its *first*
    // composition; `visible = true` would simply draw the card already there. Seeded from the same
    // flag, so a hero returning into view is already present rather than sliding in again.
    val entrance = remember { MutableTransitionState(alreadyRevealed) }
    entrance.targetState = true

    Surface(
        modifier = modifier.fillMaxWidth().testTag(ProgressHeroTag),
        shape = MaterialTheme.shapes.large,
        // Transparent, because the fill is a brush rather than a colour and `Surface` takes only
        // the latter. The shape still clips the gradient and still owns the elevation semantics.
        color = Color.Transparent,
        // The edge carries the separation in dark, where one tonal step is not enough and a shadow
        // is almost invisible; the shadow carries it in light, where the pale sweep sits close in
        // value to the page. Neither alone works in both schemes.
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccuracyRing(
                        fraction = (percentage / 100.0).toFloat() * reveal.value,
                        color = accuracy,
                        // The hero's own on-colour: a neutral track would be the only part of this
                        // surface still referring to the page's surface ramp.
                        trackColor = onHero.copy(alpha = AccuracyRingTrackAlpha),
                    )
                    Column(
                        // Weighted so a display-scale numeral and its caption wrap inside the room
                        // left beside the ring rather than pushing the row past a narrow window at
                        // a large type scale.
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
                    ) {
                        CountedFigure(
                            // Whole percent while counting. The settled figure may carry a decimal
                            // place, and a tenth digit flickering through forty frames is noise;
                            // the width is reserved from the settled text either way, so nothing
                            // beside it moves when the decimal arrives at the end.
                            shownText = if (reveal.value >= 1f) {
                                settledFigure
                            } else {
                                "${(percentage * reveal.value).toInt()}%"
                            },
                            settledText = settledFigure,
                            color = accuracy,
                            style = MaterialTheme.typography.displayMedium,
                        )
                        Text(
                            text = stringResource(Res.string.progress_accuracy_caption),
                            style = MaterialTheme.typography.titleMedium,
                            color = onHeroMuted,
                        )
                    }
                }
                HorizontalDivider(color = onHero.copy(alpha = HeroBorderAlpha))
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
                    MetricRow(
                        label = stringResource(Res.string.progress_completed_attempts_label),
                        value = completedAttemptCount.toString(),
                        labelColor = onHeroMuted,
                        valueColor = onHero,
                    )
                    MetricRow(
                        label = stringResource(Res.string.progress_questions_answered_label),
                        value = answeredQuestionCount.toString(),
                        labelColor = onHeroMuted,
                        valueColor = onHero,
                    )
                    MetricRow(
                        label = stringResource(Res.string.progress_correct_answers_label),
                        value = correctAnswerCount.toString(),
                        labelColor = onHeroMuted,
                        valueColor = onHero,
                    )
                }
                HeroCoverage(
                    coverage = coverage,
                    onHero = onHero,
                    onHeroMuted = onHeroMuted,
                    // Settles in just behind the figure rather than with it, so the hero reads as
                    // one arrival with an order to it instead of everything appearing at once. The
                    // stagger is in the spec, not in a coroutine, so nothing waits on it.
                    modifier = Modifier.animateEnterExit(
                        enter = fadeIn(AppMotion.revealSpec(CoverageRevealDelayMillis)),
                    ),
                )
            }
        }
    }
}

/**
 * How much of the current bank the learner has seen — supporting context for the figure above it,
 * never a second headline.
 *
 * Deliberately not tinted with [accuracyColor]: colouring 30% coverage red would read as a bad
 * score, when it only means most of the bank is still ahead of the learner. The meter is the
 * primary brand family for the same reason. The raw counts are always printed beside the
 * percentage, because coverage and accuracy differ substantially for normal learners and the
 * denominator is what explains why.
 */
@Composable
private fun HeroCoverage(
    coverage: ProgressCoverageUiModel,
    onHero: Color,
    onHeroMuted: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
    ) {
        val percentage = coverage.percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.progress_coverage_title),
                style = MaterialTheme.typography.labelLarge,
                color = onHeroMuted,
            )
            percentage?.let {
                Text(
                    text = formatAccuracy(it),
                    style = MaterialTheme.typography.titleMedium,
                    color = onHero,
                )
            }
        }
        if (percentage == null) {
            // 0/0 is "nothing to cover", not 0% covered, so say that rather than draw an empty bar.
            Text(
                text = stringResource(Res.string.progress_coverage_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = onHeroMuted,
            )
        } else {
            ProgressMeter(
                // The exact count ratio, not the rounded percentage printed above it.
                fraction = coverage.attemptedQuestionCount.toFloat() / coverage.totalQuestionCount,
                color = MaterialTheme.colorScheme.primary,
                // The page's neutral track would be a grey borrowed from a surface that is not
                // underneath this bar.
                trackColor = onHero.copy(alpha = CoverageTrackAlpha),
            )
            Text(
                text = stringResource(
                    Res.string.progress_coverage_count,
                    coverage.attemptedQuestionCount,
                    coverage.totalQuestionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = onHeroMuted,
            )
        }
    }
}

/**
 * Supporting text on the hero, as a share of its on-colour.
 *
 * A share rather than a second token because the gradient documents exactly one on-colour, and a
 * separate muted value would be a colour nothing has verified against either endpoint. At this
 * alpha the composite still clears WCAG body contrast over both ends of both sweeps, which
 * `ProgressHeroThemeTest` asserts rather than leaving to the eye.
 */
private const val SupportingTextAlpha = 0.8f

/** An edge, not an outline: visible where the gradient meets the page and nowhere else. */
private const val HeroBorderAlpha = 0.14f

/** The unfilled part of the coverage meter, in the hero's own on-colour. */
private const val CoverageTrackAlpha = 0.2f

private val HeroBorderWidth = 1.dp
private val HeroElevation = 2.dp

/**
 * The hero rises by a fraction of its own height, so the distance suits the surface rather than
 * being a fixed offset that reads as a long slide on a short card and a twitch on a tall one.
 */
private const val EntranceRiseFraction = 6

/** Behind the figure, not after it: the whole entry still lands inside 400ms. */
private const val CoverageRevealDelayMillis = 100

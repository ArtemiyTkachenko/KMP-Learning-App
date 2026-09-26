package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras

/**
 * Accuracy colour, keyed to the same threshold the domain uses to call an area weak.
 *
 * Percentages used to render as plain text at a uniform size and colour, so 37.5% and 91.6% were
 * indistinguishable at a glance on exactly the screens meant to show where a learner stands.
 */
@Composable
@ReadOnlyComposable
internal fun accuracyColor(percentage: Double): Color {
    val semantic = AppThemeExtras.semanticColors
    return when {
        percentage >= StrongAccuracyThreshold -> semantic.correct
        percentage >= LearningProgressPolicy.WeakAccuracyThresholdPercentage -> semantic.partiallyCorrect
        else -> semantic.incorrect
    }
}

/** Comfortably above the weakness threshold, so "good" and "only just passing" differ. */
private const val StrongAccuracyThreshold = 85.0

/**
 * The horizontal meter under a figure, in the one style the product uses for all of them.
 *
 * This existed four times with the same styling copied out, and three of those copies passed their
 * value straight through while the fourth — the assessment progress meter — animated it. So the
 * accuracy bar on the result screen snapped from one length to another while the visually identical
 * bar during an assessment travelled, which read as two different controls.
 *
 * The value is animated because a meter's length *is* its meaning: seeing it move from 40% to 60%
 * says something a redrawn bar at 60% does not. The first composition is not animated for the same
 * reason — a bar growing from zero every time a screen opens would be stating a change that did not
 * happen.
 *
 * There is deliberately no "fill from empty" option. The completion hero had one, for the one figure
 * in the app that genuinely came into existence moments ago — and it meant that screen ran two
 * animations, this meter's and the hero's own count, matched by hand on duration. The hero now draws
 * a ring swept by the same value that counts its figure, so the two are one movement and this meter
 * is back to doing exactly one thing.
 *
 * [trackColor] is a parameter for the same reason the neutral container of an answer option is: the
 * default is a statement about the *page's* surface ramp, which is not where this meter always sits.
 */
@Composable
internal fun ProgressMeter(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val target = fraction.coerceIn(0f, 1f)
    val animated = remember { Animatable(target) }
    LaunchedEffect(target) {
        // Nothing to travel on the first composition, where the Animatable was seeded with this
        // very value — and `animateTo` does not know that, so it would run a full invisible tween
        // from the target to itself. Which is not free: a screen showing several meters at once
        // spends the first frames of its life requesting frames for animations that cannot move,
        // and a list whose rows each carry one can keep that going as new rows scroll in.
        if (animated.value != target) {
            animated.animateTo(
                targetValue = target,
                animationSpec = AppMotion.effectSpec(AppMotion.ProgressDurationMillis),
            )
        }
    }
    LinearProgressIndicator(
        progress = { animated.value },
        modifier = modifier.fillMaxWidth().height(MeterHeight),
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
        // The default gap and stop indicator are Material's own progress affordances. They are
        // removed because this is a static measurement of how much has been covered, not an
        // operation in flight.
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

private val MeterHeight = 8.dp

/**
 * An accuracy as a swept ring: the form the product uses wherever a fraction-correct is the subject
 * of the surface it sits on rather than a figure inside a row.
 *
 * Material's determinate `CircularProgressIndicator` at a larger size and a heavier stroke, not a
 * `Canvas` of its own — the arc, the rounded cap, and the sweep from twelve o'clock are exactly what
 * the component already draws. The gap and the stop indicator are removed for the same reason
 * [ProgressMeter] removes them: this is a measurement of what happened, not an operation in flight.
 *
 * There is one size on purpose. The two surfaces that use it — the completion hero and the Progress
 * standing hero — are the two places in the app where the headline *is* an accuracy, and a ring that
 * changed size between them would read as two different controls rather than one product idea.
 *
 * Wrapped in a cleared box rather than given a `contentDescription`. The component publishes
 * `progressBarRangeInfo`, which beside a figure that already states the same number would be a
 * second announcement of it — and, wherever the ring is animated into place, a wrong one for the
 * duration of the animation.
 */
@Composable
internal fun AccuracyRing(
    fraction: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clearAndSetSemantics {}) {
        CircularProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.size(AccuracyRingSize),
            color = color,
            trackColor = trackColor,
            strokeWidth = AccuracyRingStroke,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
    }
}

/** Large enough to read as the surface's own indicator beside a display-scale figure, small enough
 *  that it never becomes the subject; the stroke is scaled with it. */
private val AccuracyRingSize = 64.dp
private val AccuracyRingStroke = 6.dp

/**
 * The unswept part of a ring, as a share of whatever colour the text on that surface takes.
 *
 * Stated once because both heroes need the same answer and neither can use a neutral track: a grey
 * from the page's surface ramp reads as a control borrowed from another screen when it sits on a
 * gradient, and reads as a different grey from the card under it when it sits on a raised surface.
 */
internal const val AccuracyRingTrackAlpha = 0.22f

/**
 * The figure a card exists to show, one step below [AccuracyHeroCard]'s screen headline.
 *
 * This exists because three cards — coverage, recent performance, and the Practice Builder's summary
 * — each set `FontWeight.Bold` on a headline role at their own call site, while the same roles are
 * used elsewhere as ordinary headings at the scale's SemiBold. That is two intents sharing one
 * role, which is why the weight could not simply move into [AppTypography]: a figure is short,
 * numeric, and the reason its card exists, whereas a heading introduces the content beneath it.
 * Naming the intent once keeps the three cards in step and leaves headings alone.
 */
@Composable
internal fun MetricFigure(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = modifier,
    )
}

/**
 * A figure that is being counted out, stated and announced at once.
 *
 * [shownText] is what is drawn and changes every frame; [settledText] is what the node *says* it
 * is, from the first frame onward. Without that split a screen reader would be handed a number
 * that is merely passing through — announcing "three of ten" because that is where the tween
 * happened to be — and every test that reads the figure would depend on animation timing. The
 * count is presentation; the value is the fact.
 *
 * The figure also reserves the width of [settledText] for the whole count. A number that grows
 * from one digit to two grows *sideways* as well, and everything beside it is pushed along with it
 * for half a second. Laying the settled text out invisibly underneath costs one extra measure and
 * makes the count a change of digits rather than a change of layout.
 *
 * Shared rather than private to the completion hero because the Progress hero counts its accuracy
 * the same way, and two copies of this would be two chances to drop the semantics half of it.
 * [style] is a parameter only so a hero can choose its own display role; everything else about the
 * component is fixed, because the split above is the whole point of it.
 */
@Composable
internal fun CountedFigure(
    shownText: String,
    settledText: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    Box(modifier, contentAlignment = Alignment.CenterStart) {
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

/** Whole number when exact, otherwise one decimal place, with the percent sign attached. */
internal fun formatAccuracy(percentage: Double): String {
    val rounded = (percentage * 10.0).roundToInt() / 10.0
    val number = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return "$number%"
}

/** Compact status pill. Replaces bare coloured body text for weak areas and outcomes. */
@Composable
internal fun StatusBadge(
    text: String,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.Grouped, vertical = AppSpacing.Tight),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}

/**
 * A block of text with a figure beside it, which drops below the text when it no longer fits there.
 *
 * ## The defect this exists for
 *
 * The Topics catalogue and a Topic's Subtopics draw the same shape: a weighted text column, then an
 * accuracy figure with its `learning_context_accuracy` caption under it. A `Row` measures its
 * non-weighted children first, so the figure takes its intrinsic width and the text column gets
 * whatever is left. At `fontScale = 2f` on a 360dp window that left the Subtopic title about 165dp
 * and broke "Structured concurrency" across a line **inside the word**; the Topic row, which also
 * carries a leading marker, was worse — "Kotlin langua / ge", with the learning-units badge broken
 * mid-word too and its pill stretched out of shape.
 *
 * The caption is what makes the figure wide, and removing it is not the fix: it is a recorded
 * decision on both screens, because the line to the *left* of it is a coverage fraction and the
 * figure is an accuracy, and two readings with different denominators on one row is how a learner
 * comes to believe they are one number.
 *
 * ## Why this measures rather than picking a breakpoint
 *
 * The question is not "is the type large" — it is "is there still room for the text beside the
 * figure", and that depends on the type scale, the window, the locale's longest word, and whatever
 * leads the row. So the layout asks exactly that: [Measurable.minIntrinsicWidth] on the text is the
 * width of its longest unbreakable word, which is the point below which Compose starts breaking
 * inside one. If the space left beside the figure is at least that, nothing changes; if it is not,
 * the figure moves below.
 *
 * That also means **nothing moves at ordinary type**. The beside branch places the figure hard
 * against the trailing edge with [horizontalGap] before it, which is what an `Arrangement.spacedBy`
 * row already did, so the existing layouts are unchanged where they were never broken.
 *
 * Stacked, the figure keeps the full width and its own internal alignment, so a right-aligned
 * figure stays in its column instead of jumping to the leading edge — it drops a line, it does not
 * change what kind of thing it is.
 *
 * [figure] may compose nothing, which is the ordinary case for a scope with no recorded answer.
 * There is then no reflow question to ask and the text takes the row.
 */
@Composable
internal fun TrailingFigureRow(
    modifier: Modifier = Modifier,
    horizontalGap: Dp = AppSpacing.Grouped,
    verticalGap: Dp = AppSpacing.Tight,
    figure: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    Layout(contents = listOf(text, figure), modifier = modifier) { measurables, constraints ->
        val (textMeasurables, figureMeasurables) = measurables
        val textMeasurable = textMeasurables.first()
        val figureMeasurable = figureMeasurables.firstOrNull()
        val loose = constraints.copy(minWidth = 0)

        // Nothing to place beside, so there is no decision to make.
        if (figureMeasurable == null) {
            val placeable = textMeasurable.measure(constraints)
            return@Layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }

        val horizontal = horizontalGap.roundToPx()
        val figureWidth = figureMeasurable.maxIntrinsicWidth(constraints.maxHeight)

        // An unbounded width has no "beside" to run out of: everything fits, so nothing reflows.
        // A row in a list or a card never arrives this way, but a measurement pass elsewhere can.
        val fitsBeside = !constraints.hasBoundedWidth ||
            textMeasurable.minIntrinsicWidth(constraints.maxHeight) <=
            constraints.maxWidth - figureWidth - horizontal

        if (fitsBeside) {
            val figurePlaceable = figureMeasurable.measure(loose.copy(maxWidth = figureWidth))
            val textPlaceable = textMeasurable.measure(
                loose.copy(
                    maxWidth = if (constraints.hasBoundedWidth) {
                        (constraints.maxWidth - figurePlaceable.width - horizontal)
                            .coerceAtLeast(0)
                    } else {
                        constraints.maxWidth
                    },
                ),
            )
            val width = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                textPlaceable.width + horizontal + figurePlaceable.width
            }
            val height = maxOf(textPlaceable.height, figurePlaceable.height)
            return@Layout layout(width, height) {
                textPlaceable.place(
                    x = 0,
                    y = Alignment.CenterVertically.align(textPlaceable.height, height),
                )
                figurePlaceable.place(
                    x = width - figurePlaceable.width,
                    y = Alignment.CenterVertically.align(figurePlaceable.height, height),
                )
            }
        }

        val width = constraints.maxWidth
        val textPlaceable = textMeasurable.measure(loose)
        // Given the whole width rather than its content width, so a figure that aligns itself to
        // the trailing edge stays where the eye already expects it.
        val figurePlaceable = figureMeasurable.measure(constraints.copy(minWidth = width))
        val vertical = verticalGap.roundToPx()
        val height = textPlaceable.height + vertical + figurePlaceable.height
        layout(width, height) {
            textPlaceable.place(0, 0)
            figurePlaceable.place(0, textPlaceable.height + vertical)
        }
    }
}

/** A metric line: label on the left, value on the right, so figures form a scannable column. */
@Composable
internal fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    /**
     * The label's colour, for the same reason [ProgressMeter] takes a track colour: the default
     * names a role on the *page's* surface ramp, and this row is also used on the Progress hero's
     * gradient, where `onSurfaceVariant` is a grey borrowed from a surface that is not underneath
     * it.
     */
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}

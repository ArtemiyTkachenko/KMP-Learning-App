package org.artkachenko.kmp_learning_app.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing

/**
 * Stable handle so tests can assert the chart appears and disappears without reading pixels.
 *
 * Applied to the plot itself rather than to the labelled row around it, because the plot is the node
 * that carries the description of what is drawn: the axis labels beside it are decoration with their
 * semantics cleared, and a tag on the row would name a node holding no description at all.
 */
internal const val ProgressRecentTrendChartTag = "progress_recent_trend_chart"

/** A point in chart space: `x` left to right, `y` from the top (0f) to the bottom (1f). */
internal data class TrendPoint(
    val x: Float,
    val y: Float,
)

/**
 * The full accuracy range the chart always draws, whatever the observed values are.
 *
 * Fitting the axis to the data would be actively misleading here: a 72%, 74%, 76% series scaled to
 * its own 70-77% range draws as a dramatic climb, when the honest picture is three near-identical
 * results. The window is at most five attempts, so there is never enough data for an auto-fitted
 * axis to be worth that risk.
 */
private const val AxisMinimumPercentage = 0.0
private const val AxisMaximumPercentage = 100.0

/**
 * Chart-space positions for a series of accuracy percentages, oldest first.
 *
 * Kept as a pure function over unit coordinates so the two decisions worth protecting — the fixed
 * vertical scale above and the equal horizontal spacing below — are testable without measuring a
 * rendered chart.
 *
 * Horizontal spacing is deliberately uniform: the recent window is defined by a count of
 * assessments, not by elapsed time, so the distance between two points says nothing about how long
 * passed between them.
 */
internal fun trendPoints(percentages: List<Double>): List<TrendPoint> {
    if (percentages.isEmpty()) return emptyList()
    val lastIndex = percentages.lastIndex
    return percentages.mapIndexed { index, percentage ->
        val fraction =
            (percentage.coerceIn(AxisMinimumPercentage, AxisMaximumPercentage) - AxisMinimumPercentage) /
                (AxisMaximumPercentage - AxisMinimumPercentage)
        TrendPoint(
            // A single point has no span to spread across, so it sits in the middle rather than
            // dividing by zero.
            x = if (lastIndex == 0) 0.5f else index.toFloat() / lastIndex,
            y = 1f - fraction.toFloat(),
        )
    }
}

/** Where the horizontal guides sit: the two ends of the fixed scale, plus its midpoint. */
private val GuideFractions = listOf(0f, 0.5f, 1f)

/**
 * Taller than it was, which is the part of this drawing that carries the reading.
 *
 * A five-point series spans at most the full 0-100 axis and usually far less, so the height is what
 * decides whether a 20-point swing is visible at all. At 120dp a 55-to-90 climb was about 42dp of
 * travel and read as a scratch.
 */
private val ChartHeight = 148.dp

private val MarkerRadius = 4.dp

/** The newest attempt gets a halo rather than a bigger dot, so the series keeps one marker size. */
private val LatestMarkerHaloRadius = 9.dp
private const val LatestMarkerHaloAlpha = 0.28f

/**
 * The margin the plot keeps inside its own box, top and bottom.
 *
 * Sized to the widest thing drawn at a point rather than to the marker, so a 0% or a 100% attempt —
 * and the newest one in particular, which wears the halo — is drawn whole instead of half clipped.
 * The axis label column takes the same inset, which is what keeps the top label on the 100% guide
 * and the bottom label on the 0% guide at any font scale.
 */
private val PlotInset = LatestMarkerHaloRadius

private val LineWidth = 2.dp
private val GuideWidth = 1.dp

/** The area under the line: a tint at the curve, gone by the axis. */
private const val AreaTopAlpha = 0.24f
private const val AreaBottomAlpha = 0.02f

/**
 * One compact accuracy trajectory across the recent window, oldest on the left.
 *
 * The chart states no conclusion. It carries no direction colouring, no "improving"/"declining"
 * label and no fitted axis, because the domain deliberately exposes the raw observations rather
 * than classifying them; the learner reads the actual trajectory.
 *
 * It is also never the only representation of recent performance: the card around it prints the
 * window accuracy and counts, and [description] repeats every plotted value in order for anyone who
 * cannot see the drawing.
 */
@Composable
internal fun RecentTrendChart(
    percentages: List<Double>,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
    ) {
        // The guides were drawn unlabelled, which left the drawing readable only as "the line went
        // up". Naming the three fixed guides is what turns it into a chart: a learner can now see
        // that a point sits just under half rather than only that it sits lower than the one before
        // it, and it states that the axis is the full 0-100 range rather than fitted to the data.
        //
        // Labels rather than a second described node: the Canvas's own description already repeats
        // every plotted value in order, so these are decoration to a screen reader and are cleared.
        Column(
            modifier = Modifier
                .width(AxisLabelWidth)
                .height(ChartHeight)
                // The same inset the plot applies, so the label band is exactly the band the
                // guides span: the top label sits on the 100% line and the bottom label on the 0%
                // line rather than half a line outside each. Robust at any font scale, which an
                // offset by half a text height would not be.
                .padding(vertical = PlotInset)
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            AxisGuideLabels.forEach { percentage ->
                Text(
                    text = formatAccuracy(percentage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TrendPlot(
            percentages = percentages,
            description = description,
            modifier = Modifier.testTag(ProgressRecentTrendChartTag),
        )
    }
}

/**
 * The three guide values, top to bottom, matching [GuideFractions]. They are the ends and the
 * midpoint of the fixed scale, never values read off the data.
 */
private val AxisGuideLabels = listOf(
    AxisMaximumPercentage,
    (AxisMinimumPercentage + AxisMaximumPercentage) / 2,
    AxisMinimumPercentage,
)

/** Wide enough for "100%" at `labelSmall`, so the plot's left edge does not move between values. */
private val AxisLabelWidth = 36.dp

@Composable
private fun TrendPlot(
    percentages: List<Double>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant

    // The chart grows up out of its own axis once per visit, rather than being there already.
    //
    // One value, read in the draw scope, so the growth invalidates drawing and never recomposition.
    // It is held in a `rememberSaveable` flag and claimed before the animation starts, for the same
    // reason the completion hero's reveal is: this card sits in a scrolling list, and a chart that
    // redrew itself every time it came back into view would be claiming that something had changed.
    // It is the app's ordinary content-reveal duration, not the result screen's celebratory one —
    // a dashboard a learner opens to check on themselves should not perform.
    var alreadyDrawn by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (alreadyDrawn) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadyDrawn) {
            alreadyDrawn = true
            reveal.animateTo(targetValue = 1f, animationSpec = AppMotion.revealSpec())
        }
    }

    Canvas(
        // A Canvas publishes no semantics for what it draws, so the whole chart is one described
        // node rather than a cluster of focusable decorative shapes. The description states the
        // settled values from the first frame, so the growth above is never something a screen
        // reader has to wait out.
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description },
    ) {
        val inset = PlotInset.toPx()
        val plotWidth = (size.width - inset * 2).coerceAtLeast(0f)
        val plotHeight = (size.height - inset * 2).coerceAtLeast(0f)
        val baselineY = inset + plotHeight

        GuideFractions.forEach { fraction ->
            val y = inset + plotHeight * fraction
            drawLine(
                color = guideColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = GuideWidth.toPx(),
            )
        }

        val offsets = trendPoints(percentages).map { point ->
            Offset(
                x = inset + plotWidth * point.x,
                // Every point starts on the 0% guide and rises to its own value together, so the
                // series keeps its shape the whole way up instead of assembling point by point.
                y = inset + plotHeight * (1f - (1f - point.y) * reveal.value),
            )
        }
        if (offsets.isEmpty()) return@Canvas

        // The area is what makes a shallow series legible. Five points across a desktop-width pane
        // span at most a fifth of the height, and a 2dp polyline across that reads as a scratch on
        // the card; the same line with the ground under it filled reads as a quantity. It is a tint
        // that fades out before the axis, so it never competes with the guides it crosses.
        val area = Path().apply {
            moveTo(offsets.first().x, baselineY)
            offsets.forEach { lineTo(it.x, it.y) }
            lineTo(offsets.last().x, baselineY)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = AreaTopAlpha),
                    lineColor.copy(alpha = AreaBottomAlpha),
                ),
                startY = inset,
                endY = baselineY,
            ),
        )

        offsets.zipWithNext { from, to ->
            drawLine(
                color = lineColor,
                start = from,
                end = to,
                strokeWidth = LineWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // The newest attempt is the one the learner came to the card for, so it is the one point
        // that is found without counting along the line. A halo rather than a larger dot, because
        // a series whose markers are different sizes reads as a series of different things.
        drawCircle(
            color = lineColor.copy(alpha = LatestMarkerHaloAlpha),
            radius = LatestMarkerHaloRadius.toPx(),
            center = offsets.last(),
        )
        offsets.forEach { offset ->
            drawCircle(color = lineColor, radius = MarkerRadius.toPx(), center = offset)
        }
    }
}

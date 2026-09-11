package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
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

private val ChartHeight = 120.dp

/**
 * Fills a phone's card, and stops there. On a desktop window the card is several times wider, and a
 * five-point series drawn across all of it flattens into a nearly horizontal line; capping the
 * drawing keeps its proportions readable without giving the dashboard an adaptive layout of its own.
 */
private val MaxChartWidth = 420.dp
private val MarkerRadius = 4.dp
private val LineWidth = 2.dp
private val GuideWidth = 1.dp

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
        modifier = modifier.widthIn(max = MaxChartWidth + AxisLabelWidth).fillMaxWidth(),
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
                .padding(vertical = MarkerRadius)
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
    Canvas(
        // A Canvas publishes no semantics for what it draws, so the whole chart is one described
        // node rather than a cluster of focusable decorative shapes.
        modifier = modifier
            // The cap has to precede fillMaxWidth: the other order fills first and leaves nothing
            // for the cap to narrow.
            .widthIn(max = MaxChartWidth)
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics { contentDescription = description },
    ) {
        val markerRadius = MarkerRadius.toPx()
        // Inset by the marker radius so a 0% or 100% point is drawn whole instead of half clipped.
        val plotWidth = (size.width - markerRadius * 2).coerceAtLeast(0f)
        val plotHeight = (size.height - markerRadius * 2).coerceAtLeast(0f)

        GuideFractions.forEach { fraction ->
            val y = markerRadius + plotHeight * fraction
            drawLine(
                color = guideColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = GuideWidth.toPx(),
            )
        }

        val offsets = trendPoints(percentages).map { point ->
            Offset(
                x = markerRadius + plotWidth * point.x,
                y = markerRadius + plotHeight * point.y,
            )
        }
        offsets.zipWithNext { from, to ->
            drawLine(
                color = lineColor,
                start = from,
                end = to,
                strokeWidth = LineWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
        offsets.forEach { offset ->
            drawCircle(color = lineColor, radius = markerRadius, center = offset)
        }
    }
}

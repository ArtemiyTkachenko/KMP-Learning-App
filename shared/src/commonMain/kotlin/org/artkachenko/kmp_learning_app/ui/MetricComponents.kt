package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 * The headline number for a screen: an accuracy percentage with a supporting caption and a meter.
 * Used by the progress dashboard, the topic drill-down, and both result screens so the primary
 * outcome reads the same way everywhere.
 */
@Composable
internal fun AccuracyHeadline(
    percentage: Double,
    caption: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val color = accuracyColor(percentage)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = formatAccuracy(percentage),
                style = MaterialTheme.typography.displaySmall,
                color = color,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        ProgressMeter(fraction = (percentage / 100.0).toFloat(), color = color)
        supporting?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
 */
@Composable
internal fun ProgressMeter(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val target = fraction.coerceIn(0f, 1f)
    val animated = remember { Animatable(target) }
    LaunchedEffect(target) {
        animated.animateTo(target, AppMotion.effectSpec(AppMotion.ProgressDurationMillis))
    }
    LinearProgressIndicator(
        progress = { animated.value },
        modifier = modifier.fillMaxWidth().height(MeterHeight),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
 * The figure a card exists to show, one step below [AccuracyHeadline]'s screen headline.
 *
 * This exists because three cards — coverage, recent performance, and the interview question count
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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

/** A metric line: label on the left, value on the right, so figures form a scannable column. */
@Composable
internal fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
        )
    }
}

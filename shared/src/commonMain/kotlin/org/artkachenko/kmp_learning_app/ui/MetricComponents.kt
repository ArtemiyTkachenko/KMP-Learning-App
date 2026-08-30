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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
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
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        LinearProgressIndicator(
            progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        supporting?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                fontWeight = FontWeight.SemiBold,
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
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

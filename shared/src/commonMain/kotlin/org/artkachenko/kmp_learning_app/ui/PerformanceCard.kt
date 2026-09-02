package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras

/**
 * One accuracy row: what it is on the left, how the learner is doing on the right.
 *
 * Shared by the progress dashboard, the topic drill-down, and the interview result so a topic's
 * performance reads identically wherever it appears. [detail] is already-formatted supporting text
 * so each feature keeps its own wording and string resources.
 *
 * The accuracy figure is the point of the row, so it is the largest thing in it and is coloured
 * against the domain's weakness threshold. Weak rows also tint their container and carry a badge,
 * so they are identifiable without reading the number.
 */
@Composable
internal fun PerformanceCard(
    title: String,
    detail: String,
    percentage: Double,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    caption: String? = null,
    isWeak: Boolean = false,
    weakLabel: String? = null,
    showChevron: Boolean = false,
    isSummary: Boolean = false,
) {
    val semantic = AppThemeExtras.semanticColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = if (isSummary) MaterialTheme.shapes.large else MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isWeak) {
                semantic.partiallyCorrectContainer
            } else if (isSummary) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (isWeak) BorderStroke(1.dp, semantic.partiallyCorrect) else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                caption?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isWeak && weakLabel != null) {
                    StatusBadge(
                        text = weakLabel,
                        contentColor = semantic.onPartiallyCorrectContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        icon = AppIcons.Warning,
                    )
                }
            }
            Text(
                text = formatAccuracy(percentage),
                style = MaterialTheme.typography.titleLarge,
                color = accuracyColor(percentage),
            )
            if (showChevron) {
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

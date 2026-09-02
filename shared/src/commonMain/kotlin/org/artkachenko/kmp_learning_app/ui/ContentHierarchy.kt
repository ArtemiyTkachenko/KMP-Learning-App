package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing

/** Screen-level summary content, intentionally stronger than ordinary interactive rows. */
@Composable
internal fun PrimarySummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Generous),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            content = content,
        )
    }
}

/**
 * Supporting summary content: the same shape language as [PrimarySummaryCard], one tonal step
 * quieter and without an outline, so a screen can carry several summaries without any of them
 * competing with its headline figure.
 */
@Composable
internal fun SecondarySummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Comfortable),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            content = content,
        )
    }
}

/**
 * Shared section hierarchy for scrollable learning and progress content.
 *
 * The top margin is the point of this component. A heading sits in the same `spacedBy` flow as the
 * cards it introduces, so with only 8.dp of its own it was separated from the section above by
 * about the same distance as two sibling cards — proximity then said nothing about grouping, and
 * the screens read as one undifferentiated column of cards. [AppSpacing.Section] is deliberately
 * twice the largest gap between siblings, so a section break is unambiguous.
 */
@Composable
internal fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .padding(top = AppSpacing.Section)
            .semantics { heading() },
    )
}

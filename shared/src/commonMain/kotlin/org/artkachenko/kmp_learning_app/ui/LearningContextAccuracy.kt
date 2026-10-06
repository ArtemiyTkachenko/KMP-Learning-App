package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_accuracy
import kmp_learning_app.shared.generated.resources.learning_context_answered
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The labelled accuracy figure a Topic card and a Subtopic row put at their trailing edge.
 *
 * Follows the three accuracy states of [LearningContextUiModel]. A never-answered scope emits
 * nothing, so [TrailingFigureRow] has no reflow question to ask. A scope below the evidence minimum
 * keeps the slot but states its evidence — "2 answered" in the neutral variant colour — because a
 * percentage in `accuracyColor` from two answers reads as a verdict the policy has not reached.
 * Only a scope with enough evidence draws the figure itself.
 *
 * The label stays in both answered states, so the figure cannot be mistaken for the coverage count
 * beside it and the column keeps one shape down a list. End-aligned for the same reason the
 * shared row's figure is: when the row reflows, this stays in the right-hand column.
 */
@Composable
internal fun LearningContextAccuracy(context: LearningContextUiModel) {
    val accuracy = context.accuracyPercentage ?: return
    Column(horizontalAlignment = Alignment.End) {
        if (context.hasAccuracyEvidence) {
            Text(
                text = formatAccuracy(accuracy),
                style = MaterialTheme.typography.titleMedium,
                color = accuracyColor(accuracy),
            )
        } else {
            Text(
                text = pluralStringResource(
                    Res.plurals.learning_context_answered,
                    context.answeredCount,
                    context.answeredCount,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(Res.string.learning_context_accuracy),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

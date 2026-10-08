package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_accuracy
import kmp_learning_app.shared.generated.resources.learning_context_accuracy_pending
import kmp_learning_app.shared.generated.resources.learning_context_accuracy_pending_description
import kmp_learning_app.shared.generated.resources.learning_context_weak_area
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressPolicy
import org.artkachenko.kmp_learning_app.ui.theme.AppIconSize
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The labelled accuracy figure a Topic card and a Subtopic row put at their trailing edge: a value,
 * and under it a label that always describes that value.
 *
 * Follows the three accuracy states of [LearningContextUiModel]:
 *
 * - **never answered** — nothing at all, so [TrailingFigureRow] has no reflow question to ask, and
 *   an untouched scope is never drawn as 0%;
 * - **answered, below the evidence minimum** — a neutral "—" over "accuracy". The slot keeps its
 *   shape down the list, but no percentage in `accuracyColor` appears, because one from four answers
 *   reads as a verdict the policy has not reached. Its spoken text says why the figure is missing
 *   and how many answers it needs, from [LearningProgressPolicy.WeakAreaMinimumAnswered]. This
 *   replaced an answer count ("4 answered") that sat over the "accuracy" label and read as nonsense;
 * - **with evidence** — the whole-number percentage in `accuracyColor` over "accuracy", or over
 *   "weak area" with the warning mark when the domain's verdict says so. That is where the weak
 *   signal lives on these rows: a separate pill under the text repeated what the coloured figure
 *   beside it already said and made a weak card taller than its neighbours.
 *
 * End-aligned for the same reason the shared row's figure is: when the row reflows, this stays in
 * the right-hand column.
 */
@Composable
internal fun LearningContextAccuracy(context: LearningContextUiModel) {
    val accuracy = context.accuracyPercentage ?: return
    if (!context.hasAccuracyEvidence) {
        val description = pluralStringResource(
            Res.plurals.learning_context_accuracy_pending_description,
            LearningProgressPolicy.WeakAreaMinimumAnswered,
            LearningProgressPolicy.WeakAreaMinimumAnswered,
            context.answeredCount,
        )
        Column(horizontalAlignment = Alignment.End) {
            // A dash says nothing to a screen reader, so it is read as the reason there is no
            // figure. It is the dash's *text* that is replaced, not a content description added:
            // the card and the row merge their descendants, and on Desktop a merged content
            // description becomes the whole accessible name, which would drop the Topic's name.
            Box(Modifier.clearAndSetSemantics { text = AnnotatedString(description) }) {
                Text(
                    text = stringResource(Res.string.learning_context_accuracy_pending),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FigureLabel(stringResource(Res.string.learning_context_accuracy))
        }
        return
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = formatWholeAccuracy(accuracy),
            style = MaterialTheme.typography.titleMedium,
            color = accuracyColor(accuracy),
        )
        // The domain's verdict, never re-derived from the figure. It needs the same evidence the
        // figure does, so it can only appear here.
        if (context.isWeak) {
            WeakAreaLabel()
        } else {
            FigureLabel(stringResource(Res.string.learning_context_accuracy))
        }
    }
}

@Composable
private fun FigureLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * "weak area" in the weak-area amber, with its warning mark.
 *
 * The `partiallyCorrect` accent, not its container pair: this is a line of label text on the card's
 * `surfaceContainerLow`, and `AppColorSchemeTest` holds that accent at body-text contrast on every
 * surface level in both themes. The mark is decorative; the words carry the status.
 */
@Composable
private fun WeakAreaLabel() {
    val amber = AppThemeExtras.semanticColors.partiallyCorrect
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.Warning,
            contentDescription = null,
            tint = amber,
            modifier = Modifier.size(AppIconSize.Inline),
        )
        Text(
            text = stringResource(Res.string.learning_context_weak_area),
            style = MaterialTheme.typography.labelSmall,
            color = amber,
        )
    }
}

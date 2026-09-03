package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_explanation
import kmp_learning_app.shared.generated.resources.assessment_review_source
import kmp_learning_app.shared.generated.resources.assessment_review_source_open_failed
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * The parts of a reviewed Question that are authored content rather than attempt outcome.
 *
 * An explanation, a source list, and an answer option read the same whether the learner is looking
 * at a Question they answered or at one they deliberately saved, so those pieces live here and are
 * shared. What stays in [ReviewQuestionCard] is everything that only means something in the context
 * of an attempt: the correct/partial/incorrect outcome and which answers were selected. Saved
 * Questions carry no attempt, so they render these components with no outcome at all rather than
 * with a fabricated one.
 */

/** Links sit flush with the card's text column rather than inset like a button. */
private val SourceLinkPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)

/**
 * One answer option's container.
 *
 * The colours are the caller's, because what they mean is the caller's: attempt review colours by
 * how the selection related to the authored correct set, while a saved Question can only say which
 * options are correct. [tags] is null when the option carries no label at all, so a plain option
 * does not gain the spacing of an empty tag row.
 */
@Composable
internal fun QuestionAnswerOption(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tags: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            Modifier.padding(AppSpacing.Grouped),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (tags != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tags()
                }
            }
        }
    }
}

/** Small, coloured, and bolder than the answer text so the labels stop competing with it. */
@Composable
internal fun QuestionAnswerTag(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

@Composable
internal fun QuestionExplanationBlock(
    explanation: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            Modifier.padding(AppSpacing.Grouped),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        ) {
            Text(
                stringResource(Res.string.assessment_review_explanation),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * The Question's authored sources, and the failure of a tap on one of them.
 *
 * The notice is rendered here, beside the link that failed, rather than at the top of the screen:
 * source links sit deep in a scrolling list, so a screen-level message would be out of view. The two
 * belong together, which is why one component owns both.
 */
@Composable
internal fun QuestionSources(
    sources: List<ReviewSourceUiModel>,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (sources.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped)) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            sources.forEach { source ->
                // A link, not a primary action: these used to be filled buttons stacked inside the
                // card, which competed with the answer content.
                TextButton(
                    onClick = { onSourceClick(source.url) },
                    contentPadding = SourceLinkPadding,
                ) {
                    Icon(
                        imageVector = AppIcons.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        stringResource(Res.string.assessment_review_source, source.title),
                        modifier = Modifier.padding(start = AppSpacing.Related),
                    )
                }
            }
        }
        if (failedSourceUrl != null && sources.any { it.url == failedSourceUrl }) {
            Text(
                stringResource(Res.string.assessment_review_source_open_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

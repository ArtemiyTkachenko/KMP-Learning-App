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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_correct
import kmp_learning_app.shared.generated.resources.assessment_review_correctly_selected
import kmp_learning_app.shared.generated.resources.assessment_review_explanation
import kmp_learning_app.shared.generated.resources.assessment_review_incorrect
import kmp_learning_app.shared.generated.resources.assessment_review_incorrectly_selected
import kmp_learning_app.shared.generated.resources.assessment_review_missed
import kmp_learning_app.shared.generated.resources.assessment_review_partially_correct
import kmp_learning_app.shared.generated.resources.assessment_review_source
import kmp_learning_app.shared.generated.resources.assessment_review_source_open_failed
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

/**
 * How an answered Question is presented, wherever it is answered.
 *
 * An explanation, a source list, and an answer option read the same whether the learner is looking
 * at a Question they answered or at one they deliberately saved, so those pieces live here and are
 * shared. Saved Questions carry no attempt, so they render these components with no outcome at all
 * rather than with a fabricated one.
 *
 * The outcome vocabulary below — which option was right, which was picked, and what the Question as
 * a whole came to — used to be private to [ReviewQuestionCard], because the results screen was the
 * only place a learner ever saw it. Formative practice reveals the same thing the moment an answer
 * is submitted, so the boundary moved: the derivation and the colours are stated once here and both
 * surfaces read them. Two copies of "what does a wrong answer look like" is how a product ends up
 * teaching two.
 */

/**
 * One option's relation to the authored correct set, once an answer has been submitted.
 *
 * [MISSED] is the case that makes this worth naming: an option the learner did not pick and should
 * have. It is not a failure of that option, so it is marked rather than filled — the row keeps the
 * neutral container of its surroundings and takes only the accent border and the label.
 */
internal enum class AnswerOutcome { CORRECT, MISSED, WRONG, NEUTRAL }

/** Pure derivation, so the same two facts always produce the same mark on either surface. */
internal fun answerOutcome(wasSelected: Boolean, isCorrectAnswer: Boolean): AnswerOutcome = when {
    wasSelected && isCorrectAnswer -> AnswerOutcome.CORRECT
    wasSelected -> AnswerOutcome.WRONG
    isCorrectAnswer -> AnswerOutcome.MISSED
    else -> AnswerOutcome.NEUTRAL
}

/** Overall outcome of one answered question. */
internal enum class QuestionOutcome { CORRECT, PARTIAL, INCORRECT }

/**
 * Derived from what was selected against what was authored, with scoring left alone.
 *
 * Persisted correctness stays authoritative for [QuestionOutcome.CORRECT]; the partial case only
 * refines how a question that was *scored incorrect* is presented. A learner who picked two of
 * three correct options and nothing wrong has not done the same thing as one who picked the wrong
 * option, and saying so costs nothing and changes no score.
 */
internal fun questionOutcome(
    scoredCorrect: Boolean,
    selectedAnswerIds: Set<String>,
    correctAnswerIds: Collection<String>,
): QuestionOutcome {
    if (scoredCorrect) return QuestionOutcome.CORRECT
    val pickedWrong = selectedAnswerIds.any { it !in correctAnswerIds }
    val pickedAnyCorrect = selectedAnswerIds.any { it in correctAnswerIds }
    return if (!pickedWrong && pickedAnyCorrect) QuestionOutcome.PARTIAL else QuestionOutcome.INCORRECT
}

/** The three colours one option needs: its border, its fill, and the colour of its label. */
@Immutable
internal data class AnswerOutcomeColors(
    val border: Color,
    val container: Color,
    val tagColor: Color?,
)

/**
 * [neutralContainer] is the caller's, because it is a statement about depth rather than about
 * outcome: an option on a practice page sits one level above that page, while an option inside a
 * review card sits inside the card. Only the two filled outcomes override it — semantic colour
 * marks an answer, it does not repaint every row around it.
 */
@Composable
internal fun AnswerOutcome.colors(neutralContainer: Color): AnswerOutcomeColors {
    val semantic = AppThemeExtras.semanticColors
    return when (this) {
        AnswerOutcome.CORRECT -> AnswerOutcomeColors(
            border = semantic.correct,
            container = semantic.correctContainer,
            tagColor = semantic.correct,
        )
        AnswerOutcome.WRONG -> AnswerOutcomeColors(
            border = semantic.incorrect,
            container = semantic.incorrectContainer,
            tagColor = semantic.incorrect,
        )
        AnswerOutcome.MISSED -> AnswerOutcomeColors(
            border = semantic.correct,
            container = neutralContainer,
            tagColor = semantic.correct,
        )
        AnswerOutcome.NEUTRAL -> AnswerOutcomeColors(
            border = MaterialTheme.colorScheme.outlineVariant,
            container = neutralContainer,
            tagColor = null,
        )
    }
}

/**
 * The label beside a marked option.
 *
 * Every outcome that carries colour also carries a word, because colour alone is not a channel this
 * app is willing to state a result in. [AnswerOutcome.NEUTRAL] has nothing to add, and returns null
 * so a plain option does not gain the spacing of an empty tag row.
 */
@Composable
internal fun AnswerOutcome.tagLabel(): String? = when (this) {
    AnswerOutcome.CORRECT -> stringResource(Res.string.assessment_review_correctly_selected)
    AnswerOutcome.MISSED -> stringResource(Res.string.assessment_review_missed)
    AnswerOutcome.WRONG -> stringResource(Res.string.assessment_review_incorrectly_selected)
    AnswerOutcome.NEUTRAL -> null
}

/** The Question's own verdict, as the app's status pill. */
@Composable
internal fun QuestionOutcomeBadge(
    outcome: QuestionOutcome,
    modifier: Modifier = Modifier,
) {
    val semantic = AppThemeExtras.semanticColors
    val (text, content, container) = when (outcome) {
        QuestionOutcome.CORRECT -> Triple(
            stringResource(Res.string.assessment_review_correct),
            semantic.onCorrectContainer,
            semantic.correctContainer,
        )
        QuestionOutcome.PARTIAL -> Triple(
            stringResource(Res.string.assessment_review_partially_correct),
            semantic.onPartiallyCorrectContainer,
            semantic.partiallyCorrectContainer,
        )
        QuestionOutcome.INCORRECT -> Triple(
            stringResource(Res.string.assessment_review_incorrect),
            semantic.onIncorrectContainer,
            semantic.incorrectContainer,
        )
    }
    StatusBadge(text = text, contentColor = content, containerColor = container, modifier = modifier)
}

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

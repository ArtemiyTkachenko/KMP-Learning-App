package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_collapse
import kmp_learning_app.shared.generated.resources.assessment_review_correct
import kmp_learning_app.shared.generated.resources.assessment_review_correctly_selected
import kmp_learning_app.shared.generated.resources.assessment_review_expand
import kmp_learning_app.shared.generated.resources.assessment_review_explanation
import kmp_learning_app.shared.generated.resources.assessment_review_incorrect
import kmp_learning_app.shared.generated.resources.assessment_review_incorrectly_selected
import kmp_learning_app.shared.generated.resources.assessment_review_missed_correct_answer
import kmp_learning_app.shared.generated.resources.assessment_review_partially_correct
import kmp_learning_app.shared.generated.resources.assessment_review_source
import kmp_learning_app.shared.generated.resources.assessment_review_source_open_failed
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
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
    // "Correct answer", not "Missed". This row wears the success accent because it *is* the right
    // answer, and it used to carry a cross beside it — so the one row on the screen that answers
    // "what should I have picked?" was marked with the glyph the app uses for wrong. The outcome
    // is still MISSED, which is what it is; the label is what the learner needs it to say.
    AnswerOutcome.MISSED -> stringResource(Res.string.assessment_review_missed_correct_answer)
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
        // Flush: consecutive `TextButton`s already carry Material's own vertical padding,
        // so arrangement spacing here would be added on top of two lots of it.
        Column(verticalArrangement = Arrangement.Top) {
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

/**
 * A Question's detail, behind the one disclosure the product uses for it.
 *
 * Three surfaces show the same authored content — a result transcript, the Mistakes queue, and
 * Saved Questions — and until now only two of them could close it. `SavedQuestionCard` had no
 * disclosure at all, so a collection of twenty saved Questions was twenty permanently open blocks
 * of options, explanation and sources, and finding one meant scrolling past all the others in full.
 * That is not a decision the screen had made; it is a control it never got. Sharing the control is
 * what makes it one control rather than a second one that looks similar.
 *
 * The caller owns [expanded], because the right default differs and is a statement about the
 * content: a result transcript opens the questions the learner got wrong, and a saved collection
 * opens nothing, because it is a list to browse rather than a transcript to read through.
 *
 * [detail] is an `AnimatedVisibilityScope` so its pieces can take `Modifier.animateEnterExit` with
 * staggered specs. That is how the opened card relayouts once while its contents still arrive in
 * reading order, instead of several coroutines coordinating delays.
 */
@Composable
internal fun ColumnScope.QuestionDisclosure(
    expanded: Boolean,
    onToggle: () -> Unit,
    detail: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    QuestionDisclosureAction(expanded = expanded, onClick = onToggle)
    // The transcript's answer rows arrive the same way the practice screen's reveal does, and for
    // the same reason: this is the identical content — the options, the verdict on each, the
    // explanation, the sources — met minutes later instead of seconds. It used to appear and
    // disappear in a single frame, so opening one card in a list of twenty moved everything below
    // it by several hundred pixels with nothing to follow.
    AnimatedVisibility(
        visible = expanded,
        // From the top, not Compose's default of from the bottom: the first strip of a card
        // opening downwards should be the first thing there is to read. Left on the default, the
        // sliver revealed in the first hundred milliseconds is the *end* of the content — the
        // explanation and the source links — which is both the wrong reading order and the part
        // deliberately held back, so the card appeared to open empty.
        enter = expandVertically(AppMotion.spatialSpec(), expandFrom = Alignment.Top),
        // Collapsing is the learner putting something away, so it accelerates out: the card should
        // be closed before they have finished looking at it.
        exit = shrinkVertically(AppMotion.spatialSpec(), shrinkTowards = Alignment.Top) +
            fadeOut(AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2)),
        content = detail,
    )
}

/**
 * The control that opens a Question card, as a disclosure rather than as two commands.
 *
 * It was a bare text button whose word was replaced outright — "Review answer" one frame and "Hide
 * answer" the next — which reads as two different buttons occupying one place. A chevron that
 * turns over is the conventional way to say *this thing opens*, and rotating one glyph rather than
 * swapping two means the control travels between its states instead of arriving in the new one.
 *
 * The rotation is decoration: the word beside it states the action outright, and Material's button
 * semantics announce that word, so nothing here depends on the angle being seen.
 */
@Composable
private fun QuestionDisclosureAction(expanded: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) ExpandedChevronRotation else 0f,
        animationSpec = AppMotion.spatialSpec(),
        label = "questionDisclosureChevron",
    )
    TextButton(onClick = onClick) {
        Icon(
            imageVector = AppIcons.ExpandMore,
            contentDescription = null,
            modifier = Modifier
                .size(DisclosureIconSize)
                .graphicsLayer { rotationZ = rotation },
        )
        Text(
            text = stringResource(
                if (expanded) {
                    Res.string.assessment_review_collapse
                } else {
                    Res.string.assessment_review_expand
                },
            ),
            modifier = Modifier.padding(start = AppSpacing.Tight),
        )
    }
}

/** Half a turn, so the chevron ends pointing up rather than having spun all the way round. */
private const val ExpandedChevronRotation = 180f

/** Matches the leading-icon size Material gives a text button, as the save action does. */
private val DisclosureIconSize = 18.dp

/**
 * How far an opened card's contents trail the expansion that makes room for them.
 *
 * The same ordering the practice reveal uses — options, then the reason for them — so a learner who
 * answers a question and a learner who reviews it later watch the same thing happen.
 *
 * The answers themselves are given no delay at all, which is where this differs from the practice
 * reveal: there the staggered piece is a small badge, while here the options *are* most of the
 * height being opened. Holding them back even 60ms was visibly a card opening an empty space and
 * then filling it. The thing that makes the room arrives with the room.
 */
internal const val QuestionAnswersRevealDelayMillis = 0
internal const val QuestionExplanationRevealDelayMillis = 120

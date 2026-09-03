package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_accuracy_caption
import kmp_learning_app.shared.generated.resources.assessment_review_correct
import kmp_learning_app.shared.generated.resources.assessment_review_correct_answer
import kmp_learning_app.shared.generated.resources.assessment_review_incorrect
import kmp_learning_app.shared.generated.resources.assessment_review_missing_question
import kmp_learning_app.shared.generated.resources.assessment_review_partially_correct
import kmp_learning_app.shared.generated.resources.assessment_review_save_question
import kmp_learning_app.shared.generated.resources.assessment_review_score
import kmp_learning_app.shared.generated.resources.assessment_review_selected
import kmp_learning_app.shared.generated.resources.assessment_review_unresolved_questions
import kmp_learning_app.shared.generated.resources.assessment_review_unsave_question
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AssessmentScoreSummary(
    correctAnswers: Int,
    totalQuestions: Int,
    percentage: Double,
    modifier: Modifier = Modifier,
) {
    PrimarySummaryCard(modifier) {
        // The outcome of the assessment leads at display size instead of being a plain line of
        // text the same weight as everything under it.
        AccuracyHeadline(
            percentage = percentage,
            caption = stringResource(Res.string.assessment_review_accuracy_caption),
            supporting = stringResource(
                Res.string.assessment_review_score,
                correctAnswers,
                totalQuestions,
            ),
        )
    }
}

/**
 * Explains why the score above can exceed the questions listed below.
 *
 * [AssessmentScoreSummary] renders the persisted AssessmentScore and stays
 * authoritative, while review items and any topic breakdown can only count
 * questions whose curriculum content still resolves. The count is derived here
 * rather than in each result ViewModel so both result screens share one rule.
 */
@Composable
internal fun UnresolvedReviewQuestionsNotice(
    questions: List<ReviewQuestionItem>,
    totalQuestions: Int,
    modifier: Modifier = Modifier,
) {
    val resolved = questions.count { it is ReviewQuestionItem.Available }
    if (resolved >= totalQuestions) return

    Text(
        stringResource(
            Res.string.assessment_review_unresolved_questions,
            totalQuestions - resolved,
            totalQuestions,
        ),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        // A caveat about missing curriculum content, not a failure: full error red overstated it.
        color = AppThemeExtras.semanticColors.partiallyCorrect,
    )
}

/**
 * Stable per-Question handle for the save action.
 *
 * One convention for all three review surfaces, because they render the same shared card: a
 * surface-specific tag would suggest three affordances where there is one.
 */
internal fun reviewQuestionSaveTag(questionId: String): String =
    "review_question_save_$questionId"

/** How a reviewed answer relates to the authored correct set, for colouring only. */
private enum class AnswerOutcome { CORRECT, MISSED, WRONG, NEUTRAL }

/** Overall outcome of one reviewed question. */
private enum class QuestionOutcome { CORRECT, PARTIAL, INCORRECT }

/**
 * Derived purely from what the review model already carries. Persisted correctness stays
 * authoritative for [QuestionOutcome.CORRECT]; the partial case only refines how a question that
 * was *scored incorrect* is presented, so no scoring behaviour changes.
 */
private fun ReviewQuestionUiModel.outcome(): QuestionOutcome {
    if (isCorrect) return QuestionOutcome.CORRECT
    val pickedWrong = answers.any { it.wasSelected && !it.isCorrectAnswer }
    val pickedAnyCorrect = answers.any { it.wasSelected && it.isCorrectAnswer }
    return if (!pickedWrong && pickedAnyCorrect) QuestionOutcome.PARTIAL else QuestionOutcome.INCORRECT
}

private fun ReviewAnswerUiModel.outcome(): AnswerOutcome = when {
    wasSelected && isCorrectAnswer -> AnswerOutcome.CORRECT
    wasSelected -> AnswerOutcome.WRONG
    isCorrectAnswer -> AnswerOutcome.MISSED
    else -> AnswerOutcome.NEUTRAL
}

/**
 * [saveAction] is optional so this component stays usable where saved state is unknown, or where a
 * surface has no saving to offer at all. Everything else about the card is unchanged by it: the
 * action sits beside the heading and leaves the outcome, answers, explanation, and source links
 * exactly as they were.
 */
@Composable
internal fun ReviewQuestionCard(
    question: ReviewQuestionUiModel,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String? = null,
    saveAction: ReviewSaveAction? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    question.text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (saveAction != null) {
                    SaveQuestionAction(questionId = question.questionId, action = saveAction)
                }
            }
            QuestionOutcomeLabel(question.outcome())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.answers.forEach { ReviewAnswerRow(it) }
            }
            QuestionExplanationBlock(question.explanation)
            QuestionSources(
                sources = question.sources,
                onSourceClick = onSourceClick,
                failedSourceUrl = failedSourceUrl,
            )
        }
    }
}

/**
 * States the current saved state as the label of the action that changes it, in words rather than
 * by colour or icon shape alone, so it reads the same to assistive technology as it does on screen.
 * Disabled only while this Question's own mutation is being persisted.
 */
@Composable
private fun SaveQuestionAction(
    questionId: String,
    action: ReviewSaveAction,
) {
    TextButton(
        onClick = action.onToggle,
        enabled = !action.isPending,
        modifier = Modifier.testTag(reviewQuestionSaveTag(questionId)),
    ) {
        Text(
            stringResource(
                if (action.isSaved) {
                    Res.string.assessment_review_unsave_question
                } else {
                    Res.string.assessment_review_save_question
                },
            ),
        )
    }
}

@Composable
private fun QuestionOutcomeLabel(outcome: QuestionOutcome) {
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

    StatusBadge(text = text, contentColor = content, containerColor = container)
}

/**
 * One answered option: the shared option container, coloured and labelled by how this attempt's
 * selection related to the authored correct set.
 */
@Composable
private fun ReviewAnswerRow(answer: ReviewAnswerUiModel) {
    val semantic = AppThemeExtras.semanticColors
    val outcome = answer.outcome()
    val border = when (outcome) {
        AnswerOutcome.CORRECT, AnswerOutcome.MISSED -> semantic.correct
        AnswerOutcome.WRONG -> semantic.incorrect
        AnswerOutcome.NEUTRAL -> MaterialTheme.colorScheme.outlineVariant
    }
    val container = when (outcome) {
        AnswerOutcome.CORRECT -> semantic.correctContainer
        AnswerOutcome.WRONG -> semantic.incorrectContainer
        AnswerOutcome.MISSED, AnswerOutcome.NEUTRAL -> MaterialTheme.colorScheme.surface
    }

    QuestionAnswerOption(
        text = answer.text,
        borderColor = border,
        containerColor = container,
        tags = if (answer.wasSelected || answer.isCorrectAnswer) {
            {
                if (answer.wasSelected) {
                    QuestionAnswerTag(
                        text = stringResource(Res.string.assessment_review_selected),
                        color = if (answer.isCorrectAnswer) semantic.correct else semantic.incorrect,
                    )
                }
                if (answer.isCorrectAnswer) {
                    QuestionAnswerTag(
                        text = stringResource(Res.string.assessment_review_correct_answer),
                        color = semantic.correct,
                    )
                }
            }
        } else {
            null
        },
    )
}

@Composable
internal fun MissingReviewQuestion(
    questionId: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            stringResource(Res.string.assessment_review_missing_question, questionId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

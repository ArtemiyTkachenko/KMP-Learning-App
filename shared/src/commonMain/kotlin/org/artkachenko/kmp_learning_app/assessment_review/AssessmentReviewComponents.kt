package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_correct
import kmp_learning_app.shared.generated.resources.assessment_review_correct_answer
import kmp_learning_app.shared.generated.resources.assessment_review_explanation
import kmp_learning_app.shared.generated.resources.assessment_review_incorrect
import kmp_learning_app.shared.generated.resources.assessment_review_missing_question
import kmp_learning_app.shared.generated.resources.assessment_review_partially_correct
import kmp_learning_app.shared.generated.resources.assessment_review_percentage
import kmp_learning_app.shared.generated.resources.assessment_review_score
import kmp_learning_app.shared.generated.resources.assessment_review_selected
import kmp_learning_app.shared.generated.resources.assessment_review_source
import kmp_learning_app.shared.generated.resources.assessment_review_source_open_failed
import kmp_learning_app.shared.generated.resources.assessment_review_unresolved_questions
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun AssessmentScoreSummary(
    correctAnswers: Int,
    totalQuestions: Int,
    percentage: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(Res.string.assessment_review_score, correctAnswers, totalQuestions),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(Res.string.assessment_review_percentage, percentage.roundToInt()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.error,
    )
}

/** Links sit flush with the card's text column rather than inset like a button. */
private val SourceLinkPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)

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

@Composable
internal fun ReviewQuestionCard(
    question: ReviewQuestionUiModel,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                question.text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            QuestionOutcomeLabel(question.outcome())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.answers.forEach { ReviewAnswerRow(it) }
            }
            ExplanationBlock(question.explanation)
            if (question.sources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    question.sources.forEach { source ->
                        // A link, not a primary action: these used to be filled buttons stacked
                        // inside the card, which competed with the answer content.
                        TextButton(
                            onClick = { onSourceClick(source.url) },
                            contentPadding = SourceLinkPadding,
                        ) {
                            Text(stringResource(Res.string.assessment_review_source, source.title))
                        }
                    }
                }
            }
            // Rendered inside the card the user just tapped: source buttons sit deep in a
            // scrolling list, so a notice at the top of the screen would be out of view.
            if (failedSourceUrl != null && question.sources.any { it.url == failedSourceUrl }) {
                Text(
                    stringResource(Res.string.assessment_review_source_open_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
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

    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = container,
        border = BorderStroke(1.dp, border),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                answer.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (answer.wasSelected || answer.isCorrectAnswer) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (answer.wasSelected) {
                        AnswerTag(
                            text = stringResource(Res.string.assessment_review_selected),
                            color = if (answer.isCorrectAnswer) semantic.correct else semantic.incorrect,
                        )
                    }
                    if (answer.isCorrectAnswer) {
                        AnswerTag(
                            text = stringResource(Res.string.assessment_review_correct_answer),
                            color = semantic.correct,
                        )
                    }
                }
            }
        }
    }
}

/** Small, coloured, and bolder than the answer text so the labels stop competing with it. */
@Composable
private fun AnswerTag(
    text: String,
    color: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun ExplanationBlock(explanation: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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

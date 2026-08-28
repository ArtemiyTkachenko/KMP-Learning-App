package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_correct
import kmp_learning_app.shared.generated.resources.assessment_review_correct_answer
import kmp_learning_app.shared.generated.resources.assessment_review_explanation
import kmp_learning_app.shared.generated.resources.assessment_review_incorrect
import kmp_learning_app.shared.generated.resources.assessment_review_missing_question
import kmp_learning_app.shared.generated.resources.assessment_review_percentage
import kmp_learning_app.shared.generated.resources.assessment_review_score
import kmp_learning_app.shared.generated.resources.assessment_review_selected
import kmp_learning_app.shared.generated.resources.assessment_review_source
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun AssessmentScoreSummary(
    correctAnswers: Int,
    totalQuestions: Int,
    percentage: Double,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            stringResource(Res.string.assessment_review_score, correctAnswers, totalQuestions),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(stringResource(Res.string.assessment_review_percentage, percentage.roundToInt()))
    }
}

@Composable
internal fun ReviewQuestionCard(
    question: ReviewQuestionUiModel,
    onSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(question.text, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    if (question.isCorrect) Res.string.assessment_review_correct
                    else Res.string.assessment_review_incorrect,
                ),
                color = if (question.isCorrect) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            question.answers.forEach { answer ->
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(answer.text)
                    if (answer.wasSelected) {
                        Text(stringResource(Res.string.assessment_review_selected))
                    }
                    if (answer.isCorrectAnswer) {
                        Text(stringResource(Res.string.assessment_review_correct_answer))
                    }
                }
            }
            Text(stringResource(Res.string.assessment_review_explanation))
            Text(question.explanation)
            question.sources.forEach { source ->
                Button(onClick = { onSourceClick(source.url) }) {
                    Text(stringResource(Res.string.assessment_review_source, source.title))
                }
            }
        }
    }
}

@Composable
internal fun MissingReviewQuestion(
    questionId: String,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(Res.string.assessment_review_missing_question, questionId),
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
    )
}

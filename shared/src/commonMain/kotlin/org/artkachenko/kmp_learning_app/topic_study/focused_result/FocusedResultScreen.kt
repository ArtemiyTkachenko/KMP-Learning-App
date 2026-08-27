package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.focused_result_attempt_not_found
import kmp_learning_app.shared.generated.resources.focused_result_correct
import kmp_learning_app.shared.generated.resources.focused_result_error
import kmp_learning_app.shared.generated.resources.focused_result_explanation
import kmp_learning_app.shared.generated.resources.focused_result_incorrect
import kmp_learning_app.shared.generated.resources.focused_result_loading
import kmp_learning_app.shared.generated.resources.focused_result_missing_question
import kmp_learning_app.shared.generated.resources.focused_result_not_completed
import kmp_learning_app.shared.generated.resources.focused_result_percentage
import kmp_learning_app.shared.generated.resources.focused_result_retry
import kmp_learning_app.shared.generated.resources.focused_result_score
import kmp_learning_app.shared.generated.resources.focused_result_source
import kmp_learning_app.shared.generated.resources.focused_result_selected
import kmp_learning_app.shared.generated.resources.focused_result_correct_answer
import kmp_learning_app.shared.generated.resources.focused_result_title
import kmp_learning_app.shared.generated.resources.focused_result_practice_again
import kmp_learning_app.shared.generated.resources.focused_result_practice_starting
import kmp_learning_app.shared.generated.resources.focused_result_repeat_source_missing
import kmp_learning_app.shared.generated.resources.focused_result_repeat_no_questions
import kmp_learning_app.shared.generated.resources.focused_result_repeat_error
import kmp_learning_app.shared.generated.resources.topic_detail_back
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyTopAppBar
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

internal const val FocusedResultLoadingTag = "focused_result_loading"
internal const val FocusedResultPracticeAgainTag = "focused_result_practice_again"

@Composable
internal fun FocusedResultScreen(
    state: FocusedResultUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSourceClick: (String) -> Unit,
    onRepeatPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopicStudyTopAppBar(stringResource(Res.string.focused_result_title), onBack)
        when (state) {
            FocusedResultUiState.Loading -> Message(Modifier.weight(1f)) {
                CircularProgressIndicator(Modifier.testTag(FocusedResultLoadingTag))
            }
            FocusedResultUiState.AttemptNotFound -> Message(Modifier.weight(1f)) {
                Text(stringResource(Res.string.focused_result_attempt_not_found))
            }
            FocusedResultUiState.NotCompleted -> Message(Modifier.weight(1f)) {
                Text(stringResource(Res.string.focused_result_not_completed))
            }
            FocusedResultUiState.Error -> Message(Modifier.weight(1f)) {
                Text(stringResource(Res.string.focused_result_error))
                Button(onClick = onRetry, Modifier.padding(top = 16.dp)) {
                    Text(stringResource(Res.string.focused_result_retry))
                }
            }
            is FocusedResultUiState.Content -> ResultContent(state, onSourceClick, onRepeatPractice, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResultContent(
    state: FocusedResultUiState.Content,
    onSourceClick: (String) -> Unit,
    onRepeatPractice: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(Res.string.focused_result_score, state.correctAnswers, state.totalQuestions),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(Res.string.focused_result_percentage, state.percentage.roundToInt()))
            when (state.repeatPracticeState) {
                RepeatPracticeState.Idle -> Unit
                RepeatPracticeState.Creating -> Text(stringResource(Res.string.focused_result_practice_starting))
                RepeatPracticeState.SourceAttemptNotFound -> Text(stringResource(Res.string.focused_result_repeat_source_missing), color = MaterialTheme.colorScheme.error)
                RepeatPracticeState.NoEligibleQuestions -> Text(stringResource(Res.string.focused_result_repeat_no_questions), color = MaterialTheme.colorScheme.error)
                RepeatPracticeState.Error -> Text(stringResource(Res.string.focused_result_repeat_error), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onRepeatPractice,
                enabled = state.repeatPracticeState != RepeatPracticeState.Creating,
                modifier = Modifier.testTag(FocusedResultPracticeAgainTag),
            ) {
                if (state.repeatPracticeState == RepeatPracticeState.Creating) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(Res.string.focused_result_practice_again))
                }
            }
        }
        items(state.questions) { item ->
            when (item) {
                is ReviewQuestionItem.Missing -> Text(
                    stringResource(Res.string.focused_result_missing_question, item.questionId),
                    color = MaterialTheme.colorScheme.error,
                )
                is ReviewQuestionItem.Available -> ReviewQuestion(item.question, onSourceClick)
            }
        }
    }
}

@Composable
private fun ReviewQuestion(
    question: ReviewQuestionUiModel,
    onSourceClick: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(question.text, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    if (question.isCorrect) Res.string.focused_result_correct
                    else Res.string.focused_result_incorrect,
                ),
                color = if (question.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            question.answers.forEach { answer ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(answer.text)
                    if (answer.wasSelected) Text(stringResource(Res.string.focused_result_selected))
                    if (answer.isCorrectAnswer) Text(stringResource(Res.string.focused_result_correct_answer))
                }
            }
            Text(stringResource(Res.string.focused_result_explanation))
            Text(question.explanation)
            question.sources.forEach { source ->
                Button(onClick = { onSourceClick(source.url) }) {
                    Text(stringResource(Res.string.focused_result_source, source.title))
                }
            }
        }
    }
}

@Composable
private fun Message(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        content()
    }
}

package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.focused_practice_answer_save_error
import kmp_learning_app.shared.generated.resources.focused_practice_no_questions
import kmp_learning_app.shared.generated.resources.focused_practice_ready
import kmp_learning_app.shared.generated.resources.focused_practice_select_all
import kmp_learning_app.shared.generated.resources.focused_practice_select_one
import kmp_learning_app.shared.generated.resources.focused_practice_start_error
import kmp_learning_app.shared.generated.resources.focused_practice_submit
import kmp_learning_app.shared.generated.resources.focused_practice_submitting
import kmp_learning_app.shared.generated.resources.focused_practice_title
import kmp_learning_app.shared.generated.resources.focused_practice_question_progress
import kmp_learning_app.shared.generated.resources.topic_browser_retry
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyTopAppBar
import org.jetbrains.compose.resources.stringResource

internal const val FocusedPracticeLoadingTag = "focused_practice_loading"
internal const val FocusedPracticeSubmitTag = "focused_practice_submit"

@Composable
internal fun FocusedPracticeScreen(
    state: FocusedPracticeUiState,
    onAnswerClick: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopicStudyTopAppBar(
            title = stringResource(Res.string.focused_practice_title),
            onBack = onBack,
        )
        when (state) {
            FocusedPracticeUiState.Loading -> MessageContent(Modifier.weight(1f)) {
                CircularProgressIndicator(modifier = Modifier.testTag(FocusedPracticeLoadingTag))
            }

            FocusedPracticeUiState.NoQuestions -> MessageContent(Modifier.weight(1f)) {
                Text(text = stringResource(Res.string.focused_practice_no_questions))
            }

            FocusedPracticeUiState.Error -> MessageContent(Modifier.weight(1f)) {
                Text(text = stringResource(Res.string.focused_practice_start_error))
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(Res.string.topic_browser_retry))
                }
            }

            is FocusedPracticeUiState.Content -> QuestionContent(
                state = state,
                onAnswerClick = onAnswerClick,
                onSubmit = onSubmit,
                modifier = Modifier.weight(1f),
            )

            is FocusedPracticeUiState.ReadyToComplete -> MessageContent(Modifier.weight(1f)) {
                Text(text = stringResource(Res.string.focused_practice_ready))
            }
        }
    }
}

@Composable
private fun QuestionContent(
    state: FocusedPracticeUiState.Content,
    onAnswerClick: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(
                    Res.string.focused_practice_question_progress,
                    state.questionNumber,
                    state.totalQuestions,
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = state.question.text,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(
                    if (state.question.selectionMode == AnswerSelectionMode.SINGLE) {
                        Res.string.focused_practice_select_one
                    } else {
                        Res.string.focused_practice_select_all
                    },
                ),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(
            items = state.question.answers,
            key = { it.id },
        ) { answer ->
            val selected = answer.id in state.selectedAnswerIds
            AnswerRow(
                answerText = answer.text,
                selected = selected,
                mode = state.question.selectionMode,
                enabled = !state.isSubmitting,
                onClick = { onAnswerClick(answer.id) },
            )
        }
        item {
            if (state.submissionFailed) {
                Text(
                    text = stringResource(Res.string.focused_practice_answer_save_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit && !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FocusedPracticeSubmitTag),
            ) {
                Text(
                    text = stringResource(
                        if (state.isSubmitting) {
                            Res.string.focused_practice_submitting
                        } else {
                            Res.string.focused_practice_submit
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun AnswerRow(
    answerText: String,
    selected: Boolean,
    mode: AnswerSelectionMode,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (mode == AnswerSelectionMode.SINGLE) {
                    Modifier.selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = onClick,
                    )
                } else {
                    Modifier.toggleable(
                        value = selected,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onClick() },
                    )
                },
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mode == AnswerSelectionMode.SINGLE) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
        } else {
            Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
        }
        Text(text = answerText, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun MessageContent(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

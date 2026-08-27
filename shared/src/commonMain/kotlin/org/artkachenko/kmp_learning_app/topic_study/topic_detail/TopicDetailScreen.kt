package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.clickable
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
import kmp_learning_app.shared.generated.resources.topic_browser_retry
import kmp_learning_app.shared.generated.resources.topic_detail_back
import kmp_learning_app.shared.generated.resources.topic_detail_available_questions
import kmp_learning_app.shared.generated.resources.topic_detail_no_questions
import kmp_learning_app.shared.generated.resources.topic_detail_not_found
import kmp_learning_app.shared.generated.resources.topic_detail_start_practice
import kmp_learning_app.shared.generated.resources.topic_detail_subtopics
import kmp_learning_app.shared.generated.resources.topic_browser_error
import org.jetbrains.compose.resources.stringResource

internal const val TopicDetailLoadingTag = "topic_detail_loading"
internal const val TopicPracticeButtonTag = "topic_practice_button"
internal const val SubtopicPracticeButtonTag = "subtopic_practice_button"

@Composable
internal fun TopicDetailScreen(
    state: TopicDetailUiState,
    onBack: () -> Unit,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TopicDetailUiState.Loading -> {
            CenteredState(modifier) {
                CircularProgressIndicator(modifier = Modifier.testTag(TopicDetailLoadingTag))
            }
        }

        is TopicDetailUiState.Content -> {
            TopicContent(
                state = state,
                onBack = onBack,
                onStartTopicPractice = onStartTopicPractice,
                onStartSubtopicPractice = onStartSubtopicPractice,
                modifier = modifier,
            )
        }

        is TopicDetailUiState.NoQuestions -> {
            CenteredState(modifier) {
                Text(text = state.topic.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(Res.string.topic_detail_no_questions),
                    modifier = Modifier.padding(top = 12.dp),
                )
                BackButton(onBack)
            }
        }

        TopicDetailUiState.NotFound -> {
            CenteredState(modifier) {
                Text(text = stringResource(Res.string.topic_detail_not_found))
                BackButton(onBack)
            }
        }

        TopicDetailUiState.Error -> {
            CenteredState(modifier) {
                Text(text = stringResource(Res.string.topic_browser_error))
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(Res.string.topic_browser_retry))
                }
                BackButton(onBack)
            }
        }
    }
}

@Composable
private fun TopicContent(
    state: TopicDetailUiState.Content,
    onBack: () -> Unit,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BackButton(onBack)
            Text(
                text = state.topic.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    Res.string.topic_detail_available_questions,
                    state.topicQuestionCount,
                ),
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onStartTopicPractice,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag(TopicPracticeButtonTag),
            ) {
                Text(text = stringResource(Res.string.topic_detail_start_practice))
            }
            Text(
                text = stringResource(Res.string.topic_detail_subtopics),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        items(
            items = state.subtopics,
            key = { it.subtopic.id },
        ) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartSubtopicPractice(item.subtopic.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.subtopic.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(
                                Res.string.topic_detail_available_questions,
                                item.questionCount,
                            ),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Button(
                        onClick = { onStartSubtopicPractice(item.subtopic.id) },
                        modifier = Modifier.testTag(SubtopicPracticeButtonTag),
                    ) {
                        Text(text = stringResource(Res.string.topic_detail_start_practice))
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredState(
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

@Composable
private fun BackButton(onBack: () -> Unit) {
    Button(onClick = onBack) {
        Text(text = stringResource(Res.string.topic_detail_back))
    }
}

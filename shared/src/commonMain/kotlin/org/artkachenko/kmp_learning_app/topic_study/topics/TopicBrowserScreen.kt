package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mixed_interview_description
import kmp_learning_app.shared.generated.resources.mixed_interview_question_count
import kmp_learning_app.shared.generated.resources.mixed_interview_start
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import kmp_learning_app.shared.generated.resources.progress_entry
import kmp_learning_app.shared.generated.resources.topic_browser_empty
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_browser_heading
import kmp_learning_app.shared.generated.resources.topic_browser_loading
import kmp_learning_app.shared.generated.resources.topic_browser_title
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewDefaults
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

internal const val TopicBrowserLoadingTag = "topic_browser_loading"

@Composable
internal fun TopicBrowserScreen(
    state: TopicBrowserUiState,
    onTopicClick: (String) -> Unit,
    onStartMixedInterview: () -> Unit,
    onOpenProgress: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(Res.string.topic_browser_heading),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenProgress,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.progress_entry))
        }
        Spacer(modifier = Modifier.height(12.dp))

        MixedInterviewEntry(onStart = onStartMixedInterview)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.topic_browser_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                TopicBrowserUiState.Loading -> ScreenLoading(
                    message = stringResource(Res.string.topic_browser_loading),
                    testTag = TopicBrowserLoadingTag,
                )
                is TopicBrowserUiState.Content -> TopicList(
                    topics = state.topics,
                    onTopicClick = onTopicClick,
                )
                TopicBrowserUiState.Empty -> ScreenMessage(
                    message = stringResource(Res.string.topic_browser_empty),
                )
                TopicBrowserUiState.Error -> ScreenError(
                    message = stringResource(Res.string.topic_browser_error),
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun MixedInterviewEntry(
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.mixed_interview_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.mixed_interview_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(
                    Res.string.mixed_interview_question_count,
                    MixedInterviewDefaults.QuestionCount,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(Res.string.mixed_interview_start))
            }
        }
    }
}

@Composable
private fun TopicList(
    topics: List<Topic>,
    onTopicClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = topics,
            key = { it.id },
        ) { topic ->
            TopicRow(
                topic = topic,
                onTopicClick = onTopicClick,
            )
        }
    }
}

@Composable
private fun TopicRow(
    topic: Topic,
    onTopicClick: (String) -> Unit,
) {
    // The container used to be `surface`, which is the same colour as the screen background,
    // with a filled card's 0dp elevation and no outline - so the rows read as one flat block.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onTopicClick(topic.id)
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun TopicBrowserScreenPreview() {
    AppTheme {
        TopicBrowserScreen(
            state = TopicBrowserUiState.Content(
                topics = listOf(
                    Topic("android_platform", "Android platform"),
                    Topic("ui_compose", "UI and Compose"),
                    Topic("architecture", "Architecture"),
                ),
            ),
            onTopicClick = {},
            onStartMixedInterview = {},
            onOpenProgress = {},
            onRetry = {},
        )
    }
}

package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.topic_browser_empty
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_browser_loading
import kmp_learning_app.shared.generated.resources.topic_browser_retry
import kmp_learning_app.shared.generated.resources.topic_browser_title
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.jetbrains.compose.resources.stringResource

internal const val TopicBrowserLoadingTag = "topic_browser_loading"

@Composable
internal fun TopicBrowserScreen(
    state: TopicBrowserUiState,
    onTopicClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(Res.string.topic_browser_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(20.dp))

        when (state) {
            TopicBrowserUiState.Loading -> LoadingState()
            is TopicBrowserUiState.Content -> TopicList(
                topics = state.topics,
                onTopicClick = onTopicClick,
            )
            TopicBrowserUiState.Empty -> MessageState(
                text = stringResource(Res.string.topic_browser_empty),
            )
            TopicBrowserUiState.Error -> ErrorState(onRetry = onRetry)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onTopicClick(topic.id)
            },
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
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

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TopicBrowserLoadingTag),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.topic_browser_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun MessageState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.topic_browser_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(Res.string.topic_browser_retry))
            }
        }
    }
}

@Preview
@Composable
private fun TopicBrowserScreenPreview() {
    MaterialTheme {
        TopicBrowserScreen(
            state = TopicBrowserUiState.Content(
                topics = listOf(
                    Topic("android_platform", "Android platform"),
                    Topic("ui_compose", "UI and Compose"),
                    Topic("architecture", "Architecture"),
                ),
            ),
            onTopicClick = {},
            onRetry = {},
        )
    }
}

package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import kmp_learning_app.shared.generated.resources.topic_browser_empty
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_browser_loading
import kmp_learning_app.shared.generated.resources.topic_browser_subtitle
import kmp_learning_app.shared.generated.resources.topic_browser_title
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.mixed_interview.MixedInterviewDefaults
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

internal const val TopicBrowserLoadingTag = "topic_browser_loading"
internal const val TopicBrowserHeaderTag = "topic_browser_header"
internal const val TopicBrowserViewportTag = "topic_browser_viewport"

/**
 * Space between the top safe area and the heading.
 *
 * Design spacing only: the status-bar allowance is the window inset applied above it. The two are
 * deliberately separate and additive. This was 24.dp back when the screen applied no inset and the
 * heading ran under the status bar, so the value stood in for both; once the inset was added the
 * old margin stacked on top of a safe area that is 54.dp on a current phone, leaving the heading
 * most of an inch down the screen.
 */
private val TopicBrowserHeaderSpacing = 12.dp

@Composable
internal fun TopicBrowserScreen(
    state: TopicBrowserUiState,
    onTopicClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    topWindowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // This screen carries its own heading instead of an AppTopBar, so it owns the top safe
            // area; the shell leaves that inset unconsumed for exactly this reason. The bottom is
            // not ours: the shell's Scaffold already ends this content at the top of the navigation
            // bar, so bottom padding out here would show as a strip of background above it. Any
            // scroll-end spacing belongs inside the list, as contentPadding.
            .windowInsetsPadding(topWindowInsets)
            .padding(horizontal = 20.dp)
            .padding(top = TopicBrowserHeaderSpacing),
    ) {
        Text(
            text = stringResource(Res.string.topic_browser_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(TopicBrowserHeaderTag),
        )
        Text(
            text = stringResource(Res.string.topic_browser_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).testTag(TopicBrowserViewportTag)) {
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
            onRetry = {},
        )
    }
}

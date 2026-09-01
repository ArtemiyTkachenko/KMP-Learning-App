package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.progress_subtopic_unavailable
import kmp_learning_app.shared.generated.resources.progress_topic_detail_title
import kmp_learning_app.shared.generated.resources.progress_topic_empty
import kmp_learning_app.shared.generated.resources.progress_topic_error
import kmp_learning_app.shared.generated.resources.progress_topic_loading
import kmp_learning_app.shared.generated.resources.progress_topic_subtopics
import kmp_learning_app.shared.generated.resources.progress_topic_unavailable
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.PaddingValues
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage

internal const val ProgressTopicLoadingTag = "progress_topic_loading"

@Composable
internal fun ProgressTopicScreen(
    state: ProgressTopicUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // The topic name is the aggregate card's title, so the bar keeps a stable label rather
        // than repeating it.
        AppTopBar(stringResource(Res.string.progress_topic_detail_title), onBack)
        when (state) {
            ProgressTopicUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.progress_topic_loading),
                testTag = ProgressTopicLoadingTag,
                modifier = Modifier.weight(1f),
            )
            ProgressTopicUiState.Empty -> ScreenMessage(
                message = stringResource(Res.string.progress_topic_empty),
                modifier = Modifier.weight(1f),
            )
            ProgressTopicUiState.Error -> ScreenError(
                message = stringResource(Res.string.progress_topic_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is ProgressTopicUiState.Content -> ProgressTopicContent(
                state = state,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProgressTopicContent(
    state: ProgressTopicUiState.Content,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProgressPerformanceCard(
                title = state.topicName ?: stringResource(Res.string.progress_topic_unavailable),
                subtitle = null,
                correctCount = state.correctCount,
                answeredCount = state.answeredCount,
                percentage = state.percentage,
                isWeak = state.isWeak,
                isSummary = true,
            )
        }
        // Only Subtopics with completed observations reach the snapshot, so an observed Topic can
        // still have no Subtopic rows. Omit the heading rather than leaving it dangling.
        if (state.subtopics.isNotEmpty()) {
            item {
                ProgressSectionTitle(stringResource(Res.string.progress_topic_subtopics))
            }
            items(state.subtopics, key = ProgressSubtopicUiModel::subtopicId) { subtopic ->
                ProgressPerformanceCard(
                    title = subtopic.subtopicName
                        ?: stringResource(Res.string.progress_subtopic_unavailable),
                    subtitle = null,
                    correctCount = subtopic.correctCount,
                    answeredCount = subtopic.answeredCount,
                    percentage = subtopic.percentage,
                    isWeak = subtopic.isWeak,
                )
            }
        }
    }
}

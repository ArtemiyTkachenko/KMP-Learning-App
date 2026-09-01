package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TopicBrowserDestination(
    onTopicClick: (String) -> Unit,
    onSubtopicClick: (topicId: String, subtopicId: String) -> Unit,
    viewModel: TopicBrowserViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TopicBrowserScreen(
        state = state,
        onTopicClick = onTopicClick,
        onSubtopicClick = onSubtopicClick,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onRetry = viewModel::retry,
    )
}

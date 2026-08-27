package org.artkachenko.kmp_learning_app.topic_study.topics

import org.artkachenko.kmp_learning_app.curriculum.Topic

internal sealed interface TopicBrowserUiState {
    data object Loading : TopicBrowserUiState

    data class Content(
        val topics: List<Topic>,
    ) : TopicBrowserUiState

    data object Empty : TopicBrowserUiState

    data object Error : TopicBrowserUiState
}

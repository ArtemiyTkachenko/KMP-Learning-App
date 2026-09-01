package org.artkachenko.kmp_learning_app.topic_study.topics

import org.artkachenko.kmp_learning_app.curriculum.Topic

internal sealed interface TopicBrowserUiState {
    data object Loading : TopicBrowserUiState

    data class Content(
        val topics: List<Topic>,
        val searchableSubtopics: List<SubtopicSearchResult> = emptyList(),
        val query: String = "",
        val topicMatches: List<TopicSearchResult> = emptyList(),
        val subtopicMatches: List<SubtopicSearchResult> = emptyList(),
    ) : TopicBrowserUiState

    data object Empty : TopicBrowserUiState

    data object Error : TopicBrowserUiState
}

internal data class TopicSearchResult(
    val topicId: String,
    val topicName: String,
)

internal data class SubtopicSearchResult(
    val subtopicId: String,
    val subtopicName: String,
    val parentTopicId: String,
    val parentTopicName: String,
)

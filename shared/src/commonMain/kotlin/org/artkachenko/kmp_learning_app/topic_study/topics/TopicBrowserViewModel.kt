package org.artkachenko.kmp_learning_app.topic_study.topics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class TopicBrowserViewModel(
    private val curriculumRepository: CurriculumRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopicBrowserUiState>(TopicBrowserUiState.Loading)
    val uiState: StateFlow<TopicBrowserUiState> = _uiState.asStateFlow()

    fun retry() {
        loadCatalog()
    }

    fun onSearchQueryChange(query: String) {
        val content = _uiState.value as? TopicBrowserUiState.Content ?: return
        _uiState.value = content.withQuery(query)
    }

    private fun loadCatalog() {
        _uiState.value = TopicBrowserUiState.Loading
        viewModelScope.launch {
            _uiState.value = runCatching {
                val topics = curriculumRepository.getActiveTopics()
                if (topics.isEmpty()) {
                    TopicBrowserUiState.Empty
                } else {
                    val searchableSubtopics = topics.flatMap { topic ->
                        curriculumRepository.getActiveSubtopics(topic.id).map { subtopic ->
                            SubtopicSearchResult(
                                subtopicId = subtopic.id,
                                subtopicName = subtopic.name,
                                parentTopicId = topic.id,
                                parentTopicName = topic.name,
                            )
                        }
                    }
                    TopicBrowserUiState.Content(
                        topics = topics,
                        searchableSubtopics = searchableSubtopics,
                    )
                }
            }.getOrElse {
                TopicBrowserUiState.Error
            }
        }
    }

    init {
        loadCatalog()
    }
}

private fun TopicBrowserUiState.Content.withQuery(query: String): TopicBrowserUiState.Content {
    val tokens = query.searchTokens()
    if (tokens.isEmpty()) {
        return copy(
            query = query,
            topicMatches = emptyList(),
            subtopicMatches = emptyList(),
        )
    }
    return copy(
        query = query,
        topicMatches = topics
            .filter { it.name.matchesAll(tokens) }
            .map { TopicSearchResult(topicId = it.id, topicName = it.name) },
        subtopicMatches = searchableSubtopics.filter { it.subtopicName.matchesAll(tokens) },
    )
}

private fun String.searchTokens(): List<String> =
    trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }

private fun String.matchesAll(tokens: List<String>): Boolean {
    val candidate = lowercase()
    return tokens.all(candidate::contains)
}

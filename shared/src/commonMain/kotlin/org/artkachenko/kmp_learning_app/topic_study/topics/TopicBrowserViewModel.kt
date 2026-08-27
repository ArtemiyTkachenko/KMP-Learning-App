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

    init {
        loadTopics()
    }

    fun retry() {
        loadTopics()
    }

    private fun loadTopics() {
        _uiState.value = TopicBrowserUiState.Loading
        viewModelScope.launch {
            _uiState.value = runCatching {
                val topics = curriculumRepository.getActiveTopics()
                if (topics.isEmpty()) {
                    TopicBrowserUiState.Empty
                } else {
                    TopicBrowserUiState.Content(topics)
                }
            }.getOrElse {
                TopicBrowserUiState.Error
            }
        }
    }
}

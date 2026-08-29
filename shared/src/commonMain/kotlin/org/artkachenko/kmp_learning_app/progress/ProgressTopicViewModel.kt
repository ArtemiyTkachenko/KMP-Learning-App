package org.artkachenko.kmp_learning_app.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.learning_progress.SubtopicPerformance

internal class ProgressTopicViewModel(
    private val topicId: String,
    private val learningProgressService: LearningProgressService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProgressTopicUiState>(ProgressTopicUiState.Loading)
    val uiState: StateFlow<ProgressTopicUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        require(topicId.isNotBlank()) { "topicId must not be blank." }
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = ProgressTopicUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = loadState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.value = ProgressTopicUiState.Error
            }
        }
    }

    private suspend fun loadState(): ProgressTopicUiState {
        val snapshot = learningProgressService.load()
        // The snapshot already carries observation-based performance in a deterministic order.
        // Selecting from it keeps this screen consistent with the dashboard and avoids
        // recalculating anything from the current ACTIVE curriculum.
        val topic = snapshot.topics.firstOrNull { it.topicId == topicId }
            ?: return ProgressTopicUiState.Empty

        return ProgressTopicUiState.Content(
            topicName = topic.topicName,
            answeredCount = topic.answeredCount,
            correctCount = topic.correctCount,
            percentage = topic.percentage,
            isWeak = topic.isWeak,
            subtopics = snapshot.subtopics
                .filter { it.topicId == topicId }
                .map(::toUiModel),
        )
    }

    private fun toUiModel(subtopic: SubtopicPerformance): ProgressSubtopicUiModel =
        ProgressSubtopicUiModel(
            subtopicId = subtopic.subtopicId,
            subtopicName = subtopic.subtopicName,
            answeredCount = subtopic.answeredCount,
            correctCount = subtopic.correctCount,
            percentage = subtopic.percentage,
            isWeak = subtopic.isWeak,
        )
}

package org.artkachenko.kmp_learning_app.progress

internal sealed interface ProgressTopicUiState {
    data object Loading : ProgressTopicUiState

    data object Empty : ProgressTopicUiState

    data object Error : ProgressTopicUiState

    data class Content(
        val topicName: String?,
        val answeredCount: Int,
        val correctCount: Int,
        val percentage: Double,
        val isWeak: Boolean,
        val subtopics: List<ProgressSubtopicUiModel>,
    ) : ProgressTopicUiState
}

internal data class ProgressSubtopicUiModel(
    val subtopicId: String,
    val subtopicName: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
    val isWeak: Boolean,
)

package org.artkachenko.kmp_learning_app.progress

internal sealed interface ProgressUiState {
    data object Loading : ProgressUiState

    data object Empty : ProgressUiState

    data object Error : ProgressUiState

    data class Content(
        val completedAttemptCount: Int,
        val answeredQuestionCount: Int,
        val correctAnswerCount: Int,
        val percentage: Double,
        val weakAreas: List<WeakAreaUiModel>,
        val topics: List<ProgressTopicUiModel>,
        val history: List<CompletedAttemptUiModel>,
    ) : ProgressUiState
}

internal enum class WeakAreaType {
    TOPIC,
    SUBTOPIC,
}

internal data class WeakAreaUiModel(
    val type: WeakAreaType,
    val stableId: String,
    val title: String?,
    val subtitle: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
)

internal data class ProgressTopicUiModel(
    val topicId: String,
    val topicName: String?,
    val answeredCount: Int,
    val correctCount: Int,
    val percentage: Double,
)

internal enum class CompletedAssessmentType {
    FOCUSED,
    MIXED,
}

internal sealed interface FocusedScopeUiModel {
    data class Topic(
        val topicName: String?,
    ) : FocusedScopeUiModel

    data class Subtopic(
        val topicName: String?,
        val subtopicName: String?,
    ) : FocusedScopeUiModel
}

internal data class CompletedAttemptUiModel(
    val attemptId: String,
    val assessmentType: CompletedAssessmentType,
    val focusedScope: FocusedScopeUiModel?,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val percentage: Double,
    val completedAtText: String,
)

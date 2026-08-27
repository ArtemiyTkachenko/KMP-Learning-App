package org.artkachenko.kmp_learning_app.topic_study.focused_result

internal sealed interface FocusedResultUiState {
    data object Loading : FocusedResultUiState
    data object AttemptNotFound : FocusedResultUiState
    data object NotCompleted : FocusedResultUiState
    data object Error : FocusedResultUiState

    data class Content(
        val attemptId: String,
        val totalQuestions: Int,
        val correctAnswers: Int,
        val percentage: Double,
        val questions: List<ReviewQuestionItem>,
    ) : FocusedResultUiState
}

internal sealed interface ReviewQuestionItem {
    data class Available(val question: ReviewQuestionUiModel) : ReviewQuestionItem
    data class Missing(val questionId: String) : ReviewQuestionItem
}

internal data class ReviewQuestionUiModel(
    val questionId: String,
    val text: String,
    val isCorrect: Boolean,
    val answers: List<ReviewAnswerUiModel>,
    val explanation: String,
    val sources: List<ReviewSourceUiModel>,
)

internal data class ReviewAnswerUiModel(
    val id: String,
    val text: String,
    val wasSelected: Boolean,
    val isCorrectAnswer: Boolean,
)

internal data class ReviewSourceUiModel(
    val title: String,
    val url: String,
)

package org.artkachenko.kmp_learning_app.topic_study.focused_practice

internal sealed interface FocusedPracticeUiState {
    data object Loading : FocusedPracticeUiState

    data object NoQuestions : FocusedPracticeUiState

    data object Error : FocusedPracticeUiState

    data class Content(
        val attemptId: String,
        val questionNumber: Int,
        val totalQuestions: Int,
        val question: FocusedQuestionUiModel,
        val selectedAnswerIds: Set<String>,
        val canSubmit: Boolean,
        val isSubmitting: Boolean,
        val submissionFailed: Boolean,
    ) : FocusedPracticeUiState

    data class ReadyToComplete(
        val attemptId: String,
        val totalQuestions: Int,
        val isCompleting: Boolean = false,
        val completionFailed: Boolean = false,
    ) : FocusedPracticeUiState

    data class CompletionSucceeded(
        val attemptId: String,
    ) : FocusedPracticeUiState
}

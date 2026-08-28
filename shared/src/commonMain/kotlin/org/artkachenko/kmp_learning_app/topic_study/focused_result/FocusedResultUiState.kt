package org.artkachenko.kmp_learning_app.topic_study.focused_result

import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem

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
        val repeatPracticeState: RepeatPracticeState = RepeatPracticeState.Idle,
    ) : FocusedResultUiState
}

internal sealed interface RepeatPracticeState {
    data object Idle : RepeatPracticeState
    data object Creating : RepeatPracticeState
    data object SourceAttemptNotFound : RepeatPracticeState
    data object NoEligibleQuestions : RepeatPracticeState
    data object Error : RepeatPracticeState
}

internal sealed interface FocusedResultEvent {
    data class RetakeCreated(val attemptId: String) : FocusedResultEvent
}

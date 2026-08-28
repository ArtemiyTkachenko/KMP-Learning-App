package org.artkachenko.kmp_learning_app.mixed_interview

import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem

internal sealed interface MixedInterviewResultUiState {
    data object Loading : MixedInterviewResultUiState
    data object AttemptNotFound : MixedInterviewResultUiState
    data object NotCompleted : MixedInterviewResultUiState
    data object Error : MixedInterviewResultUiState

    data class Content(
        val attemptId: String,
        val totalQuestions: Int,
        val correctAnswers: Int,
        val percentage: Double,
        val topicPerformance: List<TopicPerformanceUiModel>,
        val questions: List<ReviewQuestionItem>,
        val repeatInterviewState: RepeatInterviewState = RepeatInterviewState.Idle,
    ) : MixedInterviewResultUiState
}

internal sealed interface RepeatInterviewState {
    data object Idle : RepeatInterviewState
    data object Creating : RepeatInterviewState
    data object SourceAttemptNotFound : RepeatInterviewState
    data object NoEligibleQuestions : RepeatInterviewState
    data object Error : RepeatInterviewState
}

internal sealed interface MixedInterviewResultEvent {
    data class RetakeCreated(val attemptId: String) : MixedInterviewResultEvent
}

internal data class TopicPerformanceUiModel(
    val topicId: String,
    val topicName: String?,
    val questionCount: Int,
    val correctCount: Int,
    val percentage: Double,
)

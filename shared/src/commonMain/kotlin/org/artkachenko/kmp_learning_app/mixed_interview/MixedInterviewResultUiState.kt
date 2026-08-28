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
    ) : MixedInterviewResultUiState
}

internal data class TopicPerformanceUiModel(
    val topicId: String,
    val topicName: String?,
    val questionCount: Int,
    val correctCount: Int,
    val percentage: Double,
)

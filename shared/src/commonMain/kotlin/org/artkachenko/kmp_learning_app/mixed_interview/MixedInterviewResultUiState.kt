package org.artkachenko.kmp_learning_app.mixed_interview

import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem

/**
 * What one completed Mixed interview was.
 *
 * Settled facts only, exactly as the Focused result holds them. Taking the interview again is
 * [org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState], observed beside this
 * one and owned by the shared retake controller rather than nested in [Content].
 */
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

/**
 * One Topic's share of the interview, named.
 *
 * The counts come from [topicAnswerCounts], which derives them from the transcript alone;
 * [topicName] is the one thing that needs a curriculum read, and is null when the Topic no longer
 * resolves — the run still happened, so the row stays and only its name is absent.
 */
internal data class TopicPerformanceUiModel(
    val topicId: String,
    val topicName: String?,
    val questionCount: Int,
    val correctCount: Int,
    val percentage: Double,
)

package org.artkachenko.kmp_learning_app.topic_study.focused_result

import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem

/**
 * What one completed practice attempt was.
 *
 * Settled facts only. What the learner is doing *about* the result — taking it again — is
 * [org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState], observed beside this
 * one rather than nested inside [Content]: it belongs to a different owner, it changes while these
 * figures cannot, and keeping it here meant every retake transition had to be written as a cast
 * that silently dropped itself if the result were not loaded.
 */
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

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

    /**
     * The attempt exists, but every Question in it belongs to a Topic the learner's visibility
     * hides. Not [AttemptNotFound]: nothing is gone, and it returns when that content is shown.
     */
    data object Unavailable : FocusedResultUiState

    /**
     * The attempt as the current visibility projects it: [totalQuestions], [correctAnswers] and
     * [questions] cover visible Questions only, and [hiddenQuestionCount] says how many stored
     * answers the projection left out, so a score that differs from the stored total is explained.
     */
    data class Content(
        val attemptId: String,
        val totalQuestions: Int,
        val correctAnswers: Int,
        val percentage: Double,
        val questions: List<ReviewQuestionItem>,
        val hiddenQuestionCount: Int = 0,
    ) : FocusedResultUiState
}

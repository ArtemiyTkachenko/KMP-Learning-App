package org.artkachenko.kmp_learning_app.assessment_taking

internal sealed interface AssessmentTakingUiState {
    data object Loading : AssessmentTakingUiState

    data object Error : AssessmentTakingUiState

    /**
     * A valid persisted attempt that contains content the learner's visibility currently hides.
     * Distinct from [Error] (nothing failed, so there is no Retry) and from a missing attempt: it becomes loadable again, unchanged, once that content is shown.
     */
    data object Unavailable : AssessmentTakingUiState

    data class Content(
        val attemptId: String,
        val questionNumber: Int,
        val totalQuestions: Int,
        val question: AssessmentQuestionUiModel,
        val selectedAnswerIds: Set<String>,
        val canSubmit: Boolean,
        val isSubmitting: Boolean,
        val submissionFailed: Boolean,
        val feedback: PracticeFeedback? = null,
    ) : AssessmentTakingUiState

    data class ReadyToComplete(
        val attemptId: String,
        val totalQuestions: Int,
        val isCompleting: Boolean = false,
        val completionFailed: Boolean = false,
    ) : AssessmentTakingUiState

    data class CompletionSucceeded(
        val attemptId: String,
    ) : AssessmentTakingUiState
}

/** Feedback is only populated for formative Focused practice, never for an Interview. */
internal data class PracticeFeedback(val isCorrect: Boolean)

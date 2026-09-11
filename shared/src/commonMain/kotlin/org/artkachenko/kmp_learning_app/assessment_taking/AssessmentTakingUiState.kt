package org.artkachenko.kmp_learning_app.assessment_taking

internal sealed interface AssessmentTakingUiState {
    data object Loading : AssessmentTakingUiState

    data object NoQuestions : AssessmentTakingUiState

    data object Error : AssessmentTakingUiState

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

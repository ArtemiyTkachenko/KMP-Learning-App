package org.artkachenko.kmp_learning_app.assessment

internal sealed interface QuestionAnswerState {
    data object Unanswered : QuestionAnswerState

    data class Answered(
        val selectedAnswerIds: Set<String>,
        val isCorrect: Boolean,
    ) : QuestionAnswerState {
        init {
            require(selectedAnswerIds.isNotEmpty()) {
                "selectedAnswerIds must not be empty."
            }
            require(selectedAnswerIds.none { it.isBlank() }) {
                "selectedAnswerIds must not contain blank IDs."
            }
        }
    }
}

package org.artkachenko.kmp_learning_app.assessment

internal data class QuestionAttempt(
    val questionId: String,
    val answerState: QuestionAnswerState = QuestionAnswerState.Unanswered,
) {
    init {
        require(questionId.isNotBlank()) {
            "questionId must not be blank."
        }
    }
}

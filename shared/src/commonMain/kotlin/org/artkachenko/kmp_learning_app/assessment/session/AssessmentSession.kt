package org.artkachenko.kmp_learning_app.assessment.session

import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.Question

internal data class AssessmentSession(
    val attempt: TestAttempt,
    val questions: List<Question>,
) {
    init {
        require(questions.isNotEmpty()) {
            "questions must not be empty."
        }
        require(questions.map { it.id }.toSet().size == questions.size) {
            "questions must not contain duplicate IDs."
        }
        require(questions.map { it.id } == attempt.questionAttempts.map { it.questionId }) {
            "questions must match attempt questionAttempts in the same order."
        }
    }
}

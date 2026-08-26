package org.artkachenko.kmp_learning_app.assessment

import kotlin.time.Instant

internal data class TestAttempt(
    val id: String,
    val config: AssessmentConfig,
    val questionAttempts: List<QuestionAttempt>,
    val status: AssessmentStatus,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val score: AssessmentScore? = null,
) {
    init {
        require(id.isNotBlank()) {
            "id must not be blank."
        }
        require(questionAttempts.isNotEmpty()) {
            "questionAttempts must not be empty."
        }
        require(questionAttempts.map { it.questionId }.toSet().size == questionAttempts.size) {
            "questionAttempts must not contain duplicate question IDs."
        }

        when (status) {
            AssessmentStatus.IN_PROGRESS -> {
                require(score == null) {
                    "IN_PROGRESS attempts must not have a score."
                }
                require(completedAt == null) {
                    "IN_PROGRESS attempts must not have a completedAt timestamp."
                }
            }
            AssessmentStatus.COMPLETED -> {
                require(questionAttempts.all { it.answerState is QuestionAnswerState.Answered }) {
                    "COMPLETED attempts must have answered question attempts only."
                }
                require(score != null) {
                    "COMPLETED attempts must have a score."
                }
                require(completedAt != null) {
                    "COMPLETED attempts must have a completedAt timestamp."
                }
                require(completedAt >= startedAt) {
                    "COMPLETED attempt completedAt must not be before startedAt."
                }
                require(score.totalQuestions == questionAttempts.size) {
                    "COMPLETED attempt score totalQuestions must match questionAttempts size."
                }
            }
        }
    }
}

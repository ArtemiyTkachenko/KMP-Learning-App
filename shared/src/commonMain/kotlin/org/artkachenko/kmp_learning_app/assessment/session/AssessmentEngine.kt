package org.artkachenko.kmp_learning_app.assessment.session

import kotlin.uuid.Uuid
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector

internal class AssessmentEngine(
    private val questionSelector: AssessmentQuestionSelector,
    private val generateAttemptId: () -> String = { Uuid.random().toString() },
) {
    suspend fun start(config: AssessmentConfig): AssessmentStartResult {
        val questions = questionSelector.select(config)
        if (questions.isEmpty()) return AssessmentStartResult.NoEligibleQuestions

        val attempt = TestAttempt(
            id = generateAttemptId(),
            config = config,
            questionAttempts = questions.map {
                QuestionAttempt(questionId = it.id)
            },
            status = AssessmentStatus.IN_PROGRESS,
        )

        return AssessmentStartResult.Started(
            session = AssessmentSession(
                attempt = attempt,
                questions = questions,
            ),
        )
    }

    fun submitAnswer(
        session: AssessmentSession,
        questionId: String,
        selectedAnswerIds: Collection<String>,
    ): AssessmentSession {
        check(session.attempt.status == AssessmentStatus.IN_PROGRESS) {
            "Cannot submit answers to a completed assessment."
        }
        require(questionId.isNotBlank()) {
            "questionId must not be blank."
        }

        val questionIndex = session.questions.indexOfFirst { it.id == questionId }
        require(questionIndex >= 0) {
            "Question $questionId does not belong to this assessment session."
        }

        val questionAttempt = session.attempt.questionAttempts[questionIndex]
        check(questionAttempt.answerState == QuestionAnswerState.Unanswered) {
            "Question $questionId has already been answered."
        }

        val selectedIds = selectedAnswerIds.toSet()
        require(selectedIds.isNotEmpty()) {
            "selectedAnswerIds must not be empty."
        }

        val question = session.questions[questionIndex]
        val answerIds = question.answers.map { it.id }.toSet()
        val unknownAnswerIds = selectedIds - answerIds
        require(unknownAnswerIds.isEmpty()) {
            "Selected answer IDs do not belong to question $questionId: ${unknownAnswerIds.joinToString()}."
        }

        val updatedAnswerState = QuestionAnswerState.Answered(
            selectedAnswerIds = selectedIds,
            isCorrect = selectedIds == question.correctAnswerIds.toSet(),
        )
        val updatedQuestionAttempts =
            session.attempt.questionAttempts.mapIndexed { index, attempt ->
                if (index == questionIndex) {
                    attempt.copy(answerState = updatedAnswerState)
                } else {
                    attempt
                }
            }

        return session.copy(
            attempt = session.attempt.copy(
                questionAttempts = updatedQuestionAttempts,
                status = AssessmentStatus.IN_PROGRESS,
                score = null,
            ),
        )
    }

    fun canComplete(session: AssessmentSession): Boolean =
        session.attempt.status == AssessmentStatus.IN_PROGRESS &&
            session.attempt.questionAttempts.all {
                it.answerState is QuestionAnswerState.Answered
            }

    fun complete(session: AssessmentSession): AssessmentSession {
        check(session.attempt.status == AssessmentStatus.IN_PROGRESS) {
            "Assessment is already completed."
        }
        check(canComplete(session)) {
            "Assessment cannot be completed until every question is answered."
        }

        val correctAnswers =
            session.attempt.questionAttempts.count {
                (it.answerState as QuestionAnswerState.Answered).isCorrect
            }
        val score = AssessmentScore(
            totalQuestions = session.attempt.questionAttempts.size,
            correctAnswers = correctAnswers,
        )

        return session.copy(
            attempt = session.attempt.copy(
                status = AssessmentStatus.COMPLETED,
                score = score,
            ),
        )
    }
}

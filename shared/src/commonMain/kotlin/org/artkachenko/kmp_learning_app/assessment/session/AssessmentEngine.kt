package org.artkachenko.kmp_learning_app.assessment.session

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentSelectionResult

internal class AssessmentEngine(
    private val questionSelector: AssessmentQuestionSelector,
    private val generateAttemptId: () -> String = { Uuid.random().toString() },
    private val now: () -> Instant = { Clock.System.now() },
) {
    /**
     * Every configuration starts here, whichever selection policy produced its Questions.
     *
     * The typed no-content reasons stay at the selection boundary and collapse into
     * [AssessmentStartResult.NoEligibleQuestions] on the way out: assessment taking has one
     * no-content state, and the Practice Builder reads availability from selection before it
     * ever asks for a start. What matters here is that no attempt is created or persisted for
     * a request that cannot be run.
     */
    suspend fun start(config: AssessmentConfig): AssessmentStartResult {
        val questions = when (val selection = questionSelector.select(config)) {
            is AssessmentSelectionResult.NoContent -> return AssessmentStartResult.NoEligibleQuestions
            is AssessmentSelectionResult.Selected -> selection.questions
        }

        val attempt = TestAttempt(
            id = generateAttemptId(),
            config = config,
            questionAttempts = questions.map {
                QuestionAttempt(questionId = it.id)
            },
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = now(),
        )

        return AssessmentStartResult.Started(
            session = AssessmentSession(
                attempt = attempt,
                // Question order stays as selected, because questionAttempts is matched to it by
                // index; only the answers within each Question are reordered.
                questions = questions.map { it.withAnswersOrderedFor(attempt.id) },
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
                completedAt = now(),
                score = score,
            ),
        )
    }
}

package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt

internal class UnresolvedMistakeDerivationTest {
    @Test
    fun latestIncorrectOccurrenceIsUnresolved() {
        val unresolved = UnresolvedMistakeDerivation.derive(
            listOf(completedAttempt("latest", "q1" to false)),
        )

        assertEquals(listOf("q1"), unresolved.map { it.questionId })
        assertEquals("latest", unresolved.single().sourceAttemptId)
    }

    @Test
    fun latestCorrectOccurrenceResolvesAnOlderMistake() {
        val unresolved = UnresolvedMistakeDerivation.derive(
            listOf(
                completedAttempt("latest", "q1" to true),
                completedAttempt("older", "q1" to false),
            ),
        )

        assertEquals(emptyList(), unresolved)
    }

    @Test
    fun incorrectThenCorrectThenIncorrectUsesTheNewestOccurrence() {
        val unresolved = UnresolvedMistakeDerivation.derive(
            listOf(
                completedAttempt("newest", "q1" to false),
                completedAttempt("middle", "q1" to true),
                completedAttempt("oldest", "q1" to false),
            ),
        )

        assertEquals(listOf("q1"), unresolved.map { it.questionId })
        assertEquals("newest", unresolved.single().sourceAttemptId)
    }

    @Test
    fun repeatedIncorrectOccurrencesProduceOneLatestStableId() {
        val unresolved = UnresolvedMistakeDerivation.derive(
            listOf(
                completedAttempt("newest", "q1" to false),
                completedAttempt("middle", "q1" to false),
                completedAttempt("oldest", "q1" to false),
            ),
        )

        assertEquals(listOf("q1"), unresolved.map { it.questionId })
        assertEquals("newest", unresolved.single().sourceAttemptId)
    }

    @Test
    fun persistedCorrectnessWinsOverTheSelectedAnswerIdentity() {
        val occurrence = completedAttempt(
            "persisted",
            "q1" to false,
            selectedAnswerSuffix = "currently_correct",
        )

        assertEquals(
            listOf("q1"),
            UnresolvedMistakeDerivation.derive(listOf(occurrence)).map { it.questionId },
        )
    }

    @Test
    fun inProgressOccurrencesNeitherCreateNorResolveMistakes() {
        val unresolved = UnresolvedMistakeDerivation.derive(
            listOf(
                inProgressAttempt("q1" to true, "q2" to false),
                completedAttempt("completed", "q1" to false),
            ),
        )

        assertEquals(listOf("q1"), unresolved.map { it.questionId })
    }

    @Test
    fun assessmentTypeAndRetakeOriginDoNotPartitionLatestOccurrences() {
        val focused = completedAttempt(
            "focused",
            "q1" to true,
            config = AssessmentConfig.Focused(AssessmentScope.Topic("kotlin"), 1),
        )
        val mixed = completedAttempt("mixed", "q1" to false)
        val retake = completedAttempt("retake", "q1" to false)

        assertEquals(emptyList(), UnresolvedMistakeDerivation.derive(listOf(focused, mixed)))
        assertEquals(
            listOf("q1"),
            UnresolvedMistakeDerivation.derive(listOf(retake, focused, mixed)).map { it.questionId },
        )
    }
}

private fun completedAttempt(
    id: String,
    vararg outcomes: Pair<String, Boolean>,
    config: AssessmentConfig = AssessmentConfig.Mixed(outcomes.size),
    selectedAnswerSuffix: String = "answer",
): TestAttempt = attempt(
    id = id,
    outcomes = outcomes,
    config = config,
    status = AssessmentStatus.COMPLETED,
    selectedAnswerSuffix = selectedAnswerSuffix,
)

private fun inProgressAttempt(vararg outcomes: Pair<String, Boolean>): TestAttempt = attempt(
    id = "in_progress",
    outcomes = outcomes,
    config = AssessmentConfig.Mixed(outcomes.size),
    status = AssessmentStatus.IN_PROGRESS,
)

private fun attempt(
    id: String,
    outcomes: Array<out Pair<String, Boolean>>,
    config: AssessmentConfig,
    status: AssessmentStatus,
    selectedAnswerSuffix: String = "answer",
): TestAttempt {
    val questionAttempts = outcomes.map { (questionId, isCorrect) ->
        QuestionAttempt(
            questionId = questionId,
            answerState = QuestionAnswerState.Answered(
                selectedAnswerIds = setOf("${questionId}_$selectedAnswerSuffix"),
                isCorrect = isCorrect,
            ),
        )
    }
    return TestAttempt(
        id = id,
        config = config,
        questionAttempts = questionAttempts,
        status = status,
        startedAt = Instant.fromEpochSeconds(0),
        completedAt = if (status == AssessmentStatus.COMPLETED) Instant.fromEpochSeconds(60) else null,
        score = if (status == AssessmentStatus.COMPLETED) {
            AssessmentScore(
                totalQuestions = questionAttempts.size,
                correctAnswers = outcomes.count { it.second },
            )
        } else {
            null
        },
    )
}

package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.learning_progress.MistakeReviewPolicy

/** Every instant is an offset from [Start], a 21:00 study session, so the clock is fully controlled. */
private val Start = Instant.parse("2026-01-05T21:00:00Z")

internal class MistakeScheduleDerivationTest {
    @Test
    fun policyValuesAreTheAgreedLadder() {
        assertEquals(listOf(1.days, 3.days, 7.days), MistakeReviewPolicy.Ladder)
        assertEquals(3, MistakeReviewPolicy.ResolveAfterCountedCorrect)
        assertEquals(4.hours, MistakeReviewPolicy.Grace)
    }

    @Test
    fun aWrongAnswerIsScheduledAndBecomesDueAboutTwentyHoursLater() {
        val mistake = derive(completed("wrong", at = 0.hours, "q1" to false)).single()

        assertEquals("q1", mistake.questionId)
        assertEquals(0, mistake.stage)
        assertEquals(Start + 1.days, mistake.dueAt)
        assertEquals("wrong", mistake.latestMistake.sourceAttemptId)
        assertFalse(mistake.isDue(Start + 19.hours + 59.minutes))
        assertTrue(mistake.isDue(Start + 20.hours))
        assertTrue(mistake.isDue(Start + 3.days))
    }

    @Test
    fun aQuestionNeverAnsweredWrongIsNeverScheduled() {
        assertEquals(emptyList(), derive(completed("right", at = 0.hours, "q1" to true)))
    }

    @Test
    fun threeCountedCorrectAnswersAtTheLadderGapsResolveTheMistake() {
        val wrong = completed("wrong", at = 0.hours, "q1" to false)
        val first = completed("first", at = 1.days, "q1" to true)
        val second = completed("second", at = 4.days, "q1" to true)
        val third = completed("third", at = 11.days, "q1" to true)

        val afterFirst = derive(first, wrong).single()
        assertEquals(1, afterFirst.stage)
        assertEquals(Start + 4.days, afterFirst.dueAt)
        // The review card keeps showing the wrong answer, not the counted correct one.
        assertEquals("wrong", afterFirst.latestMistake.sourceAttemptId)

        val afterSecond = derive(second, first, wrong).single()
        assertEquals(2, afterSecond.stage)
        assertEquals(Start + 11.days, afterSecond.dueAt)

        assertEquals(emptyList(), derive(third, second, first, wrong))
    }

    @Test
    fun correctAnswersBeforeTheDueTimeDoNotAdvanceTheSchedule() {
        val mistake = derive(
            // The same evening, the next morning, and a same-day re-practice: all early.
            completed("next_morning", at = 12.hours, "q1" to true),
            completed("re_practice_again", at = 3.minutes, "q1" to true),
            completed("re_practice", at = 2.minutes, "q1" to true),
            completed("wrong", at = 0.hours, "q1" to false),
        ).single()

        assertEquals(0, mistake.stage)
        assertEquals(Start + 1.days, mistake.dueAt)
    }

    @Test
    fun earlyAnswersAtALaterStageDoNotAdvanceItEither() {
        val mistake = derive(
            completed("early", at = 2.days, "q1" to true),
            completed("counted", at = 1.days, "q1" to true),
            completed("wrong", at = 0.hours, "q1" to false),
        ).single()

        assertEquals(1, mistake.stage)
        assertEquals(Start + 4.days, mistake.dueAt)
    }

    @Test
    fun aWrongAnswerAtAnyStageResetsToStageZero() {
        val afterStageOne = derive(
            completed("reset", at = 2.days, "q1" to false),
            completed("counted", at = 1.days, "q1" to true),
            completed("wrong", at = 0.hours, "q1" to false),
        ).single()
        assertEquals(0, afterStageOne.stage)
        assertEquals(Start + 3.days, afterStageOne.dueAt)
        assertEquals("reset", afterStageOne.latestMistake.sourceAttemptId)

        // A wrong answer before the item is due resets it too: "early" only protects correct ones.
        val earlyWrong = derive(
            completed("early_wrong", at = 5.days, "q1" to false),
            completed("second", at = 4.days, "q1" to true),
            completed("first", at = 1.days, "q1" to true),
            completed("wrong", at = 0.hours, "q1" to false),
        ).single()
        assertEquals(0, earlyWrong.stage)
        assertEquals(Start + 6.days, earlyWrong.dueAt)
    }

    @Test
    fun aResolvedQuestionAnsweredWrongAgainReentersAtStageZero() {
        val mistake = derive(
            completed("relapse", at = 30.days, "q1" to false),
            completed("third", at = 11.days, "q1" to true),
            completed("second", at = 4.days, "q1" to true),
            completed("first", at = 1.days, "q1" to true),
            completed("wrong", at = 0.hours, "q1" to false),
        ).single()

        assertEquals(0, mistake.stage)
        assertEquals(Start + 31.days, mistake.dueAt)
        assertEquals("relapse", mistake.latestMistake.sourceAttemptId)
    }

    @Test
    fun theGraceBoundaryCountsJustInsideAndNotJustOutside() {
        val wrong = completed("wrong", at = 0.hours, "q1" to false)
        val graceStart = 1.days - MistakeReviewPolicy.Grace

        val inside = derive(completed("inside", at = graceStart, "q1" to true), wrong).single()
        assertEquals(1, inside.stage)

        val outside = derive(completed("outside", at = graceStart - 1.seconds, "q1" to true), wrong)
            .single()
        assertEquals(0, outside.stage)

        val scheduled = derive(wrong).single()
        assertTrue(scheduled.isDue(Start + graceStart))
        assertFalse(scheduled.isDue(Start + graceStart - 1.seconds))
        assertEquals(Start + graceStart, scheduled.dueFrom)
    }

    @Test
    fun severalQuestionsInsideOneAttemptAreScheduledIndependently() {
        val schedule = derive(
            completed("later", at = 1.days, "q1" to true, "q3" to true),
            completed("session", at = 0.hours, "q1" to false, "q2" to true, "q3" to false),
        )

        assertEquals(setOf("q1", "q3"), schedule.map { it.questionId }.toSet())
        schedule.forEach { mistake ->
            assertEquals(1, mistake.stage)
            assertEquals("session", mistake.latestMistake.sourceAttemptId)
        }
    }

    @Test
    fun occurrencesAreReplayedByCompletionTimeWhateverTheInputOrder() {
        val wrong = completed("wrong", at = 0.hours, "q1" to false)
        val counted = completed("counted", at = 1.days, "q1" to true)

        // Oldest first, newest first, and shuffled must all replay wrong-then-correct.
        assertEquals(derive(counted, wrong), derive(wrong, counted))
        assertEquals(1, derive(wrong, counted).single().stage)

        // A later wrong answer listed before an earlier correct one still resets the Question.
        val relapse = completed("relapse", at = 2.days, "q1" to false)
        assertEquals(0, derive(counted, relapse, wrong).single().stage)
    }

    @Test
    fun attemptsCompletedAtTheSameInstantReplayInRepositoryOrder() {
        // Newest-first input with a tie: the repository's later entry is the older one, so it is
        // replayed first and the wrong answer listed first is the latest.
        val schedule = derive(
            completed("listed_first", at = 0.hours, "q1" to false),
            completed("listed_second", at = 0.hours, "q1" to false),
        )

        assertEquals("listed_first", schedule.single().latestMistake.sourceAttemptId)
    }

    @Test
    fun inProgressAttemptsNeitherScheduleNorAdvance() {
        val schedule = derive(
            inProgress("q1" to true, "q2" to false),
            completed("wrong", at = 0.hours, "q1" to false),
        )

        val mistake = schedule.single()
        assertEquals("q1", mistake.questionId)
        assertEquals(0, mistake.stage)
    }

    @Test
    fun persistedCorrectnessWinsOverTheSelectedAnswerIdentity() {
        val occurrence = completed(
            "persisted",
            at = 0.hours,
            "q1" to false,
            selectedAnswerSuffix = "currently_correct",
        )

        assertEquals(listOf("q1"), derive(occurrence).map { it.questionId })
    }

    @Test
    fun assessmentTypeAndRetakeOriginDoNotPartitionTheSchedule() {
        val focused = completed(
            "focused",
            at = 1.days,
            "q1" to true,
            config = AssessmentConfig.Focused(AssessmentScope.Topic("kotlin"), 1),
        )
        val mixed = completed("mixed", at = 0.hours, "q1" to false)

        assertEquals(1, derive(focused, mixed).single().stage)
    }

    @Test
    fun questionIdsUnknownToAnyCurriculumAreScheduledLikeAnyOther() {
        // The derivation never reads content, so a deprecated or deleted Question keeps its history.
        val mistake = derive(completed("old", at = 0.hours, "retired_question" to false)).single()

        assertEquals("retired_question", mistake.questionId)
    }

    @Test
    fun theScheduleIsOrderedSoonestDueFirst() {
        val schedule = derive(
            completed("newest_wrong", at = 2.days, "q_new" to false),
            completed("counted", at = 1.days, "q_old" to true),
            completed("oldest_wrong", at = 0.hours, "q_old" to false, "q_stale" to false),
        )

        // q_stale is due at day 1, q_new at day 3, q_old (stage 1) at day 4.
        assertEquals(listOf("q_stale", "q_new", "q_old"), schedule.map { it.questionId })
    }

    private fun derive(vararg attempts: TestAttempt) = MistakeScheduleDerivation.derive(attempts.toList())
}

private fun completed(
    id: String,
    at: Duration,
    vararg outcomes: Pair<String, Boolean>,
    config: AssessmentConfig = AssessmentConfig.Mixed(outcomes.size),
    selectedAnswerSuffix: String = "answer",
): TestAttempt = attempt(
    id = id,
    outcomes = outcomes,
    config = config,
    status = AssessmentStatus.COMPLETED,
    completedAt = Start + at,
    selectedAnswerSuffix = selectedAnswerSuffix,
)

private fun inProgress(vararg outcomes: Pair<String, Boolean>): TestAttempt = attempt(
    id = "in_progress",
    outcomes = outcomes,
    config = AssessmentConfig.Mixed(outcomes.size),
    status = AssessmentStatus.IN_PROGRESS,
    completedAt = null,
)

private fun attempt(
    id: String,
    outcomes: Array<out Pair<String, Boolean>>,
    config: AssessmentConfig,
    status: AssessmentStatus,
    completedAt: Instant?,
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
        startedAt = Start - 1.days,
        completedAt = completedAt,
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

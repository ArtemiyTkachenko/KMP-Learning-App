package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessStatus.GETTING_THERE
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessStatus.NOT_ENOUGH_EVIDENCE
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessStatus.READY

/**
 * The clock is fixed at [Now], and every attempt completes an offset before it. A wrong answer two
 * days ago is due; one an hour ago is scheduled but not due yet — so each test chooses whether a
 * mistake blocks `READY` by when it was made.
 */
private val Now = Instant.parse("2026-03-01T12:00:00Z")
private val LongAgo = 2.days
private val Recently = 1.hours

internal class ReadinessDerivationTest {

    @Test
    fun policyValuesAreTheAgreedFirstValuesAndReuseTheEvidenceMinimum() {
        assertEquals(LearningProgressPolicy.WeakAreaMinimumAnswered, ReadinessPolicy.MinimumAnsweredForVerdict)
        assertEquals(10, ReadinessPolicy.RecentWindow)
        assertEquals(80.0, ReadinessPolicy.ReadyRecentAccuracy)
        assertEquals(60.0, ReadinessPolicy.ReadyCoverage)
        assertEquals(0, ReadinessPolicy.MaximumDueMistakesForReady)
    }

    @Test
    fun targetsCountTheirOwnLevelAndEveryLevelBelowIt() {
        assertEquals(setOf(QuestionLevel.FOUNDATION), ReadinessTarget.JUNIOR.levels)
        assertEquals(setOf(QuestionLevel.FOUNDATION, QuestionLevel.APPLIED), ReadinessTarget.MID_LEVEL.levels)
        assertEquals(QuestionLevel.entries.toSet(), ReadinessTarget.SENIOR.levels)
    }

    @Test
    fun belowTheEvidenceMinimumThereIsNoVerdictOnlyTheAnswersStillNeeded() {
        // Four answers, all wrong a moment ago: a 0% accuracy is not stated over four answers.
        val readiness = single(
            completed("a", Recently, "f1" to false, "f2" to false, "f3" to false, "f4" to false),
        )

        assertEquals(NOT_ENOUGH_EVIDENCE, readiness.status)
        assertEquals(4, readiness.answeredCount)
        assertEquals(listOf<ReadinessGap>(ReadinessGap.MoreAnswersNeeded(1)), readiness.gaps)
    }

    @Test
    fun belowTheMinimumADueMistakeIsStillListedAfterTheAnswersNeeded() {
        val readiness = single(
            completed("old", LongAgo, "f1" to false),
            completed("new", Recently, "f2" to true),
        )

        assertEquals(NOT_ENOUGH_EVIDENCE, readiness.status)
        assertEquals(
            listOf(ReadinessGap.MoreAnswersNeeded(3), ReadinessGap.DueMistakes(1)),
            readiness.gaps,
        )
    }

    @Test
    fun allThreeConditionsMetIsReady() {
        val readiness = single(completed("a", Recently, *correct("f1", "f2", "f3", "f4", "f5", "f6")))

        assertEquals(READY, readiness.status)
        assertEquals(emptyList(), readiness.gaps)
        assertEquals(6, readiness.coveredQuestionCount)
        assertEquals(10, readiness.targetQuestionCount)
        assertEquals(100.0, readiness.recentAccuracy)
        assertEquals(0, readiness.dueMistakeCount)
    }

    @Test
    fun lowRecentAccuracyAloneIsGettingThereWithTheAccuracyGap() {
        // Coverage 60% and nothing due — the two wrong answers were a moment ago.
        val readiness = single(
            completed(
                "a",
                Recently,
                "f1" to true, "f2" to true, "f3" to true, "f4" to true, "f5" to false, "f6" to false,
            ),
        )

        assertEquals(GETTING_THERE, readiness.status)
        assertEquals(
            listOf<ReadinessGap>(ReadinessGap.RecentAccuracyBelow(percentage = 4.0 / 6 * 100, required = 80.0)),
            readiness.gaps,
        )
    }

    @Test
    fun lowCoverageAloneIsGettingThereWithTheCoverageGap() {
        val readiness = single(completed("a", Recently, *correct("f1", "f2", "f3", "f4", "f5")))

        assertEquals(GETTING_THERE, readiness.status)
        assertEquals(
            listOf<ReadinessGap>(ReadinessGap.CoverageBelow(percentage = 50.0, required = 60.0)),
            readiness.gaps,
        )
    }

    @Test
    fun aDueMistakeAloneBlocksReady() {
        // Six correct of seven is 85.7% recent accuracy and 70% coverage; only the mistake is missing.
        val readiness = single(
            completed("old", LongAgo, "f7" to false),
            completed("new", Recently, *correct("f1", "f2", "f3", "f4", "f5", "f6")),
        )

        assertEquals(GETTING_THERE, readiness.status)
        assertEquals(1, readiness.dueMistakeCount)
        assertEquals(listOf<ReadinessGap>(ReadinessGap.DueMistakes(1)), readiness.gaps)
    }

    @Test
    fun aMistakeThatIsNotDueYetDoesNotBlockReady() {
        val readiness = single(
            completed("old", Recently + 1.minutes, "f7" to false),
            completed("new", Recently, *correct("f1", "f2", "f3", "f4", "f5", "f6")),
        )

        assertEquals(READY, readiness.status)
        assertEquals(0, readiness.dueMistakeCount)
    }

    @Test
    fun theSameMistakeBlocksReadyOnceTheInjectedClockReachesItsDueTime() {
        val history = listOf(
            completed("new", Recently, *correct("f1", "f2", "f3", "f4", "f5", "f6")),
            completed("old", Recently + 1.minutes, "f7" to false),
        )

        assertEquals(READY, derive(history, now = Now).topics.single().status)
        assertEquals(GETTING_THERE, derive(history, now = Now + 1.days).topics.single().status)
    }

    @Test
    fun everyFailingConditionIsListedInPriorityOrder() {
        val readiness = single(
            completed("old", LongAgo, "f1" to false, "f2" to false),
            completed("new", Recently, *correct("f3", "f4", "f5")),
        )

        assertEquals(GETTING_THERE, readiness.status)
        assertEquals(
            listOf(
                ReadinessGap.DueMistakes(2),
                ReadinessGap.RecentAccuracyBelow(percentage = 60.0, required = 80.0),
                ReadinessGap.CoverageBelow(percentage = 50.0, required = 60.0),
            ),
            readiness.gaps,
        )
    }

    @Test
    fun thresholdsAreInclusiveAndComparedExactly() {
        // 3 of 5 is exactly 60% coverage and 8 of 10 exactly 80% recent accuracy.
        val bank = foundation(5)
        val readiness = single(
            completed("first", Recently + 3.minutes, *correct("f1", "f2", "f3")),
            completed("second", Recently + 2.minutes, *correct("f1", "f2", "f3")),
            completed("third", Recently + 1.minutes, *correct("f1", "f2")),
            completed("fourth", Recently, "f3" to false, "f1" to false),
            bank = bank,
        )

        assertEquals(3, readiness.coveredQuestionCount)
        assertEquals(8, readiness.recentCorrectCount)
        assertEquals(10, readiness.recentAnsweredCount)
        assertEquals(READY, readiness.status)
    }

    @Test
    fun recentAccuracyReadsOnlyTheNewestTenAnswers() {
        // Ten wrong answers five days ago, then the same ten answered correctly: the window holds only
        // the newest ten. Each old mistake has counted a correct review since, so none is due.
        val ids = (1..10).map { "f$it" }.toTypedArray()
        val readiness = single(
            completed("new", Recently, *correct(*ids)),
            completed("old", 5.days, *ids.map { it to false }.toTypedArray()),
        )

        assertEquals(20, readiness.answeredCount)
        assertEquals(10, readiness.recentAnsweredCount)
        assertEquals(10, readiness.recentCorrectCount)
        assertEquals(READY, readiness.status)
    }

    @Test
    fun theWindowIsChosenByCompletionTimeNotByTheOrderHistoryArrivesIn() {
        val ids = (1..10).map { "f$it" }.toTypedArray()
        val readiness = single(
            completed("old", 5.days, *ids.map { it to false }.toTypedArray()),
            completed("new", Recently, *correct(*ids)),
        )

        assertEquals(10, readiness.recentCorrectCount)
    }

    @Test
    fun coverageCountsEachQuestionOnceAndCountsAWrongAnswerAsSeen() {
        // An attempt holds a Question at most once, so the repeats are five separate sessions.
        val readiness = single(
            *repeatedly("f1", times = 5),
            completed("wrong", Recently, "f2" to false),
        )

        assertEquals(6, readiness.answeredCount)
        assertEquals(2, readiness.coveredQuestionCount)
    }

    @Test
    fun deprecatedQuestionsKeepTheirHistoryButLeaveCoverage() {
        val retired = question("old1", level = QuestionLevel.FOUNDATION, status = ContentStatus.DEPRECATED)
        val readiness = single(
            *repeatedly("old1", times = 5),
            bank = foundation(5) + retired,
        )

        assertEquals(5, readiness.answeredCount)
        assertEquals(100.0, readiness.recentAccuracy)
        assertEquals(0, readiness.coveredQuestionCount)
        assertEquals(5, readiness.targetQuestionCount)
        assertEquals(listOf<ReadinessGap>(ReadinessGap.CoverageBelow(0.0, 60.0)), readiness.gaps)
    }

    @Test
    fun onlyAnswersAtTheTargetLevelsCount() {
        val bank = foundation(5) + applied(5)
        val history = listOf(completed("a", Recently, *correct("a1", "a2", "a3", "a4", "a5")))

        val junior = derive(history, bank, ReadinessTarget.JUNIOR).topics.single()
        assertEquals(NOT_ENOUGH_EVIDENCE, junior.status)
        assertEquals(0, junior.answeredCount)
        assertNull(junior.recentAccuracy)
        assertEquals(5, junior.targetQuestionCount)

        val mid = derive(history, bank, ReadinessTarget.MID_LEVEL).topics.single()
        assertEquals(5, mid.answeredCount)
        assertEquals(10, mid.targetQuestionCount)
    }

    @Test
    fun aDueMistakeBelowTheTargetLevelsDoesNotBlockReady() {
        val readiness = single(
            completed("old", LongAgo, "a1" to false),
            completed("new", Recently, *correct("f1", "f2", "f3", "f4", "f5", "f6")),
            bank = foundation(10) + applied(2),
        )

        assertEquals(READY, readiness.status)
        assertEquals(0, readiness.dueMistakeCount)
    }

    @Test
    fun aSeniorTargetOnATopicWithoutAdvancedQuestionsIsJudgedOnTheRestAndFlagged() {
        val bank = foundation(5) + applied(5) +
            question("x1", topicId = "compose", level = QuestionLevel.ADVANCED) +
            question("x2", topicId = "compose", level = QuestionLevel.FOUNDATION)
        val history = listOf(completed("a", Recently, *correct("f1", "f2", "f3", "a1", "a2", "a3")))

        val senior = derive(history, bank, ReadinessTarget.SENIOR).topics.associateBy(TopicReadiness::topicId)
        val kotlin = senior.getValue("kotlin")
        assertTrue(kotlin.advancedUnavailable)
        assertEquals(10, kotlin.targetQuestionCount)
        assertEquals(READY, kotlin.status)
        // A Topic that has Advanced Questions is measured on them and carries no note.
        assertFalse(senior.getValue("compose").advancedUnavailable)
        assertEquals(2, senior.getValue("compose").targetQuestionCount)

        // Only a target that asks for Advanced can be missing it.
        assertFalse(derive(history, bank, ReadinessTarget.MID_LEVEL).topics.first().advancedUnavailable)
    }

    @Test
    fun aTopicWithNoQuestionsAtTheTargetLevelsIsOmitted() {
        val bank = foundation(5) + question("x1", topicId = "advanced_only", level = QuestionLevel.ADVANCED)

        val junior = derive(emptyList(), bank, ReadinessTarget.JUNIOR)
        assertEquals(listOf("kotlin"), junior.topics.map(TopicReadiness::topicId))

        val senior = derive(emptyList(), bank, ReadinessTarget.SENIOR)
        assertEquals(listOf("advanced_only", "kotlin"), senior.topics.map(TopicReadiness::topicId))
    }

    @Test
    fun aTopicNobodyHasAnsweredIsPresentWithNoEvidence() {
        val readiness = single()

        assertEquals(NOT_ENOUGH_EVIDENCE, readiness.status)
        assertEquals(0, readiness.coveredQuestionCount)
        assertNull(readiness.recentAccuracy)
        assertEquals(listOf<ReadinessGap>(ReadinessGap.MoreAnswersNeeded(5)), readiness.gaps)
    }

    @Test
    fun theOverallFigureIsACountOfReadyTopicsNotAScore() {
        val bank = foundation(10) + (1..10).map { question("c$it", topicId = "compose") }
        val snapshot = derive(
            listOf(completed("a", Recently, *correct("f1", "f2", "f3", "f4", "f5", "f6"))),
            bank,
        )

        assertEquals(2, snapshot.topics.size)
        assertEquals(1, snapshot.readyTopicCount)
    }

    @Test
    fun inProgressAttemptsAreNotEvidence() {
        val inProgress = completed("a", Recently, *correct("f1", "f2", "f3", "f4", "f5", "f6"))
            .copy(status = AssessmentStatus.IN_PROGRESS, completedAt = null, score = null)

        assertEquals(NOT_ENOUGH_EVIDENCE, single(inProgress).status)
    }
}

private fun single(
    vararg attempts: TestAttempt,
    bank: List<Question> = foundation(10),
): TopicReadiness = derive(attempts.toList(), bank).topics.single()

/**
 * The ACTIVE part of [bank] is the current bank; all of it, deprecated included, is what the
 * historical resolver can still find.
 */
private fun derive(
    attempts: List<TestAttempt>,
    bank: List<Question> = foundation(10),
    target: ReadinessTarget = ReadinessTarget.JUNIOR,
    now: Instant = Now,
): ReadinessSnapshot =
    ReadinessDerivation.derive(
        completedAttempts = attempts,
        activeQuestions = bank.filter { it.status == ContentStatus.ACTIVE },
        historicalQuestions = bank.associateBy(Question::id),
        target = target,
        now = now,
    )

private fun foundation(count: Int): List<Question> =
    (1..count).map { question("f$it", level = QuestionLevel.FOUNDATION) }

private fun applied(count: Int): List<Question> =
    (1..count).map { question("a$it", level = QuestionLevel.APPLIED) }

/** [times] sessions, a minute apart, each answering [id] correctly. */
private fun repeatedly(id: String, times: Int): Array<TestAttempt> =
    Array(times) { index -> completed("${id}_$index", Recently + (index + 1).minutes, id to true) }

private fun correct(vararg ids: String): Array<Pair<String, Boolean>> =
    ids.map { it to true }.toTypedArray()

private fun question(
    id: String,
    topicId: String = "kotlin",
    level: QuestionLevel = QuestionLevel.FOUNDATION,
    status: ContentStatus = ContentStatus.ACTIVE,
): Question = Question(
    id = id,
    topicId = topicId,
    subtopicId = "${topicId}_core",
    text = "Question $id",
    answers = listOf(AnswerOption("${id}_a", "A"), AnswerOption("${id}_b", "B")),
    selectionMode = AnswerSelectionMode.SINGLE,
    level = level,
    correctAnswerIds = listOf("${id}_a"),
    explanation = "Because.",
    sources = listOf(SourceReference("Source", "https://example.com")),
    status = status,
)

/** An attempt completed [ago] before [Now]. Answers are in assessment order. */
private fun completed(
    id: String,
    ago: Duration,
    vararg outcomes: Pair<String, Boolean>,
): TestAttempt {
    val completedAt = Now - ago
    return TestAttempt(
        id = id,
        config = AssessmentConfig.Mixed(outcomes.size),
        questionAttempts = outcomes.map { (questionId, isCorrect) ->
            QuestionAttempt(
                questionId = questionId,
                answerState = QuestionAnswerState.Answered(
                    selectedAnswerIds = setOf("${questionId}_a"),
                    isCorrect = isCorrect,
                ),
            )
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = completedAt - 10.minutes,
        completedAt = completedAt,
        score = AssessmentScore(
            totalQuestions = outcomes.size,
            correctAnswers = outcomes.count { it.second },
        ),
    )
}

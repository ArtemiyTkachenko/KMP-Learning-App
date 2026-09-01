package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

internal class LearningProgressServiceTest {
    @Test
    fun emptyHistoryReturnsEmptySnapshot() = runTest {
        val snapshot = TestContext().service.load()

        assertEquals(
            LearningProgressSnapshot(
                completedAttemptCount = 0,
                answeredQuestionCount = 0,
                correctAnswerCount = 0,
                percentage = 0.0,
                topics = emptyList(),
                subtopics = emptyList(),
                weakAreas = emptyList(),
                coverage = CurriculumCoverage(0, 0),
                topicCoverage = emptyList(),
                subtopicCoverage = emptyList(),
                recentPerformance = RecentPerformance(emptyList(), emptyList()),
            ),
            snapshot,
        )
    }

    @Test
    fun emptyHistoryStillReportsCurrentCurriculumAsUncovered() = runTest {
        val context = TestContext(
            questions = listOf(
                question("q1", "topic_a", "sub_a"),
                question("q2", "topic_a", "sub_a"),
                question("q3", "topic_b", "sub_b"),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(CurriculumCoverage(0, 3), snapshot.coverage)
        // No performance evidence exists, yet the unseen curriculum is still representable as 0/N.
        assertEquals(emptyList(), snapshot.topics)
        assertEquals(
            listOf(TopicCoverage("topic_a", 0, 2), TopicCoverage("topic_b", 0, 1)),
            snapshot.topicCoverage,
        )
        assertEquals(
            listOf(
                SubtopicCoverage("topic_a", "sub_a", 0, 2),
                SubtopicCoverage("topic_b", "sub_b", 0, 1),
            ),
            snapshot.subtopicCoverage,
        )
        assertEquals(0, snapshot.completedAttemptCount)
        assertEquals(0, snapshot.answeredQuestionCount)
        assertEquals(0, snapshot.correctAnswerCount)
    }

    @Test
    fun coverageCountsUniqueActiveQuestionsAcrossTopicsAndSubtopics() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt("first", listOf("q1" to true, "q3" to false)),
                completedAttempt("second", listOf("q4" to true)),
            ),
            questions = listOf(
                question("q1", "topic_a", "sub_a"),
                question("q2", "topic_a", "sub_a"),
                question("q3", "topic_a", "sub_b"),
                question("q4", "topic_b", "sub_c"),
                question("q5", "topic_b", "sub_c"),
            ),
        )

        val snapshot = context.service.load()

        // q3 was answered incorrectly and is still covered: coverage is exposure, not success.
        assertEquals(CurriculumCoverage(3, 5), snapshot.coverage)
        assertEquals(
            listOf(TopicCoverage("topic_a", 2, 3), TopicCoverage("topic_b", 1, 2)),
            snapshot.topicCoverage,
        )
        assertEquals(
            listOf(
                SubtopicCoverage("topic_a", "sub_a", 1, 2),
                SubtopicCoverage("topic_a", "sub_b", 1, 1),
                SubtopicCoverage("topic_b", "sub_c", 1, 2),
            ),
            snapshot.subtopicCoverage,
        )
        // One ACTIVE read per derivation; every grouping above happens in memory.
        assertEquals(1, context.curriculum.activeQuestionCalls)
    }

    @Test
    fun repeatedQuestionCoversOnceButKeepsAccuracyOccurrenceBased() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt("a", listOf("q1" to true)),
                completedAttempt("b", listOf("q1" to false)),
                completedAttempt("c", listOf("q1" to true)),
            ),
            questions = listOf(question("q1", "topic_a", "sub_a"), question("q2", "topic_a", "sub_a")),
        )

        val snapshot = context.service.load()

        assertEquals(CurriculumCoverage(1, 2), snapshot.coverage)
        assertEquals(listOf(TopicCoverage("topic_a", 1, 2)), snapshot.topicCoverage)
        assertEquals(3, snapshot.answeredQuestionCount)
        assertEquals(2, snapshot.correctAnswerCount)
        assertEquals(3, snapshot.topics.single().answeredCount)
        assertEquals(2, snapshot.topics.single().correctCount)
        assertEquals(3, snapshot.subtopics.single().answeredCount)
    }

    @Test
    fun focusedAndMixedEncountersOfTheSameQuestionCoverItOnce() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt(
                    id = "focused",
                    observations = listOf("q1" to true),
                    config = AssessmentConfig.Focused(AssessmentScope.Topic("topic_a"), 1),
                ),
                completedAttempt(
                    id = "mixed",
                    observations = listOf("q1" to false),
                    config = AssessmentConfig.Mixed(1),
                ),
            ),
            // The Focused attempt was scoped to topic_a, but coverage follows the Question's
            // CURRENT authored Topic rather than the assessment configuration.
            questions = listOf(question("q1", "topic_b", "sub_b"), question("q2", "topic_a", "sub_a")),
        )

        val snapshot = context.service.load()

        assertEquals(CurriculumCoverage(1, 2), snapshot.coverage)
        assertEquals(
            listOf(TopicCoverage("topic_a", 0, 1), TopicCoverage("topic_b", 1, 1)),
            snapshot.topicCoverage,
        )
    }

    @Test
    fun deprecatedAndMissingHistoricalQuestionsStayOutOfCurrentCoverage() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt(
                    id = "history",
                    observations = listOf(
                        "deprecated_q" to false,
                        "active_q" to true,
                        "missing_q" to true,
                    ),
                    persistedCorrectCount = 2,
                ),
            ),
            questions = listOf(
                question("deprecated_q", "old_topic", "old_sub", status = ContentStatus.DEPRECATED),
                question("active_q", "topic_a", "sub_a"),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(CurriculumCoverage(1, 1), snapshot.coverage)
        assertEquals(listOf(TopicCoverage("topic_a", 1, 1)), snapshot.topicCoverage)
        assertEquals(listOf(SubtopicCoverage("topic_a", "sub_a", 1, 1)), snapshot.subtopicCoverage)
        // Historical accuracy still keeps the persisted score and the DEPRECATED attribution.
        assertEquals(3, snapshot.answeredQuestionCount)
        assertEquals(2, snapshot.correctAnswerCount)
        assertEquals(
            listOf("old_topic", "topic_a"),
            snapshot.topics.map { it.topicId },
        )
    }

    @Test
    fun inProgressOccurrencesDoNotCountAsCoverage() = runTest {
        val context = TestContext(
            attempts = listOf(inProgressAttempt("unfinished")),
            questions = listOf(question("unfinished_question", "topic_a", "sub_a")),
        )

        val snapshot = context.service.load()

        assertEquals(CurriculumCoverage(0, 1), snapshot.coverage)
        assertEquals(listOf(TopicCoverage("topic_a", 0, 1)), snapshot.topicCoverage)
    }

    @Test
    fun curriculumExpansionLowersCoverageWithoutTouchingAccuracy() = runTest {
        val attempts = listOf(completedAttempt("history", listOf("q1" to true)))
        val before = TestContext(
            attempts = attempts,
            questions = listOf(question("q1", "topic_a", "sub_a"), question("q2", "topic_a", "sub_a")),
        ).service.load()

        val after = TestContext(
            attempts = attempts,
            questions = listOf(
                question("q1", "topic_a", "sub_a"),
                question("q2", "topic_a", "sub_a"),
                question("q3", "topic_a", "sub_a"),
            ),
        ).service.load()

        assertEquals(CurriculumCoverage(1, 2), before.coverage)
        assertEquals(50.0, before.coverage.percentage)
        assertEquals(CurriculumCoverage(1, 3), after.coverage)
        assertEquals(1.0 / 3.0 * 100.0, assertNotNull(after.coverage.percentage), 0.0000001)
        assertEquals(before.answeredQuestionCount, after.answeredQuestionCount)
        assertEquals(before.correctAnswerCount, after.correctAnswerCount)
        assertEquals(before.percentage, after.percentage)
        assertEquals(before.topics, after.topics)
        assertEquals(before.subtopics, after.subtopics)
    }

    @Test
    fun curriculumRetirementRemovesQuestionFromCoverageButNotFromAccuracy() = runTest {
        val attempts = listOf(completedAttempt("history", listOf("q1" to true, "q2" to false)))
        val before = TestContext(
            attempts = attempts,
            questions = listOf(question("q1", "topic_a", "sub_a"), question("q2", "topic_a", "sub_a")),
        ).service.load()

        val after = TestContext(
            attempts = attempts,
            questions = listOf(
                question("q1", "topic_a", "sub_a"),
                question("q2", "topic_a", "sub_a", status = ContentStatus.DEPRECATED),
            ),
        ).service.load()

        assertEquals(CurriculumCoverage(2, 2), before.coverage)
        assertEquals(CurriculumCoverage(1, 1), after.coverage)
        assertEquals(before.topics, after.topics)
        assertEquals(2, after.topics.single().answeredCount)
        assertEquals(1, after.topics.single().correctCount)
    }

    @Test
    fun coveragePercentageIsNullOnlyWhenNoCurrentCurriculumExists() {
        assertNull(CurriculumCoverage(0, 0).percentage)
        assertEquals(0.0, assertNotNull(CurriculumCoverage(0, 4).percentage))
        assertEquals(50.0, assertNotNull(TopicCoverage("topic_a", 1, 2).percentage))
        assertEquals(100.0, assertNotNull(SubtopicCoverage("topic_a", "sub_a", 3, 3).percentage))
    }

    @Test
    fun overallCountsSumPersistedScores() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt("first", observations(3, correctCount = 2)),
                completedAttempt("second", observations(5, correctCount = 4)),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(2, snapshot.completedAttemptCount)
        assertEquals(8, snapshot.answeredQuestionCount)
        assertEquals(6, snapshot.correctAnswerCount)
        assertEquals(75.0, snapshot.percentage)
    }

    @Test
    fun overallAccuracyUsesPersistedQuestionWeightedScoresAndIgnoresInProgressRows() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt("one", observations(1, correctCount = 1)),
                completedAttempt("twenty", observations(20, correctCount = 10)),
                inProgressAttempt("unfinished"),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(2, snapshot.completedAttemptCount)
        assertEquals(21, snapshot.answeredQuestionCount)
        assertEquals(11, snapshot.correctAnswerCount)
        assertEquals(11.0 / 21.0 * 100.0, snapshot.percentage, 0.0000001)
        assertFalse(snapshot.percentage == 75.0)
    }

    @Test
    fun focusedMixedAndRepeatedQuestionOccurrencesAggregateEquallyUsingPersistedCorrectness() = runTest {
        val questions = listOf(
            question("q1", "topic_b", "sub_b", currentCorrectAnswerId = "answer_current"),
            question("q2", "topic_a", "sub_a"),
            question("q3", "topic_a", "sub_b"),
        )
        val context = TestContext(
            attempts = listOf(
                completedAttempt(
                    id = "focused_topic",
                    observations = listOf("q1" to true, "q2" to false),
                    config = AssessmentConfig.Focused(AssessmentScope.Topic("topic_b"), 2),
                ),
                completedAttempt(
                    id = "focused_subtopic",
                    observations = listOf("q1" to false, "q3" to true),
                    config = AssessmentConfig.Focused(AssessmentScope.Subtopic("sub_b"), 2),
                ),
                completedAttempt(
                    id = "mixed_retake",
                    observations = listOf("q1" to true),
                    config = AssessmentConfig.Mixed(1),
                ),
            ),
            questions = questions,
            topics = listOf(Topic("topic_a", "Topic A"), Topic("topic_b", "Topic B")),
            subtopics = listOf(
                Subtopic("sub_a", "topic_a", "Subtopic A"),
                Subtopic("sub_b", "topic_b", "Subtopic B"),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(3, snapshot.completedAttemptCount)
        assertEquals(listOf("topic_a", "topic_b"), snapshot.topics.map { it.topicId })
        assertEquals(
            TopicPerformance("topic_a", "Topic A", 2, 1, 50.0, false),
            snapshot.topics[0],
        )
        assertEquals(3, snapshot.topics[1].answeredCount)
        assertEquals(2, snapshot.topics[1].correctCount)
        assertEquals(2.0 / 3.0 * 100.0, snapshot.topics[1].percentage, 0.0000001)
        assertTrue(snapshot.topics[1].isWeak)
        assertEquals(
            listOf("topic_a:sub_a", "topic_a:sub_b", "topic_b:sub_b"),
            snapshot.subtopics.map { "${it.topicId}:${it.subtopicId}" },
        )
        assertEquals(3, snapshot.subtopics.last().answeredCount)
        assertEquals(2, snapshot.subtopics.last().correctCount)
        assertEquals(1, context.curriculum.questionLookupCalls.getValue("q1"))
    }

    @Test
    fun missingQuestionKeepsPersistedOverallWhileDeprecatedAndMissingMetadataRemainScoped() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt(
                    id = "history",
                    observations = listOf(
                        "deprecated" to false,
                        "missing_metadata" to true,
                        "missing_question" to true,
                    ),
                    persistedCorrectCount = 2,
                ),
            ),
            questions = listOf(
                question(
                    id = "deprecated",
                    topicId = "old_topic",
                    subtopicId = "old_subtopic",
                    status = ContentStatus.DEPRECATED,
                ),
                question("missing_metadata", "removed_topic", "removed_subtopic"),
            ),
            topics = listOf(Topic("old_topic", "Old topic", ContentStatus.DEPRECATED)),
            subtopics = listOf(
                Subtopic(
                    "old_subtopic",
                    "old_topic",
                    "Old subtopic",
                    ContentStatus.DEPRECATED,
                ),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(3, snapshot.answeredQuestionCount)
        assertEquals(2, snapshot.correctAnswerCount)
        assertEquals(2, snapshot.topics.sumOf { it.answeredCount })
        assertEquals(2, snapshot.subtopics.sumOf { it.answeredCount })
        assertEquals("Old topic", snapshot.topics.first { it.topicId == "old_topic" }.topicName)
        assertEquals(
            "Old subtopic",
            snapshot.subtopics.first { it.subtopicId == "old_subtopic" }.subtopicName,
        )
        assertNull(snapshot.topics.first { it.topicId == "removed_topic" }.topicName)
        val missingSubtopic = snapshot.subtopics.first { it.subtopicId == "removed_subtopic" }
        assertEquals("removed_topic", missingSubtopic.topicId)
        assertNull(missingSubtopic.topicName)
        assertNull(missingSubtopic.subtopicName)
    }

    @Test
    fun historicalLookupsCacheResolvedAndMissingIdentitiesOncePerLoad() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedAttempt("first", listOf("q1" to true, "q2" to false, "missing" to false)),
                completedAttempt("second", listOf("q1" to false, "missing" to true)),
            ),
            questions = listOf(
                question("q1", "topic_a", "subtopic"),
                question("q2", "topic_b", "subtopic"),
            ),
            topics = listOf(
                Topic("topic_a", "Topic A"),
                Topic("topic_b", "Topic B"),
                Topic("unseen", "Unseen"),
            ),
            subtopics = listOf(
                Subtopic("subtopic", "topic_a", "Subtopic"),
                Subtopic("unseen_subtopic", "unseen", "Unseen subtopic"),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(mapOf("q1" to 1, "q2" to 1, "missing" to 1), context.curriculum.questionLookupCalls)
        assertEquals(mapOf("topic_a" to 1, "topic_b" to 1), context.curriculum.topicLookupCalls)
        assertEquals(mapOf("subtopic" to 1), context.curriculum.subtopicLookupCalls)
        assertEquals(listOf("topic_a", "topic_b"), snapshot.topics.map { it.topicId })
        assertEquals(
            listOf("topic_a:subtopic", "topic_b:subtopic"),
            snapshot.subtopics.map { "${it.topicId}:${it.subtopicId}" },
        )
    }

    @Test
    fun weakPolicyRequiresEvidenceAndTreatsExactlySeventyPercentAsNotWeak() = runTest {
        val fixtures = mutableListOf<Question>()
        val observations = mutableListOf<Pair<String, Boolean>>()
        addTopicObservations(fixtures, observations, "topic_under", 2, 0)
        addTopicObservations(fixtures, observations, "topic_weak", 3, 2)
        addTopicObservations(fixtures, observations, "topic_exact", 10, 7)
        addTopicObservations(fixtures, observations, "topic_above", 4, 3)
        addSubtopicObservations(fixtures, observations, "topic_sub", "sub_under", 1, 0)
        addSubtopicObservations(fixtures, observations, "topic_sub", "sub_weak", 2, 1)
        addSubtopicObservations(fixtures, observations, "topic_sub", "sub_exact", 10, 7)
        val context = TestContext(
            attempts = listOf(completedAttempt("boundaries", observations)),
            questions = fixtures,
        )

        val snapshot = context.service.load()

        assertFalse(snapshot.topics.first { it.topicId == "topic_under" }.isWeak)
        assertTrue(snapshot.topics.first { it.topicId == "topic_weak" }.isWeak)
        assertFalse(snapshot.topics.first { it.topicId == "topic_exact" }.isWeak)
        assertFalse(snapshot.topics.first { it.topicId == "topic_above" }.isWeak)
        assertFalse(snapshot.subtopics.first { it.subtopicId == "sub_under" }.isWeak)
        assertTrue(snapshot.subtopics.first { it.subtopicId == "sub_weak" }.isWeak)
        assertFalse(snapshot.subtopics.first { it.subtopicId == "sub_exact" }.isWeak)
    }

    @Test
    fun weakAreasSortByAccuracyThenEvidenceThenStableIdentity() = runTest {
        val fixtures = mutableListOf<Question>()
        val observations = mutableListOf<Pair<String, Boolean>>()
        addTopicObservations(fixtures, observations, "topic_d", 5, 2)
        addTopicObservations(fixtures, observations, "topic_b", 10, 4)
        addTopicObservations(fixtures, observations, "topic_a", 5, 2)
        addTopicObservations(fixtures, observations, "topic_c", 5, 1)
        val context = TestContext(
            attempts = listOf(completedAttempt("sorting", observations)),
            questions = fixtures,
        )

        val snapshot = context.service.load()

        assertEquals(
            listOf("topic_c", "topic_b", "topic_a", "topic_d"),
            snapshot.weakAreas.map { assertIs<WeakArea.Topic>(it).performance.topicId },
        )
    }

    @Test
    fun newUserHasNoRecentEvidenceRatherThanZeroPercent() = runTest {
        val recent = TestContext(questions = listOf(question("q1", "topic_a", "sub_a")))
            .service.load().recentPerformance

        assertEquals(0, recent.attemptCount)
        assertEquals(0, recent.answeredQuestionCount)
        assertEquals(0, recent.correctAnswerCount)
        // Nothing has been answered, which is not the same statement as having scored 0%.
        assertNull(recent.percentage)
        assertEquals(emptyList(), recent.attemptSeries)
        assertEquals(emptyList(), recent.answerSeries)
        assertEquals(RecentTrendAvailability.InsufficientHistory(0, 3), recent.trendAvailability)
    }

    @Test
    fun oneCompletedAttemptSummarisesRecentAccuracyButCannotTrend() = runTest {
        val context = TestContext(
            attempts = listOf(completedOn(1, "only", observations(10, correctCount = 8))),
        )

        val recent = context.service.load().recentPerformance

        assertEquals(1, recent.attemptCount)
        assertEquals(10, recent.answeredQuestionCount)
        assertEquals(8, recent.correctAnswerCount)
        assertEquals(80.0, assertNotNull(recent.percentage))
        assertEquals(listOf("only"), recent.attemptSeries.map { it.attemptId })
        assertEquals(80.0, recent.attemptSeries.single().percentage)
        assertEquals(RecentTrendAvailability.InsufficientHistory(1, 3), recent.trendAvailability)
    }

    @Test
    fun twoCompletedAttemptsSummariseButStillCannotTrend() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(1, "first", answers("first", count = 4, correctCount = 1)),
                completedOn(2, "second", answers("second", count = 4, correctCount = 3)),
            ),
        )

        val recent = context.service.load().recentPerformance

        assertEquals(2, recent.attemptCount)
        assertEquals(50.0, assertNotNull(recent.percentage))
        assertEquals(listOf("first", "second"), recent.attemptSeries.map { it.attemptId })
        // One change between two results is as likely to be noise as a trajectory.
        assertEquals(RecentTrendAvailability.InsufficientHistory(2, 3), recent.trendAvailability)
    }

    @Test
    fun threeCompletedAttemptsMakeTheTrendAvailable() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(1, "first", answers("first", count = 2, correctCount = 1)),
                completedOn(2, "second", answers("second", count = 2, correctCount = 1)),
                completedOn(3, "third", answers("third", count = 2, correctCount = 2)),
            ),
        )

        val recent = context.service.load().recentPerformance

        assertEquals(RecentTrendAvailability.Available, recent.trendAvailability)
        assertEquals(3, recent.attemptSeries.size)
    }

    @Test
    fun onlyTheLatestFiveCompletedAttemptsDefineRecentPerformance() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(1, "a1", answers("a1", count = 2, correctCount = 0)),
                completedOn(2, "a2", answers("a2", count = 2, correctCount = 0)),
                completedOn(3, "a3", answers("a3", count = 1, correctCount = 1)),
                completedOn(4, "a4", answers("a4", count = 1, correctCount = 1)),
                completedOn(5, "a5", answers("a5", count = 1, correctCount = 1)),
                completedOn(6, "a6", answers("a6", count = 1, correctCount = 1)),
                completedOn(7, "a7", answers("a7", count = 1, correctCount = 1)),
            ),
        )

        val recent = context.service.load().recentPerformance

        // The two oldest attempts, and the eight wrong answers in them, are outside the window.
        assertEquals(
            listOf("a3", "a4", "a5", "a6", "a7"),
            recent.attemptSeries.map { it.attemptId },
        )
        assertEquals(5, recent.answeredQuestionCount)
        assertEquals(5, recent.correctAnswerCount)
        assertEquals(100.0, assertNotNull(recent.percentage))
        assertEquals(listOf("a3", "a4", "a5", "a6", "a7"), recent.answerSeries.map { it.attemptId })
    }

    @Test
    fun recentAccuracyIsQuestionWeightedRatherThanAnAverageOfAttemptPercentages() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(1, "one_question", answers("single", count = 1, correctCount = 1)),
                completedOn(2, "twenty_questions", answers("many", count = 20, correctCount = 10)),
            ),
        )

        val recent = context.service.load().recentPerformance

        assertEquals(21, recent.answeredQuestionCount)
        assertEquals(11, recent.correctAnswerCount)
        assertEquals(11.0 / 21.0 * 100.0, assertNotNull(recent.percentage), 0.0000001)
        // Averaging the two attempt percentages equally would claim 75%.
        assertNotEquals(75.0, recent.percentage)
        assertEquals(listOf(100.0, 50.0), recent.attemptSeries.map { it.percentage })
    }

    @Test
    fun recentPerformanceUsesPersistedCorrectnessAndAddsNoCurriculumReads() = runTest {
        val context = TestContext(
            attempts = listOf(
                // Selected answers match no current answer key, so a derivation that recompared
                // against Question.correctAnswerIds could not report these as correct.
                completedOn(1, "history", listOf("q1" to true, "q2" to true)),
                completedOn(2, "later", listOf("q1" to true, "q2" to false)),
            ),
            questions = listOf(
                question("q1", "topic_a", "sub_a", currentCorrectAnswerId = "answer_current"),
                question("q2", "topic_a", "sub_a", currentCorrectAnswerId = "answer_current"),
            ),
        )

        val snapshot = context.service.load()

        assertEquals(3, snapshot.recentPerformance.correctAnswerCount)
        assertEquals(75.0, assertNotNull(snapshot.recentPerformance.percentage))
        assertEquals(
            listOf(true, true, true, false),
            snapshot.recentPerformance.answerSeries.map { it.isCorrect },
        )
        // Historical Question resolution and the single ACTIVE read both predate this issue; recent
        // performance needs neither, so the counts are unchanged by it.
        assertEquals(mapOf("q1" to 1, "q2" to 1), context.curriculum.questionLookupCalls)
        assertEquals(1, context.curriculum.activeQuestionCalls)
    }

    @Test
    fun improvingHistoryIsExposedAsRawOrderedPercentages() = runTest {
        val context = TestContext(
            attempts = List(5) { index ->
                completedOn(
                    day = index + 1,
                    id = "attempt_$index",
                    observations = answers("attempt_$index", count = 10, correctCount = 4 + index),
                )
            },
        )

        val recent = context.service.load().recentPerformance

        // The trajectory is the numbers themselves; no direction or momentum score is derived.
        assertEquals(
            listOf(40.0, 50.0, 60.0, 70.0, 80.0),
            recent.attemptSeries.map { it.percentage },
        )
        assertEquals(RecentTrendAvailability.Available, recent.trendAvailability)
    }

    @Test
    fun regressingHistoryIsExposedAsRawOrderedPercentages() = runTest {
        val context = TestContext(
            attempts = List(5) { index ->
                completedOn(
                    day = index + 1,
                    id = "attempt_$index",
                    observations = answers("attempt_$index", count = 10, correctCount = 9 - index),
                )
            },
        )

        val recent = context.service.load().recentPerformance

        assertEquals(
            listOf(90.0, 80.0, 70.0, 60.0, 50.0),
            recent.attemptSeries.map { it.percentage },
        )
    }

    @Test
    fun windowSelectionFollowsTimestampsRatherThanInputOrder() = runTest {
        val attempts = listOf(
            completedOn(3, "third", answers("third", count = 1, correctCount = 1)),
            completedOn(1, "first", answers("first", count = 1, correctCount = 1)),
            completedOn(6, "sixth", answers("sixth", count = 1, correctCount = 1)),
            completedOn(2, "second", answers("second", count = 1, correctCount = 1)),
            completedOn(5, "fifth", answers("fifth", count = 1, correctCount = 1)),
            completedOn(4, "fourth", answers("fourth", count = 1, correctCount = 1)),
        )

        val recent = TestContext(attempts = attempts).service.load().recentPerformance

        assertEquals(
            listOf("second", "third", "fourth", "fifth", "sixth"),
            recent.attemptSeries.map { it.attemptId },
        )
    }

    @Test
    fun attemptsCompletedAtTheSameInstantBreakTiesByStartThenIdentity() = runTest {
        val completedAt = day(2) + 1.hours
        val tied = { id: String, startedAt: Instant ->
            completedAttempt(
                id = id,
                observations = answers(id, count = 1, correctCount = 1),
                startedAt = startedAt,
                completedAt = completedAt,
            )
        }
        val context = TestContext(
            attempts = listOf(
                completedOn(1, "older", answers("older", count = 1, correctCount = 1)),
                tied("a", day(2)),
                tied("b", day(2) + 30.minutes),
                tied("c", day(2) + 30.minutes),
                tied("d", day(2) + 15.minutes),
                tied("e", day(2) + 15.minutes),
            ),
        )

        val recent = context.service.load().recentPerformance

        // Newest first is startedAt descending and then id ascending, exactly as the attempt table
        // is queried, so the series read oldest -> newest is its reverse. "older" is the sixth
        // attempt and falls out of the window.
        assertEquals(
            listOf("a", "e", "d", "c", "b"),
            recent.attemptSeries.map { it.attemptId },
        )
    }

    @Test
    fun inProgressAttemptsStayOutOfRecentPerformanceEvenWhenNewest() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(1, "first", answers("first", count = 2, correctCount = 2)),
                completedOn(2, "second", answers("second", count = 2, correctCount = 2)),
                inProgressAttempt("unfinished", startedAt = day(9)),
            ),
        )

        val recent = context.service.load().recentPerformance

        assertEquals(listOf("first", "second"), recent.attemptSeries.map { it.attemptId })
        assertEquals(4, recent.answeredQuestionCount)
        assertEquals(RecentTrendAvailability.InsufficientHistory(2, 3), recent.trendAvailability)
        assertTrue(recent.answerSeries.none { it.attemptId == "unfinished" })
    }

    @Test
    fun focusedAndMixedAttemptsParticipateOnIdenticalTerms() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(
                    day = 1,
                    id = "focused_topic",
                    observations = answers("focused_topic", count = 5, correctCount = 4),
                    config = AssessmentConfig.Focused(AssessmentScope.Topic("topic_a"), 5),
                ),
                completedOn(
                    day = 2,
                    id = "mixed",
                    observations = answers("mixed", count = 10, correctCount = 7),
                    config = AssessmentConfig.Mixed(10),
                ),
                completedOn(
                    day = 3,
                    id = "focused_subtopic",
                    observations = answers("focused_subtopic", count = 5, correctCount = 5),
                    config = AssessmentConfig.Focused(AssessmentScope.Subtopic("sub_a"), 5),
                ),
            ),
        )

        val recent = context.service.load().recentPerformance

        assertEquals(
            listOf("focused_topic", "mixed", "focused_subtopic"),
            recent.attemptSeries.map { it.attemptId },
        )
        assertEquals(20, recent.answeredQuestionCount)
        assertEquals(16, recent.correctAnswerCount)
        assertEquals(80.0, assertNotNull(recent.percentage))
    }

    @Test
    fun retakeOccurrencesOfTheSameQuestionsBothCount() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(1, "original", listOf("q1" to false, "q2" to false)),
                completedOn(2, "retake", listOf("q1" to true, "q2" to true)),
            ),
            questions = listOf(question("q1", "topic_a", "sub_a"), question("q2", "topic_a", "sub_a")),
        )

        val snapshot = context.service.load()

        // Performance is occurrence-based: the retake adds observations, it does not replace them.
        assertEquals(4, snapshot.recentPerformance.answeredQuestionCount)
        assertEquals(2, snapshot.recentPerformance.correctAnswerCount)
        assertEquals(50.0, assertNotNull(snapshot.recentPerformance.percentage))
        assertEquals(
            listOf("q1", "q2", "q1", "q2"),
            snapshot.recentPerformance.answerSeries.map { it.questionId },
        )
        // Coverage still counts each stable Question ID once.
        assertEquals(CurriculumCoverage(2, 2), snapshot.coverage)
    }

    @Test
    fun answerSeriesFollowsAttemptChronologyThenStoredQuestionOrder() = runTest {
        val context = TestContext(
            attempts = listOf(
                completedOn(2, "later", listOf("y1" to false, "y2" to true)),
                completedOn(1, "earlier", listOf("x1" to true, "x2" to false, "x3" to true)),
            ),
        )

        val recent = context.service.load().recentPerformance

        assertEquals(
            listOf(
                RecentAnswerOutcome("earlier", "x1", true),
                RecentAnswerOutcome("earlier", "x2", false),
                RecentAnswerOutcome("earlier", "x3", true),
                RecentAnswerOutcome("later", "y1", false),
                RecentAnswerOutcome("later", "y2", true),
            ),
            recent.answerSeries,
        )
    }

    @Test
    fun answerSeriesKeepsTheMostRecentOutcomesWhenTheWindowExceedsTheCap() = runTest {
        val recent = TestContext(attempts = sixtyAnswerWindow()).service.load().recentPerformance

        assertEquals(50, recent.answerSeries.size)
        // The excess is dropped from the oldest end, and what remains stays oldest -> newest.
        assertEquals(RecentAnswerOutcome("a1", "a1_q10", false), recent.answerSeries.first())
        assertEquals(RecentAnswerOutcome("a5", "a5_q11", true), recent.answerSeries.last())
    }

    @Test
    fun answerSeriesCapDoesNotChangeTheRecentSummary() = runTest {
        val recent = TestContext(attempts = sixtyAnswerWindow()).service.load().recentPerformance

        // Every answer in the window counts: 48 correct out of 60. Summarising the capped series
        // instead would report 48 out of 50.
        assertEquals(60, recent.answeredQuestionCount)
        assertEquals(48, recent.correctAnswerCount)
        assertEquals(80.0, assertNotNull(recent.percentage))
        assertEquals(48, recent.answerSeries.count { it.isCorrect })
    }

    @Test
    fun allTimeFiguresStayAllTimeWhileRecentUsesTheWindow() = runTest {
        val context = TestContext(
            attempts = List(7) { index ->
                completedOn(
                    day = index + 1,
                    id = "attempt_$index",
                    observations = answers(
                        prefix = "attempt_$index",
                        count = 2,
                        correctCount = if (index < 2) 0 else 2,
                    ),
                )
            },
        )

        val snapshot = context.service.load()

        assertEquals(7, snapshot.completedAttemptCount)
        assertEquals(14, snapshot.answeredQuestionCount)
        assertEquals(10, snapshot.correctAnswerCount)
        assertEquals(10.0 / 14.0 * 100.0, snapshot.percentage, 0.0000001)
        assertEquals(5, snapshot.recentPerformance.attemptCount)
        assertEquals(10, snapshot.recentPerformance.answeredQuestionCount)
        assertEquals(100.0, assertNotNull(snapshot.recentPerformance.percentage))
    }

    @Test
    fun coverageStaysAllHistoryWhenAQuestionFallsOutOfTheRecentWindow() = runTest {
        val context = TestContext(
            attempts = listOf(completedOn(1, "old", listOf("q_old" to true))) +
                List(5) { index ->
                    completedOn(index + 2, "recent_$index", listOf("q_recent_$index" to true))
                },
            questions = listOf(question("q_old", "topic_a", "sub_a")) +
                List(5) { index -> question("q_recent_$index", "topic_a", "sub_a") },
        )

        val snapshot = context.service.load()

        // Coverage is exposure across the complete history; the recent window never narrows it.
        assertEquals(CurriculumCoverage(6, 6), snapshot.coverage)
        assertEquals(listOf(TopicCoverage("topic_a", 6, 6)), snapshot.topicCoverage)
        assertTrue(snapshot.recentPerformance.attemptSeries.none { it.attemptId == "old" })
        assertTrue(snapshot.recentPerformance.answerSeries.none { it.questionId == "q_old" })
    }

    @Test
    fun weakAreasStayAllTimeWhenTheirEvidenceIsOlderThanTheWindow() = runTest {
        val weakQuestions = List(3) { index -> question("weak_q_$index", "topic_weak", "sub_weak") }
        val context = TestContext(
            attempts = listOf(
                completedAttempt(
                    id = "old",
                    observations = weakQuestions.map { it.id to false },
                    startedAt = day(1),
                ),
            ) + List(5) { index ->
                completedOn(index + 2, "recent_$index", listOf("strong_q_$index" to true))
            },
            questions = weakQuestions +
                List(5) { index -> question("strong_q_$index", "topic_strong", "sub_strong") },
        )

        val snapshot = context.service.load()

        // The only failing evidence is outside the recent window and still defines the weak area.
        assertEquals(
            listOf("topic_weak"),
            snapshot.weakAreas.filterIsInstance<WeakArea.Topic>().map { it.performance.topicId },
        )
        assertEquals(0.0, snapshot.topics.first { it.topicId == "topic_weak" }.percentage)
        assertEquals(100.0, assertNotNull(snapshot.recentPerformance.percentage))
    }
}

/**
 * Five completed attempts holding 60 answers, more than the answer-series cap. The oldest attempt is
 * entirely incorrect and the rest entirely correct, so summarising the capped series would give a
 * visibly different figure from summarising the window.
 */
private fun sixtyAnswerWindow(): List<TestAttempt> =
    List(5) { index ->
        val id = "a${index + 1}"
        completedOn(
            day = index + 1,
            id = id,
            observations = answers(id, count = 12, correctCount = if (index == 0) 0 else 12),
        )
    }

private class TestContext(
    attempts: List<TestAttempt> = emptyList(),
    questions: List<Question> = emptyList(),
    topics: List<Topic> = emptyList(),
    subtopics: List<Subtopic> = emptyList(),
) {
    private val assessment = FakeAssessmentRepository(attempts)
    val curriculum = FakeCurriculumRepository(questions, topics, subtopics)
    val service = LearningProgressService(assessment, curriculum)
}

private class FakeAssessmentRepository(
    private val attempts: List<TestAttempt>,
) : AssessmentRepository {
    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts
}

private class FakeCurriculumRepository(
    private val questions: List<Question>,
    topics: List<Topic>,
    subtopics: List<Subtopic>,
) : CurriculumRepository {
    private val questionsById = questions.associateBy(Question::id)
    private val topicsById = topics.associateBy(Topic::id)
    private val subtopicsById = subtopics.associateBy(Subtopic::id)
    val questionLookupCalls = mutableMapOf<String, Int>()
    val topicLookupCalls = mutableMapOf<String, Int>()
    val subtopicLookupCalls = mutableMapOf<String, Int>()
    var activeQuestionCalls = 0
        private set

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")

    /** Coverage reads the current ACTIVE question bank; DEPRECATED fixtures stay out of it. */
    override suspend fun getActiveQuestions(): List<Question> {
        activeQuestionCalls += 1
        return questions.filter { it.status == ContentStatus.ACTIVE }
    }

    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")

    override suspend fun getTopicById(topicId: String): Topic? {
        topicLookupCalls[topicId] = topicLookupCalls.getOrElse(topicId) { 0 } + 1
        return topicsById[topicId]
    }

    override suspend fun getSubtopicById(subtopicId: String): Subtopic? {
        subtopicLookupCalls[subtopicId] = subtopicLookupCalls.getOrElse(subtopicId) { 0 } + 1
        return subtopicsById[subtopicId]
    }

    override suspend fun getQuestionById(questionId: String): Question? {
        questionLookupCalls[questionId] = questionLookupCalls.getOrElse(questionId) { 0 } + 1
        return questionsById[questionId]
    }
}

private fun completedAttempt(
    id: String,
    observations: List<Pair<String, Boolean>>,
    config: AssessmentConfig = AssessmentConfig.Mixed(observations.size),
    persistedCorrectCount: Int = observations.count { it.second },
    startedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    completedAt: Instant = startedAt + 1.minutes,
): TestAttempt =
    TestAttempt(
        id = id,
        config = config,
        questionAttempts = observations.map { (questionId, isCorrect) ->
            QuestionAttempt(
                questionId = questionId,
                answerState = QuestionAnswerState.Answered(
                    selectedAnswerIds = setOf("${questionId}_selected"),
                    isCorrect = isCorrect,
                ),
            )
        },
        status = AssessmentStatus.COMPLETED,
        startedAt = startedAt,
        completedAt = completedAt,
        score = AssessmentScore(observations.size, persistedCorrectCount),
    )

private fun inProgressAttempt(
    id: String,
    startedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): TestAttempt =
    TestAttempt(
        id = id,
        config = AssessmentConfig.Mixed(1),
        questionAttempts = listOf(QuestionAttempt("${id}_question")),
        status = AssessmentStatus.IN_PROGRESS,
        startedAt = startedAt,
    )

private fun observations(
    count: Int,
    correctCount: Int,
): List<Pair<String, Boolean>> =
    List(count) { index -> "question_${count}_$index" to (index < correctCount) }

/** Fixed calendar day, so recent-window fixtures read as an obvious chronology. */
private fun day(dayOfMonth: Int): Instant =
    Instant.parse("2026-03-${dayOfMonth.toString().padStart(2, '0')}T10:00:00Z")

private fun completedOn(
    day: Int,
    id: String,
    observations: List<Pair<String, Boolean>>,
    config: AssessmentConfig = AssessmentConfig.Mixed(observations.size),
): TestAttempt =
    completedAttempt(
        id = id,
        observations = observations,
        config = config,
        startedAt = day(day),
    )

/** [correctCount] leading answers correct, so an attempt's persisted accuracy is stated directly. */
private fun answers(
    prefix: String,
    count: Int,
    correctCount: Int,
): List<Pair<String, Boolean>> =
    List(count) { index -> "${prefix}_q$index" to (index < correctCount) }

private fun question(
    id: String,
    topicId: String,
    subtopicId: String,
    currentCorrectAnswerId: String = "${id}_a",
    status: ContentStatus = ContentStatus.ACTIVE,
): Question =
    Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = "Question $id",
        answers = listOf(
            AnswerOption("${id}_a", "Answer A"),
            AnswerOption("${id}_b", "Answer B"),
            AnswerOption("answer_current", "Current answer"),
        ),
        selectionMode = AnswerSelectionMode.SINGLE,
        correctAnswerIds = listOf(currentCorrectAnswerId),
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com/$id")),
        status = status,
    )

private fun addTopicObservations(
    questions: MutableList<Question>,
    observations: MutableList<Pair<String, Boolean>>,
    topicId: String,
    answeredCount: Int,
    correctCount: Int,
) {
    repeat(answeredCount) { index ->
        val questionId = "${topicId}_q_$index"
        questions += question(questionId, topicId, "${topicId}_sub_$index")
        observations += questionId to (index < correctCount)
    }
}

private fun addSubtopicObservations(
    questions: MutableList<Question>,
    observations: MutableList<Pair<String, Boolean>>,
    topicId: String,
    subtopicId: String,
    answeredCount: Int,
    correctCount: Int,
) {
    repeat(answeredCount) { index ->
        val questionId = "${subtopicId}_q_$index"
        questions += question(questionId, topicId, subtopicId)
        observations += questionId to (index < correctCount)
    }
}

package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
        startedAt = Instant.parse("2026-01-01T00:00:00Z"),
        completedAt = Instant.parse("2026-01-01T00:01:00Z"),
        score = AssessmentScore(observations.size, persistedCorrectCount),
    )

private fun inProgressAttempt(id: String): TestAttempt =
    TestAttempt(
        id = id,
        config = AssessmentConfig.Mixed(1),
        questionAttempts = listOf(QuestionAttempt("${id}_question")),
        status = AssessmentStatus.IN_PROGRESS,
        startedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

private fun observations(
    count: Int,
    correctCount: Int,
): List<Pair<String, Boolean>> =
    List(count) { index -> "question_${count}_$index" to (index < correctCount) }

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

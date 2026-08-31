package org.artkachenko.kmp_learning_app.learning_progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
            ),
            snapshot,
        )
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
    questions: List<Question>,
    topics: List<Topic>,
    subtopics: List<Subtopic>,
) : CurriculumRepository {
    private val questionsById = questions.associateBy(Question::id)
    private val topicsById = topics.associateBy(Topic::id)
    private val subtopicsById = subtopics.associateBy(Subtopic::id)
    val questionLookupCalls = mutableMapOf<String, Int>()
    val topicLookupCalls = mutableMapOf<String, Int>()
    val subtopicLookupCalls = mutableMapOf<String, Int>()

    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> = error("ACTIVE lookup must not be used.")
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

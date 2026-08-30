package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService

@OptIn(ExperimentalCoroutinesApi::class)
internal class TopicDetailViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTopicByStableIdAndPreservesSubtopicOrderAndCounts() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopics = listOf(
            Subtopic("subtopic_b", topic.id, "Subtopic B"),
            Subtopic("subtopic_a", topic.id, "Subtopic A"),
            Subtopic("subtopic_c", topic.id, "Subtopic C"),
        )
        val repository = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = subtopics,
            questions = listOf(
                question("q1", topic.id, "subtopic_b"),
                question("q2", topic.id, "subtopic_b"),
                question("q3", topic.id, "subtopic_c"),
            ),
        )
        val viewModel = TopicDetailViewModel("topic_a", repository, progressService(repository))

        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        assertEquals(topic, state.topic)
        assertEquals(3, state.topicQuestionCount)
        assertEquals(listOf("subtopic_b", "subtopic_c"), state.subtopics.map { it.subtopic.id })
        assertEquals(listOf(2, 1), state.subtopics.map { it.questionCount })
        assertEquals(1, repository.topicQuestionCalls)
        assertEquals(0, repository.subtopicQuestionCalls)
    }

    @Test
    fun missingTopicDoesNotQueryChildren() = runViewModelTest {
        val repository = FakeCurriculumRepository(topics = emptyList())
        val viewModel = TopicDetailViewModel("missing", repository, progressService(repository))

        advanceUntilIdle()

        assertEquals(TopicDetailUiState.NotFound, viewModel.uiState.value)
        assertEquals(0, repository.subtopicCalls)
        assertEquals(0, repository.topicQuestionCalls)
    }

    @Test
    fun noQuestionsProducesUnavailablePracticeState() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val viewModel = TopicDetailViewModel(
            topicId = topic.id,
            curriculumRepository = FakeCurriculumRepository(topics = listOf(topic)),
            learningProgressService = progressService(FakeCurriculumRepository(topics = listOf(topic))),
        )

        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.NoQuestions>(viewModel.uiState.value)
        assertEquals(topic, state.topic)
        assertNull(viewModel.topicPracticeConfig())
        assertNull(viewModel.subtopicPracticeConfig("unknown"))
    }

    @Test
    fun buildsTopicAndPopulatedSubtopicConfigs() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_a", topic.id, "Subtopic A")
        val viewModel = TopicDetailViewModel(
            topicId = topic.id,
            curriculumRepository = FakeCurriculumRepository(
                topics = listOf(topic),
                subtopics = listOf(subtopic),
                questions = listOf(question("q1", topic.id, subtopic.id)),
            ),
            learningProgressService = progressService(
                FakeCurriculumRepository(
                    topics = listOf(topic),
                    subtopics = listOf(subtopic),
                    questions = listOf(question("q1", topic.id, subtopic.id)),
                ),
            ),
        )

        advanceUntilIdle()

        assertEquals(
            AssessmentScope.Topic(topic.id),
            viewModel.topicPracticeConfig()?.scope,
        )
        assertEquals(
            FocusedPracticeQuestionCount,
            viewModel.topicPracticeConfig()?.questionCount,
        )
        assertEquals(
            AssessmentScope.Subtopic(subtopic.id),
            viewModel.subtopicPracticeConfig(subtopic.id)?.scope,
        )
        assertNull(viewModel.subtopicPracticeConfig("empty-or-unknown"))
    }

    @Test
    fun retryCanRecoverFromFailure() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val repository = FakeCurriculumRepository(
            topics = listOf(topic),
            failuresRemaining = 1,
            questions = listOf(question("q1", topic.id, "subtopic_a")),
        )
        val viewModel = TopicDetailViewModel(topic.id, repository, progressService(repository))

        advanceUntilIdle()
        assertEquals(TopicDetailUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        assertEquals(TopicDetailUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun observedAccuracyFromCompletedHistoryReachesTheTopicAndItsSubtopics() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopics = listOf(
            Subtopic("subtopic_a", topic.id, "Subtopic A"),
            Subtopic("subtopic_b", topic.id, "Subtopic B"),
        )
        val questions = listOf(
            question("q1", topic.id, "subtopic_a"),
            question("q2", topic.id, "subtopic_a"),
            question("q3", topic.id, "subtopic_b"),
        )
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = subtopics,
            questions = questions,
        )
        // subtopic_a: 1 of 2 correct, subtopic_b: 1 of 1, so the topic is 2 of 3.
        val history = HistoryRepository(
            completedAttempt("q1" to true, "q2" to false, "q3" to true),
        )

        val viewModel = TopicDetailViewModel(
            topicId = topic.id,
            curriculumRepository = curriculum,
            learningProgressService = LearningProgressService(history, curriculum),
        )
        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        assertEquals(2.0 / 3.0 * 100.0, state.accuracyPercentage)
        val byId = state.subtopics.associateBy { it.subtopic.id }
        assertEquals(50.0, byId.getValue("subtopic_a").accuracyPercentage)
        assertEquals(100.0, byId.getValue("subtopic_b").accuracyPercentage)
    }

    @Test
    fun aTopicWithNoCompletedHistoryReportsNoAccuracyRatherThanZero() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = listOf(Subtopic("subtopic_a", topic.id, "Subtopic A")),
            questions = listOf(question("q1", topic.id, "subtopic_a")),
        )

        val viewModel = TopicDetailViewModel(topic.id, curriculum, progressService(curriculum))
        advanceUntilIdle()

        // Null, not 0%: never answered is a different statement from answered and got none right.
        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        assertNull(state.accuracyPercentage)
        assertNull(state.subtopics.single().accuracyPercentage)
    }

    private fun completedAttempt(vararg answers: Pair<String, Boolean>): TestAttempt =
        TestAttempt(
            id = "attempt",
            config = AssessmentConfig.Mixed(answers.size),
            questionAttempts = answers.map { (questionId, isCorrect) ->
                QuestionAttempt(
                    questionId,
                    QuestionAnswerState.Answered(setOf("${questionId}_a"), isCorrect),
                )
            },
            status = AssessmentStatus.COMPLETED,
            startedAt = Instant.parse("2026-08-29T00:00:00Z"),
            completedAt = Instant.parse("2026-08-29T00:15:00Z"),
            score = AssessmentScore(answers.size, answers.count { it.second }),
        )

    private class HistoryRepository(private val attempt: TestAttempt) : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = attempt
        override suspend fun getCompletedAttempts(): List<TestAttempt> = listOf(attempt)
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun question(id: String, topicId: String, subtopicId: String) = Question(
        id = id,
        topicId = topicId,
        subtopicId = subtopicId,
        text = "Question $id",
        answers = listOf(AnswerOption("answer_a", "Answer A")),
        correctAnswerIds = listOf("answer_a"),
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com")),
    )

    private class FakeCurriculumRepository(
        private val topics: List<Topic>,
        private val subtopics: List<Subtopic> = emptyList(),
        private val questions: List<Question> = emptyList(),
        private var failuresRemaining: Int = 0,
    ) : CurriculumRepository {
        var subtopicCalls = 0
            private set
        var topicQuestionCalls = 0
            private set
        var subtopicQuestionCalls = 0
            private set

        override suspend fun getActiveTopics(): List<Topic> {
            if (failuresRemaining > 0) {
                failuresRemaining -= 1
                error("load failed")
            }
            return topics
        }

        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> {
            subtopicCalls += 1
            return subtopics
        }

        override suspend fun getActiveQuestions(): List<Question> = error("Not used.")

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> {
            topicQuestionCalls += 1
            return questions
        }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> {
            subtopicQuestionCalls += 1
            error("N+1 query must not be used.")
        }

        override suspend fun getTopicById(topicId: String): Topic? =
            topics.firstOrNull { it.id == topicId }

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            subtopics.firstOrNull { it.id == subtopicId }

        // Used by LearningProgressService to resolve each answered question back to its topic
        // and subtopic when the screen shows observed accuracy.
        override suspend fun getQuestionById(questionId: String): Question? =
            questions.firstOrNull { it.id == questionId }
    }

    /**
     * Study content is what these tests are about, so history is empty: every accuracy comes back
     * null and the screen falls back to authored counts only.
     */
    private fun progressService(curriculum: CurriculumRepository): LearningProgressService =
        LearningProgressService(EmptyHistoryRepository, curriculum)

    private object EmptyHistoryRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }
}

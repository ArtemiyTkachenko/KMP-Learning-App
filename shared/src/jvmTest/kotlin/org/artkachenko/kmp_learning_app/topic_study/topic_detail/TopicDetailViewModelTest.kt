package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
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
        val viewModel = viewModel(topic.id, repository)

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
        val viewModel = viewModel("missing", repository)

        advanceUntilIdle()

        assertEquals(TopicDetailUiState.NotFound, viewModel.uiState.value)
        assertEquals(0, repository.subtopicCalls)
        assertEquals(0, repository.topicQuestionCalls)
    }

    @Test
    fun noQuestionsProducesUnavailablePracticeState() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val viewModel = viewModel(topic.id, FakeCurriculumRepository(topics = listOf(topic)))

        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.NoQuestions>(viewModel.uiState.value)
        assertEquals(topic, state.topic)
        assertNull(viewModel.topicPracticeScope())
        assertNull(viewModel.subtopicPracticeScope("unknown"))
    }

    @Test
    fun buildsTopicAndPopulatedSubtopicScopes() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_a", topic.id, "Subtopic A")
        val viewModel = viewModel(
            topic.id,
            FakeCurriculumRepository(
                topics = listOf(topic),
                subtopics = listOf(subtopic),
                questions = listOf(question("q1", topic.id, subtopic.id)),
            ),
        )

        advanceUntilIdle()

        // The screen contributes the stable scope only; the Practice Builder owns count, levels,
        // and source. Scope stays curriculum-driven and unchanged by learning context.
        assertEquals(AssessmentScope.Topic(topic.id), viewModel.topicPracticeScope())
        assertEquals(
            AssessmentScope.Subtopic(subtopic.id),
            viewModel.subtopicPracticeScope(subtopic.id),
        )
        assertNull(viewModel.subtopicPracticeScope("empty-or-unknown"))
    }

    @Test
    fun retryCanRecoverFromFailure() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val repository = FakeCurriculumRepository(
            topics = listOf(topic),
            failuresRemaining = 1,
            questions = listOf(question("q1", topic.id, "subtopic_a")),
        )
        val viewModel = viewModel(topic.id, repository)

        advanceUntilIdle()
        assertEquals(TopicDetailUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        assertEquals(TopicDetailUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun accuracyAndCoverageBothReachTheTopicAndItsSubtopics() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = listOf(
                Subtopic("subtopic_a", topic.id, "Subtopic A"),
                Subtopic("subtopic_b", topic.id, "Subtopic B"),
            ),
            questions = listOf(
                question("q1", topic.id, "subtopic_a"),
                question("q2", topic.id, "subtopic_a"),
                question("q3", topic.id, "subtopic_b"),
            ),
        )
        // subtopic_a: q1 answered twice across a retake and q2 once — three occurrences over two
        // unique questions.
        val history = historyRepository(
            completedAttempt("attempt_1", "q1" to true, "q2" to true, "q3" to true),
            completedAttempt("attempt_2", "q1" to false),
        )

        val viewModel = viewModel(topic.id, curriculum, history)
        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        val topicContext = assertNotNull(state.learningContext)
        // Accuracy counts every occurrence; coverage counts each stable Question ID once.
        assertEquals(3.0 / 4.0 * 100.0, topicContext.accuracyPercentage)
        assertEquals(3, topicContext.attemptedQuestionCount)
        assertEquals(3, topicContext.totalQuestionCount)

        val byId = state.subtopics.associateBy { it.subtopic.id }
        val subtopicA = assertNotNull(byId.getValue("subtopic_a").learningContext)
        assertEquals(2.0 / 3.0 * 100.0, subtopicA.accuracyPercentage)
        assertEquals(2, subtopicA.attemptedQuestionCount)
        assertEquals(2, subtopicA.totalQuestionCount)

        val subtopicB = assertNotNull(byId.getValue("subtopic_b").learningContext)
        assertEquals(100.0, subtopicB.accuracyPercentage)
        assertEquals(1, subtopicB.attemptedQuestionCount)
        assertEquals(1, subtopicB.totalQuestionCount)
    }

    @Test
    fun anUnseenTopicAndSubtopicReportCoverageWithoutFabricatingAccuracy() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = listOf(Subtopic("subtopic_a", topic.id, "Subtopic A")),
            questions = listOf(
                question("q1", topic.id, "subtopic_a"),
                question("q2", topic.id, "subtopic_a"),
            ),
        )

        val viewModel = viewModel(topic.id, curriculum)
        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        val topicContext = assertNotNull(state.learningContext)
        // Null, not 0%: never answered is a different statement from answered and got none right.
        assertNull(topicContext.accuracyPercentage)
        assertEquals(0, topicContext.attemptedQuestionCount)
        assertEquals(2, topicContext.totalQuestionCount)
        assertTrue(topicContext.isUnstudied)
        assertFalse(topicContext.isWeak)

        val subtopicContext = assertNotNull(state.subtopics.single().learningContext)
        assertNull(subtopicContext.accuracyPercentage)
        assertEquals(0, subtopicContext.attemptedQuestionCount)
        assertEquals(2, subtopicContext.totalQuestionCount)
        assertTrue(subtopicContext.isUnstudied)
    }

    @Test
    fun historicalAccuracySurvivesZeroCurrentCoverage() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val subtopic = Subtopic("subtopic_a", topic.id, "Subtopic A")
        // The answered questions still resolve to this Subtopic but are no longer ACTIVE, which is
        // what a retired or reclassified question looks like from here.
        val retired = listOf(
            question("q_retired_1", topic.id, subtopic.id),
            question("q_retired_2", topic.id, subtopic.id),
            question("q_retired_3", topic.id, subtopic.id),
        )
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = listOf(subtopic),
            questions = listOf(question("q_active", topic.id, subtopic.id)),
            retiredQuestions = retired,
        )
        val history = historyRepository(
            completedAttempt(
                "attempt",
                "q_retired_1" to true,
                "q_retired_2" to true,
                "q_retired_3" to false,
            ),
        )

        val viewModel = viewModel(topic.id, curriculum, history)
        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        val subtopicContext = assertNotNull(state.subtopics.single().learningContext)
        assertEquals(2.0 / 3.0 * 100.0, subtopicContext.accuracyPercentage)
        assertEquals(0, subtopicContext.attemptedQuestionCount)
        assertEquals(1, subtopicContext.totalQuestionCount)
        // Real historical evidence exists, so this is emphatically not "not studied yet".
        assertFalse(subtopicContext.isUnstudied)
    }

    @Test
    fun weakStatusIsCopiedFromTheDomainRatherThanInferredFromThePercentage() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = listOf(
                Subtopic("weak_sub", topic.id, "Weak Subtopic"),
                Subtopic("sparse_sub", topic.id, "Sparse Subtopic"),
            ),
            questions = listOf(
                question("q_weak_1", topic.id, "weak_sub"),
                question("q_weak_2", topic.id, "weak_sub"),
                question("q_weak_3", topic.id, "weak_sub"),
                question("q_sparse_1", topic.id, "sparse_sub"),
            ),
        )
        // weak_sub: three answers at 33%. sparse_sub: one answer at 0%, below the evidence
        // threshold the policy needs, so it is just as low without being weak.
        val history = historyRepository(
            completedAttempt(
                "attempt",
                "q_weak_1" to true,
                "q_weak_2" to false,
                "q_weak_3" to false,
                "q_sparse_1" to false,
            ),
        )

        val viewModel = viewModel(topic.id, curriculum, history)
        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        assertTrue(assertNotNull(state.learningContext).isWeak)

        val byId = state.subtopics.associateBy { it.subtopic.id }
        assertTrue(assertNotNull(byId.getValue("weak_sub").learningContext).isWeak)
        val sparse = assertNotNull(byId.getValue("sparse_sub").learningContext)
        assertEquals(0.0, sparse.accuracyPercentage)
        assertFalse(sparse.isWeak)
    }

    @Test
    fun curriculumStaysUsableWhileHistoryHasNotLoaded() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = listOf(Subtopic("subtopic_a", topic.id, "Subtopic A")),
            questions = listOf(question("q1", topic.id, "subtopic_a")),
        )

        val viewModel = viewModel(topic.id, curriculum, NeverReturningHistoryRepository)
        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        // No context at all rather than an empty one, so nothing on screen can claim the Topic
        // has never been studied when the app simply does not know yet.
        assertNull(state.learningContext)
        assertNull(state.subtopics.single().learningContext)
        // Practice is unaffected by the missing statistic.
        assertNotNull(viewModel.topicPracticeScope())
        assertNotNull(viewModel.subtopicPracticeScope("subtopic_a"))
    }

    @Test
    fun aHistoryFailureDoesNotBlockPractice() = runViewModelTest {
        val topic = Topic("topic_a", "Topic A")
        val curriculum = FakeCurriculumRepository(
            topics = listOf(topic),
            subtopics = listOf(Subtopic("subtopic_a", topic.id, "Subtopic A")),
            questions = listOf(question("q1", topic.id, "subtopic_a")),
        )

        val viewModel = viewModel(topic.id, curriculum, FailingHistoryRepository)
        advanceUntilIdle()

        val state = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
        assertNull(state.learningContext)
        assertNotNull(viewModel.topicPracticeScope())
    }

    @Test
    fun completedHistoryInvalidationRefreshesContextWithoutReloadingTheCurriculum() =
        runViewModelTest {
            val topic = Topic("topic_a", "Topic A")
            val curriculum = FakeCurriculumRepository(
                topics = listOf(topic),
                subtopics = listOf(Subtopic("subtopic_a", topic.id, "Subtopic A")),
                questions = listOf(question("q1", topic.id, "subtopic_a")),
            )
            val history = MutableHistoryRepository()
            val store = AssessmentHistoryStore(history, CoroutineScope(currentDispatcher()))
            val viewModel = TopicDetailViewModel(
                topicId = topic.id,
                curriculumRepository = curriculum,
                learningProgressService = LearningProgressService(history, curriculum),
                historyStore = store,
            )
            advanceUntilIdle()

            val before = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
            assertTrue(assertNotNull(before.learningContext).isUnstudied)
            val curriculumReads = curriculum.topicQuestionCalls

            history.attempts = listOf(completedAttempt("attempt", "q1" to true))
            store.invalidate()
            advanceUntilIdle()

            val after = assertIs<TopicDetailUiState.Content>(viewModel.uiState.value)
            val context = assertNotNull(after.learningContext)
            assertEquals(100.0, context.accuracyPercentage)
            assertEquals(1, context.attemptedQuestionCount)
            assertEquals(100.0, assertNotNull(after.subtopics.single().learningContext).accuracyPercentage)
            // The curriculum was not read again: only the analytics half refreshed.
            assertEquals(curriculumReads, curriculum.topicQuestionCalls)
        }

    private fun TestScope.viewModel(
        topicId: String,
        curriculum: CurriculumRepository,
        history: AssessmentRepository = EmptyHistoryRepository,
    ): TopicDetailViewModel =
        TopicDetailViewModel(
            topicId = topicId,
            curriculumRepository = curriculum,
            learningProgressService = LearningProgressService(history, curriculum),
            historyStore = AssessmentHistoryStore(history, CoroutineScope(currentDispatcher())),
        )

    private fun TestScope.currentDispatcher() = StandardTestDispatcher(testScheduler)

    private fun historyRepository(vararg attempts: TestAttempt): AssessmentRepository =
        object : AssessmentRepository {
            override suspend fun save(attempt: TestAttempt) = Unit
            override suspend fun getById(attemptId: String): TestAttempt? = attempts.firstOrNull()
            override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts.toList()
        }

    /**
     * One completed attempt. An attempt holds each Question at most once, so a Question answered
     * twice needs two attempts — which is how a retake reaches history in the app.
     */
    private fun completedAttempt(
        id: String,
        vararg answers: Pair<String, Boolean>,
    ): TestAttempt =
        TestAttempt(
            id = id,
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

    private class MutableHistoryRepository : AssessmentRepository {
        var attempts: List<TestAttempt> = emptyList()

        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts
    }

    private object FailingHistoryRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> = error("History unavailable")
    }

    /** Keeps the shared cache on its Loading value for the whole test. */
    private object NeverReturningHistoryRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> = awaitCancellation()
    }

    private object EmptyHistoryRepository : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) = Unit
        override suspend fun getById(attemptId: String): TestAttempt? = null
        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
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
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("answer_a"),
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com")),
    )

    private class FakeCurriculumRepository(
        private val topics: List<Topic>,
        private val subtopics: List<Subtopic> = emptyList(),
        private val questions: List<Question> = emptyList(),
        /** Answered in history and still resolvable, but outside the current ACTIVE bank. */
        retiredQuestions: List<Question> = emptyList(),
        private var failuresRemaining: Int = 0,
    ) : CurriculumRepository {
        private val questionsById = (questions + retiredQuestions).associateBy(Question::id)

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

        // Read once per LearningProgressService load to derive curriculum coverage.
        override suspend fun getActiveQuestions(): List<Question> = questions

        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> {
            topicQuestionCalls += 1
            return questions
        }

        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> {
            subtopicQuestionCalls += 1
            error("N+1 query must not be used.")
        }

        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            error("Level-filtered lookup is not used by topic detail.")

        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Level-filtered lookup is not used by topic detail.")

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Level-filtered lookup is not used by topic detail.")

        override suspend fun getTopicById(topicId: String): Topic? =
            topics.firstOrNull { it.id == topicId }

        override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
            subtopics.firstOrNull { it.id == subtopicId }

        // Used by LearningProgressService to resolve each answered question back to its topic
        // and subtopic when the screen shows observed accuracy.
        override suspend fun getQuestionById(questionId: String): Question? =
            questionsById[questionId]
    }
}

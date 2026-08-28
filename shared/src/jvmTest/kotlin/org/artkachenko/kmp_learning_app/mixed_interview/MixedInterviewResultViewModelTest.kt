package org.artkachenko.kmp_learning_app.mixed_interview

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class MixedInterviewResultViewModelTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun completedMixedAttemptUsesPersistedScoreAndEncounterOrderedTopicPerformance() = runTest {
        setMain(testScheduler)
        val questions = listOf(
            question("b1", "topic-b"), question("a1", "topic-a"),
            question("b2", "topic-b"), question("c1", "topic-c"),
        )
        val curriculum = FakeCurriculumRepository(
            questions,
            listOf(
                Topic("topic-a", "Topic A"),
                Topic("topic-b", "Retired Topic B", ContentStatus.DEPRECATED),
            ),
        )
        val source = completedAttempt(
            listOf(
                answered("b1", true), answered("a1", false), answered("b2", false),
                answered("c1", true), answered("missing", true),
            ),
            AssessmentScore(5, 3),
        )
        val viewModel = viewModel(FakeAssessmentRepository(source), curriculum)

        advanceUntilIdle()

        val state = content(viewModel)
        assertEquals(5, state.totalQuestions)
        assertEquals(3, state.correctAnswers)
        assertEquals(60.0, state.percentage)
        assertEquals(listOf("b1", "a1", "b2", "c1", "missing"), state.questions.map {
            when (it) {
                is ReviewQuestionItem.Available -> it.question.questionId
                is ReviewQuestionItem.Missing -> it.questionId
            }
        })
        assertEquals(
            listOf(
                TopicPerformanceUiModel("topic-b", "Retired Topic B", 2, 1, 50.0),
                TopicPerformanceUiModel("topic-a", "Topic A", 1, 0, 0.0),
                TopicPerformanceUiModel("topic-c", null, 1, 1, 100.0),
            ),
            state.topicPerformance,
        )
        assertEquals(RepeatInterviewState.Idle, state.repeatInterviewState)
    }

    @Test
    fun successfulRepeatPersistsBeforeEventPreservesSourceAndResetsAction() = runTest {
        setMain(testScheduler)
        val source = completedAttempt(listOf(answered("q1", true)), AssessmentScore(1, 1))
        val repository = FakeAssessmentRepository(source)
        val curriculum = FakeCurriculumRepository(
            listOf(question("q1", "topic-a"), question("q2", "topic-b")),
            listOf(Topic("topic-a", "Topic A")),
        )
        val viewModel = viewModel(repository, curriculum, retakeId = "retake")
        advanceUntilIdle()
        val event = async { viewModel.events.first() }

        viewModel.repeatInterview()
        assertEquals(RepeatInterviewState.Creating, content(viewModel).repeatInterviewState)
        advanceUntilIdle()

        val createdEvent = assertIs<MixedInterviewResultEvent.RetakeCreated>(event.await())
        val retake = requireNotNull(repository.getById(createdEvent.attemptId))
        assertEquals("retake", createdEvent.attemptId)
        assertNotEquals(source.id, retake.id)
        assertEquals(source.config, retake.config)
        assertEquals(AssessmentStatus.IN_PROGRESS, retake.status)
        assertTrue(retake.questionAttempts.all { it.answerState == QuestionAnswerState.Unanswered })
        assertNull(retake.score)
        assertNull(retake.completedAt)
        assertEquals(source, repository.getById(source.id))
        assertEquals(RepeatInterviewState.Idle, content(viewModel).repeatInterviewState)
    }

    @Test
    fun duplicateClicksWhileCreationIsRunningCreateOnlyOneRetake() = runTest {
        setMain(testScheduler)
        val source = completedAttempt(listOf(answered("q1", true)), AssessmentScore(1, 1))
        val repository = FakeAssessmentRepository(source)
        val selectionGate = CompletableDeferred<Unit>()
        val curriculum = FakeCurriculumRepository(
            listOf(question("q1", "topic-a")),
            listOf(Topic("topic-a", "Topic A")),
            activeSelectionGate = selectionGate,
        )
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()

        viewModel.repeatInterview()
        viewModel.repeatInterview()
        assertEquals(RepeatInterviewState.Creating, content(viewModel).repeatInterviewState)
        runCurrent()
        assertEquals(1, curriculum.activeQuestionCalls)

        selectionGate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.saveCalls)
    }

    @Test
    fun sourceMissingAndNoQuestionsAreExplicitAndDoNotNavigate() = runTest {
        setMain(testScheduler)
        val source = completedAttempt(listOf(answered("q1", true)), AssessmentScore(1, 1))
        val repository = FakeAssessmentRepository(source)
        val curriculum = FakeCurriculumRepository(
            listOf(question("q1", "topic")),
            listOf(Topic("topic", "Topic")),
        )
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()
        repository.attempts.remove(source.id)
        val event = async { viewModel.events.first() }

        viewModel.repeatInterview()
        advanceUntilIdle()
        assertEquals(RepeatInterviewState.SourceAttemptNotFound, content(viewModel).repeatInterviewState)
        assertFalse(event.isCompleted)

        repository.attempts[source.id] = source
        curriculum.activeQuestions = emptyList()
        viewModel.repeatInterview()
        advanceUntilIdle()
        assertEquals(RepeatInterviewState.NoEligibleQuestions, content(viewModel).repeatInterviewState)
        assertEquals(0, repository.saveCalls)
        assertFalse(event.isCompleted)
        event.cancel()
    }

    @Test
    fun unexpectedFailureKeepsContentAndCanBeRetriedSuccessfully() = runTest {
        setMain(testScheduler)
        val source = completedAttempt(listOf(answered("q1", true)), AssessmentScore(1, 1))
        val repository = FakeAssessmentRepository(source)
        val curriculum = FakeCurriculumRepository(
            listOf(question("q1", "topic")),
            listOf(Topic("topic", "Topic")),
            failNextActiveSelection = true,
        )
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()

        viewModel.repeatInterview()
        advanceUntilIdle()
        val failed = content(viewModel)
        assertEquals(RepeatInterviewState.Error, failed.repeatInterviewState)
        assertEquals(1, failed.totalQuestions)
        assertEquals(1, failed.questions.size)

        val event = async { viewModel.events.first() }
        viewModel.repeatInterview()
        advanceUntilIdle()
        assertEquals("retake", assertIs<MixedInterviewResultEvent.RetakeCreated>(event.await()).attemptId)
    }

    @Test
    fun missingInProgressAndWrongConfigAttemptsHaveExplicitStates() = runTest {
        setMain(testScheduler)
        val curriculum = FakeCurriculumRepository(emptyList(), emptyList())
        val repository = FakeAssessmentRepository()
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.AttemptNotFound>(viewModel.uiState.value)

        repository.attempts[SourceId] = inProgressAttempt()
        viewModel.retry()
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.NotCompleted>(viewModel.uiState.value)

        repository.attempts[SourceId] = completedAttempt(
            listOf(answered("q", true)),
            AssessmentScore(1, 1),
            AssessmentConfig.Focused(AssessmentScope.Topic("topic"), 1),
        )
        viewModel.retry()
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun loadFailureCanBeRetried() = runTest {
        setMain(testScheduler)
        val repository = FakeAssessmentRepository(
            completedAttempt(listOf(answered("q", true)), AssessmentScore(1, 1)),
            failNextLoad = true,
        )
        val curriculum = FakeCurriculumRepository(
            listOf(question("q", "topic")),
            listOf(Topic("topic", "Topic")),
        )
        val viewModel = viewModel(repository, curriculum)
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.Error>(viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()
        assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)
    }

    private fun setMain(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    private fun content(viewModel: MixedInterviewResultViewModel) =
        assertIs<MixedInterviewResultUiState.Content>(viewModel.uiState.value)

    private fun viewModel(
        repository: FakeAssessmentRepository,
        curriculum: FakeCurriculumRepository,
        retakeId: String = "retake",
    ) = MixedInterviewResultViewModel(
        attemptId = SourceId,
        assessmentRepository = repository,
        curriculumRepository = curriculum,
        assessmentReviewLoader = AssessmentReviewLoader(curriculum),
        assessmentRetakeService = AssessmentRetakeService(
            assessmentRepository = repository,
            assessmentEngine = AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(curriculum, randomize = { it }),
                generateAttemptId = { retakeId },
                now = { Instant.fromEpochMilliseconds(3) },
            ),
        ),
    )

    private fun inProgressAttempt() = TestAttempt(
        id = SourceId,
        config = AssessmentConfig.Mixed(1),
        questionAttempts = listOf(QuestionAttempt("q")),
        status = AssessmentStatus.IN_PROGRESS,
        startedAt = Instant.fromEpochMilliseconds(1),
    )

    private fun completedAttempt(
        questionAttempts: List<QuestionAttempt>,
        score: AssessmentScore,
        config: AssessmentConfig = AssessmentConfig.Mixed(questionAttempts.size),
    ) = TestAttempt(
        id = SourceId,
        config = config,
        questionAttempts = questionAttempts,
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.fromEpochMilliseconds(1),
        completedAt = Instant.fromEpochMilliseconds(2),
        score = score,
    )

    private fun answered(questionId: String, isCorrect: Boolean) = QuestionAttempt(
        questionId,
        QuestionAnswerState.Answered(setOf("a"), isCorrect),
    )

    private fun question(id: String, topicId: String) = Question(
        id = id,
        topicId = topicId,
        subtopicId = "subtopic",
        text = "Question $id",
        answers = listOf(AnswerOption("a", "Answer A"), AnswerOption("b", "Answer B")),
        correctAnswerIds = listOf("a"),
        explanation = "Explanation $id",
        sources = listOf(SourceReference("Source $id", "https://example.com/$id")),
    )

    private class FakeAssessmentRepository(
        source: TestAttempt? = null,
        var failNextLoad: Boolean = false,
    ) : AssessmentRepository {
        val attempts = mutableMapOf<String, TestAttempt>()
        var saveCalls = 0

        init {
            source?.let { attempts[it.id] = it }
        }

        override suspend fun save(attempt: TestAttempt) {
            saveCalls++
            attempts[attempt.id] = attempt
        }

        override suspend fun getById(attemptId: String): TestAttempt? {
            if (failNextLoad) {
                failNextLoad = false
                error("load failed")
            }
            return attempts[attemptId]
        }

        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }

    private class FakeCurriculumRepository(
        private val questions: List<Question>,
        private val topics: List<Topic>,
        private val activeSelectionGate: CompletableDeferred<Unit>? = null,
        var failNextActiveSelection: Boolean = false,
    ) : CurriculumRepository {
        var activeQuestions: List<Question> = questions
        var activeQuestionCalls = 0

        override suspend fun getActiveTopics(): List<Topic> = error("Not used")
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = error("Not used")
        override suspend fun getActiveQuestions(): List<Question> {
            activeQuestionCalls++
            if (failNextActiveSelection) {
                failNextActiveSelection = false
                error("selection failed")
            }
            activeSelectionGate?.await()
            return activeQuestions
        }
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = error("Not used")
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = error("Not used")
        override suspend fun getTopicById(topicId: String): Topic? = topics.firstOrNull { it.id == topicId }
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
        override suspend fun getQuestionById(questionId: String): Question? = questions.firstOrNull { it.id == questionId }
    }
}

private const val SourceId = "attempt"

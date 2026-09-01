package org.artkachenko.kmp_learning_app.assessment_taking

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeResult
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeService
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentSessionLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class AssessmentTakingViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsAndPersistsActualQuestionCount() = runViewModelTest {
        val questions = listOf(
            question("single", listOf("a")),
            question("multi", listOf("a", "c"), selectionMode = AnswerSelectionMode.MULTIPLE),
        )
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(questions, repository)

        advanceUntilIdle()

        val state = assertIs<AssessmentTakingUiState.Content>(viewModel.uiState.value)
        assertEquals(1, state.questionNumber)
        assertEquals(2, state.totalQuestions)
        assertEquals(1, repository.savedAttempts.size)
        assertTrue(repository.savedAttempts.single().questionAttempts.all { it.answerState is QuestionAnswerState.Unanswered })
    }

    @Test
    fun singleAnswerSelectionReplacesPreviousChoice() = runViewModelTest {
        val viewModel = viewModel(listOf(question("single", listOf("a"))))
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        assertEquals(setOf("a"), content(viewModel).selectedAnswerIds)
        viewModel.selectAnswer("b")
        assertEquals(setOf("b"), content(viewModel).selectedAnswerIds)
    }

    @Test
    fun multipleAnswerSelectionTogglesStableIds() = runViewModelTest {
        val viewModel = viewModel(
            listOf(question("multi", listOf("a", "c"), selectionMode = AnswerSelectionMode.MULTIPLE)),
        )
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.selectAnswer("c")
        assertEquals(setOf("a", "c"), content(viewModel).selectedAnswerIds)
        viewModel.selectAnswer("a")
        assertEquals(setOf("c"), content(viewModel).selectedAnswerIds)
    }

    @Test
    fun authoredModesReachUiWithoutUsingCorrectAnswerCardinality() = runViewModelTest {
        val single = viewModel(listOf(question("single", listOf("a"))))
        advanceUntilIdle()
        assertEquals(AnswerSelectionMode.SINGLE, content(single).question.selectionMode)

        val multiple = viewModel(
            listOf(
                question(
                    id = "multiple-one-correct",
                    correctIds = listOf("a"),
                    selectionMode = AnswerSelectionMode.MULTIPLE,
                ),
            ),
        )
        advanceUntilIdle()
        assertEquals(AnswerSelectionMode.MULTIPLE, content(multiple).question.selectionMode)
    }

    @Test
    fun multipleSelectionWithOneCorrectAnswerStillAllowsSeveralSelections() = runViewModelTest {
        val viewModel = viewModel(
            listOf(
                question(
                    id = "multiple-one-correct",
                    correctIds = listOf("a"),
                    selectionMode = AnswerSelectionMode.MULTIPLE,
                ),
            ),
        )
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.selectAnswer("b")

        assertEquals(setOf("a", "b"), content(viewModel).selectedAnswerIds)
    }

    @Test
    fun unknownAnswerDoesNotEnterPendingState() = runViewModelTest {
        val viewModel = viewModel(listOf(question("single", listOf("a"))))
        advanceUntilIdle()

        viewModel.selectAnswer("unknown")

        assertEquals(emptySet(), content(viewModel).selectedAnswerIds)
    }

    @Test
    fun submissionUsesEngineAndAdvancesOnlyAfterSave() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(
            questions = listOf(
                question("single", listOf("a")),
                question("multi", listOf("a", "c"), selectionMode = AnswerSelectionMode.MULTIPLE),
            ),
            repository = repository,
        )
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.submitAnswer()
        advanceUntilIdle()

        val state = content(viewModel)
        assertEquals(2, state.questionNumber)
        assertEquals(QuestionAnswerState.Unanswered, repository.savedAttempts.last().questionAttempts[1].answerState)
        assertEquals(
            QuestionAnswerState.Answered(setOf("a"), isCorrect = true),
            repository.savedAttempts.last().questionAttempts.first().answerState,
        )
    }

    @Test
    fun submissionFailureKeepsQuestionAndSelectionForRetry() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(listOf(question("single", listOf("a"))), repository)
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        repository.failNextSave = true
        viewModel.submitAnswer()
        advanceUntilIdle()

        val failed = content(viewModel)
        assertEquals(1, failed.questionNumber)
        assertEquals(setOf("a"), failed.selectedAnswerIds)
        assertTrue(failed.submissionFailed)
        assertEquals(QuestionAnswerState.Unanswered, repository.savedAttempts.single().questionAttempts.single().answerState)

        repository.failNextSave = false
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertIs<AssessmentTakingUiState.ReadyToComplete>(viewModel.uiState.value)
        assertEquals(
            QuestionAnswerState.Answered(setOf("a"), isCorrect = true),
            repository.savedAttempts.last().questionAttempts.single().answerState,
        )
    }

    @Test
    fun finalSubmissionLeavesAttemptInProgressWithoutScore() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(listOf(question("single", listOf("a"))), repository)
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertIs<AssessmentTakingUiState.ReadyToComplete>(viewModel.uiState.value)
        val saved = repository.savedAttempts.last()
        assertEquals(org.artkachenko.kmp_learning_app.assessment.AssessmentStatus.IN_PROGRESS, saved.status)
        assertEquals(null, saved.score)
    }

    @Test
    fun completionExplicitlyCreatesAndPersistsCompletedAttempt() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(listOf(question("single", listOf("a"))), repository)
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.submitAnswer()
        advanceUntilIdle()
        viewModel.completeAssessment()
        advanceUntilIdle()

        assertIs<AssessmentTakingUiState.CompletionSucceeded>(viewModel.uiState.value)
        val completed = repository.savedAttempts.last()
        assertEquals(AssessmentStatus.COMPLETED, completed.status)
        assertEquals(1, completed.score?.totalQuestions)
        assertEquals(1, completed.score?.correctAnswers)
        assertTrue(completed.completedAt != null)
    }

    @Test
    fun completionFailureKeepsReadyStateAndCanBeRetried() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(listOf(question("single", listOf("a"))), repository)
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.submitAnswer()
        advanceUntilIdle()
        repository.failNextSave = true
        viewModel.completeAssessment()
        advanceUntilIdle()

        val failed = assertIs<AssessmentTakingUiState.ReadyToComplete>(viewModel.uiState.value)
        assertTrue(failed.completionFailed)
        assertEquals(AssessmentStatus.IN_PROGRESS, repository.savedAttempts.last().status)
        assertEquals(null, repository.savedAttempts.last().score)

        viewModel.completeAssessment()
        advanceUntilIdle()
        assertIs<AssessmentTakingUiState.CompletionSucceeded>(viewModel.uiState.value)
        assertEquals(AssessmentStatus.COMPLETED, repository.savedAttempts.last().status)
    }

    @Test
    fun existingAttemptLoadsWithoutStartingOrInitialSavingAgain() = runViewModelTest {
        val questions = listOf(question("retake-question", listOf("a")))
        val repository = RecordingAssessmentRepository()
        repository.savedAttempts += TestAttempt(
            id = "retake-1",
            config = AssessmentConfig.Focused(AssessmentScope.Topic("topic"), 10),
            questionAttempts = listOf(org.artkachenko.kmp_learning_app.assessment.QuestionAttempt("retake-question")),
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = Instant.fromEpochMilliseconds(1_000),
        )
        val curriculum = FakeCurriculumRepository(questions)
        val viewModel = AssessmentTakingViewModel(
            launch = AssessmentTakingLaunch.ExistingAttempt("retake-1"),
            assessmentEngine = AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(curriculum, randomize = { it }),
                generateAttemptId = { error("start must not be called") },
                now = { Instant.fromEpochMilliseconds(2_000) },
            ),
            assessmentRepository = repository,
            assessmentSessionLoader = AssessmentSessionLoader(repository, curriculum),
            historyStore = AssessmentHistoryStore(repository, backgroundScope),
        )
        advanceUntilIdle()

        assertEquals("retake-1", content(viewModel).attemptId)
        assertEquals(1, repository.savedAttempts.size)
    }

    @Test
    fun newMixedAssessmentUsesBalancedSelectionAndPersistsMixedConfig() = runViewModelTest {
        val questions = listOf(
            question("a1", listOf("a"), topicId = "topic-a"),
            question("a2", listOf("a"), topicId = "topic-a"),
            question("b1", listOf("a"), topicId = "topic-b"),
            question("c1", listOf("a"), topicId = "topic-c"),
        )
        val repository = RecordingAssessmentRepository()
        val config = AssessmentConfig.Mixed(questionCount = 3)
        val viewModel = viewModel(
            questions = questions,
            repository = repository,
            config = config,
        )

        advanceUntilIdle()

        val state = content(viewModel)
        assertEquals("a1", state.question.id)
        assertEquals(3, state.totalQuestions)
        val saved = repository.savedAttempts.single()
        assertEquals(config, saved.config)
        assertEquals(AssessmentStatus.IN_PROGRESS, saved.status)
        assertEquals(null, saved.score)
        assertEquals(listOf("a1", "b1", "c1"), saved.questionAttempts.map { it.questionId })
    }

    @Test
    fun newMixedAssessmentWithoutEligibleQuestionsShowsNoQuestions() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(
            questions = emptyList(),
            repository = repository,
            config = AssessmentConfig.Mixed(questionCount = 3),
        )

        advanceUntilIdle()

        assertIs<AssessmentTakingUiState.NoQuestions>(viewModel.uiState.value)
        assertTrue(repository.savedAttempts.isEmpty())
    }

    @Test
    fun mixedSubmissionUsesSharedEngineAndAdvances() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(
            questions = listOf(
                question("a1", listOf("a"), topicId = "topic-a"),
                question("b1", listOf("a"), topicId = "topic-b"),
            ),
            repository = repository,
            config = AssessmentConfig.Mixed(questionCount = 2),
        )
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.submitAnswer()
        advanceUntilIdle()

        assertEquals(2, content(viewModel).questionNumber)
        assertEquals(
            QuestionAnswerState.Answered(setOf("a"), isCorrect = true),
            repository.savedAttempts.last().questionAttempts.first().answerState,
        )
    }

    @Test
    fun existingMixedAttemptUsesPersistedConfigAndResumesAtFirstUnansweredQuestion() = runViewModelTest {
        val questions = listOf(
            question("q1", listOf("a"), topicId = "topic-a"),
            question("q2", listOf("a"), topicId = "topic-b"),
            question("q3", listOf("a"), topicId = "topic-c"),
        )
        val repository = RecordingAssessmentRepository()
        val config = AssessmentConfig.Mixed(questionCount = 20)
        repository.savedAttempts += TestAttempt(
            id = "mixed-existing",
            config = config,
            questionAttempts = listOf(
                QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("a"), isCorrect = true)),
                QuestionAttempt("q2"),
                QuestionAttempt("q3"),
            ),
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = Instant.fromEpochMilliseconds(1_000),
        )
        val curriculum = FakeCurriculumRepository(questions)
        val viewModel = AssessmentTakingViewModel(
            launch = AssessmentTakingLaunch.ExistingAttempt("mixed-existing"),
            assessmentEngine = AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(curriculum, randomize = { it }),
                generateAttemptId = { error("start must not be called") },
                now = { Instant.fromEpochMilliseconds(2_000) },
            ),
            assessmentRepository = repository,
            assessmentSessionLoader = AssessmentSessionLoader(repository, curriculum),
            historyStore = AssessmentHistoryStore(repository, backgroundScope),
        )

        advanceUntilIdle()

        val state = content(viewModel)
        assertEquals("mixed-existing", state.attemptId)
        assertEquals("q2", state.question.id)
        assertEquals(2, state.questionNumber)
        assertEquals(3, state.totalQuestions)
        assertEquals(config, repository.savedAttempts.single().config)
        assertEquals(1, repository.savedAttempts.size)
    }

    @Test
    fun mixedRetakeIsSelectedOnceThenReopenedWithoutStartingAgain() = runViewModelTest {
        val questions = listOf(
            question("q1", listOf("a"), topicId = "topic-a"),
            question("q2", listOf("a"), topicId = "topic-b"),
        )
        val curriculum = FakeCurriculumRepository(questions)
        val repository = RecordingAssessmentRepository()
        val source = TestAttempt(
            id = "source",
            config = AssessmentConfig.Mixed(questionCount = 2),
            questionAttempts = listOf(
                QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("a"), true)),
                QuestionAttempt("q2", QuestionAnswerState.Answered(setOf("a"), true)),
            ),
            status = AssessmentStatus.COMPLETED,
            startedAt = Instant.fromEpochMilliseconds(1_000),
            completedAt = Instant.fromEpochMilliseconds(2_000),
            score = AssessmentScore(2, 2),
        )
        repository.savedAttempts += source
        var generatedIds = 0
        val engine = AssessmentEngine(
            questionSelector = AssessmentQuestionSelector(curriculum, randomize = { it }),
            generateAttemptId = {
                generatedIds++
                if (generatedIds == 1) "retake" else error("start must not be called twice")
            },
            now = { Instant.fromEpochMilliseconds(3_000) },
        )
        val retake = assertIs<AssessmentRetakeResult.Created>(
            AssessmentRetakeService(repository, engine).createRetake(source.id),
        ).session.attempt
        assertEquals(1, curriculum.activeQuestionCalls)

        val viewModel = AssessmentTakingViewModel(
            launch = AssessmentTakingLaunch.ExistingAttempt(retake.id),
            assessmentEngine = engine,
            assessmentRepository = repository,
            assessmentSessionLoader = AssessmentSessionLoader(repository, curriculum),
            historyStore = AssessmentHistoryStore(repository, backgroundScope),
        )
        advanceUntilIdle()

        assertEquals(retake.id, content(viewModel).attemptId)
        assertEquals(1, curriculum.activeQuestionCalls)
        assertEquals(1, generatedIds)
    }

    @Test
    fun existingMixedAttemptWithAllAnswersIsReadyToComplete() = runViewModelTest {
        val questions = listOf(question("q1", listOf("a"), topicId = "topic-a"))
        val repository = RecordingAssessmentRepository()
        repository.savedAttempts += TestAttempt(
            id = "mixed-ready",
            config = AssessmentConfig.Mixed(questionCount = 20),
            questionAttempts = listOf(
                QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("a"), isCorrect = true)),
            ),
            status = AssessmentStatus.IN_PROGRESS,
            startedAt = Instant.fromEpochMilliseconds(1_000),
        )
        val curriculum = FakeCurriculumRepository(questions)
        val viewModel = AssessmentTakingViewModel(
            launch = AssessmentTakingLaunch.ExistingAttempt("mixed-ready"),
            assessmentEngine = AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(curriculum, randomize = { it }),
                generateAttemptId = { error("start must not be called") },
                now = { Instant.fromEpochMilliseconds(2_000) },
            ),
            assessmentRepository = repository,
            assessmentSessionLoader = AssessmentSessionLoader(repository, curriculum),
            historyStore = AssessmentHistoryStore(repository, backgroundScope),
        )

        advanceUntilIdle()

        val state = assertIs<AssessmentTakingUiState.ReadyToComplete>(viewModel.uiState.value)
        assertEquals("mixed-ready", state.attemptId)
        assertEquals(1, repository.savedAttempts.size)
    }

    @Test
    fun restoredCompletedAttemptContinuesToResultWithoutStartingOrSaving() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        repository.savedAttempts += TestAttempt(
            id = "mixed-completed",
            config = AssessmentConfig.Mixed(questionCount = 20),
            questionAttempts = listOf(
                QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("a"), isCorrect = true)),
            ),
            status = AssessmentStatus.COMPLETED,
            startedAt = Instant.fromEpochMilliseconds(1_000),
            completedAt = Instant.fromEpochMilliseconds(2_000),
            score = AssessmentScore(totalQuestions = 1, correctAnswers = 1),
        )
        val curriculum = FakeCurriculumRepository(emptyList())
        val viewModel = AssessmentTakingViewModel(
            launch = AssessmentTakingLaunch.ExistingAttempt("mixed-completed"),
            assessmentEngine = AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(curriculum, randomize = { it }),
                generateAttemptId = { error("start must not be called") },
                now = { Instant.fromEpochMilliseconds(3_000) },
            ),
            assessmentRepository = repository,
            assessmentSessionLoader = AssessmentSessionLoader(repository, curriculum),
            historyStore = AssessmentHistoryStore(repository, backgroundScope),
        )

        advanceUntilIdle()

        assertEquals(
            AssessmentTakingUiState.CompletionSucceeded("mixed-completed"),
            viewModel.uiState.value,
        )
        assertEquals(1, repository.savedAttempts.size)
    }

    @Test
    fun mixedAssessmentUsesSharedExplicitCompletion() = runViewModelTest {
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(
            questions = listOf(question("mixed-question", listOf("a"), topicId = "topic-a")),
            repository = repository,
            config = AssessmentConfig.Mixed(questionCount = 20),
        )
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.submitAnswer()
        advanceUntilIdle()
        assertIs<AssessmentTakingUiState.ReadyToComplete>(viewModel.uiState.value)

        viewModel.completeAssessment()
        advanceUntilIdle()

        assertIs<AssessmentTakingUiState.CompletionSucceeded>(viewModel.uiState.value)
        val completed = repository.savedAttempts.last()
        assertIs<AssessmentConfig.Mixed>(completed.config)
        assertEquals(AssessmentStatus.COMPLETED, completed.status)
        assertEquals(1, completed.score?.correctAnswers)
        assertTrue(completed.completedAt != null)
    }

    private fun viewModel(
        questions: List<Question>,
        repository: RecordingAssessmentRepository = RecordingAssessmentRepository(),
        config: AssessmentConfig = AssessmentConfig.Focused(
            scope = AssessmentScope.Topic("topic"),
            questionCount = 10,
        ),
    ) = AssessmentTakingViewModel(
        launch = AssessmentTakingLaunch.New(config),
        assessmentEngine = AssessmentEngine(
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = FakeCurriculumRepository(questions),
                randomize = { it },
            ),
            generateAttemptId = { "attempt-1" },
            now = { Instant.fromEpochMilliseconds(1_000) },
        ),
        assessmentRepository = repository,
        assessmentSessionLoader = AssessmentSessionLoader(
            assessmentRepository = repository,
            curriculumRepository = FakeCurriculumRepository(questions),
        ),
        historyStore = AssessmentHistoryStore(repository, CoroutineScope(SupervisorJob())),
    )

    private fun content(viewModel: AssessmentTakingViewModel) =
        assertIs<AssessmentTakingUiState.Content>(viewModel.uiState.value)

    private fun runViewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun question(
        id: String,
        correctIds: List<String>,
        topicId: String = "topic",
        selectionMode: AnswerSelectionMode = AnswerSelectionMode.SINGLE,
    ) = Question(
        id = id,
        topicId = topicId,
        subtopicId = "subtopic",
        text = "Question $id",
        answers = listOf(
            AnswerOption("a", "Answer A"),
            AnswerOption("b", "Answer B"),
            AnswerOption("c", "Answer C"),
        ),
        selectionMode = selectionMode,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = correctIds,
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com")),
    )

    private class FakeCurriculumRepository(
        private val questions: List<Question>,
    ) : CurriculumRepository {
        var activeQuestionCalls = 0

        override suspend fun getActiveTopics(): List<Topic> = listOf(Topic("topic", "Topic"))
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = emptyList()
        override suspend fun getActiveQuestions(): List<Question> {
            activeQuestionCalls++
            return questions
        }
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = questions
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = questions
        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
            error("Level-filtered lookup is not used by assessment taking.")
        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Level-filtered lookup is not used by assessment taking.")
        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Level-filtered lookup is not used by assessment taking.")
        override suspend fun getTopicById(topicId: String): Topic? = null
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
        override suspend fun getQuestionById(questionId: String): Question? = questions.firstOrNull { it.id == questionId }
    }

    private class RecordingAssessmentRepository(
        var failNextSave: Boolean = false,
    ) : AssessmentRepository {
        val savedAttempts = mutableListOf<TestAttempt>()

        override suspend fun save(attempt: TestAttempt) {
            if (failNextSave) {
                failNextSave = false
                error("save failed")
            }
            savedAttempts += attempt
        }

        override suspend fun getById(attemptId: String): TestAttempt? =
            savedAttempts.lastOrNull { it.id == attemptId }

        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }
}

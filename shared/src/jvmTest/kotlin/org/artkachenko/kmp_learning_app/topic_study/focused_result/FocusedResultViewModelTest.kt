package org.artkachenko.kmp_learning_app.topic_study.focused_result

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
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
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.ContentStatus
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.saved_questions.FakeSavedQuestionRepository
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState
import org.artkachenko.kmp_learning_app.saved_questions.savedQuestionStateHolder

@OptIn(ExperimentalCoroutinesApi::class)
internal class FocusedResultViewModelTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsPersistedScoreAndQuestionsInAttemptOrder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val questions = listOf(question("q3"), question("q1"), question("q2"))
        val repository = FakeAssessmentRepository(completedAttempt(questions.map { it.id }, 2))
        val curriculum = FakeCurriculumRepository(questions)
        val viewModel = FocusedResultViewModel(
            "attempt",
            repository,
            AssessmentReviewLoader(curriculum),
            retakeService(repository, questions),
            savedQuestionStateHolder(),
        )
        advanceUntilIdle()

        val state = assertIs<FocusedResultUiState.Content>(viewModel.uiState.value)
        assertEquals(2, state.correctAnswers)
        assertEquals(3, state.totalQuestions)
        assertEquals(listOf("q3", "q1", "q2"), state.questions.map { (it as ReviewQuestionItem.Available).question.questionId })
        assertEquals(listOf("q3", "q1", "q2"), curriculum.lookups)
    }

    @Test
    fun mapsSelectedAndCorrectAnswersAndPreservesSources() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val q = question("q", correctIds = listOf("a", "c"), selectedIds = setOf("a", "b"))
        val viewModel = FocusedResultViewModel(
            "attempt",
            FakeAssessmentRepository(completedAttempt(listOf("q"), 0, selectedIds = setOf("a", "b"))),
            reviewLoader(listOf(q)),
            retakeService(FakeAssessmentRepository(completedAttempt(listOf("q"), 0)), listOf(q)),
            savedQuestionStateHolder(),
        )
        advanceUntilIdle()

        val item = assertIs<ReviewQuestionItem.Available>(assertIs<FocusedResultUiState.Content>(viewModel.uiState.value).questions.single()).question
        assertTrue(item.answers.first { it.id == "a" }.wasSelected)
        assertTrue(item.answers.first { it.id == "a" }.isCorrectAnswer)
        assertTrue(item.answers.first { it.id == "b" }.wasSelected)
        assertTrue(item.answers.first { it.id == "b" }.isCorrectAnswer.not())
        assertTrue(item.answers.first { it.id == "c" }.isCorrectAnswer)
        assertEquals(listOf("Source B", "Source A"), item.sources.map { it.title })
    }

    @Test
    fun missingQuestionDoesNotHideOtherResults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = FocusedResultViewModel(
            "attempt",
            FakeAssessmentRepository(completedAttempt(listOf("missing", "q"), 1)),
            reviewLoader(listOf(question("q"))),
            retakeService(FakeAssessmentRepository(completedAttempt(listOf("q"), 1)), listOf(question("q"))),
            savedQuestionStateHolder(),
        )
        advanceUntilIdle()

        val items = assertIs<FocusedResultUiState.Content>(viewModel.uiState.value).questions
        assertIs<ReviewQuestionItem.Missing>(items.first())
        assertIs<ReviewQuestionItem.Available>(items[1])
    }

    @Test
    fun incompleteAndMissingAttemptsHaveExplicitStates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeAssessmentRepository(null)
        val missing = FocusedResultViewModel(
            "attempt",
            repo,
            reviewLoader(emptyList()),
            retakeService(repo, emptyList()),
            savedQuestionStateHolder(),
        )
        advanceUntilIdle()
        assertIs<FocusedResultUiState.AttemptNotFound>(missing.uiState.value)

        repo.attempt = TestAttempt(
            id = "attempt", config = config(), questionAttempts = listOf(QuestionAttempt("q")),
            status = AssessmentStatus.IN_PROGRESS, startedAt = Instant.fromEpochMilliseconds(1),
        )
        missing.retry()
        advanceUntilIdle()
        assertIs<FocusedResultUiState.NotCompleted>(missing.uiState.value)
    }

    @Test
    fun repeatPracticeEmitsNewAttemptEvent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val question = question("q")
        val repository = FakeAssessmentRepository(completedAttempt(listOf("q"), 1))
        val viewModel = FocusedResultViewModel(
            "attempt",
            repository,
            reviewLoader(listOf(question)),
            retakeService(repository, listOf(question)),
            savedQuestionStateHolder(),
        )
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.repeatPractice()
        advanceUntilIdle()
        assertEquals(FocusedResultEvent.RetakeCreated("retake"), event.await())
    }

    /**
     * The Focused integration boundary only: saving is the shared holder's behaviour, which its own
     * tests cover. What matters here is that the result forwards the exact Question ID it is
     * showing, and that its own state is untouched by the mutation.
     */
    @Test
    fun savingAnAvailableQuestionPersistsThatIdAndLeavesTheResultUnchanged() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAssessmentRepository(completedAttempt(listOf("q"), 1))
        val savedRepository = FakeSavedQuestionRepository()
        val viewModel = FocusedResultViewModel(
            "attempt",
            repository,
            reviewLoader(listOf(question("q"))),
            retakeService(repository, listOf(question("q"))),
            savedQuestionStateHolder(savedRepository),
        )
        advanceUntilIdle()
        val before = assertIs<FocusedResultUiState.Content>(viewModel.uiState.value)

        viewModel.toggleSaved("q")
        advanceUntilIdle()

        assertEquals(listOf("q"), savedRepository.saveCalls)
        assertEquals(
            setOf("q"),
            assertIs<SavedQuestionsState.Loaded>(viewModel.savedQuestions.value).savedQuestionIds,
        )
        // Score, review content, and the repeat-practice state are all as they were.
        assertEquals(before, viewModel.uiState.value)
    }

    /**
     * A stable ID is known for a missing historical Question, but it is not review content the
     * learner can act on, so the mutation boundary refuses it even if a caller asks.
     */
    @Test
    fun aQuestionThisResultCannotShowIsNeverSaved() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAssessmentRepository(completedAttempt(listOf("missing", "q"), 1))
        val savedRepository = FakeSavedQuestionRepository()
        val viewModel = FocusedResultViewModel(
            "attempt",
            repository,
            reviewLoader(listOf(question("q"))),
            retakeService(repository, listOf(question("q"))),
            savedQuestionStateHolder(savedRepository),
        )
        advanceUntilIdle()

        viewModel.toggleSaved("missing")
        viewModel.toggleSaved("never_in_this_result")
        advanceUntilIdle()

        assertTrue(savedRepository.saveCalls.isEmpty())
        assertTrue(savedRepository.unsaveCalls.isEmpty())
    }

    @Test
    fun unreadableSavedStateDoesNotTurnTheResultIntoAnError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAssessmentRepository(completedAttempt(listOf("q"), 1))
        val savedRepository = FakeSavedQuestionRepository().apply { failReads = true }
        val viewModel = FocusedResultViewModel(
            "attempt",
            repository,
            reviewLoader(listOf(question("q"))),
            retakeService(repository, listOf(question("q"))),
            savedQuestionStateHolder(savedRepository),
        )
        advanceUntilIdle()

        assertIs<FocusedResultUiState.Content>(viewModel.uiState.value)
        assertIs<SavedQuestionsState.Error>(viewModel.savedQuestions.value)
    }

    private fun completedAttempt(ids: List<String>, correct: Int, selectedIds: Set<String> = setOf("a")) = TestAttempt(
        id = "attempt",
        config = config(),
        questionAttempts = ids.map { QuestionAttempt(it, QuestionAnswerState.Answered(selectedIds, it != "missing" && correct > 0)) },
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.fromEpochMilliseconds(1),
        completedAt = Instant.fromEpochMilliseconds(2),
        score = AssessmentScore(ids.size, correct),
    )

    private fun config() = AssessmentConfig.Focused(AssessmentScope.Topic("topic"), 10)

    private fun reviewLoader(questions: List<Question>) =
        AssessmentReviewLoader(FakeCurriculumRepository(questions))

    private fun retakeService(repository: FakeAssessmentRepository, questions: List<Question>) =
        AssessmentRetakeService(
            assessmentRepository = repository,
            assessmentEngine = AssessmentEngine(
                questionSelector = AssessmentQuestionSelector(
                    curriculumRepository = FakeCurriculumRepository(questions),
                    completedHistory = { emptyList() },
                    randomize = { it },
                ),
                generateAttemptId = { "retake" },
                now = { Instant.fromEpochMilliseconds(2) },
            ),
        )

    private fun question(id: String, correctIds: List<String> = listOf("a"), selectedIds: Set<String> = setOf("a")) = Question(
        id = id, topicId = "topic", subtopicId = "subtopic", text = "Question $id",
        answers = listOf(AnswerOption("a", "Answer A"), AnswerOption("b", "Answer B"), AnswerOption("c", "Answer C")),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = correctIds, explanation = "Explanation", sources = listOf(SourceReference("Source B", "b"), SourceReference("Source A", "a")),
        status = ContentStatus.ACTIVE,
    )

    private class FakeAssessmentRepository(var attempt: TestAttempt?) : AssessmentRepository {
        override suspend fun save(attempt: TestAttempt) { this.attempt = attempt }
        override suspend fun getById(attemptId: String): TestAttempt? = attempt
        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }

    private class FakeCurriculumRepository(private val questions: List<Question>) : CurriculumRepository {
        val lookups = mutableListOf<String>()
        override suspend fun getActiveTopics(): List<Topic> = error("not used")
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = error("not used")
        override suspend fun getActiveQuestions(): List<Question> = questions
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = error("not used")
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = error("not used")
        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> = error("not used")
        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = questions.filter { it.level in levels }
        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = questions.filter { it.level in levels }
        override suspend fun getTopicById(topicId: String): Topic? = null
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
        override suspend fun getQuestionById(questionId: String): Question? { lookups += questionId; return questions.firstOrNull { it.id == questionId } }
    }
}

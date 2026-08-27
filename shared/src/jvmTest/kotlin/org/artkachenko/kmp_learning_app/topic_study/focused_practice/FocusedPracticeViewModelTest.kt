package org.artkachenko.kmp_learning_app.topic_study.focused_practice

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
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalCoroutinesApi::class)
internal class FocusedPracticeViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsAndPersistsActualQuestionCount() = runViewModelTest {
        val questions = listOf(
            question("single", listOf("a")),
            question("multi", listOf("a", "c")),
        )
        val repository = RecordingAssessmentRepository()
        val viewModel = viewModel(questions, repository)

        advanceUntilIdle()

        val state = assertIs<FocusedPracticeUiState.Content>(viewModel.uiState.value)
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
        val viewModel = viewModel(listOf(question("multi", listOf("a", "c"))))
        advanceUntilIdle()

        viewModel.selectAnswer("a")
        viewModel.selectAnswer("c")
        assertEquals(setOf("a", "c"), content(viewModel).selectedAnswerIds)
        viewModel.selectAnswer("a")
        assertEquals(setOf("c"), content(viewModel).selectedAnswerIds)
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
                question("multi", listOf("a", "c")),
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

        assertIs<FocusedPracticeUiState.ReadyToComplete>(viewModel.uiState.value)
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

        assertIs<FocusedPracticeUiState.ReadyToComplete>(viewModel.uiState.value)
        val saved = repository.savedAttempts.last()
        assertEquals(org.artkachenko.kmp_learning_app.assessment.AssessmentStatus.IN_PROGRESS, saved.status)
        assertEquals(null, saved.score)
    }

    private fun viewModel(
        questions: List<Question>,
        repository: RecordingAssessmentRepository = RecordingAssessmentRepository(),
    ) = FocusedPracticeViewModel(
        config = AssessmentConfig.Focused(
            scope = AssessmentScope.Topic("topic"),
            questionCount = 10,
        ),
        assessmentEngine = AssessmentEngine(
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = FakeCurriculumRepository(questions),
                randomize = { it },
            ),
            generateAttemptId = { "attempt-1" },
            now = { Instant.fromEpochMilliseconds(1_000) },
        ),
        assessmentRepository = repository,
    )

    private fun content(viewModel: FocusedPracticeViewModel) =
        assertIs<FocusedPracticeUiState.Content>(viewModel.uiState.value)

    private fun runViewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun question(id: String, correctIds: List<String>) = Question(
        id = id,
        topicId = "topic",
        subtopicId = "subtopic",
        text = "Question $id",
        answers = listOf(
            AnswerOption("a", "Answer A"),
            AnswerOption("b", "Answer B"),
            AnswerOption("c", "Answer C"),
        ),
        correctAnswerIds = correctIds,
        explanation = "Explanation",
        sources = listOf(SourceReference("Source", "https://example.com")),
    )

    private class FakeCurriculumRepository(
        private val questions: List<Question>,
    ) : CurriculumRepository {
        override suspend fun getActiveTopics(): List<Topic> = listOf(Topic("topic", "Topic"))
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = emptyList()
        override suspend fun getActiveQuestions(): List<Question> = questions
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = questions
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = questions
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
    }
}

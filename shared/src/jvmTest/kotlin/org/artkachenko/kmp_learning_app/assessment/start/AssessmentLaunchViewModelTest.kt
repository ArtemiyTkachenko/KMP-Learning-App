package org.artkachenko.kmp_learning_app.assessment.start

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment.selection.AssessmentQuestionSelector
import org.artkachenko.kmp_learning_app.assessment.session.AssessmentEngine
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
internal class AssessmentLaunchViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successfulStartPersistsOnceAndEmitsDurableAttemptIdentity() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAssessmentRepository()
        val viewModel = viewModel(repository, listOf(question("question")))
        val config = AssessmentConfig.Mixed(questionCount = 1)

        viewModel.start(config)
        viewModel.start(config)
        assertEquals(AssessmentLaunchState.Launching, viewModel.state.value)
        advanceUntilIdle()

        assertEquals(AssessmentLaunchState.Idle, viewModel.state.value)
        assertEquals(1, repository.attempts.size)
        assertEquals(
            AssessmentLaunchEvent.Created("attempt-1"),
            viewModel.events.first(),
        )
    }

    @Test
    fun coordinatorReportsOnlyTheCreatedAttemptIdentity() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val attemptIds = mutableListOf<String>()
        val viewModel = viewModel(FakeAssessmentRepository(), listOf(question("question")))

        setContent {
            AppTheme {
                AssessmentLaunchCoordinator(
                    onAttemptCreated = attemptIds::add,
                    viewModel = viewModel,
                ) { startAssessment ->
                    Button(
                        onClick = {
                            startAssessment(AssessmentConfig.Mixed(questionCount = 1))
                        },
                    ) {
                        Text("Start")
                    }
                }
            }
        }

        onNodeWithText("Start").performClick()
        waitUntil { attemptIds.isNotEmpty() }

        assertEquals(listOf("attempt-1"), attemptIds)
    }

    @Test
    fun noEligibleQuestionsKeepsTheOriginVisibleAndCanBeDismissed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel(FakeAssessmentRepository(), emptyList())
        val config = AssessmentConfig.Mixed(questionCount = 1)

        viewModel.start(config)
        advanceUntilIdle()

        assertEquals(
            AssessmentLaunchState.Failed(config, AssessmentLaunchFailure.NoEligibleQuestions),
            viewModel.state.value,
        )
        viewModel.dismissFailure()
        assertEquals(AssessmentLaunchState.Idle, viewModel.state.value)
    }

    @Test
    fun unexpectedFailureCanRetryTheSameConfiguration() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeAssessmentRepository(failSave = true)
        val viewModel = viewModel(repository, listOf(question("question")))
        val config = AssessmentConfig.Mixed(questionCount = 1)

        viewModel.start(config)
        advanceUntilIdle()
        assertEquals(
            AssessmentLaunchState.Failed(config, AssessmentLaunchFailure.Unexpected),
            viewModel.state.value,
        )

        repository.failSave = false
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(AssessmentLaunchState.Idle, viewModel.state.value)
        assertEquals(
            "attempt-2",
            assertIs<AssessmentLaunchEvent.Created>(viewModel.events.first()).attemptId,
        )
        assertEquals(listOf("attempt-2"), repository.attempts.map { it.id })
    }

    private fun viewModel(
        repository: FakeAssessmentRepository,
        questions: List<Question>,
    ): AssessmentLaunchViewModel {
        var nextAttempt = 1
        val engine = AssessmentEngine(
            questionSelector = AssessmentQuestionSelector(
                curriculumRepository = FakeCurriculumRepository(questions),
                completedHistory = { emptyList() },
                randomize = { it },
            ),
            generateAttemptId = { "attempt-${nextAttempt++}" },
        )
        return AssessmentLaunchViewModel(StartAssessment(engine, repository))
    }

    private class FakeAssessmentRepository(
        var failSave: Boolean = false,
    ) : AssessmentRepository {
        val attempts = mutableListOf<TestAttempt>()

        override suspend fun save(attempt: TestAttempt) {
            if (failSave) error("save failed")
            attempts += attempt
        }

        override suspend fun getById(attemptId: String): TestAttempt? =
            attempts.firstOrNull { it.id == attemptId }

        override suspend fun getCompletedAttempts(): List<TestAttempt> = emptyList()
    }

    private class FakeCurriculumRepository(
        private val questions: List<Question>,
    ) : CurriculumRepository {
        override suspend fun getActiveQuestions(): List<Question> = questions
        override suspend fun getActiveTopics(): List<Topic> = error("Not used.")
        override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = error("Not used.")
        override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = error("Not used.")
        override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = error("Not used.")
        override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> = error("Not used.")
        override suspend fun getActiveQuestionsByTopicAndLevels(
            topicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used.")

        override suspend fun getActiveQuestionsBySubtopicAndLevels(
            subtopicId: String,
            levels: Set<QuestionLevel>,
        ): List<Question> = error("Not used.")

        override suspend fun getTopicById(topicId: String): Topic? = error("Not used.")
        override suspend fun getSubtopicById(subtopicId: String): Subtopic? = error("Not used.")
        override suspend fun getQuestionById(questionId: String): Question? = error("Not used.")
    }

    private fun question(id: String) = Question(
        id = id,
        topicId = "topic",
        subtopicId = "subtopic",
        text = "Question?",
        answers = listOf(AnswerOption("answer", "Answer")),
        selectionMode = AnswerSelectionMode.SINGLE,
        level = QuestionLevel.FOUNDATION,
        correctAnswerIds = listOf("answer"),
        explanation = "Explanation.",
        sources = listOf(SourceReference("Source", "https://example.com")),
    )
}

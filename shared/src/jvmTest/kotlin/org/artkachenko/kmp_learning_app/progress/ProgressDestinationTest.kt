package org.artkachenko.kmp_learning_app.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.AssessmentScore
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.QuestionAnswerState
import org.artkachenko.kmp_learning_app.assessment.QuestionAttempt
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class ProgressDestinationTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resumeLoadsProgressAndReturningToTheDestinationPicksUpNewAttempts() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repository = StubAssessmentRepository(listOf(mixedAttempt("first")))
        val owner = TestLifecycleOwner()

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MaterialTheme {
                    ProgressDestination(
                        onBack = {},
                        onOpenTopic = {},
                        onReviewMistakes = {},
                        onOpenFocusedResult = {},
                        onOpenMixedResult = {},
                        viewModel = viewModel(repository),
                    )
                }
            }
        }

        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()
        onNodeWithText("1 completed assessments").assertIsDisplayed()

        repository.attempts = listOf(mixedAttempt("second"), mixedAttempt("first"))
        owner.moveTo(Lifecycle.State.CREATED)
        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()

        onNodeWithText("2 completed assessments").assertIsDisplayed()
        assertEquals(2, onAllNodesWithText("Mixed Android Interview").fetchSemanticsNodes().size)
    }

    @Test
    fun historyRowsOpenTheExistingResultDestinationsByStableAttemptId() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val focusedTargets = mutableListOf<String>()
        val mixedTargets = mutableListOf<String>()
        val repository = StubAssessmentRepository(
            listOf(
                mixedAttempt("mixed-attempt"),
                focusedAttempt("focused-attempt"),
            ),
        )
        val owner = TestLifecycleOwner()

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MaterialTheme {
                    ProgressDestination(
                        onBack = {},
                        onOpenTopic = {},
                        onReviewMistakes = {},
                        onOpenFocusedResult = focusedTargets::add,
                        onOpenMixedResult = mixedTargets::add,
                        viewModel = viewModel(repository),
                    )
                }
            }
        }

        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()

        onNodeWithText("Mixed Android Interview").performClick()
        onNodeWithText("Focused practice").performClick()

        assertEquals(listOf("mixed-attempt"), mixedTargets)
        assertEquals(listOf("focused-attempt"), focusedTargets)
    }
}

private class TestLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this)

    override val lifecycle: Lifecycle get() = registry

    fun moveTo(state: Lifecycle.State) {
        registry.currentState = state
    }
}

private fun viewModel(repository: AssessmentRepository): ProgressViewModel {
    val curriculum = StubCurriculumRepository()
    return ProgressViewModel(
        learningProgressService = LearningProgressService(repository, curriculum),
        assessmentRepository = repository,
        curriculumRepository = curriculum,
    )
}

private class StubAssessmentRepository(
    var attempts: List<TestAttempt>,
) : AssessmentRepository {
    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts
}

private class StubCurriculumRepository : CurriculumRepository {
    override suspend fun getActiveTopics(): List<Topic> = emptyList()
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> = emptyList()
    override suspend fun getActiveQuestions(): List<Question> = emptyList()
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> = emptyList()
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> = emptyList()
    override suspend fun getTopicById(topicId: String): Topic? = Topic(topicId, "Kotlin")
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? = null
    override suspend fun getQuestionById(questionId: String): Question? = null
}

private fun mixedAttempt(id: String): TestAttempt = attempt(id, AssessmentConfig.Mixed(1))

private fun focusedAttempt(id: String): TestAttempt =
    attempt(
        id,
        AssessmentConfig.Focused(scope = AssessmentScope.Topic("kotlin"), questionCount = 1),
    )

private fun attempt(
    id: String,
    config: AssessmentConfig,
): TestAttempt =
    TestAttempt(
        id = id,
        config = config,
        questionAttempts = listOf(
            QuestionAttempt("$id-q", QuestionAnswerState.Answered(setOf("$id-a"), true)),
        ),
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse("2026-08-29T00:00:00Z"),
        completedAt = Instant.parse("2026-08-29T00:15:00Z"),
        score = AssessmentScore(1, 1),
    )

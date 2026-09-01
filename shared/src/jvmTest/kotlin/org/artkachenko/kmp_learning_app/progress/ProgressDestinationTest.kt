package org.artkachenko.kmp_learning_app.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository
import org.artkachenko.kmp_learning_app.learning_progress.LearningProgressService
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class ProgressDestinationTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun returningToTheDestinationShowsHistoryThatChangedWhileItWasAway() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repository = StubAssessmentRepository(listOf(mixedAttempt("first")))
        val harness = harness(repository)
        val store = harness.store
        val owner = TestLifecycleOwner()

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MaterialTheme {
                    ProgressDestination(
                        onBack = {},
                        onBrowseTopics = {},
                        onOpenTopic = {},
                        onOpenFocusedResult = {},
                        onOpenMixedResult = {},
                        viewModel = harness.viewModel,
                    )
                }
            }
        }

        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()
        onNodeWithText("Completed assessments").assertIsDisplayed()

        // The dashboard no longer re-reads on every resume: the shared cache is what changes, and
        // it does so when something marks it stale. Leaving and returning must show the new data
        // without the screen having asked for it.
        repository.attempts = listOf(mixedAttempt("second"), mixedAttempt("first"))
        store.invalidate()
        owner.moveTo(Lifecycle.State.CREATED)
        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()

        onNodeWithText("Completed assessments").assertIsDisplayed()
        // The dashboard now carries coverage and recent performance above the history, so the rows
        // sit below the fold; each is scrolled to by its own stable attempt handle rather than
        // counted, which also says which attempt was found.
        listOf("second", "first").forEach { attemptId ->
            onNodeWithTag(ProgressContentTag)
                .performScrollToNode(hasTestTag(progressHistoryCardTag(attemptId)))
            onNodeWithTag(progressHistoryCardTag(attemptId)).assertIsDisplayed()
        }
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
                        onBrowseTopics = {},
                        onOpenTopic = {},
                        onOpenFocusedResult = focusedTargets::add,
                        onOpenMixedResult = mixedTargets::add,
                        viewModel = viewModel(repository),
                    )
                }
            }
        }

        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()

        onNodeWithTag(ProgressContentTag)
            .performScrollToNode(hasText("Mixed Android Interview"))
        onNodeWithText("Mixed Android Interview").performClick()
        onNodeWithTag(ProgressContentTag).performScrollToNode(hasText("Focused practice"))
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

private class ProgressHarness(val viewModel: ProgressViewModel, val store: AssessmentHistoryStore)

private fun viewModel(repository: AssessmentRepository): ProgressViewModel =
    harness(repository).viewModel

private fun harness(repository: AssessmentRepository): ProgressHarness {
    val curriculum = StubCurriculumRepository()
    return run {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val store = AssessmentHistoryStore(repository, scope)
        ProgressHarness(
            ProgressViewModel(
            historyStore = store,
            stateHolder = ProgressStateHolder(
                learningProgressService = LearningProgressService(repository, curriculum),
                curriculumRepository = curriculum,
                mistakeReviewService = MistakeReviewService(repository, AssessmentReviewLoader(curriculum)),
                historyStore = store,
                scope = scope,
            ),
            ),
            store,
        )
    }
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
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> = emptyList()
    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = emptyList()
    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = emptyList()
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

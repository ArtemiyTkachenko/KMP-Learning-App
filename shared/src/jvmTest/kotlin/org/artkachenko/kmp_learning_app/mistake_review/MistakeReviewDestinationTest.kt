package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.curriculum.Subtopic
import org.artkachenko.kmp_learning_app.curriculum.Topic
import org.artkachenko.kmp_learning_app.curriculum.repository.CurriculumRepository

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class MistakeReviewDestinationTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun returningToTheDestinationShowsAQueueThatChangedWhileItWasAway() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repository = MutableDestinationHistoryRepository(
            listOf(destinationAttempt("first", "q1")),
        )
        val owner = MistakeReviewLifecycleOwner()
        val viewModel = destinationViewModel(repository)

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MaterialTheme {
                    MistakeReviewDestination(
                        onBack = {},
                        onBrowseTopics = {},
                        viewModel = viewModel,
                    )
                }
            }
        }

        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()
        onNodeWithText("Question q1").assertIsDisplayed()

        // The queue no longer re-reads on every resume: the shared cache is what changes, and it
        // does so when something marks it stale. Leaving and returning must then show the new
        // queue without the screen having asked for it.
        repository.attempts = listOf(destinationAttempt("second", "q2"))
        viewModel.refresh()
        owner.moveTo(Lifecycle.State.CREATED)
        owner.moveTo(Lifecycle.State.RESUMED)
        waitForIdle()

        onNodeWithText("Question q2").assertIsDisplayed()
    }

    @Test
    fun sourceClickReachesTheHostUriHandlerWithTheExactUrl() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val uriHandler = RecordingUriHandler()

        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                MaterialTheme {
                    MistakeReviewDestination(onBack = {}, onBrowseTopics = {}, viewModel = destinationViewModel())
                }
            }
        }

        waitForIdle()
        onNodeWithText("Source: Kotlin docs").performScrollTo().performClick()

        assertEquals(listOf("https://kotlinlang.org/q1"), uriHandler.opened)
    }

    @Test
    fun aFailingUriHandlerSurfacesTheSourceOpenFailure() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val uriHandler = RecordingUriHandler(fail = true)

        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                MaterialTheme {
                    MistakeReviewDestination(onBack = {}, onBrowseTopics = {}, viewModel = destinationViewModel())
                }
            }
        }

        waitForIdle()
        onNodeWithText("Source: Kotlin docs").performScrollTo().performClick()
        waitForIdle()

        // The failure must stay visible rather than looking like a no-op.
        onNodeWithText("This source could not be opened.").performScrollTo().assertIsDisplayed()
    }
}

private class RecordingUriHandler(
    private val fail: Boolean = false,
) : UriHandler {
    val opened = mutableListOf<String>()

    override fun openUri(uri: String) {
        opened += uri
        if (fail) error("No host handler for $uri")
    }
}

private fun destinationViewModel(
    repository: AssessmentRepository = DestinationHistoryRepository,
): MistakeReviewViewModel =
    run {
        val service = MistakeReviewService(
            assessmentRepository = repository,
            assessmentReviewLoader = AssessmentReviewLoader(DestinationCurriculumRepository),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val store = AssessmentHistoryStore(repository, scope)
        MistakeReviewViewModel(
            historyStore = store,
            stateHolder = MistakeReviewStateHolder(service, store, scope),
        )
    }

private class MistakeReviewLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this)

    override val lifecycle: Lifecycle get() = registry

    fun moveTo(state: Lifecycle.State) {
        registry.currentState = state
    }
}

private class MutableDestinationHistoryRepository(
    var attempts: List<TestAttempt>,
) : AssessmentRepository {
    override suspend fun save(attempt: TestAttempt) = Unit

    override suspend fun getById(attemptId: String): TestAttempt? =
        attempts.firstOrNull { it.id == attemptId }

    override suspend fun getCompletedAttempts(): List<TestAttempt> = attempts
}

private fun destinationAttempt(
    id: String,
    questionId: String,
): TestAttempt =
    TestAttempt(
        id = id,
        config = AssessmentConfig.Mixed(1),
        questionAttempts = listOf(
            QuestionAttempt(
                questionId,
                QuestionAnswerState.Answered(setOf("${questionId}_b"), isCorrect = false),
            ),
        ),
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse("2026-08-29T09:00:00Z"),
        completedAt = Instant.parse("2026-08-29T10:00:00Z"),
        score = AssessmentScore(1, 0),
    )

private object DestinationHistoryRepository : AssessmentRepository {
    private val attempt = TestAttempt(
        id = "attempt",
        config = AssessmentConfig.Mixed(1),
        questionAttempts = listOf(
            QuestionAttempt("q1", QuestionAnswerState.Answered(setOf("q1_b"), isCorrect = false)),
        ),
        status = AssessmentStatus.COMPLETED,
        startedAt = Instant.parse("2026-08-29T09:00:00Z"),
        completedAt = Instant.parse("2026-08-29T10:00:00Z"),
        score = AssessmentScore(1, 0),
    )

    override suspend fun save(attempt: TestAttempt) = Unit
    override suspend fun getById(attemptId: String): TestAttempt? = attempt.takeIf { it.id == attemptId }
    override suspend fun getCompletedAttempts(): List<TestAttempt> = listOf(attempt)
}

private object DestinationCurriculumRepository : CurriculumRepository {
    override suspend fun getActiveTopics(): List<Topic> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveSubtopics(topicId: String): List<Subtopic> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestions(): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopic(topicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopic(subtopicId: String): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByLevels(levels: Set<QuestionLevel>): List<Question> =
        error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsByTopicAndLevels(
        topicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getActiveQuestionsBySubtopicAndLevels(
        subtopicId: String,
        levels: Set<QuestionLevel>,
    ): List<Question> = error("ACTIVE lookup must not be used.")
    override suspend fun getTopicById(topicId: String): Topic? = error("Topic lookup is not needed.")
    override suspend fun getSubtopicById(subtopicId: String): Subtopic? =
        error("Subtopic lookup is not needed.")

    override suspend fun getQuestionById(questionId: String): Question? =
        Question(
            id = questionId,
            topicId = "kotlin",
            subtopicId = "coroutines",
            text = "Question $questionId",
            answers = listOf(
                AnswerOption("${questionId}_a", "Answer A"),
                AnswerOption("${questionId}_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.SINGLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("${questionId}_a"),
            explanation = "Explanation",
            sources = listOf(SourceReference("Kotlin docs", "https://kotlinlang.org/$questionId")),
        )
}

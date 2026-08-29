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
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentReviewLoader
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.Question
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
    fun sourceClickReachesTheHostUriHandlerWithTheExactUrl() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val uriHandler = RecordingUriHandler()

        setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                MaterialTheme {
                    MistakeReviewDestination(onBack = {}, viewModel = destinationViewModel())
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
                    MistakeReviewDestination(onBack = {}, viewModel = destinationViewModel())
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

private fun destinationViewModel(): MistakeReviewViewModel =
    MistakeReviewViewModel(
        MistakeReviewService(
            assessmentRepository = DestinationHistoryRepository,
            assessmentReviewLoader = AssessmentReviewLoader(DestinationCurriculumRepository),
        ),
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
            correctAnswerIds = listOf("${questionId}_a"),
            explanation = "Explanation",
            sources = listOf(SourceReference("Kotlin docs", "https://kotlinlang.org/$questionId")),
        )
}

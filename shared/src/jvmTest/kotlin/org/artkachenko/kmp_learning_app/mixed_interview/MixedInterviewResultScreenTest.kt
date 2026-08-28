package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel

@OptIn(ExperimentalTestApi::class)
internal class MixedInterviewResultScreenTest {
    @Test
    fun contentRendersOverallTopicAndQuestionReviewAndOpensExactSource() = runComposeUiTest {
        var openedUrl: String? = null
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = { openedUrl = it },
                )
            }
        }

        onNodeWithText("Score: 3 / 5").assertIsDisplayed()
        onNodeWithText("60", substring = true).assertIsDisplayed()
        onNodeWithText("Performance by topic").performScrollTo().assertIsDisplayed()
        onNodeWithText("Kotlin").performScrollTo().assertIsDisplayed()
        onNodeWithText("Topic unavailable").performScrollTo().assertIsDisplayed()
        onNodeWithText("2 / 3 correct").performScrollTo().assertIsDisplayed()
        onNodeWithText("Question review").performScrollTo().assertIsDisplayed()
        onNodeWithText("Authored explanation").performScrollTo().assertIsDisplayed()
        onNodeWithText("Source: Official docs").performScrollTo().performClick()
        assertEquals("https://example.com/docs", openedUrl)
        onNode(hasScrollAction()).performScrollToNode(
            hasText("Question missing is no longer available."),
        )
        onNodeWithText("Question missing is no longer available.").assertIsDisplayed()
    }

    @Test
    fun loadingAndUnavailableStatesRender() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = MixedInterviewResultUiState.Loading,
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }
        onNodeWithText("Loading interview results").assertIsDisplayed()
    }

    @Test
    fun attemptNotFoundAndNotCompletedStatesRender() = runComposeUiTest {
        var state: MixedInterviewResultUiState by mutableStateOf(
            MixedInterviewResultUiState.AttemptNotFound,
        )
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(state, {}, {}, {})
            }
        }
        onNodeWithText("This interview could not be found.").assertIsDisplayed()

        state = MixedInterviewResultUiState.NotCompleted
        onNodeWithText("This interview is not complete yet.").assertIsDisplayed()
    }

    @Test
    fun errorRetryInvokesCallback() = runComposeUiTest {
        var retries = 0
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = MixedInterviewResultUiState.Error,
                    onRetry = { retries += 1 },
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithText("Interview results could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    private fun contentState() = MixedInterviewResultUiState.Content(
        attemptId = "attempt",
        totalQuestions = 5,
        correctAnswers = 3,
        percentage = 60.0,
        topicPerformance = listOf(
            TopicPerformanceUiModel("kotlin", "Kotlin", 3, 2, 66.7),
            TopicPerformanceUiModel("old", null, 1, 1, 100.0),
        ),
        questions = listOf(
            ReviewQuestionItem.Available(
                ReviewQuestionUiModel(
                    questionId = "q1",
                    topicId = "kotlin",
                    text = "Question text",
                    isCorrect = true,
                    answers = listOf(
                        ReviewAnswerUiModel("a", "Answer A", true, true),
                    ),
                    explanation = "Authored explanation",
                    sources = listOf(
                        ReviewSourceUiModel("Official docs", "https://example.com/docs"),
                    ),
                ),
            ),
            ReviewQuestionItem.Missing("missing"),
        ),
    )
}

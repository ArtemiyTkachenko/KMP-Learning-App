package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
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
    fun unresolvedQuestionsAreCalledOutWhenTheyDoNotMatchTheScoreTotal() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }

        // The persisted score counts 5 questions; only one review question resolves.
        onNodeWithText("4 of 5 questions", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun fullyResolvableReviewHidesTheUnresolvedNotice() = runComposeUiTest {
        val resolvable = contentState().let { state ->
            state.copy(
                totalQuestions = 1,
                questions = state.questions.filterIsInstance<ReviewQuestionItem.Available>(),
            )
        }
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = resolvable,
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithText("questions are no longer available", substring = true).assertDoesNotExist()
    }

    @Test
    fun failedSourceUrlShowsTheFailureInsideThatQuestionCard() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                    failedSourceUrl = "https://example.com/docs",
                )
            }
        }

        onNodeWithText("This source could not be opened.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun unrelatedFailedSourceUrlLeavesTheCardUnchanged() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                    failedSourceUrl = "https://example.com/not-in-this-card",
                )
            }
        }

        onNodeWithText("This source could not be opened.").assertDoesNotExist()
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

    @Test
    fun practiceAgainIsVisibleAndInvokesCallbackOnce() = runComposeUiTest {
        var repeats = 0
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                    onRepeatInterview = { repeats++ },
                )
            }
        }

        onNodeWithText("Practice Again").assertIsDisplayed().performClick()
        assertEquals(1, repeats)
    }

    @Test
    fun creatingDisablesActionShowsProgressAndKeepsResultVisible() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState().copy(repeatInterviewState = RepeatInterviewState.Creating),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithTag(MixedResultPracticeAgainTag).assertIsNotEnabled()
        onNodeWithTag(MixedResultCreatingIndicatorTag).assertIsDisplayed()
        onNodeWithText("Starting interview").assertIsDisplayed()
        onNodeWithText("Score: 3 / 5").assertIsDisplayed()
        onNodeWithText("Performance by topic").performScrollTo().assertIsDisplayed()
        onNodeWithText("Authored explanation").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun retakeFailuresShowSpecificMessagesAndKeepResultVisible() = runComposeUiTest {
        var repeatState: RepeatInterviewState by mutableStateOf(
            RepeatInterviewState.SourceAttemptNotFound,
        )
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState().copy(repeatInterviewState = repeatState),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithText("The original interview is no longer available.").assertIsDisplayed()
        onNodeWithText("Score: 3 / 5").assertIsDisplayed()

        repeatState = RepeatInterviewState.NoEligibleQuestions
        onNodeWithText("No interview questions are currently available.").assertIsDisplayed()
        onNodeWithText("Practice Again").assertIsDisplayed()

        repeatState = RepeatInterviewState.Error
        onNodeWithText("Interview could not be started. Try again.").assertIsDisplayed()
        onNodeWithText("Score: 3 / 5").assertIsDisplayed()
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
                    subtopicId = "kotlin_basics",
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

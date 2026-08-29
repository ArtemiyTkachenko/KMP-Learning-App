package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel

@OptIn(ExperimentalTestApi::class)
internal class MistakeReviewScreenTest {
    @Test
    fun loadingStateRenders() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(MistakeReviewUiState.Loading, {}, {}, {})
            }
        }

        onNodeWithTag(MistakeReviewLoadingTag).assertIsDisplayed()
        onNodeWithText("Loading mistakes").assertIsDisplayed()
    }

    @Test
    fun emptyStateExplainsTheResolutionRule() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(MistakeReviewUiState.Empty, {}, {}, {})
            }
        }

        onNodeWithText("No unresolved mistakes.").assertIsDisplayed()
        onNodeWithText(
            "Questions disappear from this list after your most recent completed answer is correct.",
        ).assertIsDisplayed()
    }

    @Test
    fun errorStateRendersAndRetries() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                MistakeReviewScreen(MistakeReviewUiState.Error, {}, { retryCount += 1 }, {})
            }
        }

        onNodeWithText("Mistakes could not be loaded.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun availableMistakeReusesTheSharedReviewCard() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithText("Questions stay here until your most recent completed answer is correct.")
            .assertIsDisplayed()
        onNodeWithText("Question q1").assertIsDisplayed()
        // Rendered by the shared ReviewQuestionCard rather than a mistake-specific copy.
        onNodeWithText("Incorrect").assertExists()
        onNodeWithText("Your answer").assertExists()
        onNodeWithText("Correct answer").assertExists()
        onNodeWithText("Explanation").performScrollTo().assertIsDisplayed()
        onNodeWithText("Explanation for q1").assertExists()
        onNodeWithText("Source: Kotlin docs").assertExists()
    }

    @Test
    fun missingMistakeReusesTheSharedMissingQuestionComponent() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(
                            UnresolvedMistake("gone", "attempt", ReviewQuestionItem.Missing("gone")),
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithText("Question gone is no longer available.").assertIsDisplayed()
    }

    @Test
    fun multipleMistakesRenderInQueueOrder() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(
                        listOf(availableMistake("q3"), availableMistake("q1")),
                    ),
                    onBack = {},
                    onRetry = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithText("Question q3").assertIsDisplayed()
        onNodeWithText("Question q1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sourceClickEmitsTheExactUrl() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onSourceClick = clicked::add,
                )
            }
        }

        onNodeWithText("Source: Kotlin docs").performScrollTo().performClick()

        assertEquals(listOf("https://kotlinlang.org/q1"), clicked)
    }

    @Test
    fun sourceOpenFailureRemainsVisible() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MistakeReviewScreen(
                    state = MistakeReviewUiState.Content(listOf(availableMistake("q1"))),
                    onBack = {},
                    onRetry = {},
                    onSourceClick = {},
                    failedSourceUrl = "https://kotlinlang.org/q1",
                )
            }
        }

        onNodeWithText("This source could not be opened.").performScrollTo().assertIsDisplayed()
    }
}

private fun availableMistake(questionId: String): UnresolvedMistake =
    UnresolvedMistake(
        questionId = questionId,
        sourceAttemptId = "attempt",
        reviewItem = ReviewQuestionItem.Available(
            ReviewQuestionUiModel(
                questionId = questionId,
                topicId = "kotlin",
                text = "Question $questionId",
                isCorrect = false,
                answers = listOf(
                    ReviewAnswerUiModel("${questionId}_a", "Answer A", false, isCorrectAnswer = true),
                    ReviewAnswerUiModel("${questionId}_b", "Answer B", true, isCorrectAnswer = false),
                ),
                explanation = "Explanation for $questionId",
                sources = listOf(
                    ReviewSourceUiModel("Kotlin docs", "https://kotlinlang.org/$questionId"),
                ),
            ),
        ),
    )

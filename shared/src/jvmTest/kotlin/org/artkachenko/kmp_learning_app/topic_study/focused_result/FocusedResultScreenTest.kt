package org.artkachenko.kmp_learning_app.topic_study.focused_result

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
import org.artkachenko.kmp_learning_app.assessment_review.reviewQuestionSaveTag
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState

@OptIn(ExperimentalTestApi::class)
internal class FocusedResultScreenTest {
    @Test
    fun scoreReviewAndSourceUseStableUrlCallback() = runComposeUiTest {
        var openedUrl: String? = null
        setContent {
            MaterialTheme {
                FocusedResultScreen(
                    state = FocusedResultUiState.Content(
                        attemptId = "attempt",
                        totalQuestions = 2,
                        correctAnswers = 1,
                        percentage = 50.0,
                        questions = listOf(
                            ReviewQuestionItem.Available(
                                ReviewQuestionUiModel(
                                    questionId = "q1",
                                    topicId = "topic",
                                    subtopicId = "topic_basics",
                                    text = "Question text",
                                    isCorrect = true,
                                    answers = listOf(
                                        ReviewAnswerUiModel("a", "Answer A", true, true),
                                        ReviewAnswerUiModel("b", "Answer B", false, false),
                                    ),
                                    explanation = "Read the explanation",
                                    sources = listOf(ReviewSourceUiModel("Official docs", "https://example.com/docs")),
                                ),
                            ),
                            ReviewQuestionItem.Missing("q2"),
                        ),
                    ),
                    onRetry = {}, onBack = {}, onSourceClick = { openedUrl = it },
                    onRepeatPractice = {},
                )
            }
        }

        onNodeWithText("Score: 1 / 2").assertIsDisplayed()
        onNodeWithText("50", substring = true).assertIsDisplayed()
        onNodeWithText("Correct").assertIsDisplayed()
        onNodeWithText("Your answer").assertIsDisplayed()
        onNodeWithText("Explanation").assertIsDisplayed()
        onNodeWithText("Source: Official docs").performClick()
        assertEquals("https://example.com/docs", openedUrl)
        onNodeWithText("Question q2 is no longer available.").assertIsDisplayed()
        onNodeWithText("1 of 2 questions", substring = true).assertIsDisplayed()
    }

    @Test
    fun fullyResolvableReviewHidesTheUnresolvedNotice() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FocusedResultScreen(
                    state = contentState(questions = listOf(availableQuestion())),
                    onRetry = {}, onBack = {}, onSourceClick = {},
                    onRepeatPractice = {},
                )
            }
        }

        onNodeWithText("questions are no longer available", substring = true).assertDoesNotExist()
    }

    @Test
    fun failedSourceUrlShowsTheFailureInsideThatQuestionCard() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FocusedResultScreen(
                    state = contentState(questions = listOf(availableQuestion())),
                    onRetry = {}, onBack = {}, onSourceClick = {},
                    onRepeatPractice = {},
                    failedSourceUrl = "https://example.com/docs",
                )
            }
        }

        onNodeWithText("This source could not be opened.").assertIsDisplayed()
    }

    @Test
    fun successfulSourceOpenShowsNoFailureMessage() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FocusedResultScreen(
                    state = contentState(questions = listOf(availableQuestion())),
                    onRetry = {}, onBack = {}, onSourceClick = {},
                    onRepeatPractice = {},
                    failedSourceUrl = null,
                )
            }
        }

        onNodeWithText("This source could not be opened.").assertDoesNotExist()
    }

    @Test
    fun availableQuestionsExposeSavedStateAndMissingPlaceholdersDoNot() = runComposeUiTest {
        val toggled = mutableListOf<String>()
        setContent {
            MaterialTheme {
                FocusedResultScreen(
                    state = contentState(
                        questions = listOf(availableQuestion(), ReviewQuestionItem.Missing("q2")),
                    ),
                    onRetry = {}, onBack = {}, onSourceClick = {},
                    onRepeatPractice = {},
                    savedQuestions = SavedQuestionsState.Loaded(emptyList()),
                    onToggleSaved = toggled::add,
                )
            }
        }

        onNodeWithTag(reviewQuestionSaveTag("q1")).performScrollTo().performClick()
        assertEquals(listOf("q1"), toggled)
        // A Question the curriculum no longer holds cannot be saved from here.
        onNodeWithTag(reviewQuestionSaveTag("q2")).assertDoesNotExist()
    }

    @Test
    fun aSavedQuestionOffersUnsaveAndLeavesTheResultUnchanged() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FocusedResultScreen(
                    state = contentState(questions = listOf(availableQuestion())),
                    onRetry = {}, onBack = {}, onSourceClick = {},
                    onRepeatPractice = {},
                    savedQuestions = SavedQuestionsState.Loaded(
                        listOf(SavedQuestion("q1", savedAtEpochMillis = 1_000)),
                    ),
                    onToggleSaved = {},
                )
            }
        }

        onNodeWithText("Unsave").assertIsDisplayed()
        onNodeWithText("Score: 1 / 1").assertIsDisplayed()
        onNodeWithTag(FocusedResultPracticeAgainTag).performScrollTo().assertIsDisplayed()
    }

    /** Saved state that failed to load leaves the result intact, without a misleading affordance. */
    @Test
    fun unavailableSavedStateStillShowsTheResult() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FocusedResultScreen(
                    state = contentState(questions = listOf(availableQuestion())),
                    onRetry = {}, onBack = {}, onSourceClick = {},
                    onRepeatPractice = {},
                    savedQuestions = SavedQuestionsState.Error,
                    onToggleSaved = {},
                )
            }
        }

        onNodeWithText("Score: 1 / 1").assertIsDisplayed()
        onNodeWithText("Question text").assertIsDisplayed()
        onNodeWithTag(reviewQuestionSaveTag("q1")).assertDoesNotExist()
    }

    private fun contentState(questions: List<ReviewQuestionItem>) =
        FocusedResultUiState.Content(
            attemptId = "attempt",
            totalQuestions = questions.size,
            correctAnswers = 1,
            percentage = 100.0,
            questions = questions,
        )

    private fun availableQuestion() = ReviewQuestionItem.Available(
        ReviewQuestionUiModel(
            questionId = "q1",
            topicId = "topic",
            subtopicId = "topic_basics",
            text = "Question text",
            isCorrect = true,
            answers = listOf(ReviewAnswerUiModel("a", "Answer A", true, true)),
            explanation = "Read the explanation",
            sources = listOf(ReviewSourceUiModel("Official docs", "https://example.com/docs")),
        ),
    )
}

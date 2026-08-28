package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel

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
    }
}

package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class AssessmentReviewComponentsTest {
    @Test
    fun questionReviewRendersAnswerMeaningExplanationAndOrderedSources() = runComposeUiTest {
        val openedUrls = mutableListOf<String>()
        setContent {
            AppTheme {
                ReviewQuestionCard(
                    question = ReviewQuestionUiModel(
                        questionId = "q1",
                        topicId = "topic",
                        text = "Question text",
                        isCorrect = false,
                        answers = listOf(
                            ReviewAnswerUiModel("a", "Selected wrong", true, false),
                            ReviewAnswerUiModel("b", "Missed correct", false, true),
                        ),
                        explanation = "Authored explanation",
                        sources = listOf(
                            ReviewSourceUiModel("Source B", "https://example.com/b"),
                            ReviewSourceUiModel("Source A", "https://example.com/a"),
                        ),
                    ),
                    onSourceClick = { openedUrls += it },
                )
            }
        }

        onNodeWithText("Incorrect").assertIsDisplayed()
        onNodeWithText("Your answer").assertIsDisplayed()
        onNodeWithText("Correct answer").assertIsDisplayed()
        onNodeWithText("Authored explanation").assertIsDisplayed()
        onNodeWithText("Source: Source B").performClick()
        onNodeWithText("Source: Source A").performClick()
        assertEquals(
            listOf("https://example.com/b", "https://example.com/a"),
            openedUrls,
        )
    }

    @Test
    fun aQuestionScoredCorrectShowsTheCorrectOutcome() = runComposeUiTest {
        setContent { AppTheme { ReviewQuestionCard(question(isCorrect = true), {}) } }

        onNodeWithText("Correct").assertIsDisplayed()
        onNodeWithText("Partially correct").assertDoesNotExist()
    }

    @Test
    fun pickingOnlyCorrectOptionsButMissingOneReadsAsPartiallyCorrect() = runComposeUiTest {
        // Scored incorrect, but the learner picked no wrong option - a flat "Incorrect" hid the
        // difference between a near miss and a completely wrong answer set.
        setContent {
            AppTheme {
                ReviewQuestionCard(
                    question(
                        isCorrect = false,
                        answers = listOf(
                            ReviewAnswerUiModel("a", "First correct", wasSelected = true, isCorrectAnswer = true),
                            ReviewAnswerUiModel("b", "Second correct", wasSelected = false, isCorrectAnswer = true),
                            ReviewAnswerUiModel("c", "A wrong option", wasSelected = false, isCorrectAnswer = false),
                        ),
                    ),
                    {},
                )
            }
        }

        onNodeWithText("Partially correct").assertIsDisplayed()
        onNodeWithText("Incorrect").assertDoesNotExist()
    }

    @Test
    fun pickingAWrongOptionStaysIncorrectEvenWithSomeCorrectPicks() = runComposeUiTest {
        setContent {
            AppTheme {
                ReviewQuestionCard(
                    question(
                        isCorrect = false,
                        answers = listOf(
                            ReviewAnswerUiModel("a", "First correct", wasSelected = true, isCorrectAnswer = true),
                            ReviewAnswerUiModel("c", "A wrong option", wasSelected = true, isCorrectAnswer = false),
                        ),
                    ),
                    {},
                )
            }
        }

        onNodeWithText("Incorrect").assertIsDisplayed()
        onNodeWithText("Partially correct").assertDoesNotExist()
    }

    @Test
    fun missingQuestionIsExplicit() = runComposeUiTest {
        setContent {
            AppTheme {
                MissingReviewQuestion("missing-id")
            }
        }

        onNodeWithText("Question missing-id is no longer available.").assertIsDisplayed()
    }

    private fun question(
        isCorrect: Boolean,
        answers: List<ReviewAnswerUiModel> = listOf(
            ReviewAnswerUiModel("a", "An answer", wasSelected = true, isCorrectAnswer = true),
        ),
    ): ReviewQuestionUiModel =
        ReviewQuestionUiModel(
            questionId = "q",
            topicId = "topic",
            text = "Question text",
            isCorrect = isCorrect,
            answers = answers,
            explanation = "Authored explanation",
            sources = emptyList(),
        )
}

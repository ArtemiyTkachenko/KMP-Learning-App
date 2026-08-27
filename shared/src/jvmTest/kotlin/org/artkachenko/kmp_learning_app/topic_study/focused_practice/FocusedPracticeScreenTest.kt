package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption

@OptIn(ExperimentalTestApi::class)
internal class FocusedPracticeScreenTest {
    @Test
    fun singleQuestionRendersProgressAndUsesStableAnswerId() = runComposeUiTest {
        var selectedId: String? = null
        setContent {
            MaterialTheme {
                FocusedPracticeScreen(
                    state = contentState(AnswerSelectionMode.SINGLE),
                    onAnswerClick = { selectedId = it },
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithText("Question 2 of 6").assertIsDisplayed()
        onNodeWithText("Answer B").performClick()
        assertEquals("answer_b", selectedId)
    }

    @Test
    fun multipleQuestionRendersCheckboxesAndSubmitCallback() = runComposeUiTest {
        var submitCount = 0
        setContent {
            MaterialTheme {
                FocusedPracticeScreen(
                    state = contentState(AnswerSelectionMode.MULTIPLE).copy(canSubmit = true),
                    onAnswerClick = {},
                    onSubmit = { submitCount += 1 },
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithText("Select all that apply").assertIsDisplayed()
        onNodeWithTag(FocusedPracticeSubmitTag).performClick()
        assertEquals(1, submitCount)
    }

    @Test
    fun submitIsDisabledWithoutSelectionAndWhileSubmitting() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FocusedPracticeScreen(
                    state = contentState(AnswerSelectionMode.SINGLE),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithTag(FocusedPracticeSubmitTag).assertIsNotEnabled()

        setContent {
            MaterialTheme {
                FocusedPracticeScreen(
                    state = contentState(AnswerSelectionMode.SINGLE).copy(
                        canSubmit = true,
                        isSubmitting = true,
                    ),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithTag(FocusedPracticeSubmitTag).assertIsNotEnabled()
    }

    @Test
    fun statesRenderWithoutExposingResults() = runComposeUiTest {
        var completeCount = 0
        setContent {
            MaterialTheme {
                FocusedPracticeScreen(
                    state = FocusedPracticeUiState.ReadyToComplete("attempt", 3),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = { completeCount += 1 },
                )
            }
        }
        onNodeWithText("All questions answered. Ready to finish.").assertIsDisplayed()
        onNodeWithTag(FocusedPracticeFinishTag).performClick()
        assertEquals(1, completeCount)

        setContent {
            MaterialTheme {
                FocusedPracticeScreen(
                    state = FocusedPracticeUiState.ReadyToComplete("attempt", 3, isCompleting = true),
                    onAnswerClick = {}, onSubmit = {}, onRetry = {}, onBack = {}, onComplete = {},
                )
            }
        }
        onNodeWithTag(FocusedPracticeFinishTag).assertIsNotEnabled()

        setContent {
            MaterialTheme {
                FocusedPracticeScreen(
                    state = FocusedPracticeUiState.Loading,
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithTag(FocusedPracticeLoadingTag).assertIsDisplayed()
    }

    private fun contentState(mode: AnswerSelectionMode) = FocusedPracticeUiState.Content(
        attemptId = "attempt",
        questionNumber = 2,
        totalQuestions = 6,
        question = FocusedQuestionUiModel(
            id = "question",
            text = "Question text",
            answers = listOf(
                AnswerOption("answer_a", "Answer A"),
                AnswerOption("answer_b", "Answer B"),
            ),
            selectionMode = mode,
        ),
        selectedAnswerIds = emptySet(),
        canSubmit = false,
        isSubmitting = false,
        submissionFailed = false,
    )
}

package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption

@OptIn(ExperimentalTestApi::class)
internal class AssessmentTakingScreenTest {
    @Test
    fun singleQuestionRendersProgressAndUsesStableAnswerId() = runComposeUiTest {
        var selectedId: String? = null
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
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
                AssessmentTakingScreen(
                    title = "Focused practice",
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
        onNodeWithTag(AssessmentTakingSubmitTag).performClick()
        assertEquals(1, submitCount)
    }

    @Test
    fun submitIsDisabledWithoutSelectionAndWhileSubmitting() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = contentState(AnswerSelectionMode.SINGLE),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithTag(AssessmentTakingSubmitTag).assertIsNotEnabled()

        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
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
        onNodeWithTag(AssessmentTakingSubmitTag).assertIsNotEnabled()
    }

    @Test
    fun statesRenderWithoutExposingResults() = runComposeUiTest {
        var completeCount = 0
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = AssessmentTakingUiState.ReadyToComplete("attempt", 3),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = { completeCount += 1 },
                )
            }
        }
        onNodeWithText("All questions answered. Ready to finish.").assertIsDisplayed()
        onNodeWithTag(AssessmentTakingFinishTag).performClick()
        assertEquals(1, completeCount)

        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = AssessmentTakingUiState.ReadyToComplete("attempt", 3, isCompleting = true),
                    onAnswerClick = {}, onSubmit = {}, onRetry = {}, onBack = {}, onComplete = {},
                )
            }
        }
        onNodeWithTag(AssessmentTakingFinishTag).assertIsNotEnabled()

        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = AssessmentTakingUiState.Loading,
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithTag(AssessmentTakingLoadingTag).assertIsDisplayed()
    }

    @Test
    fun rendersProductSpecificTitle() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Mixed Android Interview",
                    state = AssessmentTakingUiState.Loading,
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithText("Mixed Android Interview").assertIsDisplayed()
    }

    @Test
    fun noQuestionsAndStartErrorRemainActionable() = runComposeUiTest {
        var retryCount = 0
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Assessment",
                    state = AssessmentTakingUiState.NoQuestions,
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = { retryCount += 1 },
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithText("No practice questions are currently available.").assertIsDisplayed()

        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Assessment",
                    state = AssessmentTakingUiState.Error,
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = { retryCount += 1 },
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithText("Practice could not be started.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun persistenceFailuresKeepAssessmentContentVisible() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Assessment",
                    state = contentState(AnswerSelectionMode.SINGLE).copy(
                        selectedAnswerIds = setOf("answer_b"),
                        canSubmit = true,
                        submissionFailed = true,
                    ),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithText("Question text").assertIsDisplayed()
        onNodeWithText("Answer could not be saved. Try again.").assertIsDisplayed()

        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Assessment",
                    state = AssessmentTakingUiState.ReadyToComplete(
                        attemptId = "attempt",
                        totalQuestions = 3,
                        completionFailed = true,
                    ),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithText("Results could not be saved. Try again.").assertIsDisplayed()
        onNodeWithTag(AssessmentTakingFinishTag).assertIsDisplayed()
    }

    @Test
    fun theProgressMeterTracksHowFarThroughTheAssessmentTheLearnerIs() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    // Question 2 of 6 means one question is behind the learner, so the meter is
                    // one sixth full rather than a third — it reports completion, not position.
                    state = contentState(AnswerSelectionMode.SINGLE),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithTag(AssessmentProgressMeterTag)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(1f / 6f, 0f..1f))
    }

    @Test
    fun screensWithoutAQuestionCarryNoProgressMeter() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = AssessmentTakingUiState.ReadyToComplete(
                        attemptId = "attempt",
                        totalQuestions = 6,
                        isCompleting = false,
                        completionFailed = false,
                    ),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithTag(AssessmentProgressMeterTag).assertDoesNotExist()
    }

    private fun contentState(mode: AnswerSelectionMode) = AssessmentTakingUiState.Content(
        attemptId = "attempt",
        questionNumber = 2,
        totalQuestions = 6,
        question = AssessmentQuestionUiModel(
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

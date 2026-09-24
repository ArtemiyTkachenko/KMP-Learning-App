package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference

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
    fun questionAndAnswerRowsExposeTheirAssessmentSemantics() = runComposeUiTest {
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

        onNodeWithText("Question text").assert(isHeading())
        onNodeWithText("Answer A").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )

        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = contentState(AnswerSelectionMode.MULTIPLE),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }
        onNodeWithText("Answer A").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
        )
    }

    @Test
    fun aNewQuestionStartsAtTheTopOfItsOwnContent() = runComposeUiTest {
        val first = contentState(AnswerSelectionMode.SINGLE).copy(
            question = AssessmentQuestionUiModel(
                id = "question_a",
                text = "Question A",
                answers = (1..30).map { AnswerOption("answer_$it", "Answer $it") },
                selectionMode = AnswerSelectionMode.SINGLE,
            ),
        )
        val state = mutableStateOf(first)
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = state.value,
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNode(hasScrollAction()).performScrollToNode(hasText("Answer 25"))
        onNodeWithText("Answer 25").assertIsDisplayed()
        runOnIdle {
            state.value = first.copy(
                questionNumber = 3,
                question = first.question.copy(id = "question_b", text = "Question B"),
            )
        }

        onNodeWithText("Question B").assertIsDisplayed()
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
    fun multipleQuestionWithOneCorrectAnswerRendersMultiSelectionAffordance() = runComposeUiTest {
        val question = Question(
            id = "multiple-one-correct",
            topicId = "topic",
            subtopicId = "subtopic",
            text = "Select all that apply.",
            answers = listOf(
                AnswerOption("answer_a", "Answer A"),
                AnswerOption("answer_b", "Answer B"),
            ),
            selectionMode = AnswerSelectionMode.MULTIPLE,
            level = QuestionLevel.FOUNDATION,
            correctAnswerIds = listOf("answer_a"),
            explanation = "Answer A is correct.",
            sources = listOf(SourceReference("Source", "https://example.com")),
        )
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    // Only the authored interaction mode crosses into the UI model.
                    state = contentState(question.selectionMode),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithText("Select all that apply").assertIsDisplayed()
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

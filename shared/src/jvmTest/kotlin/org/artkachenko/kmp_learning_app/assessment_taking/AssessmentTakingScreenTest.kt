package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.curriculum.AnswerOption
import org.artkachenko.kmp_learning_app.curriculum.AnswerSelectionMode
import org.artkachenko.kmp_learning_app.curriculum.Question
import org.artkachenko.kmp_learning_app.curriculum.QuestionLevel
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

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

    /**
     * Finishing states its own condition instead of removing the word that explains it.
     *
     * The button used to replace its label with a bare `CircularProgressIndicator`, which is
     * Material's 40dp standalone size — so the one moment the control was busy was the one moment
     * it changed height, and what it changed into said nothing about why. It is the same rule the
     * retake control and the practice Submit follow, and this was the last control in the app not
     * following it.
     */
    @Test
    fun finishingKeepsTheButtonSayingWhatItIsDoing() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = AssessmentTakingUiState.ReadyToComplete(
                        attemptId = "attempt",
                        totalQuestions = 3,
                        isCompleting = true,
                    ),
                    onAnswerClick = {}, onSubmit = {}, onRetry = {}, onBack = {}, onComplete = {},
                )
            }
        }

        onNodeWithTag(AssessmentTakingFinishTag).assertIsNotEnabled()
        onNodeWithText("Finishing").assertIsDisplayed()
        onNodeWithText("Finish and view results").assertDoesNotExist()
    }

    /**
     * The meter measures the question column, so it has to be the width of the question column.
     *
     * It is pinned outside the scrolling pane, which is right — it must stay readable while a long
     * question scrolls — but it took the window's full width to get there. That is the same thing
     * as the content column only while the window is narrower than `AppContentWidth.Standard`'s
     * cap; past that the question is centred in 840dp and the meter ran the whole 1440dp, so the
     * bar and the thing it measured no longer started at the same place.
     */
    @Test
    fun theProgressMeterIsTheWidthOfTheQuestionColumnRatherThanTheWindow() =
        runSkikoComposeUiTest(size = WideDisplay) {
            setContent {
                AppTheme {
                    CompositionLocalProvider(
                        LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                    ) {
                        Box(Modifier.size(WideWidth, WideHeight).testTag(WindowRootTag)) {
                            AssessmentTakingScreen(
                                title = "Focused practice",
                                state = contentState(AnswerSelectionMode.SINGLE),
                                onAnswerClick = {}, onSubmit = {}, onRetry = {}, onBack = {},
                                onComplete = {},
                            )
                        }
                    }
                }
            }

            val windowWidth = onNodeWithTag(WindowRootTag).fetchSemanticsNode().boundsInRoot.width
            val meter = onNodeWithTag(AssessmentProgressMeterTag)
                .assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            val question = onNodeWithText("Question text").fetchSemanticsNode().boundsInRoot

            assertTrue(
                meter.width < windowWidth * 0.75f,
                "The meter spanned ${meter.width}px of a ${windowWidth}px window; it should take " +
                    "the capped content column instead.",
            )
            // The bar and the question it measures begin together, within a pixel of rounding.
            assertTrue(
                kotlin.math.abs(meter.left - question.left) <= 1f,
                "The meter starts at ${meter.left}px and the question at ${question.left}px.",
            )
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

    /**
     * The reveal, which is the whole point of formative practice and had no coverage at all.
     *
     * What is asserted is that every option says what it was — including the one the learner did
     * not pick and should have — and that the verdict is named. The colours behind those labels are
     * the palette's and are asserted there; a screen test that pinned them would fail on every
     * deliberate theme change.
     */
    @Test
    fun theRevealMarksEveryOptionAndNamesTheVerdict() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = revealedState(
                        mode = AnswerSelectionMode.SINGLE,
                        selected = setOf("answer_b"),
                        correct = listOf("answer_a"),
                        isCorrect = false,
                    ),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithTag(AssessmentTakingOutcomeTag).assert(hasAnyDescendant(hasText("Incorrect")))
        onNodeWithText("\u2715 Incorrectly selected").assertIsDisplayed()
        onNodeWithText("\u2713 Correct answer").assertIsDisplayed()
        onNodeWithText("Explanation").assertIsDisplayed()
        onNodeWithText("Because A.").assertIsDisplayed()
    }

    /**
     * Picking two of three correct options and nothing wrong is not the same as picking the wrong
     * one, and the reveal now says so. The score is untouched — this is presentation of a question
     * that was still recorded as incorrect.
     */
    @Test
    fun aPartlyRightAnswerIsNamedRatherThanCollapsedIntoWrong() = runComposeUiTest {
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = revealedState(
                        mode = AnswerSelectionMode.MULTIPLE,
                        selected = setOf("answer_a"),
                        correct = listOf("answer_a", "answer_b"),
                        isCorrect = false,
                    ),
                    onAnswerClick = {},
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithTag(AssessmentTakingOutcomeTag).assert(hasAnyDescendant(hasText("Partially correct")))
        onNodeWithText("\u2713 Correctly selected").assertIsDisplayed()
        onNodeWithText("\u2713 Correct answer").assertIsDisplayed()
    }

    /** Once the answer is revealed the rows are a record, so the choice can no longer be changed. */
    @Test
    fun revealedOptionsStopAcceptingInput() = runComposeUiTest {
        var clicks = 0
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = revealedState(
                        mode = AnswerSelectionMode.SINGLE,
                        selected = setOf("answer_a"),
                        correct = listOf("answer_a"),
                        isCorrect = true,
                    ),
                    onAnswerClick = { clicks += 1 },
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithTag(AssessmentTakingOutcomeTag).assert(hasAnyDescendant(hasText("Correct")))
        onNodeWithText("Answer B").assertIsNotEnabled()
        onNodeWithText("Answer B").performClick()
        assertEquals(0, clicks)
    }

    /**
     * Selection is still selection once the row owns its own interaction source.
     *
     * The press treatment is driven by a [androidx.compose.foundation.interaction.MutableInteractionSource]
     * the row now supplies to `selectable`/`toggleable`, which is exactly the parameter that decides
     * whether a click is delivered at all. What matters is not that the row shrinks, but that every
     * option stays live and reports its own identity while the question is open — including the one
     * already chosen, so a learner can change their mind.
     */
    @Test
    fun anOpenQuestionLetsTheLearnerChangeTheirChoice() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = contentState(AnswerSelectionMode.SINGLE).copy(
                        selectedAnswerIds = setOf("answer_a"),
                        canSubmit = true,
                    ),
                    onAnswerClick = { clicked += it },
                    onSubmit = {},
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithText("Answer B").assertIsEnabled().performClick()
        onNodeWithText("Answer A").assertIsEnabled().performClick()
        assertEquals(listOf("answer_b", "answer_a"), clicked)
    }

    /**
     * The one control at the bottom is the question's next action throughout, so it has to still be
     * the next action after the reveal. The label now crosses over inside an `AnimatedContent`
     * rather than being replaced outright; what is asserted is the settled state and the callback,
     * never the crossfade.
     */
    @Test
    fun theRevealTurnsTheActionIntoContinuingRatherThanReplacingIt() = runComposeUiTest {
        var submitCount = 0
        var nextCount = 0
        setContent {
            MaterialTheme {
                AssessmentTakingScreen(
                    title = "Focused practice",
                    state = revealedState(
                        mode = AnswerSelectionMode.SINGLE,
                        selected = setOf("answer_a"),
                        correct = listOf("answer_a"),
                        isCorrect = true,
                    ),
                    onAnswerClick = {},
                    onSubmit = { submitCount += 1 },
                    onNext = { nextCount += 1 },
                    onRetry = {},
                    onBack = {},
                    onComplete = {},
                )
            }
        }

        onNodeWithTag(AssessmentTakingSubmitTag)
            .assertIsEnabled()
            // The Button merges its descendants, so the settled label reads off the button node.
            .assert(hasText("Next question"))
            .performClick()
        assertEquals(1, nextCount)
        assertEquals(0, submitCount)
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

    private fun revealedState(
        mode: AnswerSelectionMode,
        selected: Set<String>,
        correct: List<String>,
        isCorrect: Boolean,
    ) = contentState(mode).let { base ->
        base.copy(
            question = base.question.copy(
                correctAnswerIds = correct,
                explanation = "Because A.",
            ),
            selectedAnswerIds = selected,
            canSubmit = true,
            feedback = PracticeFeedback(isCorrect = isCorrect),
        )
    }
}

private const val WindowRootTag = "assessment_taking_window_root"

/** Comfortably past the expanded breakpoint, so the content column is actually capped. */
private val WideWidth = 1440.dp
private val WideHeight = 900.dp
private val WideDisplay = Size(WideWidth.value, WideHeight.value)

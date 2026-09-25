package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment_review.ReviewAnswerUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionUiModel
import org.artkachenko.kmp_learning_app.assessment_review.ReviewSourceUiModel
import org.artkachenko.kmp_learning_app.assessment_review.reviewQuestionSaveTag
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestion
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme
import org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

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

        onNodeWithText("1 / 2").assertIsDisplayed()
        onNodeWithText("50", substring = true).assertDoesNotExist()
        onNodeWithText("Correct").assertIsDisplayed()
        onNodeWithText("Read the explanation").assertDoesNotExist()
        onNodeWithText("Review answer").performClick()
        onNodeWithText("✓ Correctly selected").assertIsDisplayed()
        onNodeWithText("Explanation").assertIsDisplayed()
        onNodeWithText("Source: Official docs").performScrollTo().performClick()
        assertEquals("https://example.com/docs", openedUrl)
        onNode(hasScrollAction()).performScrollToNode(
            hasText("Question q2 is no longer available."),
        )
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

        onNodeWithText("Review answer").performClick()
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
    fun aSavedQuestionReadsAsSavedAndLeavesTheResultUnchanged() = runComposeUiTest {
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

        onNodeWithText("Saved").assertIsDisplayed()
        onNodeWithText("1 / 1").assertIsDisplayed()
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

        onNodeWithText("1 / 1").assertIsDisplayed()
        onNodeWithText("Question text").assertIsDisplayed()
        onNodeWithTag(reviewQuestionSaveTag("q1")).assertDoesNotExist()
    }

    /**
     * The expanded arrangement, which had no test of its own.
     *
     * Focused's summary pane is the sparser of the two — there is no per-Topic breakdown to put
     * beside the outcome — so what matters is that the hero still anchors it and the action is still
     * in it, rather than the pane quietly becoming an empty half of the window. The assertions go
     * through the pane tags because "both are on screen somewhere" would pass equally well if the
     * split had collapsed back to one column.
     */
    @Test
    fun theExpandedResultKeepsTheOutcomeAndItsActionInTheSummaryPane() =
        runSkikoComposeUiTest(size = DesktopDisplay) {
            setContent {
                AppTheme {
                    CompositionLocalProvider(
                        LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                    ) {
                        Box(Modifier.size(DesktopWidth, DesktopHeight)) {
                            FocusedResultScreen(
                                state = contentState(questions = listOf(availableQuestion())),
                                onRetry = {}, onBack = {}, onSourceClick = {},
                                onRepeatPractice = {},
                            )
                        }
                    }
                }
            }

            onNodeWithTag(FocusedResultSummaryPaneTag).assertIsDisplayed()
            onNodeWithTag(FocusedResultReviewPaneTag).assertIsDisplayed()

            onNode(
                hasText("1 / 1") and hasAnyAncestor(hasTestTag(FocusedResultSummaryPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasTestTag(FocusedResultPracticeAgainTag) and
                    hasAnyAncestor(hasTestTag(FocusedResultSummaryPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Question review") and
                    hasAnyAncestor(hasTestTag(FocusedResultReviewPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Question text") and hasAnyAncestor(hasTestTag(FocusedResultReviewPaneTag)),
            ).assertIsDisplayed()
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

/** Comfortably past the expanded breakpoint, so the two-pane arrangement actually composes. */
private val DesktopWidth = 1440.dp
private val DesktopHeight = 900.dp
private val DesktopDisplay = Size(DesktopWidth.value, DesktopHeight.value)

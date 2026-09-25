package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
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

        onNodeWithText("3 / 5").assertIsDisplayed()
        onNodeWithText("60", substring = true).assertIsDisplayed()
        onNodeWithText("Performance by topic").performScrollTo().assertIsDisplayed()
        onNodeWithText("Kotlin").performScrollTo().assertIsDisplayed()
        onNodeWithText("Topic unavailable").performScrollTo().assertIsDisplayed()
        onNodeWithText("2 / 3 correct").performScrollTo().assertIsDisplayed()
        onNodeWithText("Question review").performScrollTo().assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("Review answer"))
        onNodeWithText("Review answer").performClick()
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

        onNode(hasScrollAction()).performScrollToNode(hasText("Review answer"))
        onNodeWithText("Review answer").performClick()
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

        onNodeWithText("Retake interview").assertIsDisplayed().performClick()
        assertEquals(1, repeats)
    }

    @Test
    fun creatingDisablesActionShowsProgressAndKeepsResultVisible() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    retakeState = AssessmentRetakeState.Creating,
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithTag(MixedResultPracticeAgainTag).assertIsNotEnabled()
        onNodeWithTag(MixedResultCreatingIndicatorTag).assertIsDisplayed()
        onNodeWithText("Starting interview").assertIsDisplayed()
        onNodeWithText("3 / 5").assertIsDisplayed()
        onNodeWithText("Performance by topic").performScrollTo().assertIsDisplayed()
        onNode(hasScrollAction()).performScrollToNode(hasText("Review answer"))
        onNodeWithText("Review answer").performClick()
        onNodeWithText("Authored explanation").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun retakeFailuresShowSpecificMessagesAndKeepResultVisible() = runComposeUiTest {
        var repeatState: AssessmentRetakeState by mutableStateOf(
            AssessmentRetakeState.SourceAttemptNotFound,
        )
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    retakeState = repeatState,
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                )
            }
        }

        onNodeWithText("The original interview is no longer available.").assertIsDisplayed()
        onNodeWithText("3 / 5").assertIsDisplayed()

        repeatState = AssessmentRetakeState.NoEligibleQuestions
        onNodeWithText("No interview questions are currently available.").assertIsDisplayed()
        onNodeWithText("Retake interview").assertIsDisplayed()

        repeatState = AssessmentRetakeState.Error
        onNodeWithText("Interview could not be started. Try again.").assertIsDisplayed()
        onNodeWithText("3 / 5").assertIsDisplayed()
    }

    /** The same shared card and the same saved identity the other review surfaces use. */
    @Test
    fun savingAMixedResultQuestionReportsItsExactIdAndMissingOnesOfferNothing() = runComposeUiTest {
        val toggled = mutableListOf<String>()
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                    savedQuestions = SavedQuestionsState.Loaded(emptyList()),
                    onToggleSaved = toggled::add,
                )
            }
        }

        onNodeWithTag(reviewQuestionSaveTag("q1")).performScrollTo().performClick()
        assertEquals(listOf("q1"), toggled)
        onNodeWithTag(reviewQuestionSaveTag("missing")).assertDoesNotExist()
    }

    @Test
    fun aSavedMixedResultQuestionReadsAsSavedWithoutDisturbingTheBreakdown() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                    savedQuestions = SavedQuestionsState.Loaded(
                        listOf(SavedQuestion("q1", savedAtEpochMillis = 1_000)),
                    ),
                    onToggleSaved = {},
                )
            }
        }

        onNodeWithText("3 / 5").assertIsDisplayed()
        onNodeWithText("Kotlin").performScrollTo().assertIsDisplayed()
        onNodeWithText("2 / 3 correct").performScrollTo().assertIsDisplayed()
        onNodeWithText("Saved").performScrollTo().assertIsDisplayed()
        onNodeWithText("Retake interview").assertIsDisplayed()
    }

    /** A saved-state failure must not remove the result the learner came here to read. */
    @Test
    fun unavailableSavedStateLeavesTheMixedResultIntact() = runComposeUiTest {
        setContent {
            MaterialTheme {
                MixedInterviewResultScreen(
                    state = contentState(),
                    onRetry = {},
                    onBack = {},
                    onSourceClick = {},
                    savedQuestions = SavedQuestionsState.Error,
                    onToggleSaved = {},
                )
            }
        }

        onNodeWithText("3 / 5").assertIsDisplayed()
        onNodeWithText("Question review").performScrollTo().assertIsDisplayed()
        onNodeWithTag(reviewQuestionSaveTag("q1")).assertDoesNotExist()
    }

    /**
     * The expanded arrangement, which had no test of its own.
     *
     * Two panes, each holding what it is for: the outcome and the per-Topic breakdown on one side,
     * the transcript on the other. The assertions go through the pane tags rather than through bare
     * text, because "both are on screen somewhere" would pass just as well if the split had silently
     * collapsed back to one column — which is the regression worth catching, now that the hero and
     * the review heading both change their treatment with the window class.
     */
    @Test
    fun theExpandedResultAnchorsTheSummaryPaneAndScrollsTheTranscriptBesideIt() =
        runSkikoComposeUiTest(size = DesktopDisplay) {
            setContent {
                AppTheme {
                    CompositionLocalProvider(
                        LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                    ) {
                        Box(Modifier.size(DesktopWidth, DesktopHeight)) {
                            MixedInterviewResultScreen(
                                state = contentState(),
                                onRetry = {},
                                onBack = {},
                                onSourceClick = {},
                            )
                        }
                    }
                }
            }

            onNodeWithTag(MixedResultSummaryPaneTag).assertIsDisplayed()
            onNodeWithTag(MixedResultReviewPaneTag).assertIsDisplayed()

            onNode(
                hasText("3 / 5") and hasAnyAncestor(hasTestTag(MixedResultSummaryPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Retake interview") and hasAnyAncestor(hasTestTag(MixedResultSummaryPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Performance by topic") and
                    hasAnyAncestor(hasTestTag(MixedResultSummaryPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Question review") and hasAnyAncestor(hasTestTag(MixedResultReviewPaneTag)),
            ).assertIsDisplayed()
            onNode(
                hasText("Question text") and hasAnyAncestor(hasTestTag(MixedResultReviewPaneTag)),
            ).assertIsDisplayed()
        }

    /**
     * The per-Topic breakdown is one grouped table rather than a card per Topic, and every row in
     * it stays inert.
     *
     * A result is a record of what happened, and there is no per-Topic destination to reach from
     * one — so a chevron or a click here would advertise navigation the screen cannot perform. The
     * group takes no click of its own either, which is what keeps that true for the whole section
     * rather than row by row.
     */
    @Test
    fun theTopicBreakdownIsOneInertGroupedTable() = runComposeUiTest {
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

        onNode(hasScrollAction()).performScrollToNode(hasTestTag(MixedResultTopicGroupTag))
        val insideGroup = hasAnyAncestor(hasTestTag(MixedResultTopicGroupTag))
        onNode(hasText("Kotlin") and insideGroup)
            .assertIsDisplayed()
            .assert(hasText("2 / 3 correct", substring = true))
            .assertHasNoClickAction()
        // A Topic the curriculum has dropped still gets its row and its fallback name.
        onNode(hasText("Topic unavailable") and insideGroup).assertHasNoClickAction()
        onNodeWithTag(MixedResultTopicGroupTag).assertHasNoClickAction()
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

/** Comfortably past the expanded breakpoint, so the two-pane arrangement actually composes. */
private val DesktopWidth = 1440.dp
private val DesktopHeight = 900.dp
private val DesktopDisplay = Size(DesktopWidth.value, DesktopHeight.value)

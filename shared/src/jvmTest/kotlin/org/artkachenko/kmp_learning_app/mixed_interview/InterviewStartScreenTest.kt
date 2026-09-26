package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.artkachenko.kmp_learning_app.ui.theme.AppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

@OptIn(ExperimentalTestApi::class)
internal class InterviewStartScreenTest {
    /**
     * The first visit is a state, not a gap. The screen explains what an Interview is, says what
     * will appear here once one is finished, and offers no empty table and no zeroed score.
     */
    @Test
    fun aFirstVisitExplainsTheSessionAndSaysWhereResultsWillAppear() = runComposeUiTest {
        setContent {
            MaterialTheme {
                InterviewStartScreen(onStartMixedInterview = {}, history = InterviewHistoryUiState.Empty)
            }
        }

        onNodeWithText("Mixed Android Interview").assertIsDisplayed()
        onNodeWithText("Mixed Android Interview").assert(isHeading())
        // The figure and its unit are one announced fact rather than the two fragments "20" and
        // "questions"; the assertion is on the information, not on which component draws it.
        onNodeWithText("20 questions").assertIsDisplayed()
        onNodeWithText("Test your knowledge across Android topics.").assertIsDisplayed()
        // The two rules that make an Interview different from Practice, stated before the learner
        // commits rather than discovered on question one.
        onNodeWithText(
            "Questions are drawn across the whole curriculum, so a mixed interview is the " +
                "closest thing to the real conversation.",
        ).assertIsDisplayed()
        onNodeWithText(
            "Answers are reviewed when the interview is complete, not question by question.",
        ).assertIsDisplayed()
        onNodeWithTag(InterviewStartButtonTag).assertIsDisplayed()

        onNodeWithTag(InterviewNoHistoryTag).assertIsDisplayed()
        onNodeWithText("No interviews yet").assertIsDisplayed()
        onNodeWithTag(InterviewRecordTag).assertDoesNotExist()
        onNodeWithText("0 of 20 correct").assertDoesNotExist()
    }

    /** A record that has not been read yet is not an absent one: neither shape is drawn over it. */
    @Test
    fun aRecordStillLoadingShowsNeitherTheFirstVisitStateNorAnEmptyTable() = runComposeUiTest {
        setContent {
            MaterialTheme {
                InterviewStartScreen(
                    onStartMixedInterview = {},
                    history = InterviewHistoryUiState.Loading,
                )
            }
        }

        onNodeWithTag(InterviewHistoryLoadingTag).assertIsDisplayed()
        onNodeWithTag(InterviewNoHistoryTag).assertDoesNotExist()
        onNodeWithTag(InterviewRecordTag).assertDoesNotExist()
    }

    @Test
    fun theLatestAndBestResultsAreShownAndOpenTheirOwnAttempt() = runComposeUiTest {
        val opened = mutableListOf<String>()
        setContent {
            MaterialTheme {
                InterviewStartScreen(
                    onStartMixedInterview = {},
                    history = historyState(
                        attemptCount = 4,
                        latest = InterviewAttemptUiModel("latest", 5, 20, 25.0, CompletedAt),
                        best = InterviewAttemptUiModel("best", 18, 20, 90.0, CompletedAt),
                    ),
                    onOpenResult = { opened += it },
                )
            }
        }

        onNodeWithText("4 completed").assertIsDisplayed()
        onNodeWithText("5 of 20 correct").assertIsDisplayed()
        onNodeWithText("18 of 20 correct").assertIsDisplayed()

        onNodeWithText("Last interview")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        onNodeWithText("Best")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf("latest", "best"), opened)
    }

    /**
     * The latest and the best are one record, not two stacked results.
     *
     * The assertion is an ancestry one, because what changed is the number of containers and not
     * what either row says or does: both still navigate by their own stable attempt ID, and both
     * still carry their own `Role.Button` — a group takes no click of its own that could make one
     * row reachable through the other.
     */
    @Test
    fun bothRecordRowsShareOneContainerAndKeepTheirOwnNavigation() = runComposeUiTest {
        val opened = mutableListOf<String>()
        setContent {
            MaterialTheme {
                InterviewStartScreen(
                    onStartMixedInterview = {},
                    history = historyState(
                        attemptCount = 4,
                        latest = InterviewAttemptUiModel("latest", 5, 20, 25.0, CompletedAt),
                        best = InterviewAttemptUiModel("best", 18, 20, 90.0, CompletedAt),
                    ),
                    onOpenResult = { opened += it },
                )
            }
        }

        val insideGroup = hasAnyAncestor(hasTestTag(InterviewRecordGroupTag))
        onNode(hasText("Last interview") and insideGroup)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        onNode(hasText("Best") and insideGroup)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(listOf("latest", "best"), opened)
    }

    @Test
    fun oneInterviewIsNotListedTwice() = runComposeUiTest {
        val only = InterviewAttemptUiModel("only", 7, 20, 35.0, CompletedAt)
        setContent {
            MaterialTheme {
                InterviewStartScreen(
                    onStartMixedInterview = {},
                    history = historyState(
                        attemptCount = 1,
                        latest = only,
                        best = only,
                    ),
                )
            }
        }

        onNodeWithText("Last interview").assertIsDisplayed()
        onNodeWithText("Best").assertDoesNotExist()
    }

    /**
     * The hero's one primary action still starts an interview.
     *
     * The invitation was rebuilt around a gradient surface with a staggered entrance, so the button
     * is now a descendant of an `AnimatedVisibility` and carries a delayed `fadeIn`. Neither may
     * gate the interaction: the control is composed and clickable from the first frame, and the
     * click is driven here without waiting for the motion to settle.
     */
    @Test
    fun startingTheInterviewInvokesTheCallbackWithoutWaitingForTheEntrance() = runComposeUiTest {
        var started = 0
        setContent {
            MaterialTheme {
                InterviewStartScreen(
                    onStartMixedInterview = { started += 1 },
                    history = InterviewHistoryUiState.Empty,
                )
            }
        }

        onNodeWithTag(InterviewStartButtonTag).performClick()

        assertEquals(1, started)
    }

    /**
     * The expanded arrangement is still an invitation beside a record.
     *
     * Asserted on the information each pane carries rather than on the panes themselves, because
     * what has to hold is that neither half disappeared when the invitation became a hero — not
     * which container draws them.
     */
    @Test
    fun anExpandedWindowKeepsBothTheInvitationAndTheRecord() = runComposeUiTest {
        setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalAppWindowSizeClass provides AppWindowSizeClass.Expanded,
                ) {
                    Box(Modifier.size(1200.dp, 900.dp)) {
                        InterviewStartScreen(
                            onStartMixedInterview = {},
                            history = historyState(
                                attemptCount = 4,
                                latest = InterviewAttemptUiModel("latest", 5, 20, 25.0, CompletedAt),
                                best = InterviewAttemptUiModel("best", 18, 20, 90.0, CompletedAt),
                            ),
                        )
                    }
                }
            }
        }

        onNodeWithTag(InterviewHeroTag).assertIsDisplayed()
        onNodeWithText("Mixed Android Interview").assert(isHeading())
        onNodeWithText("20 questions").assertIsDisplayed()
        onNodeWithTag(InterviewStartButtonTag).assertIsDisplayed()

        onNodeWithTag(InterviewRecordTag).assertIsDisplayed()
        onNodeWithText("Last interview").assertIsDisplayed()
        onNodeWithText("18 of 20 correct").assertIsDisplayed()
    }

    @Test
    fun historyRemainsReachableInACompactHeight() = runComposeUiTest {
        setContent {
            MaterialTheme {
                Box(Modifier.height(320.dp)) {
                    InterviewStartScreen(
                        onStartMixedInterview = {},
                        history = historyState(
                            attemptCount = 4,
                            latest = InterviewAttemptUiModel("latest", 5, 20, 25.0, CompletedAt),
                            best = InterviewAttemptUiModel("best", 18, 20, 90.0, CompletedAt),
                        ),
                    )
                }
            }
        }

        // The record sits below the fold at this height and the screen is a LazyColumn, so the row
        // is not composed until the list is driven to it — `performScrollTo` can only reach a node
        // that already exists.
        onNode(hasScrollAction()).performScrollToNode(hasText("Best"))
        onNodeWithText("Best").assertIsDisplayed()
        onNodeWithText("18 of 20 correct").assertIsDisplayed()
    }
}

/** Wraps the record in the state the screen takes, keeping these call sites unchanged in shape. */
private fun historyState(
    attemptCount: Int,
    latest: InterviewAttemptUiModel,
    best: InterviewAttemptUiModel,
): InterviewHistoryUiState =
    InterviewHistoryUiState.Content(InterviewHistoryUiModel(attemptCount, latest, best))

/**
 * A fixed completion instant, so the record's date renders deterministically whatever the agent's
 * clock and zone happen to be. `InterviewStartScreenTest` asserts on scores and labels rather than
 * on the formatted date, which `TimestampTest` covers directly.
 */
private val CompletedAt: Instant = Instant.fromEpochMilliseconds(1_757_594_626_872)

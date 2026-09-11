package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

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
        onNodeWithText("20-question interview").assertIsDisplayed()
        // The rule that makes an Interview different from Practice, stated before the learner
        // commits rather than discovered on question one.
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

        onNodeWithText("Last interview").performClick()
        onNodeWithText("Best").performClick()

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

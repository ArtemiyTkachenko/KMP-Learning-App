package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class InterviewStartScreenTest {
    @Test
    fun aFirstVisitShowsTheInvitationWithoutAnEmptyRecord() = runComposeUiTest {
        setContent {
            MaterialTheme {
                InterviewStartScreen(onStartMixedInterview = {}, history = null)
            }
        }

        onNodeWithTag(InterviewStartButtonTag).assertIsDisplayed()
        onNodeWithTag(InterviewRecordTag).assertDoesNotExist()
    }

    @Test
    fun theLatestAndBestResultsAreShownAndOpenTheirOwnAttempt() = runComposeUiTest {
        val opened = mutableListOf<String>()
        setContent {
            MaterialTheme {
                InterviewStartScreen(
                    onStartMixedInterview = {},
                    history = InterviewHistoryUiModel(
                        attemptCount = 4,
                        latest = InterviewAttemptUiModel("latest", 5, 20, 25.0),
                        best = InterviewAttemptUiModel("best", 18, 20, 90.0),
                    ),
                    onOpenResult = { opened += it },
                )
            }
        }

        onNodeWithText("4 completed").assertIsDisplayed()
        onNodeWithText("5 of 20 correct").assertIsDisplayed()
        onNodeWithText("18 of 20 correct").assertIsDisplayed()

        onNodeWithText("Latest").performClick()
        onNodeWithText("Best").performClick()

        assertEquals(listOf("latest", "best"), opened)
    }

    @Test
    fun oneInterviewIsNotListedTwice() = runComposeUiTest {
        val only = InterviewAttemptUiModel("only", 7, 20, 35.0)
        setContent {
            MaterialTheme {
                InterviewStartScreen(
                    onStartMixedInterview = {},
                    history = InterviewHistoryUiModel(
                        attemptCount = 1,
                        latest = only,
                        best = only,
                    ),
                )
            }
        }

        onNodeWithText("Latest").assertIsDisplayed()
        onNodeWithText("Best").assertDoesNotExist()
    }
}

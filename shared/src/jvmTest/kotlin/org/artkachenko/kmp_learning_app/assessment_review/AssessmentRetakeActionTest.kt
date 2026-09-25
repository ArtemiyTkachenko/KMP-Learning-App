package org.artkachenko.kmp_learning_app.assessment_review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

private const val ActionTag = "retake_action"
private const val ProgressTag = "retake_progress"

/**
 * One control for both result screens, so its six states are covered once here rather than twice
 * through the screens that host it. What each screen still owns — which five words it supplies — is
 * covered by its own test.
 */
@OptIn(ExperimentalTestApi::class)
internal class AssessmentRetakeActionTest {

    /**
     * Busy is one control saying one thing, not a spinner where a label used to be with the
     * explanation floating above it. The spinner and the word that explains it are both inside the
     * button, so this asserts them on the button's own node.
     */
    @Test
    fun creatingLocksTheActionAndSaysSoInsideIt() = runComposeUiTest {
        var taps = 0
        setContent {
            AppTheme {
                AssessmentRetakeAction(
                    state = AssessmentRetakeState.Creating,
                    wording = wording,
                    onRetake = { taps += 1 },
                    actionTestTag = ActionTag,
                    progressTestTag = ProgressTag,
                )
            }
        }

        onNodeWithTag(ActionTag).assertIsNotEnabled()
        onNodeWithTag(ProgressTag).assertIsDisplayed()
        onNodeWithText("Starting practice").assertIsDisplayed()
        onNodeWithText("Practice again").assertDoesNotExist()

        onNodeWithTag(ActionTag).performClick()
        assertEquals(0, taps)
    }

    /**
     * A created retake is still busy, which is the whole point of `Created` being terminal: the
     * attempt exists durably and navigation to it has not happened yet, so a second press here
     * would create a second one. The control must not reopen before the controller says so.
     */
    @Test
    fun aCreatedRetakeKeepsTheActionLockedUntilItHasBeenOpened() = runComposeUiTest {
        setContent {
            AppTheme {
                AssessmentRetakeAction(
                    state = AssessmentRetakeState.Created("retake-attempt"),
                    wording = wording,
                    onRetake = {},
                    actionTestTag = ActionTag,
                    progressTestTag = ProgressTag,
                )
            }
        }

        onNodeWithTag(ActionTag).assertIsNotEnabled()
        onNodeWithTag(ProgressTag).assertIsDisplayed()
    }

    /**
     * Nothing durable was created by a failure, so pressing the button again is the retry — the
     * control stays live and states which failure it was. Each message replaces the last rather
     * than accumulating.
     */
    @Test
    fun eachFailureIsNamedAndLeavesTheActionRetryable() = runComposeUiTest {
        var state: AssessmentRetakeState by mutableStateOf(
            AssessmentRetakeState.SourceAttemptNotFound,
        )
        var taps = 0
        setContent {
            AppTheme {
                AssessmentRetakeAction(
                    state = state,
                    wording = wording,
                    onRetake = { taps += 1 },
                    actionTestTag = ActionTag,
                    progressTestTag = ProgressTag,
                )
            }
        }

        onNodeWithText("The original attempt is gone.").assertIsDisplayed()

        state = AssessmentRetakeState.NoEligibleQuestions
        onNodeWithText("No questions available.").assertIsDisplayed()
        onNodeWithText("The original attempt is gone.").assertDoesNotExist()

        state = AssessmentRetakeState.Error
        onNodeWithText("Could not start. Try again.").assertIsDisplayed()

        onNodeWithTag(ActionTag).assertIsEnabled().performClick()
        assertEquals(1, taps)
        onNodeWithTag(ProgressTag).assertDoesNotExist()
    }

    /** Idle is the plain offer: the action, no notice, and nothing reporting progress. */
    @Test
    fun idleIsJustTheOffer() = runComposeUiTest {
        setContent {
            AppTheme {
                AssessmentRetakeAction(
                    state = AssessmentRetakeState.Idle,
                    wording = wording,
                    onRetake = {},
                    actionTestTag = ActionTag,
                    progressTestTag = ProgressTag,
                )
            }
        }

        onNodeWithTag(ActionTag).assertIsEnabled()
        onNodeWithText("Practice again").assertIsDisplayed()
        onNodeWithTag(ProgressTag).assertDoesNotExist()
        onNodeWithText("Could not start. Try again.").assertDoesNotExist()
    }

    private val wording = AssessmentRetakeWording(
        action = "Practice again",
        starting = "Starting practice",
        sourceMissing = "The original attempt is gone.",
        noQuestions = "No questions available.",
        error = "Could not start. Try again.",
    )
}

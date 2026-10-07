package org.artkachenko.kmp_learning_app.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.artkachenko.kmp_learning_app.curriculum.visibility.CurriculumVisibilityStateHolder
import org.artkachenko.kmp_learning_app.ui.LocalAppSnackbarHostState

/**
 * The reset confirmation, end to end through [SettingsDestination] and a real
 * [ProgressResetViewModel], with the reset itself a controllable fake: what the learner is told,
 * that Cancel changes nothing, the busy state while it runs, and both outcomes.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class SettingsProgressResetTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theDialogStatesWhatIsDeletedWhatIsKeptAndThatItIsFinal() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val harness = ResetHarness()
        setContent { harness.Content() }

        onNodeWithTag(SettingsResetProgressTag).performScrollTo().performClick()

        onNodeWithTag(SettingsResetDialogTag).assertIsDisplayed()
        onNodeWithText("Reset progress?").assertIsDisplayed()
        onNodeWithText(
            "This deletes your practice and interview history, including unfinished sessions, " +
                "your mistakes, and the lessons you marked as studied.",
        ).assertIsDisplayed()
        onNodeWithText("Your saved questions and your settings are kept.").assertIsDisplayed()
        onNodeWithText("This cannot be undone.").assertIsDisplayed()
        onNodeWithText("Reset").assertIsDisplayed()
        onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun cancelClosesTheDialogAndResetsNothing() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val harness = ResetHarness()
        setContent { harness.Content() }

        onNodeWithTag(SettingsResetProgressTag).performScrollTo().performClick()
        onNodeWithText("Cancel").performClick()
        waitForIdle()

        onNodeWithTag(SettingsResetDialogTag).assertDoesNotExist()
        assertEquals(0, harness.resetCalls)
        assertEquals(0, harness.navigationResets)
    }

    /**
     * While the reset runs the confirm keeps its place and states its own condition, and neither it
     * nor Cancel can be pressed again. On success the dialog closes, navigation is reset once, and the
     * shell's snackbar confirms it.
     */
    @Test
    fun confirmingShowsTheBusyStateThenClosesResetsNavigationAndConfirms() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val gate = CompletableDeferred<Unit>()
        val harness = ResetHarness(reset = { gate.await() })
        setContent { harness.Content() }

        onNodeWithTag(SettingsResetProgressTag).performScrollTo().performClick()
        onNodeWithTag(SettingsResetConfirmTag).performClick()
        waitForIdle()

        onNodeWithTag(SettingsResetProgressIndicatorTag).assertIsDisplayed()
        onNodeWithText("Resetting").assertIsDisplayed()
        onNodeWithTag(SettingsResetConfirmTag).assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        assertEquals(0, harness.navigationResets)

        gate.complete(Unit)
        waitForIdle()

        onNodeWithTag(SettingsResetDialogTag).assertDoesNotExist()
        assertEquals(1, harness.resetCalls)
        assertEquals(1, harness.navigationResets)
        onNodeWithText("Progress reset").assertIsDisplayed()
    }

    /**
     * A failed reset deleted nothing, so the dialog stays with the reason, navigation is untouched,
     * and Reset is the retry — which here succeeds.
     */
    @Test
    fun aFailedResetKeepsTheDialogWithAnErrorAndCanBeRetried() = runComposeUiTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        var fail = true
        val harness = ResetHarness(reset = { if (fail) error("Database unavailable") })
        setContent { harness.Content() }

        onNodeWithTag(SettingsResetProgressTag).performScrollTo().performClick()
        onNodeWithTag(SettingsResetConfirmTag).performClick()
        waitForIdle()

        onNodeWithTag(SettingsResetDialogTag).assertIsDisplayed()
        onNodeWithTag(SettingsResetFailedTag).assertIsDisplayed()
        onNodeWithText("Your progress could not be reset, and nothing was deleted. Try again.")
            .assertIsDisplayed()
        onNodeWithTag(SettingsResetConfirmTag).assertIsEnabled()
        assertEquals(0, harness.navigationResets)

        fail = false
        onNodeWithTag(SettingsResetConfirmTag).performClick()
        waitForIdle()

        onNodeWithTag(SettingsResetDialogTag).assertDoesNotExist()
        assertEquals(2, harness.resetCalls)
        assertEquals(1, harness.navigationResets)
    }
}

/** [SettingsDestination] over in-memory preferences, a fake reset, and a real snackbar host. */
private class ResetHarness(private val reset: suspend () -> Unit = {}) {
    var resetCalls = 0
        private set
    var navigationResets = 0
        private set

    private val storage = InMemoryAppPreferenceStorage()
    private val viewModel = ProgressResetViewModel(
        resetLearnerProgress = {
            resetCalls += 1
            reset()
        },
    )
    private val snackbarHostState = SnackbarHostState()
    private val appearance = AppearanceStateHolder(ThemePreferenceStore(storage))
    private val visibility = CurriculumVisibilityStateHolder(KmpContentPreferenceStore(storage))

    @Composable
    fun Content() {
        MaterialTheme {
            CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
                SettingsDestination(
                    onBack = {},
                    onProgressReset = { navigationResets += 1 },
                    holder = appearance,
                    visibilityHolder = visibility,
                    progressResetViewModel = viewModel,
                )
                SnackbarHost(snackbarHostState)
            }
        }
    }
}

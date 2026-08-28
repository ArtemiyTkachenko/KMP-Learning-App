package org.artkachenko.kmp_learning_app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.awaitCancellation

/**
 * The Ready branch composes [App], which resolves ViewModels through Koin, so these
 * tests deliberately keep initialization from succeeding.
 */
@OptIn(ExperimentalTestApi::class)
internal class AppRootTest {
    @Test
    fun loadingIsShownWhileInitializationIsInFlight() = runComposeUiTest {
        setContent {
            AppRoot { awaitCancellation() }
        }

        onNodeWithTag(AppStartupLoadingTag).assertIsDisplayed()
    }

    @Test
    fun failedInitializationShowsRetryWhichRunsInitializationAgain() = runComposeUiTest {
        var attempts = 0
        setContent {
            AppRoot {
                attempts += 1
                error("initialization failed")
            }
        }

        waitForIdle()
        onNodeWithTag(AppStartupRetryTag).assertIsDisplayed()
        assertEquals(1, attempts)

        onNodeWithTag(AppStartupRetryTag).performClick()
        waitForIdle()

        assertEquals(2, attempts)
        onNodeWithTag(AppStartupRetryTag).assertIsDisplayed()
    }
}

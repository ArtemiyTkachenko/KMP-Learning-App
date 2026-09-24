package org.artkachenko.kmp_learning_app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest

/**
 * The Ready branch composes [App], which resolves ViewModels through Koin, so these
 * tests deliberately keep initialization from succeeding.
 */
@OptIn(ExperimentalTestApi::class)
internal class AppRootTest {
    @Test
    fun loadingIsShownWhileInitializationIsInFlight() = runComposeUiTest {
        setContent {
            AppRoot(FakeAppStartupInitializer { awaitCancellation() })
        }

        onNodeWithTag(AppStartupLoadingTag).assertIsDisplayed()
    }

    @Test
    fun failedInitializationShowsRetryWhichRunsInitializationAgain() = runComposeUiTest {
        var attempts = 0
        val initializer = FakeAppStartupInitializer {
            attempts += 1
            error("initialization failed")
        }
        setContent {
            AppRoot(initializer)
        }

        waitForIdle()
        onNodeWithText("Retry").assertIsDisplayed()
        assertEquals(1, attempts)

        onNodeWithText("Retry").performClick()
        waitForIdle()

        assertEquals(2, attempts)
        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun reconstructedRootStateUsesCompletionFromTheSameApplicationInitializer() = runTest {
        var attempts = 0
        val initializer = FakeAppStartupInitializer { attempts += 1 }
        val firstRoot = AppStartupStateHolder(initializer)

        firstRoot.initialize()
        val reconstructedRoot = AppStartupStateHolder(initializer)

        assertEquals(AppStartupState.Ready, reconstructedRoot.state)
        reconstructedRoot.initialize()
        assertEquals(1, attempts)
    }

    @Test
    fun aFreshApplicationInitializerStillRequiresInitialization() = runTest {
        var attempts = 0
        val freshInitializer = FakeAppStartupInitializer { attempts += 1 }
        val root = AppStartupStateHolder(freshInitializer)

        assertEquals(AppStartupState.Loading, root.state)
        root.initialize()

        assertEquals(AppStartupState.Ready, root.state)
        assertEquals(1, attempts)
    }

    @Test
    fun retryCanRecoverAfterAnInitializationFailure() = runTest {
        var attempts = 0
        val root = AppStartupStateHolder(
            FakeAppStartupInitializer {
                attempts += 1
                if (attempts == 1) error("first attempt failed")
            },
        )

        root.initialize()
        assertEquals(AppStartupState.Error, root.state)

        root.retry()
        root.initialize()

        assertEquals(AppStartupState.Ready, root.state)
        assertEquals(2, attempts)
    }

    @Test
    fun cancellationPropagatesWithoutBecomingAStartupError() = runTest {
        val root = AppStartupStateHolder(
            FakeAppStartupInitializer { throw CancellationException("cancelled") },
        )

        assertFailsWith<CancellationException> { root.initialize() }

        assertEquals(AppStartupState.Loading, root.state)
    }
}

private class FakeAppStartupInitializer(
    private val initializeBlock: suspend () -> Unit,
) : AppStartupInitializer {
    override var isInitialized: Boolean = false
        private set

    override suspend fun initialize() {
        initializeBlock()
        isInitialized = true
    }
}

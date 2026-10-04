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
import kotlin.test.assertSame
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.artkachenko.kmp_learning_app.diagnostics.AppDiagnostics

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
        val diagnostics = RecordingDiagnostics()
        val root = AppStartupStateHolder(freshInitializer, diagnostics)

        assertEquals(AppStartupState.Loading, root.state)
        root.initialize()

        assertEquals(AppStartupState.Ready, root.state)
        assertEquals(1, attempts)
        assertEquals(emptyList(), diagnostics.reports)
    }

    @Test
    fun failedInitializationReportsTheOriginalExceptionOnceBeforeShowingError() = runTest {
        val failure = IllegalStateException("database failed", RuntimeException("disk I/O"))
        val diagnostics = RecordingDiagnostics()
        val root = AppStartupStateHolder(FakeAppStartupInitializer { throw failure }, diagnostics)

        root.initialize()

        assertEquals(AppStartupState.Error, root.state)
        val report = diagnostics.reports.single()
        assertSame(failure, report.throwable)
        assertEquals("Application startup initialization failed.", report.context)
    }

    @Test
    fun eachFailedAttemptIsReportedIndependently() = runTest {
        val failures = listOf(IllegalStateException("first"), IllegalArgumentException("second"))
        var attempts = 0
        val diagnostics = RecordingDiagnostics()
        val root = AppStartupStateHolder(
            FakeAppStartupInitializer { throw failures[attempts++] },
            diagnostics,
        )

        root.initialize()
        root.retry()
        root.initialize()

        assertEquals(AppStartupState.Error, root.state)
        assertEquals(2, diagnostics.reports.size)
        assertSame(failures[0], diagnostics.reports[0].throwable)
        assertSame(failures[1], diagnostics.reports[1].throwable)
    }

    @Test
    fun aThrowingDiagnosticsSinkStillLeavesStartupInTheRecoverableErrorState() = runTest {
        val root = AppStartupStateHolder(
            FakeAppStartupInitializer { error("initialization failed") },
            AppDiagnostics { _, _ -> error("diagnostics failed") },
        )

        root.initialize()

        assertEquals(AppStartupState.Error, root.state)
    }

    @Test
    fun retryCanRecoverAfterAnInitializationFailure() = runTest {
        var attempts = 0
        val diagnostics = RecordingDiagnostics()
        val root = AppStartupStateHolder(
            FakeAppStartupInitializer {
                attempts += 1
                if (attempts == 1) error("first attempt failed")
            },
            diagnostics,
        )

        root.initialize()
        assertEquals(AppStartupState.Error, root.state)

        root.retry()
        root.initialize()

        assertEquals(AppStartupState.Ready, root.state)
        assertEquals(2, attempts)
        // Only the failed attempt is reported; the successful retry adds nothing.
        assertEquals("first attempt failed", diagnostics.reports.single().throwable.message)
    }

    @Test
    fun cancellationPropagatesWithoutBecomingAStartupError() = runTest {
        val diagnostics = RecordingDiagnostics()
        val root = AppStartupStateHolder(
            FakeAppStartupInitializer { throw CancellationException("cancelled") },
            diagnostics,
        )

        assertFailsWith<CancellationException> { root.initialize() }

        assertEquals(AppStartupState.Loading, root.state)
        assertEquals(emptyList(), diagnostics.reports)
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

private class RecordingDiagnostics : AppDiagnostics {
    val reports = mutableListOf<ReportedError>()

    override fun reportError(context: String, throwable: Throwable) {
        reports += ReportedError(context, throwable)
    }
}

private data class ReportedError(val context: String, val throwable: Throwable)

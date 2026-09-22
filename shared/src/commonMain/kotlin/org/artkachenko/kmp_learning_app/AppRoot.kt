package org.artkachenko.kmp_learning_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.coroutines.cancellation.CancellationException
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_startup_error
import kmp_learning_app.shared.generated.resources.app_startup_loading
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.theme.AppearanceTheme
import org.jetbrains.compose.resources.stringResource

internal const val AppStartupLoadingTag = "app_startup_loading"

/**
 * Shared application root: initializes platform-local data, then enters [App].
 *
 * Runtime hosts need the same loading, failure, and retry UI around initialization,
 * so the state machine lives here rather than being duplicated per platform.
 * [initializer] is the host's application-scoped local-data initializer. Its in-process completion
 * state survives host reconstruction, while a fresh process supplies a fresh initializer.
 */
@Composable
public fun AppRoot(initializer: AppStartupInitializer) {
    val startup = remember(initializer) { AppStartupStateHolder(initializer) }

    LaunchedEffect(startup.state) {
        if (startup.state == AppStartupState.Loading) {
            startup.initialize()
        }
    }

    // AppRoot themes its startup UI, and App() retains its theme so direct test and preview
    // composition keeps the same presentation defaults. Both go through AppearanceTheme, which is
    // the application's one theme decision: the loading, error and ready screens cannot disagree
    // about light or dark, and an explicit choice reaches all of them at once. The preference is
    // already in memory before this composes — see AppearanceStateHolder — so there is no
    // light-to-dark flash at startup and nothing here waits on storage.
    AppearanceTheme {
        when (startup.state) {
            AppStartupState.Loading -> ScreenLoading(
                message = stringResource(Res.string.app_startup_loading),
                testTag = AppStartupLoadingTag,
            )
            AppStartupState.Ready -> App()
            AppStartupState.Error -> ScreenError(
                message = stringResource(Res.string.app_startup_error),
                onRetry = startup::retry,
            )
        }
    }
}

/** Application-lifetime initialization supplied by each runtime host. */
public interface AppStartupInitializer {
    /** True only after this process has completed initialization successfully. */
    public val isInitialized: Boolean

    /** Initializes local application data, or throws so the root can offer Retry. */
    public suspend fun initialize()
}

internal class AppStartupStateHolder(
    private val initializer: AppStartupInitializer,
) {
    var state by mutableStateOf(
        if (initializer.isInitialized) AppStartupState.Ready else AppStartupState.Loading,
    )
        private set

    suspend fun initialize() {
        if (state != AppStartupState.Loading) return

        try {
            initializer.initialize()
            state = AppStartupState.Ready
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            state = AppStartupState.Error
        }
    }

    fun retry() {
        if (state == AppStartupState.Error) state = AppStartupState.Loading
    }
}

internal enum class AppStartupState {
    Loading,
    Ready,
    Error,
}

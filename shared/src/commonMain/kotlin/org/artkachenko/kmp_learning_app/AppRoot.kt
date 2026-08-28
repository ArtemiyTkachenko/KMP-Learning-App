package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.app_startup_error
import kmp_learning_app.shared.generated.resources.app_startup_loading
import kmp_learning_app.shared.generated.resources.topic_browser_retry
import org.jetbrains.compose.resources.stringResource

internal const val AppStartupLoadingTag = "app_startup_loading"
internal const val AppStartupRetryTag = "app_startup_retry"

/**
 * Shared application root: initializes platform-local data, then enters [App].
 *
 * Android and Desktop both need loading, failure, and retry UI around
 * initialization, so the state machine lives here rather than being duplicated in
 * each host. [initialize] is the host's suspending initializer, for example
 * `initializeAndroidLocalData()` or `initializeDesktopLocalData()`.
 */
@Composable
public fun AppRoot(initialize: suspend () -> Unit) {
    var state by remember { mutableStateOf(AppStartupState.Loading) }

    LaunchedEffect(state) {
        if (state == AppStartupState.Loading) {
            state = runCatching {
                initialize()
                AppStartupState.Ready
            }.getOrElse {
                AppStartupState.Error
            }
        }
    }

    // AppRoot themes its own startup UI. App() keeps its own MaterialTheme because it
    // is still a standalone entry point for hosts that do not route through AppRoot;
    // the nested theme re-provides identical defaults, so rendering is unchanged.
    MaterialTheme {
        when (state) {
            AppStartupState.Loading -> AppStartupLoading()
            AppStartupState.Ready -> App()
            AppStartupState.Error -> AppStartupError(
                onRetry = {
                    state = AppStartupState.Loading
                },
            )
        }
    }
}

@Composable
private fun AppStartupLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(Modifier.testTag(AppStartupLoadingTag))
            Text(text = stringResource(Res.string.app_startup_loading))
        }
    }
}

@Composable
private fun AppStartupError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.app_startup_error))
            Button(onClick = onRetry, modifier = Modifier.testTag(AppStartupRetryTag)) {
                Text(text = stringResource(Res.string.topic_browser_retry))
            }
        }
    }
}

private enum class AppStartupState {
    Loading,
    Ready,
    Error,
}

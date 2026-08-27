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
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.desktop_startup_error
import kmp_learning_app.shared.generated.resources.desktop_startup_loading
import kmp_learning_app.shared.generated.resources.topic_browser_retry
import org.jetbrains.compose.resources.stringResource

@Composable
public fun DesktopAppRoot() {
    var state by remember { mutableStateOf(DesktopStartupState.Loading) }

    LaunchedEffect(state) {
        if (state == DesktopStartupState.Loading) {
            state = runCatching {
                initializeDesktopLocalData()
                DesktopStartupState.Ready
            }.getOrElse {
                DesktopStartupState.Error
            }
        }
    }

    MaterialTheme {
        when (state) {
            DesktopStartupState.Loading -> DesktopStartupLoading()
            DesktopStartupState.Ready -> App()
            DesktopStartupState.Error -> DesktopStartupError(
                onRetry = {
                    state = DesktopStartupState.Loading
                },
            )
        }
    }
}

@Composable
private fun DesktopStartupLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(Res.string.desktop_startup_loading))
        }
    }
}

@Composable
private fun DesktopStartupError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.desktop_startup_error))
            Button(onClick = onRetry) {
                Text(text = stringResource(Res.string.topic_browser_retry))
            }
        }
    }
}

private enum class DesktopStartupState {
    Loading,
    Ready,
    Error,
}

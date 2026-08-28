package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun FocusedResultDestination(
    attemptId: String,
    onBack: () -> Unit,
    onRetakeCreated: (String) -> Unit,
    viewModel: FocusedResultViewModel = koinViewModel { parametersOf(attemptId) },
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val uriHandler = LocalUriHandler.current
    var failedSourceUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is FocusedResultEvent.RetakeCreated -> onRetakeCreated(event.attemptId)
            }
        }
    }
    FocusedResultScreen(
        state = state,
        onRetry = viewModel::retry,
        onBack = onBack,
        onSourceClick = { url ->
            // openUri throws when no host handler can open the URI. The failure used to be
            // swallowed here, so a tap on a source looked like a no-op.
            failedSourceUrl = url.takeIf { runCatching { uriHandler.openUri(it) }.isFailure }
        },
        onRepeatPractice = viewModel::repeatPractice,
        failedSourceUrl = failedSourceUrl,
    )
}

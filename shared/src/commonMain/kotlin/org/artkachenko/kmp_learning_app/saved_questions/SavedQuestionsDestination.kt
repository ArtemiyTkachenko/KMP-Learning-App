package org.artkachenko.kmp_learning_app.saved_questions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SavedQuestionsDestination(
    onBack: () -> Unit,
    onBrowseTopics: () -> Unit,
    viewModel: SavedQuestionsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var failedSourceUrl by remember { mutableStateOf<String?>(null) }

    SavedQuestionsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onBrowseTopics = onBrowseTopics,
        onRemoveSaved = viewModel::removeSaved,
        onSourceClick = { url ->
            // The same handling as the result and mistake-review destinations: openUri throws when
            // no host handler can open the URI, and the failure must stay visible rather than
            // looking like a no-op.
            failedSourceUrl = url.takeIf { runCatching { uriHandler.openUri(it) }.isFailure }
        },
        failedSourceUrl = failedSourceUrl,
    )
}

package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MixedInterviewResultDestination(
    attemptId: String,
    onBack: () -> Unit,
    viewModel: MixedInterviewResultViewModel = koinViewModel { parametersOf(attemptId) },
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val uriHandler = LocalUriHandler.current
    MixedInterviewResultScreen(
        state = state,
        onRetry = viewModel::retry,
        onBack = onBack,
        onSourceClick = { url -> runCatching { uriHandler.openUri(url) } },
    )
}

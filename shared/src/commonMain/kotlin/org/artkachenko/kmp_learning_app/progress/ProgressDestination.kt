package org.artkachenko.kmp_learning_app.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProgressDestination(
    onBack: () -> Unit,
    onOpenFocusedResult: (String) -> Unit,
    onOpenMixedResult: (String) -> Unit,
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    ProgressScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onHistoryClick = { assessmentType, attemptId ->
            when (assessmentType) {
                CompletedAssessmentType.FOCUSED -> onOpenFocusedResult(attemptId)
                CompletedAssessmentType.MIXED -> onOpenMixedResult(attemptId)
            }
        },
    )
}

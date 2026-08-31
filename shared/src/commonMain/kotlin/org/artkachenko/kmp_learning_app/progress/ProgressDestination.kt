package org.artkachenko.kmp_learning_app.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProgressDestination(
    onBack: (() -> Unit)? = null,
    onBrowseTopics: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenFocusedResult: (String) -> Unit,
    onOpenMixedResult: (String) -> Unit,
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProgressScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onBrowseTopics = onBrowseTopics,
        onTopicClick = onOpenTopic,
        onHistoryClick = { assessmentType, attemptId ->
            when (assessmentType) {
                CompletedAssessmentType.FOCUSED -> onOpenFocusedResult(attemptId)
                CompletedAssessmentType.MIXED -> onOpenMixedResult(attemptId)
            }
        },
    )
}

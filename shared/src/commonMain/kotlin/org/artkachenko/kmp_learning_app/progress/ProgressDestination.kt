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
    onOpenTopic: (String) -> Unit,
    onReviewMistakes: () -> Unit,
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
        // Progress is always pushed from the topic browser, so leaving is how a learner gets
        // there. Naming it separately keeps the button's label honest if that ever changes.
        onBrowseTopics = onBack,
        onReviewMistakes = onReviewMistakes,
        onTopicClick = onOpenTopic,
        onHistoryClick = { assessmentType, attemptId ->
            when (assessmentType) {
                CompletedAssessmentType.FOCUSED -> onOpenFocusedResult(attemptId)
                CompletedAssessmentType.MIXED -> onOpenMixedResult(attemptId)
            }
        },
    )
}

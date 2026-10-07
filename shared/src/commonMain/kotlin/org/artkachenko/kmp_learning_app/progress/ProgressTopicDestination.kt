package org.artkachenko.kmp_learning_app.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ProgressTopicDestination(
    topicId: String,
    onBack: () -> Unit,
    onConfigurePractice: (PracticePreset) -> Unit,
    onReviewMistakes: () -> Unit,
    viewModel: ProgressTopicViewModel = koinViewModel { parametersOf(topicId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val readiness by viewModel.readiness.collectAsStateWithLifecycle()

    ProgressTopicScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        readiness = readiness,
        onReviewMistakes = onReviewMistakes,
        onPracticePreset = onConfigurePractice,
    )
}

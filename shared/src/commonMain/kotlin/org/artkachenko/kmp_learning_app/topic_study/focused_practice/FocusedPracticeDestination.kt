package org.artkachenko.kmp_learning_app.topic_study.focused_practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun FocusedPracticeDestination(
    config: AssessmentConfig.Focused,
    onBack: () -> Unit,
    viewModel: FocusedPracticeViewModel = koinViewModel { parametersOf(config) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FocusedPracticeScreen(
        state = state,
        onAnswerClick = viewModel::selectAnswer,
        onSubmit = viewModel::submitAnswer,
        onRetry = viewModel::retry,
        onBack = onBack,
    )
}

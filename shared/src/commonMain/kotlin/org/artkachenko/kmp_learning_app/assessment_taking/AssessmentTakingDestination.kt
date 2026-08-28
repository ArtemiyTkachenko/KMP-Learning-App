package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun AssessmentTakingDestination(
    title: String,
    launch: AssessmentTakingLaunch,
    onBack: () -> Unit,
    onCompleted: (String) -> Unit,
    viewModel: AssessmentTakingViewModel = koinViewModel { parametersOf(launch) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val completion = state as? AssessmentTakingUiState.CompletionSucceeded
            ?: return@LaunchedEffect
        onCompleted(completion.attemptId)
    }
    AssessmentTakingScreen(
        title = title,
        state = state,
        onAnswerClick = viewModel::selectAnswer,
        onSubmit = viewModel::submitAnswer,
        onRetry = viewModel::retry,
        onBack = onBack,
        onComplete = viewModel::completeAssessment,
    )
}

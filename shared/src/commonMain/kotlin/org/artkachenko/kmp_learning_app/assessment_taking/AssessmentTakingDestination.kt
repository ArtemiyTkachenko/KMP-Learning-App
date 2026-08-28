package org.artkachenko.kmp_learning_app.assessment_taking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun AssessmentTakingDestination(
    title: String,
    launch: AssessmentTakingLaunch,
    onBack: () -> Unit,
    onAttemptPersisted: (String) -> Unit,
    onCompleted: (String) -> Unit,
    viewModel: AssessmentTakingViewModel = koinViewModel { parametersOf(launch) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnAttemptPersisted by rememberUpdatedState(onAttemptPersisted)
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    val persistedAttemptId = if (launch is AssessmentTakingLaunch.New) {
        (state as? AssessmentTakingUiState.Content)?.attemptId
    } else {
        null
    }
    // Keyed on the attempt IDs rather than the whole UI state: Content changes on
    // every answer selection, which restarted both effects on each recomposition.
    val completedAttemptId = (state as? AssessmentTakingUiState.CompletionSucceeded)?.attemptId

    LaunchedEffect(persistedAttemptId) {
        persistedAttemptId?.let(currentOnAttemptPersisted)
    }
    LaunchedEffect(completedAttemptId) {
        completedAttemptId?.let(currentOnCompleted)
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

package org.artkachenko.kmp_learning_app.assessment.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.koin.compose.viewmodel.koinViewModel

/** Owns assessment creation for the destination that offers the launch action. */
@Composable
internal fun AssessmentLaunchCoordinator(
    onAttemptCreated: (String) -> Unit,
    viewModel: AssessmentLaunchViewModel = koinViewModel(),
    content: @Composable (startAssessment: (AssessmentConfig) -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnAttemptCreated by rememberUpdatedState(onAttemptCreated)
    val startAssessment: (AssessmentConfig) -> Unit = remember(viewModel) { viewModel::start }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AssessmentLaunchEvent.Created ->
                    currentOnAttemptCreated(event.attemptId)
            }
        }
    }

    content(startAssessment)
    AssessmentLaunchDialog(
        state = state,
        onRetry = viewModel::retry,
        onDismissFailure = viewModel::dismissFailure,
    )
}

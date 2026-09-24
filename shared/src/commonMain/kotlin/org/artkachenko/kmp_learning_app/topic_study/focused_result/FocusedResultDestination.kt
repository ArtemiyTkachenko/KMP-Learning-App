package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.artkachenko.kmp_learning_app.assessment.start.AssessmentLaunchCoordinator
import org.artkachenko.kmp_learning_app.assessment.start.AssessmentLaunchViewModel

@Composable
internal fun FocusedResultDestination(
    attemptId: String,
    onBack: () -> Unit,
    onRetakeCreated: (String) -> Unit,
    onPracticeStarted: (String) -> Unit,
    viewModel: FocusedResultViewModel = koinViewModel { parametersOf(attemptId) },
    launchViewModel: AssessmentLaunchViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val retakeState = viewModel.retakeState.collectAsStateWithLifecycle().value
    val savedQuestions = viewModel.savedQuestions.collectAsStateWithLifecycle().value
    val uriHandler = LocalUriHandler.current
    val currentOnRetakeCreated by rememberUpdatedState(onRetakeCreated)
    var failedSourceUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.retakeEvents.collect { created ->
            currentOnRetakeCreated(created.attemptId)
            viewModel.onRetakeEventHandled(created.attemptId)
        }
    }
    AssessmentLaunchCoordinator(
        onAttemptCreated = onPracticeStarted,
        viewModel = launchViewModel,
    ) { startAssessment ->
        FocusedResultScreen(
            state = state,
            onRetry = viewModel::retry,
            onBack = onBack,
            onSourceClick = { url ->
                // openUri throws when no host handler can open the URI. The failure used to be
                // swallowed here, so a tap on a source looked like a no-op.
                failedSourceUrl = url.takeIf { runCatching { uriHandler.openUri(it) }.isFailure }
            },
            onRepeatPractice = viewModel::repeatPractice,
            retakeState = retakeState,
            onPracticeMistakes = startAssessment,
            savedQuestions = savedQuestions,
            // The semantic action, not the repository: persistence stays behind the ViewModel.
            onToggleSaved = viewModel::toggleSaved,
            failedSourceUrl = failedSourceUrl,
        )
    }
}

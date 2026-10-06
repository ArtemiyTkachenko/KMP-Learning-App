package org.artkachenko.kmp_learning_app.assessment_taking

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

@Composable
internal fun AssessmentTakingDestination(
    title: String,
    attemptId: String,
    onBack: () -> Unit,
    onCompleted: (String) -> Unit,
    viewModel: AssessmentTakingViewModel = koinViewModel { parametersOf(attemptId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val uriHandler = LocalUriHandler.current
    var failedReportUrl by remember { mutableStateOf<String?>(null) }

    val completedAttemptId = (state as? AssessmentTakingUiState.CompletionSucceeded)?.attemptId

    LaunchedEffect(completedAttemptId) {
        completedAttemptId?.let(currentOnCompleted)
    }
    AssessmentTakingScreen(
        title = title,
        state = state,
        onAnswerClick = viewModel::selectAnswer,
        onSubmit = viewModel::submitAnswer,
        onNext = viewModel::nextQuestion,
        onRetry = viewModel::retry,
        onBack = onBack,
        onComplete = viewModel::completeAssessment,
        onReportProblem = { url ->
            // The same handling as the result destinations' source links: openUri throws when no
            // host handler can open the URI, and the failure is shown beside the link.
            failedReportUrl = url.takeIf { runCatching { uriHandler.openUri(it) }.isFailure }
        },
        failedReportUrl = failedReportUrl,
    )
}

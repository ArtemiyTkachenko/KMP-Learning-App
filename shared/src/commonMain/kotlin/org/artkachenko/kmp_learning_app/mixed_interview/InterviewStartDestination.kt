package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun InterviewStartDestination(
    onStartMixedInterview: () -> Unit,
    onOpenResult: (String) -> Unit,
    viewModel: InterviewStartViewModel = koinViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    InterviewStartScreen(
        onStartMixedInterview = onStartMixedInterview,
        history = history,
        onOpenResult = onOpenResult,
    )
}

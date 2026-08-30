package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun InterviewStartDestination(
    onStartMixedInterview: () -> Unit,
    onOpenResult: (String) -> Unit,
    viewModel: InterviewStartViewModel = koinViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    InterviewStartScreen(
        onStartMixedInterview = onStartMixedInterview,
        history = history,
        onOpenResult = onOpenResult,
    )
}

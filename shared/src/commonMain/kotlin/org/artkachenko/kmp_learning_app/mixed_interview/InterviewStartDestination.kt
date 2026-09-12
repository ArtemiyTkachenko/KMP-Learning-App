package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.start.AssessmentLaunchCoordinator
import org.artkachenko.kmp_learning_app.assessment.start.AssessmentLaunchViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun InterviewStartDestination(
    onInterviewStarted: (String) -> Unit,
    onOpenResult: (String) -> Unit,
    viewModel: InterviewStartViewModel = koinViewModel(),
    launchViewModel: AssessmentLaunchViewModel = koinViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    AssessmentLaunchCoordinator(
        onAttemptCreated = onInterviewStarted,
        viewModel = launchViewModel,
    ) { startAssessment ->
        InterviewStartScreen(
            onStartMixedInterview = {
                startAssessment(
                    AssessmentConfig.Mixed(MixedInterviewDefaults.QuestionCount),
                )
            },
            history = history,
            onOpenResult = onOpenResult,
        )
    }
}

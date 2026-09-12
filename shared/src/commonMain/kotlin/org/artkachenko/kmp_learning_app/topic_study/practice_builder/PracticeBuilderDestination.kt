package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment.start.AssessmentLaunchCoordinator
import org.artkachenko.kmp_learning_app.assessment.start.AssessmentLaunchViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Practice Builder destination.
 *
 * [target] is what the learner chose, addressed by stable ID. Turning it into an assessment scope —
 * which for a Learning Unit means reading its current Lessons — happens inside the ViewModel, so no
 * derived scope travels through navigation and no screen resolves content on the builder's behalf.
 */
@Composable
internal fun PracticeBuilderDestination(
    target: PracticeBuilderTarget,
    onBack: () -> Unit,
    onPracticeStarted: (String) -> Unit,
    initialSource: PracticeQuestionSource = PracticeQuestionSource.ALL,
    viewModel: PracticeBuilderViewModel = koinViewModel {
        parametersOf(target, initialSource)
    },
    launchViewModel: AssessmentLaunchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AssessmentLaunchCoordinator(
        onAttemptCreated = onPracticeStarted,
        viewModel = launchViewModel,
    ) { startAssessment ->
        val currentStartAssessment by rememberUpdatedState(startAssessment)
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is PracticeBuilderEvent.StartPractice ->
                        currentStartAssessment(event.config)
                }
            }
        }
        PracticeBuilderScreen(
            state = state,
            onBack = onBack,
            onQuestionCountClick = viewModel::selectQuestionCount,
            onLevelClick = viewModel::toggleLevel,
            onSourceClick = viewModel::selectSource,
            onStartClick = viewModel::startPractice,
            onRetryAvailability = viewModel::retryAvailability,
        )
    }
}

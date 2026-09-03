package org.artkachenko.kmp_learning_app.topic_study.practice_builder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun PracticeBuilderDestination(
    scope: AssessmentScope,
    onBack: () -> Unit,
    onStartPractice: (AssessmentConfig.Focused) -> Unit,
    initialSource: PracticeQuestionSource = PracticeQuestionSource.ALL,
    viewModel: PracticeBuilderViewModel = koinViewModel {
        parametersOf(scope, initialSource)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnStartPractice by rememberUpdatedState(onStartPractice)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PracticeBuilderEvent.StartPractice -> currentOnStartPractice(event.config)
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

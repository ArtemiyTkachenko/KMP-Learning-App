package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Learning Unit overview destination.
 *
 * Takes the stable [unitId] and nothing else: the authored Unit is resolved through
 * `LearningContentRepository` by the ViewModel, never handed in by the caller. [onLessonClick]
 * emits a stable Lesson ID, and the shell pairs it with the Unit ID it already holds in the route
 * — this screen never decides what a route looks like.
 */
@Composable
internal fun LearningUnitDestination(
    unitId: String,
    onBack: () -> Unit,
    onLessonClick: (String) -> Unit,
    viewModel: LearningUnitViewModel = koinViewModel { parametersOf(unitId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LearningUnitScreen(
        state = state,
        onBack = onBack,
        onLessonClick = onLessonClick,
        onRetry = viewModel::retry,
    )
}

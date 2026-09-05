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
 *
 * [onPracticeUnit] carries nothing at all: the shell already holds the Unit ID in the route it is
 * rendering, so the Practice Builder is opened on the Unit the learner navigated through rather
 * than on whatever this screen happens to have loaded. Nothing derived from the Unit — its
 * concepts, its title — leaves here, because the builder resolves the Unit again on arrival.
 */
@Composable
internal fun LearningUnitDestination(
    unitId: String,
    onBack: () -> Unit,
    onLessonClick: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    viewModel: LearningUnitViewModel = koinViewModel { parametersOf(unitId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LearningUnitScreen(
        state = state,
        onBack = onBack,
        onLessonClick = onLessonClick,
        onPracticeUnit = onPracticeUnit,
        onRetry = viewModel::retry,
    )
}

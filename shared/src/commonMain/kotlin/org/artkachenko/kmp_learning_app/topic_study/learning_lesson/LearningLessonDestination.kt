package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Lesson destination.
 *
 * Both stable identities arrive from the route and neither is derived from screen state: [unitId]
 * is the Unit the learner is reading this Lesson in, and the ViewModel resolves the Lesson through
 * it rather than beside it.
 */
@Composable
internal fun LearningLessonDestination(
    unitId: String,
    lessonId: String,
    onBack: () -> Unit,
    viewModel: LearningLessonViewModel = koinViewModel { parametersOf(unitId, lessonId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LearningLessonScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
    )
}

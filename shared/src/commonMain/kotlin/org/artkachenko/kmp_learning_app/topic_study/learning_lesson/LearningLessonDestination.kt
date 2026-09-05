package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Lesson destination.
 *
 * Both stable identities arrive from the route and neither is derived from screen state: [unitId]
 * is the Unit the learner is reading this Lesson in, and the ViewModel resolves the Lesson through
 * it rather than beside it.
 *
 * [onNavigateLesson] carries a sibling Lesson's stable ID out to the shell, which owns what that
 * does to the back stack. A Source, by contrast, never becomes navigation at all: it is an external
 * URI opened through Compose's own `LocalUriHandler`, so no Source URL is serialized into a route
 * and no platform browser API reaches this shared code.
 */
@Composable
internal fun LearningLessonDestination(
    unitId: String,
    lessonId: String,
    onBack: () -> Unit,
    onNavigateLesson: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    viewModel: LearningLessonViewModel = koinViewModel { parametersOf(unitId, lessonId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var failedSourceUrl by remember { mutableStateOf<String?>(null) }

    LearningLessonScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onNavigateLesson = onNavigateLesson,
        // The owning Unit is [unitId] from the route, which the shell holds — this destination
        // never derives a practice target from the Lesson it is showing.
        onPracticeUnit = onPracticeUnit,
        onOpenSource = { url ->
            // openUri throws when the host has no handler for the URI. Caught here and reported
            // beside the link, so a tap that cannot succeed does not look like a tap that did
            // nothing — and so a hostile or unavailable host cannot take the reader down with it.
            failedSourceUrl = url.takeIf { runCatching { uriHandler.openUri(it) }.isFailure }
        },
        failedSourceUrl = failedSourceUrl,
    )
}

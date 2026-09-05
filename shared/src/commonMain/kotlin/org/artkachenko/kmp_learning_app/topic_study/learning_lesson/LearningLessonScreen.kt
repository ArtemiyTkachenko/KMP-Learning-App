package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_lesson_error
import kmp_learning_app.shared.generated.resources.learning_lesson_loading
import kmp_learning_app.shared.generated.resources.learning_lesson_not_found
import kmp_learning_app.shared.generated.resources.learning_lesson_title
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.stringResource

internal const val LearningLessonLoadingTag = "learning_lesson_loading"

/**
 * The Lesson reading surface, as far as E21-03 takes it.
 *
 * It presents what the Lesson is — its title and summary — and proves the route resolves to real
 * current content inside its Unit. The authored section body, its blocks, and its Sources are
 * E21-04's, and this screen is where that renderer goes. It says nothing about work in progress:
 * a page announcing its own incompleteness is developer copy, not something a learner should read.
 */
@Composable
internal fun LearningLessonScreen(
    state: LearningLessonUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        // A stable label, for the same reason as the Unit overview: the Lesson title leads the
        // content, and the bar has to read sensibly before the Lesson has resolved.
        AppTopBar(stringResource(Res.string.learning_lesson_title), onBack, scrollBehavior)
        when (state) {
            LearningLessonUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.learning_lesson_loading),
                testTag = LearningLessonLoadingTag,
                modifier = Modifier.weight(1f),
            )
            LearningLessonUiState.NotFound -> ScreenMessage(
                message = stringResource(Res.string.learning_lesson_not_found),
                modifier = Modifier.weight(1f),
            )
            LearningLessonUiState.Error -> ScreenError(
                message = stringResource(Res.string.learning_lesson_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is LearningLessonUiState.Content -> LearningLessonContent(
                state = state,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LearningLessonContent(
    state: LearningLessonUiState.Content,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(appScreenContentPadding()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = state.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

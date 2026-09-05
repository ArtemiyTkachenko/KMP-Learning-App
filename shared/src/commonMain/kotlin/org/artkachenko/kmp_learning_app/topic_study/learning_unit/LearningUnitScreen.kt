package org.artkachenko.kmp_learning_app.topic_study.learning_unit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_practice_unit
import kmp_learning_app.shared.generated.resources.learning_unit_error
import kmp_learning_app.shared.generated.resources.learning_unit_lessons
import kmp_learning_app.shared.generated.resources.learning_unit_loading
import kmp_learning_app.shared.generated.resources.learning_unit_no_lessons
import kmp_learning_app.shared.generated.resources.learning_unit_not_found
import kmp_learning_app.shared.generated.resources.learning_unit_title
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.stringResource

internal const val LearningUnitLoadingTag = "learning_unit_loading"
internal const val LearningUnitPracticeButtonTag = "learning_unit_practice_button"

internal fun learningLessonRowTag(lessonId: String): String = "learning_lesson_$lessonId"

/**
 * The Unit overview: what this Unit is about, and what there is to read in it.
 *
 * A reading and discovery surface, so it stays deliberately quiet — no scores, no badges, no
 * progress. None of those exist for learning content, and borrowing the assessment screens' visual
 * weight would imply they do.
 */
@Composable
internal fun LearningUnitScreen(
    state: LearningUnitUiState,
    onBack: () -> Unit,
    onLessonClick: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        // A stable label rather than the Unit title: the title is the first thing the content
        // says, so repeating it in the bar would say it twice — and the bar would otherwise have
        // nothing to show while the Unit is still resolving or turns out not to be current.
        AppTopBar(stringResource(Res.string.learning_unit_title), onBack, scrollBehavior)
        when (state) {
            LearningUnitUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.learning_unit_loading),
                testTag = LearningUnitLoadingTag,
                modifier = Modifier.weight(1f),
            )
            LearningUnitUiState.NotFound -> ScreenMessage(
                message = stringResource(Res.string.learning_unit_not_found),
                modifier = Modifier.weight(1f),
            )
            LearningUnitUiState.Error -> ScreenError(
                message = stringResource(Res.string.learning_unit_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is LearningUnitUiState.Content -> LearningUnitContent(
                state = state,
                onLessonClick = onLessonClick,
                onPracticeUnit = onPracticeUnit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LearningUnitContent(
    state: LearningUnitUiState.Content,
    onLessonClick: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
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
        item {
            // Above the Lessons rather than after them: practising the Unit is the second thing
            // this screen offers, and burying it under a list the learner may not scroll would
            // make studying and quizzing look like sequential steps rather than two ways in.
            //
            // A labelled Button, not an icon: the action names what it practises, which is what a
            // screen reader announces and what makes it distinguishable from the Lesson cards.
            // It opens the builder — nothing is started here, so the learner still chooses the
            // length, levels, and source.
            Button(
                onClick = onPracticeUnit,
                modifier = Modifier.fillMaxWidth().testTag(LearningUnitPracticeButtonTag),
            ) {
                Text(text = stringResource(Res.string.learning_practice_unit))
            }
        }
        if (state.lessons.isEmpty()) {
            item {
                // Controlled rather than fatal: an ACTIVE Unit whose Lessons have all been retired
                // is a coherent document, and nothing is invented to fill the list.
                Text(
                    text = stringResource(Res.string.learning_unit_no_lessons),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            item {
                SectionHeading(text = stringResource(Res.string.learning_unit_lessons))
            }
            // Authored order, exactly as the repository returned it: that sequence is how the Unit
            // is meant to be read, so nothing here sorts by title or length.
            items(state.lessons, key = LearningLessonItemUiModel::lessonId) { lesson ->
                LearningLessonRow(lesson = lesson, onClick = { onLessonClick(lesson.lessonId) })
            }
        }
    }
}

/**
 * One Lesson as an ordinary clickable card, which carries its own click semantics — the title and
 * summary are already read out, so no content description repeats them.
 */
@Composable
private fun LearningLessonRow(
    lesson: LearningLessonItemUiModel,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(learningLessonRowTag(lesson.lessonId)),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Comfortable),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        ) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = lesson.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

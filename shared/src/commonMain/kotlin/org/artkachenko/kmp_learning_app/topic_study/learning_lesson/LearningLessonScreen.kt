package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_lesson_error
import kmp_learning_app.shared.generated.resources.learning_lesson_loading
import kmp_learning_app.shared.generated.resources.learning_lesson_next
import kmp_learning_app.shared.generated.resources.learning_lesson_not_found
import kmp_learning_app.shared.generated.resources.learning_lesson_previous
import kmp_learning_app.shared.generated.resources.learning_lesson_source_open_failed
import kmp_learning_app.shared.generated.resources.learning_lesson_sources
import kmp_learning_app.shared.generated.resources.learning_lesson_title
import kmp_learning_app.shared.generated.resources.learning_practice_unit
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.stringResource

internal const val LearningLessonLoadingTag = "learning_lesson_loading"
internal const val LearningLessonReadingColumnTag = "learning_lesson_reading_column"
internal const val LearningLessonPreviousTag = "learning_lesson_previous"
internal const val LearningLessonNextTag = "learning_lesson_next"
internal const val LearningLessonPracticeButtonTag = "learning_lesson_practice_button"

/**
 * The Lesson reading surface.
 *
 * A reading page, not an assessment one: no score, no question count, no progress figure, and no
 * answer controls, because none of those exist for learning content and borrowing their visual
 * weight would imply they do. The page reads top to bottom in authored order — title, summary, the
 * Sections, the Sources, and only then the way to the next Lesson — so nothing offers to move on
 * before the material has been shown.
 *
 * [onNavigateLesson] emits a stable Lesson ID and nothing else. What that means for the back stack
 * is the shell's decision, so no route, index, or `LearningLesson` leaves this screen.
 *
 * [onPracticeUnit] carries nothing: practising is a Unit-level action, and the Unit it belongs to
 * is the one the shell is already rendering this Lesson inside. Emitting a Lesson identity here
 * would invite a Lesson-sized quiz, which is not what this action means.
 */
@Composable
internal fun LearningLessonScreen(
    state: LearningLessonUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNavigateLesson: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    failedSourceUrl: String? = null,
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
                onNavigateLesson = onNavigateLesson,
                onPracticeUnit = onPracticeUnit,
                onOpenSource = onOpenSource,
                failedSourceUrl = failedSourceUrl,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The whole Lesson in one scrolling column.
 *
 * A `Column` with `verticalScroll` rather than a `LazyColumn`: a Lesson is a bounded authored
 * document of a few dozen blocks read end to end, so virtualization would buy nothing and would
 * cost the straightforward nesting the block renderers rely on.
 *
 * The column is capped at [AppLayout.MaxContentWidth] and centred. The shell applies the same cap,
 * but long-form prose is the surface where an uncapped measure is actually unreadable — a paragraph
 * spanning a 1600px browser window — so the reader states the limit itself instead of depending on
 * a shell it can be rendered without. On a phone the cap is never reached and the page is full
 * width behind the ordinary screen margins.
 *
 * The scroll state is keyed on the Lesson: previous/next replaces the route rather than pushing
 * one, and a reader that opened the next Lesson already scrolled halfway down would be reading from
 * a position that belongs to the Lesson it just left. Keying is the whole fix — nothing scrolls
 * itself to the top on recomposition.
 */
@Composable
private fun LearningLessonContent(
    state: LearningLessonUiState.Content,
    onNavigateLesson: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    onOpenSource: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    val scrollState = key(state.lessonId) { rememberScrollState() }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = AppLayout.MaxContentWidth)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(appScreenContentPadding())
                .testTag(LearningLessonReadingColumnTag),
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
            state.sections.forEachIndexed { index, section ->
                LearningSectionContent(
                    section = section,
                    showDepthHeading = index == 0 ||
                        state.sections[index - 1].depth != section.depth,
                )
            }
            LessonSources(
                sources = state.sources,
                onOpenSource = onOpenSource,
                failedSourceUrl = failedSourceUrl,
            )
            AdjacentLessonNavigation(state = state, onNavigateLesson = onNavigateLesson)
            // Last on the page, after the way on to the next Lesson. Reading on is the ordinary
            // continuation and stays closest to the material it continues; practising is the step
            // after the reading is done, so it ends the page rather than interrupting it.
            //
            // It practises the whole Unit, not this Lesson: a Lesson teaches part of what the Unit
            // is responsible for, and quizzing that part alone under a Unit label would be a
            // different, narrower assessment than the one offered. Reaching it from here only
            // saves the learner a trip back to the overview.
            Button(
                onClick = onPracticeUnit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.Section)
                    .testTag(LearningLessonPracticeButtonTag),
            ) {
                Text(text = stringResource(Res.string.learning_practice_unit))
            }
        }
    }
}

/**
 * The Lesson's authoritative Sources, if it has any.
 *
 * Optional reference material rather than part of the argument, so it follows the body and
 * disappears entirely when the list is empty — an empty "Sources" heading would promise something
 * the Lesson does not have. Each row is labelled with the authored title: a URL is an address, and
 * a learner choosing what to read next should be reading a name.
 */
@Composable
private fun LessonSources(
    sources: List<SourceReference>,
    onOpenSource: (String) -> Unit,
    failedSourceUrl: String?,
) {
    if (sources.isEmpty()) return
    SectionHeading(stringResource(Res.string.learning_lesson_sources))
    sources.forEach { source ->
        // A link rather than a primary action: leaving the app is a side path off the Lesson, and
        // the same treatment the review screens already give a source.
        TextButton(onClick = { onOpenSource(source.url) }) {
            Icon(
                imageVector = AppIcons.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(SourceIconSize),
            )
            Text(
                text = source.title,
                modifier = Modifier.padding(start = AppSpacing.Related).weight(1f, fill = false),
            )
        }
    }
    // Beside the link that failed rather than at the top of the page, which by then is well out of
    // view. Opening an external URI is best effort; a rejected one must read as a failed action
    // instead of a tap that did nothing.
    if (failedSourceUrl != null && sources.any { it.url == failedSourceUrl }) {
        Text(
            text = stringResource(Res.string.learning_lesson_source_open_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * The way on to the sibling Lesson, at the end of the reading.
 *
 * Unavailable directions are absent rather than disabled: the first Lesson has nothing before it,
 * and a greyed "Previous" would be a control that exists only to keep the row symmetrical. The
 * controls are stacked and full width because each one carries the sibling's title, which is what
 * makes them worth reading — a bare chevron would tell a learner nothing about where it leads, and
 * nothing about where it leads to a screen reader either.
 */
@Composable
private fun AdjacentLessonNavigation(
    state: LearningLessonUiState.Content,
    onNavigateLesson: (String) -> Unit,
) {
    if (state.previousLesson == null && state.nextLesson == null) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        state.previousLesson?.let { previous ->
            AdjacentLessonCard(
                direction = stringResource(Res.string.learning_lesson_previous),
                lesson = previous,
                testTag = LearningLessonPreviousTag,
                onClick = { onNavigateLesson(previous.lessonId) },
            )
        }
        state.nextLesson?.let { next ->
            AdjacentLessonCard(
                direction = stringResource(Res.string.learning_lesson_next),
                lesson = next,
                testTag = LearningLessonNextTag,
                onClick = { onNavigateLesson(next.lessonId) },
            )
        }
    }
}

/**
 * The same card the Unit overview uses for a Lesson, because it does the same thing: choosing which
 * Lesson to read. The direction label above the title is what distinguishes it, and the card's own
 * click semantics already cover the two lines it contains.
 */
@Composable
private fun AdjacentLessonCard(
    direction: String,
    lesson: AdjacentLessonUiModel,
    testTag: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
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
                text = direction,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val SourceIconSize = 16.dp

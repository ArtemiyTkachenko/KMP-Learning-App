package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
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
internal const val LearningLessonReadingProgressTag = "learning_lesson_reading_progress"
internal const val LearningLessonPreviousTag = "learning_lesson_previous"
internal const val LearningLessonNextTag = "learning_lesson_next"
internal const val LearningLessonPracticeButtonTag = "learning_lesson_practice_button"

/**
 * The Lesson reading surface.
 *
 * A reading page, not an assessment one: no score, no question count, no achievement figure, and
 * no answer controls, because none of those exist for learning content and borrowing their visual
 * weight would imply they do. The page reads top to bottom in authored order — title, summary, the
 * Sections, the Sources, and only then the way to the next Lesson — so nothing offers to move on
 * before the material has been shown.
 *
 * The one meter it does carry, [LessonReadingProgress], measures scroll position and nothing else.
 * That is a property of the document rather than of the learner, so it makes no claim about what
 * has been understood and is not the kind of progress the paragraph above rules out.
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
    // Hoisted out of the reading column so the column and the meter above it share one
    // `ScrollState` — the meter sits beside the top bar and the column does not, so neither can own
    // the state the other needs. Still keyed on the Lesson, for the reason [LearningLessonContent]
    // gives; the key is null for the states that have no Lesson, which is a key like any other.
    val scrollState = key((state as? LearningLessonUiState.Content)?.lessonId) {
        rememberScrollState()
    }
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        // A stable label, for the same reason as the Unit overview: the Lesson title leads the
        // content, and the bar has to read sensibly before the Lesson has resolved.
        AppTopBar(stringResource(Res.string.learning_lesson_title), onBack, scrollBehavior)
        // Only under a Lesson. Loading, NotFound, and Error each fill the page with a single
        // centred message that does not scroll, so a reading meter over one would be measuring
        // nothing.
        if (state is LearningLessonUiState.Content) {
            LessonReadingProgress(scrollState)
        }
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
                scrollState = scrollState,
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
 * How much of the Lesson is behind the reader, as a hairline directly beneath the top bar.
 *
 * Position rather than colour. The bar's own container colour is already spoken for: the pinned
 * scroll behaviour tints it once content passes underneath, which is the Material cue for where the
 * bar ends and the page begins, and driving that same colour from reading position would put two
 * meanings on one channel. A colour is also not a quantity — nobody reads a hue as "three quarters"
 * — and as a sole channel it says nothing to a reader who cannot separate the two shades.
 *
 * Attached to the bar rather than run down the side of the column as a draggable thumb. A thumb is
 * a way of *seeking* a list being hunted through; a Lesson is a bounded document meant to be read
 * in authored order, and the page deliberately puts the way onward at the end. A seek handle would
 * be an invitation to skip past the material. It would also have to be built by hand: Compose
 * Multiplatform's `VerticalScrollbar` is desktop-only, absent from the Android, iOS, and web
 * artifacts this module also builds.
 *
 * Deliberately not [org.artkachenko.kmp_learning_app.ui.ProgressMeter], which animates its value.
 * That is right for a measurement that jumps from one figure to another, and wrong here: this one
 * tracks a finger, and an eased catch-up would read as lag rather than as travel.
 */
@Composable
private fun LessonReadingProgress(scrollState: ScrollState) {
    // `maxValue` is `Int.MAX_VALUE` until the column has been measured, and `0` for a Lesson short
    // enough to fit the viewport. Neither is progress worth drawing — a bar pinned at zero for a
    // page with nothing below the fold would report a journey the reader is not on — so the meter
    // is absent rather than empty, and appears on the frame the measurement arrives.
    val scrollRange = scrollState.maxValue
    if (scrollRange <= 0 || scrollRange == Int.MAX_VALUE) return
    // `clearAndSetSemantics` on the wrapper rather than on the indicator. It clears the semantics
    // of a node's *descendants*, not of the node itself, and every semantics modifier in one chain
    // collapses into a single configuration — so on the indicator it would leave the
    // `ProgressBarRangeInfo` that `LinearProgressIndicator` publishes from its own `semantics`
    // block, and clear only the children it does not have.
    //
    // Left uncleared, that range info alone makes the meter screen-reader focusable, so a reader
    // navigating the Lesson element by element hits an unlabelled stop announced as bare "37%"
    // between the toolbar and the title. It also makes every scrolled frame a value change, and a
    // value change on a progress node is an accessibility event pushed to every enabled service.
    Box(
        Modifier
            .fillMaxWidth()
            .height(ReadingProgressHeight)
            .testTag(LearningLessonReadingProgressTag)
            .clearAndSetSemantics {},
    ) {
        LinearProgressIndicator(
            // Read inside the lambda rather than in composition. The indicator samples it while
            // drawing, so a scroll redraws one hairline instead of recomposing the whole Lesson.
            progress = { scrollState.value.toFloat() / scrollRange },
            modifier = Modifier.fillMaxSize(),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            // Square caps and no gap: this spans the window edge to edge as part of the bar above
            // it, where the rounded, gapped treatment of an inset meter would look like a loose
            // component that had drifted under the toolbar. The stop indicator goes for the reason
            // it goes on every meter here — this measures a position, it is not an operation in
            // flight with an end state to mark.
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
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
 * The [scrollState] is keyed on the Lesson by the caller: previous/next replaces the route rather
 * than pushing one, and a reader that opened the next Lesson already scrolled halfway down would be
 * reading from a position that belongs to the Lesson it just left. Keying is the whole fix —
 * nothing scrolls itself to the top on recomposition.
 */
@Composable
private fun LearningLessonContent(
    state: LearningLessonUiState.Content,
    scrollState: ScrollState,
    onNavigateLesson: (String) -> Unit,
    onPracticeUnit: () -> Unit,
    onOpenSource: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
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

/**
 * A hairline. Thinner than [org.artkachenko.kmp_learning_app.ui.ProgressMeter]'s 8dp, because that
 * one is a figure a card exists to show and this one is a margin note on the top bar that should
 * never compete with the Lesson title beneath it.
 */
private val ReadingProgressHeight = 3.dp

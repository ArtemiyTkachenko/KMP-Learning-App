package org.artkachenko.kmp_learning_app.topic_study.learning_lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_lesson_complete
import kmp_learning_app.shared.generated.resources.learning_lesson_end_title
import kmp_learning_app.shared.generated.resources.learning_lesson_error
import kmp_learning_app.shared.generated.resources.learning_lesson_in_progress
import kmp_learning_app.shared.generated.resources.learning_lesson_loading
import kmp_learning_app.shared.generated.resources.learning_lesson_next
import kmp_learning_app.shared.generated.resources.learning_lesson_not_found
import kmp_learning_app.shared.generated.resources.learning_lesson_position
import kmp_learning_app.shared.generated.resources.learning_lesson_previous
import kmp_learning_app.shared.generated.resources.learning_lesson_scroll_to_end
import kmp_learning_app.shared.generated.resources.learning_lesson_source_open_failed
import kmp_learning_app.shared.generated.resources.learning_lesson_sources
import kmp_learning_app.shared.generated.resources.learning_lesson_studied
import kmp_learning_app.shared.generated.resources.learning_lesson_title
import kmp_learning_app.shared.generated.resources.learning_lesson_unmark_studied
import kmp_learning_app.shared.generated.resources.learning_practice_unit
import kmp_learning_app.shared.generated.resources.learning_study_progress_unavailable
import org.artkachenko.kmp_learning_app.curriculum.SourceReference
import org.artkachenko.kmp_learning_app.lesson_study.StudyProgressUiState
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import kmp_learning_app.shared.generated.resources.learning_lesson_depth_core
import kmp_learning_app.shared.generated.resources.learning_lesson_depth_practical
import kmp_learning_app.shared.generated.resources.learning_lesson_depth_senior
import kmp_learning_app.shared.generated.resources.learning_lesson_outline_title
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningDepth
import org.artkachenko.kmp_learning_app.curriculum.learning.LearningSection
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass
import org.artkachenko.kmp_learning_app.ui.theme.maxWidth
import org.jetbrains.compose.resources.StringResource

internal const val LearningLessonLoadingTag = "learning_lesson_loading"
internal const val LearningLessonReadingColumnTag = "learning_lesson_reading_column"
internal const val LearningLessonReadingProgressTag = "learning_lesson_reading_progress"
internal const val LearningLessonPreviousTag = "learning_lesson_previous"
internal const val LearningLessonNextTag = "learning_lesson_next"
internal const val LearningLessonPracticeButtonTag = "learning_lesson_practice_button"
internal const val LearningLessonStudyStatusTag = "learning_lesson_study_status"
internal const val LearningLessonStudyActionTag = "learning_lesson_study_action"
internal const val LearningLessonStudyUnavailableTag = "learning_lesson_study_unavailable"
internal const val LearningLessonScrollToEndTag = "learning_lesson_scroll_to_end"

/** The expanded-window section outline, absent at every other width. */
internal const val LearningLessonOutlineTag = "learning_lesson_outline"

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
 *
 * [onToggleStudied] carries nothing either, and it is the *only* thing on this page that changes
 * study state. Reaching the bottom, pressing Next, opening a Source, and starting practice all
 * leave the learner's record exactly as it was: studied is a claim they make, not one the page
 * makes on their behalf.
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
    onToggleStudied: () -> Unit = {},
    onBottomNavigationVisibilityChange: (Boolean) -> Unit = {},
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    // Hoisted out of the reading column so the column and the meter above it share one
    // `ScrollState` — the meter sits beside the top bar and the column does not, so neither can own
    // the state the other needs. Still keyed on the Lesson, for the reason [LearningLessonContent]
    // gives; the key is null for the states that have no Lesson, which is a key like any other.
    val scrollState = key((state as? LearningLessonUiState.Content)?.lessonId) {
        rememberScrollState()
    }
    val lessonId = (state as? LearningLessonUiState.Content)?.lessonId
    val directionThresholdPx = with(LocalDensity.current) { AppSpacing.Section.roundToPx() }
    val scrollReducer = remember(lessonId, directionThresholdPx) {
        LessonScrollStateReducer(directionThresholdPx)
    }
    var scrollUiState by remember(lessonId) { mutableStateOf(InitialLessonScrollUiState) }
    val currentVisibilityCallback by rememberUpdatedState(onBottomNavigationVisibilityChange)

    DisposableEffect(Unit) {
        onDispose { currentVisibilityCallback(true) }
    }
    LaunchedEffect(scrollState, scrollReducer) {
        var previousBottom = false
        var lastNavigationRequest: Boolean? = null
        snapshotFlow { scrollState.value to scrollState.maxValue }.collect { (position, maximum) ->
            val next = scrollReducer.update(position, maximum)
            scrollUiState = next
            if (next.showsBottomNavigation != lastNavigationRequest) {
                currentVisibilityCallback(next.showsBottomNavigation)
                lastNavigationRequest = next.showsBottomNavigation
            }

            val reachedBottom = next.isAtBottom && !previousBottom && maximum > 0
            previousBottom = next.isAtBottom
            if (reachedBottom) {
                // Showing the reserved bottom bar shortens the viewport. Stay anchored to the end
                // after remeasurement unless the reader has already moved upward.
                val reachedPosition = position
                withFrameNanos { }
                if (scrollState.value >= reachedPosition) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }
        }
    }

    val content = state as? LearningLessonUiState.Content
    val topBarTitle = content?.placement?.unitTitle
        ?: content?.title
        ?: stringResource(Res.string.learning_lesson_title)
    val topBarSubtitle = content?.placement?.let { placement ->
        stringResource(
            Res.string.learning_lesson_position,
            placement.position,
            placement.lessonCount,
        )
    }
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(
            title = topBarTitle,
            onBack = onBack,
            scrollBehavior = scrollBehavior,
            subtitle = topBarSubtitle,
            isSubtitleVisible = scrollUiState.showsToolbarSubtitle,
        )
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
                onToggleStudied = onToggleStudied,
                failedSourceUrl = failedSourceUrl,
                showsScrollToEnd = scrollUiState.hasContentBelow,
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
 * The whole Lesson in one scrolling column, with an outline beside it where there is room.
 *
 * A `Column` with `verticalScroll` rather than a `LazyColumn`: a Lesson is a bounded authored
 * document of a few dozen blocks read end to end, so virtualization would buy nothing and would
 * cost the straightforward nesting the block renderers rely on.
 *
 * The column is capped at the reading measure — [AppLayout.ReadingMeasure] plus the window margin
 * on each side — and centred. This is the one screen in the app that does not take
 * [AppLayout.MaxContentWidth]: 840dp of `bodyLarge` is something over a hundred characters a line,
 * which is past the width at which the eye reliably finds the start of the next one. Dashboards
 * and lists are scanned and do not have that problem; prose is read, and does. On a phone the cap
 * is never reached and the page is full width behind the ordinary screen margins.
 *
 * The extra width an expanded window has is spent on orientation rather than on measure. The
 * outline is built from the Lesson's own structure — the depth runs and the authored Section
 * titles, which are the same two things [LearningSectionContent] draws as headings — so it cannot
 * drift from the page and nothing is parsed out of rendered text to produce it. A Lesson whose
 * Sections carry no titles and sit in one depth run produces fewer than two entries and gets no
 * outline at all, which is correct: a contents list of one item is a decoration.
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
    onToggleStudied: () -> Unit,
    failedSourceUrl: String?,
    showsScrollToEnd: Boolean,
    modifier: Modifier,
) {
    val outline = rememberLessonOutline(state.sections)
    val showsOutline = LocalAppWindowSizeClass.current.isExpanded && outline.size >= MinimumOutlineEntries
    // Where each outlined Section starts, in the scrolling column's own coordinates. Measured
    // rather than estimated: block heights depend on the font scale, the window width, and how a
    // paragraph happened to wrap, none of which are knowable from the authored model. Keyed on the
    // Lesson so positions from the previous one cannot survive a previous/next.
    val sectionOffsets = remember(state.lessonId) { mutableStateMapOf<Int, Int>() }
    var columnTop by remember(state.lessonId) { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier
                .widthIn(max = lessonLayoutMaxWidth(showsOutline))
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AppLayout.PaneGutter),
        ) {
            Column(
                modifier = Modifier
                    // `weight` rather than `widthIn` so the reading column keeps the measure it
                    // would have had on its own: the Row above is capped at the measure plus the
                    // outline plus the gutter, so what is left after the outline is exactly the
                    // measure.
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(appScreenContentPadding())
                    .onGloballyPositioned { columnTop = it.positionInWindow().y }
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
                LessonStudyStatus(studyState = state.studyState)
                state.sections.forEachIndexed { index, section ->
                    LearningSectionContent(
                        section = section,
                        showDepthHeading = index == 0 ||
                            state.sections[index - 1].depth != section.depth,
                        modifier = if (showsOutline) {
                            Modifier.onGloballyPositioned { coordinates ->
                                sectionOffsets[index] =
                                    (coordinates.positionInWindow().y - columnTop).toInt() +
                                        scrollState.value
                            }
                        } else {
                            Modifier
                        },
                    )
                }
                LessonSources(
                    sources = state.sources,
                    onOpenSource = onOpenSource,
                    failedSourceUrl = failedSourceUrl,
                )
                LessonEnding(
                    state = state,
                    onToggleStudied = onToggleStudied,
                    onNavigateLesson = onNavigateLesson,
                    onPracticeUnit = onPracticeUnit,
                )
            }
            if (showsOutline) {
                LessonOutline(
                    entries = outline,
                    // Read inside the lambda so scrolling recomposes the outline and not the
                    // Lesson: the reading column samples nothing from this.
                    currentIndex = {
                        outline.lastOrNull { entry ->
                            (sectionOffsets[entry.sectionIndex] ?: Int.MAX_VALUE) <=
                                scrollState.value + OutlineActivationSlack
                        // Falling back to the first entry rather than to nothing. At the very top
                        // of a Lesson the reader is above every Section — they are in the title and
                        // summary — and an outline with no entry marked reads as one that has not
                        // worked out where they are, rather than as an accurate statement that they
                        // have not reached section one yet.
                        }?.sectionIndex ?: outline.firstOrNull()?.sectionIndex
                    },
                    onEntryClick = { sectionIndex ->
                        sectionOffsets[sectionIndex]?.let { offset ->
                            scope.launch { scrollState.animateScrollTo(offset) }
                        }
                    },
                )
            }
        }
        AnimatedVisibility(
            visible = showsScrollToEnd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.Comfortable),
            enter = fadeIn(AppMotion.effectSpec()),
            exit = fadeOut(AppMotion.effectSpec(AppMotion.StateChangeDurationMillis / 2)),
        ) {
            SmallFloatingActionButton(
                onClick = { scope.launch { scrollState.animateToLessonEnd() } },
                modifier = Modifier.testTag(LearningLessonScrollToEndTag),
            ) {
                Icon(
                    imageVector = AppIcons.ArrowDownward,
                    contentDescription = stringResource(Res.string.learning_lesson_scroll_to_end),
                )
            }
        }
    }
}

private suspend fun ScrollState.animateToLessonEnd() {
    repeat(MaxEndScrollPasses) {
        val target = maxValue
        if (target <= 0 || target == Int.MAX_VALUE) return
        animateScrollTo(target)
        withFrameNanos { }
        if (!canScrollForward || maxValue == target) return
    }
    animateScrollTo(maxValue)
}

/**
 * The widest the Lesson layout may become.
 *
 * Without an outline this is the reading measure and nothing more, which is what keeps a Lesson
 * centred in a desktop window rather than stretched across it. With one, the outline and its
 * gutter are added on top, so the prose keeps exactly the same measure either way — the outline
 * takes space from the empty margins, never from the text.
 */
@Composable
@ReadOnlyComposable
private fun lessonLayoutMaxWidth(showsOutline: Boolean): Dp {
    val reading = AppContentWidth.Reading.maxWidth()
    return if (showsOutline) reading + AppLayout.PaneGutter + LessonOutlineWidth else reading
}

/** One entry of the Lesson outline: what the page shows as a heading, and the Section it heads. */
@Immutable
private data class LessonOutlineEntry(
    val sectionIndex: Int,
    val label: String,
)

/**
 * The outline, derived from the same two facts the page draws headings from.
 *
 * A Section earns an entry when it is the start of a depth run — which is exactly when
 * [LearningSectionContent] draws the depth heading — or when it carries an authored title. Its
 * label is that title where there is one and the depth otherwise, so every entry names something a
 * reader can actually see on the page. A Section with no title in the middle of a depth run has no
 * heading of its own and so gets no entry: there would be nothing for it to point at.
 */
@Composable
private fun rememberLessonOutline(sections: List<LearningSection>): List<LessonOutlineEntry> {
    val depthLabels = LearningDepth.entries.associateWith { stringResource(it.outlineLabel()) }
    return remember(sections, depthLabels) {
        sections.mapIndexedNotNull { index, section ->
            val startsDepthRun = index == 0 || sections[index - 1].depth != section.depth
            val label = section.title ?: depthLabels[section.depth].takeIf { startsDepthRun }
            label?.let { LessonOutlineEntry(sectionIndex = index, label = it) }
        }
    }
}

/**
 * Where the reader is in the Lesson, and a way to move within it.
 *
 * Beside the prose rather than above it, and only on a window wide enough that it costs the text
 * nothing. On a phone this would be a second navigation system competing with the previous/next
 * cards at the end of the Lesson, and it would take more of the reading area than the title.
 *
 * It does not scroll with the page — it is a sibling of the reading column, not a child of it — so
 * it stays in view as a reader moves through a long Lesson, which is the whole reason it is worth
 * the width. It scrolls on its own if the Lesson has more sections than the window is tall.
 *
 * Entries are buttons rather than a list of links: each one moves the page, which is an action.
 * The current one is marked by weight and colour together, never colour alone, and publishes
 * `selected` so assistive technology hears which section the reader is in rather than having to
 * infer it from a tint.
 */
@Composable
private fun LessonOutline(
    entries: List<LessonOutlineEntry>,
    currentIndex: () -> Int?,
    onEntryClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(LessonOutlineWidth)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = AppSpacing.Comfortable)
            .testTag(LearningLessonOutlineTag),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
    ) {
        Text(
            text = stringResource(Res.string.learning_lesson_outline_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(bottom = AppSpacing.Tight)
                .semantics { heading() },
        )
        entries.forEach { entry ->
            val isCurrent = currentIndex() == entry.sectionIndex
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .selectable(
                        selected = isCurrent,
                        onClick = { onEntryClick(entry.sectionIndex) },
                    )
                    .heightIn(min = OutlineEntryMinHeight)
                    .padding(
                        horizontal = AppSpacing.Related,
                        vertical = AppSpacing.Related,
                    ),
            )
        }
    }
}

/**
 * The depth layer's name as an outline entry.
 *
 * The same resources the page's own depth headings use, so an entry and the heading it points at
 * can never disagree. It is a separate function only because the one in `LearningLessonBlocks` is
 * private to that file.
 */
private fun LearningDepth.outlineLabel(): StringResource =
    when (this) {
        LearningDepth.CORE -> Res.string.learning_lesson_depth_core
        LearningDepth.PRACTICAL -> Res.string.learning_lesson_depth_practical
        LearningDepth.SENIOR -> Res.string.learning_lesson_depth_senior
    }

/**
 * Narrow enough that the reading column keeps the whole measure, wide enough for a Section title
 * to wrap to two lines rather than to five.
 */
private val LessonOutlineWidth: Dp = 220.dp

/** Material's minimum touch target, which an outline entry has to clear like any other control. */
private val OutlineEntryMinHeight: Dp = 48.dp

/** Below this an outline is a decoration rather than a way of getting anywhere. */
private const val MinimumOutlineEntries = 2

/**
 * How far past a Section's top the reader may be and still be "in" the previous one.
 *
 * Without it the entry would change the instant a heading's top pixel crossed the viewport's top
 * edge, which happens while that heading is still off screen below. A quarter of the usual
 * viewport is not knowable here, so this is a fixed, deliberately small allowance.
 */
private const val OutlineActivationSlack = 24

/**
 * The end of the Lesson: finish it, then choose where to go next.
 *
 * This is the whole of E-P1's completion hierarchy in one place, and the order is the argument.
 * Completion used to be offered directly under the summary as a filled-weight control beside a
 * badge, so the first thing a learner met on an unread Lesson was an invitation to declare they had
 * read it. Up there the page now only *states* whether the Lesson is studied; the action to change
 * that lives here, where reaching it means the material is genuinely behind the reader.
 *
 * Then continuation, and only one of the two options is primary. Reading on is the ordinary next
 * step in an authored sequence, so on any Lesson with a successor the next-Lesson card is the
 * emphasised surface and practice steps down to an outlined button. On the last Lesson of a Unit
 * there is nothing left to read, so practising the Unit becomes the primary action and takes the
 * filled button — which is the same decision, not a different one: whatever the real next step is,
 * exactly one control says so.
 */
@Composable
private fun LessonEnding(
    state: LearningLessonUiState.Content,
    onToggleStudied: () -> Unit,
    onNavigateLesson: (String) -> Unit,
    onPracticeUnit: () -> Unit,
) {
    val hasNextLesson = state.nextLesson != null
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LessonCompletion(studyState = state.studyState, onToggleStudied = onToggleStudied)
        state.previousLesson?.let { previous ->
            AdjacentLessonCard(
                direction = stringResource(Res.string.learning_lesson_previous),
                lesson = previous,
                testTag = LearningLessonPreviousTag,
                emphasised = false,
                onClick = { onNavigateLesson(previous.lessonId) },
            )
        }
        state.nextLesson?.let { next ->
            AdjacentLessonCard(
                direction = stringResource(Res.string.learning_lesson_next),
                lesson = next,
                testTag = LearningLessonNextTag,
                emphasised = true,
                onClick = { onNavigateLesson(next.lessonId) },
            )
        }
        // It practises the whole Unit, not this Lesson: a Lesson teaches part of what the Unit is
        // responsible for, and quizzing that part alone under a Unit label would be a different,
        // narrower assessment than the one offered. Reaching it from here only saves the learner a
        // trip back to the overview.
        LessonPracticeAction(primary = !hasNextLesson, onPracticeUnit = onPracticeUnit)
    }
}

/**
 * One control, filled where practice is the real next step and outlined where reading on is.
 *
 * Both branches keep [LearningLessonPracticeButtonTag] and the same label, because they are the same
 * action: only its rank against the next-Lesson card changes.
 */
@Composable
private fun LessonPracticeAction(
    primary: Boolean,
    onPracticeUnit: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().testTag(LearningLessonPracticeButtonTag)
    val label = @Composable { Text(text = stringResource(Res.string.learning_practice_unit)) }
    if (primary) {
        Button(onClick = onPracticeUnit, modifier = modifier) { label() }
    } else {
        OutlinedButton(onClick = onPracticeUnit, modifier = modifier) { label() }
    }
}

/**
 * Whether this Lesson is studied, stated near the top and nothing more.
 *
 * This used to be a badge with a same-size outlined button beside it, so an unread Lesson opened
 * with "Not studied · Mark as studied" as the third thing on the page — an invitation to declare the
 * material read, placed before any of it. Up here the page now only reports: `In progress` until the
 * learner says otherwise, `Studied` once they have. The action that changes it is at the end of the
 * Lesson, in [LessonCompletion].
 *
 * "In progress" rather than "Not studied" because the reader is, by definition, in the middle of it:
 * the negative phrasing described the record rather than the learner, and read as a reproach on a
 * page they had just opened.
 *
 * Nothing is shown while study state is loading. A badge over a record that has not been read yet
 * would be a claim about the learner, and the page is already readable without it.
 */
@Composable
private fun LessonStudyStatus(studyState: StudyProgressUiState<LessonStudyUiModel>) {
    when (studyState) {
        StudyProgressUiState.Loading -> Unit
        // Says what is missing, not what the answer is. "Not studied" here would invent a record
        // the app could not read, which is the one thing this state exists to prevent. The Lesson,
        // its Sources, its neighbours, and Practice this unit are all untouched.
        StudyProgressUiState.Unavailable -> Text(
            text = stringResource(Res.string.learning_study_progress_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(LearningLessonStudyUnavailableTag),
        )
        is StudyProgressUiState.Available -> {
            val studied = studyState.value.isStudied
            StatusBadge(
                text = stringResource(
                    if (studied) {
                        Res.string.learning_lesson_studied
                    } else {
                        Res.string.learning_lesson_in_progress
                    },
                ),
                contentColor = if (studied) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                containerColor = if (studied) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                icon = if (studied) AppIcons.CheckCircle else null,
                modifier = Modifier.testTag(LearningLessonStudyStatusTag),
            )
        }
    }
}

/**
 * The end-of-lesson completion step, and — once it is done — the way to undo it.
 *
 * The two states are deliberately not symmetrical, which is the point of moving this here. An
 * unstudied Lesson ends on a short prompt and one filled button, because completing is what the
 * learner has just earned the right to do. A studied Lesson ends on a statement with a text button
 * under it: "Studied" and "Mark as not studied" used to be two pill-shaped controls of equal weight
 * sitting side by side, which made undoing look like half of what the feature was for.
 *
 * Both states carry the same [LearningLessonStudyActionTag], because both are the one control that
 * writes study state. The button's visible label is the action it performs — which is what a screen
 * reader announces from the Material button's own semantics — and the stored value travels
 * alongside it as `stateDescription`, so the control says both what it will do and what is true now.
 *
 * The button is disabled only while this Lesson's own write is being persisted, and the status above
 * keeps showing the stored value throughout: a pending mark must never be drawn as though it had
 * already been saved.
 *
 * Nothing is rendered for Loading or Unavailable. A completion action over a record the app could
 * not read would offer to change something whose current value is unknown.
 */
@Composable
private fun LessonCompletion(
    studyState: StudyProgressUiState<LessonStudyUiModel>,
    onToggleStudied: () -> Unit,
) {
    val study = (studyState as? StudyProgressUiState.Available)?.value ?: return
    val stateLabel = stringResource(
        if (study.isStudied) Res.string.learning_lesson_studied else Res.string.learning_lesson_in_progress,
    )
    if (study.isStudied) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(StudiedIconSize),
            )
            Text(
                text = stringResource(Res.string.learning_lesson_studied),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onToggleStudied,
                enabled = !study.isPending,
                modifier = Modifier
                    .testTag(LearningLessonStudyActionTag)
                    .semantics { stateDescription = stateLabel },
            ) {
                Text(text = stringResource(Res.string.learning_lesson_unmark_studied))
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped)) {
            Text(
                text = stringResource(Res.string.learning_lesson_end_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(
                onClick = onToggleStudied,
                enabled = !study.isPending,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LearningLessonStudyActionTag)
                    .semantics { stateDescription = stateLabel },
            ) {
                Text(text = stringResource(Res.string.learning_lesson_complete))
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
 * The same card the Unit overview uses for a Lesson, because it does the same thing: choosing which
 * Lesson to read. The direction label above the title is what distinguishes it, and the card's own
 * click semantics already cover the two lines it contains.
 *
 * Unavailable directions are absent rather than disabled: the first Lesson has nothing before it,
 * and a greyed "Previous" would be a control that exists only to keep the layout symmetrical. Each
 * card is full width and carries the sibling's title, which is what makes it worth reading — a bare
 * chevron would tell a learner nothing about where it leads, and nothing about where it leads to a
 * screen reader either.
 *
 * [emphasised] is the whole of the end-of-lesson ranking: the next Lesson gets the primary container
 * so it reads as the recommended continuation, and Previous keeps the quiet container it always had.
 * Both remain the same kind of control, so nothing about going back is harder than it was.
 */
@Composable
private fun AdjacentLessonCard(
    direction: String,
    lesson: AdjacentLessonUiModel,
    testTag: String,
    emphasised: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (emphasised) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (emphasised) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Comfortable),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                Text(
                    text = direction,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (emphasised) {
                        contentColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(ChevronSize),
            )
        }
    }
}

private val SourceIconSize = 16.dp

private val ChevronSize = 20.dp

/** Matches the badge the same fact is drawn as at the top of the page. */
private val StudiedIconSize = 18.dp

/**
 * A hairline. Thinner than [org.artkachenko.kmp_learning_app.ui.ProgressMeter]'s 8dp, because that
 * one is a figure a card exists to show and this one is a margin note on the top bar that should
 * never compete with the Lesson title beneath it.
 */
private val ReadingProgressHeight = 3.dp

private const val MaxEndScrollPasses = 3

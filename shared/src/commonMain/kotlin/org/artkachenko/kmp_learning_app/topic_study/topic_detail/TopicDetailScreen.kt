package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_detail_heading
import kmp_learning_app.shared.generated.resources.topic_detail_loading
import kmp_learning_app.shared.generated.resources.topic_detail_not_found
import kmp_learning_app.shared.generated.resources.topic_detail_practice
import kmp_learning_app.shared.generated.resources.topic_detail_study
import kmp_learning_app.shared.generated.resources.topic_detail_subtopics
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.ScreenStateTransition
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import kotlin.math.roundToInt

internal const val TopicDetailLoadingTag = "topic_detail_loading"
internal const val TopicPracticeButtonTag = "topic_practice_button"
internal const val SubtopicPracticeButtonTag = "subtopic_practice_button"

/**
 * The Topic's secondary way into the Practice Builder, beside the promoted recommendation.
 *
 * The Topic-level weak and unseen shortcuts this replaces are gone: their two intents are now ranked
 * into one promoted action, and everything they could reach stays reachable through the builder this
 * opens. The Subtopic rows keep their own shortcuts, which are not ranked against anything.
 */
internal const val TopicCustomPracticeTag = "topic_custom_practice"

internal fun subtopicWeakPracticeTag(subtopicId: String): String =
    "subtopic_weak_practice_$subtopicId"

internal fun subtopicUnseenPracticeTag(subtopicId: String): String =
    "subtopic_unseen_practice_$subtopicId"

internal fun learningUnitCardTag(unitId: String): String = "learning_unit_$unitId"

internal fun learningUnitStudyTag(unitId: String): String = "learning_unit_study_$unitId"

internal const val TopicStudyUnavailableTag = "topic_study_unavailable"

/** The Topic-level "N of M lessons studied" figure and its meter, above the Unit list. */
internal const val TopicStudyProgressTag = "topic_study_progress"

/**
 * The Practice page's curriculum-coverage bar.
 *
 * Tagged because its *absence* is the assertion worth protecting: the counts beside it are a true
 * statement at zero, and the bar is not — a Topic nobody has attempted must not be shown a gauge at
 * a value the learner never produced.
 */
internal const val TopicCoverageMeterTag = "topic_coverage_meter"

/**
 * The two lazy lists themselves, so a test can drive one to a node it wants.
 *
 * A lazy list composes only what is on screen, so a Unit or Subtopic further down does not exist in
 * the semantics tree until the list is scrolled to it; and the pager is horizontally scrollable
 * too, so a bare scroll-action matcher cannot say which of the two it meant.
 */
internal const val TopicStudyListTag = "topic_study_list"
internal const val TopicSubtopicsListTag = "topic_subtopics_list"

/**
 * The hairlines between Subtopic rows, tagged only so a test can assert how many there are.
 *
 * That count is the assertion: a rule belongs *between* two rows, and one drawn after the last is a
 * separator with nothing to separate. A `HorizontalDivider` publishes no semantics of its own, so
 * counting the tag is the only way to state that. Nothing about where a rule sits or how wide it is
 * is asserted; those are geometry. The tag adds no role, label or state, so the rules stay absent
 * from the accessibility tree.
 */
internal const val SubtopicRowDividerTag = "subtopic_row_divider"

/**
 * The tabs themselves, so a test can select a page without matching localised label text.
 *
 * Selected state is not tagged: `Tab` already exposes it through standard Material selection
 * semantics, and a second, hand-maintained signal for the same thing could disagree with it.
 */
internal const val TopicStudyTabTag = "topic_tab_study"
internal const val TopicPracticeTabTag = "topic_tab_practice"
internal const val TopicSubtopicsTabTag = "topic_tab_subtopics"

/**
 * The single moving indicator, tagged only so a test can assert that there is exactly one of it.
 *
 * That is the whole point of the tag: the row used to own three separate rules that crossfaded past
 * each other, and one shared rule is what replaced them. It also lets a test hold the rule to the
 * selected tab's edges at rest — a relationship, never a coordinate or a mid-drag frame. A bare
 * test tag adds no role, no label, and no state, so the indicator stays absent from the
 * accessibility tree; selection is still announced by the tabs alone.
 */
internal const val TopicTabIndicatorTag = "topic_tab_indicator"

/**
 * A Topic's three capabilities, in the order they are taught.
 *
 * Study first because reading the material precedes being asked about it, Practice second because
 * it is what the whole Topic is assessed on, and Subtopics last because it is the drill-down. The
 * declaration order *is* the page order and the tab order, so the two cannot fall out of step.
 */
private enum class TopicDetailTab(val label: StringResource, val testTag: String) {
    Study(Res.string.topic_detail_study, TopicStudyTabTag),
    Practice(Res.string.topic_detail_practice, TopicPracticeTabTag),
    Subtopics(Res.string.topic_detail_subtopics, TopicSubtopicsTabTag),
}

/**
 * How far a drag has to travel before it commits to the next page rather than springing back.
 *
 * See the pager's fling behaviour for why this sits below Compose's 0.5 default.
 */
private const val TabSnapPositionalThreshold = 0.25f

/**
 * Material's own tab height (`PrimaryNavigationTabTokens.ContainerHeight`). It is also exactly the
 * minimum touch target, so the two constraints are satisfied by one number.
 *
 * It is applied as a **minimum** rather than as a fixed height. As a fixed one it clipped the tab
 * labels at a doubled type size: the cell had no room for the line box, so the selected label's
 * descender was sliced flat by the bottom of the cell. The tab row sizes itself from its tallest
 * tab, so a minimum lets the row grow with the type while leaving the touch target and the
 * container height untouched at every ordinary scale.
 */
private val TabHeight = 48.dp

/**
 * The space either side of a tab's label, which with the label is the whole of a tab's natural
 * width — Material's `Tab` adds none of its own. Named because [sharedMinimumTabWidth] is given the
 * natural widths before the tabs are composed, and has to add the same padding the tab does.
 */
private val TabLabelHorizontalPadding = AppSpacing.Related

/**
 * Material's `PrimaryNavigationTabTokens.ActiveIndicatorHeight`, and the height of the rule that
 * marks the selected tab.
 */
private val TabIndicatorHeight = 3.dp

/**
 * One tab: a label that changes colour with selection, and nothing else.
 *
 * These tabs used to carry the same filled `secondaryContainer` pill the navigation bar gives the
 * current area, on the reasoning that "this is the thing you are looking at" should look the same
 * everywhere. The reasoning was sound about the *fact* and wrong about the *level*: the app has two
 * navigation systems and they are not peers. The bar or rail says which of the four areas of the
 * product the learner is in and persists across every screen in it; this row says which of one
 * Topic's three capabilities is on screen and exists only here. Drawing them identically made a
 * page control look like a second copy of the app's navigation, and made the Topic screen read as
 * though it had two rows of destinations.
 *
 * So the distinction is the indicator: the app's navigation keeps the filled pill, and page-level
 * tabs take Material's own tab affordance — a rule under the selected tab, with the label in
 * `primary` rather than on a container. That is a step down in weight without being quiet.
 *
 * The rule itself is no longer drawn here. Each tab used to own and crossfade its own underline,
 * which made three independent controls out of what is really one continuous pager; the row now
 * renders [TopicTabIndicator] once and moves it. What is left in this composable is the label, its
 * colours, and the tab's own Material behaviour: with no pill there is nothing for a hard-edged
 * state layer to disagree with, so the tab keeps Material's full-cell hover, focus, and press,
 * which is what makes the whole cell visibly the target on a pointer host.
 *
 * The label still switches on `selected`, crossfaded by Material's own `Tab` transition. Mid-drag
 * that crossover lands at the halfway point — the same place the indicator is — so the two agree
 * without the label needing any pager-derived state of its own.
 */
@Composable
private fun TopicDetailTab(
    tab: TopicDetailTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = TabHeight)
            .testTag(tab.testTag),
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = stringResource(tab.label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(
                horizontal = TabLabelHorizontalPadding,
                vertical = AppSpacing.Related,
            ),
        )
    }
}

/**
 * The one rule under the tab row, positioned from [pagerState] rather than from a selected index.
 *
 * This is the whole point of the change. A drag is a continuous thing and the pages already move
 * continuously with the finger, so the mark that says which page you are on should move with them.
 * Deriving it from an animated selected index cannot do that: it only learns that something
 * happened once `currentPage` flips, and then plays a second animation of its own, arriving after
 * the page it is describing. There is no animation here and no `AppMotion` spec, deliberately —
 * the pager *is* the animation, for a drag and equally for the `animateScrollToPage` a tab tap
 * runs, and anything layered on top would only add lag.
 *
 * `currentPage + currentPageOffsetFraction` is that continuous position in whole-tab units. It
 * stays continuous across the midpoint where `currentPage` flips and the fraction changes sign, it
 * rewinds by itself when a drag is abandoned below the snap threshold, and it is exactly integral
 * whenever the pager is idle — so a settled indicator lands on the tab, not a fraction of a pixel
 * away from it. The coercion only guards an overscroll pull at either end, which is a stretch of
 * the pager and not a navigation.
 *
 * The tabs are content-sized, so neither where a tab starts nor how wide it is can be computed from
 * its index. Both come from the `TabPosition`s the row hands [TabIndicatorScope.tabIndicatorLayout]
 * — the same values it placed the tabs with — and the rule's two edges are interpolated separately
 * by [tabIndicatorEdge]. Moving the edges rather than a centre is what makes the rule both travel
 * and change width between a narrow tab and a wide one, from the one pager position and with no
 * width animation of its own. Reading the pager inside `measure` means a drag re-measures this one
 * node instead of recomposing the row.
 *
 * The rule spans the whole tab cell, as it did when the cells were equal thirds, rather than
 * Material's content-width inset: it is the underline of the tab, not a mark under its word.
 *
 * The slot is placed by the row, not by this node: the row puts it at
 * `max(0, (selected tab width - slot width) / 2)` from the start and never adds the tab's own
 * `left`. Reporting a zero-width slot — which every constraint admits — makes that offset exactly
 * half the selected tab's width, and the rule is placed back by the same amount. The row mirrors
 * that offset under a right-to-left layout, but `placeRelative` does not mirror inside a zero-width
 * layout, so the rule's own placement is mirrored here by hand — the tab positions are logical,
 * measured from the start edge — as Material's own indicator offset does. [selectedTabIndex] has
 * to be the index the row was given in the same composition, because it is the row's own offset
 * being undone.
 *
 * Using the indicator slot at all is a reversal: it was left empty because it is placed after the
 * tabs, and the filled pill that lived here then would have covered the label it was marking. A
 * rule at the bottom edge never reaches the label, and drawing it last is what stock Material does.
 * It has no pointer input of its own, so it cannot take a tap from the tab underneath it.
 */
@Composable
private fun TabIndicatorScope.TopicTabIndicator(pagerState: PagerState, selectedTabIndex: Int) {
    Box(
        Modifier
            .tabIndicatorLayout { measurable, constraints, tabPositions ->
                if (tabPositions.isEmpty()) return@tabIndicatorLayout layout(0, 0) {}
                val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val left = tabIndicatorEdge(position, tabPositions.size) {
                    tabPositions[it].left.toPx()
                }.roundToInt()
                val right = tabIndicatorEdge(position, tabPositions.size) {
                    tabPositions[it].right.toPx()
                }.roundToInt()
                val width = (right - left).coerceAtLeast(0)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = width, maxWidth = width),
                )
                val rowOffset = tabPositions[selectedTabIndex].width.roundToPx() / 2
                val x = when (layoutDirection) {
                    LayoutDirection.Ltr -> left - rowOffset
                    LayoutDirection.Rtl -> rowOffset - left - width
                }
                layout(0, placeable.height) {
                    placeable.place(x = x, y = 0)
                }
            }
            .fillMaxWidth()
            .height(TabIndicatorHeight)
            .background(MaterialTheme.colorScheme.primary)
            .testTag(TopicTabIndicatorTag),
    )
}

/**
 * One edge of the tab indicator — its start or its end — at a continuous pager [position], in the
 * tab row's own logical coordinates.
 *
 * [position] is `currentPage + currentPageOffsetFraction`, and it is used only as that sum. The
 * pager flips `currentPage` at the midpoint of a swipe while the fraction changes sign, so the page
 * a gesture started from cannot be read off `currentPage`; the sum does not change at the flip, so
 * neither does anything derived from it. The tab below the position and the one after it are the
 * pair it lies between, and the edge moves linearly from one tab's to the other's, whichever
 * direction the swipe is going and however often it reverses.
 *
 * The clamp is for an overscroll pull at either end, which stretches the pager past its first or
 * last page without navigating anywhere: the edge stays on the end tab rather than leaving the row.
 */
internal fun tabIndicatorEdge(position: Float, tabCount: Int, edgeOf: (Int) -> Float): Float {
    if (tabCount <= 1) return edgeOf(0)
    val clamped = position.coerceIn(0f, (tabCount - 1).toFloat())
    val from = clamped.toInt().coerceAtMost(tabCount - 2)
    val fraction = clamped - from
    return edgeOf(from) + (edgeOf(from + 1) - edgeOf(from)) * fraction
}

/**
 * The minimum width every Topic tab is given, in pixels, from the row's width and each tab's
 * natural width — its label on one line plus its padding.
 *
 * It is the largest common minimum that does not make the row wider than [rowWidth]: lifting the
 * narrower tabs to it spends exactly the slack the wider ones leave. When every label fits an equal
 * share, that share is the answer, so at an ordinary type size the row is the three equal cells it
 * has always been. When one label outgrows its share, it keeps its natural width and the others
 * divide what is left, so the row still fills the window and does not scroll. Only when the natural
 * widths together are wider than the row is there nothing to share: the minimum falls to
 * [materialMinimum], each tab keeps its own width, and the row scrolls.
 *
 * It never goes below [materialMinimum] — Material's own smallest scrollable tab — and it is a
 * floor, not a width: no tab is ever made narrower than its label, whatever this returns. That is
 * what keeps a word whole; this only decides how the row spends the space around it.
 */
internal fun sharedMinimumTabWidth(
    rowWidth: Int,
    naturalWidths: List<Int>,
    materialMinimum: Int,
): Int {
    val ascending = naturalWidths.sorted()
    // Lift the k narrowest tabs to a common width and leave the rest at their own; the largest k
    // for which that width still covers every lifted tab is the answer.
    for (lifted in ascending.size downTo 1) {
        val remaining = rowWidth - ascending.drop(lifted).sum()
        val shared = remaining / lifted
        if (shared >= ascending[lifted - 1]) return maxOf(shared, materialMinimum)
    }
    return materialMinimum
}

/**
 * The Topic screen: a top bar, the Topic's terminal states, and — only for a loaded Topic — three
 * tabbed pages.
 *
 * The terminal states deliberately stay outside the pager. Loading, a Topic that does not exist,
 * and a failed curriculum read are statements about the Topic itself, so they replace the whole
 * screen; a tab with nothing in it is a statement about one capability and never does.
 *
 * Practice intents are unchanged and still leave through two separate callbacks.
 * [onStartTopicPractice] and [onStartSubtopicPractice] are ordinary practice: they carry a scope
 * only, so the builder applies its `ALL` default. [onPracticePreset] carries a scope *and* an
 * existing question source, and is emitted only where a page is already displaying the signal that
 * justifies it. Neither is re-derived in the UI.
 */
@Composable
internal fun TopicDetailScreen(
    state: TopicDetailUiState,
    targetSubtopicId: String? = null,
    onBack: () -> Unit,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onRetry: () -> Unit,
    onLearningUnitClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(
            title = when (state) {
                is TopicDetailUiState.Content -> state.topic.name
                else -> stringResource(Res.string.topic_detail_heading)
            },
            onBack = onBack,
            scrollBehavior = scrollBehavior,
        )

        AppScreenPane(AppContentWidth.Standard) {
            // Keyed on the state's class, which is what the default `contentKey` gives: crossing
            // from Loading into the tabs, a message, or an error is one thing becoming another and
            // fades, while a change *within* content keeps the same key and does not.
            ScreenStateTransition(state = state, modifier = Modifier.fillMaxSize()) { current ->
                // The `Column` is not decoration: [TopicDetailTabs] is a `ColumnScope` extension
                // because its pager takes the height the tab row leaves, and inside `AnimatedContent`
                // the scope is a box. Restoring the column here keeps the tabs laid out exactly as
                // they were, so the only thing this change alters is how one state becomes the next.
                Column(Modifier.fillMaxSize()) {
                    when (current) {
                        TopicDetailUiState.Loading -> ScreenLoading(
                            message = stringResource(Res.string.topic_detail_loading),
                            testTag = TopicDetailLoadingTag,
                            modifier = Modifier.weight(1f),
                        )

                        is TopicDetailUiState.Content -> TopicDetailTabs(
                            state = current,
                            targetSubtopicId = targetSubtopicId,
                            onStartTopicPractice = onStartTopicPractice,
                            onStartSubtopicPractice = onStartSubtopicPractice,
                            onPracticePreset = onPracticePreset,
                            onLearningUnitClick = onLearningUnitClick,
                        )

                        TopicDetailUiState.NotFound -> ScreenMessage(
                            message = stringResource(Res.string.topic_detail_not_found),
                            modifier = Modifier.weight(1f),
                        )

                        TopicDetailUiState.Error -> ScreenError(
                            message = stringResource(Res.string.topic_browser_error),
                            onRetry = onRetry,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The tab row and its pager, directly under the top bar so all three capabilities are visible
 * without scrolling. There is no second Topic-name header: the top bar already carries it.
 *
 * The selected tab is presentation state and lives here, not in the route and not in the ViewModel.
 * It is not even a separate value: [rememberPagerState] is the single source of truth, and the tab
 * row simply renders `currentPage`, so a tapped tab and a swiped page cannot disagree. That state
 * is `rememberSaveable`-backed by construction, which is what makes the selection survive ordinary
 * recreation without anything being serialised into the back stack.
 *
 * The same pager state also positions the row's one indicator — see [TopicTabIndicator] — which is
 * why a half-finished swipe leaves the rule halfway between two tabs rather than parked on one.
 *
 * Each page's scroll state is remembered *here* rather than inside the page lambda. A pager keeps
 * only its neighbouring pages composed, so hoisting is what lets a learner scroll deep into the
 * Subtopics, look at Practice, and come back to where they were.
 */
@Composable
private fun ColumnScope.TopicDetailTabs(
    state: TopicDetailUiState.Content,
    targetSubtopicId: String?,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onLearningUnitClick: ((String) -> Unit)?,
) {
    val tabs = TopicDetailTab.entries
    // Arriving with a Subtopic in hand opens on the page that Subtopic is on. This is read from the
    // route data that already exists — AppRoute.Topic.subtopicId — rather than from a tab field
    // added to the route: which page happens to be showing is not navigation state.
    val pagerState = rememberPagerState(
        initialPage = if (targetSubtopicId == null) {
            TopicDetailTab.Study.ordinal
        } else {
            TopicDetailTab.Subtopics.ordinal
        },
        pageCount = { tabs.size },
    )
    val studyListState = rememberLazyListState()
    val practiceScrollState = rememberScrollState()
    val subtopicsListState = rememberLazyListState()

    // Resolved once per (list, target) rather than inside the effect, so the effect below can key on
    // the position itself: a history refresh that rebuilds the rows without moving the target no
    // longer re-runs the arrival scroll under the learner.
    val targetIndex = remember(state.subtopics, targetSubtopicId) {
        if (targetSubtopicId == null) {
            -1
        } else {
            state.subtopics.indexOfFirst { it.subtopic.id == targetSubtopicId }
        }
    }
    LaunchedEffect(targetIndex) {
        // The Subtopics list is now a list of Subtopics and nothing else, so the index is the
        // index: the fixed header offset the combined column needed is gone. A target that no
        // longer exists resolves to -1 and simply leaves the page at the top.
        //
        // Animated rather than instant: arriving here from search used to place the learner at an
        // arbitrary offset with no indication that the screen had scrolled at all, so a Subtopic
        // partway down a long Topic looked like the top of the list.
        if (targetIndex >= 0) {
            subtopicsListState.animateScrollToItem(targetIndex)
        }
    }

    val scope = rememberCoroutineScope()
    // An empty Study or Subtopics page still has somewhere to send the learner, and the only thing
    // that can move the pager is the pager. Selecting a tab is what this function already does for a
    // tab tap, so the empty states borrow it rather than growing any state of their own.
    val browsePractice = {
        scope.launch { pagerState.animateScrollToPage(TopicDetailTab.Practice.ordinal) }
        Unit
    }
    // A content-sized row, so a label keeps its own width at a large type size instead of being
    // broken inside a word to fit an equal third of the window. It scrolls only when the labels
    // together are wider than the window, and Material scrolls the selected tab into view.
    //
    // Material's 52dp edge padding is removed: with it at each end and its 90dp minimum per tab,
    // the row needs 374dp, wider than a compact pane, so it would scroll at every type size and
    // open with a gap before Study. Its minimum is raised to [sharedMinimumTabWidth], which keeps
    // the ordinary row the three equal cells it was and lets a larger type size spend the slack.
    val selectedTabIndex = pagerState.currentPage
    val labels = tabs.map { stringResource(it.label) }
    val labelStyle = MaterialTheme.typography.titleSmall
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints {
        val density = LocalDensity.current
        val rowWidth = constraints.maxWidth
        val minTabWidth = remember(labels, labelStyle, rowWidth, density) {
            with(density) {
                val labelPadding = TabLabelHorizontalPadding.roundToPx() * 2
                sharedMinimumTabWidth(
                    rowWidth = rowWidth,
                    naturalWidths = labels.map {
                        textMeasurer.measure(it, labelStyle).size.width + labelPadding
                    },
                    materialMinimum = TabRowDefaults.ScrollableTabRowMinTabWidth.roundToPx(),
                ).toDp()
            }
        }
        PrimaryScrollableTabRow(
            // Material reads this to size the indicator slot and to scroll the selected tab into
            // view. The indicator's position is read from the pager itself, so the two cannot
            // disagree about which tab is current.
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            minTabWidth = minTabWidth,
            indicator = { TopicTabIndicator(pagerState, selectedTabIndex) },
        ) {
            tabs.forEach { tab ->
                TopicDetailTab(
                    tab = tab,
                    selected = selectedTabIndex == tab.ordinal,
                    onClick = { scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                )
            }
        }
    }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f),
        // Keyed by the tab rather than by page index so a page keeps its identity; the set is
        // fixed, but this is what states that a page is a capability and not a position.
        key = { tabs[it] },
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            // A swipe settles on the neighbouring page and never travels past it, however hard it
            // was thrown: three tabs are three capabilities, not a reel to be flung through. This
            // is also Compose's current default, and is stated because it is behaviour this screen
            // depends on rather than behaviour it happens to inherit.
            pagerSnapDistance = PagerSnapDistance.atMost(1),
            // Below the 0.5 default: a drag past a quarter of the width is already a clear request
            // for the next tab, and making the learner haul it more than halfway before it commits
            // is what makes a pager feel like it is being dragged rather than switched.
            snapPositionalThreshold = TabSnapPositionalThreshold,
        ),
    ) { page ->
        when (tabs[page]) {
            TopicDetailTab.Study -> TopicStudyPage(
                state = state.learningUnits,
                studyProgress = state.studyProgress,
                onLearningUnitClick = onLearningUnitClick,
                onBrowsePractice = browsePractice,
                listState = studyListState,
                modifier = Modifier.fillMaxSize(),
            )

            TopicDetailTab.Practice -> TopicPracticePage(
                topicId = state.topic.id,
                topicQuestionCount = state.topicQuestionCount,
                learningContext = state.learningContext,
                unresolvedMistakeCount = state.unresolvedMistakeCount,
                onStartTopicPractice = onStartTopicPractice,
                onPracticePreset = onPracticePreset,
                scrollState = practiceScrollState,
                modifier = Modifier.fillMaxSize(),
            )

            TopicDetailTab.Subtopics -> TopicSubtopicsPage(
                subtopics = state.subtopics,
                onStartSubtopicPractice = onStartSubtopicPractice,
                onPracticePreset = onPracticePreset,
                onBrowsePractice = browsePractice,
                listState = subtopicsListState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

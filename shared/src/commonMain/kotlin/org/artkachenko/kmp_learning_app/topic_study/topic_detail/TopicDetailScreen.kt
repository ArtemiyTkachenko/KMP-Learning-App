package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
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
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal const val TopicDetailLoadingTag = "topic_detail_loading"
internal const val TopicPracticeButtonTag = "topic_practice_button"
internal const val SubtopicPracticeButtonTag = "subtopic_practice_button"

/** The Topic's own targeted shortcuts, whose labels repeat on every Subtopic row below them. */
internal const val TopicWeakPracticeTag = "topic_weak_practice"
internal const val TopicUnseenPracticeTag = "topic_unseen_practice"

internal fun subtopicWeakPracticeTag(subtopicId: String): String =
    "subtopic_weak_practice_$subtopicId"

internal fun subtopicUnseenPracticeTag(subtopicId: String): String =
    "subtopic_unseen_practice_$subtopicId"

internal fun learningUnitCardTag(unitId: String): String = "learning_unit_$unitId"

internal fun learningUnitStudyTag(unitId: String): String = "learning_unit_study_$unitId"

internal const val TopicStudyUnavailableTag = "topic_study_unavailable"

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
 * The tabs themselves, so a test can select a page without matching localised label text.
 *
 * Selected state is not tagged: `Tab` already exposes it through standard Material selection
 * semantics, and a second, hand-maintained signal for the same thing could disagree with it.
 */
internal const val TopicStudyTabTag = "topic_tab_study"
internal const val TopicPracticeTabTag = "topic_tab_practice"
internal const val TopicSubtopicsTabTag = "topic_tab_subtopics"

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
 * The selected tab's pill. `Shapes` deliberately does not model one — a pill is a function of the
 * element's own height rather than a step on the shape scale — so it is declared locally, which is
 * the convention `AppShapes` sets out.
 */
private val TabIndicatorShape = RoundedCornerShape(percent = 50)

/**
 * One tab, marked as selected by a filled pill rather than by a rule beneath it.
 *
 * The pill is the same treatment the navigation bar already uses for the current area — Material's
 * `secondaryContainer` over `onSecondaryContainer` — so "this is the thing you are looking at"
 * looks the same everywhere in the app. An underline states the same fact far more quietly, and on
 * a three-tab row where the whole point is that the learner notices all three capabilities, the
 * selected one should be unmistakable.
 *
 * The pill hugs its label rather than filling the tab cell: across a wide window a cell is a third
 * of the content measure, and a filled block that size stops reading as a selection marker.
 *
 * The tab's own state layer — hover, focus, press — is clipped to the same shape. A `Tab` otherwise
 * draws it as a hard-edged rectangle across the whole cell, which frames the pill instead of
 * agreeing with it. That is barely visible on a touch screen, where a press fades immediately, and
 * permanent on a desktop pointer: hover is the resting state of whichever tab the mouse happens to
 * be over. Clipping rather than insetting keeps the whole cell clickable.
 *
 * Colour rather than shape animates, and it animates from the same colour at zero alpha rather than
 * from transparent black, which would drag every intermediate frame through grey.
 */
@Composable
private fun TopicDetailTab(
    tab: TopicDetailTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor = MaterialTheme.colorScheme.secondaryContainer
    val container by animateColorAsState(
        targetValue = if (selected) indicatorColor else indicatorColor.copy(alpha = 0f),
        animationSpec = AppMotion.effectSpec(),
    )
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            // Enough of a gap that two adjacent state layers never meet.
            .padding(horizontal = AppSpacing.Tight)
            .clip(TabIndicatorShape)
            .testTag(tab.testTag),
        selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = stringResource(tab.label),
            style = MaterialTheme.typography.titleSmall,
            // The outer inset is what gives the row its height, and keeps the tab's own touch
            // target comfortably past the 48dp minimum while the pill stays label-sized.
            modifier = Modifier
                .padding(vertical = AppSpacing.Related)
                .background(container, TabIndicatorShape)
                .padding(
                    horizontal = AppSpacing.Comfortable,
                    vertical = AppSpacing.Related,
                ),
        )
    }
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

        when (state) {
            TopicDetailUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.topic_detail_loading),
                testTag = TopicDetailLoadingTag,
                modifier = Modifier.weight(1f),
            )

            is TopicDetailUiState.Content -> TopicDetailTabs(
                state = state,
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
    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        // No underline. The indicator slot is measured and placed *after* the tabs, so anything
        // filled drawn here would sit on top of the label it is meant to be highlighting; the
        // selected tab carries its own pill instead, below.
        indicator = {},
    ) {
        tabs.forEach { tab ->
            TopicDetailTab(
                tab = tab,
                selected = pagerState.currentPage == tab.ordinal,
                onClick = { scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
            )
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
                listState = studyListState,
                modifier = Modifier.fillMaxSize(),
            )

            TopicDetailTab.Practice -> TopicPracticePage(
                topicId = state.topic.id,
                topicQuestionCount = state.topicQuestionCount,
                learningContext = state.learningContext,
                onStartTopicPractice = onStartTopicPractice,
                onPracticePreset = onPracticePreset,
                scrollState = practiceScrollState,
                modifier = Modifier.fillMaxSize(),
            )

            TopicDetailTab.Subtopics -> TopicSubtopicsPage(
                subtopics = state.subtopics,
                onStartSubtopicPractice = onStartSubtopicPractice,
                onPracticePreset = onPracticePreset,
                listState = subtopicsListState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

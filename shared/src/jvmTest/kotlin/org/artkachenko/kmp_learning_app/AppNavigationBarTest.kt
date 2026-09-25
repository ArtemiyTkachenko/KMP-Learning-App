package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserItemUiModel
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserScreen
import org.artkachenko.kmp_learning_app.topic_study.topics.TopicBrowserUiState
import org.artkachenko.kmp_learning_app.ui.LocalAppSnackbarHostState
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class AppNavigationBarTest {
    @Test
    fun compactBarOffersEveryAreaWithSelectionAndTouchTargets() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { }
                }
            }
        }

        AppTopLevelDestination.entries.forEach { destination ->
            val item = onNodeWithTag(appNavigationBarItemTag(destination)).assertIsDisplayed()
            assertTrue(
                item.fetchSemanticsNode().boundsInRoot.height >= MinimumTouchTargetPx,
                "$destination did not keep a 48dp touch target.",
            )
        }
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS)).assertIsSelected()

        val bounds = onNodeWithTag(AppNavigationBarTag).fetchSemanticsNode().boundsInRoot
        assertEquals(AppLayout.CompactNavigationHeight.value, bounds.height)
        assertEquals(16f, bounds.left)
        assertEquals(384f, bounds.right)
    }

    @Test
    fun compactDestinationsKeepEqualCentredSlotsAtPhoneWidths() = runComposeUiTest {
        var width by mutableStateOf(CompactWidths.first().dp)
        setContent {
            AppTheme {
                Box(Modifier.size(width, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { }
                }
            }
        }

        CompactWidths.forEach { compactWidth ->
            runOnIdle { width = compactWidth.dp }
            waitForIdle()

            val itemBounds = AppTopLevelDestination.entries.associateWith {
                onNodeWithTag(appNavigationBarItemTag(it)).fetchSemanticsNode().boundsInRoot
            }
            val firstWidth = itemBounds.getValue(AppTopLevelDestination.TOPICS).width
            itemBounds.forEach { (destination, bounds) ->
                assertEquals(firstWidth, bounds.width, LayoutTolerancePx, "$destination slot width")
                val iconBounds = onNodeWithTag(
                    appNavigationBarIconTag(destination),
                    useUnmergedTree = true,
                ).fetchSemanticsNode().boundsInRoot
                val labelBounds = navigationLabelBounds(destination)
                assertEquals(bounds.center.x, iconBounds.center.x, LayoutTolerancePx)
                assertEquals(iconBounds.center.x, labelBounds.center.x, LayoutTolerancePx)
                onNodeWithText(destination.labelText, useUnmergedTree = true).assertIsDisplayed()
            }
        }
    }

    @Test
    fun selectionWrapsIconAndLabelWithoutChangingDestinationGeometry() = runComposeUiTest {
        var selected by mutableStateOf(AppTopLevelDestination.TOPICS)
        setContent {
            AppTheme {
                Box(Modifier.size(390.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = selected,
                        onSelect = {},
                        showsNavigation = true,
                    ) { }
                }
            }
        }

        val itemBoundsBefore = destinationBounds()
        val iconBoundsBefore = destinationIconBounds()
        val labelBoundsBefore = destinationLabelBounds()
        assertSelectedIndicatorWraps(AppTopLevelDestination.TOPICS)

        runOnIdle { selected = AppTopLevelDestination.INTERVIEW }
        waitForIdle()

        assertEquals(itemBoundsBefore, destinationBounds())
        assertEquals(iconBoundsBefore, destinationIconBounds())
        assertEquals(labelBoundsBefore, destinationLabelBounds())
        assertSelectedIndicatorWraps(AppTopLevelDestination.INTERVIEW)
    }

    /**
     * The pill is one travelling indicator rather than a background each destination owns, so the
     * behaviour worth protecting is that there is never more than one of it and that it ends up over
     * whichever destination is selected — including a jump across the bar rather than to a
     * neighbour, and including a return to the first slot.
     */
    @Test
    fun oneSelectedIndicatorSettlesOverWhicheverDestinationIsSelected() = runComposeUiTest {
        var selected by mutableStateOf(AppTopLevelDestination.TOPICS)
        setContent {
            AppTheme {
                Box(Modifier.size(390.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = selected,
                        onSelect = {},
                        showsNavigation = true,
                    ) { }
                }
            }
        }

        val visited = listOf(
            AppTopLevelDestination.MISTAKES,
            AppTopLevelDestination.PROGRESS,
            AppTopLevelDestination.TOPICS,
        )
        visited.forEach { destination ->
            runOnIdle { selected = destination }
            waitForIdle()

            assertSelectedIndicatorWraps(destination)
            val indicator = selectedIndicatorBounds()
            val item = onNodeWithTag(appNavigationBarItemTag(destination))
                .fetchSemanticsNode().boundsInRoot
            assertEquals(
                item.center.x,
                indicator.center.x,
                LayoutTolerancePx,
                "the indicator settled away from the $destination slot",
            )
        }
    }

    @Test
    fun mistakesBadgeDoesNotMoveOrResizeItsIcon() = runComposeUiTest {
        var badges by mutableStateOf<AppNavigationBadges>(emptyMap())
        setContent {
            AppTheme {
                AppNavigationBar(
                    selected = AppTopLevelDestination.TOPICS,
                    onSelect = {},
                    badges = badges,
                    modifier = Modifier.size(360.dp, AppLayout.CompactNavigationHeight),
                )
            }
        }

        val iconWithoutBadge = navigationIconBounds(AppTopLevelDestination.MISTAKES)
        runOnIdle { badges = mapOf(AppTopLevelDestination.MISTAKES to 7) }
        waitForIdle()

        assertEquals(iconWithoutBadge, navigationIconBounds(AppTopLevelDestination.MISTAKES))
        onNodeWithText("7", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun compactVisibilityAnimationDoesNotResizeContent() = runComposeUiTest {
        var visible by mutableStateOf(true)
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                        showsBottomNavigation = visible,
                    ) { padding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .testTag(ScaffoldContentTag),
                        )
                    }
                }
            }
        }

        val visibleBounds = contentBounds()
        runOnIdle { visible = false }
        waitForIdle()

        onNodeWithTag(AppNavigationBarTag).assertDoesNotExist()
        assertEquals(visibleBounds, contentBounds())
    }

    @Test
    fun routeDrivenVisibilityUsesTheSameSettledCompactTransition() = runComposeUiTest {
        var ownsNavigation by mutableStateOf(true)
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = ownsNavigation,
                    ) { Box(Modifier.fillMaxSize()) }
                }
            }
        }

        onNodeWithTag(AppNavigationBarTag).assertIsDisplayed()
        runOnIdle { ownsNavigation = false }
        waitForIdle()
        onNodeWithTag(AppNavigationBarTag).assertDoesNotExist()
    }

    @Test
    fun scrollingViewportExtendsBehindOverlayAndFinalRowClearsIt() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 700.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            TopicBrowserScreen(
                                state = TopicBrowserUiState.Content(
                                    topics = List(20) { index ->
                                        TopicBrowserItemUiModel(
                                            topicId = "topic_$index",
                                            topicName = "Topic $index",
                                        )
                                    },
                                ),
                                onTopicClick = {},
                                onRetry = {},
                                topWindowInsets = WindowInsets(0, 0, 0, 0),
                            )
                        }
                    }
                }
            }
        }

        val navigationTop = onNodeWithTag(AppNavigationBarTag)
            .fetchSemanticsNode().boundsInRoot.top
        val viewportBottom = onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(
            viewportBottom > navigationTop,
            "the scroll viewport ended before the floating navigation overlay.",
        )

        onNode(hasScrollAction()).performScrollToNode(hasText("Topic 19"))
        waitForIdle()

        val lastRowBottom = onNodeWithText("Topic 19").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(
            lastRowBottom < navigationTop,
            "the last row ended at $lastRowBottom, below navigation at $navigationTop.",
        )
    }

    @Test
    fun snackbarClearsVisibleCompactNavigation() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) {
                        val hostState = requireNotNull(LocalAppSnackbarHostState.current)
                        LaunchedEffect(hostState) { hostState.showSnackbar(SnackbarMessage) }
                    }
                }
            }
        }

        onNodeWithText(SnackbarMessage).assertIsDisplayed()
        val snackbarBottom = onNodeWithText(SnackbarMessage).fetchSemanticsNode().boundsInRoot.bottom
        val navigationTop = onNodeWithTag(AppNavigationBarTag)
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue(snackbarBottom < navigationTop)
    }

    @Test
    fun navigationSelectionAndBadgesRemainInteractive() = runComposeUiTest {
        val selected = mutableListOf<AppTopLevelDestination>()
        setContent {
            AppTheme {
                AppNavigationBar(
                    selected = AppTopLevelDestination.TOPICS,
                    onSelect = selected::add,
                    badges = mapOf(AppTopLevelDestination.MISTAKES to 7),
                )
            }
        }

        onNodeWithText("7", useUnmergedTree = true).assertIsDisplayed()
        AppTopLevelDestination.entries.forEach {
            onNodeWithTag(appNavigationBarItemTag(it)).performClick()
        }
        assertEquals(AppTopLevelDestination.entries.toList(), selected)
    }

    @Test
    fun bottomVisibilityRequestsDoNotAffectTheWideRail() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(AppNavigationRailBreakpoint, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                        showsBottomNavigation = false,
                    ) { Box(Modifier.testTag(ScaffoldContentTag)) }
                }
            }
        }

        onNodeWithTag(AppNavigationRailDividerTag).assertIsDisplayed()
        onNodeWithTag(AppNavigationBarTag).assertDoesNotExist()
        val content = onNodeWithTag(ScaffoldContentTag).fetchSemanticsNode().positionInRoot
        val railItem = onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS))
            .fetchSemanticsNode().positionInRoot
        assertTrue(content.x > railItem.x)
    }

    @Test
    fun focusModeRendersNeitherCompactNavigationNorRail() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = false,
                    ) { Box(Modifier.fillMaxSize().testTag(ScaffoldContentTag)) }
                }
            }
        }

        onNodeWithTag(AppNavigationBarTag).assertDoesNotExist()
        onNodeWithTag(AppNavigationRailDividerTag).assertDoesNotExist()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.contentBounds(): Rect =
        onNodeWithTag(ScaffoldContentTag).fetchSemanticsNode().boundsInRoot

    private fun androidx.compose.ui.test.ComposeUiTest.destinationBounds():
        Map<AppTopLevelDestination, Rect> = AppTopLevelDestination.entries.associateWith {
            onNodeWithTag(appNavigationBarItemTag(it)).fetchSemanticsNode().boundsInRoot
        }

    private fun androidx.compose.ui.test.ComposeUiTest.destinationIconBounds():
        Map<AppTopLevelDestination, Rect> = AppTopLevelDestination.entries.associateWith {
            navigationIconBounds(it)
        }

    private fun androidx.compose.ui.test.ComposeUiTest.destinationLabelBounds():
        Map<AppTopLevelDestination, Rect> = AppTopLevelDestination.entries.associateWith {
            navigationLabelBounds(it)
        }

    private fun androidx.compose.ui.test.ComposeUiTest.navigationIconBounds(
        destination: AppTopLevelDestination,
    ): Rect = onNodeWithTag(
        appNavigationBarIconTag(destination),
        useUnmergedTree = true,
    ).fetchSemanticsNode().boundsInRoot

    private fun androidx.compose.ui.test.ComposeUiTest.navigationLabelBounds(
        destination: AppTopLevelDestination,
    ): Rect = onNodeWithText(
        destination.labelText,
        useUnmergedTree = true,
    ).fetchSemanticsNode().boundsInRoot

    private fun androidx.compose.ui.test.ComposeUiTest.selectedIndicatorBounds(): Rect {
        val indicators = onAllNodesWithTag(
            AppNavigationSelectedIndicatorTag,
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertEquals(1, indicators.size, "a compact bar draws exactly one selection indicator")
        return indicators.single().boundsInRoot
    }

    private fun androidx.compose.ui.test.ComposeUiTest.assertSelectedIndicatorWraps(
        destination: AppTopLevelDestination,
    ) {
        val pill = selectedIndicatorBounds()
        val icon = navigationIconBounds(destination)
        val label = navigationLabelBounds(destination)

        assertTrue(pill.left <= icon.left && pill.right >= icon.right)
        assertTrue(pill.top <= icon.top && pill.bottom >= icon.bottom)
        assertTrue(pill.left <= label.left && pill.right >= label.right)
        assertTrue(pill.top <= label.top && pill.bottom >= label.bottom)
        assertFalse(pill == icon, "the selected treatment must not be the icon-only indicator")
    }
}

private val AppTopLevelDestination.labelText: String
    get() = when (this) {
        AppTopLevelDestination.TOPICS -> "Learn"
        AppTopLevelDestination.INTERVIEW -> "Interview"
        AppTopLevelDestination.PROGRESS -> "Progress"
        AppTopLevelDestination.MISTAKES -> "Mistakes"
    }

private const val ScaffoldContentTag = "scaffold_content"
private const val SnackbarMessage = "Copied"
private const val MinimumTouchTargetPx = 48f
private const val LayoutTolerancePx = 1f
private val CompactWidths = listOf(360, 390, 412, 599)

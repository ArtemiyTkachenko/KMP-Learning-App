package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.topic_study.topics.browsingContent
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
        onNodeWithTag(AppNavigationBadgeTag, useUnmergedTree = true).assertIsDisplayed()
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
                                state = browsingContent(
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

        onNodeWithTag(AppNavigationBadgeTag, useUnmergedTree = true).assertIsDisplayed()
        // Drawn is not announced: the count must also reach the Mistakes target's merged node.
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.MISTAKES))
            .assert(hasText(SevenUnresolved))
        AppTopLevelDestination.entries.forEach {
            onNodeWithTag(appNavigationBarItemTag(it)).performClick()
        }
        assertEquals(AppTopLevelDestination.entries.toList(), selected)
    }

    /**
     * The drawn count stops growing at two digits so a long queue cannot crowd the 24dp glyph. The
     * boundary is the behaviour: 99 is still exact, 100 is the first count drawn as "99+".
     */
    @Test
    fun badgeDrawsExactCountsUpToNinetyNineAndCapsAbove() = runComposeUiTest {
        val labels = mutableMapOf<Int, String>()
        setContent {
            listOf(1, 99, 100, 120).forEach { labels[it] = navigationBadgeLabel(it) }
        }
        waitForIdle()

        assertEquals(mapOf(1 to "1", 99 to "99", 100 to "99+", 120 to "99+"), labels)
    }

    /**
     * The icon box is 24dp, and measured inside it "99+" was clipped to "99" and pushed back across
     * the glyph. A wider count must keep the single-digit badge's leading edge and grow past it.
     */
    @Test
    fun aCappedBadgeKeepsItsLeadingEdgeAndIsNotSqueezedToTheIcon() = runComposeUiTest {
        var badges by mutableStateOf(mapOf(AppTopLevelDestination.MISTAKES to 7))
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

        val singleDigit = badgeBounds()
        runOnIdle { badges = mapOf(AppTopLevelDestination.MISTAKES to 120) }
        waitForIdle()
        val capped = badgeBounds()

        assertEquals(singleDigit.left, capped.left, LayoutTolerancePx)
        assertEquals(singleDigit.top, capped.top, LayoutTolerancePx)
        assertTrue(
            capped.width > navigationIconBounds(AppTopLevelDestination.MISTAKES).width,
            "the 99+ badge was squeezed to the icon's width.",
        )
    }

    @Test
    fun anEmptyQueueDrawsNoBadgeAndAnnouncesNoCount() = runComposeUiTest {
        setContent {
            AppTheme {
                AppNavigationBar(
                    selected = AppTopLevelDestination.TOPICS,
                    onSelect = {},
                    badges = mapOf(AppTopLevelDestination.MISTAKES to 0),
                )
            }
        }

        onNodeWithTag(AppNavigationBadgeTag, useUnmergedTree = true).assertDoesNotExist()
        onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.MISTAKES))
            .assert(hasText("unresolved", substring = true).not())
    }

    /**
     * The visible cap is a drawing concern only. A screen reader hears the real count in words —
     * "120 unresolved mistakes", never "99+" or a bare number — on the compact bar and on
     * the rail, and the singular form is used for one.
     */
    @Test
    fun theMistakesItemAnnouncesItsRealCountInWords() = runComposeUiTest {
        var badges by mutableStateOf(mapOf(AppTopLevelDestination.MISTAKES to 120))
        var width by mutableStateOf(400.dp)
        setContent {
            AppTheme {
                Box(Modifier.size(width, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                        badges = badges,
                    ) { }
                }
            }
        }

        listOf(400.dp, AppNavigationRailBreakpoint).forEach { navigationWidth ->
            runOnIdle {
                width = navigationWidth
                badges = mapOf(AppTopLevelDestination.MISTAKES to 120)
            }
            waitForIdle()
            val mistakes = onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.MISTAKES))
            mistakes.assert(hasText("120 unresolved mistakes"))
            mistakes.assert(hasText("99+", substring = true).not())
            mistakes.assert(hasText("120").not())

            runOnIdle { badges = mapOf(AppTopLevelDestination.MISTAKES to 1) }
            waitForIdle()
            mistakes.assert(hasText("1 unresolved mistake"))
        }
    }

    /**
     * The rail is a second implementation of the same navigation, not a restyled bar: it is built
     * from Material's `NavigationRailItem` and its own badge icon, so nothing above covers it.
     * What must match is meaning rather than appearance — every destination, the one selection,
     * the badge on the item it counts, and each target reporting its own destination.
     */
    @Test
    fun theWideRailOffersTheSameDestinationsSelectionBadgeAndCallbacks() = runComposeUiTest {
        val selected = mutableListOf<AppTopLevelDestination>()
        setContent {
            AppTheme {
                Box(Modifier.size(AppNavigationRailBreakpoint, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.PROGRESS,
                        onSelect = selected::add,
                        showsNavigation = true,
                        badges = mapOf(AppTopLevelDestination.MISTAKES to 7),
                    ) { }
                }
            }
        }

        onNodeWithTag(AppNavigationRailDividerTag).assertIsDisplayed()
        onNodeWithTag(AppNavigationBarTag).assertDoesNotExist()
        AppTopLevelDestination.entries.forEach { destination ->
            val item = onNodeWithTag(appNavigationBarItemTag(destination)).assertIsDisplayed()
            if (destination == AppTopLevelDestination.PROGRESS) {
                item.assertIsSelected()
            } else {
                item.assertIsNotSelected()
            }
            // The count belongs to the Mistakes target in the merged tree, which is what a
            // screen reader announces with it, and to no other. Material's rail item clears its
            // icon's semantics, so a drawn badge alone would not satisfy this.
            if (destination == AppTopLevelDestination.MISTAKES) {
                item.assert(hasText(SevenUnresolved))
            } else {
                item.assert(hasText(SevenUnresolved).not())
            }
            item.performClick()
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

    private fun androidx.compose.ui.test.ComposeUiTest.badgeBounds(): Rect =
        onNodeWithTag(AppNavigationBadgeTag, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

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

    @Test
    fun compactContainerHidesContentBehindItInLightTheme() = runComposeUiTest {
        assertContainerIsOpaqueOverContent(darkTheme = false)
    }

    @Test
    fun compactContainerHidesContentBehindItInDarkTheme() = runComposeUiTest {
        assertContainerIsOpaqueOverContent(darkTheme = true)
    }

    /**
     * Renders the bar over a backdrop no theme colour is near and reads back a pixel from the
     * container's empty top padding, between destinations. An opaque container paints exactly
     * `surfaceContainer` there; any translucency mixes the backdrop in and moves the pixel by
     * several steps per channel, which is the bleed-through list text showed on the dark theme.
     */
    private fun androidx.compose.ui.test.ComposeUiTest.assertContainerIsOpaqueOverContent(
        darkTheme: Boolean,
    ) {
        var container = Color.Unspecified
        setContent {
            AppTheme(darkTheme = darkTheme) {
                container = MaterialTheme.colorScheme.surfaceContainer
                Box(Modifier.size(400.dp, 200.dp).background(OpacityBackdrop)) {
                    AppNavigationBar(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                    )
                }
            }
        }

        val bar = onNodeWithTag(AppNavigationBarTag).fetchSemanticsNode().boundsInRoot
        val pixels = onRoot().captureToImage().toPixelMap()
        val probe = pixels[bar.center.x.roundToInt(), (bar.top + EmptyContainerProbeInsetPx).roundToInt()]

        listOf(
            "red" to (probe.red to container.red),
            "green" to (probe.green to container.green),
            "blue" to (probe.blue to container.blue),
        ).forEach { (channel, values) ->
            val (drawn, expected) = values
            assertTrue(
                abs(drawn - expected) <= ColorChannelTolerance,
                "dark=$darkTheme: the container's $channel was $drawn, not surfaceContainer's " +
                    "$expected, so the backdrop showed through.",
            )
        }
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

private const val SevenUnresolved = "7 unresolved mistakes"
private const val ScaffoldContentTag = "scaffold_content"
private const val SnackbarMessage = "Copied"
private const val MinimumTouchTargetPx = 48f
private const val LayoutTolerancePx = 1f
private val CompactWidths = listOf(360, 390, 412, 599)

/** Far from every neutral container in both themes, so any bleed-through moves the probe. */
private val OpacityBackdrop = Color.Magenta

/** Inside the container's 4dp top content padding: no pill, icon, or label is drawn there. */
private const val EmptyContainerProbeInsetPx = 2f

/** One 8-bit step either way, for rounding between the colour space and the captured pixel. */
private const val ColorChannelTolerance = 1f / 255f

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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
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
}

private const val ScaffoldContentTag = "scaffold_content"
private const val SnackbarMessage = "Copied"
private const val MinimumTouchTargetPx = 48f

package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.ui.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
internal class AppNavigationBarTest {
    @Test
    fun everyAreaIsReachableFromTheBar() = runComposeUiTest {
        setContent {
            AppTheme {
                AppNavigationBar(selected = AppTopLevelDestination.TOPICS, onSelect = {})
            }
        }

        onNodeWithText("Topics").assertIsDisplayed()
        onNodeWithText("Interview").assertIsDisplayed()
        onNodeWithText("Progress").assertIsDisplayed()
        onNodeWithText("Mistakes").assertIsDisplayed()
    }

    @Test
    fun shellOwnsTheBottomInsetAndLeavesTheTopToTheScreen() = runComposeUiTest {
        // Each system inset must have exactly one owner. The shell deliberately excludes the top
        // from its content insets so a screen without an AppTopBar can pad for the status bar
        // itself, and it reports the whole bottom so no screen adds its own above the bar.
        var contentPadding: PaddingValues? = null
        setContent {
            AppTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { padding ->
                        contentPadding = padding
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        val padding = requireNotNull(contentPadding)
        assertEquals(0.dp, padding.calculateTopPadding())
        assertTrue(
            padding.calculateBottomPadding() > 0.dp,
            "the shell must reserve the navigation bar so screens do not",
        )
    }

    @Test
    fun aRailLayoutReservesNoBottomNavigationSpace() = runComposeUiTest {
        // Beside a rail there is no bottom bar to clear, so the content must run to the window
        // edge. This host reports no system insets, so the whole bottom padding should be zero.
        var contentPadding: PaddingValues? = null
        setContent {
            AppTheme {
                Box(Modifier.size(AppNavigationRailBreakpoint, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { padding ->
                        contentPadding = padding
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        val padding = requireNotNull(contentPadding)
        assertEquals(0.dp, padding.calculateTopPadding())
        assertEquals(0.dp, padding.calculateBottomPadding())
    }

    @Test
    fun selectingAnAreaEmitsThatDestination() = runComposeUiTest {
        val selected = mutableListOf<AppTopLevelDestination>()
        setContent {
            AppTheme {
                AppNavigationBar(selected = AppTopLevelDestination.TOPICS, onSelect = selected::add)
            }
        }

        AppTopLevelDestination.entries.forEach {
            onNodeWithTag(appNavigationBarItemTag(it)).performClick()
        }

        assertEquals(AppTopLevelDestination.entries.toList(), selected)
    }

    @Test
    fun eachAreaRouteMapsBackToItsDestination() {
        AppTopLevelDestination.entries.forEach { destination ->
            assertEquals(destination, AppTopLevelDestination.forRoute(destination.route))
        }
    }

    @Test
    fun detailRoutesBelongToNoAreaSoNoItemLooksSelectedOnThem() {
        // Whether the control is shown is decided by showsAreaNavigation; forRoute only answers
        // which item is highlighted, and on a detail screen that is none of them.
        listOf(
            AppRoute.Topic("topic"),
            AppRoute.ProgressTopic("topic"),
            AppRoute.MixedInterviewResult("attempt"),
            AppRoute.FocusedPracticeResult("attempt"),
            AppRoute.MixedInterviewAttempt("attempt"),
        ).forEach { route ->
            assertEquals(null, AppTopLevelDestination.forRoute(route))
        }
    }

    @Test
    fun theMistakesItemCarriesTheUnresolvedCount() = runComposeUiTest {
        setContent {
            AppTheme {
                AppNavigationBar(
                    selected = AppTopLevelDestination.TOPICS,
                    onSelect = {},
                    badges = mapOf(AppTopLevelDestination.MISTAKES to 7),
                )
            }
        }

        onNodeWithText("7", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun anEmptyQueuePutsNoBadgeOnTheBar() = runComposeUiTest {
        setContent {
            AppTheme {
                AppNavigationBar(
                    selected = AppTopLevelDestination.TOPICS,
                    onSelect = {},
                    badges = mapOf(AppTopLevelDestination.MISTAKES to 0),
                )
            }
        }

        onNodeWithText("0", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theRailOffersTheSameAreasAndBadgesAsTheBar() = runComposeUiTest {
        val selected = mutableListOf<AppTopLevelDestination>()
        setContent {
            AppTheme {
                AppNavigationRail(
                    selected = AppTopLevelDestination.TOPICS,
                    onSelect = selected::add,
                    badges = mapOf(AppTopLevelDestination.MISTAKES to 2),
                )
            }
        }

        onNodeWithText("2", useUnmergedTree = true).assertIsDisplayed()
        AppTopLevelDestination.entries.forEach {
            onNodeWithTag(appNavigationBarItemTag(it)).performClick()
        }

        assertEquals(AppTopLevelDestination.entries.toList(), selected)
    }

    @Test
    fun aPhoneShapedWindowPutsNavigationAlongTheBottom() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(AppNavigationRailBreakpoint - 1.dp, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { Box(Modifier.testTag(ScaffoldContentTag)) }
                }
            }
        }

        // A bar takes height from the bottom and leaves the content against the leading edge; a
        // rail would take width instead and push the content across.
        val content = onNodeWithTag(ScaffoldContentTag).fetchSemanticsNode().positionInRoot
        val topics = onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS))
            .fetchSemanticsNode().positionInRoot
        assertEquals(0f, content.x, "content should not be pushed across by a rail")
        assertTrue(topics.y > content.y, "navigation should be below the content")
    }

    @Test
    fun aWindowWideEnoughForARailPutsNavigationBesideTheContent() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(AppNavigationRailBreakpoint, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = true,
                    ) { Box(Modifier.testTag(ScaffoldContentTag)) }
                }
            }
        }

        val content = onNodeWithTag(ScaffoldContentTag).fetchSemanticsNode().positionInRoot
        val topics = onNodeWithTag(appNavigationBarItemTag(AppTopLevelDestination.TOPICS))
            .fetchSemanticsNode().positionInRoot
        assertTrue(content.x > topics.x, "content should start after the rail")
    }

    @Test
    fun anImmersiveScreenGetsTheWholeWindow() = runComposeUiTest {
        setContent {
            AppTheme {
                Box(Modifier.size(AppNavigationRailBreakpoint, 800.dp)) {
                    AppNavigationScaffold(
                        selected = AppTopLevelDestination.TOPICS,
                        onSelect = {},
                        showsNavigation = false,
                    ) { Box(Modifier.testTag(ScaffoldContentTag)) }
                }
            }
        }

        AppTopLevelDestination.entries.forEach {
            onNodeWithTag(appNavigationBarItemTag(it)).assertDoesNotExist()
        }
        assertEquals(
            0f,
            onNodeWithTag(ScaffoldContentTag).fetchSemanticsNode().positionInRoot.x,
        )
    }
}

private const val ScaffoldContentTag = "scaffold_content"
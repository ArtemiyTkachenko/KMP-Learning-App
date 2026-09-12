package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.LocalAppSnackbarHostState
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass
import org.jetbrains.compose.resources.stringResource

/** Counts worth surfacing on a navigation item; absent or zero renders no badge. */
internal typealias AppNavigationBadges = Map<AppTopLevelDestination, Int>

@Composable
private fun DestinationIcon(destination: AppTopLevelDestination, badges: AppNavigationBadges) {
    val count = badges[destination] ?: 0
    if (count <= 0) {
        Icon(destination.icon, contentDescription = null)
        return
    }
    BadgedBox(badge = { Badge { Text(count.toString()) } }) {
        Icon(destination.icon, contentDescription = null)
    }
}

internal fun appNavigationBarItemTag(destination: AppTopLevelDestination): String =
    "app_nav_${destination.name.lowercase()}"

/**
 * Tags for the rules that mark where area navigation ends and the page begins.
 *
 * Both layouts carry one so the boundary reads the same way whichever the window gets, but they
 * start from different places. The rail has no edge at all: `NavigationRail` paints `surface` and
 * the `Scaffold` beside it paints `background`, and this app's scheme gives those the same value
 * in both themes, so without a rule the items simply float in the content. The bottom bar does
 * have an edge — it paints `surfaceContainer` — but the step from the page is about 12/255 per
 * channel, slight enough to miss. A rule states the boundary in both cases rather than leaving it
 * to a tonal difference that is either absent or nearly so.
 */
internal const val AppNavigationRailDividerTag = "app_nav_rail_divider"
internal const val AppNavigationBarDividerTag = "app_nav_bar_divider"

/**
 * Below this a window is phone-shaped and navigation sits along the bottom edge; at or above it
 * there is room for a rail beside the content. This is the Material compact/medium boundary, and
 * it is a window measurement rather than a platform check because the same host can be either
 * size — a desktop or browser window can be dragged narrow.
 *
 * The value lives in [AppLayout] because the content margin turns on the same boundary; this name
 * is kept so the navigation call sites and their tests still read in terms of the rail.
 */
internal val AppNavigationRailBreakpoint: Dp = AppLayout.CompactWidthBreakpoint

private val RailHeaderHeight: Dp = 12.dp

@Composable
internal fun AppNavigationBar(
    selected: AppTopLevelDestination,
    onSelect: (AppTopLevelDestination) -> Unit,
    badges: AppNavigationBadges = emptyMap(),
) {
    NavigationBar {
        AppTopLevelDestination.entries.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, badges) },
                label = { Text(label) },
                modifier = Modifier.testTag(appNavigationBarItemTag(destination)),
            )
        }
    }
}

@Composable
internal fun AppNavigationRail(
    selected: AppTopLevelDestination,
    onSelect: (AppTopLevelDestination) -> Unit,
    badges: AppNavigationBadges = emptyMap(),
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        // Without a header the first item sits hard against the window's top edge on desktop and
        // web, where the rail gets no system insets of its own.
        header = { Spacer(Modifier.height(RailHeaderHeight)) },
    ) {
        AppTopLevelDestination.entries.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationRailItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, badges) },
                label = { Text(label) },
                modifier = Modifier.testTag(appNavigationBarItemTag(destination)),
            )
        }
    }
}

/**
 * Places the content beside or above the area navigation, whichever the window has room for.
 *
 * Below [AppNavigationRailBreakpoint] the window is phone-shaped and navigation sits along the
 * bottom edge within thumb reach; at or above it a rail runs down the leading edge instead, so a
 * desktop or browser window no longer stretches four items across its full width. The decision is
 * made from the measured width rather than the platform, because the same host can be either size.
 */
@Composable
internal fun AppNavigationScaffold(
    selected: AppTopLevelDestination,
    onSelect: (AppTopLevelDestination) -> Unit,
    showsNavigation: Boolean,
    modifier: Modifier = Modifier,
    badges: AppNavigationBadges = emptyMap(),
    content: @Composable (PaddingValues) -> Unit,
) {
    // One host for the whole shell, placed here because this is the only composable that owns a
    // Scaffold; see LocalAppSnackbarHostState for why it is not left to each screen.
    val snackbarHostState = remember { SnackbarHostState() }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val windowSizeClass = AppLayout.windowSizeClassFor(maxWidth)
        val usesRail = windowSizeClass.isAtLeastMedium
        // This is already the one place that measures the window, so it is also where the content
        // margin is decided; screens read it from the composition local rather than each deciding
        // for itself or re-measuring.
        val contentMargin = AppLayout.screenHorizontalMargin(maxWidth)
        Row(Modifier.fillMaxSize()) {
            if (showsNavigation && usesRail) {
                AppNavigationRail(selected = selected, onSelect = onSelect, badges = badges)
                // Inside the same condition as the rail, so the rule cannot outlive what it marks.
                VerticalDivider(
                    modifier = Modifier.testTag(AppNavigationRailDividerTag),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                // The window's top edge is deliberately left out of the content padding: screens
                // start with their own TopAppBar, which pads for the status bar and paints its
                // container behind it. Padding the content here as well would push every bar a
                // status bar's height down the screen. Bottom and horizontal insets stay, so a
                // screen shown without the navigation bar still clears the gesture bar and a
                // landscape display cutout.
                contentWindowInsets = WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                // Inside the Scaffold rather than over the whole window, so a message clears the
                // navigation bar instead of covering it.
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showsNavigation && !usesRail) {
                        // Above `AppNavigationBar` rather than within it: the bar pads itself for
                        // the gesture inset, and a rule inside that padding would sit below the
                        // edge it is meant to draw rather than on it.
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.testTag(AppNavigationBarDividerTag),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            AppNavigationBar(
                                selected = selected,
                                onSelect = onSelect,
                                badges = badges,
                            )
                        }
                    }
                },
            ) { scaffoldPadding ->
                CompositionLocalProvider(
                    LocalAppContentMargin provides contentMargin,
                    LocalAppWindowSizeClass provides windowSizeClass,
                    LocalAppSnackbarHostState provides snackbarHostState,
                ) {
                    // No width cap here. The shell used to centre everything below it inside a
                    // single 840dp box, which stopped a phone layout stretching across a desktop
                    // window but could not tell a Lesson from a dashboard, and capped each
                    // screen's own `TopAppBar` along with its content — so on a wide window the
                    // bar stopped short of both window edges and floated in the middle of the
                    // page. Each screen now states which measure its content wants, with
                    // `AppContentPane`; the shell states the window class and the margin, which
                    // are facts about the window rather than about what is in it.
                    content(scaffoldPadding)
                }
            }
        }
    }
}

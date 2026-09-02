package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
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
    BoxWithConstraints(modifier.fillMaxSize()) {
        val usesRail = maxWidth >= AppNavigationRailBreakpoint
        // This is already the one place that measures the window, so it is also where the content
        // margin is decided; screens read it from the composition local rather than each deciding
        // for itself or re-measuring.
        val contentMargin = AppLayout.screenHorizontalMargin(maxWidth)
        Row(Modifier.fillMaxSize()) {
            if (showsNavigation && usesRail) {
                AppNavigationRail(selected = selected, onSelect = onSelect, badges = badges)
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
                bottomBar = {
                    if (showsNavigation && !usesRail) {
                        AppNavigationBar(
                            selected = selected,
                            onSelect = onSelect,
                            badges = badges,
                        )
                    }
                },
            ) { scaffoldPadding ->
                CompositionLocalProvider(LocalAppContentMargin provides contentMargin) {
                    // Wide windows stop the layout growing with them. A phone layout stretched
                    // across a desktop window puts a Topic name against the far left edge and its
                    // accuracy figure against the far right, with a foot of empty card between —
                    // readable on a phone, unreadable at 1600px. Centred rather than leading-aligned
                    // so a window between the breakpoint and the cap does not appear off-balance.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(Modifier.widthIn(max = AppLayout.MaxContentWidth).fillMaxSize()) {
                            content(scaffoldPadding)
                        }
                    }
                }
            }
        }
    }
}

package org.artkachenko.kmp_learning_app

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 */
internal val AppNavigationRailBreakpoint: Dp = 600.dp

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
        Row(Modifier.fillMaxSize()) {
            if (showsNavigation && usesRail) {
                AppNavigationRail(selected = selected, onSelect = onSelect, badges = badges)
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showsNavigation && !usesRail) {
                        AppNavigationBar(
                            selected = selected,
                            onSelect = onSelect,
                            badges = badges,
                        )
                    }
                },
                content = content,
            )
        }
    }
}

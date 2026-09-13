package org.artkachenko.kmp_learning_app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.LocalAppSnackbarHostState
import org.artkachenko.kmp_learning_app.ui.theme.AppLayout
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppNavigationOverlayInfo
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppNavigationOverlay
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass
import org.jetbrains.compose.resources.stringResource

/** Counts worth surfacing on a navigation item; absent or zero renders no badge. */
internal typealias AppNavigationBadges = Map<AppTopLevelDestination, Int>

@Composable
private fun DestinationIcon(
    destination: AppTopLevelDestination,
    badges: AppNavigationBadges,
    modifier: Modifier = Modifier,
) {
    val count = badges[destination] ?: 0
    if (count <= 0) {
        Icon(destination.icon, contentDescription = null, modifier = modifier)
        return
    }
    BadgedBox(badge = { Badge { Text(count.toString()) } }) {
        Icon(destination.icon, contentDescription = null, modifier = modifier)
    }
}

internal fun appNavigationBarItemTag(destination: AppTopLevelDestination): String =
    "app_nav_${destination.name.lowercase()}"

/** The rail needs a rule because its surface and the adjacent page share the same theme colour. */
internal const val AppNavigationRailDividerTag = "app_nav_rail_divider"

/** The compact floating container, used for layout assertions rather than item interaction. */
internal const val AppNavigationBarTag = "app_navigation_bar"

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
    modifier: Modifier = Modifier,
    badges: AppNavigationBadges = emptyMap(),
) {
    Surface(
        modifier = modifier
            .height(AppLayout.CompactNavigationHeight)
            .testTag(AppNavigationBarTag),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = NavigationContainerAlpha),
        tonalElevation = NavigationContainerElevation,
        shadowElevation = NavigationContainerElevation,
    ) {
        Row(Modifier.fillMaxSize().selectableGroup()) {
            AppTopLevelDestination.entries.forEach { destination ->
                val label = stringResource(destination.label)
                NavigationBarItem(
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    icon = {
                        DestinationIcon(
                            destination = destination,
                            badges = badges,
                            modifier = Modifier.size(NavigationIconSize),
                        )
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag(appNavigationBarItemTag(destination)),
                )
            }
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
 * Below [AppNavigationRailBreakpoint] the window is phone-shaped and navigation floats over the
 * bottom of the content within thumb reach; at or above it a rail runs down the leading edge, so a
 * desktop or browser window no longer stretches four items across its full width. The decision is
 * made from the measured width rather than the platform, because the same host can be either size.
 */
@Composable
internal fun AppNavigationScaffold(
    selected: AppTopLevelDestination,
    onSelect: (AppTopLevelDestination) -> Unit,
    showsNavigation: Boolean,
    showsBottomNavigation: Boolean = showsNavigation,
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
        val ownsCompactNavigation = showsNavigation && !usesRail
        val compactNavigationVisible = ownsCompactNavigation && showsBottomNavigation
        val navigationOverlayInfo = AppNavigationOverlayInfo(
            clearance = if (ownsCompactNavigation) AppLayout.CompactNavigationClearance else 0.dp,
            isVisible = compactNavigationVisible,
        )
        val snackbarBottomPadding by animateDpAsState(
            targetValue = if (compactNavigationVisible) {
                AppLayout.CompactNavigationClearance
            } else {
                0.dp
            },
            animationSpec = AppMotion.effectSpec(),
        )
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
            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    // Screens own the top safe area through their app bars. Bottom and horizontal
                    // insets still have one owner, while compact navigation floats inside them.
                    contentWindowInsets = WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = snackbarBottomPadding),
                        )
                    },
                ) { scaffoldPadding ->
                    CompositionLocalProvider(
                        LocalAppContentMargin provides contentMargin,
                        LocalAppWindowSizeClass provides windowSizeClass,
                        LocalAppNavigationOverlay provides navigationOverlayInfo,
                        LocalAppSnackbarHostState provides snackbarHostState,
                    ) {
                        content(scaffoldPadding)
                    }
                }

                if (!usesRail) {
                    CompactNavigationOverlay(
                        visible = compactNavigationVisible,
                        selected = selected,
                        onSelect = onSelect,
                        badges = badges,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactNavigationOverlay(
    visible: Boolean,
    selected: AppTopLevelDestination,
    onSelect: (AppTopLevelDestination) -> Unit,
    badges: AppNavigationBadges,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = AppMotion.StateChangeDurationMillis,
                easing = AppMotion.EmphasizedDecelerateEasing,
            ),
            initialOffsetY = { it },
        ) + fadeIn(
            tween(
                durationMillis = AppMotion.StateChangeDurationMillis,
                easing = AppMotion.EmphasizedDecelerateEasing,
            ),
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = AppMotion.StateChangeDurationMillis,
                easing = AppMotion.EmphasizedAccelerateEasing,
            ),
            targetOffsetY = { it },
        ) + fadeOut(
            tween(
                durationMillis = AppMotion.StateChangeDurationMillis / 2,
                easing = AppMotion.EmphasizedAccelerateEasing,
            ),
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(
                    horizontal = AppSpacing.Comfortable,
                    vertical = AppLayout.CompactNavigationBottomGap,
                ),
        ) {
            AppNavigationBar(
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
                badges = badges,
            )
        }
    }
}

private const val NavigationContainerAlpha = 0.94f
private val NavigationContainerElevation: Dp = 3.dp
private val NavigationIconSize: Dp = 20.dp

package org.artkachenko.kmp_learning_app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
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

internal fun appNavigationBarIconTag(destination: AppTopLevelDestination): String =
    "app_nav_icon_${destination.name.lowercase()}"

/**
 * The travelling selection pill. There is exactly one of these in a compact bar, which is the point
 * of it: the tag is per-bar rather than per-destination because the indicator is no longer something
 * each destination owns a copy of.
 */
internal const val AppNavigationSelectedIndicatorTag = "app_nav_selected_indicator"

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
    val destinations = AppTopLevelDestination.entries
    Surface(
        modifier = modifier
            .height(AppLayout.CompactNavigationHeight)
            .testTag(AppNavigationBarTag),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = NavigationContainerAlpha),
        tonalElevation = NavigationContainerElevation,
        shadowElevation = NavigationContainerElevation,
    ) {
        // The pill is a sibling of the destination row rather than a background belonging to the
        // selected destination. Per-destination backgrounds meant a switch faded one pill out while
        // fading a second one in somewhere else, so nothing travelled and the selection had no
        // identity; with one indicator behind the row, Learn -> Progress is a single movement. Every
        // destination now draws only its icon and label, over whatever the indicator has painted.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = NavigationContentHorizontalPadding,
                    vertical = NavigationContentVerticalPadding,
                ),
        ) {
            SelectedDestinationIndicator(
                selectedSlot = destinations.indexOf(selected).coerceAtLeast(0),
                slotCount = destinations.size,
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
            ) {
                destinations.forEach { destination ->
                    CompactNavigationDestination(
                        destination = destination,
                        selected = destination == selected,
                        onClick = { onSelect(destination) },
                        badges = badges,
                    )
                }
            }
        }
    }
}

/**
 * The selected pill, drawn once behind the destination row and moved between slots.
 *
 * Every destination takes an equal `weight(1f)`, so a slot's geometry is arithmetic — width divided
 * by [slotCount] — and the indicator needs no measurement of the destinations, no reported
 * coordinates, and no state of its own beyond the selection it is handed. The animated slot index is
 * read inside [Modifier.layout], which makes it a layout-phase read: the pill re-places itself every
 * frame without recomposing a single destination.
 */
@Composable
private fun SelectedDestinationIndicator(selectedSlot: Int, slotCount: Int) {
    // Spatial, not effect: this is the one thing in the bar that actually travels. The spring
    // settles ahead of the destination cross-fade it accompanies, which is the intended order — the
    // indicator must never be the thing the learner is waiting for.
    val slot = animateFloatAsState(
        targetValue = selectedSlot.toFloat(),
        animationSpec = AppMotion.spatialSpec(),
        label = "compactNavigationIndicator",
    )
    // The outer box spans the bar and holds one slot-wide child; the pill itself is the child, so
    // its own bounds are the pill's bounds rather than the whole strip it travels along.
    Box(
        Modifier
            .layout { measurable, constraints ->
                val slotWidth = constraints.maxWidth.toFloat() / slotCount
                val pillWidth = slotWidth.roundToInt()
                val placeable = measurable.measure(
                    constraints.copy(minWidth = pillWidth, maxWidth = pillWidth),
                )
                layout(constraints.maxWidth, placeable.height) {
                    // placeRelative rather than place, so a right-to-left layout mirrors the travel
                    // along with the row whose order it is tracking.
                    placeable.placeRelative(x = (slotWidth * slot.value).roundToInt(), y = 0)
                }
            }
            // The same inset the selected destination's own background used, so the pill still sits
            // inside one destination's slot rather than spanning the bar.
            .padding(horizontal = NavigationItemHorizontalInset),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .testTag(AppNavigationSelectedIndicatorTag),
        )
    }
}

@Composable
private fun RowScope.CompactNavigationDestination(
    destination: AppTopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    badges: AppNavigationBadges,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = AppMotion.effectSpec(),
        label = "compactNavigationContent",
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(horizontal = NavigationItemHorizontalInset),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // The clip stays although the destination no longer paints anything: it is what
                // keeps the selectable state layer inside the pill's shape, and that state layer is
                // still the whole of the press feedback here.
                .clip(MaterialTheme.shapes.extraLarge)
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .testTag(appNavigationBarItemTag(destination)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                CompactDestinationIcon(
                    destination = destination,
                    badges = badges,
                    contentColor = contentColor,
                    selected = selected,
                )
                Text(
                    text = stringResource(destination.label),
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CompactDestinationIcon(
    destination: AppTopLevelDestination,
    badges: AppNavigationBadges,
    contentColor: Color,
    selected: Boolean,
) {
    // A second, quiet cue that the selection landed here, reinforcing a pill whose colour separation
    // from the bar is slight in the light scheme. Only the glyph scales, never the box around it:
    // the badge is positioned against that box, so it neither moves nor grows, and the destination's
    // measured geometry is untouched. Read inside graphicsLayer, so the emphasis is a draw-phase
    // change and costs no recomposition.
    val iconScale = animateFloatAsState(
        targetValue = if (selected) SelectedIconScale else UnselectedIconScale,
        animationSpec = AppMotion.spatialSpec(),
        label = "compactNavigationIconScale",
    )
    Box(
        modifier = Modifier
            .size(NavigationIconSize)
            .testTag(appNavigationBarIconTag(destination)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = iconScale.value
                    scaleX = scale
                    scaleY = scale
                },
        )
        val count = badges[destination] ?: 0
        if (count > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = NavigationBadgeOffsetX, y = NavigationBadgeOffsetY),
            ) {
                Text(count.toString())
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
                .padding(horizontal = AppSpacing.Comfortable)
                .padding(bottom = AppLayout.CompactNavigationBottomGap),
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
private val NavigationContentHorizontalPadding: Dp = 2.dp
private val NavigationContentVerticalPadding: Dp = 4.dp
private val NavigationItemHorizontalInset: Dp = 2.dp
private val NavigationIconSize: Dp = 24.dp

/**
 * Deliberately small: the 24dp glyph gains 0.6dp per edge. The emphasis has to register on the
 * transition without the selected destination reading as a different size from its neighbours once
 * it has settled, and it is a single axis-free scale rather than one of several transforms at once.
 */
private const val SelectedIconScale = 1.05f
private const val UnselectedIconScale = 1f
private val NavigationBadgeOffsetX: Dp = 6.dp
private val NavigationBadgeOffsetY: Dp = (-5).dp

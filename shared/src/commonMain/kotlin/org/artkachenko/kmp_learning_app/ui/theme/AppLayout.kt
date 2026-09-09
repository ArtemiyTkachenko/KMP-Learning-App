package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window-dependent layout values.
 *
 * These are separated from [AppSpacing] because they are not scale steps: they are decisions that
 * depend on how much room the window actually has. The app runs on five hosts and any of them can
 * be either size — a desktop or browser window can be dragged narrow — so the values are keyed to
 * measured width rather than to the platform.
 */
internal object AppLayout {

    /**
     * The Material compact/medium boundary.
     *
     * Below this a window is phone-shaped; at or above it there is room for a navigation rail
     * beside the content and for a wider content margin. This is the single definition of that
     * boundary — the navigation shell reads it from here rather than declaring its own.
     */
    val CompactWidthBreakpoint: Dp = 600.dp

    /**
     * The largest width content is allowed to occupy.
     *
     * Without this, a phone layout stretches across a full desktop or browser window: a two-line
     * Topic row spans 1600px with its name pinned to the far left and its accuracy figure to the
     * far right, which is unreadable and is the most conspicuously wrong thing about the wide
     * hosts. 840.dp is Material's expanded-pane guidance and is close to the classic measure of
     * 60–75 characters at this type scale.
     */
    val MaxContentWidth: Dp = 840.dp

    /**
     * The horizontal margin for screen content at a given window width.
     *
     * Material specifies 16.dp for compact windows and 24.dp from medium upward. The app
     * previously used a fixed 20.dp everywhere, which is slightly too generous on a phone and
     * clearly too tight once the window is wide enough to show a rail.
     */
    fun screenHorizontalMargin(windowWidth: Dp): Dp =
        if (windowWidth >= CompactWidthBreakpoint) AppSpacing.Section else AppSpacing.Comfortable
}

/**
 * The horizontal margin the current window calls for.
 *
 * Provided by the navigation scaffold, which is the one place that already measures window width.
 * A composition local rather than a parameter because every screen needs it and none of them make
 * a decision with it — threading a `Dp` through ten screen signatures and their previews would add
 * noise to each one to express a value none of them owns.
 *
 * The default matches a compact window so a screen composed on its own — a preview, or a test that
 * bypasses the shell — still lays out sensibly.
 */
internal val LocalAppContentMargin = staticCompositionLocalOf { AppSpacing.Comfortable }

/**
 * Content padding for a scrolling screen: the window's horizontal margin plus vertical breathing
 * room. This replaced `PaddingValues(horizontal = 20.dp, vertical = 16.dp)`, which was written out
 * identically in eight screen files and so could only ever be changed in eight places at once.
 *
 * For a list of flat rows use [appListContentPadding] instead — see the reason there.
 */
@Composable
@ReadOnlyComposable
internal fun appScreenContentPadding(
    top: Dp = AppSpacing.Comfortable,
    bottom: Dp = AppSpacing.Comfortable,
): PaddingValues {
    val margin = LocalAppContentMargin.current
    return PaddingValues(start = margin, end = margin, top = top, bottom = bottom)
}

/**
 * Content padding for a list whose rows are their own interactive surfaces: vertical only.
 *
 * A list item's container spans its pane, and the horizontal margin belongs *inside* the row —
 * Material states this as `ListTokens.ItemLeadingSpace` / `ItemTrailingSpace`. Giving the margin to
 * the list instead insets the row itself, which insets its state layer with it: hover, focus, and
 * press then draw a band whose edges land exactly on the text, with no padding anywhere inside it.
 * That is invisible on a touch screen, where a press flashes and is gone, and permanent on a
 * pointer host, where hover is the resting state of whatever the cursor is over.
 *
 * So the list keeps only its vertical padding, and each row applies [LocalAppContentMargin] itself,
 * inside its own `clickable`. The text does not move; the surface behind it grows to the pane.
 * Dividers between such rows take the same inset, which is what keeps them aligned with the
 * content — Material's `ListTokens.DividerLeadingSpace` is that alignment stated at the compact
 * margin. A list of Cards needs none of this: a Card is its own container and clips its own state
 * layer, so those lists keep [appScreenContentPadding].
 */
@Composable
@ReadOnlyComposable
internal fun appListContentPadding(
    top: Dp = AppSpacing.Comfortable,
    bottom: Dp = AppSpacing.Comfortable,
): PaddingValues = PaddingValues(top = top, bottom = bottom)

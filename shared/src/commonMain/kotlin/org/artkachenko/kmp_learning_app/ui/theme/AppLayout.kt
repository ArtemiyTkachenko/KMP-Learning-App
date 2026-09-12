package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
     * The medium/expanded boundary: the width at which a screen may compose two panes side by side.
     *
     * Material's own value is 840dp. This app uses 1040dp, and the difference is deliberate:
     * 840dp is [MaxContentWidth], the width of this app's *single* pane. A window exactly that
     * wide has room for one comfortable column and not for two, so treating it as expanded would
     * hand every dashboard a pair of 380dp columns — narrower than the phone layout they replaced.
     *
     * 1040dp is where a two-pane split still leaves each pane above [CompactWidthBreakpoint] once
     * the navigation rail (80dp), both window margins, and the gutter between the panes are taken
     * out, so each pane is at least as wide as a phone rather than a squeezed version of one.
     */
    val ExpandedWidthBreakpoint: Dp = 1040.dp

    /**
     * The largest width ordinary single-column content is allowed to occupy.
     *
     * Without this, a phone layout stretches across a full desktop or browser window: a two-line
     * Topic row spans 1600px with its name pinned to the far left and its accuracy figure to the
     * far right, which is unreadable and is the most conspicuously wrong thing about the wide
     * hosts. 840.dp is Material's expanded-pane guidance.
     */
    val MaxContentWidth: Dp = 840.dp

    /**
     * The largest width a multi-pane screen is allowed to occupy.
     *
     * Dashboards and review screens compose two panes at [ExpandedWidthBreakpoint] and above, so
     * they need more than [MaxContentWidth]; they still need a limit, because a 2560px monitor
     * would otherwise give each pane a 1200px measure and reintroduce, once per pane, exactly the
     * stretched row [MaxContentWidth] exists to prevent.
     */
    val MaxPanedWidth: Dp = 1440.dp

    /**
     * The text measure for long-form lesson prose.
     *
     * Prose is the one content type in this app where the [MaxContentWidth] cap is still too
     * generous. At `bodyLarge` (16sp) an average character occupies roughly half the type size, so
     * 840dp carries something over 100 characters a line — past the point where the eye reliably
     * finds the start of the next one. 600dp is about 70 characters at the same scale, inside the
     * 65–75 band that typographic convention settles on, and it is a *maximum*: a phone never
     * reaches it and reads full width behind the ordinary margins.
     *
     * This is the width of the text column itself. A pane holding it adds the window margin on
     * each side — see [AppContentWidth.Reading].
     */
    val ReadingMeasure: Dp = 600.dp

    /** The gutter between two panes of an expanded layout. Wider than any gap inside a pane. */
    val PaneGutter: Dp = AppSpacing.Section

    /**
     * The horizontal margin for screen content at a given window width.
     *
     * Material specifies 16.dp for compact windows and 24.dp from medium upward. The app
     * previously used a fixed 20.dp everywhere, which is slightly too generous on a phone and
     * clearly too tight once the window is wide enough to show a rail.
     */
    fun screenHorizontalMargin(windowWidth: Dp): Dp =
        if (windowWidth >= CompactWidthBreakpoint) AppSpacing.Section else AppSpacing.Comfortable

    /** The class a measured window width falls into. */
    fun windowSizeClassFor(windowWidth: Dp): AppWindowSizeClass = when {
        windowWidth >= ExpandedWidthBreakpoint -> AppWindowSizeClass.Expanded
        windowWidth >= CompactWidthBreakpoint -> AppWindowSizeClass.Medium
        else -> AppWindowSizeClass.Compact
    }
}

/**
 * How much room the window has, in the three classes the app actually makes decisions with.
 *
 * Three rather than one check per screen. Before this, the only window measurement in the app was
 * the navigation shell's rail breakpoint, and any screen wanting to behave differently on a desktop
 * would have had to measure for itself — which is how a codebase ends up with four breakpoints that
 * disagree. A screen asks which class it is in and composes accordingly; it does not compare
 * `Dp` values of its own.
 *
 * The classes mean:
 * - [Compact] — phone-shaped. One column, navigation along the bottom edge.
 * - [Medium] — a tablet, a split-screen desktop window, a narrow browser. Still one column, but a
 *   wider one, with a navigation rail beside it.
 * - [Expanded] — a desktop or full-width tablet. A screen whose content genuinely divides may
 *   compose two panes here; one that does not simply keeps its single column, centred.
 *
 * Expanded does not mean "must be two panes". The Lesson reader stays a single centred column at
 * every width, because prose does not become more readable by being split in half.
 */
internal enum class AppWindowSizeClass {
    Compact,
    Medium,
    Expanded,
    ;

    /** True from [Medium] upward: there is a navigation rail and a wider margin. */
    val isAtLeastMedium: Boolean get() = this != Compact

    /** True only at [Expanded]: a screen may compose two panes. */
    val isExpanded: Boolean get() = this == Expanded
}

/**
 * The window class the current window falls into.
 *
 * Provided by the navigation scaffold, which is the one place that measures window width, for the
 * same reason [LocalAppContentMargin] is. The default is [AppWindowSizeClass.Compact] so a screen
 * composed on its own — a preview, or a test that bypasses the shell — gets the phone layout, which
 * is the one that lays out sensibly at any size.
 */
internal val LocalAppWindowSizeClass =
    staticCompositionLocalOf { AppWindowSizeClass.Compact }

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
 * How wide a screen's content is allowed to grow before it stops using the window.
 *
 * The shell used to cap every screen at one value, which is why a wide window showed the same
 * phone column whatever was in it: a twenty-paragraph Lesson, a dashboard of eight figures, and a
 * result transcript all stopped at 840dp. They are not the same kind of content and they do not
 * want the same measure, so the cap moved out of the shell and each screen now states which of
 * three it is. The shell still decides the *margin* and the window class, which are properties of
 * the window rather than of the content.
 */
internal enum class AppContentWidth {
    /** Long-form prose. The narrowest, and the only one derived from a character count. */
    Reading,

    /** The default: lists, forms, detail screens. One comfortable column. */
    Standard,

    /** A screen that composes two panes at [AppWindowSizeClass.Expanded]. */
    Paned,
}

/** The cap this content width imposes at the current window margin. */
@Composable
@ReadOnlyComposable
internal fun AppContentWidth.maxWidth(): Dp = when (this) {
    // The measure is the text column; the margins sit outside it, so a pane that stopped at
    // ReadingMeasure would leave the prose itself narrower than the value says.
    AppContentWidth.Reading -> AppLayout.ReadingMeasure + LocalAppContentMargin.current * 2
    AppContentWidth.Standard -> AppLayout.MaxContentWidth
    // Only once there are actually two panes. Below the expanded breakpoint a `Paned` screen is
    // composing the same single column as a `Standard` one, and letting it run to 1440dp there
    // would stretch that column past the measure every other screen stops at.
    AppContentWidth.Paned -> if (LocalAppWindowSizeClass.current.isExpanded) {
        AppLayout.MaxPanedWidth
    } else {
        AppLayout.MaxContentWidth
    }
}

/**
 * Centres a screen's content and stops it growing past what its content type can use.
 *
 * Every top-level screen wraps its content in one of these. It replaced the single cap the
 * navigation shell applied to everything below it, which could not distinguish a Lesson from a
 * dashboard and had the further problem that it capped each screen's `TopAppBar` too — so on a
 * wide desktop window the bar stopped short of both window edges and floated in the middle of the
 * page, which is the single clearest symptom of a phone layout dropped into a desktop window.
 */
@Composable
internal fun AppContentPane(
    width: AppContentWidth,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        // Centred rather than leading-aligned so a window between the breakpoint and the cap does
        // not appear off-balance.
        Box(Modifier.widthIn(max = width.maxWidth()).fillMaxSize()) {
            content()
        }
    }
}

/**
 * The pane below a screen's `TopAppBar`, filling the rest of the column.
 *
 * The shape almost every screen in this app has is a `Column` holding a bar and then a `when` over
 * its loading, empty, error, and content states, each branch taking `Modifier.weight(1f)`. This
 * keeps that shape working: it takes the weight itself and hands its content a `ColumnScope`, so
 * the branches inside are unchanged and the bar above stays outside the cap — which is the point,
 * because a bar that stops short of the window edges is what a capped shell looked like on a
 * desktop.
 */
@Composable
internal fun ColumnScope.AppScreenPane(
    width: AppContentWidth = AppContentWidth.Standard,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = width.maxWidth()).fillMaxSize(), content = content)
    }
}

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

package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale.
 *
 * Spacing was previously written as `.dp` literals at every call site. The convention was real —
 * most values already sat on a 4.dp grid — but nothing enforced it, so `6.dp`, `10.dp`, and
 * `14.dp` had drifted in, and a screen could not state whether its `12.dp` meant "these two things
 * are related" or "this is just the gap I used".
 *
 * The names are relational rather than absolute so a value can be retuned without every call site
 * lying about what it asked for: [Related] separates things that belong together, [Grouped]
 * separates siblings in a list, [Section] separates one group from the next.
 */
internal object AppSpacing {

    /** Lines within a single block of text. */
    val Tight: Dp = 4.dp

    /** Elements that read as one unit: an icon and its label, a value and its caption. */
    val Related: Dp = 8.dp

    /** Siblings in a list, and the gap inside a compact card. */
    val Grouped: Dp = 12.dp

    /** A card's own interior padding, and the gap between distinct blocks inside one. */
    val Comfortable: Dp = 16.dp

    /** A prominent card's interior padding. */
    val Generous: Dp = 20.dp

    /**
     * One section from the next.
     *
     * This must stay visibly larger than [Grouped]. A section heading previously sat in the same
     * 12.dp flow as the cards it introduced, so a section break and an ordinary item gap looked
     * near-identical and proximity stopped communicating grouping at all.
     */
    val Section: Dp = 24.dp
}

/**
 * The vertical padding every scrolling screen applies to its content.
 *
 * The horizontal margin is deliberately not part of this: it varies with window width and is
 * supplied by `AppLayout.screenHorizontalMargin`. Combine the two rather than reintroducing a
 * fixed horizontal literal.
 */
internal val AppScreenVerticalPadding: PaddingValues =
    PaddingValues(vertical = AppSpacing.Comfortable)

/** Keeps the last item in a scrolling list clear of the navigation bar when it scrolls to the end. */
internal val AppListBottomPadding: Dp = AppSpacing.Section

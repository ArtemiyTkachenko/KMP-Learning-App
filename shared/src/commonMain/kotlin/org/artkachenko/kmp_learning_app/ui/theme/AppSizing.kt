package org.artkachenko.kmp_learning_app.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The glyph scale.
 *
 * These were written as private `val`s at every call site, and `20.dp` alone had reached twelve
 * declarations under nine names — `NavigationChevronSize` in four separate files, plus
 * `ChevronSize`, `RowIconSize`, `SectionAccentSize`, `CompletionIconSize`, `StudiedMarkSize`,
 * `ScopeIconSize` and `VerdictIconSize` — with five further call sites writing the number inline.
 * Two of those declarations carried the comment *"at the size every other row draws them"*, which
 * is a call site asking for a scale it did not have.
 *
 * Nothing had drifted yet. That is the reason to do this now rather than the reason not to: the
 * same thing happened one level up with the centred loading and error states, which reached seven
 * private wrappers before anyone noticed they had settled on three different spacings.
 *
 * The names say where the glyph sits rather than how big it is, so a step can be retuned without
 * every call site lying about what it asked for — the same principle as [AppSpacing].
 *
 * Sizes that are genuinely about one surface stay where they are used: the Topic marker's glyph is
 * proportional to the marker around it, the navigation bar's icon is a navigation-bar token, the
 * lesson bullet's column is a prose measure rather than an icon, and the trend chart's geometry is
 * a set of proportions that only mean anything to each other. A scale is for what the product
 * shares.
 */
internal object AppIconSize {

    /**
     * Set within a line of body text: a source link's external-link mark.
     *
     * Smaller than [Action] because it sits *inside* a sentence rather than beside a label, and at
     * [Action] it outweighed the text it belonged to.
     */
    val Inline: Dp = 16.dp

    /**
     * The leading icon of a text button, and anything sized to match one.
     *
     * This is Material's own `ButtonDefaults.IconSize` rather than a number of ours, quoted so a
     * reviewer can re-derive it and so it cannot drift from the buttons it sits in. A busy
     * control's progress indicator takes it too, which is what keeps a button the same width while
     * it works as it was at rest.
     */
    val Action: Dp = ButtonDefaults.IconSize

    /**
     * Beside a row's text: the trailing navigation chevron, a completion mark, a leading accent.
     *
     * This one is the app's own convention rather than a Material token — it is the size at which a
     * glyph reads as a mark on a `titleMedium` line without becoming a badge. Every row in the
     * product draws its chevron at this size, which is the fact the old private copies were each
     * trying to state on their own.
     */
    val Row: Dp = 20.dp
}

/**
 * The stroke scale: how heavily an edge is drawn.
 *
 * Two weights, and the distinction between them is meaning rather than taste. [Hairline] is an edge
 * that separates a surface from what is under it; [Emphasis] is an edge that says *this one is
 * selected*. A selected option drawn at [Hairline] does not read as chosen, and a resting card
 * drawn at [Emphasis] reads as chosen when it is not.
 */
internal object AppStroke {

    /** A surface's own edge: a hero's rim, a card's accent border, an unselected option's outline. */
    val Hairline: Dp = 1.dp

    /** The selected state of a choice the learner has made. */
    val Emphasis: Dp = 2.dp

    /**
     * The stroke of a busy control's progress indicator.
     *
     * Scaled to the [AppIconSize.Action] circle it draws in rather than to anything around it:
     * Material's 4dp default on an 18dp indicator is a ring, not a spinner. It shares a number with
     * [Emphasis] and not a meaning, which is why it is its own entry — a control's busy state and a
     * learner's selected choice are not one concept that happens to be drawn twice.
     */
    val Indicator: Dp = 2.dp
}

/**
 * How far a surface is lifted off the page.
 *
 * One entry, because this app states depth tonally — three surface levels, not a spectrum — and
 * reaches for a shadow only where a surface has to outrank the cards *beside* it rather than sit at
 * a different tone from them. That is the hero case and, so far, only the hero case.
 */
internal object AppElevation {

    /**
     * A hero, above the cards under it. `ElevationTokens.Level2`.
     *
     * The three heroes each declared this as 2dp, which is not a Material elevation level: the
     * scale is 0, 1, 3, 6, 8, 12. It was landing between two levels rather than on one, and
     * consolidating three copies of that would have settled an unasked question by accident.
     *
     * Level1 is the wrong answer even though it is nearer the old value, because 1dp is exactly
     * what `FilledCardTokens.HoverContainerElevation` lifts an ordinary card to. On a pointer host
     * hover is the resting state of whatever the cursor is over, so a hero at Level1 would be
     * indistinguishable from any card the learner happened to be pointing at. Level2 is the first
     * level that clears it, which is what the old 2dp was reaching for without a token to land on.
     */
    val Hero: Dp = 3.dp
}

/**
 * Material's minimum touch target, for the rows that have to state it themselves.
 *
 * Material components enforce this through `LocalMinimumInteractiveComponentSize`, so a `Button` or
 * a `Checkbox` needs nothing here. A bare `Row` made tappable with `Modifier.clickable` is not a
 * Material component and gets no minimum of its own, and four surfaces had each written the number
 * out with its own prose explaining that.
 *
 * It is a `Dp` rather than a read of the composition local because these call sites apply it as a
 * `heightIn` minimum at the point they build the row, and the local's value is the same 48dp.
 */
internal val AppMinimumTouchTarget: Dp = 48.dp

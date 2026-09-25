package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing

/**
 * The one surface on a screen whose subject is an accuracy.
 *
 * This replaces `PrimarySummaryCard`, which said the right thing on paper — level 2, the surface
 * that outranks the rest — and did not deliver it. `surfaceContainer` is a single step above the
 * `surfaceContainerLow` of the cards beneath it, and at that distance a dashboard's headline figure
 * and the two summaries qualifying it read as three equal boxes. In dark, where separation is
 * luminance rather than tone, they were very nearly indistinguishable.
 *
 * So the rank is stated three ways rather than one: `surfaceContainerHigh`, a hairline
 * `outlineVariant` edge, and a small shadow. The edge is what carries it in dark, where one
 * container step is not enough on its own and a shadow is almost invisible; the shadow is what
 * carries it in light, where the two tones are close in value. Neither alone is sufficient in both
 * schemes, which is why both are here.
 *
 * ## Ring, not bar
 *
 * The figure sits beside [AccuracyRing]. A bar is the picture of *how far through something you
 * are* — which is exactly what a coverage meter under this card shows — and a ring is the picture
 * of a *rate*. Drawing both with the same control made a Topic's accuracy and its coverage look
 * like two readings of one quantity, when the whole reason they are on the same card is that they
 * are not.
 *
 * ## What this is not
 *
 * Not the hero gradient. Its two call sites are the screens where nothing else competes — a finished
 * run's score, and the Progress dashboard's standing answer above six or more containers. This card
 * is what a *scope* leads with: a Topic drill-down and a Topic's practice page, each of which sits
 * inside a page that has other subjects. Handing the gradient to those as well would leave the
 * product with no surface that means anything by being one.
 *
 * [percentage] is nullable because a scope can be the subject of its screen without having earned a
 * figure yet — below the domain's evidence minimum there is no honest accuracy to draw, and a ring
 * at 0% would be a measurement the learner never produced. The card then states [caption] alone and
 * leaves the explanation to [content]. A caller whose *whole* surface should step down when there
 * is nothing to lead on should use [SecondarySummaryCard] instead of passing null here.
 *
 * [content] is everything the screen puts under the figure — a divider and its supporting counts, a
 * coverage block, a badge — and is the reason this is one component rather than three that happen
 * to share a border.
 */
@Composable
internal fun AccuracyHeroCard(
    percentage: Double?,
    caption: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(HeroBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = HeroElevation,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Generous),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (percentage == null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val accuracy = accuracyColor(percentage)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccuracyRing(
                        fraction = (percentage / 100.0).toFloat(),
                        color = accuracy,
                        trackColor = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = AccuracyRingTrackAlpha),
                    )
                    Column(
                        // Weighted so the figure and its caption wrap inside the space left beside
                        // the ring rather than pushing the row past the window at a large type
                        // scale, which a fixed row of a 64dp ring and a display-size numeral does.
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
                    ) {
                        Text(
                            text = formatAccuracy(percentage),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = accuracy,
                        )
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
    }
}

/** The edge and the lift that make a hero outrank the cards under it; see [AccuracyHeroCard]. */
private val HeroBorderWidth = 1.dp
private val HeroElevation = 2.dp

/**
 * Supporting summary content: the same shape language as [AccuracyHeroCard], two tonal steps
 * quieter and without an edge, so a screen can carry several summaries without any of them
 * competing with its headline figure.
 */
@Composable
internal fun SecondarySummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.Comfortable),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
            content = content,
        )
    }
}

/**
 * Several related rows inside one container, instead of one container each.
 *
 * ## The missing rank
 *
 * The app's surface vocabulary went straight from a hero to "a card", so any screen with several
 * comparable things to offer drew several comparable cards — and a column of six rounded boxes at
 * one tonal level says nothing about which of them belong together. The Topics screen opened on
 * four of them before the catalogue it is named for; the Progress dashboard ran three more under
 * "Topic performance". In both cases the rows were *already* a set, and the layout was spending a
 * container per member to say so.
 *
 * This is that set, drawn as one thing: one edge, one corner radius, one shadow-free surface, with
 * the members separated by hairlines instead of by gaps. It is a rank *between* an ordinary card
 * and a bare list — at the same tonal level as a card, deliberately, because grouping is not
 * promotion. What changes is the count of edges, and the count of edges is what the eye reads as
 * the count of things.
 *
 * With it, a screen has four ranks to say things with rather than two: a hero, a card, a group,
 * and rows flat on the page. The Progress dashboard now uses all four in one scroll — the
 * standing hero, the recent-performance card, the per-Topic table as a group, and session
 * history as bare rows — and the ladder is what makes the order of importance readable without
 * any of them changing colour.
 *
 * ## When not to use it
 *
 * **A group is for a set that supports the screen, not for the thing the screen is about.** The
 * Topics catalogue stays a column of cards even though its rows are as uniform as any table's,
 * because browsing it *is* what that screen is for and its rows carry a colour marker, a variable
 * badge, and an accuracy block that make them genuinely different heights. The Progress dashboard's
 * per-Topic table is the mirror case: the same kind of content, but supporting detail under a hero,
 * and two plain lines per row. One is content, one is reference — so one keeps its edges and one
 * gives them up.
 *
 * **Rows are composed eagerly**, so the length must be bounded and small: two continuation
 * shortcuts, the Topics a learner has answered anything in. An unbounded record such as session
 * history stays a lazy list of its own rows, because a group that composed four hundred attempts to
 * draw one border would be paying for the border with the scroll.
 *
 * **A row that has to be visually singled out** does not belong here either. A weak-area row
 * carries an accent border, and a border is a property of a container — inside a group there is no
 * container to put it on, and the group's own edge would have to change colour because one member
 * did.
 *
 * ## What a row owes
 *
 * The group draws no padding and no click of its own. Each row states its own [GroupRowPadding]
 * inset — [AccuracyRow] is the shared row for the common case, a name and a rate — and each row
 * that navigates carries its own `clickable`, its own `Role`, and its own test handle — so an item inside a group is exactly as identifiable, as tappable, and as announceable
 * as the card it replaced, and the group can hold a mix of navigable and inert rows without a flag
 * saying which is which. Because the container clips, a row's state layer follows the group's
 * corners at the ends and runs edge to edge in the middle, which is what makes a hover read as one
 * row of a list rather than as a rectangle floating inside a card.
 *
 * [rows] is a list rather than a `ColumnScope` block so the dividers cannot be forgotten, doubled,
 * or left dangling after a conditional row: the group knows how many members it has and draws
 * exactly one hairline between each adjacent pair. An empty list composes nothing at all, so a
 * caller may build it conditionally and never has to ask whether anything survived.
 */
@Composable
internal fun ContentGroup(
    rows: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    // Inset to the rows' own text inset rather than run full bleed. Material states
                    // this as `ListTokens.DividerLeadingSpace`: a rule that starts where the content
                    // starts reads as a separator between two entries, and one that crosses the
                    // whole container reads as a cut through the container itself.
                    modifier = Modifier.padding(start = GroupRowPadding),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            row()
        }
    }
}

/**
 * The horizontal inset a [ContentGroup] row applies, and the inset its dividers are drawn at.
 *
 * Stated here rather than at each row so the two cannot drift: a divider aligned to a different
 * inset from the text above it is the one detail that makes a grouped list look assembled by hand.
 */
internal val GroupRowPadding: Dp = AppSpacing.Comfortable

/**
 * Shared section hierarchy for scrollable learning and progress content.
 *
 * The top margin is the point of this component. A heading sits in the same `spacedBy` flow as the
 * cards it introduces, so with only 8.dp of its own it was separated from the section above by
 * about the same distance as two sibling cards — proximity then said nothing about grouping, and
 * the screens read as one undifferentiated column of cards. [AppSpacing.Section] is deliberately
 * twice the largest gap between siblings, so a section break is unambiguous.
 */
@Composable
internal fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = AppSpacing.Section,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .padding(top = topPadding)
            .semantics { heading() },
    )
}

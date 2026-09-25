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

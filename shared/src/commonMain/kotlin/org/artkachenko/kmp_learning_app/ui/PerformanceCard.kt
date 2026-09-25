package org.artkachenko.kmp_learning_app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras

/**
 * One accuracy row: what it is on the left, how the learner is doing on the right.
 *
 * Shared by the progress dashboard, the topic drill-down, and the interview result so a topic's
 * performance reads identically wherever it appears. [detail] is already-formatted supporting text
 * so each feature keeps its own wording and string resources.
 *
 * The accuracy figure is the point of the row, so it is the largest thing in it and is coloured
 * against the domain's weakness threshold.
 *
 * A weak row is marked by an accent border and the coloured figure, over the same neutral container
 * every other row uses. It previously filled the whole card with `partiallyCorrectContainer`, which
 * is a saturated amber: on a single row that reads as emphasis, and down a list of six weak areas it
 * reads as an alarm wall in which nothing stands out because everything is shouting. The border is
 * the quieter statement of the same fact, and it never travels alone — the figure is tinted, the
 * ordering puts weak rows first, and [weakLabel] is available wherever the surrounding context does
 * not already say what these rows are.
 *
 * [comparesWithSiblings] adds a meter under the row, and is off by default because most lists of
 * these cards are not comparisons. A Topic's Subtopics are: they are parts of one whole, measured on
 * one scale, and the question the learner opened the screen to ask is which of them is worst. Four
 * percentages down the right-hand edge answer that only by being read and remembered one at a time,
 * where four bars answer it at a glance. A session-history list is the counter-example — consecutive
 * attempts of different lengths on different scopes are not rows to compare against each other, and
 * a bar on each would invite exactly that.
 *
 * [onClick] makes the whole card a button and adds the navigation chevron. Keeping the callback and
 * affordance together prevents an inert card from advertising navigation. [action] is an optional
 * low-emphasis control on its own line under the figures. It is absent by
 * default, so a card stays a reading surface unless a caller deliberately gives it something to do,
 * and it sits below the row rather than inside it so a long title and the control never compete for
 * the same width.
 */
@Composable
internal fun PerformanceCard(
    title: String,
    detail: String,
    percentage: Double?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    caption: String? = null,
    isWeak: Boolean = false,
    weakLabel: String? = null,
    comparesWithSiblings: Boolean = false,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    require(onClick == null || action == null) {
        "A PerformanceCard cannot be both navigable and contain a separate action."
    }
    val semantic = AppThemeExtras.semanticColors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                },
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = if (isWeak) BorderStroke(WeakBorderWidth, semantic.partiallyCorrect) else null,
    ) {
        val meter = percentage.takeIf { comparesWithSiblings }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Whatever comes next supplies the card's bottom inset, so the row does not leave a
                // full gap above a meter or a control that belongs to it.
                .padding(
                    start = AppSpacing.Comfortable,
                    end = AppSpacing.Comfortable,
                    top = AppSpacing.Comfortable,
                )
                .padding(
                    bottom = when {
                        action != null -> AppSpacing.Tight
                        meter != null -> AppSpacing.Grouped
                        else -> AppSpacing.Comfortable
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                caption?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Only where the badge adds something the container does not already say. A list
                // whose heading is "Weak areas" passes null: repeating the heading on every card in
                // it is noise, not a second signal.
                if (isWeak && weakLabel != null) {
                    StatusBadge(
                        text = weakLabel,
                        contentColor = semantic.onPartiallyCorrectContainer,
                        containerColor = semantic.partiallyCorrectContainer,
                        icon = AppIcons.Warning,
                    )
                }
            }
            percentage?.let {
                Text(
                    text = formatAccuracy(it),
                    style = MaterialTheme.typography.titleLarge,
                    color = accuracyColor(it),
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        meter?.let {
            ProgressMeter(
                fraction = (it / 100.0).toFloat(),
                color = accuracyColor(it),
                // The figure above it is already announced; a second progress node saying the same
                // number is noise to a screen reader, so the bar is drawing only.
                modifier = Modifier
                    .padding(
                        start = AppSpacing.Comfortable,
                        end = AppSpacing.Comfortable,
                        bottom = AppSpacing.Comfortable,
                    )
                    .clearAndSetSemantics {},
            )
        }
        action?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacing.Related,
                        end = AppSpacing.Related,
                        bottom = AppSpacing.Tight,
                    ),
            ) {
                it()
            }
        }
    }
}

/**
 * The same accuracy row as [PerformanceCard], without the card.
 *
 * A row inside a [ContentGroup] and a row inside a Card are the same reading: what it is on the
 * left, how the learner is doing on the right. Three screens reached the group form within two
 * changes of each other — the Progress dashboard's per-Topic table, a Mixed interview's
 * performance-by-topic breakdown, and the Interview start screen's own record — and each wrote the
 * row out again, so three surfaces were one edit away from disagreeing about the type scale of a
 * percentage.
 *
 * This is deliberately *not* `PerformanceCard(inGroup = true)`. The card carries three things a
 * grouped row cannot: an accent border, which is a property of a container; a comparison meter,
 * which belongs under a row rather than in it; and a low-emphasis action on its own line, which a
 * group would let bleed into the divider below it. A flag would have to answer for all three, and
 * the answer is the same every time — those are card features. Two components, one shared reading.
 *
 * [onClick] makes the whole row a button and adds the navigation chevron, for the same reason the
 * card keeps them together: an inert row must not advertise navigation. The minimum touch target
 * sits inside the `clickable`, so the state layer spans the row rather than being inset from it and
 * a two-line row still clears 48dp.
 *
 * The row merges its descendants whether or not it is navigable. A `clickable` merges on its own,
 * so a navigable row was already announced as one thing — but an inert one was three separate
 * nodes, and inside a group there is no longer a card edge to imply that the name, the score, and
 * the rate belong together. A Mixed interview's breakdown would have been read as nine unrelated
 * fragments. Merging is what makes a row a row to a screen reader as well as to the eye.
 */
@Composable
internal fun AccuracyRow(
    title: String,
    detail: String,
    percentage: Double?,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                },
            )
            .heightIn(min = MinimumTouchTargetSize)
            .padding(GroupRowPadding)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        percentage?.let {
            Text(
                text = formatAccuracy(it),
                style = MaterialTheme.typography.titleLarge,
                color = accuracyColor(it),
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(NavigationChevronSize),
            )
        }
    }
}

/** Material's minimum touch target, stated here because a bare row is not a Material component. */
private val MinimumTouchTargetSize = 48.dp

/** The trailing navigation affordance, at the size every other row in the app draws it. */
private val NavigationChevronSize = 20.dp

/** Thick enough to read as a deliberate accent at a glance, thin enough not to become a frame. */
private val WeakBorderWidth = 1.dp

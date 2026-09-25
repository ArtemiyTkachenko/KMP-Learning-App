package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_weak_label
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.jetbrains.compose.resources.stringResource

/**
 * One observation-based performance row.
 *
 * The accuracy figure is the point of the row, so it is the largest thing in it and is coloured
 * against the domain's weakness threshold — these numbers used to render at body size in a uniform
 * colour, which made a weak area and a strong one look identical. A weak row additionally carries an
 * accent border and, where the context does not already state it, a badge — so it is identifiable
 * without reading the number at all.
 *
 * [caption] is the row's quietest line, and is where current-curriculum coverage goes: a second
 * figure with a different denominator, worth saying but never worth another card.
 *
 * [action] is absent by default, so adding a shortcut to one kind of row leaves every other
 * performance card on the dashboard exactly as readable and exactly as inert as it was.
 */
@Composable
internal fun ProgressPerformanceCard(
    title: String,
    subtitle: String?,
    correctCount: Int,
    answeredCount: Int,
    percentage: Double?,
    modifier: Modifier = Modifier,
    caption: String? = null,
    isWeak: Boolean = false,
    /**
     * The badge text for a weak row, or `null` to leave the accent border and the coloured figure to
     * say it alone. A caller passes `null` where the surrounding context already states it — under a
     * "Weak areas" heading, every card carrying the badge repeats that heading once per row.
     */
    weakLabel: String? = stringResource(Res.string.progress_weak_label),
    comparesWithSiblings: Boolean = false,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    PerformanceCard(
        title = title,
        detail = stringResource(Res.string.progress_score, correctCount, answeredCount),
        percentage = percentage,
        modifier = modifier,
        subtitle = subtitle,
        caption = caption,
        isWeak = isWeak,
        weakLabel = weakLabel,
        comparesWithSiblings = comparesWithSiblings,
        onClick = onClick,
        action = action,
    )
}

/**
 * A dashboard section heading, optionally with a small status accent beside it.
 *
 * [icon] is a leading mark rather than a parameter that changes what a heading *is*: the text, the
 * role, the heading semantics and the top margin are the shared [SectionHeading] either way. The
 * accent exists so one section — weak areas — can carry the app's warning tone once, at the top,
 * instead of every row underneath it carrying a badge that repeats the heading.
 *
 * The icon is always decorative. A heading whose status is readable only from the colour of a glyph
 * beside it is a status nobody using a screen reader receives, so the words carry it and the mark
 * only finds it faster.
 */
@Composable
internal fun ProgressSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = AppSpacing.Section,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (icon == null) {
        SectionHeading(text, modifier, topPadding)
        return
    }
    Row(
        modifier = modifier.padding(top = topPadding),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(SectionAccentSize),
        )
        // The margin is already applied to the row, so the heading itself adds none.
        SectionHeading(text, topPadding = 0.dp)
    }
}

/** Beside `titleLarge`: large enough to register as an accent, small enough not to be a badge. */
private val SectionAccentSize = 20.dp

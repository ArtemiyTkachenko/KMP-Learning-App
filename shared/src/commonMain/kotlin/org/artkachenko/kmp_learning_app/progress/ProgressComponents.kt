package org.artkachenko.kmp_learning_app.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_weak_label
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.jetbrains.compose.resources.stringResource

/**
 * One observation-based performance row.
 *
 * The accuracy figure is the point of the row, so it is the largest thing in it and is coloured
 * against the domain's weakness threshold — these numbers used to render at body size in a uniform
 * colour, which made a weak area and a strong one look identical. A weak row additionally tints its
 * container and carries a badge, so it is identifiable without reading the number at all.
 */
@Composable
internal fun ProgressPerformanceCard(
    title: String,
    subtitle: String?,
    correctCount: Int,
    answeredCount: Int,
    percentage: Double,
    modifier: Modifier = Modifier,
    isWeak: Boolean = false,
    showChevron: Boolean = false,
    isSummary: Boolean = false,
) {
    PerformanceCard(
        title = title,
        detail = stringResource(Res.string.progress_score, correctCount, answeredCount),
        percentage = percentage,
        modifier = modifier,
        subtitle = subtitle,
        isWeak = isWeak,
        weakLabel = stringResource(Res.string.progress_weak_label),
        showChevron = showChevron,
        isSummary = isSummary,
    )
}

@Composable
internal fun ProgressSectionTitle(text: String) {
    SectionHeading(text)
}

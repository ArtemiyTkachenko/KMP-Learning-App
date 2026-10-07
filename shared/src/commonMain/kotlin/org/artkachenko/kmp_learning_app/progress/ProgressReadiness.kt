package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.progress_readiness_action_practice
import kmp_learning_app.shared.generated.resources.progress_readiness_action_practice_unseen
import kmp_learning_app.shared.generated.resources.progress_readiness_advanced_unavailable
import kmp_learning_app.shared.generated.resources.progress_readiness_answered
import kmp_learning_app.shared.generated.resources.progress_readiness_block_title
import kmp_learning_app.shared.generated.resources.progress_readiness_change_target
import kmp_learning_app.shared.generated.resources.progress_readiness_change_target_description
import kmp_learning_app.shared.generated.resources.progress_readiness_chooser_detail
import kmp_learning_app.shared.generated.resources.progress_readiness_chooser_question
import kmp_learning_app.shared.generated.resources.progress_readiness_evidence_coverage
import kmp_learning_app.shared.generated.resources.progress_readiness_evidence_due_mistakes
import kmp_learning_app.shared.generated.resources.progress_readiness_evidence_no_due_mistakes
import kmp_learning_app.shared.generated.resources.progress_readiness_evidence_recent
import kmp_learning_app.shared.generated.resources.progress_readiness_evidence_recent_insufficient
import kmp_learning_app.shared.generated.resources.progress_readiness_gap_coverage
import kmp_learning_app.shared.generated.resources.progress_readiness_gap_due_mistakes
import kmp_learning_app.shared.generated.resources.progress_readiness_gap_more_answers
import kmp_learning_app.shared.generated.resources.progress_readiness_gap_recent_accuracy
import kmp_learning_app.shared.generated.resources.progress_readiness_missing_title
import kmp_learning_app.shared.generated.resources.progress_readiness_none_measurable
import kmp_learning_app.shared.generated.resources.progress_readiness_ready_count
import kmp_learning_app.shared.generated.resources.progress_readiness_row_evidence
import kmp_learning_app.shared.generated.resources.progress_readiness_status_getting_there
import kmp_learning_app.shared.generated.resources.progress_readiness_status_not_enough_evidence
import kmp_learning_app.shared.generated.resources.progress_readiness_status_ready
import kmp_learning_app.shared.generated.resources.progress_readiness_target_junior
import kmp_learning_app.shared.generated.resources.progress_readiness_target_label
import kmp_learning_app.shared.generated.resources.progress_readiness_target_mid_level
import kmp_learning_app.shared.generated.resources.progress_readiness_target_senior
import kmp_learning_app.shared.generated.resources.progress_readiness_title
import kmp_learning_app.shared.generated.resources.progress_review_mistakes_action
import kmp_learning_app.shared.generated.resources.progress_topic_unavailable
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessGap
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessPolicy
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessStatus
import org.artkachenko.kmp_learning_app.learning_progress.ReadinessTarget
import org.artkachenko.kmp_learning_app.ui.AccuracyRow
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.ContentGroup
import org.artkachenko.kmp_learning_app.ui.SecondarySummaryCard
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal const val ProgressReadinessChooserTag = "progress_readiness_chooser"
internal const val ProgressReadinessGroupTag = "progress_readiness_group"
internal const val ProgressReadinessChangeTargetTag = "progress_readiness_change_target"
internal const val ProgressTopicReadinessTag = "progress_topic_readiness"

internal fun progressReadinessTargetTag(target: ReadinessTarget): String =
    "progress_readiness_target_${target.name}"

internal fun progressReadinessRowTag(topicId: String): String = "progress_readiness_row_$topicId"

internal fun progressReadinessStatusTag(status: ReadinessStatus): String =
    "progress_readiness_status_${status.name}"

/**
 * The readiness section: where the learner stands against the interview level they chose.
 *
 * It belongs to the standing group — beneath the all-time hero and the recent window, above
 * everything a learner acts on — because it answers the same kind of question those do, "where am I
 * now?", one step more specific. It is not a second hero and takes no gradient: its answer is a set
 * of per-Topic verdicts, not one figure.
 *
 * Until a target is chosen the section is a question rather than a table, because a status against
 * a level the learner never picked would be a verdict nobody asked for. [choosingTarget] reopens
 * that question from the header; it is presentation state, owned by the dashboard.
 *
 * The Topic rows are one [ContentGroup] of [AccuracyRow]s, the shape the Topic performance table
 * below already uses, with the status badge in the figure's slot. Each row opens the Topic's
 * drill-down, where the full evidence and the actions are; a grouped row has no room for an action
 * line of its own, and fifteen action buttons would turn the standing group into a to-do list.
 */
internal fun LazyListScope.readinessSection(
    readiness: ProgressReadinessUiModel,
    choosingTarget: Boolean,
    onChangeTarget: () -> Unit,
    onChooseTarget: (ReadinessTarget) -> Unit,
    onTopicClick: (String) -> Unit,
) {
    when (readiness) {
        ProgressReadinessUiModel.TargetNotChosen -> {
            item(key = "readiness_heading") {
                ProgressSectionTitle(
                    stringResource(Res.string.progress_readiness_title),
                    topPadding = AppSpacing.Grouped,
                )
            }
            item(key = "readiness_chooser") {
                ReadinessTargetChooser(selected = null, onChooseTarget = onChooseTarget)
            }
        }
        is ProgressReadinessUiModel.Assessed -> {
            item(key = "readiness_heading") {
                ReadinessHeader(readiness, onChangeTarget)
            }
            if (choosingTarget) {
                item(key = "readiness_chooser") {
                    ReadinessTargetChooser(selected = readiness.target, onChooseTarget = onChooseTarget)
                }
            }
            if (readiness.topics.isEmpty()) {
                item(key = "readiness_none") {
                    Text(
                        text = stringResource(Res.string.progress_readiness_none_measurable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item(key = "readiness_group") {
                    ContentGroup(
                        modifier = Modifier.testTag(ProgressReadinessGroupTag),
                        rows = readiness.topics.map { topic ->
                            { ReadinessTopicRow(topic) { onTopicClick(topic.topicId) } }
                        },
                    )
                }
            }
        }
    }
}

/**
 * The section heading, then what it is measured against and how many Topics are ready — a count,
 * never an average — with the way to change the target beside it.
 */
@Composable
private fun ReadinessHeader(
    readiness: ProgressReadinessUiModel.Assessed,
    onChangeTarget: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
        ProgressSectionTitle(
            stringResource(Res.string.progress_readiness_title),
            topPadding = AppSpacing.Grouped,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        Res.string.progress_readiness_target_label,
                        targetLabel(readiness.target),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = pluralStringResource(
                        Res.plurals.progress_readiness_ready_count,
                        readiness.topics.size,
                        readiness.readyTopicCount,
                        readiness.topics.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val description = stringResource(Res.string.progress_readiness_change_target_description)
            TextButton(
                onClick = onChangeTarget,
                modifier = Modifier
                    .testTag(ProgressReadinessChangeTargetTag)
                    .semantics { contentDescription = description },
            ) {
                Text(stringResource(Res.string.progress_readiness_change_target))
            }
        }
    }
}

/**
 * "What are you preparing for?", answered by one of three low-emphasis actions.
 *
 * Text buttons rather than the builder's option tiles: nothing here is a configuration that stays
 * on screen to be read back — a tap is the answer, and the section turns into statuses. The current
 * target, when the chooser is reopened, is the one disabled option, which says it is already chosen
 * without a second selection treatment.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadinessTargetChooser(
    selected: ReadinessTarget?,
    onChooseTarget: (ReadinessTarget) -> Unit,
) {
    SecondarySummaryCard(modifier = Modifier.testTag(ProgressReadinessChooserTag)) {
        Text(
            text = stringResource(Res.string.progress_readiness_chooser_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.progress_readiness_chooser_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
            ReadinessTarget.entries.forEach { target ->
                TextButton(
                    onClick = { onChooseTarget(target) },
                    enabled = target != selected,
                    modifier = Modifier.testTag(progressReadinessTargetTag(target)),
                ) {
                    Text(targetLabel(target))
                }
            }
        }
    }
}

/**
 * One Topic's verdict, as a row of the readiness table: the name, the evidence, the most important
 * thing missing, and the status in words on the right.
 *
 * A Senior verdict on a Topic with no Advanced Questions carries the note under it here as well as
 * on the drill-down, because the badge alone would claim senior readiness on evidence the app does
 * not have.
 */
@Composable
private fun ReadinessTopicRow(
    readiness: ProgressTopicReadinessUiModel,
    onClick: () -> Unit,
) {
    val caption = listOfNotNull(
        readiness.gaps.firstOrNull()?.let { gapText(it) },
        if (readiness.advancedUnavailable) {
            stringResource(Res.string.progress_readiness_advanced_unavailable)
        } else {
            null
        },
    ).joinToString(separator = "\n").ifEmpty { null }
    AccuracyRow(
        title = readiness.topicName ?: stringResource(Res.string.progress_topic_unavailable),
        detail = evidenceSummary(readiness),
        percentage = null,
        modifier = Modifier.testTag(progressReadinessRowTag(readiness.topicId)),
        caption = caption,
        onClick = onClick,
        trailing = { ReadinessStatusBadge(readiness.status) },
    )
}

/**
 * The status in words, with colour as the second channel. None of the three is red or amber.
 *
 * - `READY` takes the correct family, the app's "this went well" tone, with its check mark.
 * - `GETTING_THERE` takes the secondary tonal pair: a neutral, branded tone, because being part way
 *   is not a fault, and the weak-area amber is reserved for the one verdict that is.
 * - `NOT_ENOUGH_EVIDENCE` is the quietest neutral there is — the "in progress" lesson badge's pair —
 *   because it is not a judgement at all.
 */
@Composable
internal fun ReadinessStatusBadge(
    status: ReadinessStatus,
    modifier: Modifier = Modifier,
) {
    val colors = readinessBadgeColors(status)
    StatusBadge(
        text = stringResource(
            when (status) {
                ReadinessStatus.READY -> Res.string.progress_readiness_status_ready
                ReadinessStatus.GETTING_THERE -> Res.string.progress_readiness_status_getting_there
                ReadinessStatus.NOT_ENOUGH_EVIDENCE ->
                    Res.string.progress_readiness_status_not_enough_evidence
            },
        ),
        contentColor = colors.content,
        containerColor = colors.container,
        icon = if (status == ReadinessStatus.READY) AppIcons.CheckCircle else null,
        modifier = modifier.testTag(progressReadinessStatusTag(status)),
    )
}

internal data class ReadinessBadgeColors(val content: Color, val container: Color)

/** The tonal pair each status takes; see [ReadinessStatusBadge] for why. */
@Composable
internal fun readinessBadgeColors(status: ReadinessStatus): ReadinessBadgeColors {
    val semantic = AppThemeExtras.semanticColors
    val colors = MaterialTheme.colorScheme
    return when (status) {
        ReadinessStatus.READY -> ReadinessBadgeColors(semantic.onCorrectContainer, semantic.correctContainer)
        ReadinessStatus.GETTING_THERE -> ReadinessBadgeColors(colors.onSecondaryContainer, colors.secondaryContainer)
        ReadinessStatus.NOT_ENOUGH_EVIDENCE ->
            ReadinessBadgeColors(colors.onSurfaceVariant, colors.surfaceContainerHighest)
    }
}

/**
 * The drill-down's readiness block for the current target: the status, every piece of evidence
 * behind it, and everything still missing, each with the existing destination that addresses it.
 *
 * A [SecondarySummaryCard] beneath the Topic's hero: it is a second reading of the same Topic, not a
 * competitor for the hero's rank. The evidence lines name their denominators, because coverage
 * counts current Questions once each while recent accuracy counts answers.
 */
@Composable
internal fun ProgressTopicReadinessBlock(
    readiness: ProgressTopicReadinessUiModel,
    onReviewMistakes: () -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
) {
    SecondarySummaryCard(modifier = Modifier.testTag(ProgressTopicReadinessTag)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    Res.string.progress_readiness_block_title,
                    targetLabel(readiness.target),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            ReadinessStatusBadge(readiness.status)
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
            EvidenceLine(
                stringResource(
                    Res.string.progress_readiness_evidence_coverage,
                    readiness.coveredQuestionCount,
                    readiness.targetQuestionCount,
                ),
            )
            EvidenceLine(recentAccuracyEvidence(readiness))
            EvidenceLine(
                if (readiness.dueMistakeCount == 0) {
                    stringResource(Res.string.progress_readiness_evidence_no_due_mistakes)
                } else {
                    pluralStringResource(
                        Res.plurals.progress_readiness_evidence_due_mistakes,
                        readiness.dueMistakeCount,
                        readiness.dueMistakeCount,
                    )
                },
            )
            if (readiness.advancedUnavailable) {
                EvidenceLine(stringResource(Res.string.progress_readiness_advanced_unavailable))
            }
        }
        if (readiness.gaps.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.progress_readiness_missing_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
                readiness.gaps.forEach { gap ->
                    ReadinessGapLine(
                        gap = gap,
                        topicId = readiness.topicId,
                        target = readiness.target,
                        onReviewMistakes = onReviewMistakes,
                        onPracticePreset = onPracticePreset,
                    )
                }
            }
        }
    }
}

@Composable
private fun EvidenceLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * One missing item and, where one obviously exists, the way to address it: due mistakes open the
 * Mistakes area, and the rest open the Practice Builder on this Topic at the target's levels. Every
 * destination already exists; nothing starts from here.
 */
@Composable
private fun ReadinessGapLine(
    gap: ReadinessGap,
    topicId: String,
    target: ReadinessTarget,
    onReviewMistakes: () -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = gapText(gap),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val preset = gap.toPracticePreset(topicId, target.levels)
        TextButton(
            onClick = { if (preset == null) onReviewMistakes() else onPracticePreset(preset) },
            modifier = Modifier.testTag(progressReadinessGapActionTag(gap)),
        ) {
            Text(
                stringResource(
                    when {
                        preset == null -> Res.string.progress_review_mistakes_action
                        gap is ReadinessGap.CoverageBelow ->
                            Res.string.progress_readiness_action_practice_unseen
                        else -> Res.string.progress_readiness_action_practice
                    },
                ),
            )
        }
    }
}

internal fun progressReadinessGapActionTag(gap: ReadinessGap): String =
    "progress_readiness_gap_action_${gap::class.simpleName}"

@Composable
private fun gapText(gap: ReadinessGap): String =
    when (gap) {
        is ReadinessGap.MoreAnswersNeeded -> pluralStringResource(
            Res.plurals.progress_readiness_gap_more_answers,
            gap.remaining,
            gap.remaining,
        )
        is ReadinessGap.DueMistakes -> pluralStringResource(
            Res.plurals.progress_readiness_gap_due_mistakes,
            gap.count,
            gap.count,
        )
        is ReadinessGap.RecentAccuracyBelow -> stringResource(
            Res.string.progress_readiness_gap_recent_accuracy,
            formatWholePercent(gap.percentage),
            formatAccuracy(gap.required),
        )
        is ReadinessGap.CoverageBelow -> stringResource(
            Res.string.progress_readiness_gap_coverage,
            formatWholePercent(gap.percentage),
            formatAccuracy(gap.required),
        )
    }

/**
 * Rounded down, so a gap never reads as met: 79.9% shown as "80%" beside "aim for 80%" would tell the
 * learner they had reached a threshold the derivation says they have not.
 */
private fun formatWholePercent(percentage: Double): String = "${percentage.toInt()}%"

/**
 * Below the evidence minimum there is no recent figure, for the reason the accuracy evidence minimum
 * gives everywhere else: a percentage over two answers describes the two answers.
 */
@Composable
private fun recentAccuracyEvidence(readiness: ProgressTopicReadinessUiModel): String {
    val accuracy = readiness.recentAccuracy
    return if (accuracy == null || readiness.status == ReadinessStatus.NOT_ENOUGH_EVIDENCE) {
        stringResource(
            Res.string.progress_readiness_evidence_recent_insufficient,
            ReadinessPolicy.MinimumAnsweredForVerdict,
        )
    } else {
        pluralStringResource(
            Res.plurals.progress_readiness_evidence_recent,
            readiness.recentAnsweredCount,
            formatAccuracy(accuracy),
            readiness.recentAnsweredCount,
        )
    }
}

@Composable
private fun evidenceSummary(readiness: ProgressTopicReadinessUiModel): String {
    val accuracy = readiness.recentAccuracy
    return if (accuracy == null || readiness.status == ReadinessStatus.NOT_ENOUGH_EVIDENCE) {
        pluralStringResource(
            Res.plurals.progress_readiness_answered,
            readiness.answeredCount,
            readiness.answeredCount,
        )
    } else {
        stringResource(
            Res.string.progress_readiness_row_evidence,
            formatAccuracy(accuracy),
            readiness.coveredQuestionCount,
            readiness.targetQuestionCount,
        )
    }
}

@Composable
private fun targetLabel(target: ReadinessTarget): String =
    stringResource(
        when (target) {
            ReadinessTarget.JUNIOR -> Res.string.progress_readiness_target_junior
            ReadinessTarget.MID_LEVEL -> Res.string.progress_readiness_target_mid_level
            ReadinessTarget.SENIOR -> Res.string.progress_readiness_target_senior
        },
    )

package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_accuracy
import kmp_learning_app.shared.generated.resources.learning_context_explored
import kmp_learning_app.shared.generated.resources.learning_context_not_studied
import kmp_learning_app.shared.generated.resources.progress_weak_label
import kmp_learning_app.shared.generated.resources.topic_detail_available_questions
import kmp_learning_app.shared.generated.resources.topic_detail_subtopics_empty
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.stringResource

/**
 * The Topic's Subtopics, as rows rather than as a Card each.
 *
 * This is the long list on the screen — a dozen rows on a large Topic — and a container per row
 * made it read as a dozen competing surfaces. Dividers, alignment, and typography carry the same
 * separation for a fraction of the visual weight, and the row keeps its own click target so the tap
 * affordance is unchanged.
 */
@Composable
internal fun TopicSubtopicsPage(
    subtopics: List<SubtopicPracticeItem>,
    onStartSubtopicPractice: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    if (subtopics.isEmpty()) {
        // A Topic whose questions are all authored at Topic level is an ordinary Topic, not a
        // broken one, so the tab says so rather than sitting blank.
        ScreenMessage(
            message = stringResource(Res.string.topic_detail_subtopics_empty),
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(TopicSubtopicsListTag),
        contentPadding = appScreenContentPadding(top = AppSpacing.Related),
    ) {
        items(items = subtopics, key = { it.subtopic.id }) { item ->
            SubtopicRow(
                item = item,
                onStartSubtopicPractice = onStartSubtopicPractice,
                onPracticePreset = onPracticePreset,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * One Subtopic: name, what is known about it, and the practice it can start.
 *
 * The row itself starts ordinary practice for the whole Subtopic, which is why there is no filled
 * button here — one would duplicate the row's own click target and compete with the Topic-level
 * primary action on the Practice tab. The targeted shortcuts sit below the click target and are
 * labelled, so tapping the row and tapping a shortcut cannot be confused for one another.
 */
@Composable
private fun SubtopicRow(
    item: SubtopicPracticeItem,
    onStartSubtopicPractice: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
) {
    val context = item.learningContext
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SubtopicPracticeButtonTag)
                .clickable { onStartSubtopicPractice(item.subtopic.id) }
                .padding(vertical = AppSpacing.Comfortable),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                Text(
                    text = item.subtopic.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (context == null) {
                    // No analytics to show, so the row keeps the authored count it has always had
                    // rather than claiming the Subtopic is unstudied.
                    Text(
                        text = stringResource(
                            Res.string.topic_detail_available_questions,
                            item.questionCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Coverage already carries the Subtopic's current total, so the authored count
                    // is not repeated beside it.
                    SubtopicLearningContext(context)
                }
            }
            // Absent rather than 0% for a Subtopic with no recorded answer.
            context?.accuracyPercentage?.let { accuracy ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatAccuracy(accuracy),
                        style = MaterialTheme.typography.titleMedium,
                        color = accuracyColor(accuracy),
                    )
                    Text(
                        text = stringResource(Res.string.learning_context_accuracy),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        // Outside the row's click target, and labelled: tapping the row is still ordinary practice
        // for the whole Subtopic, and these say what else they do.
        TargetedPracticeActions(
            context = context,
            onPracticeWeakAreas = {
                onPracticePreset(
                    PracticePreset(
                        scope = AssessmentScope.Subtopic(item.subtopic.id),
                        source = PracticeQuestionSource.WEAK_AREAS,
                    ),
                )
            },
            onPracticeUnseen = {
                onPracticePreset(
                    PracticePreset(
                        scope = AssessmentScope.Subtopic(item.subtopic.id),
                        source = PracticeQuestionSource.UNSEEN,
                    ),
                )
            },
            weakTestTag = subtopicWeakPracticeTag(item.subtopic.id),
            unseenTestTag = subtopicUnseenPracticeTag(item.subtopic.id),
            modifier = Modifier.padding(bottom = AppSpacing.Related),
        )
    }
}

/**
 * A Subtopic row's supporting line. Compact by design: this list can run to a dozen rows, so a
 * Subtopic gets its coverage count, a neutral note when nothing has been studied, and a weak badge
 * only when the domain says so.
 */
@Composable
private fun SubtopicLearningContext(context: LearningContextUiModel) {
    if (context.hasCoverageScope) {
        Text(
            text = stringResource(
                Res.string.learning_context_explored,
                context.attemptedQuestionCount,
                context.totalQuestionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (context.isUnstudied) {
        Text(
            text = stringResource(Res.string.learning_context_not_studied),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (context.isWeak) {
        StatusBadge(
            text = stringResource(Res.string.progress_weak_label),
            contentColor = AppThemeExtras.semanticColors.onPartiallyCorrectContainer,
            containerColor = AppThemeExtras.semanticColors.partiallyCorrectContainer,
            icon = AppIcons.Warning,
        )
    }
}

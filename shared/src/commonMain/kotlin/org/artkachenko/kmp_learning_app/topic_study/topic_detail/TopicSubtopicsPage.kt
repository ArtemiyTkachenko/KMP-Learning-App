package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_accuracy
import kmp_learning_app.shared.generated.resources.learning_context_explored
import kmp_learning_app.shared.generated.resources.learning_context_not_studied
import kmp_learning_app.shared.generated.resources.progress_weak_label
import kmp_learning_app.shared.generated.resources.topic_detail_available_questions
import kmp_learning_app.shared.generated.resources.topic_detail_practice_this_topic
import kmp_learning_app.shared.generated.resources.topic_detail_subtopics_empty
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.TrailingFigureRow
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppContentMargin
import org.artkachenko.kmp_learning_app.ui.theme.appListContentPadding
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
    onBrowsePractice: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    if (subtopics.isEmpty()) {
        // A Topic whose questions are all authored at Topic level is an ordinary Topic, not a
        // broken one. Practising it is exactly what the missing rows would have led to, so the tab
        // points at the Topic-level action rather than leaving the learner on a dead end.
        ScreenAction(
            message = stringResource(Res.string.topic_detail_subtopics_empty),
            actionLabel = stringResource(Res.string.topic_detail_practice_this_topic),
            onAction = onBrowsePractice,
            modifier = modifier,
        )
        return
    }
    // Full-bleed: the rows own the horizontal margin, so their state layers reach the pane edges.
    // See `appListContentPadding`.
    val margin = LocalAppContentMargin.current
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag(TopicSubtopicsListTag),
        contentPadding = appListContentPadding(top = AppSpacing.Related),
    ) {
        itemsIndexed(items = subtopics, key = { _, item -> item.subtopic.id }) { index, item ->
            SubtopicRow(
                item = item,
                onStartSubtopicPractice = onStartSubtopicPractice,
                onPracticePreset = onPracticePreset,
            )
            // Between rows only. A rule under the last one is a separator with nothing to separate,
            // and on a list that ends short of the window it draws a line across empty background
            // that reads as content still to come. `ContentGroup` states the same rule for the same
            // reason: exactly one hairline between each adjacent pair.
            if (index < subtopics.lastIndex) {
                HorizontalDivider(
                    // Inset to the same margin as the row content, so the rule stays aligned with
                    // the text rather than running the full width of the pane behind it.
                    modifier = Modifier
                        .padding(horizontal = margin)
                        .testTag(SubtopicRowDividerTag),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
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
 *
 * ## Making the shortcuts belong to the row
 *
 * They are outside the click target, which is correct, and that left them looking like they were
 * outside the *row*: the gap above them was wider than the gap to the rule beneath, so on a list of
 * a dozen Subtopics two text buttons sat midway between the entry they act on and the next one. The
 * row's content therefore gives up its bottom padding when it has shortcuts and the shortcut block
 * takes the larger gap instead, which puts the controls with their evidence and the space before
 * the separator. Whether there are any is [hasTargetedPractice], asked once here and once by the
 * controls themselves, so the spacing and the controls cannot disagree.
 *
 * Their labels also line up with the row's title now. A `TextButton` insets its own label by
 * `ButtonDefaults.TextButtonContentPadding`, so a block placed at the content margin put the only
 * other column of text on the page at a second, slightly different left edge. The offset is taken
 * from the token rather than written as a literal, and through `calculateStartPadding` so it stays
 * on the correct side in a right-to-left layout.
 */
@Composable
private fun SubtopicRow(
    item: SubtopicPracticeItem,
    onStartSubtopicPractice: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
) {
    val context = item.learningContext
    val margin = LocalAppContentMargin.current
    val hasShortcuts = context.hasTargetedPractice
    val labelInset = ButtonDefaults.TextButtonContentPadding
        .calculateStartPadding(LocalLayoutDirection.current)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SubtopicPracticeButtonTag)
                .clickable(role = Role.Button) {
                    onStartSubtopicPractice(item.subtopic.id)
                }
                // Inside the clickable, so the state layer spans the pane and the content is inset
                // within it rather than the whole row being inset and the band hugging the text.
                .padding(
                    start = margin,
                    end = margin,
                    top = AppSpacing.Comfortable,
                    // The shortcuts below are part of this row, so when there are any they supply
                    // the separation from what follows and this stops paying for it twice.
                    bottom = if (hasShortcuts) AppSpacing.Tight else AppSpacing.Comfortable,
                ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrailingFigureRow(
                modifier = Modifier.weight(1f),
                // Absent rather than 0% for a Subtopic with no recorded answer, and the row then
                // has no reflow question to ask.
                figure = {
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
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
                    Text(
                        text = item.subtopic.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (context == null) {
                        // No analytics to show, so the row keeps the authored count it has always
                        // had rather than claiming the Subtopic is unstudied.
                        Text(
                            text = stringResource(
                                Res.string.topic_detail_available_questions,
                                item.questionCount,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Coverage already carries the Subtopic's current total, so the authored
                        // count is not repeated beside it.
                        SubtopicLearningContext(context)
                    }
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
            // Outside the row's click target, so it carries the margin itself — less the inset the
            // buttons apply to their own labels, so those labels start where the title does.
            modifier = Modifier.padding(
                start = (margin - labelInset).coerceAtLeast(0.dp),
                end = margin,
                bottom = AppSpacing.Grouped,
            ),
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
    // Only when the coverage line above is absent. `isUnstudied` is a strict subset of it — nothing
    // attempted and no accuracy — so wherever coverage is shown it already reads "0 of N explored",
    // and printing both made every untouched row state the same fact twice in two type sizes. A
    // scope with no current Questions has no coverage line, and there this is the only thing to say.
    if (!context.hasCoverageScope && context.isUnstudied) {
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

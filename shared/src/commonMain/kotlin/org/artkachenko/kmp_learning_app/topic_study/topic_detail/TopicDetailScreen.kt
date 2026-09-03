package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_accuracy
import kmp_learning_app.shared.generated.resources.learning_context_coverage_count
import kmp_learning_app.shared.generated.resources.learning_context_coverage_title
import kmp_learning_app.shared.generated.resources.learning_context_explored
import kmp_learning_app.shared.generated.resources.learning_context_not_studied
import kmp_learning_app.shared.generated.resources.practice_shortcut_unseen
import kmp_learning_app.shared.generated.resources.practice_shortcut_weak_area
import kmp_learning_app.shared.generated.resources.progress_weak_label
import kmp_learning_app.shared.generated.resources.topic_browser_error
import kmp_learning_app.shared.generated.resources.topic_detail_accuracy_caption
import kmp_learning_app.shared.generated.resources.topic_detail_available_questions
import kmp_learning_app.shared.generated.resources.topic_detail_heading
import kmp_learning_app.shared.generated.resources.topic_detail_loading
import kmp_learning_app.shared.generated.resources.topic_detail_no_questions
import kmp_learning_app.shared.generated.resources.topic_detail_not_found
import kmp_learning_app.shared.generated.resources.topic_detail_start_practice
import kmp_learning_app.shared.generated.resources.topic_detail_subtopics
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.SecondarySummaryCard
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.StatusBadge
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

internal const val TopicDetailLoadingTag = "topic_detail_loading"
internal const val TopicPracticeButtonTag = "topic_practice_button"
internal const val SubtopicPracticeButtonTag = "subtopic_practice_button"

/** The Topic's own targeted shortcuts, whose labels repeat on every Subtopic row below them. */
internal const val TopicWeakPracticeTag = "topic_weak_practice"
internal const val TopicUnseenPracticeTag = "topic_unseen_practice"

internal fun subtopicWeakPracticeTag(subtopicId: String): String =
    "subtopic_weak_practice_$subtopicId"

internal fun subtopicUnseenPracticeTag(subtopicId: String): String =
    "subtopic_unseen_practice_$subtopicId"

/**
 * The Topic's practice surface, with three kinds of practice entry point that must stay distinct.
 *
 * [onStartTopicPractice] and [onStartSubtopicPractice] are ordinary practice and are unchanged: they
 * carry a scope only, so the builder applies its `ALL` default. [onPracticePreset] carries a scope
 * *and* an existing question source, and is emitted only where this screen is already displaying the
 * signal that justifies it — the domain's `isWeak` verdict, or coverage that still has current
 * questions left in it. Neither is re-derived here, and neither ranks above the other: a scope that
 * is both weak and partly covered offers both, because the learner chose to look at that scope.
 * Choosing one for them is what Recommended Next does, elsewhere and on purpose.
 */
@Composable
internal fun TopicDetailScreen(
    state: TopicDetailUiState,
    targetSubtopicId: String? = null,
    onBack: () -> Unit,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(
            title = when (state) {
                is TopicDetailUiState.Content -> state.topic.name
                is TopicDetailUiState.NoQuestions -> state.topic.name
                else -> stringResource(Res.string.topic_detail_heading)
            },
            onBack = onBack,
            scrollBehavior = scrollBehavior,
        )

        when (state) {
            TopicDetailUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.topic_detail_loading),
                testTag = TopicDetailLoadingTag,
                modifier = Modifier.weight(1f),
            )

            is TopicDetailUiState.Content -> {
                TopicContent(
                    state = state,
                    targetSubtopicId = targetSubtopicId,
                    onStartTopicPractice = onStartTopicPractice,
                    onStartSubtopicPractice = onStartSubtopicPractice,
                    onPracticePreset = onPracticePreset,
                    modifier = Modifier.weight(1f),
                )
            }

            is TopicDetailUiState.NoQuestions -> ScreenMessage(
                message = stringResource(Res.string.topic_detail_no_questions),
                modifier = Modifier.weight(1f),
            )

            TopicDetailUiState.NotFound -> ScreenMessage(
                message = stringResource(Res.string.topic_detail_not_found),
                modifier = Modifier.weight(1f),
            )

            TopicDetailUiState.Error -> ScreenError(
                message = stringResource(Res.string.topic_browser_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TopicContent(
    state: TopicDetailUiState.Content,
    targetSubtopicId: String?,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.subtopics, targetSubtopicId) {
        val subtopicIndex = state.subtopics.indexOfFirst {
            it.subtopic.id == targetSubtopicId
        }
        if (subtopicIndex >= 0) {
            // The first lazy-list item is the topic summary and action block.
            //
            // Animated rather than instant: arriving here from search used to place the learner at
            // an arbitrary offset with no indication that the screen had scrolled at all, so a
            // Subtopic partway down a long Topic looked like the top of the list. Travelling there
            // shows that there is content above.
            listState.animateScrollToItem(subtopicIndex + 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Deliberately one lazy item: the summary, the action, and the Subtopics heading form the
        // header block, so a Subtopic opened from search still sits at its index plus one.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val context = state.learningContext
                if (context == null) {
                    // Analytics are unavailable, so the screen falls back to the authored count and
                    // says nothing about the learner. Practice is unaffected.
                    Text(
                        text = stringResource(
                            Res.string.topic_detail_available_questions,
                            state.topicQuestionCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    TopicLearningSummary(context)
                }
                // One primary action for the topic; subtopic rows below are the lower-emphasis
                // path, so the screen no longer shows several filled buttons of equal weight.
                Button(
                    onClick = onStartTopicPractice,
                    modifier = Modifier.fillMaxWidth().testTag(TopicPracticeButtonTag),
                ) {
                    Text(text = stringResource(Res.string.topic_detail_start_practice))
                }
                // Accelerators beside the primary action, never instead of it: with no analytics
                // this block is simply empty and ordinary practice is unaffected.
                TargetedPracticeActions(
                    context = context,
                    onPracticeWeakAreas = {
                        onPracticePreset(
                            PracticePreset(
                                scope = AssessmentScope.Topic(state.topic.id),
                                source = PracticeQuestionSource.WEAK_AREAS,
                            ),
                        )
                    },
                    onPracticeUnseen = {
                        onPracticePreset(
                            PracticePreset(
                                scope = AssessmentScope.Topic(state.topic.id),
                                source = PracticeQuestionSource.UNSEEN,
                            ),
                        )
                    },
                    weakTestTag = TopicWeakPracticeTag,
                    unseenTestTag = TopicUnseenPracticeTag,
                )
                SectionHeading(
                    text = stringResource(Res.string.topic_detail_subtopics),
                )
            }
        }
        items(
            items = state.subtopics,
            key = { it.subtopic.id },
        ) { item ->
            val context = item.learningContext
            // The row itself starts practice, so the per-row filled button is gone: it duplicated
            // the row's own click target and competed with the topic-level primary action.
            Card(
                onClick = { onStartSubtopicPractice(item.subtopic.id) },
                modifier = Modifier.fillMaxWidth().testTag(SubtopicPracticeButtonTag),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        // The targeted actions supply the bottom inset when there are any, so the
                        // row does not leave a full gap above controls that belong to it.
                        .padding(bottom = if (context.hasTargetedPractice) 4.dp else 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.subtopic.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (context == null) {
                            // No analytics to show, so the row keeps the authored count it has
                            // always had rather than claiming the Subtopic is unstudied.
                            Text(
                                text = stringResource(
                                    Res.string.topic_detail_available_questions,
                                    item.questionCount,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            // Coverage already carries the Subtopic's current total, so the
                            // authored count is not repeated beside it.
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
                // Inside the card but outside its click target, and labelled: tapping the row is
                // still ordinary practice for the whole Subtopic, and these say what else they do.
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
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                )
            }
        }
    }
}

/**
 * Whether a scope's already-derived context justifies any targeted shortcut at all.
 *
 * Defined on the nullable receiver so an unknown context answers `false` here rather than at every
 * call site: unknown analytics justify nothing, and that is a different statement from "not weak,
 * nothing left to see". This is also the only place the layout below asks the question, so a row's
 * spacing and its controls cannot disagree about whether it has any.
 */
private val LearningContextUiModel?.hasTargetedPractice: Boolean
    get() = this != null && (isWeak || hasUnseenQuestions)

/**
 * The targeted shortcuts a scope's already-derived learning context justifies, if any.
 *
 * Both conditions are read verbatim off the model: [LearningContextUiModel.isWeak] is the domain's
 * verdict and is never re-derived from the accuracy shown beside it, and
 * [LearningContextUiModel.hasUnseenQuestions] only restates the coverage counts already displayed.
 *
 * Both can be true at once and both are then offered. Which Questions either source actually yields
 * is decided later, by the selector, from history as it stands when practice is configured.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetedPracticeActions(
    context: LearningContextUiModel?,
    onPracticeWeakAreas: () -> Unit,
    onPracticeUnseen: () -> Unit,
    weakTestTag: String,
    unseenTestTag: String,
    modifier: Modifier = Modifier,
) {
    if (context == null || !context.hasTargetedPractice) return
    // Wraps rather than clips: two labelled text buttons do not share a line on a compact width or
    // at a large font scale.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (context.isWeak) {
            TextButton(onClick = onPracticeWeakAreas, modifier = Modifier.testTag(weakTestTag)) {
                Text(text = stringResource(Res.string.practice_shortcut_weak_area))
            }
        }
        if (context.hasUnseenQuestions) {
            TextButton(onClick = onPracticeUnseen, modifier = Modifier.testTag(unseenTestTag)) {
                Text(text = stringResource(Res.string.practice_shortcut_unseen))
            }
        }
    }
}

/**
 * The Topic's learning summary: one coherent surface rather than two competing cards.
 *
 * All-time accuracy leads when there is any, because it is the figure the learner came for, with
 * current coverage under a divider as the second, differently-scoped question. With no accuracy to
 * lead on, the whole thing steps down to a quieter card: an unstudied Topic should not open with a
 * display-size headline, and it must never open with a fabricated 0%.
 */
@Composable
private fun TopicLearningSummary(context: LearningContextUiModel) {
    val accuracy = context.accuracyPercentage
    if (accuracy == null) {
        SecondarySummaryCard {
            Text(
                text = stringResource(Res.string.learning_context_not_studied),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TopicCoverage(context)
        }
    } else {
        PrimarySummaryCard {
            AccuracyHeadline(
                percentage = accuracy,
                caption = stringResource(Res.string.topic_detail_accuracy_caption),
            )
            if (context.isWeak) {
                StatusBadge(
                    text = stringResource(Res.string.progress_weak_label),
                    contentColor = AppThemeExtras.semanticColors.onPartiallyCorrectContainer,
                    containerColor = AppThemeExtras.semanticColors.partiallyCorrectContainer,
                    icon = AppIcons.Warning,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TopicCoverage(context)
        }
    }
}

/**
 * Current curriculum coverage, in neutral theme colours throughout.
 *
 * Coverage is not scored: low coverage means material is still ahead of the learner, not that they
 * did badly, so it never borrows the correct/incorrect palette that accuracy uses. The meter is
 * driven by the exact counts rather than by the rounded percentage above it.
 */
@Composable
private fun TopicCoverage(context: LearningContextUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
        Text(
            text = stringResource(Res.string.learning_context_coverage_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                Res.string.learning_context_coverage_count,
                context.attemptedQuestionCount,
                context.totalQuestionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (context.hasCoverageScope) {
            ProgressMeter(
                fraction = context.attemptedQuestionCount.toFloat() / context.totalQuestionCount,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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

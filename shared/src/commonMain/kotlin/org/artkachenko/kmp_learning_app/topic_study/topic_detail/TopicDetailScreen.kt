package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.learning_context_accuracy
import kmp_learning_app.shared.generated.resources.learning_context_coverage_count
import kmp_learning_app.shared.generated.resources.learning_context_coverage_title
import kmp_learning_app.shared.generated.resources.learning_context_explored
import kmp_learning_app.shared.generated.resources.learning_context_not_studied
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
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.LearningContextUiModel
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
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

@Composable
internal fun TopicDetailScreen(
    state: TopicDetailUiState,
    targetSubtopicId: String? = null,
    onBack: () -> Unit,
    onStartTopicPractice: () -> Unit,
    onStartSubtopicPractice: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = when (state) {
                is TopicDetailUiState.Content -> state.topic.name
                is TopicDetailUiState.NoQuestions -> state.topic.name
                else -> stringResource(Res.string.topic_detail_heading)
            },
            onBack = onBack,
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
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.subtopics, targetSubtopicId) {
        val subtopicIndex = state.subtopics.indexOfFirst {
            it.subtopic.id == targetSubtopicId
        }
        if (subtopicIndex >= 0) {
            // The first lazy-list item is the topic summary and action block.
            listState.scrollToItem(subtopicIndex + 1)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                SectionHeading(
                    text = stringResource(Res.string.topic_detail_subtopics),
                )
            }
        }
        items(
            items = state.subtopics,
            key = { it.subtopic.id },
        ) { item ->
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        val context = item.learningContext
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
                    item.learningContext?.accuracyPercentage?.let { accuracy ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatAccuracy(accuracy),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            LinearProgressIndicator(
                progress = {
                    (context.attemptedQuestionCount.toFloat() / context.totalQuestionCount)
                        .coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
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

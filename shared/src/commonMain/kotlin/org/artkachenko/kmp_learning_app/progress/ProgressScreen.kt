package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_none
import kmp_learning_app.shared.generated.resources.mistake_review_unresolved_count
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import kmp_learning_app.shared.generated.resources.practice_shortcut_weak_area
import kmp_learning_app.shared.generated.resources.progress_accuracy_caption
import kmp_learning_app.shared.generated.resources.progress_completed_attempts_label
import kmp_learning_app.shared.generated.resources.progress_correct_answers_label
import kmp_learning_app.shared.generated.resources.progress_coverage_count
import kmp_learning_app.shared.generated.resources.progress_coverage_title
import kmp_learning_app.shared.generated.resources.progress_coverage_unavailable
import kmp_learning_app.shared.generated.resources.progress_empty
import kmp_learning_app.shared.generated.resources.progress_empty_action
import kmp_learning_app.shared.generated.resources.progress_error
import kmp_learning_app.shared.generated.resources.progress_focused_practice
import kmp_learning_app.shared.generated.resources.progress_focused_subtopic_scope
import kmp_learning_app.shared.generated.resources.progress_history
import kmp_learning_app.shared.generated.resources.progress_loading
import kmp_learning_app.shared.generated.resources.progress_overall
import kmp_learning_app.shared.generated.resources.progress_questions_answered_label
import kmp_learning_app.shared.generated.resources.progress_recent_title
import kmp_learning_app.shared.generated.resources.progress_recent_trend_description
import kmp_learning_app.shared.generated.resources.progress_recent_trend_insufficient
import kmp_learning_app.shared.generated.resources.progress_recent_trend_title
import kmp_learning_app.shared.generated.resources.progress_recent_window_one
import kmp_learning_app.shared.generated.resources.progress_recent_window_other
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_subtopic_unavailable
import kmp_learning_app.shared.generated.resources.progress_title
import kmp_learning_app.shared.generated.resources.progress_topic_performance
import kmp_learning_app.shared.generated.resources.progress_topic_unavailable
import kmp_learning_app.shared.generated.resources.progress_weak_areas
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.MetricFigure
import org.artkachenko.kmp_learning_app.ui.MetricRow
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.ProgressMeter
import org.artkachenko.kmp_learning_app.ui.PrimarySummaryCard
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenStateTransition
import org.artkachenko.kmp_learning_app.ui.SecondarySummaryCard
import org.artkachenko.kmp_learning_app.ui.accuracyColor
import org.artkachenko.kmp_learning_app.ui.formatAccuracy
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

internal const val ProgressLoadingTag = "progress_loading"

/** The scrolling dashboard itself, so tests can reach sections below the fold. */
internal const val ProgressContentTag = "progress_content"

/** Stable per-row handle so tests can target a Topic card without depending on label uniqueness. */
internal fun progressTopicCardTag(topicId: String): String = "progress_topic_card_$topicId"

/** Stable per-row handle for completed attempts whose visible labels may be identical. */
internal fun progressHistoryCardTag(attemptId: String): String = "progress_history_card_$attemptId"

/** Stable per-row handle for a weak area's practice shortcut, whose label repeats across rows. */
internal fun progressWeakAreaPracticeTag(area: WeakAreaUiModel): String =
    "progress_weak_area_practice_${area.type}_${area.stableId}"

/**
 * [onPracticePreset] carries a semantic practice intent, never a route: the dashboard says which
 * scope and which existing question source the learner asked for, and the navigation boundary turns
 * that into the Practice Builder. Only weak-area rows produce one — they are the dashboard's only
 * signal that names a Topic or Subtopic.
 */
@Composable
internal fun ProgressScreen(
    state: ProgressUiState,
    onBack: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onBrowseTopics: () -> Unit,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.progress_title), onBack, scrollBehavior)
        ScreenStateTransition(state = state, modifier = Modifier.weight(1f)) { current ->
            when (current) {
                ProgressUiState.Loading -> ScreenLoading(
                    message = stringResource(Res.string.progress_loading),
                    testTag = ProgressLoadingTag,
                    modifier = Modifier.fillMaxSize(),
                )
                ProgressUiState.Empty -> ScreenAction(
                    message = stringResource(Res.string.progress_empty),
                    actionLabel = stringResource(Res.string.progress_empty_action),
                    onAction = onBrowseTopics,
                    modifier = Modifier.fillMaxSize(),
                    icon = AppIcons.Insights,
                )
                ProgressUiState.Error -> ScreenError(
                    message = stringResource(Res.string.progress_error),
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
                is ProgressUiState.Content -> ProgressContent(
                    state = current,
                    onTopicClick = onTopicClick,
                    onHistoryClick = onHistoryClick,
                    onPracticePreset = onPracticePreset,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ProgressContent(
    state: ProgressUiState.Content,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(ProgressContentTag),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProgressSectionTitle(stringResource(Res.string.progress_overall))
        }
        item {
            OverallSummary(state)
        }
        // Coverage and recent performance sit under the headline as quieter summaries: they answer
        // different questions from all-time accuracy, so they must be separate surfaces, but making
        // all three equally dominant would leave the screen with no headline at all.
        item {
            CurriculumCoverageSummary(state.coverage)
        }
        state.recentPerformance?.let { recent ->
            item {
                RecentPerformanceSummary(recent)
            }
        }
        item {
            UnresolvedMistakeSummary(unresolvedCount = state.unresolvedMistakeCount)
        }
        if (state.weakAreas.isNotEmpty()) {
            item {
                ProgressSectionTitle(stringResource(Res.string.progress_weak_areas))
            }
            items(state.weakAreas, key = { "${it.type}:${it.stableId}" }) { area ->
                WeakAreaCard(area) { onPracticePreset(area.toPracticePreset()) }
            }
        }
        // Observation-based sections can be empty even when overall statistics exist, for
        // example after a curriculum import replaces the question IDs the history refers to.
        if (state.topics.isNotEmpty()) {
            item {
                ProgressSectionTitle(stringResource(Res.string.progress_topic_performance))
            }
            items(state.topics, key = ProgressTopicUiModel::topicId) { topic ->
                TopicPerformanceCard(topic) { onTopicClick(topic.topicId) }
            }
        }
        if (state.history.isNotEmpty()) {
            item {
                ProgressSectionTitle(stringResource(Res.string.progress_history))
            }
            items(state.history, key = CompletedAttemptUiModel::attemptId) { attempt ->
                HistoryCard(attempt) {
                    onHistoryClick(attempt.assessmentType, attempt.attemptId)
                }
            }
        }
    }
}

/**
 * Accuracy is the headline of the whole app, so it leads at display size with a meter behind it;
 * the counts that support it become a scannable label/value column instead of four equal lines.
 */
@Composable
private fun OverallSummary(state: ProgressUiState.Content) {
    PrimarySummaryCard {
        AccuracyHeadline(
            percentage = state.percentage,
            caption = stringResource(Res.string.progress_accuracy_caption),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
            MetricRow(
                label = stringResource(Res.string.progress_completed_attempts_label),
                value = state.completedAttemptCount.toString(),
            )
            MetricRow(
                label = stringResource(Res.string.progress_questions_answered_label),
                value = state.answeredQuestionCount.toString(),
            )
            MetricRow(
                label = stringResource(Res.string.progress_correct_answers_label),
                value = state.correctAnswerCount.toString(),
            )
        }
    }
}

/**
 * How much of the current question bank the learner has seen — a different question from how
 * accurately they answered it, and one the percentage alone cannot answer, so the raw counts are
 * always shown beside it and the meter is never the only representation.
 *
 * The figure is deliberately not tinted with [accuracyColor]: colouring 30% coverage red would read
 * as a bad score, when it only means most of the bank is still ahead of the learner.
 */
@Composable
private fun CurriculumCoverageSummary(coverage: ProgressCoverageUiModel) {
    SecondarySummaryCard {
        Text(
            text = stringResource(Res.string.progress_coverage_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val percentage = coverage.percentage
        if (percentage == null) {
            // 0/0 is "nothing to cover", not 0% covered, so say that rather than draw an empty bar.
            Text(
                text = stringResource(Res.string.progress_coverage_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            MetricFigure(text = formatAccuracy(percentage))
            Text(
                text = stringResource(
                    Res.string.progress_coverage_count,
                    coverage.attemptedQuestionCount,
                    coverage.totalQuestionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProgressMeter(
                // The exact count ratio, not the rounded percentage above it.
                fraction = coverage.attemptedQuestionCount.toFloat() / coverage.totalQuestionCount,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * The latest few assessments, kept visibly apart from the lifetime figures above: all-time accuracy
 * moves very slowly once history is long, so a learner who has improved needs a second, explicitly
 * labelled signal rather than a reweighted first one.
 *
 * The percentage is the domain's question-weighted accuracy across the whole window, not the mean of
 * the plotted attempts — a 1/1 attempt and a 10/20 attempt make 11/21, not 75%.
 */
@Composable
private fun RecentPerformanceSummary(recent: ProgressRecentPerformanceUiModel) {
    SecondarySummaryCard {
        Text(
            text = stringResource(Res.string.progress_recent_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        MetricFigure(
            text = formatAccuracy(recent.percentage),
            color = accuracyColor(recent.percentage),
        )
        Text(
            text = recentWindowLabel(recent.attemptCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                Res.string.progress_score,
                recent.correctAnswerCount,
                recent.answeredQuestionCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (val trend = recent.trend) {
            // Still real evidence, so the summary above stays; only the trajectory is withheld, and
            // as a plain statement rather than a warning about something the learner did wrong.
            is ProgressRecentTrendUiModel.InsufficientHistory -> Text(
                text = stringResource(
                    Res.string.progress_recent_trend_insufficient,
                    trend.requiredAttemptCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is ProgressRecentTrendUiModel.Available -> {
                Text(
                    text = stringResource(Res.string.progress_recent_trend_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val percentages = trend.attempts.map(ProgressRecentAttemptUiModel::percentage)
                RecentTrendChart(
                    percentages = percentages,
                    description = stringResource(
                        Res.string.progress_recent_trend_description,
                        percentages.joinToString(transform = ::formatAccuracy),
                    ),
                    modifier = Modifier.testTag(ProgressRecentTrendChartTag),
                )
            }
        }
    }
}

@Composable
private fun recentWindowLabel(attemptCount: Int): String =
    if (attemptCount == 1) {
        stringResource(Res.string.progress_recent_window_one)
    } else {
        stringResource(Res.string.progress_recent_window_other, attemptCount)
    }

/**
 * Reports the size of the mistake queue without offering to open it. Opening it is the Mistakes
 * navigation item's job, and that item carries the same count as a badge; a button here as well
 * gave the learner two controls for one destination sitting a few millimetres apart.
 *
 * It gets no practice shortcut either, for a different reason: this count spans the whole
 * curriculum, and focused practice has to name a Topic or Subtopic. Choosing one — the first, the
 * weakest, the one holding the most mistakes — would be a recommendation made silently on the
 * learner's behalf. Scoped mistake practice is offered where a scope is actually known, on a queue
 * entry in Mistake Review.
 */
@Composable
private fun UnresolvedMistakeSummary(unresolvedCount: Int) {
    val semantic = AppThemeExtras.semanticColors
    val resolved = unresolvedCount == 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (resolved) AppIcons.CheckCircle else AppIcons.Warning,
            contentDescription = null,
            tint = if (resolved) semantic.correct else semantic.incorrect,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = if (resolved) {
                stringResource(Res.string.mistake_review_none)
            } else {
                stringResource(Res.string.mistake_review_unresolved_count, unresolvedCount)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A weak area, still primarily a performance row.
 *
 * The row gains one low-emphasis text button rather than becoming a practice card or a click target
 * of its own: the section exists to report where the learner is struggling, and a whole card that
 * silently starts configuring practice would hide that meaning behind an unlabelled tap.
 *
 * The shortcut is offered because the row is here at all — the domain put it in the snapshot's weak
 * areas — so nothing about weakness is re-decided from the percentage this card displays.
 */
@Composable
private fun WeakAreaCard(
    area: WeakAreaUiModel,
    onPracticeClick: () -> Unit,
) {
    val title = when (area.type) {
        WeakAreaType.TOPIC ->
            area.title ?: stringResource(Res.string.progress_topic_unavailable)
        WeakAreaType.SUBTOPIC ->
            area.title ?: stringResource(Res.string.progress_subtopic_unavailable)
    }
    val subtitle = when {
        area.type != WeakAreaType.SUBTOPIC -> null
        area.title == null -> area.subtitle
        else -> area.subtitle ?: stringResource(Res.string.progress_topic_unavailable)
    }
    ProgressPerformanceCard(
        title = title,
        subtitle = subtitle,
        correctCount = area.correctCount,
        answeredCount = area.answeredCount,
        percentage = area.percentage,
        isWeak = true,
        action = {
            TextButton(
                onClick = onPracticeClick,
                modifier = Modifier.testTag(progressWeakAreaPracticeTag(area)),
            ) {
                Text(text = stringResource(Res.string.practice_shortcut_weak_area))
            }
        },
    )
}

@Composable
private fun TopicPerformanceCard(
    topic: ProgressTopicUiModel,
    onClick: () -> Unit,
) {
    ProgressPerformanceCard(
        title = topic.topicName ?: stringResource(Res.string.progress_topic_unavailable),
        subtitle = null,
        correctCount = topic.correctCount,
        answeredCount = topic.answeredCount,
        percentage = topic.percentage,
        modifier = Modifier
            .testTag(progressTopicCardTag(topic.topicId))
            .clickable(onClick = onClick),
        showChevron = true,
    )
}

@Composable
private fun HistoryCard(
    attempt: CompletedAttemptUiModel,
    onClick: () -> Unit,
) {
    PerformanceCard(
        title = when (attempt.assessmentType) {
            CompletedAssessmentType.MIXED -> stringResource(Res.string.mixed_interview_title)
            CompletedAssessmentType.FOCUSED -> stringResource(Res.string.progress_focused_practice)
        },
        detail = stringResource(
            Res.string.progress_score,
            attempt.correctAnswers,
            attempt.totalQuestions,
        ),
        percentage = attempt.percentage,
        modifier = Modifier
            .testTag(progressHistoryCardTag(attempt.attemptId))
            .clickable(onClick = onClick),
        subtitle = focusedScopeLabel(attempt.focusedScope),
        caption = attempt.completedAtText,
        showChevron = true,
    )
}

@Composable
private fun focusedScopeLabel(scope: FocusedScopeUiModel?): String? =
    when (scope) {
        null -> null
        is FocusedScopeUiModel.Topic ->
            scope.topicName ?: stringResource(Res.string.progress_topic_unavailable)
        is FocusedScopeUiModel.Subtopic -> {
            val subtopicName = scope.subtopicName
                ?: return stringResource(Res.string.progress_subtopic_unavailable)
            stringResource(
                Res.string.progress_focused_subtopic_scope,
                scope.topicName ?: stringResource(Res.string.progress_topic_unavailable),
                subtopicName,
            )
        }
    }

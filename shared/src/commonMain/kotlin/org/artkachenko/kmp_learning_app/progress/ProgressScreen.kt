package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_none
import kmp_learning_app.shared.generated.resources.mistake_review_unresolved_count
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import kmp_learning_app.shared.generated.resources.progress_accuracy_caption
import kmp_learning_app.shared.generated.resources.progress_completed_attempts_label
import kmp_learning_app.shared.generated.resources.progress_correct_answers_label
import kmp_learning_app.shared.generated.resources.progress_empty
import kmp_learning_app.shared.generated.resources.progress_empty_action
import kmp_learning_app.shared.generated.resources.progress_error
import kmp_learning_app.shared.generated.resources.progress_focused_practice
import kmp_learning_app.shared.generated.resources.progress_focused_subtopic_scope
import kmp_learning_app.shared.generated.resources.progress_history
import kmp_learning_app.shared.generated.resources.progress_loading
import kmp_learning_app.shared.generated.resources.progress_overall
import kmp_learning_app.shared.generated.resources.progress_questions_answered_label
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_subtopic_unavailable
import kmp_learning_app.shared.generated.resources.progress_title
import kmp_learning_app.shared.generated.resources.progress_topic_performance
import kmp_learning_app.shared.generated.resources.progress_topic_unavailable
import kmp_learning_app.shared.generated.resources.progress_weak_areas
import org.artkachenko.kmp_learning_app.ui.AccuracyHeadline
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.MetricRow
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

internal const val ProgressLoadingTag = "progress_loading"

/** Stable per-row handle so tests can target a Topic card without depending on label uniqueness. */
internal fun progressTopicCardTag(topicId: String): String = "progress_topic_card_$topicId"

/** Stable per-row handle for completed attempts whose visible labels may be identical. */
internal fun progressHistoryCardTag(attemptId: String): String = "progress_history_card_$attemptId"

@Composable
internal fun ProgressScreen(
    state: ProgressUiState,
    onBack: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onBrowseTopics: () -> Unit,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        AppTopBar(stringResource(Res.string.progress_title), onBack)
        when (state) {
            ProgressUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.progress_loading),
                testTag = ProgressLoadingTag,
                modifier = Modifier.weight(1f),
            )
            ProgressUiState.Empty -> ScreenAction(
                message = stringResource(Res.string.progress_empty),
                actionLabel = stringResource(Res.string.progress_empty_action),
                onAction = onBrowseTopics,
                modifier = Modifier.weight(1f),
                icon = AppIcons.Insights,
            )
            ProgressUiState.Error -> ScreenError(
                message = stringResource(Res.string.progress_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is ProgressUiState.Content -> ProgressContent(
                state = state,
                onTopicClick = onTopicClick,
                onHistoryClick = onHistoryClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProgressContent(
    state: ProgressUiState.Content,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProgressSectionTitle(stringResource(Res.string.progress_overall))
        }
        item {
            OverallSummary(state)
        }
        item {
            UnresolvedMistakeSummary(unresolvedCount = state.unresolvedMistakeCount)
        }
        if (state.weakAreas.isNotEmpty()) {
            item {
                ProgressSectionTitle(stringResource(Res.string.progress_weak_areas))
            }
            items(state.weakAreas, key = { "${it.type}:${it.stableId}" }) { area ->
                WeakAreaCard(area)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccuracyHeadline(
                percentage = state.percentage,
                caption = stringResource(Res.string.progress_accuracy_caption),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
}

/**
 * Reports the size of the mistake queue without offering to open it. Opening it is the Mistakes
 * navigation item's job, and that item carries the same count as a badge; a button here as well
 * gave the learner two controls for one destination sitting a few millimetres apart.
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

@Composable
private fun WeakAreaCard(area: WeakAreaUiModel) {
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

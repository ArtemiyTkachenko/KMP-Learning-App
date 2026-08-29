package org.artkachenko.kmp_learning_app.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_entry
import kmp_learning_app.shared.generated.resources.mixed_interview_title
import kmp_learning_app.shared.generated.resources.progress_accuracy
import kmp_learning_app.shared.generated.resources.progress_completed_attempts
import kmp_learning_app.shared.generated.resources.progress_correct_answers
import kmp_learning_app.shared.generated.resources.progress_empty
import kmp_learning_app.shared.generated.resources.progress_error
import kmp_learning_app.shared.generated.resources.progress_focused_practice
import kmp_learning_app.shared.generated.resources.progress_focused_subtopic_scope
import kmp_learning_app.shared.generated.resources.progress_history
import kmp_learning_app.shared.generated.resources.progress_loading
import kmp_learning_app.shared.generated.resources.progress_overall
import kmp_learning_app.shared.generated.resources.progress_percentage
import kmp_learning_app.shared.generated.resources.progress_questions_answered
import kmp_learning_app.shared.generated.resources.progress_retry
import kmp_learning_app.shared.generated.resources.progress_score
import kmp_learning_app.shared.generated.resources.progress_subtopic_unavailable
import kmp_learning_app.shared.generated.resources.progress_title
import kmp_learning_app.shared.generated.resources.progress_topic_performance
import kmp_learning_app.shared.generated.resources.progress_topic_unavailable
import kmp_learning_app.shared.generated.resources.progress_weak_areas
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyTopAppBar
import org.jetbrains.compose.resources.stringResource

internal const val ProgressLoadingTag = "progress_loading"

/** Stable per-row handle so tests can target a Topic card without depending on label uniqueness. */
internal fun progressTopicCardTag(topicId: String): String = "progress_topic_card_$topicId"

/** Stable per-row handle for completed attempts whose visible labels may be identical. */
internal fun progressHistoryCardTag(attemptId: String): String = "progress_history_card_$attemptId"

@Composable
internal fun ProgressScreen(
    state: ProgressUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onReviewMistakes: () -> Unit,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopicStudyTopAppBar(stringResource(Res.string.progress_title), onBack)
        when (state) {
            ProgressUiState.Loading -> ProgressMessage(Modifier.weight(1f)) {
                CircularProgressIndicator(Modifier.testTag(ProgressLoadingTag))
                Text(stringResource(Res.string.progress_loading))
            }
            ProgressUiState.Empty -> ProgressMessage(Modifier.weight(1f)) {
                Text(stringResource(Res.string.progress_empty))
            }
            ProgressUiState.Error -> ProgressMessage(Modifier.weight(1f)) {
                Text(stringResource(Res.string.progress_error))
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.progress_retry))
                }
            }
            is ProgressUiState.Content -> ProgressContent(
                state = state,
                onReviewMistakes = onReviewMistakes,
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
    onReviewMistakes: () -> Unit,
    onTopicClick: (String) -> Unit,
    onHistoryClick: (CompletedAssessmentType, String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ProgressSectionTitle(stringResource(Res.string.progress_overall))
        }
        item {
            OverallSummary(state)
        }
        item {
            // The queue owns its own empty state, so the dashboard never computes a mistake count
            // just to decide whether to offer this action.
            OutlinedButton(
                onClick = onReviewMistakes,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.mistake_review_entry))
            }
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

@Composable
private fun OverallSummary(state: ProgressUiState.Content) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(Res.string.progress_completed_attempts, state.completedAttemptCount))
            Text(stringResource(Res.string.progress_questions_answered, state.answeredQuestionCount))
            Text(stringResource(Res.string.progress_correct_answers, state.correctAnswerCount))
            Text(
                stringResource(
                    Res.string.progress_accuracy,
                    formatProgressPercentage(state.percentage),
                ),
                fontWeight = FontWeight.SemiBold,
            )
        }
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
    )
}

@Composable
private fun HistoryCard(
    attempt: CompletedAttemptUiModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(progressHistoryCardTag(attempt.attemptId))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = when (attempt.assessmentType) {
                    CompletedAssessmentType.MIXED -> stringResource(Res.string.mixed_interview_title)
                    CompletedAssessmentType.FOCUSED ->
                        stringResource(Res.string.progress_focused_practice)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            focusedScopeLabel(attempt.focusedScope)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(
                    Res.string.progress_score,
                    attempt.correctAnswers,
                    attempt.totalQuestions,
                ),
            )
            Text(
                stringResource(
                    Res.string.progress_percentage,
                    formatProgressPercentage(attempt.percentage),
                ),
            )
            Text(attempt.completedAtText, style = MaterialTheme.typography.bodySmall)
        }
    }
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

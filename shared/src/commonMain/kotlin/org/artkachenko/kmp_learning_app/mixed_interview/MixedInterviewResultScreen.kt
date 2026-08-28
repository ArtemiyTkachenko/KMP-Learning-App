package org.artkachenko.kmp_learning_app.mixed_interview

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_percentage
import kmp_learning_app.shared.generated.resources.mixed_result_attempt_not_found
import kmp_learning_app.shared.generated.resources.mixed_result_error
import kmp_learning_app.shared.generated.resources.mixed_result_loading
import kmp_learning_app.shared.generated.resources.mixed_result_not_completed
import kmp_learning_app.shared.generated.resources.mixed_result_performance_by_topic
import kmp_learning_app.shared.generated.resources.mixed_result_question_review
import kmp_learning_app.shared.generated.resources.mixed_result_retry
import kmp_learning_app.shared.generated.resources.mixed_result_title
import kmp_learning_app.shared.generated.resources.mixed_result_topic_score
import kmp_learning_app.shared.generated.resources.mixed_result_topic_unavailable
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentScoreSummary
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.UnresolvedReviewQuestionsNotice
import org.artkachenko.kmp_learning_app.topic_study.topic_detail.TopicStudyTopAppBar
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun MixedInterviewResultScreen(
    state: MixedInterviewResultUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopicStudyTopAppBar(stringResource(Res.string.mixed_result_title), onBack)
        when (state) {
            MixedInterviewResultUiState.Loading -> ResultMessage(Modifier.weight(1f)) {
                CircularProgressIndicator()
                Text(stringResource(Res.string.mixed_result_loading))
            }
            MixedInterviewResultUiState.AttemptNotFound -> ResultMessage(Modifier.weight(1f)) {
                Text(stringResource(Res.string.mixed_result_attempt_not_found))
            }
            MixedInterviewResultUiState.NotCompleted -> ResultMessage(Modifier.weight(1f)) {
                Text(stringResource(Res.string.mixed_result_not_completed))
            }
            MixedInterviewResultUiState.Error -> ResultMessage(Modifier.weight(1f)) {
                Text(stringResource(Res.string.mixed_result_error))
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.mixed_result_retry))
                }
            }
            is MixedInterviewResultUiState.Content -> MixedResultContent(
                state = state,
                onSourceClick = onSourceClick,
                failedSourceUrl = failedSourceUrl,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MixedResultContent(
    state: MixedInterviewResultUiState.Content,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AssessmentScoreSummary(
                correctAnswers = state.correctAnswers,
                totalQuestions = state.totalQuestions,
                percentage = state.percentage,
            )
            UnresolvedReviewQuestionsNotice(state.questions, state.totalQuestions)
        }
        item {
            Text(
                stringResource(Res.string.mixed_result_performance_by_topic),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(state.topicPerformance, key = { it.topicId }) { topic ->
            TopicPerformanceCard(topic)
        }
        item {
            Text(
                stringResource(Res.string.mixed_result_question_review),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(state.questions) { item ->
            when (item) {
                is ReviewQuestionItem.Available ->
                    ReviewQuestionCard(item.question, onSourceClick, failedSourceUrl)
                is ReviewQuestionItem.Missing -> MissingReviewQuestion(item.questionId)
            }
        }
    }
}

@Composable
private fun TopicPerformanceCard(
    topic: TopicPerformanceUiModel,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                topic.topicName ?: stringResource(Res.string.mixed_result_topic_unavailable),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    Res.string.mixed_result_topic_score,
                    topic.correctCount,
                    topic.questionCount,
                ),
            )
            Text(
                stringResource(
                    Res.string.assessment_review_percentage,
                    topic.percentage.roundToInt(),
                ),
            )
        }
    }
}

@Composable
private fun ResultMessage(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

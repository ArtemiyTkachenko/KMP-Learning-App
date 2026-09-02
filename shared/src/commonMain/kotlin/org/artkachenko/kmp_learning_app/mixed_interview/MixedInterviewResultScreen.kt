package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mixed_result_attempt_not_found
import kmp_learning_app.shared.generated.resources.mixed_result_error
import kmp_learning_app.shared.generated.resources.mixed_result_loading
import kmp_learning_app.shared.generated.resources.mixed_result_not_completed
import kmp_learning_app.shared.generated.resources.mixed_result_performance_by_topic
import kmp_learning_app.shared.generated.resources.mixed_result_practice_again
import kmp_learning_app.shared.generated.resources.mixed_result_practice_starting
import kmp_learning_app.shared.generated.resources.mixed_result_question_review
import kmp_learning_app.shared.generated.resources.mixed_result_repeat_error
import kmp_learning_app.shared.generated.resources.mixed_result_repeat_no_questions
import kmp_learning_app.shared.generated.resources.mixed_result_repeat_source_missing
import kmp_learning_app.shared.generated.resources.mixed_result_title
import kmp_learning_app.shared.generated.resources.mixed_result_topic_score
import kmp_learning_app.shared.generated.resources.mixed_result_topic_unavailable
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentScoreSummary
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.UnresolvedReviewQuestionsNotice
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.jetbrains.compose.resources.stringResource

internal const val MixedResultLoadingTag = "mixed_result_loading"
internal const val MixedResultPracticeAgainTag = "mixed_result_practice_again"
internal const val MixedResultCreatingIndicatorTag = "mixed_result_creating_indicator"

@Composable
internal fun MixedInterviewResultScreen(
    state: MixedInterviewResultUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSourceClick: (String) -> Unit,
    onRepeatInterview: () -> Unit = {},
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.mixed_result_title), onBack, scrollBehavior)
        when (state) {
            MixedInterviewResultUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.mixed_result_loading),
                testTag = MixedResultLoadingTag,
                modifier = Modifier.weight(1f),
            )
            MixedInterviewResultUiState.AttemptNotFound -> ScreenMessage(
                message = stringResource(Res.string.mixed_result_attempt_not_found),
                modifier = Modifier.weight(1f),
            )
            MixedInterviewResultUiState.NotCompleted -> ScreenMessage(
                message = stringResource(Res.string.mixed_result_not_completed),
                modifier = Modifier.weight(1f),
            )
            MixedInterviewResultUiState.Error -> ScreenError(
                message = stringResource(Res.string.mixed_result_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is MixedInterviewResultUiState.Content -> MixedResultContent(
                state = state,
                onSourceClick = onSourceClick,
                onRepeatInterview = onRepeatInterview,
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
    onRepeatInterview: () -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AssessmentScoreSummary(
                correctAnswers = state.correctAnswers,
                totalQuestions = state.totalQuestions,
                percentage = state.percentage,
            )
            UnresolvedReviewQuestionsNotice(state.questions, state.totalQuestions)
            when (state.repeatInterviewState) {
                RepeatInterviewState.Idle -> Unit
                RepeatInterviewState.Creating ->
                    Text(stringResource(Res.string.mixed_result_practice_starting))
                RepeatInterviewState.SourceAttemptNotFound ->
                    Text(
                        stringResource(Res.string.mixed_result_repeat_source_missing),
                        color = MaterialTheme.colorScheme.error,
                    )
                RepeatInterviewState.NoEligibleQuestions ->
                    Text(
                        stringResource(Res.string.mixed_result_repeat_no_questions),
                        color = MaterialTheme.colorScheme.error,
                    )
                RepeatInterviewState.Error ->
                    Text(
                        stringResource(Res.string.mixed_result_repeat_error),
                        color = MaterialTheme.colorScheme.error,
                    )
            }
            Button(
                onClick = onRepeatInterview,
                enabled = state.repeatInterviewState != RepeatInterviewState.Creating,
                modifier = Modifier.testTag(MixedResultPracticeAgainTag),
            ) {
                if (state.repeatInterviewState == RepeatInterviewState.Creating) {
                    CircularProgressIndicator(Modifier.testTag(MixedResultCreatingIndicatorTag))
                } else {
                    Text(stringResource(Res.string.mixed_result_practice_again))
                }
            }
        }
        item {
            SectionHeading(stringResource(Res.string.mixed_result_performance_by_topic))
        }
        items(state.topicPerformance, key = { it.topicId }) { topic ->
            TopicPerformanceCard(topic)
        }
        item {
            SectionHeading(stringResource(Res.string.mixed_result_question_review))
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
    PerformanceCard(
        title = topic.topicName ?: stringResource(Res.string.mixed_result_topic_unavailable),
        detail = stringResource(
            Res.string.mixed_result_topic_score,
            topic.correctCount,
            topic.questionCount,
        ),
        percentage = topic.percentage,
    )
}

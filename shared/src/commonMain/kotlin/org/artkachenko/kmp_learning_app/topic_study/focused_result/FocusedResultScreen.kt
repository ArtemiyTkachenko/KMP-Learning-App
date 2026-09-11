package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.focused_result_attempt_not_found
import kmp_learning_app.shared.generated.resources.focused_result_error
import kmp_learning_app.shared.generated.resources.focused_result_loading
import kmp_learning_app.shared.generated.resources.focused_result_not_completed
import kmp_learning_app.shared.generated.resources.focused_result_title
import kmp_learning_app.shared.generated.resources.focused_result_practice_again
import kmp_learning_app.shared.generated.resources.focused_result_practice_starting
import kmp_learning_app.shared.generated.resources.focused_result_repeat_source_missing
import kmp_learning_app.shared.generated.resources.focused_result_repeat_no_questions
import kmp_learning_app.shared.generated.resources.focused_result_repeat_error
import kmp_learning_app.shared.generated.resources.assessment_review_practice_complete
import kmp_learning_app.shared.generated.resources.assessment_review_question_review
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentScoreSummary
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.UnresolvedReviewQuestionsNotice
import org.artkachenko.kmp_learning_app.assessment_review.MistakeRetentionNotice
import org.artkachenko.kmp_learning_app.assessment_review.reviewSaveAction
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.PaddingValues
import kmp_learning_app.shared.generated.resources.focused_result_loading
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing

internal const val FocusedResultLoadingTag = "focused_result_loading"
internal const val FocusedResultPracticeAgainTag = "focused_result_practice_again"

@Composable
internal fun FocusedResultScreen(
    state: FocusedResultUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSourceClick: (String) -> Unit,
    onRepeatPractice: () -> Unit,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)? = null,
    savedQuestions: SavedQuestionsState = SavedQuestionsState.Loading,
    onToggleSaved: (String) -> Unit = {},
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.focused_result_title), onBack, scrollBehavior)
        when (state) {
            FocusedResultUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.focused_result_loading),
                testTag = FocusedResultLoadingTag,
                modifier = Modifier.weight(1f),
            )
            FocusedResultUiState.AttemptNotFound -> ScreenMessage(
                message = stringResource(Res.string.focused_result_attempt_not_found),
                modifier = Modifier.weight(1f),
            )
            FocusedResultUiState.NotCompleted -> ScreenMessage(
                message = stringResource(Res.string.focused_result_not_completed),
                modifier = Modifier.weight(1f),
            )
            FocusedResultUiState.Error -> ScreenError(
                message = stringResource(Res.string.focused_result_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is FocusedResultUiState.Content -> ResultContent(
                state = state,
                onSourceClick = onSourceClick,
                onRepeatPractice = onRepeatPractice,
                onPracticeMistakes = onPracticeMistakes,
                savedQuestions = savedQuestions,
                onToggleSaved = onToggleSaved,
                failedSourceUrl = failedSourceUrl,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ResultContent(
    state: FocusedResultUiState.Content,
    onSourceClick: (String) -> Unit,
    onRepeatPractice: () -> Unit,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)?,
    savedQuestions: SavedQuestionsState,
    onToggleSaved: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable)) {
                AssessmentScoreSummary(
                    correctAnswers = state.correctAnswers,
                    totalQuestions = state.totalQuestions,
                    percentage = state.percentage,
                    title = stringResource(Res.string.assessment_review_practice_complete),
                )
                UnresolvedReviewQuestionsNotice(state.questions, state.totalQuestions)
                MistakeRetentionNotice(state.questions, onPracticeMistakes)
                when (state.repeatPracticeState) {
                    RepeatPracticeState.Idle -> Unit
                    RepeatPracticeState.Creating -> Text(stringResource(Res.string.focused_result_practice_starting))
                    RepeatPracticeState.SourceAttemptNotFound -> Text(stringResource(Res.string.focused_result_repeat_source_missing), color = MaterialTheme.colorScheme.error)
                    RepeatPracticeState.NoEligibleQuestions -> Text(stringResource(Res.string.focused_result_repeat_no_questions), color = MaterialTheme.colorScheme.error)
                    RepeatPracticeState.Error -> Text(stringResource(Res.string.focused_result_repeat_error), color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(
                    onClick = onRepeatPractice,
                    enabled = state.repeatPracticeState != RepeatPracticeState.Creating,
                    modifier = Modifier.testTag(FocusedResultPracticeAgainTag),
                ) {
                    if (state.repeatPracticeState == RepeatPracticeState.Creating) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(Res.string.focused_result_practice_again))
                    }
                }
            }
        }
        item {
            SectionHeading(
                stringResource(Res.string.assessment_review_question_review),
                topPadding = AppSpacing.Related,
            )
        }
        items(state.questions) { item ->
            when (item) {
                // A Question the curriculum no longer holds is not review content the learner can
                // act on, so the placeholder gets no save action.
                is ReviewQuestionItem.Missing -> MissingReviewQuestion(item.questionId)
                is ReviewQuestionItem.Available -> ReviewQuestionCard(
                    question = item.question,
                    onSourceClick = onSourceClick,
                    failedSourceUrl = failedSourceUrl,
                    saveAction = savedQuestions.reviewSaveAction(
                        questionId = item.question.questionId,
                        onToggleSaved = onToggleSaved,
                    ),
                )
            }
        }
    }
}

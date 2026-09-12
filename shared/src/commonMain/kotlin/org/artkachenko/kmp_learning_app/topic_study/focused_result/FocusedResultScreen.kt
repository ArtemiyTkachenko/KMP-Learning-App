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
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

internal const val FocusedResultLoadingTag = "focused_result_loading"
internal const val FocusedResultPracticeAgainTag = "focused_result_practice_again"

/** The two panes of the expanded result, named for the same reason the Progress panes are. */
internal const val FocusedResultSummaryPaneTag = "focused_result_summary_pane"
internal const val FocusedResultReviewPaneTag = "focused_result_review_pane"

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
        AppScreenPane(AppContentWidth.Paned) {
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
}

/**
 * The result, as one scroll or as two panes.
 *
 * The same split the Interview result takes, and for the same reason: the outcome and what follows
 * from it are short and worth keeping in view, and the transcript is long and meant to be read
 * through. On a phone they are one column with the outcome at the top; on a desktop the outcome
 * stops scrolling away. Nothing about answer review changes with the arrangement — the transcript
 * pane holds the same cards in the same order.
 */
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
    if (LocalAppWindowSizeClass.current.isExpanded) {
        AppTwoPaneRow(
            modifier = modifier,
            primary = {
                ResultPane(Modifier.weight(1f).testTag(FocusedResultSummaryPaneTag)) {
                    outcomeSection(
                        state = state,
                        onRepeatPractice = onRepeatPractice,
                        onPracticeMistakes = onPracticeMistakes,
                    )
                }
            },
            secondary = {
                ResultPane(Modifier.weight(1f).testTag(FocusedResultReviewPaneTag)) {
                    reviewSection(
                        state = state,
                        onSourceClick = onSourceClick,
                        savedQuestions = savedQuestions,
                        onToggleSaved = onToggleSaved,
                        failedSourceUrl = failedSourceUrl,
                    )
                }
            },
        )
        return
    }
    ResultPane(modifier) {
        outcomeSection(
            state = state,
            onRepeatPractice = onRepeatPractice,
            onPracticeMistakes = onPracticeMistakes,
        )
        reviewSection(
            state = state,
            onSourceClick = onSourceClick,
            savedQuestions = savedQuestions,
            onToggleSaved = onToggleSaved,
            failedSourceUrl = failedSourceUrl,
        )
    }
}

/** One column of the result, with the same padding and rhythm in either arrangement. */
@Composable
private fun ResultPane(
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable),
        content = content,
    )
}

/** What happened, and what the learner can do about it. */
private fun LazyListScope.outcomeSection(
    state: FocusedResultUiState.Content,
    onRepeatPractice: () -> Unit,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)?,
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
                RepeatPracticeState.Creating ->
                    Text(stringResource(Res.string.focused_result_practice_starting))
                RepeatPracticeState.SourceAttemptNotFound ->
                    Text(
                        stringResource(Res.string.focused_result_repeat_source_missing),
                        color = MaterialTheme.colorScheme.error,
                    )
                RepeatPracticeState.NoEligibleQuestions ->
                    Text(
                        stringResource(Res.string.focused_result_repeat_no_questions),
                        color = MaterialTheme.colorScheme.error,
                    )
                RepeatPracticeState.Error ->
                    Text(
                        stringResource(Res.string.focused_result_repeat_error),
                        color = MaterialTheme.colorScheme.error,
                    )
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
}

/** The transcript: every question as it was answered, in the order it was asked. */
private fun LazyListScope.reviewSection(
    state: FocusedResultUiState.Content,
    onSourceClick: (String) -> Unit,
    savedQuestions: SavedQuestionsState,
    onToggleSaved: (String) -> Unit,
    failedSourceUrl: String?,
) {
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

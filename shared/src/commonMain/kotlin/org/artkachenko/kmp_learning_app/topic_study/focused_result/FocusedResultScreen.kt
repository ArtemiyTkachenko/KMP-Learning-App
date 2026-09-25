package org.artkachenko.kmp_learning_app.topic_study.focused_result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
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
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentCompletionHero
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentRetakeAction
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentRetakeWording
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.UnresolvedReviewQuestionsNotice
import org.artkachenko.kmp_learning_app.assessment_review.MistakeRetentionNotice
import org.artkachenko.kmp_learning_app.assessment_review.reviewSaveAction
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.retake.AssessmentRetakeState
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState
import org.jetbrains.compose.resources.stringResource
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentResultLayout
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane

internal const val FocusedResultLoadingTag = "focused_result_loading"
internal const val FocusedResultPracticeAgainTag = "focused_result_practice_again"
internal const val FocusedResultCreatingIndicatorTag = "focused_result_creating_indicator"

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
    retakeState: AssessmentRetakeState = AssessmentRetakeState.Idle,
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
                    retakeState = retakeState,
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
    retakeState: AssessmentRetakeState,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)?,
    savedQuestions: SavedQuestionsState,
    onToggleSaved: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    AssessmentResultLayout(
        modifier = modifier,
        summaryPaneModifier = Modifier.testTag(FocusedResultSummaryPaneTag),
        reviewPaneModifier = Modifier.testTag(FocusedResultReviewPaneTag),
        summary = {
            outcomeSection(
                state = state,
                onRepeatPractice = onRepeatPractice,
                retakeState = retakeState,
                onPracticeMistakes = onPracticeMistakes,
            )
        },
        review = {
            reviewSection(
                state = state,
                onSourceClick = onSourceClick,
                savedQuestions = savedQuestions,
                onToggleSaved = onToggleSaved,
                failedSourceUrl = failedSourceUrl,
            )
        },
    )
}

/** What happened, and what the learner can do about it. */
private fun LazyListScope.outcomeSection(
    state: FocusedResultUiState.Content,
    onRepeatPractice: () -> Unit,
    retakeState: AssessmentRetakeState,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)?,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable)) {
            AssessmentCompletionHero(
                correctAnswers = state.correctAnswers,
                totalQuestions = state.totalQuestions,
                percentage = state.percentage,
                title = stringResource(Res.string.assessment_review_practice_complete),
            )
            UnresolvedReviewQuestionsNotice(state.questions, state.totalQuestions)
            MistakeRetentionNotice(state.questions, onPracticeMistakes)
            AssessmentRetakeAction(
                state = retakeState,
                wording = AssessmentRetakeWording(
                    action = stringResource(Res.string.focused_result_practice_again),
                    starting = stringResource(Res.string.focused_result_practice_starting),
                    sourceMissing = stringResource(Res.string.focused_result_repeat_source_missing),
                    noQuestions = stringResource(Res.string.focused_result_repeat_no_questions),
                    error = stringResource(Res.string.focused_result_repeat_error),
                ),
                onRetake = onRepeatPractice,
                actionTestTag = FocusedResultPracticeAgainTag,
                progressTestTag = FocusedResultCreatingIndicatorTag,
            )
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

package org.artkachenko.kmp_learning_app.mixed_interview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import kmp_learning_app.shared.generated.resources.assessment_review_interview_complete
import org.artkachenko.kmp_learning_app.assessment_review.AssessmentScoreSummary
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.assessment_review.UnresolvedReviewQuestionsNotice
import org.artkachenko.kmp_learning_app.assessment_review.MistakeRetentionNotice
import org.artkachenko.kmp_learning_app.assessment_review.reviewSaveAction
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.PerformanceCard
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.SectionHeading
import org.jetbrains.compose.resources.stringResource
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import org.artkachenko.kmp_learning_app.ui.AppTwoPaneRow
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.LocalAppWindowSizeClass

internal const val MixedResultLoadingTag = "mixed_result_loading"
internal const val MixedResultPracticeAgainTag = "mixed_result_practice_again"
internal const val MixedResultCreatingIndicatorTag = "mixed_result_creating_indicator"

/** The two panes of the expanded result, named for the same reason the Progress panes are. */
internal const val MixedResultSummaryPaneTag = "mixed_result_summary_pane"
internal const val MixedResultReviewPaneTag = "mixed_result_review_pane"

@Composable
internal fun MixedInterviewResultScreen(
    state: MixedInterviewResultUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSourceClick: (String) -> Unit,
    onRepeatInterview: () -> Unit = {},
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)? = null,
    savedQuestions: SavedQuestionsState = SavedQuestionsState.Loading,
    onToggleSaved: (String) -> Unit = {},
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.mixed_result_title), onBack, scrollBehavior)
        AppScreenPane(AppContentWidth.Paned) {
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
 * An Interview transcript is the longest single thing in the app: twenty questions, each with its
 * options, the learner's answer, the correct answer, an explanation, and its sources. On a phone
 * that is unavoidably one long scroll, and the score and the next actions sit at the top of it. On
 * a desktop the same arrangement wastes the window and makes the summary something the learner has
 * to scroll back up to — so the outcome, the notices about it, the way to take it again, and the
 * per-Topic breakdown take a pane of their own, and the transcript scrolls beside them.
 *
 * This is deliberately not a master/detail list with one question shown at a time. Master/detail
 * would mean introducing selection state for a document the learner is meant to read through, and
 * it would hide nineteen of twenty reviews behind a click each — for a screen whose whole purpose
 * is to go back over what happened. Every P0 review rule survives a two-pane split untouched,
 * because the transcript pane holds the same `ReviewQuestionCard` list in the same order: mistakes
 * keep their marking, correct answers keep their lower emphasis, the four-state multi-select review
 * is the card's own, and explanations, sources, and study links are all still on the card.
 */
@Composable
private fun MixedResultContent(
    state: MixedInterviewResultUiState.Content,
    onSourceClick: (String) -> Unit,
    onRepeatInterview: () -> Unit,
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
                ResultPane(Modifier.weight(1f).testTag(MixedResultSummaryPaneTag)) {
                    outcomeSection(
                        state = state,
                        onRepeatInterview = onRepeatInterview,
                        onPracticeMistakes = onPracticeMistakes,
                    )
                }
            },
            secondary = {
                ResultPane(Modifier.weight(1f).testTag(MixedResultReviewPaneTag)) {
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
            onRepeatInterview = onRepeatInterview,
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

/** What happened: the score, what follows from it, and where it went well or badly. */
private fun LazyListScope.outcomeSection(
    state: MixedInterviewResultUiState.Content,
    onRepeatInterview: () -> Unit,
    onPracticeMistakes: ((AssessmentConfig.Focused) -> Unit)?,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Comfortable)) {
            AssessmentScoreSummary(
                correctAnswers = state.correctAnswers,
                totalQuestions = state.totalQuestions,
                percentage = state.percentage,
                title = stringResource(Res.string.assessment_review_interview_complete),
            )
            UnresolvedReviewQuestionsNotice(state.questions, state.totalQuestions)
            MistakeRetentionNotice(state.questions, onPracticeMistakes)
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
            OutlinedButton(
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
    }
    item {
        SectionHeading(
            stringResource(Res.string.mixed_result_performance_by_topic),
            topPadding = AppSpacing.Related,
        )
    }
    items(state.topicPerformance, key = { it.topicId }) { topic ->
        TopicPerformanceCard(topic)
    }
}

/** The transcript: every question as it was answered, in the order it was asked. */
private fun LazyListScope.reviewSection(
    state: MixedInterviewResultUiState.Content,
    onSourceClick: (String) -> Unit,
    savedQuestions: SavedQuestionsState,
    onToggleSaved: (String) -> Unit,
    failedSourceUrl: String?,
) {
    item {
        SectionHeading(
            stringResource(Res.string.mixed_result_question_review),
            topPadding = AppSpacing.Related,
        )
    }
    items(state.questions) { item ->
        when (item) {
            is ReviewQuestionItem.Available -> ReviewQuestionCard(
                question = item.question,
                onSourceClick = onSourceClick,
                failedSourceUrl = failedSourceUrl,
                // The same shared card and the same shared saved state as the other review
                // surfaces; Mixed results do not bookmark by their own rules.
                saveAction = savedQuestions.reviewSaveAction(
                    questionId = item.question.questionId,
                    onToggleSaved = onToggleSaved,
                ),
            )
            is ReviewQuestionItem.Missing -> MissingReviewQuestion(item.questionId)
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

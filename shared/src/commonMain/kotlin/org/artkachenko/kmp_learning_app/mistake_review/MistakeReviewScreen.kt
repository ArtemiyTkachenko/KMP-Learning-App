package org.artkachenko.kmp_learning_app.mistake_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.mistake_review_description
import kmp_learning_app.shared.generated.resources.mistake_review_empty
import kmp_learning_app.shared.generated.resources.mistake_review_empty_action
import kmp_learning_app.shared.generated.resources.mistake_review_empty_detail
import kmp_learning_app.shared.generated.resources.mistake_review_error
import kmp_learning_app.shared.generated.resources.mistake_review_loading
import kmp_learning_app.shared.generated.resources.mistake_review_title
import kmp_learning_app.shared.generated.resources.practice_shortcut_subtopic_mistakes
import org.artkachenko.kmp_learning_app.assessment.AssessmentScope
import org.artkachenko.kmp_learning_app.assessment.PracticeQuestionSource
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionCard
import org.artkachenko.kmp_learning_app.assessment_review.ReviewQuestionItem
import org.artkachenko.kmp_learning_app.guided_learning.PracticePreset
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenMessage
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.jetbrains.compose.resources.stringResource

internal const val MistakeReviewLoadingTag = "mistake_review_loading"

/** Stable per-entry handle for the scoped practice shortcut, whose label repeats on every card. */
internal fun mistakePracticeShortcutTag(questionId: String): String =
    "mistake_review_practice_$questionId"

/**
 * [onPracticePreset] carries the Subtopic the tapped entry already belongs to, together with the
 * existing unresolved-mistake source. The queue itself is unchanged: which Questions are unresolved
 * remains `UnresolvedMistakeDerivation`'s answer, and which of a Subtopic's unresolved Questions are
 * currently eligible remains the selector's.
 */
@Composable
internal fun MistakeReviewScreen(
    state: MistakeReviewUiState,
    onBack: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onBrowseTopics: () -> Unit,
    onSourceClick: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.mistake_review_title), onBack, scrollBehavior)
        when (state) {
            MistakeReviewUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.mistake_review_loading),
                testTag = MistakeReviewLoadingTag,
                modifier = Modifier.weight(1f),
            )
            MistakeReviewUiState.Empty -> ScreenAction(
                message = stringResource(Res.string.mistake_review_empty),
                actionLabel = stringResource(Res.string.mistake_review_empty_action),
                onAction = onBrowseTopics,
                modifier = Modifier.weight(1f),
                detail = stringResource(Res.string.mistake_review_empty_detail),
                icon = AppIcons.CheckCircle,
                iconTint = AppThemeExtras.semanticColors.correct,
            )
            MistakeReviewUiState.Error -> ScreenError(
                message = stringResource(Res.string.mistake_review_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is MistakeReviewUiState.Content -> MistakeReviewContent(
                state = state,
                onSourceClick = onSourceClick,
                onPracticePreset = onPracticePreset,
                failedSourceUrl = failedSourceUrl,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MistakeReviewContent(
    state: MistakeReviewUiState.Content,
    onSourceClick: (String) -> Unit,
    onPracticePreset: (PracticePreset) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.mistake_review_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        // Review rendering is reused from the shared assessment-review components so selected
        // answers, correct answers, explanation, and sources stay consistent with result screens.
        // A mistake leaves this list the moment it is answered correctly elsewhere, so entries are
        // genuinely removed while the learner is looking at them. Animating the removal is what
        // shows which one resolved; without it the remaining cards simply jump up a slot.
        items(state.mistakes, key = UnresolvedMistake::questionId) { mistake ->
            when (val item = mistake.reviewItem) {
                is ReviewQuestionItem.Available -> Column(
                    modifier = Modifier.animateItem(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ReviewQuestionCard(
                        question = item.question,
                        onSourceClick = onSourceClick,
                        failedSourceUrl = failedSourceUrl,
                    )
                    // Secondary to the explanation above it, and offered per entry rather than for
                    // the queue as a whole: this Question names its own Subtopic, so the scope is
                    // read off the card the learner is looking at instead of being ranked out of
                    // the queue. The clicked Question is context, not a candidate list — nothing
                    // about which Questions the run will draw travels with it.
                    if (item.question.subtopicId.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onPracticePreset(
                                    PracticePreset(
                                        scope = AssessmentScope.Subtopic(item.question.subtopicId),
                                        source = PracticeQuestionSource.UNRESOLVED_MISTAKES,
                                    ),
                                )
                            },
                            modifier = Modifier.testTag(
                                mistakePracticeShortcutTag(item.question.questionId),
                            ),
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.practice_shortcut_subtopic_mistakes,
                                ),
                            )
                        }
                    }
                }
                // A Question the curriculum no longer holds cannot name a current scope, so it gets
                // no shortcut rather than one built from metadata that is not there.
                is ReviewQuestionItem.Missing -> MissingReviewQuestion(
                    questionId = item.questionId,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

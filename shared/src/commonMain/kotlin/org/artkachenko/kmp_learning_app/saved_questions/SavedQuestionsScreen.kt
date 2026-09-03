package org.artkachenko.kmp_learning_app.saved_questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_correct_answer
import kmp_learning_app.shared.generated.resources.saved_questions_description
import kmp_learning_app.shared.generated.resources.saved_questions_empty
import kmp_learning_app.shared.generated.resources.saved_questions_empty_action
import kmp_learning_app.shared.generated.resources.saved_questions_empty_detail
import kmp_learning_app.shared.generated.resources.saved_questions_error
import kmp_learning_app.shared.generated.resources.saved_questions_loading
import kmp_learning_app.shared.generated.resources.saved_questions_remove
import kmp_learning_app.shared.generated.resources.saved_questions_title
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.QuestionAnswerOption
import org.artkachenko.kmp_learning_app.assessment_review.QuestionAnswerTag
import org.artkachenko.kmp_learning_app.assessment_review.QuestionExplanationBlock
import org.artkachenko.kmp_learning_app.assessment_review.QuestionSources
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.stringResource

internal const val SavedQuestionsLoadingTag = "saved_questions_loading"

/** Stable per-entry handle for the removal action, whose label repeats on every card. */
internal fun savedQuestionRemoveTag(questionId: String): String =
    "saved_question_remove_$questionId"

/**
 * Deliberate Question review, not attempt review.
 *
 * Nothing on this screen says Correct, Incorrect, or Your answer, because a saved Question is not
 * tied to an occurrence: the learner may have saved it having answered it either way, and E18-03
 * defines no attempt to attach. What the authored content does say — which options are correct, the
 * explanation, the sources — is shown in the same presentation the result screens use.
 */
@Composable
internal fun SavedQuestionsScreen(
    state: SavedQuestionsUiState,
    onBack: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onBrowseTopics: () -> Unit,
    onRemoveSaved: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = rememberAppTopBarScrollBehavior()
    Column(modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AppTopBar(stringResource(Res.string.saved_questions_title), onBack, scrollBehavior)
        when (state) {
            SavedQuestionsUiState.Loading -> ScreenLoading(
                message = stringResource(Res.string.saved_questions_loading),
                testTag = SavedQuestionsLoadingTag,
                modifier = Modifier.weight(1f),
            )
            // An empty collection is a normal state with a way forward, not a failure: the learner
            // has simply not saved anything yet, and the place to do that is a Question.
            SavedQuestionsUiState.Empty -> ScreenAction(
                message = stringResource(Res.string.saved_questions_empty),
                actionLabel = stringResource(Res.string.saved_questions_empty_action),
                onAction = onBrowseTopics,
                modifier = Modifier.weight(1f),
                detail = stringResource(Res.string.saved_questions_empty_detail),
            )
            SavedQuestionsUiState.Error -> ScreenError(
                message = stringResource(Res.string.saved_questions_error),
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
            is SavedQuestionsUiState.Content -> SavedQuestionsContent(
                state = state,
                onRemoveSaved = onRemoveSaved,
                onSourceClick = onSourceClick,
                failedSourceUrl = failedSourceUrl,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SavedQuestionsContent(
    state: SavedQuestionsUiState.Content,
    onRemoveSaved: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = appScreenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
    ) {
        item {
            Text(
                text = stringResource(Res.string.saved_questions_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = AppSpacing.Related),
            )
        }
        // The repository's saved order, rendered as given. Nothing here re-sorts by content status,
        // Topic, or text: what the learner saved most recently is what they see first.
        items(state.items, key = SavedQuestionItem::questionId) { item ->
            val isPending = item.questionId in state.pendingQuestionIds
            when (item) {
                is SavedQuestionItem.Available -> SavedQuestionCard(
                    question = item.question,
                    isRemovalPending = isPending,
                    onRemoveSaved = onRemoveSaved,
                    onSourceClick = onSourceClick,
                    failedSourceUrl = failedSourceUrl,
                    modifier = Modifier.animateItem(),
                )
                // Unlike a missing placeholder on a result screen, which offers no save action
                // because there is nothing to review, this one is already saved: without a way to
                // remove it the identity would be impossible to get rid of.
                is SavedQuestionItem.Missing -> MissingSavedQuestion(
                    questionId = item.questionId,
                    isRemovalPending = isPending,
                    onRemoveSaved = onRemoveSaved,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SavedQuestionCard(
    question: SavedQuestionContentUiModel,
    isRemovalPending: Boolean,
    onRemoveSaved: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    failedSourceUrl: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            Modifier.padding(AppSpacing.Comfortable),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    question.text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                RemoveSavedAction(
                    questionId = question.questionId,
                    isPending = isRemovalPending,
                    onRemoveSaved = onRemoveSaved,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Related)) {
                question.answers.forEach { SavedQuestionAnswerRow(it) }
            }
            QuestionExplanationBlock(question.explanation)
            QuestionSources(
                sources = question.sources,
                onSourceClick = onSourceClick,
                failedSourceUrl = failedSourceUrl,
            )
        }
    }
}

/**
 * An answer option marked only by whether the curriculum authored it as correct.
 *
 * The correct options take the same outline the result screens give a correct answer the learner
 * did not pick, which is the one treatment that carries no claim about a selection.
 */
@Composable
private fun SavedQuestionAnswerRow(answer: SavedQuestionAnswerUiModel) {
    val semantic = AppThemeExtras.semanticColors
    QuestionAnswerOption(
        text = answer.text,
        borderColor = if (answer.isCorrectAnswer) {
            semantic.correct
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        tags = if (answer.isCorrectAnswer) {
            {
                QuestionAnswerTag(
                    text = stringResource(Res.string.assessment_review_correct_answer),
                    color = semantic.correct,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun MissingSavedQuestion(
    questionId: String,
    isRemovalPending: Boolean,
    onRemoveSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight)) {
        // The same treatment a review surface gives content the curriculum no longer holds. No
        // Question text or answers are invented to fill the card, and the row is not removed for
        // the learner either: the saved identity is still theirs.
        MissingReviewQuestion(questionId = questionId)
        RemoveSavedAction(
            questionId = questionId,
            isPending = isRemovalPending,
            onRemoveSaved = onRemoveSaved,
        )
    }
}

/** Disabled only while this Question's own removal is being persisted. */
@Composable
private fun RemoveSavedAction(
    questionId: String,
    isPending: Boolean,
    onRemoveSaved: (String) -> Unit,
) {
    TextButton(
        onClick = { onRemoveSaved(questionId) },
        enabled = !isPending,
        modifier = Modifier.testTag(savedQuestionRemoveTag(questionId)),
    ) {
        Text(stringResource(Res.string.saved_questions_remove))
    }
}

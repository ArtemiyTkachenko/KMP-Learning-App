package org.artkachenko.kmp_learning_app.saved_questions

import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.assessment_review_correct_answer
import kmp_learning_app.shared.generated.resources.saved_questions_count
import kmp_learning_app.shared.generated.resources.saved_questions_description
import kmp_learning_app.shared.generated.resources.saved_questions_empty
import kmp_learning_app.shared.generated.resources.saved_questions_empty_action
import kmp_learning_app.shared.generated.resources.saved_questions_empty_detail
import kmp_learning_app.shared.generated.resources.saved_questions_error
import kmp_learning_app.shared.generated.resources.saved_questions_loading
import kmp_learning_app.shared.generated.resources.saved_questions_title
import org.artkachenko.kmp_learning_app.assessment_review.MissingReviewQuestion
import org.artkachenko.kmp_learning_app.assessment_review.QuestionAnswerOption
import org.artkachenko.kmp_learning_app.assessment_review.QuestionAnswerTag
import org.artkachenko.kmp_learning_app.assessment_review.QuestionAnswersRevealDelayMillis
import org.artkachenko.kmp_learning_app.assessment_review.QuestionBookmarkAction
import org.artkachenko.kmp_learning_app.assessment_review.QuestionDisclosure
import org.artkachenko.kmp_learning_app.assessment_review.QuestionExplanationBlock
import org.artkachenko.kmp_learning_app.assessment_review.QuestionExplanationRevealDelayMillis
import org.artkachenko.kmp_learning_app.assessment_review.QuestionSources
import org.artkachenko.kmp_learning_app.ui.AppIcons
import org.artkachenko.kmp_learning_app.ui.AppTopBar
import org.artkachenko.kmp_learning_app.ui.ScreenAction
import org.artkachenko.kmp_learning_app.ui.ScreenError
import org.artkachenko.kmp_learning_app.ui.ScreenLoading
import org.artkachenko.kmp_learning_app.ui.ScreenStateTransition
import org.artkachenko.kmp_learning_app.ui.TrailingFigureRow
import org.artkachenko.kmp_learning_app.ui.rememberAppTopBarScrollBehavior
import org.artkachenko.kmp_learning_app.ui.theme.AppMotion
import org.artkachenko.kmp_learning_app.ui.theme.AppSpacing
import org.artkachenko.kmp_learning_app.ui.theme.AppThemeExtras
import org.artkachenko.kmp_learning_app.ui.theme.appScreenContentPadding
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.artkachenko.kmp_learning_app.ui.theme.AppContentWidth
import org.artkachenko.kmp_learning_app.ui.theme.AppScreenPane

internal const val SavedQuestionsLoadingTag = "saved_questions_loading"

/**
 * Stable per-entry handle for this screen's saved-state control, whose label repeats on every card.
 *
 * The control is the shared `QuestionBookmarkAction`, which the review surfaces reach by
 * `reviewQuestionSaveTag` instead. One affordance, two handles on purpose: a saved collection entry
 * and a result transcript's save action are reached by different tests for different reasons, and
 * one tag across both would make each surface's tests depend on the other's. The id keeps its
 * original spelling so those tests stay stable.
 */
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
        AppScreenPane(AppContentWidth.Standard) {
            // Keyed on the state's class, which is what the default `contentKey` gives: the screen
            // crossing from Loading into a collection, an empty state, or an error is one thing
            // becoming another and fades, while unsaving a Question stays inside Content and so is
            // not a screen transition at all. Keying on the state itself would fade the whole list
            // out and back in every time a row left it, over the top of the row's own animateItem.
            ScreenStateTransition(state = state, modifier = Modifier.fillMaxSize()) { current ->
                when (current) {
                    SavedQuestionsUiState.Loading -> ScreenLoading(
                        message = stringResource(Res.string.saved_questions_loading),
                        testTag = SavedQuestionsLoadingTag,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // An empty collection is a normal state with a way forward, not a failure: the
                    // learner has simply not saved anything yet, and the place to do that is a
                    // Question. The bookmark is the icon because it is the control they are being
                    // sent to look for — the same ribbon they will press on a Question card.
                    SavedQuestionsUiState.Empty -> ScreenAction(
                        message = stringResource(Res.string.saved_questions_empty),
                        actionLabel = stringResource(Res.string.saved_questions_empty_action),
                        onAction = onBrowseTopics,
                        modifier = Modifier.fillMaxSize(),
                        detail = stringResource(Res.string.saved_questions_empty_detail),
                        icon = AppIcons.Bookmark,
                    )
                    SavedQuestionsUiState.Error -> ScreenError(
                        message = stringResource(Res.string.saved_questions_error),
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )
                    is SavedQuestionsUiState.Content -> SavedQuestionsContent(
                        state = current,
                        onRemoveSaved = onRemoveSaved,
                        onSourceClick = onSourceClick,
                        failedSourceUrl = failedSourceUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
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
            // What this list holds, before what it is for. The screen stated its purpose and left
            // the learner to count the cards; a saved collection grows over weeks, and its size is
            // the first thing its owner wants to know — the Mistakes queue has led with its count
            // since it existed, and these two are siblings.
            Column(
                modifier = Modifier.padding(top = AppSpacing.Related),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Tight),
            ) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.saved_questions_count,
                        state.items.size,
                        state.items.size,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.saved_questions_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    // Closed by default, which is the one place this differs from a result transcript and is the
    // point of the screen. A transcript opens the Questions the learner got wrong because they came
    // to read it through; a saved collection is a list to browse, and twenty permanently open cards
    // of options, explanation and sources meant finding one meant scrolling past all the others in
    // full. Keyed by Question so a card the learner opened stays open as the list changes around it.
    var expanded by rememberSaveable(question.questionId) { mutableStateOf(false) }
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
            // The Question text and its saved state, with the text dominant. Through
            // `TrailingFigureRow` rather than a plain `Row`, because the bookmark is a
            // non-weighted trailing child of a weighted text column and a `Row` measures it
            // first: at an ordinary type size it sits centred against the trailing edge exactly
            // as before, and where the title would be left with less width than its own longest
            // word the bookmark drops below it instead of breaking the question mid-word. Centred
            // rather than top-aligned because the control is a text button with Material's 40dp
            // minimum height, so against a one-line question a top-aligned row was half again as
            // tall as its text with all the slack below it.
            TrailingFigureRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalGap = AppSpacing.Related,
                figure = {
                    SavedQuestionBookmark(
                        questionId = question.questionId,
                        isPending = isRemovalPending,
                        onRemoveSaved = onRemoveSaved,
                    )
                },
                text = {
                    Text(
                        question.text,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
            QuestionDisclosure(expanded = expanded, onToggle = { expanded = !expanded }) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.Grouped)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.Related),
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(
                                AppMotion.revealSpec(QuestionAnswersRevealDelayMillis),
                            ),
                        ),
                    ) {
                        question.answers.forEach { SavedQuestionAnswerRow(it) }
                    }
                    QuestionExplanationBlock(
                        explanation = question.explanation,
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(
                                AppMotion.revealSpec(QuestionExplanationRevealDelayMillis),
                            ),
                        ),
                    )
                    QuestionSources(
                        sources = question.sources,
                        onSourceClick = onSourceClick,
                        failedSourceUrl = failedSourceUrl,
                        modifier = Modifier.animateEnterExit(
                            enter = fadeIn(
                                AppMotion.revealSpec(QuestionExplanationRevealDelayMillis),
                            ),
                        ),
                    )
                }
            }
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
        // The same saved-state control the resolvable cards carry, and for the same reason: the
        // Question content is gone but the saved identity is not, and that identity is exactly what
        // a bookmark represents. Leaving this one entry with an unrelated destructive command would
        // make the single row on the screen with nothing to read the only one that shouts.
        SavedQuestionBookmark(
            questionId = questionId,
            isPending = isRemovalPending,
            onRemoveSaved = onRemoveSaved,
        )
    }
}

/**
 * The shared bookmark control, in the one place where it is always in its saved state.
 *
 * Every entry on this screen is saved by construction, so [QuestionBookmarkAction] is given
 * `isSaved = true` and renders the filled ribbon and the word the rest of the app uses for it.
 * Pressing it is the same intent as pressing it on a result transcript — *this Question is saved;
 * this toggles that* — and so it invokes the removal the screen already had. It replaced a bare
 * "Remove" text button, which made the one screen made entirely of saved questions the one screen
 * where saved state read as a destructive command.
 *
 * Disabled only while this Question's own removal is being persisted; the ribbon keeps showing the
 * stored value throughout, so a pending removal never draws as though it had already happened.
 */
@Composable
private fun SavedQuestionBookmark(
    questionId: String,
    isPending: Boolean,
    onRemoveSaved: (String) -> Unit,
) {
    QuestionBookmarkAction(
        isSaved = true,
        isPending = isPending,
        onToggle = { onRemoveSaved(questionId) },
        modifier = Modifier.testTag(savedQuestionRemoveTag(questionId)),
    )
}

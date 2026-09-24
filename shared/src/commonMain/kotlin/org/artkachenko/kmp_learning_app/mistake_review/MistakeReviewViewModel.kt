package org.artkachenko.kmp_learning_app.mistake_review

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.assessment_review.isAvailableFor
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionStateHolder
import org.artkachenko.kmp_learning_app.saved_questions.SavedQuestionsState

/**
 * Presents the mistake queue.
 *
 * The queue belongs to [MistakeReviewStateHolder], which outlives this ViewModel, so returning to
 * the tab shows the queue that was already there rather than a spinner. Retrying marks the shared
 * history stale so every screen derived from it recovers together.
 *
 * Saved state arrives from a separate app-scoped holder and stays orthogonal to the queue: which
 * Questions are unresolved is still derived from history alone, and saving or unsaving one changes
 * nothing about whether it is unresolved.
 */
internal class MistakeReviewViewModel(
    private val historyStore: AssessmentHistoryStore,
    stateHolder: MistakeReviewStateHolder,
    private val savedQuestionStateHolder: SavedQuestionStateHolder,
) : ViewModel() {
    val uiState: StateFlow<MistakeReviewUiState> = stateHolder.state

    val savedQuestions: StateFlow<SavedQuestionsState> = savedQuestionStateHolder.state

    init {
        savedQuestionStateHolder.refresh()
    }

    /**
     * Recovers from either failure the queue can show — an unreadable attempt table, and a
     * derivation over readable history that could not reconstruct review content — with the one
     * call that reaches both: the store re-announces the cached history once its re-read settles,
     * so the derivation runs again even when the attempts came back identical. Saved state has its
     * own read and is refreshed alongside.
     */
    fun refresh() {
        historyStore.invalidate()
        savedQuestionStateHolder.refresh()
    }

    /**
     * Ignores an ID the queue does not currently show as available review content, so the mutation
     * boundary cannot save a Question whose content is gone.
     */
    fun toggleSaved(questionId: String) {
        val content = uiState.value as? MistakeReviewUiState.Content ?: return
        if (content.mistakes.none { it.reviewItem.isAvailableFor(questionId) }) return
        savedQuestionStateHolder.toggleSaved(questionId)
    }
}

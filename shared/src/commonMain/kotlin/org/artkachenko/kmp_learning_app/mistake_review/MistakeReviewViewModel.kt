package org.artkachenko.kmp_learning_app.mistake_review

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore

/**
 * Presents the mistake queue.
 *
 * The queue belongs to [MistakeReviewStateHolder], which outlives this ViewModel, so returning to
 * the tab shows the queue that was already there rather than a spinner. Retrying marks the shared
 * history stale so every screen derived from it recovers together.
 */
internal class MistakeReviewViewModel(
    private val historyStore: AssessmentHistoryStore,
    stateHolder: MistakeReviewStateHolder,
) : ViewModel() {
    val uiState: StateFlow<MistakeReviewUiState> = stateHolder.state

    fun refresh() {
        historyStore.invalidate()
    }
}

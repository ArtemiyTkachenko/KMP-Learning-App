package org.artkachenko.kmp_learning_app.progress

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore

/**
 * Presents the progress dashboard.
 *
 * The state itself belongs to [ProgressStateHolder], which outlives this ViewModel, so returning to
 * the tab renders what was already there instead of starting from a spinner. Retrying marks the
 * shared history stale rather than re-reading privately, so every screen derived from it recovers
 * together.
 */
internal class ProgressViewModel(
    private val historyStore: AssessmentHistoryStore,
    private val stateHolder: ProgressStateHolder,
) : ViewModel() {
    val uiState: StateFlow<ProgressUiState> = stateHolder.state

    /**
     * Recovers from either failure the dashboard can show.
     *
     * Invalidating the shared history covers an unreadable attempt table, and recovers every other
     * screen derived from it at the same time. Asking the holder to derive again covers the other
     * half — history that read successfully but could not be turned into a dashboard — which an
     * invalidation alone would not reach, because a re-read of unchanged history is an equal value
     * that a `StateFlow` does not re-emit.
     */
    fun refresh() {
        historyStore.invalidate()
        stateHolder.retryDerivation()
    }
}

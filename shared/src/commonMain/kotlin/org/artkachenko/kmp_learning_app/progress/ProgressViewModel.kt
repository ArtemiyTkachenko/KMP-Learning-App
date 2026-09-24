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
    stateHolder: ProgressStateHolder,
) : ViewModel() {
    val uiState: StateFlow<ProgressUiState> = stateHolder.state

    /**
     * Recovers from either failure the dashboard can show, with the one call that reaches both.
     *
     * Invalidating the shared history re-reads an unreadable attempt table, and recovers every
     * other screen derived from it at the same time. It also covers the other half — history that
     * read successfully but could not be turned into a dashboard — because the store re-announces
     * the cached history once the re-read settles whether or not it changed, which is what makes
     * the derivation run again.
     */
    fun refresh() {
        historyStore.invalidate()
    }
}

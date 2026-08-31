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

    fun refresh() {
        historyStore.invalidate()
    }
}

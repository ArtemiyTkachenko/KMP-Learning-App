package org.artkachenko.kmp_learning_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * State the navigation control itself needs, independent of any one destination.
 *
 * Today that is only the unresolved mistake count, which badges the Mistakes item. The count used
 * to appear as a second button on the Progress dashboard, which duplicated the navigation item next
 * to it; as a badge it stays visible from every area and leads to the one place that acts on it.
 *
 * The count is derived from the shared history cache rather than counted on every navigation, so it
 * comes from the same read the Progress and Mistakes screens use and updates when an assessment
 * completes rather than when the learner happens to move between areas.
 */
internal class AppShellViewModel(
    private val mistakeReviewService: MistakeReviewService,
    historyStore: AssessmentHistoryStore,
) : ViewModel() {
    val unresolvedMistakeCount: StateFlow<Int> = historyStore.history
        .map { history ->
            when (history) {
                is AssessmentHistory.Loaded -> countUnresolved(history.attempts)
                // A badge is decoration: while the history is loading or unreadable, showing no
                // badge is better than interrupting navigation.
                AssessmentHistory.Loading, AssessmentHistory.Failed -> 0
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private suspend fun countUnresolved(attempts: List<org.artkachenko.kmp_learning_app.assessment.TestAttempt>): Int =
        runCatching { mistakeReviewService.countUnresolved(attempts) }.getOrDefault(0)
}


package org.artkachenko.kmp_learning_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.VisibleAssessmentHistory
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * State the navigation control itself needs, independent of any one destination.
 *
 * Today that is only the due mistake count, which badges the Mistakes item. The count used to
 * appear as a second button on the Progress dashboard, which duplicated the navigation item next to
 * it; as a badge it stays visible from every area and leads to the one place that acts on it.
 *
 * It counts only mistakes that are due, not every scheduled one: a badge is a prompt to act now, and
 * an entry whose next review is days away cannot be advanced today, so it should not nag.
 *
 * The count is derived from the shared history cache rather than counted on every navigation, so it
 * comes from the same read the Progress and Mistakes screens use and updates when an assessment
 * completes, the curriculum visibility changes, or a scheduled mistake becomes due — the cache
 * re-announces its snapshot at that moment — rather than when the learner happens to move between
 * areas.
 */
internal class AppShellViewModel(
    private val mistakeReviewService: MistakeReviewService,
    visibleHistory: VisibleAssessmentHistory,
) : ViewModel() {
    val dueMistakeCount: StateFlow<Int> = visibleHistory.history
        .map { history ->
            when (history) {
                is AssessmentHistory.Loaded -> countDue(history.attempts)
                // A badge is decoration: while the history is loading or unreadable, showing no
                // badge is better than interrupting navigation.
                AssessmentHistory.Loading, AssessmentHistory.Failed -> 0
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private suspend fun countDue(attempts: List<TestAttempt>): Int =
        try {
            mistakeReviewService.countDue(attempts)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            0
        }
}

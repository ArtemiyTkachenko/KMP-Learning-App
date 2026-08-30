package org.artkachenko.kmp_learning_app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.artkachenko.kmp_learning_app.mistake_review.MistakeReviewService

/**
 * State the navigation control itself needs, independent of any one destination.
 *
 * Today that is only the unresolved mistake count, which badges the Mistakes item. The count used
 * to appear as a second button on the Progress dashboard, which duplicated the navigation item next
 * to it; as a badge it stays visible from every area and leads to the one place that acts on it.
 */
internal class AppShellViewModel(
    private val mistakeReviewService: MistakeReviewService,
) : ViewModel() {
    private val _unresolvedMistakeCount = MutableStateFlow(0)
    val unresolvedMistakeCount: StateFlow<Int> = _unresolvedMistakeCount.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                _unresolvedMistakeCount.value = mistakeReviewService.countUnresolved()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // A badge is decoration. Failing to count is not worth interrupting navigation
                // for, and the last known count stays on screen until a later refresh succeeds.
            }
        }
    }
}

package org.artkachenko.kmp_learning_app.mistake_review

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore

/**
 * App-scoped mistake queue, derived from the shared history cache.
 *
 * The queue is rebuilt from completed attempts, which the navigation entry made this screen do from
 * scratch on every visit: the ViewModel is destroyed on a tab switch, so returning always started at
 * a spinner. Holding the last queue here means it is on screen for the first frame, with the re-read
 * happening behind it.
 */
internal class MistakeReviewStateHolder(
    private val mistakeReviewService: MistakeReviewService,
    historyStore: AssessmentHistoryStore,
    scope: CoroutineScope,
) {
    val state: StateFlow<MistakeReviewUiState> = historyStore.history
        .map { history ->
            when (history) {
                AssessmentHistory.Loading -> MistakeReviewUiState.Loading
                AssessmentHistory.Failed -> MistakeReviewUiState.Error
                is AssessmentHistory.Loaded -> queueFor(history.attempts)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, MistakeReviewUiState.Loading)

    private suspend fun queueFor(attempts: List<org.artkachenko.kmp_learning_app.assessment.TestAttempt>) =
        runCatching {
            // The service already orders the queue by most recent unresolved occurrence, so
            // presentation preserves that list exactly. Handing over the cached history keeps this
            // to the curriculum reads for the unresolved items alone.
            mistakeReviewService.load(attempts)
        }.fold(
            onSuccess = { mistakes ->
                if (mistakes.isEmpty()) MistakeReviewUiState.Empty else MistakeReviewUiState.Content(mistakes)
            },
            onFailure = { MistakeReviewUiState.Error },
        )
}


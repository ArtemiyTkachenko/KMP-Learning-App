package org.artkachenko.kmp_learning_app.mixed_interview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.artkachenko.kmp_learning_app.assessment.AssessmentConfig
import org.artkachenko.kmp_learning_app.assessment.AssessmentStatus
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistory
import org.artkachenko.kmp_learning_app.assessment.history.AssessmentHistoryStore

/**
 * The interview record, separating "not read yet" from "no interviews taken".
 *
 * A single nullable model conflated the two, so the screen rendered as though the learner had no
 * record and the card appeared underneath them once the read finished.
 */
internal sealed interface InterviewHistoryUiState {
    data object Loading : InterviewHistoryUiState

    /** Read successfully, and the learner has not completed a mixed interview. */
    data object Empty : InterviewHistoryUiState

    data class Content(val history: InterviewHistoryUiModel) : InterviewHistoryUiState
}

/**
 * App-scoped interview record, derived from the shared history cache.
 *
 * Lives outside the ViewModel so returning to the screen shows the record that was already on it
 * rather than rebuilding from nothing; see [AssessmentHistoryStore] for why the cache sits here.
 */
internal class InterviewHistoryStateHolder(
    historyStore: AssessmentHistoryStore,
    scope: CoroutineScope,
) {
    val state: StateFlow<InterviewHistoryUiState> = historyStore.history
        .map { history ->
            when (history) {
                AssessmentHistory.Loading -> InterviewHistoryUiState.Loading
                // The record is supplementary and the screen's action does not depend on it, so a
                // failed read shows the first-run shape rather than blocking the only control.
                AssessmentHistory.Failed -> InterviewHistoryUiState.Empty
                is AssessmentHistory.Loaded -> history.attempts.toUiState()
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, InterviewHistoryUiState.Loading)
}

private fun List<TestAttempt>.toUiState(): InterviewHistoryUiState {
    val attempts = filter {
        it.status == AssessmentStatus.COMPLETED && it.config is AssessmentConfig.Mixed
    }.mapNotNull(::toInterviewAttemptUiModel)
    if (attempts.isEmpty()) return InterviewHistoryUiState.Empty

    // The cache preserves the repository's newest-first order, which the progress history relies
    // on too, so the most recent attempt is the first.
    return InterviewHistoryUiState.Content(
        InterviewHistoryUiModel(
            attemptCount = attempts.size,
            latest = attempts.first(),
            best = attempts.maxBy { it.percentage },
        ),
    )
}


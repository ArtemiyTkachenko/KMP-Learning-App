package org.artkachenko.kmp_learning_app.assessment.history

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.artkachenko.kmp_learning_app.assessment.TestAttempt
import org.artkachenko.kmp_learning_app.assessment.repository.AssessmentRepository

/** Completed assessment history, or the fact that it has not been read yet. */
internal sealed interface AssessmentHistory {
    /** No read has completed, so there is nothing to show but a loading indicator. */
    data object Loading : AssessmentHistory

    /** Newest first, matching the order [AssessmentRepository.getCompletedAttempts] returns. */
    data class Loaded(val attempts: List<TestAttempt>) : AssessmentHistory

    data object Failed : AssessmentHistory
}

/**
 * App-scoped cache of completed assessment history.
 *
 * Progress, the mistake queue, the interview record, and the navigation badge all derive from the
 * same read, and each of them used to make it again on every visit — through a ViewModel that the
 * navigation entry destroys on a tab switch, so every return started from [AssessmentHistory
 * .Loading] and showed a spinner over content the app had already loaded seconds earlier.
 *
 * Holding the result here instead means the value outlives any screen. A returning screen sees the
 * cached history on its first frame and a re-read runs behind it, so the spinner appears only when
 * there is genuinely nothing to show yet.
 *
 * ## Why this is not a database observation
 *
 * Nothing outside this app writes the attempt tables, and an attempt is saved after *every answered
 * question* — observing the tables would recompute the whole history on each of those writes, in
 * the background, while the learner is mid-assessment. Every consumer here reads *completed*
 * attempts only, which an in-progress save cannot change, so the cache is invalidated on the one
 * transition that can affect it: an attempt completing. Adding a writer outside the app, such as a
 * server sync, is the point at which this would need to become a real observation.
 */
internal class AssessmentHistoryStore(
    private val assessmentRepository: AssessmentRepository,
    scope: CoroutineScope,
) {
    private val reloads = MutableStateFlow(0)

    /**
     * Started eagerly rather than while subscribed, because the upstream is not a live subscription
     * that costs anything to hold: it is an invalidation signal mapped to a read, so it re-runs only
     * when [invalidate] is called. Sharing while subscribed would re-read on every tab switch — more
     * work than caching, not less — and would leave the first screen of a session waiting.
     */
    val history: StateFlow<AssessmentHistory> = reloads
        .map { read() }
        .stateIn(scope, SharingStarted.Eagerly, AssessmentHistory.Loading)

    /**
     * Marks the cached history stale. Call after an attempt reaches a completed state; an
     * in-progress save cannot change what any consumer of this store reads.
     */
    fun invalidate() {
        reloads.update { it + 1 }
    }

    private suspend fun read(): AssessmentHistory =
        runCatching { assessmentRepository.getCompletedAttempts() }
            .fold(
                onSuccess = { AssessmentHistory.Loaded(it) },
                // A failed re-read must not blank content that is already on screen, so the
                // failure is only surfaced when nothing has ever loaded.
                onFailure = { if (history.value is AssessmentHistory.Loaded) history.value else AssessmentHistory.Failed },
            )
}


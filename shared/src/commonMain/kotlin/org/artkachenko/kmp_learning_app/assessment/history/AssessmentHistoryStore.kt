package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
) : CompletedAssessmentHistory {
    private val reloads = MutableStateFlow(0)
    private val failedReadRetry = Mutex()

    private val refreshes: StateFlow<HistoryRefresh> = reloads
        .map(::read)
        .stateIn(scope, SharingStarted.Eagerly, HistoryRefresh.Pending)

    /**
     * The cached history, re-announced once per settled refresh.
     *
     * **The contract: every [invalidate] produces exactly one emission here once the read it
     * started settles — whether the attempts changed, came back identical, or could not be read at
     * all.** A consumer therefore needs one signal to recover, not two, and it recovers by
     * observing this rather than by arranging anything of its own.
     *
     * Deliberately a [SharedFlow] rather than a `StateFlow`, because that contract is the part a
     * `StateFlow` cannot express: it drops an emission equal to the last one, and a re-read of an
     * attempt table nobody has written produces exactly that. Consumers do not only *render* this
     * value, they *derive* from it — over a curriculum that can be unavailable while the attempt
     * table reads perfectly well — so "derive again" has to be a signal this store can still send
     * when the history itself did not change. Four consumers used to manufacture that signal
     * privately, as a counter combined into their own state, which made every retry a pair of calls
     * that a caller had to know to make together; a retry that made only one of them recovered only
     * half the screens derived from this read.
     *
     * `replay = 1` is what a durable cache owes a late subscriber: a destination re-entering
     * composition renders the current history on its first frame instead of waiting for the next
     * refresh.
     *
     * Started eagerly rather than while subscribed, because the upstream is not a live subscription
     * that costs anything to hold: it is an invalidation signal mapped to a read, so it re-runs only
     * when [invalidate] is called. Sharing while subscribed would re-read on every tab switch — more
     * work than caching, not less — and would leave the first screen of a session waiting.
     */
    val history: SharedFlow<AssessmentHistory> = refreshes
        .filterIsInstance<HistoryRefresh.Settled>()
        .scan(AssessmentHistory.Loading, ::historyAfter)
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    /**
     * The same cached history as [history], for a caller that wants one answer rather than a
     * subscription — question selection, which resolves a practice request against what the learner
     * has already been shown.
     *
     * Waiting for the first read to settle is the point: reporting the initial
     * [AssessmentHistory.Loading] as "no completed attempts" would make every Question look unseen
     * for as long as the app had been running, which is exactly when the learner is most likely to
     * open practice. A failure is raised rather than returned empty, for the same reason. Serving
     * this from the cache is what keeps the Practice Builder's per-edit preflight from issuing a
     * history query for every level chip the learner taps.
     *
     * Unlike screen observers, selection cannot use stale history after [invalidate]: it waits for
     * the refresh generation that was current when this call began. A failed generation is surfaced
     * instead of falling back to older attempts. Calling again after a settled failure starts one
     * new read, which makes the Practice Builder's Retry action a real repository retry.
     */
    override suspend fun completedAttempts(): List<TestAttempt> {
        val requiredGeneration = generationForOneShotRead()
        val settled = refreshes
            .filterIsInstance<HistoryRefresh.Settled>()
            .first { it.generation >= requiredGeneration }
        return when (settled) {
            is HistoryRefresh.Loaded -> settled.attempts
            is HistoryRefresh.Failed -> throw AssessmentHistoryUnavailableException()
        }
    }

    /**
     * Marks the cached history stale. Call after an attempt reaches a completed state; an
     * in-progress save cannot change what any consumer of this store reads.
     *
     * This is also the whole of a consumer's retry: [history] emits once the resulting read settles
     * even if it reads the same attempts back, so a derivation that failed over readable history
     * runs again without the consumer signalling anything else.
     */
    fun invalidate() {
        reloads.update { it + 1 }
    }

    /** Coalesces concurrent callers onto one retry when the current generation has failed. */
    private suspend fun generationForOneShotRead(): Int = failedReadRetry.withLock {
        val currentGeneration = reloads.value
        val latestRefresh = refreshes.value
        if (
            latestRefresh is HistoryRefresh.Failed &&
            latestRefresh.generation == currentGeneration
        ) {
            (currentGeneration + 1).also { retryGeneration -> reloads.value = retryGeneration }
        } else {
            currentGeneration
        }
    }

    private suspend fun read(generation: Int): HistoryRefresh.Settled =
        runCatching { assessmentRepository.getCompletedAttempts() }
            .fold(
                onSuccess = { HistoryRefresh.Loaded(generation, it) },
                onFailure = { failure ->
                    if (failure is CancellationException) throw failure
                    HistoryRefresh.Failed(generation)
                },
            )
}

/**
 * The history a settled refresh leaves behind, as a pure function of the previous history.
 *
 * The one place the stale-on-failure rule lives: a failed re-read keeps attempts that were read
 * successfully, rather than replacing a working screen with an error, while a failure with nothing
 * cached yet is genuinely unreadable history. Either way the result is published, so a consumer
 * whose own derivation failed over the cached attempts derives again.
 */
private fun historyAfter(
    previous: AssessmentHistory,
    refresh: HistoryRefresh.Settled,
): AssessmentHistory =
    when (refresh) {
        is HistoryRefresh.Loaded -> AssessmentHistory.Loaded(refresh.attempts)
        is HistoryRefresh.Failed -> previous as? AssessmentHistory.Loaded ?: AssessmentHistory.Failed
    }

/**
 * One read of the attempt table, identified by the invalidation generation that started it.
 *
 * [Settled] is a type rather than a convention so that a caller which has already waited for a
 * generation cannot be handed [Pending]: the `when` over a settled refresh has two branches and no
 * unreachable third to fail loudly in.
 */
private sealed interface HistoryRefresh {
    /** No read has completed yet, so no generation has settled. */
    data object Pending : HistoryRefresh

    /** A read that finished, either way. */
    sealed interface Settled : HistoryRefresh {
        val generation: Int
    }

    data class Loaded(
        override val generation: Int,
        val attempts: List<TestAttempt>,
    ) : Settled

    data class Failed(override val generation: Int) : Settled
}

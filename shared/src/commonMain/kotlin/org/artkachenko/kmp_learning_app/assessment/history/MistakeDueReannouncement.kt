package org.artkachenko.kmp_learning_app.assessment.history

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * The longest single wait before the clock is read again.
 *
 * A coroutine delay runs on a monotonic clock, which on desktop and mobile stops while the device
 * sleeps; an entry due during the night would otherwise become due only after another full delay
 * once the lid opens. Re-reading the wall clock at this interval bounds that lateness without
 * polling: nothing downstream runs until an entry has actually become due.
 */
private val MaxWaitBeforeClockCheck: Duration = 15.minutes

/**
 * Forwards every snapshot and re-announces the latest one each time a coming-up mistake becomes
 * due, so the badge, the queue and the recommendation follow the clock without any new history.
 *
 * Due-ness is a function of history *and* time, and the history cache only announces changes to
 * history. Re-announcing the unchanged snapshot is the cheapest way to make time a trigger too:
 * every consumer already re-derives on each announcement, including equal ones, and each reads its
 * own `now`. It happens once per distinct due instant — Questions failed in one attempt share one —
 * so this is a handful of re-derivations a day rather than a periodic refresh.
 *
 * The wait is counted down by both the delays taken and the wall clock, whichever has moved
 * further, so it always ends: a sleep that advances the wall clock ends it early, and a test that
 * advances only virtual time still reaches the due instant.
 */
internal fun Flow<VisibleHistorySnapshot>.reannouncedWhenMistakesFallDue(
    now: () -> Instant,
): Flow<VisibleHistorySnapshot> = channelFlow {
    collectLatest { snapshot ->
        send(snapshot)
        val attempts = (snapshot.history as? AssessmentHistory.Loaded)?.attempts ?: return@collectLatest
        val dueInstants = MistakeScheduleDerivation.derive(attempts)
            .map { it.dueFrom }
            .distinct()
            .sorted()
        for (dueFrom in dueInstants) {
            var remaining = dueFrom - now()
            if (remaining <= Duration.ZERO) continue
            while (remaining > Duration.ZERO) {
                val step = minOf(remaining, MaxWaitBeforeClockCheck)
                delay(step)
                remaining = minOf(remaining - step, dueFrom - now())
            }
            // The delays can finish ahead of the wall clock if it was set back meanwhile; announcing
            // then would find nothing due. One more bounded wait covers that skew and still ends.
            val wallClockShortfall = dueFrom - now()
            if (wallClockShortfall > Duration.ZERO) {
                delay(minOf(wallClockShortfall, MaxWaitBeforeClockCheck))
            }
            send(snapshot)
        }
    }
}

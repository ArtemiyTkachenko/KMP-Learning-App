package org.artkachenko.kmp_learning_app

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/**
 * Clicks a row only once it has stopped moving.
 *
 * The Learn catalogue is enriched asynchronously: the recommendation, the continue-studying
 * shortcut, and the next-Lesson card are each derived from a separate read, and each one is
 * inserted *above* the Topic list as it resolves. Every Topic row therefore moves down — by about
 * a third of a compact window once all three have arrived — some frames after the list first
 * renders.
 *
 * A journey test that waits only for a Topic to exist can dispatch its press before that settling
 * and its release after, so the two land on different rows and no click is emitted at all. The
 * failure is invisible in the result — the row takes focus on press, so the tree afterwards looks
 * like a screen that was simply never tapped — and it is intermittent, which is the worst of both.
 *
 * This waits for the row's bounds to repeat across [StableFrames] consecutive frames, clicks, and
 * then keeps going until [arrived] says the destination is on screen. Settling alone turned out
 * not to be enough: an enrichment can land in the gap between the last sample and the press, so
 * the click has to be *verified* rather than assumed, and retried against a freshly settled row if
 * it was swallowed.
 *
 * [arrived] is the caller's, and it has to name something only the destination has. "The row is
 * gone" is not usable: Topic detail titles its app bar with the Topic name, so the very text that
 * was tapped is still on screen after a successful navigation.
 *
 * It is a test-side guard around a real product behaviour rather than a fix for it: a learner
 * reaching for a Topic on a slow first load can have the same thing happen to them, and the fix
 * for that is in how the catalogue admits late enrichment, not here.
 */
@OptIn(ExperimentalTestApi::class)
internal suspend fun ComposeUiTest.clickRowWhenSettled(
    text: String,
    timeoutMillis: Long = 10_000,
    arrived: () -> Boolean,
) {
    val seen = ArrayDeque<Rect>()
    waitUntil(timeoutMillis = timeoutMillis) {
        if (arrived()) return@waitUntil true
        val bounds = runCatching {
            onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        }.getOrNull() ?: return@waitUntil false
        seen.addLast(bounds)
        if (seen.size > StableFrames) seen.removeFirst()
        if (seen.size == StableFrames && seen.all { it == bounds }) {
            onNodeWithText(text).performClick()
            // Cleared so a swallowed click has to re-settle before being retried, rather than
            // firing again on the very next frame.
            seen.clear()
        }
        false
    }
}

/**
 * Enough frames that a single enrichment landing between two samples cannot look settled. Each
 * `waitUntil` iteration advances the clock by one frame, so this is a handful of frames rather
 * than a wall-clock delay.
 */
private const val StableFrames = 4

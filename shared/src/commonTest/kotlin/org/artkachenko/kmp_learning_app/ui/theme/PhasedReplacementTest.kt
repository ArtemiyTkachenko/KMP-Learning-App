package org.artkachenko.kmp_learning_app.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The two properties every phased replacement must keep, whatever the split is retuned to: the
 * incoming content never starts before the outgoing content has finished, and the phases together
 * take exactly the duration the replacement was given — no longer, so retuning the split cannot
 * quietly slow navigation down.
 */
internal class PhasedReplacementTest {

    private val replacements = mapOf(
        "navigation" to (AppMotion.NavigationReplacement to AppMotion.NavigationDurationMillis),
        "state change" to (AppMotion.StateReplacement to AppMotion.StateChangeDurationMillis),
    )

    @Test
    fun theEntranceWaitsForTheExitToFinish() {
        replacements.forEach { (name, pair) ->
            val (replacement, _) = pair
            assertTrue(
                replacement.enterDelayMillis >= replacement.exitMillis,
                "$name: the entrance starts at ${replacement.enterDelayMillis}ms, before the " +
                    "exit has finished at ${replacement.exitMillis}ms",
            )
            assertTrue(replacement.exitMillis > 0, "$name: the exit has no duration")
            assertTrue(replacement.enterMillis > 0, "$name: the entrance has no duration")
        }
    }

    @Test
    fun thePhasesFillTheOriginalDuration() {
        replacements.forEach { (name, pair) ->
            val (replacement, total) = pair
            assertEquals(total, replacement.totalMillis, "$name: built from the wrong duration")
            assertEquals(
                total,
                replacement.enterDelayMillis + replacement.enterMillis,
                "$name: delay plus entrance should take exactly the replacement's duration",
            )
        }
    }
}

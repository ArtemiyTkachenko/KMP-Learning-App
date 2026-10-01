package org.artkachenko.kmp_learning_app.ui.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * The browser lookup, checked against the browser's own reading of local time.
 *
 * A browser test cannot choose its zone, so instead of expected offsets this compares the app's
 * civil time — UTC plus `localUtcOffset` — with the hour and minute `Date` itself reports locally.
 * That agrees in every zone only if the `getTimezoneOffset` sign is inverted correctly and the
 * offset is taken at the instant, which is why a winter and a summer instant are both checked. In a
 * UTC environment both sides are trivially equal, so a CI run on UTC proves less than a local one.
 */
internal class UtcOffsetWebTest {

    @Test
    fun civilTimeMatchesTheBrowsersLocalReadingInWinterAndSummer() {
        listOf("2026-01-15T12:34:00Z", "2026-07-15T12:34:00Z").forEach { text ->
            val instant = Instant.parse(text)
            val local = instant.toLocalTimestamp(localUtcOffset(instant))
            val epochMilliseconds = instant.toEpochMilliseconds().toDouble()

            assertEquals(browserLocalHour(epochMilliseconds), local.hour, text)
            assertEquals(browserLocalMinute(epochMilliseconds), local.minute, text)
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun browserLocalHour(epochMilliseconds: Double): Int =
    js("new Date(epochMilliseconds).getHours()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun browserLocalMinute(epochMilliseconds: Double): Int =
    js("new Date(epochMilliseconds).getMinutes()")

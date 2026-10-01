package org.artkachenko.kmp_learning_app.ui.time

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The desktop lookup behind every displayed timestamp, under zones the test chooses.
 *
 * `LocalTimestampTest` deliberately supplies offsets by hand, so this is the one place the platform
 * step is checked: the sign the rest of the app assumes, and an offset resolved for the instant
 * itself rather than for today. Android runs the same `java.util.TimeZone` call. The process-wide
 * default zone is restored whatever happens, so no later test inherits it.
 */
internal class UtcOffsetJvmTest {

    private val winter = Instant.parse("2026-01-15T12:00:00Z")
    private val summer = Instant.parse("2026-07-15T12:00:00Z")

    @Test
    fun aZoneAheadOfUtcIsPositiveAndFollowsDaylightSavingAtTheInstant() = inZone("Europe/Berlin") {
        assertEquals(1.hours, localUtcOffset(winter))
        assertEquals(2.hours, localUtcOffset(summer))
    }

    @Test
    fun aZoneBehindUtcIsNegativeAndKeepsItsHalfHour() = inZone("America/St_Johns") {
        assertEquals(-(3.hours + 30.minutes), localUtcOffset(winter))
        assertEquals(-(2.hours + 30.minutes), localUtcOffset(summer))
    }

    @Test
    fun aFractionalOffsetWithoutDaylightSavingIsPreserved() = inZone("Asia/Kolkata") {
        assertEquals(5.hours + 30.minutes, localUtcOffset(winter))
        assertEquals(5.hours + 30.minutes, localUtcOffset(summer))
    }

    private fun inZone(id: String, block: () -> Unit) {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }
}

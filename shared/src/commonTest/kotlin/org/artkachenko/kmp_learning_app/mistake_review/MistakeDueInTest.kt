package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal class MistakeDueInTest {
    private val now = Instant.parse("2026-01-05T21:00:00Z")

    @Test
    fun anHourOrLessIsWithinTheHour() {
        assertEquals(MistakeDueIn.WithinHour, mistakeDueIn(now + 1.hours, now))
        assertEquals(MistakeDueIn.WithinHour, mistakeDueIn(now + 5.minutes, now))
    }

    @Test
    fun underADayIsPhrasedInHoursRoundedUp() {
        // A fresh mistake: one day less the grace.
        assertEquals(MistakeDueIn.Hours(20), mistakeDueIn(now + 20.hours, now))
        assertEquals(MistakeDueIn.Hours(20), mistakeDueIn(now + 19.hours + 1.minutes, now))
        assertEquals(MistakeDueIn.Hours(2), mistakeDueIn(now + 1.hours + 1.minutes, now))
    }

    @Test
    fun aDayOrMoreIsPhrasedInWholeDaysRoundedToTheNearest() {
        assertEquals(MistakeDueIn.Days(1), mistakeDueIn(now + 1.days, now))
        assertEquals(MistakeDueIn.Days(1), mistakeDueIn(now + 1.days + 1.hours, now))
        // The three-day step less the grace.
        assertEquals(MistakeDueIn.Days(3), mistakeDueIn(now + 3.days - 4.hours, now))
        // The seven-day step less the grace.
        assertEquals(MistakeDueIn.Days(7), mistakeDueIn(now + 7.days - 4.hours, now))
    }
}

package org.artkachenko.kmp_learning_app.ui.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The rules behind "Today · 13:23".
 *
 * Every case states its offset explicitly and never reads the machine's zone, which is the point of
 * `toLocalTimestamp` taking one: the same input must produce the same answer on a developer's laptop
 * and on a CI agent running in UTC. The one genuinely platform-dependent step, looking the offset
 * up, is `localUtcOffset` and is deliberately not exercised here.
 */
internal class LocalTimestampTest {

    @Test
    fun anInstantIsReadAsTheCivilDateAndClockTimeOfTheGivenOffset() {
        val instant = Instant.parse("2026-09-11T12:23:46.872Z")

        val utc = instant.toLocalTimestamp(0.hours)
        assertEquals(2026, utc.year)
        assertEquals(9, utc.month)
        assertEquals(11, utc.dayOfMonth)
        assertEquals("12:23", utc.timeOfDayText())

        // One instant, three zones, three different local readings — which is exactly why the
        // stored value must stay an Instant and be phrased at the point of display.
        assertEquals("13:23", instant.toLocalTimestamp(1.hours).timeOfDayText())
        assertEquals("05:23", instant.toLocalTimestamp((-7).hours).timeOfDayText())
    }

    @Test
    fun theOffsetCanMoveAnInstantOntoAnotherCalendarDay() {
        val lateEvening = Instant.parse("2026-09-11T23:30:00Z")

        val ahead = lateEvening.toLocalTimestamp(2.hours)
        assertEquals(12, ahead.dayOfMonth)
        assertEquals("01:30", ahead.timeOfDayText())

        val behind = lateEvening.toLocalTimestamp((-1).hours)
        assertEquals(11, behind.dayOfMonth)
        assertEquals("22:30", behind.timeOfDayText())
    }

    /** Half-hour and three-quarter-hour zones are real; the offset is a Duration, not an Int. */
    @Test
    fun fractionalHourOffsetsAreApplied() {
        val instant = Instant.parse("2026-09-11T12:00:00Z")

        assertEquals("17:30", instant.toLocalTimestamp(5.hours + 30.minutes).timeOfDayText())
        assertEquals("17:45", instant.toLocalTimestamp(5.hours + 45.minutes).timeOfDayText())
    }

    @Test
    fun midnightAndSingleDigitTimesAreZeroPadded() {
        assertEquals(
            "00:00",
            Instant.parse("2026-01-01T00:00:00Z").toLocalTimestamp(0.hours).timeOfDayText(),
        )
        assertEquals(
            "09:05",
            Instant.parse("2026-01-01T09:05:00Z").toLocalTimestamp(0.hours).timeOfDayText(),
        )
    }

    /**
     * The calendar arithmetic is written out rather than taken from a library, so the cases that
     * catch a wrong month-length table are worth stating: a leap day, the day after it, and the
     * year boundary either side.
     */
    @Test
    fun calendarBoundariesResolveCorrectly() {
        fun dateOf(text: String): Triple<Int, Int, Int> =
            Instant.parse(text).toLocalTimestamp(0.hours)
                .let { Triple(it.year, it.month, it.dayOfMonth) }

        assertEquals(Triple(2024, 2, 29), dateOf("2024-02-29T12:00:00Z"))
        assertEquals(Triple(2024, 3, 1), dateOf("2024-03-01T00:00:00Z"))
        assertEquals(Triple(2026, 2, 28), dateOf("2026-02-28T23:59:59Z"))
        assertEquals(Triple(2026, 3, 1), dateOf("2026-03-01T00:00:00Z"))
        assertEquals(Triple(2025, 12, 31), dateOf("2025-12-31T23:00:00Z"))
        assertEquals(Triple(2026, 1, 1), dateOf("2026-01-01T00:00:00Z"))
        // 1900 was not a leap year and 2000 was: the century rule and its exception.
        assertEquals(Triple(1900, 3, 1), dateOf("1900-03-01T00:00:00Z"))
        assertEquals(Triple(2000, 2, 29), dateOf("2000-02-29T00:00:00Z"))
    }

    /**
     * "Today" is a question about local calendar days, not about elapsed hours: two timestamps
     * twenty minutes apart can fall on different days, and two timestamps twenty hours apart can
     * fall on the same one.
     */
    @Test
    fun relativeDaysAreDecidedByLocalCalendarDayAndNotByElapsedTime() {
        val offset = 0.hours
        val now = Instant.parse("2026-09-11T00:10:00Z").toLocalTimestamp(offset)

        val sameDayEarly = Instant.parse("2026-09-11T00:00:00Z").toLocalTimestamp(offset)
        assertEquals(TimestampDay.TODAY, sameDayEarly.dayRelativeTo(now))

        // Twenty minutes earlier, and a different day.
        val justBeforeMidnight = Instant.parse("2026-09-10T23:50:00Z").toLocalTimestamp(offset)
        assertEquals(TimestampDay.YESTERDAY, justBeforeMidnight.dayRelativeTo(now))

        val twoDaysBack = Instant.parse("2026-09-09T23:50:00Z").toLocalTimestamp(offset)
        assertEquals(TimestampDay.EARLIER, twoDaysBack.dayRelativeTo(now))
    }

    /**
     * The reader's offset is what decides the day, so the same pair of instants can be "today" in
     * one zone and "yesterday" in another.
     */
    @Test
    fun theOffsetDecidesWhichDayAnInstantCountsAs() {
        val attempt = Instant.parse("2026-09-11T23:30:00Z")
        val now = Instant.parse("2026-09-12T01:00:00Z")

        assertEquals(
            TimestampDay.YESTERDAY,
            attempt.toLocalTimestamp(0.hours).dayRelativeTo(now.toLocalTimestamp(0.hours)),
        )
        assertEquals(
            TimestampDay.TODAY,
            attempt.toLocalTimestamp(2.hours).dayRelativeTo(now.toLocalTimestamp(2.hours)),
        )
    }

    /** A future timestamp — a clock that moved backwards — is not "yesterday" and is not "today". */
    @Test
    fun aTimestampAheadOfNowIsNotNamedAsARecentDay() {
        val now = Instant.parse("2026-09-11T12:00:00Z").toLocalTimestamp(0.hours)
        val tomorrow = Instant.parse("2026-09-12T12:00:00Z").toLocalTimestamp(0.hours)

        assertEquals(TimestampDay.EARLIER, tomorrow.dayRelativeTo(now))
    }
}

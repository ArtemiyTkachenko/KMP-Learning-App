package org.artkachenko.kmp_learning_app.ui.time

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * One instant as the calendar and clock a learner in a given zone would read off it.
 *
 * The app stores every timestamp as an [Instant] — an unambiguous point on the timeline — and that
 * is the right thing to persist, but it is not something to show anybody: `Instant.toString()`
 * produces `2026-09-11T12:23:46.872Z`, which states the moment in UTC to the millisecond and tells a
 * learner nothing they wanted to know. This type is the intermediate step that makes the difference
 * explicit: an instant plus an offset is a civil date, and only a civil date can be phrased.
 *
 * [epochDay] is kept because it, not the year/month/day triple, is what "today" and "yesterday" are
 * decided by: two timestamps are on the same local day exactly when their local day numbers match,
 * which is a single comparison rather than three.
 */
internal data class LocalTimestamp(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int,
    val epochDay: Long,
)

/**
 * How a day should be named relative to the day the learner is reading it on.
 *
 * Only the two nearest days are named. "3 days ago" is harder to place than a date, and anything
 * further back is genuinely a date, so past that the calendar day is simply printed.
 */
internal enum class TimestampDay {
    TODAY,
    YESTERDAY,
    EARLIER,
}

private const val SecondsPerMinute = 60L
private const val MinutesPerHour = 60L
private const val SecondsPerHour = SecondsPerMinute * MinutesPerHour
private const val HoursPerDay = 24L
private const val SecondsPerDay = SecondsPerHour * HoursPerDay

/**
 * Reads this instant as local civil time at [offset].
 *
 * The offset is a parameter rather than being looked up here, which is what makes every rule in this
 * file testable without touching the machine's zone: a test states the offset it means and gets the
 * same answer on any build agent. Production supplies it from [localUtcOffset].
 */
internal fun Instant.toLocalTimestamp(offset: Duration): LocalTimestamp {
    val localSeconds = epochSeconds + offset.inWholeSeconds
    val epochDay = localSeconds.floorDiv(SecondsPerDay)
    val secondOfDay = localSeconds.mod(SecondsPerDay)
    val date = civilDateFromEpochDay(epochDay)
    return LocalTimestamp(
        year = date.year,
        month = date.month,
        dayOfMonth = date.dayOfMonth,
        hour = (secondOfDay / SecondsPerHour).toInt(),
        minute = (secondOfDay % SecondsPerHour / SecondsPerMinute).toInt(),
        epochDay = epochDay,
    )
}

/** Whether [this] falls on the same local day as [now], the day before it, or earlier. */
internal fun LocalTimestamp.dayRelativeTo(now: LocalTimestamp): TimestampDay =
    when (now.epochDay - epochDay) {
        0L -> TimestampDay.TODAY
        1L -> TimestampDay.YESTERDAY
        else -> TimestampDay.EARLIER
    }

/** Zero-padded to two digits, so 09:05 does not render as 9:5. */
internal fun LocalTimestamp.timeOfDayText(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private data class CivilDate(val year: Int, val month: Int, val dayOfMonth: Int)

/**
 * The civil date a local day number names, by Howard Hinnant's `civil_from_days`.
 *
 * Written out rather than taken from a date-time library because it is the only calendar arithmetic
 * the app needs, and because the alternative — see [localUtcOffset] — is a new dependency plus an
 * npm package on the browser targets. The algorithm shifts the era to start in March so that the
 * leap day falls at the end of a year and no month-length table is required; it is exact for every
 * year the app can encounter.
 */
private fun civilDateFromEpochDay(epochDay: Long): CivilDate {
    // Days from 0000-03-01 rather than from 1970-01-01.
    val shifted = epochDay + 719468
    val era = (if (shifted >= 0) shifted else shifted - 146096) / 146097
    val dayOfEra = shifted - era * 146097
    val yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
    val marchYear = yearOfEra + era * 400
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val marchMonth = (5 * dayOfYear + 2) / 153
    val dayOfMonth = dayOfYear - (153 * marchMonth + 2) / 5 + 1
    val month = if (marchMonth < 10) marchMonth + 3 else marchMonth - 9
    return CivilDate(
        year = (if (month <= 2) marchYear + 1 else marchYear).toInt(),
        month = month.toInt(),
        dayOfMonth = dayOfMonth.toInt(),
    )
}

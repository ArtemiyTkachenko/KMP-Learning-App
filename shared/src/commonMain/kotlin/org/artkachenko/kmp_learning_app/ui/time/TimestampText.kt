package org.artkachenko.kmp_learning_app.ui.time

import androidx.compose.runtime.Composable
import kmp_learning_app.shared.generated.resources.Res
import kmp_learning_app.shared.generated.resources.month_short_april
import kmp_learning_app.shared.generated.resources.month_short_august
import kmp_learning_app.shared.generated.resources.month_short_december
import kmp_learning_app.shared.generated.resources.month_short_february
import kmp_learning_app.shared.generated.resources.month_short_january
import kmp_learning_app.shared.generated.resources.month_short_july
import kmp_learning_app.shared.generated.resources.month_short_june
import kmp_learning_app.shared.generated.resources.month_short_march
import kmp_learning_app.shared.generated.resources.month_short_may
import kmp_learning_app.shared.generated.resources.month_short_november
import kmp_learning_app.shared.generated.resources.month_short_october
import kmp_learning_app.shared.generated.resources.month_short_september
import kmp_learning_app.shared.generated.resources.timestamp_date
import kmp_learning_app.shared.generated.resources.timestamp_day_and_time
import kmp_learning_app.shared.generated.resources.timestamp_today
import kmp_learning_app.shared.generated.resources.timestamp_yesterday
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The app's one convention for showing a learner when something happened: a named day, a middle dot,
 * and the local clock time — `Today · 13:23`, or `11 Sep 2026 · 13:23` once the day is far enough
 * back to be worth dating.
 *
 * Every part of it is a string resource, including the month abbreviations and the separator, so the
 * convention is translatable rather than assembled out of English punctuation in Kotlin. The two
 * decisions that are *not* presentation — which local day an instant falls on, and whether that day
 * is today — are made by `LocalTimestamp.kt`, which is pure and takes its offset and its "now" as
 * arguments; this function is only the wording around them.
 *
 * [now] defaults to the system clock and is a parameter so a caller — a test, or a preview showing a
 * fixed record — can pin it. Both instants resolve their own offset, which is what keeps the
 * comparison right across a daylight-saving boundary: an attempt finished before the clocks changed
 * is still placed on the calendar day it was actually finished on.
 */
@Composable
internal fun timestampText(
    instant: Instant,
    now: Instant = Clock.System.now(),
): String {
    val timestamp = instant.toLocalTimestamp(localUtcOffset(instant))
    val today = now.toLocalTimestamp(localUtcOffset(now))
    val dayText = when (timestamp.dayRelativeTo(today)) {
        TimestampDay.TODAY -> stringResource(Res.string.timestamp_today)
        TimestampDay.YESTERDAY -> stringResource(Res.string.timestamp_yesterday)
        TimestampDay.EARLIER -> stringResource(
            Res.string.timestamp_date,
            timestamp.dayOfMonth.toString(),
            stringResource(shortMonthResource(timestamp.month)),
            timestamp.year.toString(),
        )
    }
    return stringResource(
        Res.string.timestamp_day_and_time,
        dayText,
        timestamp.timeOfDayText(),
    )
}

/**
 * Month numbers are 1-based and the resources are named rather than indexed, so the mapping is
 * written out. A `when` rather than a list because it is exhaustive over a closed range and reads as
 * the table it is; the `else` cannot be reached from [toLocalTimestamp], whose month is always 1..12.
 */
private fun shortMonthResource(month: Int): StringResource =
    when (month) {
        1 -> Res.string.month_short_january
        2 -> Res.string.month_short_february
        3 -> Res.string.month_short_march
        4 -> Res.string.month_short_april
        5 -> Res.string.month_short_may
        6 -> Res.string.month_short_june
        7 -> Res.string.month_short_july
        8 -> Res.string.month_short_august
        9 -> Res.string.month_short_september
        10 -> Res.string.month_short_october
        11 -> Res.string.month_short_november
        else -> Res.string.month_short_december
    }

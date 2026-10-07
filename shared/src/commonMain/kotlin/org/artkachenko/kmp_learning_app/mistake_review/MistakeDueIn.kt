package org.artkachenko.kmp_learning_app.mistake_review

import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** How far away a coming-up mistake's review is, in the units the queue phrases it in. */
internal sealed interface MistakeDueIn {
    data object WithinHour : MistakeDueIn

    data class Hours(val hours: Int) : MistakeDueIn

    data class Days(val days: Int) : MistakeDueIn
}

/**
 * Phrases the wait until [dueFrom] as elapsed time, matching the schedule, which is elapsed time
 * rather than calendar days.
 *
 * Hours round up, so "Due in 20 hours" is never early. Days round to the nearest whole day, since a
 * three-day review counted from an evening study session is about 68 hours away and "3 days" is
 * what the learner was told; rounding up would call 25 hours "2 days".
 */
internal fun mistakeDueIn(dueFrom: Instant, now: Instant): MistakeDueIn {
    val remaining = dueFrom - now
    return when {
        remaining <= 1.hours -> MistakeDueIn.WithinHour
        remaining < 1.days -> MistakeDueIn.Hours(ceil(remaining.toDouble(DurationUnit.HOURS)).toInt())
        else -> MistakeDueIn.Days(remaining.toDouble(DurationUnit.DAYS).roundToInt())
    }
}

package org.artkachenko.kmp_learning_app.ui.time

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

/**
 * Foundation's own zone rules. `localTimeZone` follows the device's current setting, and the offset
 * is resolved for the attempt's own date so daylight saving is applied as it was on the day.
 */
internal actual fun localUtcOffset(instant: Instant): Duration {
    val date = NSDate.dateWithTimeIntervalSince1970(
        instant.toEpochMilliseconds() / MillisecondsPerSecond,
    )
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(date).seconds
}

private const val MillisecondsPerSecond = 1000.0

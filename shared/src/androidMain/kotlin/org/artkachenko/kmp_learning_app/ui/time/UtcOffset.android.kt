package org.artkachenko.kmp_learning_app.ui.time

import java.util.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Android carries the same `java.util.TimeZone` rules the JVM host does, asked for the offset in
 * effect at this instant rather than the raw one, so a summer attempt keeps reading as summer once
 * the clocks have gone back.
 */
internal actual fun localUtcOffset(instant: Instant): Duration =
    TimeZone.getDefault().getOffset(instant.toEpochMilliseconds()).milliseconds

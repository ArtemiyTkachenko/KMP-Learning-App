package org.artkachenko.kmp_learning_app.ui.time

import java.util.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * The JVM's own zone rules, asked for the offset in effect at this instant rather than the raw
 * offset, so a summer attempt keeps reading as summer once the clocks have gone back.
 */
internal actual fun localUtcOffset(instant: Instant): Duration =
    TimeZone.getDefault().getOffset(instant.toEpochMilliseconds()).milliseconds

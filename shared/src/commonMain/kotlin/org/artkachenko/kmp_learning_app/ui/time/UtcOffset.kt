package org.artkachenko.kmp_learning_app.ui.time

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * How far local civil time runs ahead of UTC at [instant].
 *
 * This is the only platform-specific part of showing a learner a date. Everything above it — which
 * calendar day an [Instant] falls on, whether that day is today, and how the result reads — is
 * ordinary arithmetic and lives in `Timestamps.kt` in `commonMain`, so there is exactly one
 * implementation of the formatting rules and four one-line implementations of the lookup.
 *
 * It is an `expect` rather than a dependency because `kotlin.time` models an instant but not a time
 * zone: there is no multiplatform zone database in the standard library, and every host already
 * carries one. Adding `kotlinx-datetime` for a single offset would pull a new artifact — and, on the
 * two browser targets, an npm package — into the build to answer a question each platform answers in
 * one call.
 *
 * The offset is asked for *at an instant* rather than once, because it is not a constant: a zone
 * that observes daylight saving returns a different value for a summer attempt than for a winter
 * one, and a result completed in June must keep reading as June whenever it is looked at.
 */
internal expect fun localUtcOffset(instant: Instant): Duration

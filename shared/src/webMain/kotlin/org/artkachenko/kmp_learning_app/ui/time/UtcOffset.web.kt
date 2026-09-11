package org.artkachenko.kmp_learning_app.ui.time

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The browser's own zone rules, shared by both web targets.
 *
 * `Date.prototype.getTimezoneOffset` reports how far local time runs *behind* UTC, which is the
 * opposite sign from the rest of the app, so the result is negated here rather than at the one call
 * site that would then have to remember which convention it was holding.
 */
internal actual fun localUtcOffset(instant: Instant): Duration =
    -timeZoneOffsetMinutes(instant.toEpochMilliseconds().toDouble()).minutes

/**
 * One `js()` function serving both targets. It is the shape Kotlin/Wasm requires — top level, a
 * single expression body, parameters referenced by name — and Kotlin/JS accepts the same form, so
 * `webMain` needs no per-target copy. The opt-in is Wasm's; the annotation resolves on both because
 * `kotlin.js` is a default import for each.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private fun timeZoneOffsetMinutes(epochMilliseconds: Double): Int =
    js("new Date(epochMilliseconds).getTimezoneOffset()")

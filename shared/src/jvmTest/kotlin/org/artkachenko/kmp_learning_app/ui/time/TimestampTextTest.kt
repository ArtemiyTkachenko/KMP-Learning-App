package org.artkachenko.kmp_learning_app.ui.time

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * The wording layer of every displayed timestamp: which day name is chosen, which month resource
 * each month number reads as, and which offset each instant is read at.
 *
 * `LocalTimestampTest` already pins the calendar arithmetic and the zero-padded clock, and
 * `UtcOffsetJvmTest` the platform lookup, so nothing here re-derives a date. What only this test
 * can catch is a mistake in `timestampText` itself — a month table with two entries swapped would
 * date every Progress history row and every interview record wrongly while the arithmetic below it
 * stayed perfectly correct.
 *
 * Both instants are explicit, so the machine's clock never decides a branch, and the zone is
 * installed for the test and restored whatever happens, as in `UtcOffsetJvmTest`.
 */
@OptIn(ExperimentalTestApi::class)
internal class TimestampTextTest {

    @Test
    fun theSameLocalDayIsTodayThePreviousOneYesterdayAndAnythingOlderIsDated() = inZone("UTC") {
        val now = Instant.parse("2026-03-18T15:00:00Z")

        assertEquals(
            listOf(
                "Today · 09:05",
                "Yesterday · 23:59",
                "16 Mar 2026 · 00:00",
            ),
            render(
                now,
                Instant.parse("2026-03-18T09:05:00Z"),
                Instant.parse("2026-03-17T23:59:00Z"),
                Instant.parse("2026-03-16T00:00:00Z"),
            ),
        )
    }

    /** One table rather than twelve tests: the contract is the whole mapping, in order. */
    @Test
    fun everyMonthIsNamedByItsOwnAbbreviation() = inZone("UTC") {
        val now = Instant.parse("2026-03-18T15:00:00Z")
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )

        assertEquals(
            months.map { "15 $it 2025 · 08:07" },
            render(
                now,
                *(1..12).map { month ->
                    Instant.parse("2025-${month.toString().padStart(2, '0')}-15T08:07:00Z")
                }.toTypedArray(),
            ),
        )
    }

    /**
     * A winter record read in summer keeps its winter clock time. Reading it at the offset of `now`
     * instead would move it an hour, and near midnight onto the wrong day.
     */
    @Test
    fun eachInstantIsReadAtItsOwnOffsetRatherThanTheOffsetOfNow() = inZone("Europe/Berlin") {
        val summerNow = Instant.parse("2026-07-15T12:00:00Z")

        assertEquals(
            listOf("15 Jan 2026 · 13:00"),
            render(summerNow, Instant.parse("2026-01-15T12:00:00Z")),
        )
    }

    private fun render(now: Instant, vararg instants: Instant): List<String> {
        var rendered = emptyList<String>()
        runComposeUiTest {
            setContent {
                rendered = instants.map { timestampText(it, now) }
            }
            waitForIdle()
        }
        return rendered
    }

    private fun inZone(id: String, block: () -> Unit) {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(original)
        }
    }
}

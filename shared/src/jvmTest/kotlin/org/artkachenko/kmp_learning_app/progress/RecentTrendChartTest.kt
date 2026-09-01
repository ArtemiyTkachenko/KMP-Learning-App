package org.artkachenko.kmp_learning_app.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the two chart decisions that are easy to "improve" into something dishonest: the fixed
 * accuracy axis and the equal horizontal spacing.
 */
internal class RecentTrendChartTest {
    @Test
    fun theVerticalScaleIsTheFullAccuracyRange() {
        val points = trendPoints(listOf(0.0, 50.0, 100.0))

        assertEquals(listOf(1f, 0.5f, 0f), points.map(TrendPoint::y))
    }

    @Test
    fun aFlatSeriesIsNotStretchedAcrossTheChart() {
        // Fitted to their own 72-76 range these three would climb from the floor to the ceiling.
        // On the fixed scale they stay within three percent of the chart height, which is what
        // actually happened.
        val ys = trendPoints(listOf(72.0, 74.0, 76.0)).map(TrendPoint::y)

        assertTrue(
            ys.all { it in 0.23f..0.29f },
            "each point must sit at its own share of the full range, but was $ys",
        )
        assertTrue(ys.max() - ys.min() < 0.05f, "a near-flat series must draw as near-flat")
    }

    @Test
    fun attemptsAreEvenlySpacedInTheOrderTheyArrive() {
        // Spacing is by position, not by elapsed time: the recent window is defined by a count of
        // assessments, so the gaps carry no duration meaning.
        val points = trendPoints(listOf(10.0, 20.0, 30.0, 40.0, 50.0))

        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), points.map(TrendPoint::x))
    }

    @Test
    fun theOldestAttemptStaysOnTheLeft() {
        val points = trendPoints(listOf(90.0, 10.0))

        assertTrue(points.first().y < points.last().y, "the first attempt must be plotted first")
    }

    @Test
    fun degenerateSeriesLengthsAreHandledWithoutDividingByZero() {
        assertEquals(emptyList<TrendPoint>(), trendPoints(emptyList()))
        assertEquals(listOf(TrendPoint(0.5f, 0.5f)), trendPoints(listOf(50.0)))
    }

    @Test
    fun valuesOutsideTheAccuracyRangeAreClampedRatherThanDrawnOffTheChart() {
        val points = trendPoints(listOf(-10.0, 110.0))

        assertEquals(listOf(1f, 0f), points.map(TrendPoint::y))
    }
}

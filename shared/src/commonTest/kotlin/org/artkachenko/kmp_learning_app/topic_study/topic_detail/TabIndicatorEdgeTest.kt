package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Where the Topic tab indicator's edges sit for a continuous pager position, over tabs of unequal
 * width.
 *
 * The tabs are content-sized, so the rule has to travel between different starts *and* change width
 * between different cells. Rendered frames of a drag are not assertable, but the derivation is a
 * pure function of the pager position, and these are the properties a drag depends on: exact at
 * rest, linear between neighbours, continuous where the pager flips `currentPage`, and clamped at
 * the ends.
 */
internal class TabIndicatorEdgeTest {

    // Three unequal cells laid end to end, as a content-sized row places them.
    private val lefts = floatArrayOf(0f, 110f, 250f)
    private val rights = floatArrayOf(110f, 250f, 420f)

    private fun left(position: Float) = tabIndicatorEdge(position, 3) { lefts[it] }
    private fun right(position: Float) = tabIndicatorEdge(position, 3) { rights[it] }

    @Test
    fun aSettledPagerPutsTheRuleExactlyOnItsTab() {
        for (page in 0..2) {
            assertEquals(lefts[page], left(page.toFloat()))
            assertEquals(rights[page], right(page.toFloat()))
        }
    }

    /**
     * Halfway between two tabs of different widths the rule is halfway between them at *both*
     * edges, which is what makes it change width while it travels rather than slide at one width.
     */
    @Test
    fun betweenTwoTabsBothEdgesMoveSoTheWidthMorphs() {
        assertEquals(55f, left(0.5f))
        assertEquals(180f, right(0.5f))
        assertEquals(125f, right(0.5f) - left(0.5f))

        assertEquals(180f, left(1.5f))
        assertEquals(335f, right(1.5f))
        assertEquals(155f, right(1.5f) - left(1.5f))
    }

    /**
     * The pager flips `currentPage` at the midpoint of a swipe while the offset fraction changes
     * sign. The two readings of the same physical position must give the same rule, in either
     * direction of travel, or the rule would jump when the page flips.
     */
    @Test
    fun theRuleIsContinuousWhereThePagerFlipsItsCurrentPage() {
        for ((before, after) in listOf(0 to 1, 1 to 2)) {
            val fromBefore = before + 0.5f
            val fromAfter = after + -0.5f
            assertEquals(left(fromBefore), left(fromAfter))
            assertEquals(right(fromBefore), right(fromAfter))
        }
    }

    /** Across a whole drag, forwards or back, neither edge ever jumps and the width never inverts. */
    @Test
    fun aDragAcrossEveryTabMovesMonotonicallyWithAPositiveWidth() {
        var previousLeft = left(0f)
        var previousRight = right(0f)
        for (step in 1..200) {
            val position = step / 100f
            val l = left(position)
            val r = right(position)
            assertTrue(l >= previousLeft && r >= previousRight, "Edges went backwards at $position")
            assertTrue(l - previousLeft <= 2f && r - previousRight <= 2f, "An edge jumped at $position")
            assertTrue(r > l, "The rule has no width at $position")
            previousLeft = l
            previousRight = r
        }
    }

    /** An overscroll pull stretches the pager past an end without navigating; the rule stays put. */
    @Test
    fun anOverscrollPullKeepsTheRuleOnTheEndTab() {
        assertEquals(lefts[0], left(-0.3f))
        assertEquals(rights[0], right(-0.3f))
        assertEquals(lefts[2], left(2.4f))
        assertEquals(rights[2], right(2.4f))
    }

    @Test
    fun aSingleTabIsItsOwnPosition() {
        assertEquals(7f, tabIndicatorEdge(0.6f, 1) { 7f })
    }
}

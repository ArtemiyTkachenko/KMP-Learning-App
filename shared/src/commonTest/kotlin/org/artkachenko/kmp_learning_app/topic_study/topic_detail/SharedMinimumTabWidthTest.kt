package org.artkachenko.kmp_learning_app.topic_study.topic_detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How the content-sized Topic tab row spends its width.
 *
 * The widths are the ones measured on a 380px compact pane: at an ordinary type size every label
 * fits a third, and at a doubled one the natural widths are about 91, 130 and 154. The rule is a
 * floor, so each case states the row it produces — `max(floor, natural)` per tab — because that is
 * what a learner sees.
 */
internal class SharedMinimumTabWidthTest {

    private fun row(rowWidth: Int, natural: List<Int>): List<Int> {
        val floor = sharedMinimumTabWidth(rowWidth, natural, materialMinimum = 90)
        return natural.map { maxOf(floor, it) }
    }

    /** Every label fits an equal share, so the row is the three equal cells it always was. */
    @Test
    fun labelsThatFitTheirShareKeepTheEqualCells() {
        assertEquals(listOf(126, 126, 126), row(380, listOf(60, 80, 85)))
        assertEquals(listOf(280, 280, 280), row(840, listOf(60, 80, 85)))
    }

    /**
     * One label outgrows its share but the three still fit: the wide ones keep their width and the
     * narrow one takes the slack, so the row fills the window exactly and does not scroll.
     */
    @Test
    fun aLabelWiderThanItsShareTakesItsOwnWidthAndTheRowStillFits() {
        val widths = row(380, listOf(91, 130, 154))
        assertEquals(listOf(96, 130, 154), widths)
        assertEquals(380, widths.sum())
    }

    /** When the labels are wider than the row together, each keeps its own width and it scrolls. */
    @Test
    fun labelsWiderThanTheRowKeepTheirOwnWidths() {
        assertEquals(listOf(91, 130, 154), row(360, listOf(91, 130, 154)))
    }

    /** Never below Material's own smallest scrollable tab, even with room to spare elsewhere. */
    @Test
    fun theFloorIsNeverBelowMaterialsMinimum() {
        assertEquals(90, sharedMinimumTabWidth(360, listOf(40, 150, 150), materialMinimum = 90))
    }

    /** No tab is made narrower than its label, whatever the floor. */
    @Test
    fun noTabIsNarrowerThanItsNaturalWidth() {
        for (rowWidth in 200..900 step 7) {
            val natural = listOf(91, 130, 154)
            row(rowWidth, natural).zip(natural).forEach { (width, label) ->
                assertTrue(width >= label, "A $label px label got $width px at a $rowWidth px row")
            }
        }
    }
}

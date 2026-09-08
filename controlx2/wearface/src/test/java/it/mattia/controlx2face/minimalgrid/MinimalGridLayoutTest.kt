package it.mattia.controlx2face.minimalgrid

import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalGridLayoutTest {

    /**
     * The bug this exists to prevent: a complication row that looks right on a square preview but
     * has its outer corners hanging off the side of a round watch, where they are clipped and the
     * text inside them is cut. Caught here rather than on a wrist.
     */
    @Test
    fun everySlotFitsInsideARoundDisplay() {
        MinimalGridLayout.allSlots.forEach { slot ->
            assertTrue(
                "slot $slot has a corner outside the round display",
                MinimalGridLayout.isInsideRoundDisplay(slot),
            )
        }
    }

    @Test
    fun theThreeCellsTileTheRowWithoutGapsOrOverlap() {
        val cells = MinimalGridLayout.cells
        assertTrue("expected three cells", cells.size == 3)
        cells.zipWithNext { left, right ->
            assertTrue("cells $left and $right do not meet", left.right == right.left)
        }
        cells.forEach { cell ->
            assertTrue("cell $cell is not the row's height", cell.top == MinimalGridLayout.ROW_TOP)
            assertTrue("cell $cell is not the row's height", cell.bottom == MinimalGridLayout.ROW_BOTTOM)
            assertTrue("cell $cell has no width", cell.right > cell.left)
        }
    }

    @Test
    fun dividersSitOnTheCellBoundaries() {
        val boundaries = MinimalGridLayout.cells.zipWithNext { left, _ -> left.right }
        assertTrue(
            "dividers ${MinimalGridLayout.cellDividers} do not match boundaries $boundaries",
            MinimalGridLayout.cellDividers == boundaries,
        )
    }

    /** A sanity check on the guard itself: a full-width row must be rejected. */
    @Test
    fun theGuardRejectsARowThatSpillsOffARoundDisplay() {
        val naive = MinimalGridLayout.Rect(0.14f, 0.70f, 0.86f, 0.88f)
        assertTrue(
            "the round-display guard would not have caught the naive full-width layout",
            !MinimalGridLayout.isInsideRoundDisplay(naive),
        )
    }
}

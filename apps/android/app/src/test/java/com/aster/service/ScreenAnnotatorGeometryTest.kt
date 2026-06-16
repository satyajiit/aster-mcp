package com.aster.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAnnotatorGeometryTest {

    @Test
    fun clampRect_keepsInsideBitmapBounds() {
        // Box partially off the right/bottom edge of a 1280x720 bitmap.
        val r = ScreenAnnotator.clampRect(
            left = 1200, top = 700, right = 1400, bottom = 900,
            bmpW = 1280, bmpH = 720
        )
        assertEquals(1200, r.left)
        assertEquals(700, r.top)
        assertEquals(1280, r.right)   // clamped to bmpW
        assertEquals(720, r.bottom)   // clamped to bmpH
    }

    @Test
    fun clampRect_keepsNegativeOriginAtZero() {
        val r = ScreenAnnotator.clampRect(
            left = -50, top = -10, right = 100, bottom = 100,
            bmpW = 1280, bmpH = 720
        )
        assertEquals(0, r.left)
        assertEquals(0, r.top)
    }

    @Test
    fun labelChipRect_sitsAtBoxTopLeftWhenRoomAbove() {
        // Chip for a 2-digit index; default chip height 36, width grows with digits.
        val chip = ScreenAnnotator.labelChipRect(
            boxLeft = 200, boxTop = 300, index = 12,
            bmpW = 1280, bmpH = 720
        )
        // Anchored to box left; chip bottom aligns to box top (label above the box).
        assertEquals(200, chip.left)
        assertEquals(300, chip.bottom)
        assertTrue("chip must have positive area", chip.right > chip.left && chip.bottom > chip.top)
        assertTrue("chip stays on-screen", chip.left >= 0 && chip.top >= 0)
    }

    @Test
    fun labelChipRect_dropsBelowWhenNoRoomAbove() {
        // Box flush to the top: chip can't go above (top would be negative) -> place below box top.
        val chip = ScreenAnnotator.labelChipRect(
            boxLeft = 0, boxTop = 0, index = 1,
            bmpW = 1280, bmpH = 720
        )
        assertEquals(0, chip.left)
        assertEquals(0, chip.top)         // chip top clamped to 0
        assertTrue(chip.bottom > 0)       // chip drops below the box top
    }
}

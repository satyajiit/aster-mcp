package com.aster.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenActionLogicTest {

    // -- keyNameToKeycode (Task 4) --

    @Test
    fun keyNameToKeycode_mapsCommonKeys() {
        assertEquals("66", keyNameToKeycode("ENTER"))
        assertEquals("4", keyNameToKeycode("BACK"))
        assertEquals("67", keyNameToKeycode("DEL"))
        assertEquals("112", keyNameToKeycode("FORWARD_DEL"))
        assertEquals("19", keyNameToKeycode("DPAD_UP"))
        assertEquals("20", keyNameToKeycode("DPAD_DOWN"))
        assertEquals("21", keyNameToKeycode("DPAD_LEFT"))
        assertEquals("22", keyNameToKeycode("DPAD_RIGHT"))
        assertEquals("23", keyNameToKeycode("DPAD_CENTER"))
        assertEquals("61", keyNameToKeycode("TAB"))
        assertEquals("62", keyNameToKeycode("SPACE"))
        assertEquals("84", keyNameToKeycode("SEARCH"))
        assertEquals("279", keyNameToKeycode("PASTE"))
        assertEquals("278", keyNameToKeycode("COPY"))
        assertEquals("3", keyNameToKeycode("HOME"))
        assertEquals("187", keyNameToKeycode("APP_SWITCH"))
        assertEquals("111", keyNameToKeycode("ESCAPE"))
    }

    @Test
    fun keyNameToKeycode_isCaseInsensitive() {
        assertEquals("66", keyNameToKeycode("enter"))
        assertEquals("4", keyNameToKeycode("Back"))
    }

    @Test
    fun keyNameToKeycode_acceptsRawNumeric() {
        // A bare numeric keycode passes through (advanced callers).
        assertEquals("99", keyNameToKeycode("99"))
    }

    @Test
    fun keyNameToKeycode_unknownReturnsNull() {
        assertNull(keyNameToKeycode("NOT_A_KEY"))
    }

    // -- scroll normalizers (Task 5) --

    @Test
    fun normalizeScrollDirection_buckets() {
        assertEquals(ScrollAxis.VERTICAL_BACKWARD, normalizeScrollDirection("up"))
        assertEquals(ScrollAxis.VERTICAL_FORWARD, normalizeScrollDirection("down"))
        assertEquals(ScrollAxis.HORIZONTAL_BACKWARD, normalizeScrollDirection("left"))
        assertEquals(ScrollAxis.HORIZONTAL_FORWARD, normalizeScrollDirection("right"))
        assertEquals(ScrollAxis.VERTICAL_FORWARD, normalizeScrollDirection("DOWN"))
        // legacy aliases
        assertEquals(ScrollAxis.VERTICAL_BACKWARD, normalizeScrollDirection("backward"))
        assertEquals(ScrollAxis.VERTICAL_FORWARD, normalizeScrollDirection("forward"))
    }

    @Test
    fun normalizeScrollDirection_unknownIsNull() {
        assertNull(normalizeScrollDirection("diagonal"))
    }

    @Test
    fun normalizeScrollAmount_buckets() {
        assertEquals(ScrollAmount.PAGE, normalizeScrollAmount("page"))
        assertEquals(ScrollAmount.HALF_PAGE, normalizeScrollAmount("halfpage"))
        assertEquals(ScrollAmount.TO_EDGE, normalizeScrollAmount("toEdge"))
        assertEquals(ScrollAmount.PAGE, normalizeScrollAmount(null)) // default
        assertEquals(ScrollAmount.PAGE, normalizeScrollAmount("anything-else"))
    }

    // -- pinch / drag point math (Task 6) --

    @Test
    fun pinchStrokePoints_diverging_movesOutward() {
        // Zoom-in pinch (in=false → diverge): two fingers start near center, end farther apart.
        val (a, b) = pinchStrokePoints(cx = 500f, cy = 1000f, startGap = 100f, endGap = 400f)
        // start points are closer to center than end points on the same side
        assertTrue(kotlin.math.abs(a.startX - 500f) < kotlin.math.abs(a.endX - 500f))
        assertTrue(kotlin.math.abs(b.startX - 500f) < kotlin.math.abs(b.endX - 500f))
        // the two strokes are on opposite sides of center
        assertTrue((a.startX - 500f) * (b.startX - 500f) < 0f)
    }

    @Test
    fun pinchStrokePoints_converging_movesInward() {
        // Zoom-out pinch (converge): start far apart, end near center.
        val (a, b) = pinchStrokePoints(cx = 500f, cy = 1000f, startGap = 400f, endGap = 100f)
        assertTrue(kotlin.math.abs(a.startX - 500f) > kotlin.math.abs(a.endX - 500f))
        assertTrue(kotlin.math.abs(b.startX - 500f) > kotlin.math.abs(b.endX - 500f))
    }

    @Test
    fun dragStrokePoints_isStartToEnd() {
        val stroke = dragStrokePoints(10f, 20f, 200f, 400f)
        assertEquals(10f, stroke.startX, 0.001f)
        assertEquals(20f, stroke.startY, 0.001f)
        assertEquals(200f, stroke.endX, 0.001f)
        assertEquals(400f, stroke.endY, 0.001f)
    }
}

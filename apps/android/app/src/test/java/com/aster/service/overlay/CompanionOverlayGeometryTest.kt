package com.aster.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class CompanionOverlayGeometryTest {
    private fun compute(
        screenWidth: Int = 1080,
        density: Float = 3f,
        cutout: CutoutBounds? = null,
    ) = CompanionOverlayGeometry.compute(
        screenWidthPx = screenWidth,
        density = density,
        cutout = cutout,
        statusBarHeightPx = 72,
        minWidthDp = 168,
        maxWidthDp = 240,
        sideMarginDp = 8,
        cutoutSideRoomDp = 96,
        cutoutHangDp = 44,
        fallbackTopDp = 8,
    )

    @Test
    fun centeredCutout_centersPillOnSensorAndStartsAtTopEdge() {
        val result = compute(cutout = CutoutBounds(510, 0, 570, 90))

        assertEquals(540, result.x + result.width / 2)
        assertEquals(0, result.y)
        assertTrue(result.height >= 90 + 44 * 3)
    }

    @Test
    fun cornerCutout_clampsPillInsideDisplay() {
        val result = compute(cutout = CutoutBounds(1010, 0, 1060, 80))

        assertEquals(1080, result.x + result.width)
        assertTrue(result.x >= 0)
    }

    @Test
    fun noCutout_usesTopCenterFallbackBelowStatusBar() {
        val result = compute()

        assertEquals(540, result.x + result.width / 2)
        assertEquals(72 + 8 * 3, result.y)
        assertEquals(
            (result.width * CompanionOverlayGeometry.CROP_HEIGHT.toFloat() /
                CompanionOverlayGeometry.CROP_WIDTH).roundToInt(),
            result.height,
        )
    }

    @Test
    fun narrowDisplay_preservesSideMarginsAndPositiveSize() {
        val result = compute(screenWidth = 320, density = 2f)

        assertEquals(16, result.x)
        assertEquals(320 - 32, result.width)
        assertTrue(result.height > 0)
    }

    @Test
    fun wideCutout_expandsPillWithinMaximum() {
        val result = compute(cutout = CutoutBounds(290, 0, 790, 100))

        assertEquals(240 * 3, result.width)
        assertEquals(540, result.x + result.width / 2)
    }
}

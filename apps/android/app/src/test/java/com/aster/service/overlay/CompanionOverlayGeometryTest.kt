package com.aster.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionOverlayGeometryTest {
    private fun compute(
        screenWidth: Int = 1080,
        density: Float = 3f,
        cutouts: List<CutoutBounds> = emptyList(),
        expanded: Boolean = false,
    ) = CompanionOverlayGeometry.compute(
        screenWidthPx = screenWidth,
        density = density,
        cutouts = cutouts,
        statusBarHeightPx = 72,
        expanded = expanded,
        minWidthDp = 168,
        maxWidthDp = 240,
        expandedWidthDp = 280,
        sideMarginDp = 8,
        cutoutSideRoomDp = 96,
        safeGapDp = 4,
        contentHeightDp = 60,
        fallbackTopDp = 8,
    )

    @Test
    fun centeredCutout_keepsEveryDrawablePixelBelowSensor() {
        val cutout = CutoutBounds(510, 0, 570, 90)
        val result = compute(cutouts = listOf(cutout))

        assertEquals(540, result.x + result.width / 2)
        assertEquals(90 + 4 * 3, result.y)
        assertEquals(0, result.contentBounds.top)
        assertEquals(60 * 3, result.contentBounds.height)
        assertEquals(cutout, result.avoidedCutouts.single())
        assertEquals(result.height, result.contentBounds.bottom)
    }

    @Test
    fun cornerCutout_clampsPillInsideDisplayMargins() {
        val result = compute(cutouts = listOf(CutoutBounds(1010, 0, 1060, 80)))

        assertEquals(1080 - 8 * 3, result.x + result.width)
        assertTrue(result.x >= 8 * 3)
        assertEquals(80 + 4 * 3, result.y)
    }

    @Test
    fun noCutout_usesCompactTopCenterFallbackBelowStatusBar() {
        val result = compute()

        assertEquals(540, result.x + result.width / 2)
        assertEquals(72 + 8 * 3, result.y)
        assertEquals(0, result.contentBounds.top)
        assertEquals(60 * 3, result.height)
        assertTrue(result.avoidedCutouts.isEmpty())
        assertTrue(result.bottomCornerRadius in 12 * 3f..20 * 3f)
    }

    @Test
    fun shallowPunchHole_neverPlacesContentInsideStatusBar() {
        val result = compute(cutouts = listOf(CutoutBounds(520, 0, 560, 48)))

        assertEquals(72 + 4 * 3, result.y)
        assertEquals(0, result.contentBounds.top)
    }

    @Test
    fun narrowDisplay_preservesSideMarginsAndPositiveSize() {
        val result = compute(screenWidth = 320, density = 2f)

        assertEquals(16, result.x)
        assertEquals(320 - 32, result.width)
        assertTrue(result.contentBounds.width > 0)
        assertTrue(result.contentBounds.height > 0)
    }

    @Test
    fun expandedReadout_isCompactAndStillCutoutSafe() {
        val result = compute(
            cutouts = listOf(CutoutBounds(510, 0, 570, 90)),
            expanded = true,
        )

        assertEquals(280 * 3, result.width)
        assertEquals(540, result.x + result.width / 2)
        assertEquals(90 + 4 * 3, result.y)
        assertEquals(0, result.contentBounds.top)
        assertEquals(60 * 3, result.contentBounds.height)
    }

    @Test
    fun multiplePunchHoles_useDeepestIntersectionAsSafeLine() {
        val result = compute(
            cutouts = listOf(
                CutoutBounds(430, 0, 470, 64),
                CutoutBounds(600, 0, 648, 104),
                CutoutBounds(0, 0, 24, 140), // outside the centred overlay
            ),
            expanded = true,
        )

        assertEquals(2, result.avoidedCutouts.size)
        assertEquals(104 + 4 * 3, result.y)
        assertTrue(result.avoidedCutouts.all { it.bottom < result.y })
    }

    @Test
    fun wideCutout_expandsAmbientPillWithinMaximum() {
        val result = compute(cutouts = listOf(CutoutBounds(290, 0, 790, 100)))

        assertEquals(240 * 3, result.width)
        assertEquals(540, result.x + result.width / 2)
    }
}

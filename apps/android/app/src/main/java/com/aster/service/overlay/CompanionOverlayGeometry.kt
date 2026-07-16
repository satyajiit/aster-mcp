package com.aster.service.overlay

import kotlin.math.roundToInt

/** Android-free inputs so placement can be exhaustively unit-tested on the JVM. */
data class CutoutBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Int get() = left + width / 2
}

/** A local rectangle inside the overlay window which is guaranteed not to be occluded. */
data class SafeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
}

data class OverlayGeometry(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val bottomCornerRadius: Float,
    /** The only region in which text, face features, progress, and touch may live. */
    val contentBounds: SafeBounds,
    /** Top sensors which forced the window below their deepest edge. */
    val avoidedCutouts: List<CutoutBounds>,
)

/**
 * Pure placement policy for the Android companion notch.
 *
 * Android devices do not share one notch shape: a display can report a centred notch,
 * one or more punch holes, or no cutout at all. The complete interactive window begins
 * below the deepest horizontally intersecting cutout plus a safety gap. Keeping the
 * window itself out of the occluded region is intentional: Android's public overlay API
 * exposes rectangular input windows, so a transparent wing over the status bar would
 * still steal taps. This policy keeps both pixels and input outside every camera region.
 */
object CompanionOverlayGeometry {
    const val CROP_WIDTH = 200
    const val CROP_HEIGHT = 96

    fun compute(
        screenWidthPx: Int,
        density: Float,
        cutouts: List<CutoutBounds>,
        statusBarHeightPx: Int,
        expanded: Boolean,
        minWidthDp: Int,
        maxWidthDp: Int,
        expandedWidthDp: Int,
        sideMarginDp: Int,
        cutoutSideRoomDp: Int,
        safeGapDp: Int,
        contentHeightDp: Int,
        fallbackTopDp: Int,
    ): OverlayGeometry {
        val safeDensity = density.coerceAtLeast(0.1f)
        fun dp(value: Int): Int = (value * safeDensity).roundToInt()

        val screenWidth = screenWidthPx.coerceAtLeast(1)
        val margin = dp(sideMarginDp).coerceAtMost((screenWidth - 1) / 2)
        val availableWidth = (screenWidth - margin * 2).coerceAtLeast(1)
        val validCutouts = cutouts.filter { it.width > 0 && it.height > 0 }
        val anchor = validCutouts.minByOrNull { kotlin.math.abs(it.centerX - screenWidth / 2) }
        val ambientWidth = maxOf(
            dp(minWidthDp),
            (anchor?.width ?: 0) + dp(cutoutSideRoomDp),
        ).coerceAtMost(dp(maxWidthDp))
        val requestedWidth = if (expanded) maxOf(ambientWidth, dp(expandedWidthDp)) else ambientWidth
        val maximumWidth = dp(if (expanded) expandedWidthDp else maxWidthDp)
        val width = requestedWidth.coerceAtMost(maximumWidth).coerceAtMost(availableWidth).coerceAtLeast(1)

        val anchorX = anchor?.centerX ?: screenWidth / 2
        val minX = margin
        val maxX = (screenWidth - margin - width).coerceAtLeast(minX)
        val x = (anchorX - width / 2).coerceIn(minX, maxX)
        val avoidedCutouts = validCutouts.filter { cutout ->
            cutout.right > x && cutout.left < x + width
        }
        val y = if (avoidedCutouts.isEmpty()) {
            statusBarHeightPx.coerceAtLeast(0) + dp(fallbackTopDp)
        } else {
            maxOf(
                statusBarHeightPx.coerceAtLeast(0),
                avoidedCutouts.maxOf { it.bottom }.coerceAtLeast(0),
            ) + dp(safeGapDp)
        }
        val contentHeight = dp(contentHeightDp).coerceAtLeast(1)
        val height = contentHeight
        val cornerRadius = (contentHeight * 0.30f).coerceIn(dp(12).toFloat(), dp(20).toFloat())

        return OverlayGeometry(
            x = x,
            y = y,
            width = width,
            height = height,
            bottomCornerRadius = cornerRadius,
            contentBounds = SafeBounds(0, 0, width, height),
            avoidedCutouts = avoidedCutouts,
        )
    }
}

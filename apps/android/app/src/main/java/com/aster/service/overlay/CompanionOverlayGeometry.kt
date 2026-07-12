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
    val centerX: Int get() = left + width / 2
}

data class OverlayGeometry(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val bottomCornerRadius: Float,
)

/**
 * Pure placement policy for the Android companion notch.
 *
 * The visible face uses the desktop renderer's 200×96 feature-band crop. A real
 * cutout anchors the pill at its horizontal centre and top edge; without one the
 * same pill sits below the status bar and makes no claim about nonexistent hardware.
 */
object CompanionOverlayGeometry {
    const val CROP_WIDTH = 200
    const val CROP_HEIGHT = 96

    fun compute(
        screenWidthPx: Int,
        density: Float,
        cutout: CutoutBounds?,
        statusBarHeightPx: Int,
        minWidthDp: Int,
        maxWidthDp: Int,
        sideMarginDp: Int,
        cutoutSideRoomDp: Int,
        cutoutHangDp: Int,
        fallbackTopDp: Int,
    ): OverlayGeometry {
        fun dp(value: Int): Int = (value * density).roundToInt()

        val margin = dp(sideMarginDp)
        val availableWidth = (screenWidthPx - margin * 2).coerceAtLeast(1)
        val desiredWidth = maxOf(
            dp(minWidthDp),
            (cutout?.width ?: 0) + dp(cutoutSideRoomDp),
        )
        val width = desiredWidth.coerceAtMost(dp(maxWidthDp)).coerceAtMost(availableWidth)
        val cropHeight = (width * CROP_HEIGHT.toFloat() / CROP_WIDTH).roundToInt()
        val height = if (cutout == null) {
            cropHeight
        } else {
            maxOf(cropHeight, cutout.bottom + dp(cutoutHangDp))
        }
        val anchorX = cutout?.centerX ?: screenWidthPx / 2
        val x = (anchorX - width / 2).coerceIn(0, (screenWidthPx - width).coerceAtLeast(0))
        val y = cutout?.top?.coerceAtLeast(0)
            ?: (statusBarHeightPx + dp(fallbackTopDp))
        // Desktop parity: CSS uses `0 0 r r`, where r is 24% of the visible
        // hang below the hardware notch, clamped to 12…24 logical pixels.
        val hang = if (cutout == null) height else (height - cutout.bottom).coerceAtLeast(0)
        val bottomCornerRadius = (hang * 0.24f).coerceIn(dp(12).toFloat(), dp(24).toFloat())

        return OverlayGeometry(
            x = x,
            y = y,
            width = width,
            height = height,
            bottomCornerRadius = bottomCornerRadius,
        )
    }
}

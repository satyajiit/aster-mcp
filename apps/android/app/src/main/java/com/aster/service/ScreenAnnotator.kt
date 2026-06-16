package com.aster.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max

/**
 * One numbered box to overlay. [bounds] is in SCREENSHOT-pixel space (the caller multiplies
 * real-px observe bounds by the screenshot `scale` before constructing the mark).
 */
data class Mark(val index: Int, val bounds: Rect)

/**
 * Draws a Set-of-Marks overlay (numbered boxes) onto a copy of a screenshot bitmap.
 *
 * The box-layout math ([clampRect], [labelChipRect]) is extracted into pure Int->Rect functions so
 * it is JVM-unit-testable without a real Bitmap. [annotate] is a thin Canvas wrapper around them.
 */
object ScreenAnnotator {

    private const val BOX_STROKE = 4f
    private const val CHIP_HEIGHT = 36
    private const val CHIP_PAD = 8
    private const val CHIP_DIGIT_WIDTH = 18 // approx per-character advance for the label text size
    private const val LABEL_TEXT_SIZE = 28f
    private val BOX_COLOR = Color.rgb(0xFF, 0x3B, 0x30)   // high-contrast red, vision-model friendly
    private val CHIP_COLOR = Color.rgb(0xFF, 0x3B, 0x30)
    private val LABEL_COLOR = Color.WHITE

    /** Clamp a box to [0,bmpW] x [0,bmpH]. Pure — no Bitmap needed. */
    fun clampRect(left: Int, top: Int, right: Int, bottom: Int, bmpW: Int, bmpH: Int): Rect {
        val l = left.coerceIn(0, bmpW)
        val t = top.coerceIn(0, bmpH)
        val r = right.coerceIn(0, bmpW)
        val b = bottom.coerceIn(0, bmpH)
        return Rect(l, t, max(l, r), max(t, b))
    }

    /**
     * Rect for the numbered label chip of a box. Anchored to the box top-left; placed ABOVE the box
     * (chip bottom == box top) when there is room, otherwise clamped to the top edge so it never
     * spills off-screen. Width scales with the digit count. Pure — no Bitmap needed.
     */
    fun labelChipRect(boxLeft: Int, boxTop: Int, index: Int, bmpW: Int, bmpH: Int): Rect {
        val digits = index.toString().length
        val width = CHIP_PAD * 2 + digits * CHIP_DIGIT_WIDTH
        val left = boxLeft.coerceIn(0, max(0, bmpW - width))
        val right = (left + width).coerceAtMost(bmpW)
        // Prefer above the box: top = boxTop - CHIP_HEIGHT. If that is negative, clamp top to 0
        // (chip then sits below the box top, overlapping the box's first rows).
        val rawTop = boxTop - CHIP_HEIGHT
        val top = rawTop.coerceIn(0, max(0, bmpH - CHIP_HEIGHT))
        val bottom = if (rawTop >= 0) boxTop.coerceAtMost(bmpH) else (top + CHIP_HEIGHT).coerceAtMost(bmpH)
        return Rect(left, top, right, bottom)
    }

    /**
     * Returns a NEW annotated ARGB_8888 bitmap; does not mutate [src] and does not recycle it
     * (the caller owns [src] and its lifecycle / JPEG write).
     */
    fun annotate(src: Bitmap, marks: List<Mark>): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = BOX_STROKE
            color = BOX_COLOR
        }
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = CHIP_COLOR
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LABEL_COLOR
            textSize = LABEL_TEXT_SIZE
            isFakeBoldText = true
        }

        for (mark in marks) {
            val box = clampRect(
                mark.bounds.left, mark.bounds.top, mark.bounds.right, mark.bounds.bottom,
                out.width, out.height
            )
            if (box.width() <= 0 || box.height() <= 0) continue
            canvas.drawRect(box, boxPaint)

            val chip = labelChipRect(box.left, box.top, mark.index, out.width, out.height)
            canvas.drawRect(chip, chipPaint)
            // Baseline a few px above the chip bottom so the digits sit inside the chip.
            canvas.drawText(
                mark.index.toString(),
                (chip.left + CHIP_PAD).toFloat(),
                (chip.bottom - CHIP_PAD).toFloat(),
                labelPaint
            )
        }
        return out
    }
}

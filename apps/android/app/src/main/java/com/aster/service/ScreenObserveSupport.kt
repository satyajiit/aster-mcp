package com.aster.service

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.math.roundToInt

/**
 * Pure, JVM-testable helpers shared with the P1 `observe` action.
 *
 * P4 owns these so the OCR-merge contract can land and be unit-tested independently of P1.
 * When P1 builds the `observe` branch it calls [isSparse] / [mergeOcr] / [ocrBlockToElement]
 * (see PLAN-P4 Task 7 for the exact wiring P1 adds).
 */
object ScreenObserveSupport {

    /** Concrete N for SPEC §3.4 "< N actionable elements but screen non-trivial". Owner-tunable. */
    const val SPARSE_THRESHOLD = 3

    /** OCR pseudo-element ref prefix, distinct from P1's a11y `e<idx>` so refs never collide. */
    const val OCR_REF_PREFIX = "o"

    data class MergeResult(val elements: List<JsonObject>, val source: String)

    /**
     * SPEC §3.4 sparsity heuristic. Sparse = fewer than [SPARSE_THRESHOLD] actionable elements
     * AND the screen has non-trivial content worth reading (a genuinely blank screen is not sparse).
     */
    fun isSparse(actionableCount: Int, hasNonTrivialContent: Boolean): Boolean {
        return hasNonTrivialContent && actionableCount < SPARSE_THRESHOLD
    }

    /**
     * Convert an [OcrBlock] (screenshot-px bounds) into a SPEC §3.1 element JSON object in REAL
     * screen pixels. Real px = screenshot px / [scale] (the inverse of the screenshot downscale).
     * [scale] == 1.0 when the screenshot was not downscaled (realW <= 1280).
     */
    fun ocrBlockToElement(block: OcrBlock, index: Int, scale: Float): JsonObject {
        val inv = if (scale <= 0f) 1.0f else 1.0f / scale
        val x = (block.bounds.left * inv).roundToInt()
        val y = (block.bounds.top * inv).roundToInt()
        val w = (block.bounds.width() * inv).roundToInt()
        val h = (block.bounds.height() * inv).roundToInt()
        return buildJsonObject {
            put("ref", "$OCR_REF_PREFIX$index")
            put("role", "text")
            put("text", block.text)
            put("desc", "")
            putJsonObject("bounds") {
                put("x", x)
                put("y", y)
                put("w", w)
                put("h", h)
                put("cx", x + w / 2)
                put("cy", y + h / 2)
            }
            put("source", "ocr")
        }
    }

    /** Convert recognized blocks to OCR pseudo-elements (real-px), preserving order. */
    fun ocrBlocksToElements(blocks: List<OcrBlock>, scale: Float): List<JsonObject> {
        return blocks.mapIndexed { i, b -> ocrBlockToElement(b, i, scale) }
    }

    /**
     * Merge a11y elements with OCR pseudo-elements per SPEC §3.1 `source` enum.
     * - not sparse           -> keep a11y only, source "a11y" (OCR discarded — tree is rich enough)
     * - a11y empty + ocr      -> source "ocr"
     * - a11y sparse + ocr     -> a11y first then OCR appended, source "merged"
     * - sparse but no ocr     -> a11y only, source "a11y"
     */
    fun mergeOcr(
        a11yElements: List<JsonObject>,
        ocrElements: List<JsonObject>,
        a11ySparse: Boolean
    ): MergeResult {
        if (!a11ySparse || ocrElements.isEmpty()) {
            return MergeResult(a11yElements, "a11y")
        }
        if (a11yElements.isEmpty()) {
            return MergeResult(ocrElements, "ocr")
        }
        return MergeResult(a11yElements + ocrElements, "merged")
    }

    /** Build the SPEC §3.1 `elements` array as a JsonArray from a [MergeResult]. */
    fun elementsToJsonArray(result: MergeResult): JsonArray = JsonArray(result.elements)
}

package com.aster.service

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenObserveSupportTest {

    // --- isSparse ---------------------------------------------------------

    @Test
    fun isSparse_trueWhenFewActionableButContentNonTrivial() {
        assertTrue(ScreenObserveSupport.isSparse(actionableCount = 0, hasNonTrivialContent = true))
        assertTrue(ScreenObserveSupport.isSparse(actionableCount = 2, hasNonTrivialContent = true))
    }

    @Test
    fun isSparse_falseAtOrAboveThreshold() {
        // Threshold N = 3: 3+ actionable elements is NOT sparse.
        assertFalse(ScreenObserveSupport.isSparse(actionableCount = 3, hasNonTrivialContent = true))
        assertFalse(ScreenObserveSupport.isSparse(actionableCount = 9, hasNonTrivialContent = true))
    }

    @Test
    fun isSparse_falseWhenContentTrivialEvenWithZeroActionable() {
        // A genuinely blank screen is not "sparse needing OCR" — nothing to read.
        assertFalse(ScreenObserveSupport.isSparse(actionableCount = 0, hasNonTrivialContent = false))
    }

    // --- ocrBlockToElement ------------------------------------------------

    @Test
    fun ocrBlockToElement_mapsScreenshotPxToRealPxAndTagsSource() {
        // Screenshot px (scale = 1280/2160 ~ 0.5926). Real px = screenshot px / scale.
        val scale = 0.5f
        // Box in screenshot space: left=100, top=200, right=300, bottom=260.
        val block = OcrBlock(
            text = "Continue",
            bounds = android.graphics.Rect(100, 200, 300, 260),
            confidence = 1.0f
        )
        val el = ScreenObserveSupport.ocrBlockToElement(block, index = 4, scale = scale)

        assertEquals("o4", el["ref"]!!.jsonPrimitive.content)
        assertEquals("text", el["role"]!!.jsonPrimitive.content)
        assertEquals("Continue", el["text"]!!.jsonPrimitive.content)
        assertEquals("ocr", el["source"]!!.jsonPrimitive.content)

        val b = el["bounds"]!!.jsonObject
        // 100 / 0.5 = 200, 200 / 0.5 = 400, width = (300-100)/0.5 = 400, height = (260-200)/0.5 = 120.
        assertEquals(200, b["x"]!!.jsonPrimitive.content.toInt())
        assertEquals(400, b["y"]!!.jsonPrimitive.content.toInt())
        assertEquals(400, b["w"]!!.jsonPrimitive.content.toInt())
        assertEquals(120, b["h"]!!.jsonPrimitive.content.toInt())
        // cx = x + w/2 = 200 + 200 = 400; cy = y + h/2 = 400 + 60 = 460.
        assertEquals(400, b["cx"]!!.jsonPrimitive.content.toInt())
        assertEquals(460, b["cy"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun ocrBlockToElement_scaleOneIsIdentity() {
        val block = OcrBlock("Hi", android.graphics.Rect(10, 20, 50, 60), 1.0f)
        val b = ScreenObserveSupport.ocrBlockToElement(block, 0, scale = 1.0f)["bounds"]!!.jsonObject
        assertEquals(10, b["x"]!!.jsonPrimitive.content.toInt())
        assertEquals(20, b["y"]!!.jsonPrimitive.content.toInt())
        assertEquals(40, b["w"]!!.jsonPrimitive.content.toInt())
        assertEquals(40, b["h"]!!.jsonPrimitive.content.toInt())
    }

    // --- mergeOcr ---------------------------------------------------------

    private fun a11yEl(ref: String) = buildJsonObject {
        put("ref", ref)
        put("role", "button")
        put("source", "a11y")
    }

    private fun ocrEl(ref: String) = buildJsonObject {
        put("ref", ref)
        put("role", "text")
        put("source", "ocr")
    }

    @Test
    fun mergeOcr_a11ySourceWhenNotSparse() {
        val a11y = listOf(a11yEl("e1"), a11yEl("e2"), a11yEl("e3"))
        val res = ScreenObserveSupport.mergeOcr(a11y, listOf(ocrEl("o0")), a11ySparse = false)
        assertEquals("a11y", res.source)
        // Not sparse => OCR is discarded; only the a11y elements survive.
        assertEquals(3, res.elements.size)
    }

    @Test
    fun mergeOcr_ocrSourceWhenA11yEmpty() {
        val res = ScreenObserveSupport.mergeOcr(emptyList(), listOf(ocrEl("o0"), ocrEl("o1")), a11ySparse = true)
        assertEquals("ocr", res.source)
        assertEquals(2, res.elements.size)
    }

    @Test
    fun mergeOcr_mergedSourceWhenA11ySparseAndOcrPresent() {
        val a11y = listOf(a11yEl("e1"))
        val res = ScreenObserveSupport.mergeOcr(a11y, listOf(ocrEl("o0")), a11ySparse = true)
        assertEquals("merged", res.source)
        assertEquals(2, res.elements.size)
        // a11y elements come first, OCR appended.
        assertEquals("e1", (res.elements[0] as JsonObject)["ref"]!!.jsonPrimitive.content)
        assertEquals("o0", (res.elements[1] as JsonObject)["ref"]!!.jsonPrimitive.content)
    }

    @Test
    fun mergeOcr_a11ySourceWhenSparseButNoOcr() {
        val a11y = listOf(a11yEl("e1"))
        val res = ScreenObserveSupport.mergeOcr(a11y, emptyList(), a11ySparse = true)
        assertEquals("a11y", res.source)
        assertEquals(1, res.elements.size)
    }
}

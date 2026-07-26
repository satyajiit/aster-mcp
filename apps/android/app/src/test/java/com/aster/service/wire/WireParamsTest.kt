package com.aster.service.wire

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the snake_case↔camelCase normalisation that keeps the kernel's emitted
 * params reachable by handlers that read the other convention.
 *
 * The six entries in [driftedParams] are the real, verified drifts between
 * `aster-one`'s `cortex-tools-screen` emitters and this companion's handlers —
 * each one was a silently-dropped key before this shim.
 */
class WireParamsTest {

    private fun params(vararg pairs: Pair<String, String>): Map<String, JsonElement> =
        pairs.associate { (k, v) -> k to JsonPrimitive(v) }

    /** kernel spelling → the companion spelling that used to miss it. */
    private val driftedParams = listOf(
        "search_text" to "searchText",     // observe — narrowing was inoperative
        "max_elements" to "maxElements",   // observe — element cap ignored
        "until_text" to "untilText",       // scroll — scroll-to-find was one swipe
        "quiet_ms" to "quietMs",           // wait_for_idle — tuned settle ignored
        "view_id" to "viewId",             // wait_for — HARD failure before this
        "gesture_type" to "gestureType",   // input_gesture — required param
    )

    @Test
    fun `every drifted kernel param becomes reachable under the companion spelling`() {
        for ((kernelKey, companionKey) in driftedParams) {
            val out = WireParams.normalize(params(kernelKey to "v"))
            assertEquals(
                "kernel `$kernelKey` must resolve as `$companionKey`",
                JsonPrimitive("v"),
                out?.get(companionKey),
            )
            // The original spelling must survive — other handlers read it.
            assertEquals(JsonPrimitive("v"), out?.get(kernelKey))
        }
    }

    @Test
    fun `camelCase input is also reachable under snake_case`() {
        val out = WireParams.normalize(params("searchText" to "hello"))
        assertEquals(JsonPrimitive("hello"), out?.get("search_text"))
        assertEquals(JsonPrimitive("hello"), out?.get("searchText"))
    }

    @Test
    fun `an explicitly supplied key is never overwritten by an alias`() {
        val out = WireParams.normalize(params("view_id" to "snake", "viewId" to "camel"))
        assertEquals(JsonPrimitive("snake"), out?.get("view_id"))
        assertEquals(JsonPrimitive("camel"), out?.get("viewId"))
    }

    @Test
    fun `single-word keys are untouched and the map is returned unchanged`() {
        val input = params("text" to "a", "role" to "button", "gone" to "true")
        val out = WireParams.normalize(input)
        // Nothing to alias, so we must not pay for a copy.
        assertSame(input, out)
        assertEquals(3, out?.size)
    }

    @Test
    fun `null and empty pass through`() {
        assertNull(WireParams.normalize(null))
        assertTrue(WireParams.normalize(emptyMap()).isNullOrEmpty())
    }

    @Test
    fun `side-channel params the kernel stamps are preserved verbatim`() {
        // IpcMode reads these off the map for the P7 audit log; aliasing must not
        // disturb the exact keys it looks up.
        val out = WireParams.normalize(
            params("target_text" to "Post", "risk" to "high", "ai_name" to "Aster"),
        )
        assertEquals(JsonPrimitive("Post"), out?.get("target_text"))
        assertEquals(JsonPrimitive("high"), out?.get("risk"))
        assertEquals(JsonPrimitive("Aster"), out?.get("ai_name"))
    }

    @Test
    fun `case conversion round-trips the shapes we actually send`() {
        assertEquals("searchText", WireParams.toCamelCase("search_text"))
        assertEquals("maxElements", WireParams.toCamelCase("max_elements"))
        assertEquals("a", WireParams.toCamelCase("a"))
        assertEquals("search_text", WireParams.toSnakeCase("searchText"))
        assertEquals("foreground_after", WireParams.toSnakeCase("foregroundAfter"))
        assertEquals("text", WireParams.toSnakeCase("text"))
    }

    @Test
    fun `degenerate underscore keys do not collide or crash`() {
        // A leading/doubled underscore has no letter to fold into. It must stay
        // distinguishable rather than aliasing onto another key.
        assertEquals("_largeResult", WireParams.toCamelCase("_large_result"))
        val out = WireParams.normalize(params("_large_result" to "x"))
        assertEquals(JsonPrimitive("x"), out?.get("_large_result"))
    }
}

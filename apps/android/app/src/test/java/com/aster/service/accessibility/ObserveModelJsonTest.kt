package com.aster.service.accessibility

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveModelJsonTest {

    private fun sampleResult(): ObserveResult {
        val bounds = Bounds.fromLTRB(980, 2180, 1076, 2276)
        val element = ObservedElement(
            ref = "e7",
            role = "button",
            text = "Send",
            desc = "",
            viewId = "com.whatsapp:id/send",
            window = 12,
            bounds = bounds,
            state = ElementState(
                clickable = true, editable = false, checkable = false, checked = false,
                scrollable = false, selected = false, focused = false, enabled = true,
                password = false,
            ),
            actions = listOf("click", "long_click"),
        )
        val screen = ScreenContext(
            width = 1080, height = 2400, density = 2.75f, rotation = 0,
            foregroundPackage = "com.whatsapp", activity = "com.whatsapp.HomeActivity",
            imeVisible = false,
            windows = listOf(WindowInfo(id = 12, type = "application", title = "WhatsApp", focused = true)),
        )
        return ObserveResult(
            screen = screen,
            elements = listOf(element),
            scrollables = listOf(Scrollable(ref = "e3", bounds = bounds, directions = listOf("up", "down"))),
            source = "a11y",
            snapshotId = "s1-123",
            truncated = false,
        )
    }

    @Test
    fun emits_exact_spec_top_level_keys() {
        val json: JsonObject = sampleResult().toJson()
        assertTrue(json.containsKey("screen"))
        assertTrue(json.containsKey("elements"))
        assertTrue(json.containsKey("scrollables"))
        assertTrue(json.containsKey("source"))
        assertTrue(json.containsKey("snapshot_id"))
        assertTrue(json.containsKey("truncated"))
        assertEquals("a11y", json["source"]!!.jsonPrimitive.content)
        assertEquals("s1-123", json["snapshot_id"]!!.jsonPrimitive.content)
        assertEquals(false, json["truncated"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun screen_uses_snake_case_spec_keys() {
        val screen = sampleResult().toJson()["screen"]!!.jsonObject
        assertEquals(1080, screen["width"]!!.jsonPrimitive.int)
        assertEquals(2400, screen["height"]!!.jsonPrimitive.int)
        assertTrue(screen.containsKey("density"))
        assertEquals(0, screen["rotation"]!!.jsonPrimitive.int)
        assertEquals("com.whatsapp", screen["foreground_package"]!!.jsonPrimitive.content)
        assertEquals("com.whatsapp.HomeActivity", screen["activity"]!!.jsonPrimitive.content)
        assertEquals(false, screen["ime_visible"]!!.jsonPrimitive.boolean)
        assertTrue(screen.containsKey("windows"))
    }

    @Test
    fun window_keys_match_spec() {
        val window = sampleResult().toJson()["screen"]!!.jsonObject["windows"]!!
            .jsonArray[0].jsonObject
        assertEquals(12, window["id"]!!.jsonPrimitive.int)
        assertEquals("application", window["type"]!!.jsonPrimitive.content)
        assertEquals("WhatsApp", window["title"]!!.jsonPrimitive.content)
        assertEquals(true, window["focused"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun element_keys_and_bounds_and_state_match_spec() {
        val el = sampleResult().toJson()["elements"]!!.jsonArray[0].jsonObject
        assertEquals("e7", el["ref"]!!.jsonPrimitive.content)
        assertEquals("button", el["role"]!!.jsonPrimitive.content)
        assertEquals("Send", el["text"]!!.jsonPrimitive.content)
        assertTrue(el.containsKey("desc"))
        assertEquals("com.whatsapp:id/send", el["viewId"]!!.jsonPrimitive.content)
        assertEquals(12, el["window"]!!.jsonPrimitive.int)

        val b = el["bounds"]!!.jsonObject
        assertEquals(980, b["x"]!!.jsonPrimitive.int)
        assertEquals(2180, b["y"]!!.jsonPrimitive.int)
        assertEquals(96, b["w"]!!.jsonPrimitive.int)
        assertEquals(96, b["h"]!!.jsonPrimitive.int)
        assertEquals(1028, b["cx"]!!.jsonPrimitive.int)
        assertEquals(2228, b["cy"]!!.jsonPrimitive.int)

        val s = el["state"]!!.jsonObject
        listOf(
            "clickable", "editable", "checkable", "checked", "scrollable",
            "selected", "focused", "enabled", "password",
        ).forEach { assertTrue("state missing $it", s.containsKey(it)) }

        val actions = el["actions"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("click", "long_click"), actions)
    }

    @Test
    fun scrollable_keys_match_spec() {
        val sc = sampleResult().toJson()["scrollables"]!!.jsonArray[0].jsonObject
        assertEquals("e3", sc["ref"]!!.jsonPrimitive.content)
        assertTrue(sc.containsKey("bounds"))
        assertEquals(listOf("up", "down"), sc["directions"]!!.jsonArray.map { it.jsonPrimitive.content })
    }
}

package com.aster.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.content
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenActionResultTest {

    @Test
    fun resolvedBy_wireValues_matchSpec() {
        // SPEC §3.1 strategy names — exact.
        assertEquals("viewId", ScreenActionResult.ResolvedBy.VIEW_ID.wire)
        assertEquals("text_role", ScreenActionResult.ResolvedBy.TEXT_ROLE.wire)
        assertEquals("nearest_bounds", ScreenActionResult.ResolvedBy.NEAREST_BOUNDS.wire)
        assertEquals("center_tap", ScreenActionResult.ResolvedBy.CENTER_TAP.wire)
    }

    @Test
    fun ok_buildsSuccessShape() {
        val result = ScreenActionResult.ok(
            ScreenActionResult.ResolvedBy.VIEW_ID
        ) {
            put("clicked", true)
        }
        assertTrue(result.success)
        val data = result.data as JsonObject
        assertTrue(data["ok"]!!.jsonPrimitive.boolean)
        assertEquals("viewId", data["resolved_by"]!!.jsonPrimitive.content)
        assertTrue(data["clicked"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun staleRef_buildsStructuredFailure() {
        val result = ScreenActionResult.staleRef("e7", "snap-3")
        assertFalse(result.success)
        assertNull(result.data)
        // error is a JSON-encoded object the agent can parse to decide to re-observe.
        val obj = Json.parseToJsonElement(result.error!!) as JsonObject
        assertEquals("stale_ref", obj["code"]!!.jsonPrimitive.content)
        assertEquals("e7", obj["ref"]!!.jsonPrimitive.content)
        assertEquals("snap-3", obj["snapshot_id"]!!.jsonPrimitive.content)
        assertEquals("re-observe", obj["hint"]!!.jsonPrimitive.content)
    }

    @Test
    fun staleRef_nullSnapshot_omitsNothingBreaks() {
        val result = ScreenActionResult.staleRef("e2", null)
        val obj = Json.parseToJsonElement(result.error!!) as JsonObject
        assertEquals("stale_ref", obj["code"]!!.jsonPrimitive.content)
        assertEquals("e2", obj["ref"]!!.jsonPrimitive.content)
        // null snapshot serializes as JSON null, still parseable.
        assertTrue(obj.containsKey("snapshot_id"))
    }

    @Test
    fun descriptorMatches_viewId_isStrongMatch() {
        // Same viewId → match regardless of text drift.
        assertTrue(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "com.x:id/send", nodeViewId = "com.x:id/send",
                cachedText = "Send", nodeText = "Sent",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.Button", nodeClassName = "android.widget.Button"
            )
        )
    }

    @Test
    fun descriptorMatches_textPlusRole_whenNoViewId() {
        // No viewId on either side → fall back to text + role/className equality.
        assertTrue(
            ScreenActionResult.descriptorMatches(
                cachedViewId = null, nodeViewId = null,
                cachedText = "Send", nodeText = "Send",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.Button", nodeClassName = "android.widget.Button"
            )
        )
    }

    @Test
    fun descriptorMatches_textDrift_noViewId_isMismatch() {
        // Text changed and no viewId anchor → mismatch (must NOT act).
        assertFalse(
            ScreenActionResult.descriptorMatches(
                cachedViewId = null, nodeViewId = null,
                cachedText = "Send", nodeText = "Delete",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.Button", nodeClassName = "android.widget.Button"
            )
        )
    }

    @Test
    fun descriptorMatches_viewIdConflict_isMismatch() {
        // Both have viewIds but they differ → hard mismatch.
        assertFalse(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "com.x:id/send", nodeViewId = "com.x:id/cancel",
                cachedText = "Send", nodeText = "Send",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.Button", nodeClassName = "android.widget.Button"
            )
        )
    }
}

package com.aster.service

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.put
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
                cachedDesc = "", nodeDesc = "",
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
                cachedDesc = "", nodeDesc = "",
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
                cachedDesc = "", nodeDesc = "",
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
                cachedDesc = "", nodeDesc = "",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.Button", nodeClassName = "android.widget.Button"
            )
        )
    }

    @Test
    fun descriptorMatches_iconOnlyButtons_areDistinguishedByDescription() {
        // The case that made this gate close to vacuous. Two icon-only toolbar
        // buttons: no viewId, no text, same role and class. Before `desc` was
        // carried, the predicate reduced to `"" == ""` + a role comparison, so
        // EVERY icon button matched the descriptor and the nearest-bounds
        // strategy picked among them by proximity — then tapped it.
        assertFalse(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "", nodeViewId = "",
                cachedText = "", nodeText = "",
                cachedDesc = "Share", nodeDesc = "Delete",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.ImageButton",
                nodeClassName = "android.widget.ImageButton"
            )
        )
        assertTrue(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "", nodeViewId = "",
                cachedText = "", nodeText = "",
                cachedDesc = "Share", nodeDesc = "Share",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.ImageButton",
                nodeClassName = "android.widget.ImageButton"
            )
        )
    }

    @Test
    fun descriptorMatches_anonymousRows_areDistinguishedByAggregatedLabel() {
        // The dominant Android list-row shape: a clickable LinearLayout with no
        // viewId, no text and no desc. Every other clause of the gate is vacuous
        // here, so without the label two DIFFERENT rows match each other and
        // nearest-bounds taps whichever is closer to a stale rectangle.
        assertFalse(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "", nodeViewId = "",
                cachedText = "", nodeText = "",
                cachedDesc = "", nodeDesc = "",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.LinearLayout",
                nodeClassName = "android.widget.LinearLayout",
                cachedLabel = "Network & internet · Mobile, Wi-Fi, hotspot",
                nodeLabel = { "Connected devices · Bluetooth, pairing" }
            )
        )
        assertTrue(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "", nodeViewId = "",
                cachedText = "", nodeText = "",
                cachedDesc = "", nodeDesc = "",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.LinearLayout",
                nodeClassName = "android.widget.LinearLayout",
                cachedLabel = "Network & internet · Mobile, Wi-Fi, hotspot",
                nodeLabel = { "Network & internet · Mobile, Wi-Fi, hotspot" }
            )
        )
    }

    @Test
    fun descriptorMatches_labelSurvivesVolatileSummaryDrift() {
        // A row's title identifies it; its summary is live state. An Uber ride row
        // re-prices every few seconds and a Settings row's summary changes when a
        // SIM is pulled. Requiring byte equality would fail closed on a row that
        // has not moved — the same wrong-but-honest failure this path exists to
        // stop producing.
        assertTrue(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "", nodeViewId = "",
                cachedText = "", nodeText = "",
                cachedDesc = "", nodeDesc = "",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.LinearLayout",
                nodeClassName = "android.widget.LinearLayout",
                cachedLabel = "UberGo · 4 min away · ₹243",
                nodeLabel = { "UberGo · 7 min away · ₹268" }
            )
        )
    }

    @Test
    fun descriptorMatches_labelIsRequiredWhenCachedHadOne() {
        // The live node lost its label entirely (subtree replaced by a spinner).
        // Not confidently the same row → fail closed, mirroring the viewId rule.
        assertFalse(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "", nodeViewId = "",
                cachedText = "", nodeText = "",
                cachedDesc = "", nodeDesc = "",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.LinearLayout",
                nodeClassName = "android.widget.LinearLayout",
                cachedLabel = "Network & internet",
                nodeLabel = { "" }
            )
        )
    }

    @Test
    fun descriptorMatches_labelProviderIsNotInvokedWhenCheapClausesFail() {
        // Re-deriving a live label walks the node's subtree, and the predicate is
        // called once per node on screen by findNearestMatchingNode. It must not
        // pay that cost for a node that already disagrees on text.
        var invoked = 0
        ScreenActionResult.descriptorMatches(
            cachedViewId = "", nodeViewId = "",
            cachedText = "Send", nodeText = "Cancel",
            cachedDesc = "", nodeDesc = "",
            cachedRole = "button", nodeRole = "button",
            cachedClassName = "android.widget.Button", nodeClassName = "android.widget.Button",
            cachedLabel = "Send", nodeLabel = { invoked++; "Send" }
        )
        assertEquals(0, invoked)
    }

    @Test
    fun descriptorMatches_noCachedLabel_behavesExactlyAsBefore() {
        // A descriptor whose node owned its own text never had a label; the new
        // clause must be inert for it, provider or not.
        var invoked = 0
        assertTrue(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "", nodeViewId = "",
                cachedText = "Send", nodeText = "Send",
                cachedDesc = "", nodeDesc = "",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.Button", nodeClassName = "android.widget.Button",
                cachedLabel = "", nodeLabel = { invoked++; "anything" }
            )
        )
        assertEquals(0, invoked)
    }

    @Test
    fun descriptorMatches_viewIdAnchorStillWinsOverDescriptionDrift() {
        // A viewId match is a STRONG match: a description that changed (locale,
        // state — "Play" → "Pause") must not veto it.
        assertTrue(
            ScreenActionResult.descriptorMatches(
                cachedViewId = "com.x:id/play", nodeViewId = "com.x:id/play",
                cachedText = "", nodeText = "",
                cachedDesc = "Play", nodeDesc = "Pause",
                cachedRole = "button", nodeRole = "button",
                cachedClassName = "android.widget.ImageButton",
                nodeClassName = "android.widget.ImageButton"
            )
        )
    }
}

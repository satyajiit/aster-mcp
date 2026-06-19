package com.aster.service.overlay

import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractiveOverlayModelTest {

    // ── parsePrompt ───────────────────────────────────────────────────────

    @Test
    fun parsePrompt_readsOptionsTextFieldAndName() {
        val params = buildJsonObject {
            put("prompt", "Use as-is, or ask each run?")
            put("ai_name", "Ada")
            putJsonArray("options") {
                addJsonObject { put("id", "hardcoded"); put("label", "Use exactly this") }
                addJsonObject { put("id", "dynamic"); put("label", "Ask me each run"); put("hint", "a {{key}}") }
            }
            putJsonObject("text_input") { put("label", "Input name"); put("hint", "e.g. post_content") }
            put("default", "hardcoded")
        }
        val prompt = InteractiveOverlayModel.parsePrompt(params)
        assertEquals("Use as-is, or ask each run?", prompt.prompt)
        assertEquals("Ada", prompt.aiName)
        assertEquals(2, prompt.options.size)
        assertEquals("dynamic", prompt.options[1].id)
        assertEquals("a {{key}}", prompt.options[1].hint)
        assertEquals("Input name", prompt.textInput?.label)
        assertEquals("hardcoded", prompt.default)
        // No timeout → default.
        assertEquals(InteractiveOverlayModel.DEFAULT_TIMEOUT_MS, prompt.timeoutMs)
    }

    @Test
    fun parsePrompt_defaultsAiNameWhenAbsent() {
        val params = buildJsonObject {
            put("prompt", "?")
            putJsonArray("options") { addJsonObject { put("id", "a"); put("label", "A") } }
        }
        assertEquals(InteractiveOverlayModel.DEFAULT_AI_NAME, InteractiveOverlayModel.parsePrompt(params).aiName)
    }

    @Test
    fun parsePrompt_missingPromptThrows() {
        val params = buildJsonObject {
            putJsonArray("options") { addJsonObject { put("id", "a"); put("label", "A") } }
        }
        assertThrows(IllegalArgumentException::class.java) { InteractiveOverlayModel.parsePrompt(params) }
    }

    @Test
    fun parsePrompt_emptyOptionsThrows() {
        val params = buildJsonObject {
            put("prompt", "?")
            putJsonArray("options") {}
        }
        assertThrows(IllegalArgumentException::class.java) { InteractiveOverlayModel.parsePrompt(params) }
    }

    // ── parseApproval ─────────────────────────────────────────────────────

    @Test
    fun parseApproval_readsVariantsAndEditableDefault() {
        val params = buildJsonObject {
            put("title", "Ada drafted 2 posts")
            putJsonArray("variants") {
                addJsonObject { put("id", "v1"); put("text", "First") }
                addJsonObject { put("id", "v2"); put("text", "Second") }
            }
        }
        val approval = InteractiveOverlayModel.parseApproval(params)
        assertEquals("Ada drafted 2 posts", approval.title)
        assertEquals(2, approval.variants.size)
        assertEquals("Second", approval.variants[1].text)
        assertTrue("editable defaults true", approval.editable)
    }

    @Test
    fun parseApproval_variantMissingTextThrows() {
        val params = buildJsonObject {
            put("title", "Drafts")
            putJsonArray("variants") { addJsonObject { put("id", "v1") } }
        }
        assertThrows(IllegalArgumentException::class.java) { InteractiveOverlayModel.parseApproval(params) }
    }

    // ── clampTimeoutMs ────────────────────────────────────────────────────

    @Test
    fun clampTimeoutMs_clampsBounds() {
        assertEquals(InteractiveOverlayModel.DEFAULT_TIMEOUT_MS, InteractiveOverlayModel.clampTimeoutMs(null))
        assertEquals(5_000L, InteractiveOverlayModel.clampTimeoutMs(10L))      // below min
        assertEquals(140_000L, InteractiveOverlayModel.clampTimeoutMs(999_999L)) // above max
        assertEquals(60_000L, InteractiveOverlayModel.clampTimeoutMs(60_000L)) // in range
        // Always strictly below the kernel's 150s host-call ceiling.
        assertTrue(InteractiveOverlayModel.clampTimeoutMs(999_999L) < 150_000L)
    }

    // ── result shapes ─────────────────────────────────────────────────────

    @Test
    fun chooserResult_shape() {
        val r = InteractiveOverlayModel.chooserResult("dynamic", "post_content")
        assertEquals("dynamic", r["choice"]?.jsonPrimitive?.content)
        assertEquals("post_content", r["text"]?.jsonPrimitive?.content)
        // Omits text when null.
        assertNull(InteractiveOverlayModel.chooserResult("cancel", null)["text"])
    }

    @Test
    fun approvalResult_shape() {
        val r = InteractiveOverlayModel.approvalResult("approve", "v2", "edited body")
        assertEquals("approve", r["decision"]?.jsonPrimitive?.content)
        assertEquals("v2", r["selected_id"]?.jsonPrimitive?.content)
        assertEquals("edited body", r["text"]?.jsonPrimitive?.content)
    }

    @Test
    fun timeoutAndCancelResults_perType() {
        val chooser = buildJsonObject {
            put("prompt", "?")
            putJsonArray("options") { addJsonObject { put("id", "a"); put("label", "A") } }
        }.let { InteractiveOverlayModel.parsePrompt(it) }
        assertEquals("cancel", chooser.timeoutResult()["choice"]?.jsonPrimitive?.content)
        assertEquals("cancel", chooser.cancelResult()["choice"]?.jsonPrimitive?.content)

        val approval = buildJsonObject {
            put("title", "T")
            putJsonArray("variants") { addJsonObject { put("id", "v1"); put("text", "x") } }
        }.let { InteractiveOverlayModel.parseApproval(it) }
        assertEquals("timeout", approval.timeoutResult()["decision"]?.jsonPrimitive?.content)
        assertEquals("reject", approval.cancelResult()["decision"]?.jsonPrimitive?.content)
    }
}

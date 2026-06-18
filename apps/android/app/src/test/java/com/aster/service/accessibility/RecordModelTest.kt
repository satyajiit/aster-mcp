package com.aster.service.accessibility

import android.view.accessibility.AccessibilityEvent
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure tests for the companion live-recorder model: [StepInferrer] (event → step
 * verb), [RecordBuffer] (bounded capture), and [RecordedStep] JSON (must be
 * loadable by the kernel's `automation_record_step` — `element` carries the SPEC
 * §3.1 viewId/text/desc/role/window/bounds the kernel `Selector` lifts).
 */
class RecordModelTest {

    private fun facts(
        editable: Boolean = false,
        checkable: Boolean = false,
        checked: Boolean = false,
        longClickable: Boolean = false,
        text: String = "",
        desc: String = "",
        viewId: String = "",
        role: String = "button",
    ) = RecordNodeFacts(editable, checkable, checked, longClickable, text, desc, viewId, role)

    private fun sampleElement() = ObservedElement(
        ref = "rec0",
        role = "button",
        text = "Post",
        desc = "",
        viewId = "com.linkedin.android:id/share_box",
        window = 1,
        bounds = Bounds.fromLTRB(500, 100, 620, 160),
        state = ElementState(
            clickable = true, editable = false, checkable = false, checked = false,
            scrollable = false, selected = false, focused = false, enabled = true,
            password = false,
        ),
        actions = listOf("click"),
    )

    // ---- StepInferrer ----

    @Test
    fun text_changed_on_editable_infers_set_text() {
        val shape = StepInferrer.infer(
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            facts(editable = true, role = "edittext", viewId = "com.x:id/field"),
            changedText = "hello world",
        )
        assertEquals(RecordStepKind.SET_TEXT, shape!!.kind)
        assertEquals("hello world", shape.params["text"]!!.jsonPrimitive.content)
        assertFalse(shape.params["submit"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun text_changed_on_non_editable_is_dropped() {
        val shape = StepInferrer.infer(
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            facts(editable = false, text = "label"),
            changedText = "noise",
        )
        assertNull(shape)
    }

    @Test
    fun click_on_checkable_infers_set_toggle_with_post_click_checked() {
        val on = StepInferrer.infer(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            facts(checkable = true, checked = true, role = "switch", text = "Notifications"),
            changedText = null,
        )
        assertEquals(RecordStepKind.SET_TOGGLE, on!!.kind)
        assertTrue(on.params["on"]!!.jsonPrimitive.boolean)

        val off = StepInferrer.infer(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            facts(checkable = true, checked = false, role = "switch", text = "Notifications"),
            changedText = null,
        )
        assertFalse(off!!.params["on"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun click_on_actionable_infers_tap() {
        val shape = StepInferrer.infer(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            facts(text = "Post", role = "button", viewId = "com.x:id/post"),
            changedText = null,
        )
        assertEquals(RecordStepKind.TAP, shape!!.kind)
        assertTrue(shape.params.isEmpty())
        assertTrue(shape.label.contains("Post"))
    }

    @Test
    fun click_on_inert_container_without_primitives_is_dropped() {
        // No viewId, no text/desc, inert role → nothing durable to replay.
        val shape = StepInferrer.infer(
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            facts(role = "other"),
            changedText = null,
        )
        assertNull(shape)
    }

    @Test
    fun long_click_records_tap_flagged_long_for_kernel_replay() {
        // No `long_press` kind exists in the kernel StepKind vocabulary; a long-click
        // is recorded as `tap` + params.long so engine::control_verb maps it to the
        // companion long_press verb at replay.
        val shape = StepInferrer.infer(
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
            facts(text = "Message", role = "listitem"),
            changedText = null,
        )
        assertEquals(RecordStepKind.TAP, shape!!.kind)
        assertTrue(shape.params["long"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun unrelated_event_type_returns_null() {
        val shape = StepInferrer.infer(
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            facts(text = "x"),
            changedText = null,
        )
        assertNull(shape)
    }

    // ---- RecordBuffer ----

    @Test
    fun offer_is_noop_while_inactive() {
        val buf = RecordBuffer()
        assertFalse(buf.isActive())
        assertFalse(buf.offer(RecordedStep("tap", "Tap", sampleElement(), buildJsonObject {}, "com.x")))
        assertEquals(0, buf.size())
    }

    @Test
    fun start_arms_and_buffers_then_stop_disarms_and_snapshots() {
        val buf = RecordBuffer()
        buf.start()
        assertTrue(buf.isActive())
        assertTrue(buf.offer(RecordedStep("tap", "Tap", sampleElement(), buildJsonObject {}, "com.x")))
        assertEquals(1, buf.size())

        val steps = buf.stop()
        assertFalse(buf.isActive())
        assertEquals(1, steps.size)
        // Stop does not clear — a status read after stop still reflects the session.
        assertEquals(1, buf.size())

        // After stop, offer is a no-op again.
        assertFalse(buf.offer(RecordedStep("tap", "Tap", sampleElement(), buildJsonObject {}, "com.x")))
    }

    @Test
    fun start_clears_prior_session() {
        val buf = RecordBuffer()
        buf.start()
        buf.offer(RecordedStep("tap", "Tap", sampleElement(), buildJsonObject {}, "com.x"))
        buf.start()
        assertEquals(0, buf.size())
        assertEquals(0, buf.droppedCount())
    }

    @Test
    fun buffer_is_bounded_and_counts_drops() {
        val buf = RecordBuffer()
        buf.start()
        repeat(RecordBuffer.MAX_STEPS + 5) {
            buf.offer(RecordedStep("tap", "Tap", sampleElement(), buildJsonObject {}, "com.x"))
        }
        assertEquals(RecordBuffer.MAX_STEPS, buf.size())
        assertEquals(5, buf.droppedCount())
    }

    // ---- RecordedStep wire shape (kernel-loadable) ----

    @Test
    fun step_json_matches_kernel_record_step_args() {
        val step = RecordedStep(
            kind = "set_text",
            label = "Type \"hi\"",
            element = sampleElement(),
            params = buildJsonObject { put("text", "hi"); put("submit", false) },
            foregroundPackage = "com.linkedin.android",
        )
        val json = step.toJson()
        assertEquals("set_text", json["kind"]!!.jsonPrimitive.content)
        assertEquals("Type \"hi\"", json["label"]!!.jsonPrimitive.content)
        assertEquals("com.linkedin.android", json["foreground_package"]!!.jsonPrimitive.content)
        assertEquals("hi", json["params"]!!.jsonObject["text"]!!.jsonPrimitive.content)

        // The `element` carries the SPEC §3.1 primitives the kernel Selector reads
        // (Selector::from_observed_value: viewId/text/desc/role/window/bounds).
        val el = json["element"]!!.jsonObject
        assertEquals("com.linkedin.android:id/share_box", el["viewId"]!!.jsonPrimitive.content)
        assertEquals("Post", el["text"]!!.jsonPrimitive.content)
        assertEquals("button", el["role"]!!.jsonPrimitive.content)
        assertEquals(1, el["window"]!!.jsonPrimitive.int)
        val b = el["bounds"]!!.jsonObject
        assertEquals(560, b["cx"]!!.jsonPrimitive.int)
        assertEquals(130, b["cy"]!!.jsonPrimitive.int)
    }

    @Test
    fun buffer_to_json_array_emits_step_objects() {
        val buf = RecordBuffer()
        buf.start()
        buf.offer(RecordedStep("tap", "Tap Post", sampleElement(), buildJsonObject {}, "com.x"))
        val arr = buf.toJsonArray().jsonArray
        assertEquals(1, arr.size)
        assertEquals("tap", arr[0].jsonObject["kind"]!!.jsonPrimitive.content)
    }
}

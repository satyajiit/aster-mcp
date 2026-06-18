package com.aster.service.accessibility

import android.view.accessibility.AccessibilityEvent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Live-recorder (companion "Appium recorder") step model.
 *
 * The companion RECORD MODE captures the *user's own* taps / typing / toggles and
 * lifts each one into a step that is byte-compatible with the kernel's
 * `automation_record_step` tool (cortex-tools-automation): the buffered step's
 * `element` is the SAME SPEC §3.1 [ObservedElement] JSON the `observe` path emits,
 * so `cortex_automations::Selector::from_observed_value(element)` lifts identical
 * selector primitives whether the step was AI-demonstrated or user-recorded.
 *
 * This is complementary to the AI-demonstrate path (where the agent drives the
 * flow and calls `automation_record_step` itself). Both converge on one durable
 * [Selector] shape — the recorder is just the human-in-the-loop capture source.
 *
 * Device-free POJO + JSON producer → unit-testable without a live service.
 */

/**
 * The step `kind` vocabulary the kernel accepts (cortex-automations `StepKind`,
 * validated by `StepKind::from_db`). The live recorder only emits the subset
 * reachable from a single a11y event: [TAP], [SET_TEXT], [SET_TOGGLE].
 *
 * IMPORTANT — there is NO `long_press` kind. A long-click is recorded as [TAP]
 * with `params { "long": true }`: the kernel replay engine (`engine::control_verb`)
 * maps `StepKind::Tap` → the companion `long_press` verb when `params.long` is
 * truthy (or a `duration` is present). Emitting "long_press" as a kind would be
 * rejected by `from_db`. Kept string-typed so a kind never silently drifts from
 * the kernel parser.
 */
object RecordStepKind {
    const val TAP = "tap"
    const val SET_TEXT = "set_text"
    const val SET_TOGGLE = "set_toggle"
}

/**
 * One captured step. [element] is the SPEC §3.1 [ObservedElement] (its `.toJson()`)
 * — the lifted node — so the kernel's `Selector::from_observed_value` reads
 * `viewId`/`text`/`desc`/`role`/`window`/`bounds` straight off it. [params] carries
 * the verb arguments (e.g. `{ "text": "...", "submit": false }` for set_text,
 * `{ "on": true }` for set_toggle). [foregroundPackage] is the app the action
 * happened in (the kernel step's `foreground_package`).
 */
data class RecordedStep(
    val kind: String,
    val label: String,
    val element: ObservedElement,
    val params: JsonObject,
    val foregroundPackage: String?,
) {
    /**
     * The wire shape the kernel's `automation_record_step` tool consumes — keys
     * mirror that tool's `StepArgs` (`kind`/`label`/`element`/`params`/
     * `foreground_package`). The companion buffers an array of these; the RPC
     * layer relays each to the kernel verbatim, where `element` becomes the
     * step's durable [Selector].
     */
    fun toJson(): JsonObject = buildJsonObject {
        put("kind", kind)
        put("label", label)
        put("element", element.toJson())
        put("params", params)
        // The kotlinx put(String, String?) overload writes a real JSON null when the
        // foreground was not resolvable — consistent with the observe / settleAndVerify
        // foreground_after convention (NOT the literal "null" string).
        put("foreground_package", foregroundPackage)
    }
}

/**
 * Pure facts lifted off the source node of an a11y event, decoupled from
 * [android.view.accessibility.AccessibilityNodeInfo] so [StepInferrer] is
 * unit-testable. Mirrors the inputs [RoleMapper] / [ElementFilter] already take.
 */
data class RecordNodeFacts(
    val editable: Boolean,
    val checkable: Boolean,
    val checked: Boolean,
    val longClickable: Boolean,
    val text: String,
    val desc: String,
    val viewId: String,
    val role: String,
)

/**
 * Pure event → step-shape inference (NO Android types). Given an a11y event type
 * and the source node facts, decides the step [kind], the verb [params], and a
 * human [label]. Returns null for events that carry no actionable step (so the
 * buffer never fills with noise).
 *
 * Mapping (matches the kernel verb vocabulary and the `observe` action model):
 *  - TYPE_VIEW_TEXT_CHANGED on an editable    → `set_text`   params { text, submit:false }
 *  - TYPE_VIEW_CLICKED on a checkable/toggle  → `set_toggle` params { on }
 *  - TYPE_VIEW_CLICKED otherwise              → `tap`
 *  - TYPE_VIEW_LONG_CLICKED                   → `tap`        params { long:true }
 *
 * A click on a non-actionable, text-less node (e.g. a layout container that
 * happens to fire CLICKED) is dropped — there is nothing durable to replay.
 */
object StepInferrer {

    /** Inferred verb without the lifted [ObservedElement] (the service attaches that). */
    data class StepShape(val kind: String, val params: JsonObject, val label: String)

    fun infer(eventType: Int, facts: RecordNodeFacts, changedText: String?): StepShape? = when (eventType) {
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
            if (!facts.editable) {
                null
            } else {
                val typed = changedText ?: facts.text
                StepShape(
                    kind = RecordStepKind.SET_TEXT,
                    params = buildJsonObject {
                        put("text", typed)
                        // The recorder never auto-submits — a separate ENTER/IME action
                        // is captured as its own step if the user submits.
                        put("submit", false)
                    },
                    label = "Type \"${ellipsize(typed)}\"${intoSuffix(facts)}",
                )
            }
        }

        AccessibilityEvent.TYPE_VIEW_CLICKED -> when {
            facts.checkable -> StepShape(
                kind = RecordStepKind.SET_TOGGLE,
                // `isChecked` is read AFTER the click landed, so it already reflects
                // the new desired state the user just set.
                params = buildJsonObject { put("on", facts.checked) },
                label = "${if (facts.checked) "Enable" else "Disable"} ${describe(facts)}",
            )

            isActionable(facts) -> StepShape(
                kind = RecordStepKind.TAP,
                params = buildJsonObject {},
                label = "Tap ${describe(facts)}",
            )

            else -> null // non-actionable container click → nothing durable to replay
        }

        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> if (isActionable(facts)) {
            // No `long_press` kind exists — record as `tap` flagged long; the kernel
            // replay engine maps StepKind::Tap + params.long → the long_press verb.
            StepShape(
                kind = RecordStepKind.TAP,
                params = buildJsonObject { put("long", true) },
                label = "Long-press ${describe(facts)}",
            )
        } else {
            null
        }

        else -> null
    }

    /**
     * Whether a clicked node is worth recording: it either advertises an
     * interaction primitive (the role isn't inert "other"/"text") or carries
     * text/desc we can re-resolve by. Pure mirror of the `observe` keep heuristic.
     */
    private fun isActionable(facts: RecordNodeFacts): Boolean {
        if (facts.viewId.isNotEmpty()) return true
        if (facts.text.isNotEmpty() || facts.desc.isNotEmpty()) return true
        return facts.role != "other" && facts.role != "text"
    }

    /** A short human descriptor of the node for the step label. */
    private fun describe(facts: RecordNodeFacts): String = when {
        facts.text.isNotEmpty() -> "\"${ellipsize(facts.text)}\""
        facts.desc.isNotEmpty() -> "\"${ellipsize(facts.desc)}\""
        facts.viewId.isNotEmpty() -> facts.viewId.substringAfterLast('/')
        else -> facts.role
    }

    /** " into <field>" suffix for a set_text label when the field has a name. */
    private fun intoSuffix(facts: RecordNodeFacts): String = when {
        facts.desc.isNotEmpty() -> " into \"${ellipsize(facts.desc)}\""
        facts.viewId.isNotEmpty() -> " into ${facts.viewId.substringAfterLast('/')}"
        else -> ""
    }

    private fun ellipsize(s: String, max: Int = 40): String =
        if (s.length <= max) s else s.take(max - 1) + "…"
}

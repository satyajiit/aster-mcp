package com.aster.service.overlay

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * App Automations /goal R-C — the pure (Android-free) core of the interactive
 * overlay round-trip: parse the kernel's `screen_prompt` / `screen_approve`
 * `device.execute` params into a typed [InteractivePrompt], and build the choice
 * JSON the companion returns under `data`. Kept Android-free so the parsing /
 * clamping / result-shaping is unit-tested on the JVM (mirrors `ObserveBudget`).
 *
 * The kernel tools (`cortex-tools-screen` `prompt.rs` / `approve.rs`) are the
 * sole producers of these params; the result shapes here are the sole contract
 * the kernel's `run_screen_action` passes back verbatim to the EA.
 */
object InteractiveOverlayModel {

    /** Honest fallback when the kernel did not stamp an `ai_name` (I4). */
    const val DEFAULT_AI_NAME = "Aster"

    /** Default dismissal budget when the kernel omits `timeout_ms`. */
    const val DEFAULT_TIMEOUT_MS = 120_000L
    private const val MIN_TIMEOUT_MS = 5_000L

    /**
     * Hard ceiling kept strictly below the kernel's 150s `REMOTE_TIMEOUT`, so
     * the companion always returns a clean result before the kernel host-call
     * gives up (which would orphan the overlay).
     */
    private const val MAX_TIMEOUT_MS = 140_000L

    /** One selectable button on the chooser. `id` is returned as `choice`. */
    data class ChoiceOption(val id: String, val label: String, val hint: String?)

    /** Optional free-text field on the chooser; its value rides back as `text`. */
    data class TextField(val label: String?, val hint: String?, val initial: String?)

    /** One draft card on the approval overlay. */
    data class DraftVariant(val id: String, val text: String)

    /** A parsed, validated interactive prompt ready to render. */
    sealed interface InteractivePrompt {
        val aiName: String
        val timeoutMs: Long

        /** The result to return when the prompt times out (no user action). */
        fun timeoutResult(): JsonObject

        /** The result to return when the prompt is cancelled (back / kill switch). */
        fun cancelResult(): JsonObject

        /** `screen_prompt` — a record-mode chooser with an optional text field. */
        data class Chooser(
            val prompt: String,
            override val aiName: String,
            val options: List<ChoiceOption>,
            val textInput: TextField?,
            val default: String?,
            override val timeoutMs: Long,
        ) : InteractivePrompt {
            override fun timeoutResult() = chooserResult("cancel", null)
            override fun cancelResult() = chooserResult("cancel", null)
        }

        /** `screen_approve` — variant cards the owner approves / edits / rejects. */
        data class Approval(
            val title: String,
            override val aiName: String,
            val variants: List<DraftVariant>,
            val editable: Boolean,
            override val timeoutMs: Long,
        ) : InteractivePrompt {
            override fun timeoutResult() = approvalResult("timeout", null, null)
            override fun cancelResult() = approvalResult("reject", null, null)
        }
    }

    /** Parse a `screen_prompt` action's params. Throws on a malformed request. */
    fun parsePrompt(params: Map<String, JsonElement>): InteractivePrompt.Chooser {
        val prompt = str(params["prompt"])
            ?: throw IllegalArgumentException("screen_prompt: missing `prompt`")
        val options = (params["options"]?.jsonArrayOrNull() ?: emptyList()).map { el ->
            val obj = el.jsonObject
            val id = str(obj["id"])
                ?: throw IllegalArgumentException("screen_prompt: an option is missing `id`")
            ChoiceOption(id = id, label = str(obj["label"]) ?: id, hint = str(obj["hint"]))
        }
        if (options.isEmpty()) {
            throw IllegalArgumentException("screen_prompt: `options` must have at least one choice")
        }
        val textInput = params["text_input"]?.let { el ->
            val obj = el.jsonObject
            TextField(label = str(obj["label"]), hint = str(obj["hint"]), initial = str(obj["initial"]))
        }
        return InteractivePrompt.Chooser(
            prompt = prompt,
            aiName = str(params["ai_name"]) ?: DEFAULT_AI_NAME,
            options = options,
            textInput = textInput,
            default = str(params["default"]),
            timeoutMs = clampTimeoutMs(long(params["timeout_ms"])),
        )
    }

    /** Parse a `screen_approve` action's params. Throws on a malformed request. */
    fun parseApproval(params: Map<String, JsonElement>): InteractivePrompt.Approval {
        val title = str(params["title"])
            ?: throw IllegalArgumentException("screen_approve: missing `title`")
        val variants = (params["variants"]?.jsonArrayOrNull() ?: emptyList()).map { el ->
            val obj = el.jsonObject
            val id = str(obj["id"])
                ?: throw IllegalArgumentException("screen_approve: a variant is missing `id`")
            val text = str(obj["text"])
                ?: throw IllegalArgumentException("screen_approve: a variant is missing `text`")
            DraftVariant(id = id, text = text)
        }
        if (variants.isEmpty()) {
            throw IllegalArgumentException("screen_approve: `variants` must have at least one draft")
        }
        return InteractivePrompt.Approval(
            title = title,
            aiName = str(params["ai_name"]) ?: DEFAULT_AI_NAME,
            variants = variants,
            editable = bool(params["editable"]) ?: true,
            timeoutMs = clampTimeoutMs(long(params["timeout_ms"])),
        )
    }

    /** Clamp the requested timeout into `[5s, 140s]`, defaulting when absent. */
    fun clampTimeoutMs(raw: Long?): Long =
        (raw ?: DEFAULT_TIMEOUT_MS).coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS)

    /** `screen_prompt` result: `{ choice, text? }`. */
    fun chooserResult(choice: String, text: String?): JsonObject = buildJsonObject {
        put("choice", choice)
        if (text != null) put("text", text)
    }

    /** `screen_approve` result: `{ decision, selected_id?, text? }`. */
    fun approvalResult(decision: String, selectedId: String?, text: String?): JsonObject = buildJsonObject {
        put("decision", decision)
        if (selectedId != null) put("selected_id", selectedId)
        if (text != null) put("text", text)
    }

    private fun str(el: JsonElement?): String? = (el as? JsonPrimitive)?.contentOrNull
    private fun long(el: JsonElement?): Long? = (el as? JsonPrimitive)?.longOrNull
    private fun bool(el: JsonElement?): Boolean? = (el as? JsonPrimitive)?.booleanOrNull
    private fun JsonElement.jsonArrayOrNull() = try { jsonArray } catch (_: Exception) { null }
}

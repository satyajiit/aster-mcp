package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.overlay.SignInWaitOverlay
import com.aster.service.overlay.ToolExecutionOverlay
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * App Automations — bridges the kernel's non-blocking wait actions to the
 * on-device [SignInWaitOverlay]:
 *  - `screen_signin_wait` — a login / register wall (`kind` = `login` | `register`).
 *  - `screen_handoff` — a clean step-back hand-off such as payment (`kind` =
 *    `payment` | `explicit_handoff`, with the owner-facing `message`).
 *
 * The automation replay engine fires these (fire-and-forget) when a run pauses
 * for the human: a small non-focusable banner appears over the controlled app and
 * the call returns IMMEDIATELY — the run does NOT block on it. For a sign-in wall
 * the EA polls `automation_run` until it clears (then the run continues in place);
 * a hand-off is terminal (the owner finishes the payment themselves). Reads the
 * kernel-stamped `ai_name` (the assistant's name; overlay falls back to a neutral
 * "your assistant"), `kind`, and optional `message`. This is a companion dialog,
 * not a screen mutation, so it runs no P7 gate.
 *
 * Showing either banner means the run has STOPPED driving and is now waiting for
 * the human, so we first tear down the [ToolExecutionOverlay] ("AI is controlling
 * your screen" border + STOP strip) — otherwise it lingers (its 15s idle timer)
 * and the wait banner stacks on top of it, and the STOP strip wrongly stays up
 * over a hand-off the owner is meant to finish themselves.
 */
class SignInWaitHandler(
    private val overlay: SignInWaitOverlay,
    private val toolExecutionOverlay: ToolExecutionOverlay,
) : CommandHandler {

    override fun supportedActions(): List<String> =
        listOf("screen_signin_wait", "screen_handoff")

    override suspend fun handle(command: Command): CommandResult {
        val params = command.params ?: emptyMap()
        val aiName = (params["ai_name"] as? JsonPrimitive)?.contentOrNull
        val kind = (params["kind"] as? JsonPrimitive)?.contentOrNull
        val message = (params["message"] as? JsonPrimitive)?.contentOrNull
        // The AI has handed control back — clear the "controlling your screen"
        // overlay so it doesn't sit under/over this wait banner.
        toolExecutionOverlay.clearActive()
        overlay.show(aiName, kind, message)
        return CommandResult.success(buildJsonObject { put("ok", true) })
    }
}

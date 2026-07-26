package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.safety.PackagePolicyGuard
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * `screen_set_policy` — the kernel pushes the owner's per-app screen-control
 * policy into [PackagePolicyGuard].
 *
 * Until this existed, `PackagePolicyGuard.updatePolicy` had **zero callers**:
 * the companion enforced only its bundled banking/payments fragment list, so an
 * owner allow-override (recorded kernel-side, in `screen:set-app-policy`) never
 * reached the process that actually refuses the tap. An owner who allowed their
 * bank saw the kernel agree and the companion refuse.
 *
 * The kernel sends its COMPLETE `{allow, deny}` set on every push, so this
 * replaces rather than merges — see [PackagePolicyGuard.updatePolicy].
 *
 * Ungated by design: it dispatches no gesture, reads no foreground app, and is
 * the very thing that teaches the guard what to allow. (`GATED_ACTIONS` omits
 * it, so the guard decorator waves it through.)
 */
class PolicyHandler(
    private val guard: PackagePolicyGuard,
) : CommandHandler {

    override fun supportedActions() = listOf("screen_set_policy")

    override suspend fun handle(command: Command): CommandResult {
        val allow = packageSet(command, "allow")
        val deny = packageSet(command, "deny")
        guard.updatePolicy(allow, deny)
        return CommandResult.success(
            buildJsonObject {
                put("ok", true)
                put("allow", allow.size)
                put("deny", deny.size)
            }
        )
    }

    /**
     * Read one string array param, dropping blanks. A missing key yields an
     * EMPTY set rather than "leave as-is": the push is a whole-policy replace,
     * and an owner who cleared every allow-override sends `{allow: []}` — or,
     * depending on the encoder, omits the key entirely. Treating absence as
     * "keep the old set" would make that clear un-revokable.
     */
    private fun packageSet(command: Command, key: String): Set<String> {
        val arr = command.params?.get(key) as? JsonArray ?: return emptySet()
        return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
            .toSet()
    }
}

package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.AsterAccessibilityService
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Screen-control preflight probe (read-only).
 *
 * The kernel fires `screen_capability` BEFORE an automation run to learn whether
 * the companion can actually drive the screen. Without it, an `observe` against a
 * disabled/unbound accessibility service returns 0 elements, which is
 * indistinguishable from "selector not found" — so the kernel loops forever. This
 * verb is the honest preflight signal: it reports the three booleans the kernel
 * needs and NEVER fails.
 *
 * CRITICAL: this handler must answer truthfully even when the service is not
 * bound. Unlike [AccessibilityHandler], it does NOT early-return
 * `CommandResult.failure(...)` when [AsterAccessibilityService.getInstance] is
 * null — a null instance is a valid answer (`{false, false, false}`), not an
 * error. It is also a pure status read (no foreground app, no gesture), so it is
 * NOT gated by [com.aster.service.safety.PackagePolicyGuard] beyond being on its
 * read-only allowlist, and it is intentionally absent from
 * [com.aster.service.mode.IpcMode]'s SCREEN_CONTROL_ACTIONS kill-switch set.
 */
class CapabilityHandler : CommandHandler {

    override fun supportedActions(): List<String> = listOf("screen_capability")

    override suspend fun handle(command: Command): CommandResult {
        // No early-return on null — a null instance IS the answer (service unbound).
        val svc = AsterAccessibilityService.getInstance()

        // service_bound: is a service instance currently bound (non-null)?
        val serviceBound = svc != null
        // accessibility_enabled: is the service enabled/connected? The static mirrors
        // `instance != null`, so it agrees with serviceBound, but use it as the
        // canonical signal in case the two ever diverge.
        val accessibilityEnabled = AsterAccessibilityService.isServiceEnabled()
        // can_observe: can it read a window right now? Cheap best-effort —
        // foregroundPackage() reads rootInActiveWindow (and recycles it); a non-null
        // package means a window is readable. No heavy observe walk.
        val canObserve = svc?.foregroundPackage() != null

        return CommandResult.success(buildJsonObject {
            put("accessibility_enabled", accessibilityEnabled)
            put("service_bound", serviceBound)
            put("can_observe", canObserve)
        })
    }
}

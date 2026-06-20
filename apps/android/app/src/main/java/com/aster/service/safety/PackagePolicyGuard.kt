package com.aster.service.safety

import android.util.Log
import com.aster.service.AsterAccessibilityService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Companion-side denylist enforcement for screen-control actions (Screen
 * Control /goal P7). Defense in depth: the kernel also gates, but the
 * companion independently refuses to drive a denylisted foreground package.
 *
 * Fail-closed: if the live foreground package cannot be read, a control
 * action is REFUSED (only screen READS — `observe` / screenshot / hierarchy —
 * are allowed without a readable package).
 *
 * The denylist is the SAME bundled banking/payments fragment set the kernel
 * ships (`cortex-tools-screen::denylist`). Owner allow/deny overrides are the
 * kernel's source of truth; the companion keeps only the fail-safe default
 * set so it can refuse even if it has never synced (SPEC §3.6 "enforced both
 * kernel-side and companion-side").
 */
@Singleton
class PackagePolicyGuard @Inject constructor() {

    companion object {
        private const val TAG = "PackagePolicyGuard"

        /** Actions that only READ the screen — allowed without a package. */
        private val READ_ONLY_ACTIONS = setOf(
            "observe", "get_screen_hierarchy", "take_screenshot",
            "find_element", "wait_for_idle", "wait_for",
            // Screen-control preflight probe (CapabilityHandler): a pure status
            // read of whether the service can drive the screen. Dispatches no
            // gesture and must answer even when no app is readable, so it is
            // always allowed by the guard.
            "screen_capability",
            // Companion live recorder: these arm/disarm/read the recorder buffer and
            // dispatch NO gesture — they only passively capture the user's own actions.
            // Gating them behind the denylist would drop a recording stopped while a
            // banking app is foreground; the recorder is non-acting, so allow it.
            "automation_record_start", "automation_record_stop", "automation_record_status"
        )

        /** Bundled default-deny banking/payments package fragments. */
        private val DENY_FRAGMENTS = listOf(
            "paisa", "phonepe", "paytm", "bank", "upi", "wallet", "payment",
            "paypal", "venmo", "cashapp", "wise.android", "revolut",
            "coinbase", "binance"
        )
    }

    /** Owner allow-overrides pushed from the kernel (package names). */
    @Volatile
    private var allowOverrides: Set<String> = emptySet()

    /** Owner extra-deny pushed from the kernel (package names). */
    @Volatile
    private var denyOverrides: Set<String> = emptySet()

    /** Kernel pushes the synced policy here (via a future companion action). */
    fun updatePolicy(allow: Set<String>, deny: Set<String>) {
        allowOverrides = allow
        denyOverrides = deny
    }

    /**
     * @return null if the action may proceed, or a human refusal message if it
     * is denied (fail-closed). Reads the live foreground package via the
     * accessibility service (`foregroundPackage()` reads `rootInActiveWindow`
     * and recycles it).
     */
    fun checkAllowed(action: String): String? {
        if (action in READ_ONLY_ACTIONS) return null

        val service = AsterAccessibilityService.getInstance()
            ?: return "Accessibility service not available — refusing screen action (fail-closed)."

        val pkg = service.foregroundPackage()
        if (pkg.isNullOrBlank()) {
            Log.w(TAG, "Foreground package unreadable — refusing '$action' (fail-closed)")
            return "Could not read the current app — refusing to act for safety. Try again after the screen settles."
        }

        if (pkg in denyOverrides) {
            return "App '$pkg' is denied for screen control by your settings."
        }
        if (pkg in allowOverrides) return null
        if (DENY_FRAGMENTS.any { pkg.contains(it, ignoreCase = true) }) {
            return "App '$pkg' is a banking/payments app and is denied for screen control by default. You can allow it in OpenAlly settings."
        }
        return null
    }
}

package com.aster.service.safety

import android.util.Log
import com.aster.data.model.Command
import com.aster.service.AsterAccessibilityService
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Companion-side denylist enforcement for screen-control actions (Screen
 * Control /goal P7). Defense in depth: the kernel also gates, but the
 * companion independently refuses to drive a denylisted foreground package.
 *
 * Fail-closed: if the live foreground package cannot be read, a gated action is
 * REFUSED. Only actions in [GATED_ACTIONS] are gated at all — everything else
 * (screen READS, device info, notifications, the companion's own overlays) is
 * waved through, because this guard is about *driving another app*, not about
 * whether the companion may run.
 *
 * The denylist is the SAME bundled banking/payments fragment set the kernel
 * ships (`cortex-tools-screen::denylist`). Owner allow/deny overrides are the
 * kernel's source of truth, pushed here by the `screen_set_policy` verb
 * ([PolicyHandler]); the companion keeps the fail-safe default set so it can
 * refuse even if it has never synced (SPEC §3.6 "enforced both kernel-side and
 * companion-side").
 */
@Singleton
class PackagePolicyGuard @Inject constructor() {

    companion object {
        private const val TAG = "PackagePolicyGuard"

        /**
         * Actions that DRIVE another app, and are therefore the only ones this
         * guard refuses. Everything absent from this set is ungated.
         *
         * Deliberately excluded, and each exclusion is load-bearing:
         *  - Screen READS (`observe`, `take_screenshot`, `wait_for`, …). The
         *    kernel gates what it does with what it reads; refusing the read
         *    would also blind the obstruction classifier.
         *  - `screen_capability` — the preflight probe. It must answer even when
         *    no app is readable, or the agent cannot tell "companion off" from
         *    "banking app in front".
         *  - `screen_prompt` / `screen_approve` / `screen_signin_wait` /
         *    `screen_handoff` — the companion's OWN overlay dialogs and banners.
         *    They touch no foreground app, and gating them would suppress the
         *    hand-off banner at exactly the moment it matters most: a payment
         *    screen, which is a denylisted package by construction.
         *
         * Kept in sync with the kernel's control tier
         * (`cortex-tools-screen::register_default_screen`).
         */
        val GATED_ACTIONS = setOf(
            "tap", "set_text", "long_press", "set_toggle", "perform", "scroll",
            "input_gesture", "press_key", "global_action", "input_text",
            "click_by_text", "click_by_view_id",
            // Opening an app IS control: gated on the package it is about to
            // launch (see [targetPackageOf]), not on whatever is in front now.
            "launch_intent",
        )

        /** Bundled default-deny banking/payments package fragments. */
        private val DENY_FRAGMENTS = listOf(
            "paisa", "phonepe", "paytm", "bank", "upi", "wallet", "payment",
            "paypal", "venmo", "cashapp", "wise.android", "revolut",
            "coinbase", "binance"
        )

        /**
         * The package a command is about to act on, when the command names one
         * itself rather than implying the foreground.
         *
         * Only `launch_intent` does: it is asked to bring a package to the
         * front, so checking the CURRENT foreground would be checking the wrong
         * app — it would happily open a banking app from the home screen, and
         * refuse to leave one. `null` means "use the live foreground package".
         */
        fun targetPackageOf(command: Command): String? {
            if (command.action != "launch_intent") return null
            return (command.params?.get("package") as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        }
    }

    /** Owner allow-overrides pushed from the kernel (package names). */
    @Volatile
    private var allowOverrides: Set<String> = emptySet()

    /** Owner extra-deny pushed from the kernel (package names). */
    @Volatile
    private var denyOverrides: Set<String> = emptySet()

    /** Whether the kernel has ever pushed a policy into this process. */
    @Volatile
    private var synced: Boolean = false

    /**
     * Kernel pushes the synced policy here, via the `screen_set_policy` verb.
     *
     * Replaces wholesale rather than merging: the kernel sends its complete
     * `{allow, deny}` set every time, so a package the owner CLEARED must
     * disappear here too. Merging would make a clear un-revokable until the
     * companion restarted.
     */
    fun updatePolicy(allow: Set<String>, deny: Set<String>) {
        allowOverrides = allow
        denyOverrides = deny
        synced = true
        log { Log.d(TAG, "Policy synced: ${allow.size} allow, ${deny.size} deny") }
    }

    /**
     * Emit a log line, tolerating the absence of the Android framework.
     *
     * `android.util.Log` is an unmocked stub on the JVM unit-test classpath and
     * every call throws. This guard is pure decision logic and must be testable
     * there — the alternative (`returnDefaultValues = true`) would silently turn
     * every other stubbed Android call in the module into a zero, which is how
     * a geometry test starts passing against an all-zero Rect.
     */
    private inline fun log(emit: () -> Unit) {
        try {
            emit()
        } catch (_: RuntimeException) {
            // Unit-test JVM: no Android framework. Nothing to report to.
        }
    }

    /** Whether the kernel has pushed a policy since this process started. */
    fun isSynced(): Boolean = synced

    /** Current policy, for the `screen_set_policy` ack and for tests. */
    fun snapshot(): Pair<Set<String>, Set<String>> = allowOverrides to denyOverrides

    /**
     * @param action the companion verb about to run.
     * @param targetPackage the package the action names explicitly, if any
     *   (see [targetPackageOf]); `null` → check the live foreground package.
     * @return null if the action may proceed, or a human refusal message if it
     * is denied (fail-closed). Reads the live foreground package via the
     * accessibility service (`foregroundPackage()` reads `rootInActiveWindow`
     * and recycles it).
     */
    fun checkAllowed(action: String, targetPackage: String? = null): String? {
        if (action !in GATED_ACTIONS) return null

        val pkg = if (targetPackage != null) {
            targetPackage
        } else {
            val service = AsterAccessibilityService.getInstance()
                ?: return "Accessibility service not available — refusing screen action (fail-closed)."
            service.foregroundPackage()
        }

        if (pkg.isNullOrBlank()) {
            log { Log.w(TAG, "Foreground package unreadable — refusing '$action' (fail-closed)") }
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

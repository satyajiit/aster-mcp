package com.aster.service.safety

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The foreground as the device shows it at the moment an action is about to run.
 *
 * [activeWindow] is the root package of the active window: the window every
 * `ref` is resolved in (`AsterAccessibilityService.resolveRef` searches
 * `rootInActiveWindow` and nothing else), so it is the app a ref action lands
 * in. [applicationWindows] is every application window `getWindows()` lists,
 * with whether it holds focus; it is empty when that list is unavailable, and
 * then the active root is all the device shows (observe makes the same
 * fallback, so the kernel's snapshot was built from it too).
 */
data class LiveForeground(
    val activeWindow: String?,
    val applicationWindows: List<AppWindow>,
) {
    data class AppWindow(val packageName: String?, val focused: Boolean)
}

/**
 * Refuses an action whose `expected_package` is not the app in front (agent
 * autonomy §2.3, kernel `cortex-tools-screen` `gate.rs`).
 *
 * The kernel lets a send or confirm tap skip its approval card only on what
 * the device's last observe showed, and stamps that observe's foreground app
 * on the action as `expected_package`. Between that observe and this tap the
 * screen can move in ways the kernel never sees: an approved card, an
 * automation, the person picking up the phone. This is the last look, taken
 * here because only the device knows what is in front now.
 *
 * Fail-closed: the action runs only when the active window AND the focused
 * application window both belong to the expected app. Anything else is a
 * refusal: another app in front, the notification shade or another app's
 * dialog holding the active window, a split screen whose other half has
 * focus, no focused application window, an unreadable root, no accessibility
 * service, or an `expected_package` that is not a non-blank string. An action
 * WITHOUT the field is untouched: the kernel sends it only on a bypassed
 * action, and every other caller keeps today's behaviour.
 *
 * Pure decision logic over [LiveForeground], so the whole table is testable on
 * the JVM; [com.aster.service.AsterAccessibilityService.liveForeground] reads
 * the live side.
 */
object ExpectedPackageGuard {

    /** The refusal's `code`, for the kernel and the model to match on. */
    const val CODE = "foreground_mismatch"

    /**
     * The typed refusal for an action carrying [params], or null when it may
     * run. [live] is read only when the field is present, so an action without
     * it costs no window read.
     */
    fun refusal(params: Map<String, JsonElement>?, live: () -> LiveForeground?): String? {
        // Every transport widens keys to both spellings (WireParams.normalize),
        // but read both here too: this guard must not depend on that shim to
        // notice the field at all.
        val raw = params?.get("expected_package") ?: params?.get("expectedPackage") ?: return null
        val expected = (raw as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim().orEmpty()
        if (expected.isEmpty()) return encode(expected = null, front = null)
        val front = frontOf(live())
        return if (front == expected) null else encode(expected, front)
    }

    /**
     * The one app the device shows in front, or null when it cannot say.
     *
     * The active window and the focused application window must agree. Either
     * alone is wrong somewhere: the active window is the notification shade
     * while it is pulled down (with the app still focused underneath), and in
     * split screen the focused half is the app the observe reported but the
     * active window is whichever half was last touched.
     */
    internal fun frontOf(live: LiveForeground?): String? {
        if (live == null) return null
        val active = live.activeWindow?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (live.applicationWindows.isEmpty()) return active
        val focused = live.applicationWindows.singleOrNull { it.focused }
            ?.packageName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        return active.takeIf { it == focused }
    }

    private fun encode(expected: String?, front: String?): String {
        val payload = buildJsonObject {
            put("code", CODE)
            put("expected_package", expected)
            put("foreground_package", front)
            put("hint", "re-observe")
            put(
                "message",
                if (front == null) {
                    "Could not confirm which app is in front, so the action did not run. Look at the screen again."
                } else {
                    "Another app is in front ($front), so the action did not run. Look at the screen again."
                },
            )
        }
        // Same shape as ScreenActionResult.staleRef: a JSON object in `error`, so
        // every transport surfaces it as a failure without special-casing.
        return Json.encodeToString(JsonElement.serializer(), payload)
    }
}

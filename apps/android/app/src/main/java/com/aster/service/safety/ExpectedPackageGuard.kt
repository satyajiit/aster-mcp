package com.aster.service.safety

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The screen as the device shows it at the moment an action is about to run.
 *
 * [activeWindow] is the root package of the active window: the window every
 * `ref` is resolved in (`AsterAccessibilityService.resolveRef` searches
 * `rootInActiveWindow` and nothing else), so it is the app a ref action lands
 * in. [windows] is every window `getWindows()` lists, empty when that list is
 * unavailable. [ownPackage] is this companion's, whose own overlays never
 * count against an action.
 */
data class LiveForeground(
    val activeWindow: String?,
    val windows: List<Window>,
    val ownPackage: String,
) {
    data class Window(val kind: Kind, val packageName: String?, val focused: Boolean)

    /** Only the distinctions the guard makes; every other window type is [OTHER]. */
    enum class Kind { APPLICATION, INPUT_METHOD, OTHER }
}

/**
 * Guards an action whose `expected_package` says it skipped its approval card
 * (agent autonomy §2.3, kernel `cortex-tools-screen` `gate.rs`).
 *
 * The kernel lets a send or confirm tap skip its card only on what the device's
 * last observe showed, and stamps that observe's foreground app on the action
 * as `expected_package`. Between that observe and this tap the screen can move
 * in ways the kernel never sees: an approved card, an automation, the person
 * picking up the phone. This is the last look, taken here because only the
 * device knows what is in front now. Three refusals, each a typed `code`:
 *
 * - [MISMATCH] (`foreground_mismatch`): the active window and the focused
 *   application window are not both the expected app, or either cannot be read,
 *   or the window list is unavailable, or `expected_package` is not a non-blank
 *   string. Also raised at the node, when the element about to be clicked turns
 *   out to belong to another app.
 * - [OVERLAY] (`overlay_present`): a window of any other app is on screen: another
 *   app's window (split screen, picture in picture) or an overlay drawn over
 *   apps, which Android lists as a system window under the drawing app's
 *   package. The keyboard, the system bars ([SYSTEM_UI]) and this companion's
 *   own windows do not count. A window whose package cannot be read counts.
 * - [GESTURE] (`gesture_refused_guarded`): the action could only run as a
 *   screen-position gesture, which any window on top could take. Only a click
 *   or long click performed on the resolved node itself, or on its nearest
 *   clickable ancestor, may run ([target]); coordinates, the centre-tap
 *   fallback and every gesture-based verb are refused.
 *
 * An action WITHOUT the field is untouched: the kernel sends it only on a
 * bypassed action, and every other caller keeps today's behaviour.
 *
 * Pure decision logic, so the whole table is testable on the JVM;
 * [com.aster.service.AsterAccessibilityService.liveForeground] and
 * `actOnRefGuarded` supply the live side.
 */
object ExpectedPackageGuard {

    const val MISMATCH = "foreground_mismatch"
    const val OVERLAY = "overlay_present"
    const val GESTURE = "gesture_refused_guarded"

    /** The system bars' package: on screen all the time, and drawing no app. */
    private val SYSTEM_UI = setOf("com.android.systemui")

    /**
     * The verbs that act on a resolved node and so can run guarded. Every other
     * control verb ([PackagePolicyGuard.GATED_ACTIONS]) is a gesture, a key or a
     * launch, and is refused when it carries the field.
     */
    private val NODE_ACTIONS = setOf("tap", "long_press", "set_toggle", "perform")

    /** How far up the tree a guarded click looks for a clickable ancestor. */
    const val MAX_ANCESTORS = 4

    /**
     * The package an action must land in, or null when it carries no
     * `expected_package`. A value that is not a string reads as "", which no
     * app's package equals, so a malformed field can only refuse.
     */
    fun expectedOf(params: Map<String, JsonElement>?): String? {
        // Every transport widens keys to both spellings (WireParams.normalize),
        // but read both here too: the guard must not depend on that shim to
        // notice the field at all.
        val raw = params?.get("expected_package") ?: params?.get("expectedPackage") ?: return null
        return (raw as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim().orEmpty()
    }

    /**
     * The typed refusal for [action] carrying [params], or null when it may go
     * on to its handler. [live] is read only when the field is present, so an
     * action without it costs no window read.
     */
    fun refusal(action: String, params: Map<String, JsonElement>?, live: () -> LiveForeground?): String? {
        val expected = expectedOf(params) ?: return null
        if (expected.isEmpty()) return mismatch(expected = null, front = null)
        if (action in PackagePolicyGuard.GATED_ACTIONS &&
            (action !in NODE_ACTIONS || params?.get("ref") == null)
        ) {
            return gesture(expected)
        }
        val screen = live()
        val front = frontOf(screen)
        if (screen == null || front != expected) return mismatch(expected, front)
        overlayOf(screen, expected)?.let { other -> return overlay(expected, other) }
        return null
    }

    /**
     * The one app the device shows in front, or null when it cannot say.
     *
     * The active window and the focused application window must agree. Either
     * alone is wrong somewhere: the active window is the notification shade
     * while it is pulled down (with the app still focused underneath), and in
     * split screen the focused half is the app the observe reported but the
     * active window is whichever half was last touched. No window list means
     * no focused window to agree with.
     */
    internal fun frontOf(live: LiveForeground?): String? {
        if (live == null) return null
        val active = live.activeWindow?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val focused = live.windows
            .filter { it.kind == LiveForeground.Kind.APPLICATION }
            .singleOrNull { it.focused }
            ?.packageName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        return active.takeIf { it == focused }
    }

    /**
     * A window on screen that belongs to neither [expected] nor the system bars,
     * the keyboard or this companion: its package, or "" when it cannot be read.
     * Null when there is none.
     */
    internal fun overlayOf(live: LiveForeground, expected: String): String? {
        for (window in live.windows) {
            if (window.kind == LiveForeground.Kind.INPUT_METHOD) continue
            val pkg = window.packageName?.trim().orEmpty()
            if (pkg == expected || pkg == live.ownPackage || pkg in SYSTEM_UI) continue
            return pkg
        }
        return null
    }

    /** One node on the way from the resolved element up to the window root. */
    data class TargetNode(val packageName: String?, val clickable: Boolean, val longClickable: Boolean)

    /** Where a guarded click goes: the [index] into the chain, or a refusal. */
    sealed interface Target {
        data class Act(val index: Int) : Target
        data class Refuse(val error: String) : Target
    }

    /**
     * Which node of [chain] (the resolved element first, then its ancestors) a
     * guarded click or long click may be performed on: the nearest one that
     * takes it, provided it belongs to [expected]. None takes it, or the chain
     * is empty (the element was not found as a live node): [GESTURE], since a
     * gesture is the only way left. The node found belongs to another app:
     * [MISMATCH], the screen moved after the check in front of the handler.
     */
    fun target(chain: List<TargetNode>, expected: String, longPress: Boolean): Target {
        val index = chain.indexOfFirst { if (longPress) it.longClickable else it.clickable }
        if (index < 0) return Target.Refuse(gesture(expected))
        nodeRefusal(chain[index].packageName, expected)?.let { return Target.Refuse(it) }
        return Target.Act(index)
    }

    /**
     * The click-time check on the node an action is about to be performed on:
     * [MISMATCH] when its package is not [expected] or cannot be read, the
     * screen having moved after the check in front of the handler. Null when it
     * belongs to the expected app.
     */
    fun nodeRefusal(nodePackage: String?, expected: String): String? {
        val pkg = nodePackage?.trim()?.takeIf { it.isNotEmpty() }
        return if (pkg != null && pkg == expected) null else mismatch(expected, pkg)
    }

    /** [GESTURE]'s refusal, for the handler paths that find only a gesture left. */
    fun gesture(expected: String): String = encode(
        GESTURE, expected, null, null,
        "This would need a tap on a screen position, which is not allowed without asking, " +
            "so the action did not run. Ask the owner to do it.",
    )

    private fun mismatch(expected: String?, front: String?): String = encode(
        MISMATCH, expected, "foreground_package", front,
        if (front == null) {
            "Could not confirm which app is in front, so the action did not run. Look at the screen again."
        } else {
            "Another app is in front ($front), so the action did not run. Look at the screen again."
        },
    )

    private fun overlay(expected: String, other: String): String = encode(
        OVERLAY, expected, "overlay_package", other.ifEmpty { null },
        "Another app's window is on the screen" +
            (if (other.isEmpty()) "" else " ($other)") +
            ", so the action did not run. Close it, then look at the screen again.",
    )

    private fun encode(code: String, expected: String?, otherKey: String?, other: String?, message: String): String {
        val payload = buildJsonObject {
            put("code", code)
            put("expected_package", expected)
            if (otherKey != null) put(otherKey, other)
            put("hint", "re-observe")
            put("message", message)
        }
        // Same shape as ScreenActionResult.staleRef: a JSON object in `error`, so
        // every transport surfaces it as a failure without special-casing.
        return Json.encodeToString(JsonElement.serializer(), payload)
    }
}

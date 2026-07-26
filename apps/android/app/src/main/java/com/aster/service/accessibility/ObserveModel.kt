package com.aster.service.accessibility

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** SPEC §3.1 bounds: { x, y, w, h, cx, cy } — real screen pixels. */
data class Bounds(
    val x: Int, val y: Int, val w: Int, val h: Int, val cx: Int, val cy: Int,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("x", x); put("y", y); put("w", w); put("h", h); put("cx", cx); put("cy", cy)
    }

    companion object {
        fun fromLTRB(left: Int, top: Int, right: Int, bottom: Int): Bounds {
            val w = right - left
            val h = bottom - top
            return Bounds(x = left, y = top, w = w, h = h, cx = left + w / 2, cy = top + h / 2)
        }
    }
}

/** SPEC §3.1 element state flags. */
data class ElementState(
    val clickable: Boolean,
    val editable: Boolean,
    val checkable: Boolean,
    val checked: Boolean,
    val scrollable: Boolean,
    val selected: Boolean,
    val focused: Boolean,
    val enabled: Boolean,
    val password: Boolean,
) {
    /**
     * Emit only the flags that are actually SET, plus `enabled` and `password`.
     *
     * Nine booleans per element, eight of them false on a typical node, is most of
     * an observe payload — and every one of those `false`s is a token the model
     * pays for and reads past. Absence already means false; there is nothing to
     * learn from being told so.
     *
     * Two exceptions, both deliberate:
     * * `enabled` is emitted whenever it is FALSE, because a disabled control is
     *   an unusual and decision-relevant state that silence would hide.
     * * `password` is emitted whenever it is TRUE — it is the primary login-wall
     *   signal (`ObservedScreen::has_password_field`), and a detector that
     *   depends on a key existing must not have that key optimised away.
     */
    fun toJson(): JsonObject = buildJsonObject {
        if (clickable) put("clickable", true)
        if (editable) put("editable", true)
        if (checkable) put("checkable", true)
        if (checked) put("checked", true)
        if (scrollable) put("scrollable", true)
        if (selected) put("selected", true)
        if (focused) put("focused", true)
        if (!enabled) put("enabled", false)
        if (password) put("password", true)
    }
}

/** SPEC §3.1 screen.windows[] entry. */
data class WindowInfo(
    val id: Int,
    val type: String,
    val title: String,
    val focused: Boolean,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("id", id)
        put("type", type)
        put("title", title)
        put("focused", focused)
    }
}

/** SPEC §3.1 screen context. */
data class ScreenContext(
    val width: Int,
    val height: Int,
    val density: Float,
    val rotation: Int,
    val foregroundPackage: String,
    val activity: String,
    val imeVisible: Boolean,
    val windows: List<WindowInfo>,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("width", width)
        put("height", height)
        put("density", density)
        put("rotation", rotation)
        put("foreground_package", foregroundPackage)
        put("activity", activity)
        put("ime_visible", imeVisible)
        put("windows", JsonArray(windows.map { it.toJson() }))
    }
}

/** SPEC §3.1 flat element. */
/**
 * Actions the `state` flags already tell the reader about. Emitting them is pure
 * duplication — see [ObservedElement.toJson].
 */
private val ACTIONS_IMPLIED_BY_STATE = setOf(
    "click",
    "long_click",
    "scroll_forward",
    "scroll_backward",
    "set_text",
)

data class ObservedElement(
    val ref: String,
    val role: String,
    val text: String,
    val desc: String,
    /**
     * Text gathered from this node's own DESCENDANTS, when the node itself
     * carries none.
     *
     * The dominant Android list-row pattern is a clickable `ViewGroup` whose
     * label lives in child `TextView`s. In `actionable` mode the filter keeps
     * only the clickable ancestor and drops those children — so the model saw a
     * screen of anonymous boxes and had nothing to name a row by. Every "tap the
     * first search result" failure starts here.
     *
     * Empty when the node has its own [text] or [desc] (nothing to aggregate) or
     * when no descendant text was found.
     */
    val label: String = "",
    val viewId: String,
    /**
     * Jetpack Compose `Modifier.testTag`, when the node carries one.
     *
     * Compose sets no `viewIdResourceName`, so on a Compose screen the strongest
     * primitive in the match ladder is simply missing and every step falls
     * through to visible text — the one primitive that changes with translation,
     * copy edits and A/B tests. This is the closest thing Compose offers to a
     * stable id.
     */
    val testTag: String = "",
    val window: Int,
    val bounds: Bounds,
    val state: ElementState,
    val actions: List<String>,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("ref", ref)
        put("role", role)
        put("text", text)
        put("desc", desc)
        // Only when non-empty: an always-present empty string would add a key to
        // every element in every observe for the majority that don't need it.
        if (label.isNotEmpty()) put("label", label)
        put("viewId", viewId)
        if (testTag.isNotEmpty()) put("testTag", testTag)
        put("window", window)
        put("bounds", bounds.toJson())
        put("state", state.toJson())
        // Only the actions that are NOT already implied by `state`. `click`,
        // `long_click`, `scroll_forward`/`scroll_backward` and `set_text` restate
        // `clickable`/`longClickable`/`scrollable`/`editable` on essentially every
        // element that has them — the same fact, twice, per element, all the way
        // down the tree. What is left is the genuinely informative remainder
        // (`expand`, `dismiss`, `focus`, `set_progress`, custom actions).
        //
        // `focus`/`clear_focus` are NOT in that implied set even though `focused`
        // exists: that flag reports the CURRENT state, not whether focusing is
        // available, so dropping them would remove a performable action rather
        // than a restatement.
        val informative = actions.filterNot { it in ACTIONS_IMPLIED_BY_STATE }
        if (informative.isNotEmpty()) {
            put("actions", buildJsonArray { informative.forEach { add(it) } })
        }
    }
}

/** SPEC §3.1 scrollables[] entry. */
data class Scrollable(
    val ref: String,
    val bounds: Bounds,
    val directions: List<String>,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("ref", ref)
        put("bounds", bounds.toJson())
        put("directions", buildJsonArray { directions.forEach { add(it) } })
    }
}

/** SPEC §3.1 top-level observe payload. */
data class ObserveResult(
    val screen: ScreenContext,
    val elements: List<ObservedElement>,
    val scrollables: List<Scrollable>,
    val source: String,          // "a11y" | "ocr" | "merged" (P1 always "a11y")
    val snapshotId: String,
    val truncated: Boolean,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("screen", screen.toJson())
        put("elements", JsonArray(elements.map { it.toJson() }))
        put("scrollables", JsonArray(scrollables.map { it.toJson() }))
        put("source", source)
        put("snapshot_id", snapshotId)
        put("truncated", truncated)
    }
}

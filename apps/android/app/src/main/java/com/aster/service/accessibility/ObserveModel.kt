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
    fun toJson(): JsonObject = buildJsonObject {
        put("clickable", clickable)
        put("editable", editable)
        put("checkable", checkable)
        put("checked", checked)
        put("scrollable", scrollable)
        put("selected", selected)
        put("focused", focused)
        put("enabled", enabled)
        put("password", password)
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
data class ObservedElement(
    val ref: String,
    val role: String,
    val text: String,
    val desc: String,
    val viewId: String,
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
        put("viewId", viewId)
        put("window", window)
        put("bounds", bounds.toJson())
        put("state", state.toJson())
        put("actions", buildJsonArray { actions.forEach { add(it) } })
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

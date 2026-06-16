package com.aster.service.accessibility

/**
 * Serialization-free screen-pixel bounds carried inside a [NodeDescriptor].
 * Plain ints so the descriptor is unit-testable without a real Rect / node.
 * Mirrors the SPEC §3.1 `bounds` shape: { x, y, w, h, cx, cy }.
 */
data class DescriptorBounds(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val cx: Int,
    val cy: Int,
) {
    companion object {
        /** Build from raw screen-rect edges (left, top, right, bottom). */
        fun fromLTRB(left: Int, top: Int, right: Int, bottom: Int): DescriptorBounds {
            val w = right - left
            val h = bottom - top
            return DescriptorBounds(
                x = left,
                y = top,
                w = w,
                h = h,
                cx = left + w / 2,
                cy = top + h / 2,
            )
        }
    }
}

/**
 * Re-resolution descriptor cached per `ref` inside a snapshot.
 *
 * INVARIANT: this NEVER holds a live AccessibilityNodeInfo. Live nodes are
 * recycled/pooled and go stale (SPEC §3.1). P2 re-resolves a ref → a fresh
 * live node from these primitives, in priority order:
 *   (1) findAccessibilityNodeInfosByViewId(viewId)
 *   (2) text + role match within [windowId]
 *   (3) nearest node to [bounds] center
 *   (4) raw center-tap of [bounds]
 *
 * [childPath] is the sequence of getChild() indices from the window root to
 * this node, recorded during the SAME walk that emitted the element, so it is
 * consistent with the tree state at snapshot time.
 */
data class NodeDescriptor(
    val ref: String,
    val viewId: String,
    val text: String,
    val role: String,
    val className: String,
    val bounds: DescriptorBounds,
    val windowId: Int,
    val childPath: List<Int>,
)

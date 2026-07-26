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
    /**
     * Content description.
     *
     * Captured in the same walk as [text] and, for a long time, discarded here —
     * which quietly gutted the verify-before-act gate for the elements that need
     * it most. An icon-only button has no [viewId] and no [text], so the gate
     * reduced to `"" == ""` plus a role comparison: on a toolbar of icon buttons
     * every one of them "matched" the descriptor, and the nearest-bounds strategy
     * then picked among them by proximity alone. The description is usually the
     * ONLY primitive such a node exposes.
     */
    val desc: String,
    /**
     * Aggregated descendant text ([LabelAggregator]) — non-empty only when the
     * node owns neither [text] nor [desc], i.e. the anonymous-list-row case.
     *
     * Kept SEPARATE from [text] on purpose. The two are compared against
     * different things on the live node: [text] against `node.text`, this
     * against a freshly re-derived aggregate of the live node's descendants.
     * Collapsing them into one field is what made list rows untappable — the
     * gate compared an aggregated label to an empty own-text and every strategy
     * missed, so `tap` fail-closed on a row that had not moved at all.
     */
    val label: String = "",
    /**
     * Compose `Modifier.testTag`. Compared FIRST in the verify-before-act
     * predicate for the same reason `viewId` is: it is the only stable id a
     * Compose node has.
     */
    val testTag: String = "",
    val role: String,
    val className: String,
    val bounds: DescriptorBounds,
    val windowId: Int,
    val childPath: List<Int>,
)

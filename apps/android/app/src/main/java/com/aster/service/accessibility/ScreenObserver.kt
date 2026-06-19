package com.aster.service.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Single-pass traversal engine for `observe`.
 *
 * Walks each window root ONCE (P3 multi-window merge — the caller drives one
 * [walk] per window from getWindows()), building in the same accumulating
 * observer:
 *   - elements: flat filtered List<ObservedElement> in reading order with e<N> refs
 *   - descriptors: parallel NodeDescriptor cache entries (childPath recorded inline)
 *   - scrollables: List<Scrollable>
 *
 * Applies the [ElementFilter] mode/searchText predicate and a token-budget cap
 * (truncated flag). Recycles every getChild() node in a finally, even on the
 * truncation break. Does NOT recycle the [root] — the caller owns that.
 *
 * App Automations /goal I3 (SPEC §I3): the budget is SPLIT into two independent
 * buckets so a huge application window can never starve the system/navigation/
 * IME/decor windows (the bottom-nav "Post" tab must always survive):
 *   - application windows share an [maxElements] budget;
 *   - non-application windows (system / input_method / accessibility_overlay /
 *     decor) share a separate [systemReserve] budget.
 * The two buckets are counted independently, but every kept node still draws
 * from the SINGLE `e<N>` [refCounter] — one ref namespace across all windows
 * (SPEC §7.1), so reordering windows never produces colliding refs.
 *
 * INVARIANT: never returns or caches a live AccessibilityNodeInfo. Primitives
 * are extracted into POJOs during the walk.
 */
class ScreenObserver(
    private val mode: String,
    private val searchText: String?,
    private val maxElements: Int,
    private val systemReserve: Int = ElementFilter.SYSTEM_WINDOW_RESERVE,
) {

    /** Output of the accumulated walks; the caller assembles the ObserveResult. */
    data class Walk(
        val elements: List<ObservedElement>,
        val descriptors: List<NodeDescriptor>,
        val scrollables: List<Scrollable>,
        val truncated: Boolean,
    )

    private val elements = mutableListOf<ObservedElement>()
    private val descriptors = mutableListOf<NodeDescriptor>()
    private val scrollables = mutableListOf<Scrollable>()
    /** SINGLE e<N> ref namespace shared across every window (SPEC §7.1). */
    private var refCounter = 0
    /** Per-window-type budget (I3) — NOT a ref namespace; it only gates emits. */
    private val budget = ObserveBudget(appCap = maxElements, systemCap = systemReserve)
    private var truncated = false

    /**
     * Walk one window's [root] (a live node OWNED by the caller — not recycled
     * here), accumulating into the shared element/descriptor/scrollable lists and
     * the shared `e<N>` namespace. [isApplication] selects which budget bucket the
     * window draws from (I3): application windows share [maxElements], everything
     * else shares [systemReserve]. Re-invoke once per window; read the merged
     * result from the LAST returned [Walk].
     */
    fun walk(root: AccessibilityNodeInfo, windowId: Int, isApplication: Boolean): Walk {
        visit(root, windowId, intArrayOf().toList(), isApplication)
        return Walk(
            elements = elements.toList(),
            descriptors = descriptors.toList(),
            scrollables = scrollables.toList(),
            truncated = truncated,
        )
    }

    private fun visit(
        node: AccessibilityNodeInfo,
        windowId: Int,
        path: List<Int>,
        isApplication: Boolean,
    ) {
        // Only THIS window's bucket being full stops THIS walk — a full app
        // bucket must never block a later non-application window from filling its
        // reserve (and vice-versa), so the gate is per-bucket, never a single
        // global flag (I3).
        if (budget.isFull(isApplication)) return

        // Skip invisible nodes entirely (they are not addressable).
        val visible = node.isVisibleToUser
        if (visible) {
            emitIfKept(node, windowId, path, isApplication)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            if (budget.isFull(isApplication)) break
            val child = node.getChild(i) ?: continue
            try {
                visit(child, windowId, path + i, isApplication)
            } finally {
                child.recycle()
            }
        }
    }

    private fun emitIfKept(
        node: AccessibilityNodeInfo,
        windowId: Int,
        path: List<Int>,
        isApplication: Boolean,
    ) {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        val facts = NodeFacts(
            clickable = node.isClickable,
            editable = node.isEditable,
            checkable = node.isCheckable,
            scrollable = node.isScrollable,
            longClickable = node.isLongClickable,
            text = text,
            desc = desc,
        )

        if (!ElementFilter.keep(facts, mode, searchText)) return

        // I3: charge the kept node to its window's bucket; flip `truncated` and
        // stop only when THAT bucket is full (the other bucket keeps its slots).
        if (!budget.take(isApplication)) {
            truncated = true
            return
        }

        val ref = "e${refCounter++}"
        val className = node.className?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val role = RoleMapper.roleOf(
            className = className,
            isEditable = node.isEditable,
            isCheckable = node.isCheckable,
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            hasText = text.isNotEmpty() || desc.isNotEmpty(),
        )
        val bounds = boundsOf(node)
        val actions = node.actionList.mapNotNull { ActionMapper.normalize(it.id) }.distinct()

        // A scrollable container is an element too: cross-reference its e<N> ref
        // in scrollables[] so scroll(ref) re-resolves through the SAME descriptor
        // cache — one ref namespace for nodes (SPEC §3.1 scrollables[] use e-refs;
        // ElementFilter keeps scrollable in actionable mode, so `ref`/`bounds`
        // below always exist for a scrollable node).
        if (node.isScrollable) {
            scrollables.add(
                Scrollable(ref = ref, bounds = bounds, directions = scrollDirectionsOf(node)),
            )
        }

        elements.add(
            ObservedElement(
                ref = ref,
                role = role,
                text = text,
                desc = desc,
                viewId = viewId,
                window = windowId,
                bounds = bounds,
                state = ElementState(
                    clickable = node.isClickable,
                    editable = node.isEditable,
                    checkable = node.isCheckable,
                    checked = node.isChecked,
                    scrollable = node.isScrollable,
                    selected = node.isSelected,
                    focused = node.isFocused,
                    enabled = node.isEnabled,
                    password = node.isPassword,
                ),
                actions = actions,
            ),
        )

        descriptors.add(
            NodeDescriptor(
                ref = ref,
                viewId = viewId,
                text = text,
                role = role,
                className = className,
                bounds = DescriptorBounds.fromLTRB(bounds.x, bounds.y, bounds.x + bounds.w, bounds.y + bounds.h),
                windowId = windowId,
                childPath = path,
            ),
        )
    }

    private fun boundsOf(node: AccessibilityNodeInfo): Bounds {
        val r = Rect()
        node.getBoundsInScreen(r)
        return Bounds.fromLTRB(r.left, r.top, r.right, r.bottom)
    }

    private fun scrollDirectionsOf(node: AccessibilityNodeInfo): List<String> {
        val dirs = mutableListOf<String>()
        val ids = node.actionList.map { it.id }
        if (AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD in ids) dirs.add("up")
        if (AccessibilityNodeInfo.ACTION_SCROLL_FORWARD in ids) dirs.add("down")
        return dirs
    }
}

package com.aster.service.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Single-pass traversal engine for `observe`.
 *
 * Walks the active-window root ONCE (P1: rootInActiveWindow; P3 will merge
 * getWindows()), building in the same walk:
 *   - elements: flat filtered List<ObservedElement> in reading order with e<N> refs
 *   - descriptors: parallel NodeDescriptor cache entries (childPath recorded inline)
 *   - scrollables: List<Scrollable>
 *
 * Applies the [ElementFilter] mode/searchText predicate and a token-budget cap
 * (truncated flag). Recycles every getChild() node in a finally, even on the
 * truncation break. Does NOT recycle the [root] — the caller owns that.
 *
 * INVARIANT: never returns or caches a live AccessibilityNodeInfo. Primitives
 * are extracted into POJOs during the walk.
 */
class ScreenObserver(
    private val mode: String,
    private val searchText: String?,
    private val maxElements: Int,
) {

    /** Output of one walk; the caller assembles the ObserveResult. */
    data class Walk(
        val elements: List<ObservedElement>,
        val descriptors: List<NodeDescriptor>,
        val scrollables: List<Scrollable>,
        val truncated: Boolean,
    )

    private val elements = mutableListOf<ObservedElement>()
    private val descriptors = mutableListOf<NodeDescriptor>()
    private val scrollables = mutableListOf<Scrollable>()
    private var refCounter = 0
    private var truncated = false

    /** Walk [root] (a live node OWNED by the caller — not recycled here). */
    fun walk(root: AccessibilityNodeInfo, windowId: Int): Walk {
        visit(root, windowId, intArrayOf().toList())
        return Walk(
            elements = elements.toList(),
            descriptors = descriptors.toList(),
            scrollables = scrollables.toList(),
            truncated = truncated,
        )
    }

    private fun visit(node: AccessibilityNodeInfo, windowId: Int, path: List<Int>) {
        if (truncated) return

        // Skip invisible nodes entirely (they are not addressable).
        val visible = node.isVisibleToUser
        if (visible) {
            emitIfKept(node, windowId, path)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            if (truncated) break
            val child = node.getChild(i) ?: continue
            try {
                visit(child, windowId, path + i)
            } finally {
                child.recycle()
            }
        }
    }

    private fun emitIfKept(node: AccessibilityNodeInfo, windowId: Int, path: List<Int>) {
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

        if (elements.size >= maxElements) {
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

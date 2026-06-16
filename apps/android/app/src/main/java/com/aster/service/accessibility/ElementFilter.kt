package com.aster.service.accessibility

/**
 * Lightweight in-memory projection of the node fields the observe filter needs.
 * Lets the predicate run headless (no AccessibilityNodeInfo) and be unit-tested.
 */
data class NodeFacts(
    val clickable: Boolean,
    val editable: Boolean,
    val checkable: Boolean,
    val scrollable: Boolean,
    val longClickable: Boolean,
    val text: String,
    val desc: String,
)

/** SPEC §3.1 observe modes. */
object ObserveMode {
    const val ACTIONABLE = "actionable"
    const val TEXT = "text"
    const val FULL = "full"
}

/**
 * Pure observe filter predicate + token-budget constant.
 *
 *  - actionable (default): clickable | editable | checkable | scrollable | long-clickable.
 *  - text: any node with text or content-description (reading view).
 *  - full: everything (debug).
 *
 * [searchText] (case-insensitive) narrows the kept set to nodes whose text OR
 * desc contains it, applied on top of the mode predicate.
 */
object ElementFilter {

    /** Token-budget cap on emitted elements. Beyond this, ObserveResult.truncated = true. */
    const val MAX_ELEMENTS_DEFAULT = 100

    fun keep(node: NodeFacts, mode: String, searchText: String?): Boolean {
        val passesMode = when (mode) {
            ObserveMode.FULL -> true
            ObserveMode.TEXT -> node.text.isNotEmpty() || node.desc.isNotEmpty()
            else -> // ACTIONABLE (default)
                node.clickable || node.editable || node.checkable ||
                    node.scrollable || node.longClickable
        }
        if (!passesMode) return false

        if (searchText.isNullOrEmpty()) return true
        return node.text.contains(searchText, ignoreCase = true) ||
            node.desc.contains(searchText, ignoreCase = true)
    }
}

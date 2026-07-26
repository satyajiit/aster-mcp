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
 * desc OR aggregated label contains it, applied on top of the mode predicate.
 *
 * The label clause is not a nicety. On the dominant list-row pattern the row
 * itself has NO text — its words live in child TextViews, which `actionable`
 * mode filters out — so descendant aggregation is what makes the row
 * addressable at all. Matching search against the node's own text only meant
 * `search_text: "Network"` returned ZERO elements on a Settings screen whose
 * first row plainly reads "Network & internet", i.e. narrowing failed on
 * exactly the shape it exists to narrow.
 */
object ElementFilter {

    /**
     * Token-budget cap on emitted elements from APPLICATION windows. Beyond this,
     * ObserveResult.truncated = true. Raised 100 → 160 (App Automations /goal I3)
     * so dense feeds (e.g. a scrolled LinkedIn/X timeline) still leave room for
     * the rest of the screen to register.
     */
    const val MAX_ELEMENTS_DEFAULT = 160

    /**
     * Separate, reserved element budget for NON-application windows — system /
     * navigation / decor / input-method / accessibility-overlay (App Automations
     * /goal I3, SPEC §I3). Counted independently of [MAX_ELEMENTS_DEFAULT] so a
     * huge application window can never consume the bottom-navigation window's
     * slots; the model always sees its clickable tabs (the "Post" CTA).
     */
    const val SYSTEM_WINDOW_RESERVE = 25

    /**
     * The MODE half of the predicate. Split out so the caller can decide what a
     * node is before paying to aggregate a label for it — see
     * [matchesSearch].
     */
    fun keepByMode(node: NodeFacts, mode: String): Boolean = when (mode) {
        ObserveMode.FULL -> true
        ObserveMode.TEXT -> node.text.isNotEmpty() || node.desc.isNotEmpty()
        else -> // ACTIONABLE (default)
            node.clickable || node.editable || node.checkable ||
                node.scrollable || node.longClickable
    }

    /**
     * The SEARCH half. `label` is the descendant-aggregated name for a node that
     * says nothing itself (empty when the node has its own text/desc, or when
     * the caller has not aggregated one).
     */
    fun matchesSearch(node: NodeFacts, label: String, searchText: String?): Boolean {
        if (searchText.isNullOrEmpty()) return true
        return node.text.contains(searchText, ignoreCase = true) ||
            node.desc.contains(searchText, ignoreCase = true) ||
            label.contains(searchText, ignoreCase = true)
    }

    /** Both halves, for callers with no label to offer. */
    fun keep(node: NodeFacts, mode: String, searchText: String?): Boolean =
        keepByMode(node, mode) && matchesSearch(node, "", searchText)
}

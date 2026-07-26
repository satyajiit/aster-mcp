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

    private companion object {
        /**
         * Where Compose stashes `Modifier.testTag`. Not public API — it is an
         * extras-bundle key Compose's semantics layer writes — so it is spelled
         * out here rather than referenced, and read defensively.
         */
        const val COMPOSE_TEST_TAG_KEY = "androidx.compose.ui.semantics.testTag"

        /**
         * Ceiling on nodes COLLECTED before ranking. Not the answer size — the
         * per-window budget still decides that — just a bound on the work, so a
         * pathological tree cannot make ranking unbounded. Comfortably above any
         * real screen.
         */
        const val COLLECT_HARD_CAP = 2000
    }

    /** Output of the accumulated walks; the caller assembles the ObserveResult. */
    data class Walk(
        val elements: List<ObservedElement>,
        val descriptors: List<NodeDescriptor>,
        val scrollables: List<Scrollable>,
        val truncated: Boolean,
    )

    /** Every kept node, in walk (reading) order, before ranking. */
    private val candidates = mutableListOf<Cand>()
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
        return finish()
    }

    /**
     * Rank the collected candidates, apply the per-bucket budget, and build the
     * result in reading order.
     *
     * Idempotent, and safe to call after every [walk] — which is what keeps the
     * caller's "read the LAST returned Walk" contract working while the budget
     * is now applied across ALL windows rather than first-come-first-served.
     */
    fun finish(): Walk {
        val elements = mutableListOf<ObservedElement>()
        val descriptors = mutableListOf<NodeDescriptor>()
        val scrollables = mutableListOf<Scrollable>()

        // Rank first, THEN meter — so the budget is spent on the most useful
        // elements rather than on whichever came first in the tree. The two-bucket
        // rule still holds ([ObserveBudget]): a busy application window can never
        // consume the system/navigation window's reserved slots (I3), which is
        // what keeps the bottom-nav "Post" tab visible on a dense feed.
        val budget = ObserveBudget(appCap = maxElements, systemCap = systemReserve)
        val kept = mutableListOf<Cand>()
        var dropped = false
        candidates
            .sortedWith(compareByDescending<Cand> { it.rank() }.thenBy { it.order })
            .forEach { c ->
                if (budget.take(c.isApplication)) kept.add(c) else dropped = true
            }
        // Back to reading order: rank decides WHAT survives, never what order the
        // model reads it in. A tree shuffled by score is much harder to reason
        // about, and `e<N>` refs should still ascend down the screen.
        kept.sortBy { it.order }

        kept.forEachIndexed { index, c ->
            val ref = "e$index"
            if (c.scrollable) {
                scrollables.add(Scrollable(ref = ref, bounds = c.bounds, directions = c.scrollDirections))
            }
            elements.add(
                ObservedElement(
                    ref = ref,
                    role = c.role,
                    text = c.text,
                    desc = c.desc,
                    label = c.label,
                    viewId = c.viewId,
                    testTag = c.testTag,
                    window = c.windowId,
                    bounds = c.bounds,
                    state = c.state,
                    actions = c.actions,
                ),
            )
            descriptors.add(
                NodeDescriptor(
                    ref = ref,
                    viewId = c.viewId,
                    // `text` is the node's OWN text and nothing else. It is compared
                    // against `node.text` by the verify-before-act gate and fed to
                    // findAccessibilityNodeInfosByText by re-resolution, and both of
                    // those read the node's own text — so substituting the aggregated
                    // descendant label here made every anonymous list row unmatchable
                    // by every strategy. The label travels in its own field.
                    text = c.text,
                    label = c.label,
                    desc = c.desc,
                    testTag = c.testTag,
                    role = c.role,
                    className = c.className,
                    bounds = DescriptorBounds.fromLTRB(
                        c.bounds.x, c.bounds.y, c.bounds.x + c.bounds.w, c.bounds.y + c.bounds.h,
                    ),
                    windowId = c.windowId,
                    childPath = c.path,
                ),
            )
        }

        return Walk(
            elements = elements,
            descriptors = descriptors,
            scrollables = scrollables,
            truncated = truncated || dropped,
        )
    }

    private fun visit(
        node: AccessibilityNodeInfo,
        windowId: Int,
        path: List<Int>,
        isApplication: Boolean,
    ) {
        // The walk no longer stops at a budget: truncation happens in [finish],
        // AFTER ranking, so the cap is spent on the most useful elements rather
        // than on whichever happened to come first in the tree. Only the hard
        // collection cap (checked per emit) bounds this.
        if (candidates.size >= COLLECT_HARD_CAP) return

        // Skip invisible nodes entirely (they are not addressable).
        val visible = node.isVisibleToUser
        if (visible) {
            emitIfKept(node, windowId, path, isApplication)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            if (candidates.size >= COLLECT_HARD_CAP) break
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

        if (!ElementFilter.keepByMode(facts, mode)) return

        // Name the anonymous box BEFORE the search filter. A list row has no text
        // of its own — its words are in descendants that `actionable` mode drops —
        // so a search matched against the node's own text alone returned ZERO
        // elements for "Network" on a screen whose first row reads
        // "Network & internet". Aggregating after the mode gate (not before) keeps
        // the cost exactly where it was: only nodes that survive the mode filter
        // pay for it. Aggregating over a node that already has text would only
        // make its text longer and less exact, so that case stays empty.
        val label = if (text.isEmpty() && desc.isEmpty()) LabelAggregator.aggregate(node) else ""
        if (!ElementFilter.matchesSearch(facts, label, searchText)) return

        // Hard safety cap on COLLECTION (not on the answer). Ranking needs to see
        // every candidate before it can choose, but a pathological tree must not
        // be able to make that unbounded. Sized well above any real screen.
        if (candidates.size >= COLLECT_HARD_CAP) {
            truncated = true
            return
        }
        val className = node.className?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val testTag = testTagOf(node)
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
        candidates.add(
            Cand(
                order = candidates.size,
                isApplication = isApplication,
                windowId = windowId,
                path = path,
                text = text,
                desc = desc,
                label = label,
                viewId = viewId,
                testTag = testTag,
                role = role,
                className = className,
                bounds = bounds,
                actions = actions,
                scrollable = node.isScrollable,
                editable = node.isEditable,
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
                scrollDirections = if (node.isScrollable) scrollDirectionsOf(node) else emptyList(),
            ),
        )
    }

    /**
     * One kept node, held until every window has been walked.
     *
     * Collected rather than emitted so [finish] can RANK before truncating. The
     * old walk emitted in reading order and stopped at the cap — first-N-wins —
     * so on a dense feed the budget was spent on whatever happened to be at the
     * top of the tree, and the "Post" button at the bottom simply did not exist
     * as far as the model was concerned.
     */
    private data class Cand(
        val order: Int,
        val isApplication: Boolean,
        val windowId: Int,
        val path: List<Int>,
        val text: String,
        val desc: String,
        val label: String,
        val viewId: String,
        val testTag: String,
        val role: String,
        val className: String,
        val bounds: Bounds,
        val actions: List<String>,
        val scrollable: Boolean,
        val editable: Boolean,
        val state: ElementState,
        val scrollDirections: List<String>,
    ) {
        /**
         * Higher survives truncation. Signals, in order of weight:
         *
         * * **Scrollable containers are never dropped.** Losing one costs the
         *   agent the ability to reach anything below the fold — a far larger
         *   loss than any single element.
         * * **Named beats anonymous.** An element the model can refer to is worth
         *   more than one it can only point at.
         * * **A stable id beats no id**, for the same reason it wins in matching.
         * * **Editable fields**, because a form the agent cannot see is a dead end.
         * * **Bigger tap targets**, as a weak tiebreak — a 4px spacer that happens
         *   to be clickable is not what anyone means to tap.
         * * **Zero-area nodes are pushed to the back**: not tappable, not useful.
         *
         * Mirrored by `ObserveRankTest`, which pins this rule on the JVM (this
         * type is private walk state, so the test carries a copy of the formula).
         * Change one, change both — otherwise the budget silently reverts to
         * first-N-wins and a dense feed loses its bottom-nav CTA again.
         */
        fun rank(): Int {
            var score = 0
            if (scrollable) score += 1000
            if (text.isNotEmpty() || desc.isNotEmpty() || label.isNotEmpty()) score += 400
            if (viewId.isNotEmpty() || testTag.isNotEmpty()) score += 200
            if (editable) score += 100
            val area = bounds.w.toLong() * bounds.h.toLong()
            score += if (area <= 0L) -500 else (area / 1000L).coerceAtMost(100L).toInt()
            return score
        }
    }


    /**
     * The node's Jetpack Compose `testTag`, or "" when it has none.
     *
     * Compose sets NO `viewIdResourceName`. On a Compose screen the strongest
     * primitive in the whole ladder is therefore simply absent, and every step
     * falls through to matching on visible text — which is exactly the primitive
     * that changes when an app is translated, A/B tested, or reworded. `testTag`
     * is the closest thing Compose has to a stable id, and it rides in the node's
     * extras bundle rather than in any first-class field, so nothing surfaces it
     * unless we go and look.
     *
     * The key is Compose's own internal constant. Read defensively: the bundle is
     * IPC'd from the app's process, so a malformed or absent value must degrade to
     * "no tag", never throw inside the observe walk.
     */
    private fun testTagOf(node: AccessibilityNodeInfo): String =
        runCatching {
            node.extras?.getString(COMPOSE_TEST_TAG_KEY)?.trim().orEmpty()
        }.getOrDefault("")

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

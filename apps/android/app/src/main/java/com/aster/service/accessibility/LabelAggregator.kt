package com.aster.service.accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * The ONE definition of "what is this anonymous control called".
 *
 * The dominant Android list-row shape is a clickable `LinearLayout` carrying no
 * `resource-id`, no `text` and no `contentDescription`, whose label lives in
 * child `TextView`s that are themselves neither clickable nor focusable. P3
 * names such a row by aggregating those descendants.
 *
 * That aggregate has to be produced by the SAME code in two places, or the
 * feature is worse than useless:
 *
 *  1. [ScreenObserver], when it emits the element and caches its
 *     [NodeDescriptor] — perception.
 *  2. `AsterAccessibilityService.resolveRef`, when it re-derives the label of a
 *     LIVE candidate node to decide whether it is the same row — action.
 *
 * Those two ran different code exactly once, and the result was that every
 * anonymous list row became permanently untappable: the observer stored the
 * aggregated label in `NodeDescriptor.text`, and the verify-before-act gate
 * compared that string against the live node's OWN `text` — which is empty for
 * precisely the rows the aggregation exists to serve. Every strategy missed,
 * and with the coordinate fallback correctly defaulted off, `tap` returned
 * `stale_ref` on a row that was sitting right there under the finger.
 *
 * Hence: one object, two callers, no second implementation.
 */
object LabelAggregator {

    /**
     * How deep [aggregate] descends. A row's label is one or two levels down;
     * deeper than that and we are describing a layout, not naming a control.
     */
    const val LABEL_MAX_DEPTH = 3

    /** How many distinct text fragments one label may join. */
    const val LABEL_MAX_PARTS = 6

    /** Hard ceiling on an aggregated label, in characters. */
    const val LABEL_MAX_CHARS = 120

    /** Joiner between fragments. Also the split point for [primary]. */
    const val SEPARATOR = " · "

    /**
     * Gather text from a node's descendants to name it.
     *
     * Three bounds, each load-bearing:
     *
     * * **Stop at a nested actionable node.** Its text belongs to IT. A list row
     *   containing a "Follow" button must not absorb "Follow" — that is how a
     *   row ends up labelled with the name of the button inside it, and how a
     *   selector for the button starts matching the row.
     * * **[LABEL_MAX_DEPTH].** See above.
     * * **[LABEL_MAX_PARTS] / [LABEL_MAX_CHARS].** A scrollable container's
     *   subtree is the whole screen. Without a cap, "aggregate the descendants"
     *   means "concatenate everything", which is worse than the anonymous box it
     *   replaces — it would blow the observe payload AND be useless to match on.
     *
     * Every child fetched here is recycled in a `finally`.
     */
    fun aggregate(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()

        fun gather(n: AccessibilityNodeInfo, depth: Int) {
            if (depth > LABEL_MAX_DEPTH || parts.size >= LABEL_MAX_PARTS) return
            val count = n.childCount
            for (i in 0 until count) {
                if (parts.size >= LABEL_MAX_PARTS) return
                val child = n.getChild(i) ?: continue
                try {
                    if (!child.isVisibleToUser) continue
                    // A control of its own — it will be emitted separately and
                    // owns its text.
                    val ownsItsText = child.isClickable || child.isEditable ||
                        child.isCheckable || child.isLongClickable
                    val childText = child.text?.toString()?.trim().orEmpty()
                    val childDesc = child.contentDescription?.toString()?.trim().orEmpty()
                    if (!ownsItsText) {
                        val part = childText.ifEmpty { childDesc }
                        if (part.isNotEmpty() && parts.none { it.equals(part, ignoreCase = true) }) {
                            parts.add(part)
                        }
                        gather(child, depth + 1)
                    }
                } finally {
                    child.recycle()
                }
            }
        }

        gather(node, 1)
        if (parts.isEmpty()) return ""
        val joined = parts.joinToString(SEPARATOR)
        return if (joined.length <= LABEL_MAX_CHARS) {
            joined
        } else {
            joined.take(LABEL_MAX_CHARS).trimEnd()
        }
    }

    /**
     * The row's title — the first fragment. Fragments are gathered in tree order,
     * and the title `TextView` precedes the summary in every stock row layout
     * (`android:id/title` then `android:id/summary`).
     */
    fun primary(label: String): String = label.substringBefore(SEPARATOR).trim()

    /**
     * Does a live node's re-derived label identify the same row as the cached one?
     *
     * Exact equality is the strong answer. The weaker one — equal FIRST fragments —
     * exists because the trailing fragments are exactly the volatile part: the row
     * says "Network & internet · Mobile, Wi-Fi, hotspot" today and may say
     * "· Wi-Fi, hotspot" after the SIM is pulled; an Uber ride row's title is the
     * product and its summary is an ETA that changes every few seconds. Requiring
     * byte equality there would fail closed on a row that has not moved, which is
     * the same "honest failure that is nonetheless wrong" this whole path is
     * trying to stop producing.
     *
     * The title alone is NOT a licence to act: the caller still requires role /
     * className agreement and, in the nearest-bounds strategy, proximity.
     *
     * Only ever consulted when the cached side actually HAS a label — a descriptor
     * whose node owned its own text is matched on that text as before.
     */
    fun matches(cached: String?, node: String?): Boolean {
        val c = cached?.trim().orEmpty()
        val n = node?.trim().orEmpty()
        if (c.isEmpty()) return true
        if (n.isEmpty()) return false
        if (c.equals(n, ignoreCase = true)) return true
        val cp = primary(c)
        val np = primary(n)
        return cp.isNotEmpty() && cp.equals(np, ignoreCase = true)
    }
}

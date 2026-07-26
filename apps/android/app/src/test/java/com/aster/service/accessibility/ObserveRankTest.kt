package com.aster.service.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The observe budget must be spent on the most USEFUL elements, not on whichever
 * happened to come first in the tree.
 *
 * `ScreenObserver` itself needs a live `AccessibilityNodeInfo` to exercise, so
 * this pins the ranking RULE — the pure part that decides what survives
 * truncation — against the same signal set the observer scores on. If this and
 * `ScreenObserver.Cand.rank()` ever disagree, the budget silently goes back to
 * first-N-wins and a dense feed loses its bottom-nav CTA again.
 */
class ObserveRankTest {

    /**
     * Mirror of `ScreenObserver.Cand.rank()`. Deliberately a copy: `Cand` is
     * private to the observer (it holds walk state that has no meaning outside
     * one traversal), and duplicating six lines is a smaller cost than widening
     * that type's visibility. The doc on both sides names the other.
     */
    private fun rank(
        scrollable: Boolean = false,
        named: Boolean = false,
        stableId: Boolean = false,
        editable: Boolean = false,
        w: Int = 100,
        h: Int = 100,
    ): Int {
        var score = 0
        if (scrollable) score += 1000
        if (named) score += 400
        if (stableId) score += 200
        if (editable) score += 100
        val area = w.toLong() * h.toLong()
        score += if (area <= 0L) -500 else (area / 1000L).coerceAtMost(100L).toInt()
        return score
    }

    @Test
    fun scrollableContainersOutrankEverything() {
        // Losing a scrollable costs the agent the ability to reach anything below
        // the fold — a far bigger loss than any single element.
        assertTrue(
            rank(scrollable = true, w = 10, h = 10) >
                rank(named = true, stableId = true, editable = true, w = 800, h = 400),
        )
    }

    @Test
    fun namedElementsOutrankAnonymousOnes() {
        // An element the model can refer to beats one it can only point at.
        assertTrue(rank(named = true) > rank(named = false, stableId = true))
    }

    @Test
    fun aStableIdOutranksNothing() {
        assertTrue(rank(stableId = true) > rank())
    }

    @Test
    fun zeroAreaNodesArePushedToTheBack() {
        // A 0×0 node that happens to be clickable is not what anyone means to tap.
        assertTrue(rank(named = true, w = 0, h = 0) < rank(named = true))
        assertTrue(rank(w = 0, h = 0) < 0)
    }

    @Test
    fun biggerTapTargetsWinOnlyAsATiebreak() {
        // Size discriminates between otherwise-equal candidates…
        assertTrue(rank(w = 400, h = 400) > rank(w = 20, h = 20))
        // …but can never outweigh being named.
        assertTrue(rank(named = true, w = 20, h = 20) > rank(named = false, w = 4000, h = 4000))
    }

    @Test
    fun theAreaBonusIsCapped() {
        // Otherwise one full-screen view would dominate the entire ranking.
        assertEquals(rank(w = 10_000, h = 10_000), rank(w = 1000, h = 100))
    }
}

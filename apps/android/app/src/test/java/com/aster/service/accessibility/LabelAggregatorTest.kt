package com.aster.service.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure half of [LabelAggregator] — the identity rule for anonymous rows.
 * [LabelAggregator.aggregate] needs a live node and is covered on-device.
 */
class LabelAggregatorTest {

    @Test
    fun primary_isTheTitleFragment() {
        assertEquals(
            "Network & internet",
            LabelAggregator.primary("Network & internet · Mobile, Wi-Fi, hotspot"),
        )
    }

    @Test
    fun primary_ofSingleFragmentLabel_isTheWholeLabel() {
        assertEquals("Continue", LabelAggregator.primary("Continue"))
    }

    @Test
    fun primary_doesNotSplitOnABareMiddleDot() {
        // The joiner is " · " with spaces. A title that itself contains "·" with
        // no surrounding spaces is one fragment, not two.
        assertEquals("4·20 Studio", LabelAggregator.primary("4·20 Studio"))
    }

    @Test
    fun matches_exact() {
        assertTrue(LabelAggregator.matches("Settings · System", "Settings · System"))
    }

    @Test
    fun matches_titleEqual_summaryDrifted() {
        assertTrue(LabelAggregator.matches("UberGo · 4 min · ₹243", "UberGo · 7 min · ₹268"))
    }

    @Test
    fun matches_differentTitle_isRejected() {
        assertFalse(LabelAggregator.matches("Network & internet · Wi-Fi", "Connected devices · Bluetooth"))
    }

    @Test
    fun matches_emptyCached_isVacuouslyTrue() {
        // Only consulted when the cached side HAS a label; an absent one must not
        // veto a descriptor that matched on its own text.
        assertTrue(LabelAggregator.matches("", "anything"))
        assertTrue(LabelAggregator.matches(null, null))
    }

    @Test
    fun matches_liveNodeLostItsLabel_isRejected() {
        assertFalse(LabelAggregator.matches("Network & internet", ""))
        assertFalse(LabelAggregator.matches("Network & internet", null))
    }
}

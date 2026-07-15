package com.aster.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionStatusModelTest {
    @Test
    fun `wire null and empty text clear the readout`() {
        assertTrue(parseCompanionStatus("null", 1_000L).valid)
        assertNull(parseCompanionStatus("null", 1_000L).status)
        assertNull(parseCompanionStatus("{\"mode\":\"side\",\"text\":\"\"}", 1_000L).status)
    }

    @Test
    fun `side status is bounded clamped and self expiring`() {
        val parsed = parseCompanionStatus(
            """{"mode":"side","text":"Charging","detail":"42%","icon":"bolt","hue":"#34d399","progress":4}""",
            2_000L,
        )
        assertTrue(parsed.valid)
        assertEquals("Charging", parsed.status?.text)
        assertEquals(1f, parsed.status?.progress)
        assertEquals(10_000L, parsed.status?.expiresAtMs)
    }

    @Test
    fun `full status persists and malformed payload is rejected`() {
        val full = parseCompanionStatus("{\"mode\":\"full\",\"text\":\"24:59\"}", 5_000L)
        assertEquals(0L, full.status?.expiresAtMs)
        assertFalse(parseCompanionStatus("{not-json", 5_000L).valid)
    }

    @Test
    fun `full countdown carries a bounded native deadline`() {
        val status = parseCompanionStatus(
            """{"mode":"full","text":"24:59","countdownEndsAtMs":160000,"countdownTotalSeconds":1500}""",
            nowMs = 10_000L,
            wallNowMs = 100_000L,
        ).status ?: error("countdown should parse")

        assertEquals(160_000L, status.countdownEndsAtEpochMs)
        assertEquals(1_500, status.countdownTotalSeconds)
    }

    @Test
    fun `face only interlude preserves and later reveals countdown`() {
        val status = parseCompanionStatus(
            """{"mode":"side","text":"","countdownEndsAtMs":160000,"countdownTotalSeconds":1500,"countdownRevealAtMs":104000}""",
            nowMs = 10_000L,
            wallNowMs = 100_000L,
        ).status ?: error("interlude should preserve countdown")

        assertEquals("full", status.mode)
        assertEquals("", status.text)
        assertEquals(104_000L, status.visibleAfterEpochMs)
        assertEquals(0L, status.expiresAtMs)
    }
}

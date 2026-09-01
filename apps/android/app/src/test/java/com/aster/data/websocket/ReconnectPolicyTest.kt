package com.aster.data.websocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyTest {

    @Test
    fun reconnectDelay_base2sDoublesUntil30sCap() {
        assertEquals(2_000L, ReconnectPolicy.reconnectDelay(0))
        assertEquals(4_000L, ReconnectPolicy.reconnectDelay(1))
        assertEquals(8_000L, ReconnectPolicy.reconnectDelay(2))
        assertEquals(16_000L, ReconnectPolicy.reconnectDelay(3))
        assertEquals(30_000L, ReconnectPolicy.reconnectDelay(4))
        assertEquals(30_000L, ReconnectPolicy.reconnectDelay(10))
    }

    @Test
    fun shouldIgnoreStale_whenGenerationDiffers() {
        assertFalse(ReconnectPolicy.shouldIgnoreStale(1, 1))
        assertTrue(ReconnectPolicy.shouldIgnoreStale(1, 2))
        assertTrue(ReconnectPolicy.shouldIgnoreStale(0, 1))
        assertFalse(ReconnectPolicy.shouldIgnoreStale(7, 7))
    }

    @Test
    fun ackTimedOut_zeroLastAckAtIsNotTimedOut() {
        assertFalse(
            ReconnectPolicy.ackTimedOut(lastAckAt = 0L, now = 120_000L, intervalMs = 30_000L)
        )
    }

    @Test
    fun ackTimedOut_greaterThanTwiceInterval() {
        val interval = 30_000L
        val lastAck = 1_000L
        assertFalse(ReconnectPolicy.ackTimedOut(lastAck, lastAck + 2 * interval, interval))
        assertTrue(ReconnectPolicy.ackTimedOut(lastAck, lastAck + 2 * interval + 1, interval))
    }
}

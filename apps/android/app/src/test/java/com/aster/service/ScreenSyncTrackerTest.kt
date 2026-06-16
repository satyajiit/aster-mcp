package com.aster.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for ScreenSyncTracker quiescence math.
 * 'now' is always injected so no Android/Handler/clock deps are needed.
 */
class ScreenSyncTrackerTest {

    @Test
    fun idle_when_quiet_period_elapsed() {
        val t = ScreenSyncTracker()
        t.recordChange(now = 1_000L)
        // 600ms later, with a 500ms quiet window -> idle
        assertTrue(t.isIdle(now = 1_600L, quietMs = 500L))
        assertEquals(0L, t.remainingQuietMs(now = 1_600L, quietMs = 500L))
    }

    @Test
    fun not_idle_before_quiet_period_elapsed() {
        val t = ScreenSyncTracker()
        t.recordChange(now = 1_000L)
        // only 200ms later -> not idle, 300ms remaining
        assertFalse(t.isIdle(now = 1_200L, quietMs = 500L))
        assertEquals(300L, t.remainingQuietMs(now = 1_200L, quietMs = 500L))
    }

    @Test
    fun idle_boundary_is_inclusive() {
        val t = ScreenSyncTracker()
        t.recordChange(now = 1_000L)
        // exactly quietMs later -> idle (remaining 0)
        assertTrue(t.isIdle(now = 1_500L, quietMs = 500L))
        assertEquals(0L, t.remainingQuietMs(now = 1_500L, quietMs = 500L))
    }

    @Test
    fun content_change_resets_the_clock() {
        val t = ScreenSyncTracker()
        t.recordChange(now = 1_000L)
        // would be idle at 1_600...
        assertTrue(t.isIdle(now = 1_600L, quietMs = 500L))
        // but a new change at 1_550 pushes the deadline out to 2_050
        t.recordChange(now = 1_550L)
        assertFalse(t.isIdle(now = 1_600L, quietMs = 500L))
        assertEquals(450L, t.remainingQuietMs(now = 1_600L, quietMs = 500L))
    }

    @Test
    fun state_change_counts_as_activity_and_bumps_revision() {
        val t = ScreenSyncTracker()
        val r0 = t.revision()
        t.recordChange(now = 1_000L) // content or state — both call recordChange
        assertEquals(r0 + 1, t.revision())
        assertFalse(t.isIdle(now = 1_100L, quietMs = 500L))
    }

    @Test
    fun revision_increments_monotonically_per_change() {
        val t = ScreenSyncTracker()
        val start = t.revision()
        t.recordChange(now = 10L)
        t.recordChange(now = 20L)
        t.recordChange(now = 30L)
        assertEquals(start + 3, t.revision())
    }

    @Test
    fun idle_before_any_change_uses_construction_baseline() {
        // No change recorded yet: quietMs after construction-time baseline is idle.
        // We pass now far in the future so the (now - lastChange) >= quietMs holds.
        val t = ScreenSyncTracker(baselineNow = 0L)
        assertTrue(t.isIdle(now = 10_000L, quietMs = 500L))
    }

    @Test
    fun remaining_never_negative() {
        val t = ScreenSyncTracker()
        t.recordChange(now = 1_000L)
        // long past the quiet window -> clamps to 0, never negative
        assertEquals(0L, t.remainingQuietMs(now = 99_999L, quietMs = 500L))
    }
}

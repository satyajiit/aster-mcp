package com.aster.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionNativeAnimatorTest {
    @Test
    fun `fresh remote geometry is not double animated`() {
        val animator = CompanionNativeAnimator()
        assertEquals(CompanionMotionFrame(), animator.sample(10_000L, 9_900L))
    }

    @Test
    fun `stale remote geometry becomes a living autonomous face`() {
        val animator = CompanionNativeAnimator()
        val first = animator.sample(10_000L, 1_000L)
        val next = animator.sample(10_200L, 1_000L)
        assertTrue(first.autonomous)
        assertTrue(next.autonomous)
        assertNotEquals(first, next)
    }

    @Test
    fun `lift persists until an explicit landing reaction`() {
        val animator = CompanionNativeAnimator()
        animator.react(CompanionReaction.LIFT, 1_000L)
        assertEquals(CompanionReaction.LIFT, animator.sample(60_000L, 60_000L).decoration)
        animator.land(60_100L)
        assertEquals(CompanionReaction.LAND, animator.sample(60_200L, 60_200L).decoration)
    }

    @Test
    fun `stale geometry keeps native speech alive while fresh geometry is untouched`() {
        val animator = CompanionNativeAnimator()
        animator.setSpeaking(
            active = true,
            nowMs = 1_000L,
            energy = 0.8f,
            viseme = CompanionViseme.OPEN,
            expiresAtMs = 31_000L,
        )

        assertEquals(CompanionMotionFrame(), animator.sample(1_100L, 1_050L))
        val first = animator.sample(2_000L, 1_000L)
        val next = animator.sample(2_100L, 1_000L)
        assertTrue(first.autonomous)
        assertTrue(first.mouthScaleY > 0f)
        assertNotEquals(first, next)
    }

    @Test
    fun `speech watchdog expires instead of wedging the mouth`() {
        val animator = CompanionNativeAnimator()
        animator.setSpeaking(
            active = true,
            nowMs = 1_000L,
            energy = 0.8f,
            expiresAtMs = 2_000L,
        )
        val afterExpiry = animator.sample(2_001L, 1_000L)
        // Ambient fallback remains alive, but no speech decoration/state survives.
        assertTrue(afterExpiry.autonomous)
        assertEquals(null, afterExpiry.decoration)
    }

    @Test
    fun `reduced motion retains mouth articulation without body bob`() {
        val animator = CompanionNativeAnimator()
        animator.setSpeaking(
            active = true,
            nowMs = 1_000L,
            viseme = CompanionViseme.WIDE,
            expiresAtMs = 31_000L,
            reducedMotion = true,
        )
        val frame = animator.sample(2_000L, 1_000L)
        assertTrue(frame.autonomous)
        assertEquals(0f, frame.dy)
        assertEquals(0f, frame.rotationDeg)
        assertNotEquals(1f, frame.mouthScaleX)
    }
}

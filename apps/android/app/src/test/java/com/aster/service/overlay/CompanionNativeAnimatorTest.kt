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
}


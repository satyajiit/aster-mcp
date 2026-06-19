package com.aster.service.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * App Automations /goal I3 (SPEC §I3): the two-bucket observe budget must keep a
 * small system/navigation window's elements even when a large application window
 * was walked first and exhausted the application budget.
 */
class ObserveBudgetTest {

    @Test
    fun app_bucket_caps_at_appCap() {
        val budget = ObserveBudget(appCap = 3, systemCap = 2)
        assertTrue(budget.take(isApplication = true))
        assertTrue(budget.take(isApplication = true))
        assertTrue(budget.take(isApplication = true))
        // Fourth application element is rejected — bucket full.
        assertTrue(budget.isFull(isApplication = true))
        assertFalse(budget.take(isApplication = true))
    }

    @Test
    fun system_bucket_caps_at_systemCap() {
        val budget = ObserveBudget(appCap = 5, systemCap = 2)
        assertTrue(budget.take(isApplication = false))
        assertTrue(budget.take(isApplication = false))
        assertTrue(budget.isFull(isApplication = false))
        assertFalse(budget.take(isApplication = false))
    }

    @Test
    fun full_app_bucket_does_not_starve_system_window() {
        // A huge app window walked FIRST exhausts the application budget...
        val budget = ObserveBudget(appCap = 4, systemCap = 3)
        repeat(10) { budget.take(isApplication = true) }
        assertTrue("app bucket should be full", budget.isFull(isApplication = true))

        // ...yet the bottom-nav/system window walked next still gets its reserve:
        // every one of its systemCap slots must be accepted (the "Post" tab survives).
        assertFalse("system bucket must NOT be full", budget.isFull(isApplication = false))
        assertTrue(budget.take(isApplication = false))
        assertTrue(budget.take(isApplication = false))
        assertTrue(budget.take(isApplication = false))
        assertTrue(budget.isFull(isApplication = false))
    }

    @Test
    fun system_window_emits_when_walked_after_large_app_window() {
        // Models the real ordering+budget contract: system/nav window walked
        // AFTER a dense app window still emits exactly systemCap elements.
        val budget = ObserveBudget(
            appCap = ElementFilter.MAX_ELEMENTS_DEFAULT,
            systemCap = ElementFilter.SYSTEM_WINDOW_RESERVE,
        )
        // Dense feed fills the entire application budget.
        repeat(ElementFilter.MAX_ELEMENTS_DEFAULT + 50) { budget.take(isApplication = true) }

        var systemEmitted = 0
        while (budget.take(isApplication = false)) systemEmitted++
        assertEquals(ElementFilter.SYSTEM_WINDOW_RESERVE, systemEmitted)
        assertTrue("nav tabs must survive a large app window", systemEmitted > 0)
    }

    @Test
    fun buckets_are_independent() {
        val budget = ObserveBudget(appCap = 1, systemCap = 1)
        assertTrue(budget.take(isApplication = true))
        // App bucket full; system bucket untouched.
        assertTrue(budget.isFull(isApplication = true))
        assertFalse(budget.isFull(isApplication = false))
        assertTrue(budget.take(isApplication = false))
        assertTrue(budget.isFull(isApplication = false))
    }
}

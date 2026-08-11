package com.aster.service.input

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The router's fall-through rule.
 *
 * The asymmetry is the entire safety property: a [BackendOutcome.Declined]
 * backend never touched the device, so the press may be offered to the next one;
 * a [BackendOutcome.Failed] backend reached for it and may have partially landed,
 * so the chain must STOP and report its reason. Getting this backwards would
 * either double-deliver a key press or bury a real diagnosis under a vague
 * "nothing could do this".
 *
 * The other half is that a decline is not a success. `press_key` previously
 * reported through a bare Boolean, and a "nothing here can do that" was
 * indistinguishable from "done" at every layer above.
 */
class InputRouterTest {

    /**
     * A backend that answers exactly as told and counts its calls, so
     * "the chain stopped" can be asserted as *not called* rather than inferred
     * from the result.
     */
    private class StubBackend(
        private val available: Boolean,
        private val outcome: BackendOutcome,
    ) : InputBackend {
        var calls = 0
            private set

        override val kind = InputBackendKind.ACCESSIBILITY

        override fun isAvailable(): Boolean = available

        override suspend fun pressKeyCode(keyCode: Int): BackendOutcome {
            calls++
            return outcome
        }
    }

    private fun ok(available: Boolean = true) = StubBackend(available, BackendOutcome.Ok)
    private fun declines(why: String, available: Boolean = true) =
        StubBackend(available, BackendOutcome.Declined(why))

    private fun fails(why: String) = StubBackend(true, BackendOutcome.Failed(why))

    // ---- a decline is not a success ----

    @Test
    fun `a decline is not reported as success`() = runBlocking<Unit> {
        val backend = declines("keycode 61 is not expressible")
        val result = InputRouter(listOf(backend)).pressKeyCode(61)

        assertFalse("a declined press must never read as ok", result.ok)
        // No backend owned the press, so none may be named as having served it.
        assertNull(result.backend)
        assertTrue(
            "the decline's reason must survive to the caller, got: ${result.reason}",
            result.reason?.contains("keycode 61 is not expressible") == true
        )
    }

    @Test
    fun `an exhausted chain names every backend that declined`() = runBlocking<Unit> {
        val first = declines("no editable field has focus")
        val second = declines("companion not installed")
        val result = InputRouter(listOf(first, second)).pressKeyCode(66)

        assertFalse(result.ok)
        assertTrue(result.reason?.contains("no editable field has focus") == true)
        assertTrue(result.reason?.contains("companion not installed") == true)
    }

    @Test
    fun `an empty chain fails honestly rather than silently succeeding`() = runBlocking<Unit> {
        val result = InputRouter(emptyList()).pressKeyCode(66)

        assertFalse(result.ok)
        assertNull(result.backend)
        assertEquals("no input backend is configured", result.reason)
    }

    // ---- fall-through asymmetry ----

    @Test
    fun `a decline falls through to the next backend`() = runBlocking<Unit> {
        val first = declines("cannot express this keycode")
        val second = ok()
        val result = InputRouter(listOf(first, second)).pressKeyCode(66)

        assertTrue(result.ok)
        assertEquals(InputBackendKind.ACCESSIBILITY, result.backend)
        assertNull(result.reason)
        assertEquals(1, first.calls)
        assertEquals("the declined press must be offered onward", 1, second.calls)
    }

    @Test
    fun `a failure stops the chain and is not retried by a later backend`() = runBlocking<Unit> {
        val first = fails("the focused field refused ACTION_IME_ENTER")
        val second = ok()
        val result = InputRouter(listOf(first, second)).pressKeyCode(66)

        assertFalse(result.ok)
        assertEquals(
            "a backend that owned and failed the press must be named",
            InputBackendKind.ACCESSIBILITY,
            result.backend
        )
        assertEquals("the focused field refused ACTION_IME_ENTER", result.reason)
        assertEquals(1, first.calls)
        assertEquals(
            "retrying a press a backend already attempted risks delivering it twice",
            0,
            second.calls
        )
    }

    // ---- availability ----

    @Test
    fun `an unavailable backend is skipped without being asked to press`() = runBlocking<Unit> {
        val offline = ok(available = false)
        val online = ok()
        val result = InputRouter(listOf(offline, online)).pressKeyCode(4)

        assertTrue(result.ok)
        assertEquals("an unavailable backend must not be invoked", 0, offline.calls)
        assertEquals(1, online.calls)
    }

    @Test
    fun `a chain of only unavailable backends fails and says so`() = runBlocking<Unit> {
        val offline = ok(available = false)
        val result = InputRouter(listOf(offline)).pressKeyCode(4)

        assertFalse(result.ok)
        assertNull(result.backend)
        assertTrue(
            "the caller needs to know the backend was absent, not that the key was bad",
            result.reason?.contains("not available") == true
        )
        assertEquals(0, offline.calls)
    }
}

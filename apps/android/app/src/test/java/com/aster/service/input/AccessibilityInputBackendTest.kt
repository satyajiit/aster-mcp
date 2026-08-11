package com.aster.service.input

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the accessibility backend admits it cannot do.
 *
 * This is the honesty half of the fix. The path this replaces claimed every
 * keycode in `keyNameToKeycode` and delivered none of them: it shelled out to
 * `/system/bin/input`, which needs the signature-level `INJECT_EVENTS`, so a TAB
 * and a DEL and an ENTER all failed identically — and were reported as "unknown
 * key or input keyevent failed", blaming the caller's key name.
 *
 * A decline has to NAME the keycode, because that string is the only thing an
 * agent on the other side of the wire gets to reason with.
 *
 * These run in a plain JVM, where no accessibility service is bound. That is not
 * a limitation here — it is the second case worth pinning: a backend whose
 * mechanism is absent must decline (nothing attempted, another backend may try),
 * never fail.
 */
class AccessibilityInputBackendTest {

    private val backend = AccessibilityInputBackend()

    /** KEYCODE_TAB — a real key with no honest accessibility expression. */
    private val keycodeTab = 61

    /** KEYCODE_DEL (backspace) — likewise. */
    private val keycodeDel = 67

    /** KEYCODE_BACK — one this backend does serve, via a global action. */
    private val keycodeBack = 4

    @Test
    fun `declines a keycode the accessibility API cannot express, naming it`() = runBlocking<Unit> {
        val outcome = backend.pressKeyCode(keycodeTab)

        assertTrue(
            "an inexpressible keycode must decline, not fail — nothing was attempted. Got: $outcome",
            outcome is BackendOutcome.Declined
        )
        val why = (outcome as BackendOutcome.Declined).why
        assertTrue("the reason must name the keycode, got: $why", why.contains("$keycodeTab"))
    }

    @Test
    fun `the decline explains what this backend does reach`() = runBlocking<Unit> {
        val outcome = backend.pressKeyCode(keycodeDel)

        val why = (outcome as BackendOutcome.Declined).why
        assertTrue("the reason must name the keycode, got: $why", why.contains("$keycodeDel"))
        // Naming the served set is what stops a caller retrying key names forever
        // against a wall that was never about the name.
        assertTrue(why.contains("BACK"))
        assertTrue(why.contains("ENTER"))
        assertTrue(why.contains("PASTE"))
    }

    @Test
    fun `an unbound accessibility service declines rather than failing`() = runBlocking<Unit> {
        // No AccessibilityService exists in a JVM unit test, so getInstance() is
        // null. BACK is a keycode this backend genuinely serves, so reaching this
        // decline proves the classification ran BEFORE the service lookup — which
        // is what keeps the two reasons ("bad key" vs "service off") distinct.
        val outcome = backend.pressKeyCode(keycodeBack)

        assertTrue(
            "an absent mechanism must decline so another backend may try. Got: $outcome",
            outcome is BackendOutcome.Declined
        )
        val why = (outcome as BackendOutcome.Declined).why
        assertTrue("got: $why", why.contains("accessibility service is not connected"))
    }

    @Test
    fun `isAvailable is false when no accessibility service is bound`() {
        assertTrue(!backend.isAvailable())
    }

    @Test
    fun `reports itself as the accessibility backend`() {
        assertTrue(backend.kind == InputBackendKind.ACCESSIBILITY)
    }
}

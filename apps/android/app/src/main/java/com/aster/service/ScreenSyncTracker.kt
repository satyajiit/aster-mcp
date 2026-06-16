package com.aster.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe quiescence + change tracker for the accessibility engine (SPEC §3.3).
 *
 * Records timestamps of TYPE_WINDOW_CONTENT_CHANGED / TYPE_WINDOW_STATE_CHANGED events
 * (both routed through [recordChange]) so the service can compute "the screen has been
 * quiet for quietMs" without a fixed sleep. Also exposes a monotonic [revision] counter
 * used to derive the verify-after-act `changed` signal, and a [changes] SharedFlow that
 * nudges event-driven waits.
 *
 * INTENTIONALLY free of android.os.Handler / Looper / AccessibilityNodeInfo so the
 * timestamp math is JVM-unit-testable without Robolectric. The looper-bound quiescence
 * timer lives in AsterAccessibilityService, which reads these primitives.
 *
 * @param baselineNow the construction-time "last change" baseline. Production passes
 *   System.currentTimeMillis(); tests pass a fixed value. Before any real change is
 *   recorded, idle math is measured from this baseline.
 */
class ScreenSyncTracker(baselineNow: Long = System.currentTimeMillis()) {

    /** Wall-clock millis of the most recent recorded change. */
    private val lastChangeAtMs = AtomicLong(baselineNow)

    /** Monotonic count of recorded changes; used for pre/post-act `changed` derivation. */
    private val revisionCounter = AtomicLong(0L)

    // replay=0: only nudge live collectors. extraBufferCapacity=1 + DROP_OLDEST keeps
    // tryEmit() non-suspending and lossless-enough (a coalesced extra emission is fine —
    // the consumer re-evaluates the whole tree on each nudge).
    private val _changes = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    /** Emits Unit on every recorded change; consumed on the main handler by event-nudged waits. */
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    /**
     * Record a screen change. Called from onAccessibilityEvent for
     * TYPE_WINDOW_CONTENT_CHANGED and TYPE_WINDOW_STATE_CHANGED. Allocation-free on the
     * hot path apart from the SharedFlow nudge (tryEmit, non-suspending).
     */
    fun recordChange(now: Long) {
        lastChangeAtMs.set(now)
        revisionCounter.incrementAndGet()
        _changes.tryEmit(Unit)
    }

    /** Wall-clock millis of the most recent change (or the construction baseline). */
    fun lastChangeAt(): Long = lastChangeAtMs.get()

    /** Monotonic revision; compare pre/post-act to derive `changed`. */
    fun revision(): Long = revisionCounter.get()

    /**
     * Millis of quiet still required before the screen is considered idle, given [now].
     * Clamped to >= 0. Zero means "idle now".
     */
    fun remainingQuietMs(now: Long, quietMs: Long): Long {
        val elapsed = now - lastChangeAtMs.get()
        val remaining = quietMs - elapsed
        return if (remaining < 0L) 0L else remaining
    }

    /** True when no change has been recorded within the last [quietMs] as of [now]. */
    fun isIdle(now: Long, quietMs: Long): Boolean = remainingQuietMs(now, quietMs) == 0L
}

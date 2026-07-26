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

    private companion object {
        /**
         * Cap on distinct surfaces tracked. A device has few live windows, but a
         * long session that cycles through many apps would grow the map without
         * bound; on overflow it is cleared wholesale, which costs one action's
         * `changed` accuracy and nothing else.
         */
        const val MAX_TRACKED_SURFACES = 64
    }

    /** Wall-clock millis of the most recent recorded change. */
    private val lastChangeAtMs = AtomicLong(baselineNow)

    /** Monotonic count of ALL recorded changes — the quiescence signal. */
    private val revisionCounter = AtomicLong(0L)

    /**
     * Per-surface revisions, keyed `"<package>#<windowId>"`.
     *
     * The global counter cannot answer "did MY tap do anything?". A screen with a
     * live map, an autoplay video or a ticking clock bumps it many times a second,
     * so `changed` came back true no matter what — a tap that hit nothing looked
     * exactly like one that worked, and the engine's strongest post-condition
     * quietly always passed. Dimensioning by surface means an unrelated animation
     * in another window can no longer vouch for an action.
     *
     * Bounded: a device only has so many windows, but a long session cycling
     * through many apps would otherwise grow this without limit.
     */
    private val perSurface = java.util.concurrent.ConcurrentHashMap<String, AtomicLong>()

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
    @JvmOverloads
    fun recordChange(now: Long, surfaceKey: String? = null) {
        lastChangeAtMs.set(now)
        revisionCounter.incrementAndGet()
        if (surfaceKey != null) {
            if (perSurface.size >= MAX_TRACKED_SURFACES) perSurface.clear()
            perSurface.computeIfAbsent(surfaceKey) { AtomicLong(0L) }.incrementAndGet()
        }
        _changes.tryEmit(Unit)
    }

    /** Build the key [recordChange] and [revision] agree on. */
    fun surfaceKey(packageName: String?, windowId: Int?): String =
        "${packageName.orEmpty()}#${windowId ?: -1}"

    /**
     * Revision for ONE surface. Compare pre/post-act to derive `changed` without
     * an unrelated animation elsewhere on the device vouching for the action.
     */
    fun revision(surfaceKey: String): Long = perSurface[surfaceKey]?.get() ?: 0L

    /** Wall-clock millis of the most recent change (or the construction baseline). */
    fun lastChangeAt(): Long = lastChangeAtMs.get()

    /**
     * Monotonic count of every change anywhere — the QUIESCENCE signal.
     *
     * Deliberately not what `changed` is derived from any more; see [perSurface].
     */
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

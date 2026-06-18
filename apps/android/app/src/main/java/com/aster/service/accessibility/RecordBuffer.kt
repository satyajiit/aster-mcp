package com.aster.service.accessibility

import kotlinx.serialization.json.JsonArray

/**
 * Bounded, thread-safe buffer of [RecordedStep]s captured by the companion
 * RECORD MODE. Producer is the a11y main thread (onAccessibilityEvent); the
 * consumer is a Binder/IO thread draining via the `automation_record_stop`
 * command — hence every access is guarded.
 *
 * OFF by default: [active] is false until [start]; [offer] is a no-op while
 * inactive, so the recorder costs nothing on the hot a11y path outside an
 * explicit start/stop window.
 *
 * Bounded at [MAX_STEPS]: once full, further steps are dropped (oldest kept —
 * a recording session is short and the head is the meaningful prefix). A
 * [droppedCount] is tracked so the UI/agent can tell the capture was clipped.
 */
class RecordBuffer {

    companion object {
        /** Hard cap on buffered steps for one recording session. */
        const val MAX_STEPS = 200
    }

    private val lock = Any()
    private val steps = ArrayList<RecordedStep>(32)
    private var active = false
    private var dropped = 0
    private var startedAtMs = 0L

    /** Begin a session: clear any prior capture and arm [offer]. */
    fun start() {
        synchronized(lock) {
            steps.clear()
            dropped = 0
            active = true
            startedAtMs = System.currentTimeMillis()
        }
    }

    /**
     * End the session, disarm [offer], and return the captured steps (snapshot).
     * The buffer is NOT cleared here so a status read right after stop still
     * reflects the last session until the next [start].
     */
    fun stop(): List<RecordedStep> {
        synchronized(lock) {
            active = false
            return steps.toList()
        }
    }

    /** Whether the recorder is currently armed. */
    fun isActive(): Boolean = synchronized(lock) { active }

    /**
     * Offer one captured step. No-op (returns false) when inactive or full.
     * Returns true only when the step was buffered.
     */
    fun offer(step: RecordedStep): Boolean {
        synchronized(lock) {
            if (!active) return false
            if (steps.size >= MAX_STEPS) {
                dropped++
                return false
            }
            steps.add(step)
            return true
        }
    }

    /** Current captured-step count (live, even mid-session). */
    fun size(): Int = synchronized(lock) { steps.size }

    /** How many steps were dropped after hitting [MAX_STEPS] this session. */
    fun droppedCount(): Int = synchronized(lock) { dropped }

    /** Epoch-ms the current/last session started (0 before the first [start]). */
    fun startedAt(): Long = synchronized(lock) { startedAtMs }

    /** Snapshot the buffered steps as a wire JSON array (each = [RecordedStep.toJson]). */
    fun toJsonArray(): JsonArray {
        val snapshot = synchronized(lock) { steps.toList() }
        return JsonArray(snapshot.map { it.toJson() })
    }
}

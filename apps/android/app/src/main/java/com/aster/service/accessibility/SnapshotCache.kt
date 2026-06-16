package com.aster.service.accessibility

import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe, bounded store of the last [MAX_SNAPSHOTS] observe snapshots:
 *   snapshot_id -> (ref -> NodeDescriptor).
 *
 * NEVER stores live AccessibilityNodeInfo — only the [NodeDescriptor] POJO.
 * Evicting an old snapshot makes its refs return null from [get], which is the
 * `stale_ref` precondition P2 enforces (a ref into an evicted snapshot must
 * fail closed → re-observe, never act on a wrong node).
 */
class SnapshotCache {

    companion object {
        /** Keep the last N snapshots so a just-superseded ref still resolves briefly. */
        const val MAX_SNAPSHOTS = 3
    }

    private val seq = AtomicLong(0L)
    private val lock = Any()

    /**
     * Id of the most-recently-created snapshot, or null before the first [newSnapshot].
     * Lets ref-actions that omit `snapshot_id` resolve against the latest observation
     * (the kernel passes `snapshot_id` optionally; the companion stays robust when it is
     * absent). Guarded by [lock].
     */
    private var latestSnapshotId: String? = null

    // LinkedHashMap with removeEldestEntry → simple LRU by insertion order.
    private val snapshots = object : LinkedHashMap<String, MutableMap<String, NodeDescriptor>>(
        /* initialCapacity = */ MAX_SNAPSHOTS + 1,
        /* loadFactor = */ 0.75f,
        /* accessOrder = */ false,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, MutableMap<String, NodeDescriptor>>?,
        ): Boolean = size > MAX_SNAPSHOTS
    }

    /** Allocate a fresh, unique snapshot id and register an empty ref map. */
    fun newSnapshot(): String {
        val id = "s${seq.incrementAndGet()}-${System.currentTimeMillis()}"
        synchronized(lock) {
            snapshots[id] = HashMap()
            latestSnapshotId = id
        }
        return id
    }

    /**
     * Id of the most-recently-created snapshot that is still resident, or null if there is no
     * snapshot at all. Used as the fallback when a ref-action omits `snapshot_id` — the action
     * resolves against the latest observation. The newest snapshot is never the eldest evicted,
     * so this normally returns a live id; the membership check keeps it honest if it were ever
     * cleared.
     */
    fun latestId(): String? {
        synchronized(lock) {
            val id = latestSnapshotId ?: return null
            return if (snapshots.containsKey(id)) id else null
        }
    }

    /** Record a ref → descriptor under [snapshotId] (no-op if snapshot was evicted). */
    fun put(snapshotId: String, ref: String, descriptor: NodeDescriptor) {
        synchronized(lock) {
            snapshots[snapshotId]?.put(ref, descriptor)
        }
    }

    /** Resolve a ref to its descriptor, or null if the snapshot/ref is gone (stale). */
    fun get(snapshotId: String, ref: String): NodeDescriptor? {
        synchronized(lock) {
            return snapshots[snapshotId]?.get(ref)
        }
    }
}

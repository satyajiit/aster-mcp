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
        }
        return id
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

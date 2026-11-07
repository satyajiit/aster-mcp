package com.aster.service.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class SnapshotCacheTest {

    private fun desc(ref: String) = NodeDescriptor(
        ref = ref,
        viewId = "com.x:id/$ref",
        text = "t-$ref",
        desc = "",
        role = "button",
        className = "android.widget.Button",
        bounds = DescriptorBounds.fromLTRB(0, 0, 10, 10),
        windowId = 1,
        childPath = listOf(0, 1),
    )

    @Test
    fun newSnapshot_ids_are_unique() {
        val cache = SnapshotCache()
        val a = cache.newSnapshot()
        val b = cache.newSnapshot()
        assertNotEquals(a, b)
    }

    @Test
    fun an_ephemeral_observe_leaves_the_latest_snapshot_alone() {
        // The kernel's own look before a guarded tap must never become the
        // snapshot a ref without `snapshot_id` resolves against.
        val cache = SnapshotCache()
        val modelSnapshot = cache.record(listOf(desc("e1")), ephemeral = false)!!

        // More reads than the cache holds: an ephemeral read must not evict either.
        repeat(SnapshotCache.MAX_SNAPSHOTS + 1) {
            assertNull(cache.record(listOf(desc("e1"), desc("e2")), ephemeral = true))
        }

        assertEquals(modelSnapshot, cache.latestId())
        assertEquals("com.x:id/e1", cache.get(modelSnapshot, "e1")?.viewId)
        assertNull("the ephemeral read's refs were never stored", cache.get(modelSnapshot, "e2"))

        // An ordinary observe still becomes the newest.
        val next = cache.record(listOf(desc("e2")), ephemeral = false)
        assertEquals(next, cache.latestId())
        assertNotEquals(modelSnapshot, next)
    }

    @Test
    fun put_then_get_round_trips() {
        val cache = SnapshotCache()
        val sid = cache.newSnapshot()
        cache.put(sid, "e1", desc("e1"))
        assertEquals("com.x:id/e1", cache.get(sid, "e1")?.viewId)
    }

    @Test
    fun get_missing_ref_returns_null() {
        val cache = SnapshotCache()
        val sid = cache.newSnapshot()
        cache.put(sid, "e1", desc("e1"))
        assertNull(cache.get(sid, "e99"))
    }

    @Test
    fun get_unknown_snapshot_returns_null() {
        val cache = SnapshotCache()
        assertNull(cache.get("never-created", "e1"))
    }

    @Test
    fun lru_evicts_oldest_snapshot_so_stale_ref_returns_null() {
        // MAX_SNAPSHOTS = 3. Create 4; the first must be evicted.
        val cache = SnapshotCache()
        val sids = (1..4).map { i ->
            val sid = cache.newSnapshot()
            cache.put(sid, "e1", desc("e1"))
            sid
        }
        // Oldest (sids[0]) evicted → stale_ref precondition: get returns null.
        assertNull(cache.get(sids[0], "e1"))
        // The 3 most recent survive.
        assertEquals("com.x:id/e1", cache.get(sids[1], "e1")?.viewId)
        assertEquals("com.x:id/e1", cache.get(sids[3], "e1")?.viewId)
    }

    @Test
    fun concurrent_put_get_does_not_throw() {
        val cache = SnapshotCache()
        val sid = cache.newSnapshot()
        val pool = Executors.newFixedThreadPool(8)
        val latch = CountDownLatch(200)
        repeat(200) { i ->
            pool.submit {
                try {
                    cache.put(sid, "e$i", desc("e$i"))
                    cache.get(sid, "e$i")
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        pool.shutdown()
        // No exception thrown == pass.
    }
}

package com.aster.service.execution

import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors

/** Real SQLite state/transactions; injected cipher only avoids JVM AndroidKeyStore. Unrun. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExecutionJournalTest {
    @get:Rule val folder = TemporaryFolder()
    private val opened = mutableListOf<ExecutionJournal>()
    private val cipher = object : ExecutionResultCipher {
        override fun encrypt(bytes: ByteArray) = bytes.map { (it.toInt() xor 0x5a).toByte() }.toByteArray()
        override fun decrypt(bytes: ByteArray) = encrypt(bytes)
    }
    private fun open(file: File = File(folder.root, "journal.sqlite"), boot: String = "boot-one") =
        ExecutionJournal(file, cipher, boot) { }.also { opened.add(it) }
    private fun reopen(journal: ExecutionJournal): ExecutionJournal {
        journal.closeForTest(); opened.remove(journal)
        return open(boot = "boot-two")
    }
    private fun query(journal: ExecutionJournal, suffix: String = "V") = ExecutionQuery(
        ExecutionIdentity(2, "01ARZ3NDEKTSV4RRFFQ69G5FA$suffix", "01ARZ3NDEKTSV4RRFFQ69G5FB0", null,
            ExecutionProtocol.sha256("private exact input"), 123),
        ExecutionTarget("phone", "original-adapter", journal.companionId, journal.bootId),
    )
    @After fun close() { opened.forEach { it.closeForTest() } }

    @Test fun sealMissingIsDurableAndLateSubmitNeverDispatches() {
        val store = open(); val q = query(store)
        assertEquals("uncertain", store.observe(q, "ipc:12").state)
        store.seal(q, "ipc:12")
        assertFalse(store.prepare(q, "ipc:12", true))
        assertFalse(store.begin(q, "ipc:12"))
        val proof = store.observe(q, "ipc:12")
        assertEquals("no_dispatch", proof.state)
        assertNotNull(proof.closed_at_ms)
        assertNull(proof.result_hash)
        assertEquals(proof.closed_at_ms, reopen(store).observe(q, "ipc:12").closed_at_ms)
    }

    @Test fun duplicateAndChangedIdentityCannotReplaceOriginalReceipt() {
        val store = open(); val q = query(store)
        assertTrue(store.prepare(q, "ipc:12", true)); assertTrue(store.begin(q, "ipc:12"))
        val bytes = "{\"success\":true,\"data\":{\"text\":\"private-screen\"},\"error\":null}"
        store.finish(q, "ipc:12", bytes)
        assertFalse(store.prepare(q, "ipc:12", true))
        val changed = q.copy(request = q.request.copy(payload_hash = "0".repeat(64)))
        assertTrue(runCatching { store.prepare(changed, "ipc:12", true) }.isFailure)
        assertTrue(runCatching { store.observe(q, "ws:other") }.isFailure)
        val proof = reopen(store).observe(q, "ipc:12")
        assertEquals("closed", proof.state); assertEquals(q.target, proof.target)
        assertEquals(bytes, proof.result_json); assertEquals(ExecutionProtocol.sha256(bytes), proof.result_hash)
        assertTrue(proof.result_available)
        assertFalse(File(folder.root, "journal.sqlite").readBytes().toString(Charsets.ISO_8859_1).contains("private-screen"))
    }

    @Test fun runningRestartStaysUncertainWhilePreparedCanOnlyBeSealed() {
        var store = open(); val running = query(store)
        store.prepare(running, "ipc:12", true); store.begin(running, "ipc:12")
        store = reopen(store)
        assertEquals("uncertain", store.observe(running, "ipc:12").state)
        store.seal(running, "ipc:12")
        assertEquals("uncertain", store.observe(running, "ipc:12").state)
        assertFalse(store.prepare(query(store, "W"), "ipc:12", true))
        val other = open(File(folder.root, "prepared.sqlite")); val prepared = query(other)
        other.prepare(prepared, "ipc:12", true); other.closeForTest(); opened.remove(other)
        val fresh = open(File(folder.root, "prepared.sqlite"), "boot-two")
        assertEquals("accepted", fresh.observe(prepared, "ipc:12").state)
        assertFalse(fresh.begin(prepared, "ipc:12"))
        fresh.seal(prepared, "ipc:12")
        assertEquals("no_dispatch", fresh.observe(prepared, "ipc:12").state)
    }

    @Test fun preparedSealWinsBeforeWorkerAndForeignBootCannotStart() {
        val store = open(); val q = query(store)
        assertTrue(store.prepare(q, "ipc:12", true)); store.seal(q, "ipc:12")
        assertFalse(store.begin(q, "ipc:12"))
        val oldBoot = query(store, "W").copy(target = q.target.copy(boot_id = "old-boot"))
        assertFalse(store.prepare(oldBoot, "ipc:12", true))
        assertEquals("no_dispatch", store.observe(oldBoot, "ipc:12").state)
        assertTrue(runCatching { store.seal(query(store, "X").copy(target = q.target.copy(companion_id = "foreign")), "ipc:12") }.isFailure)
    }

    @Test fun concurrentDifferentRequestsHaveOnePhysicalAdmission() {
        val store = open(); val pool = Executors.newFixedThreadPool(2); val barrier = CyclicBarrier(2)
        try {
            val futures = listOf("V", "W").map { suffix -> pool.submit<Boolean> {
                barrier.await(); store.prepare(query(store, suffix), "ipc:12", true)
            } }
            assertEquals(1, futures.count { it.get() })
            assertTrue(store.unresolved())
        } finally { pool.shutdownNow() }
    }

    @Test fun unavailableResultRetainsExactHashAndTerminalProofAcrossReopen() {
        val store = open(); val q = query(store)
        store.prepare(q, "ipc:12", true); store.begin(q, "ipc:12")
        val large = "{\"success\":true,\"data\":\"" + "x".repeat(ExecutionProtocol.MAX_RESULT_BYTES) + "\",\"error\":null}"
        store.finish(q, "ipc:12", large)
        val proof = reopen(store).observe(q, "ipc:12")
        assertEquals("closed", proof.state); assertEquals(ExecutionProtocol.sha256(large), proof.result_hash)
        assertFalse(proof.result_available); assertNull(proof.result_json)
    }

    @Test fun legacyCrashBlocksNewTrackedAndLegacyWorkWithoutInventingClosure() {
        val store = open(); val legacy = store.beginLegacy()!!
        assertNull(store.beginLegacy())
        val reopened = reopen(store)
        reopened.finishLegacy(legacy) // A different boot cannot close the old local flight.
        assertTrue(reopened.unresolved()); assertNull(reopened.beginLegacy())
        assertFalse(reopened.prepare(query(reopened), "ipc:12", true))
    }

    @Test fun storageStabilizationFailureReturnsNoUsableStoreAndPreservesOriginal() {
        val store = open(); val q = query(store); store.seal(q, "ipc:12")
        store.closeForTest(); opened.remove(store)
        assertTrue(runCatching { ExecutionJournal(File(folder.root, "journal.sqlite"), cipher, "boot-two") {
            throw java.io.IOException("injected directory sync failure")
        } }.isFailure)
        assertEquals("no_dispatch", open(boot = "boot-three").observe(q, "ipc:12").state)
    }

    @Test fun encryptionFailureRefusesBeforePreparedOrPhysicalAdmission() {
        val broken = object : ExecutionResultCipher {
            override fun encrypt(bytes: ByteArray): ByteArray = throw java.io.IOException("unavailable key")
            override fun decrypt(bytes: ByteArray): ByteArray = error("unused")
        }
        val store = ExecutionJournal(File(folder.root, "journal.sqlite"), broken, "boot-one") { }.also { opened.add(it) }
        val q = query(store)
        assertTrue(runCatching { store.prepare(q, "ipc:12", true) }.isFailure)
        assertFalse(store.unresolved()); assertEquals("uncertain", store.observe(q, "ipc:12").state)
    }

    @Test fun terminalTombstonesDoNotExhaustFutureAdmissionOrLoseOriginalProof() {
        val store = open(); val original = query(store)
        store.seal(original, "ipc:12")
        val first = store.observe(original, "ipc:12")
        for (index in 1..2050) {
            store.seal(original.copy(request = original.request.copy(request_id = index.toString().padStart(26, '0'))), "ipc:12")
        }
        assertTrue(store.prepare(query(store, "W"), "ipc:12", true))
        assertEquals(first.closed_at_ms, store.observe(original, "ipc:12").closed_at_ms)
        assertFalse(store.prepare(original, "ipc:12", true))
    }

    @Test fun privateResultCompactionRetainsHistoricalClosureAndHash() {
        // Small injected byte budget exercises the same real SQLite compaction
        // transaction without allocating the production 64 MiB in a fixture.
        val store = ExecutionJournal(File(folder.root, "journal.sqlite"), cipher, "boot-one", 100) { }
            .also { opened.add(it) }
        val first = query(store); val second = query(store, "W")
        val result = "{\"success\":true,\"data\":\"" + "x".repeat(60) + "\",\"error\":null}"
        store.prepare(first, "ipc:12", true); store.begin(first, "ipc:12"); store.finish(first, "ipc:12", result)
        store.prepare(second, "ipc:12", true); store.begin(second, "ipc:12"); store.finish(second, "ipc:12", result)
        val proof = reopen(store).observe(first, "ipc:12")
        assertEquals("closed", proof.state); assertFalse(proof.result_available)
        assertEquals(ExecutionProtocol.sha256(result), proof.result_hash)
        assertTrue(runCatching { opened.last().observe(first, "ws:other") }.isFailure)
    }

    @Test fun postCompletionEncryptionFailureRetainsExactClosureWithoutInventedResult() {
        var ready = true
        val lateFailure = object : ExecutionResultCipher {
            override fun encrypt(bytes: ByteArray): ByteArray {
                check(ready) { "keystore became unavailable" }
                return cipher.encrypt(bytes)
            }
            override fun decrypt(bytes: ByteArray) = cipher.decrypt(bytes)
        }
        val store = ExecutionJournal(File(folder.root, "journal.sqlite"), lateFailure, "boot-one") { }
            .also { opened.add(it) }
        val q = query(store)
        assertTrue(store.prepare(q, "ipc:12", true)); assertTrue(store.begin(q, "ipc:12"))
        ready = false
        val result = "{\"success\":false,\"data\":null,\"error\":\"original error\"}"
        store.finish(q, "ipc:12", result)
        val proof = reopen(store).observe(q, "ipc:12")
        assertEquals("closed", proof.state); assertFalse(proof.result_available)
        assertEquals(ExecutionProtocol.sha256(result), proof.result_hash)
        assertNull(proof.result_json); assertFalse(opened.last().unresolved())
    }
}

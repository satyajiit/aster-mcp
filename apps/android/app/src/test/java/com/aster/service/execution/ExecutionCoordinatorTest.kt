package com.aster.service.execution

import com.aster.service.CommandResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

/** Actual owned coordinator + SQLite, with controlled child callbacks. Unrun. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExecutionCoordinatorTest {
    @get:Rule val folder = TemporaryFolder()
    private var journal: ExecutionJournal? = null
    private fun setup(): Pair<ExecutionJournal, ExecutionCoordinator> {
        val cipher = object : ExecutionResultCipher {
            override fun encrypt(bytes: ByteArray) = bytes.copyOf()
            override fun decrypt(bytes: ByteArray) = bytes.copyOf()
        }
        val store = ExecutionJournal(File(folder.root, "journal.sqlite"), cipher, "boot-one") { }
        journal = store
        return store to ExecutionCoordinator(store)
    }
    @After fun close() { journal?.closeForTest() }
    private fun submit(store: ExecutionJournal, id: String = "V", action: String = "tap"): ExecutionSubmit {
        val payload = "{\"action\":\"$action\",\"params\":{}}"
        return ExecutionSubmit(ExecutionRequest(2, "01ARZ3NDEKTSV4RRFFQ69G5FA$id", "01ARZ3NDEKTSV4RRFFQ69G5FB0",
            null, payload, ExecutionProtocol.sha256(payload), 123),
            ExecutionTarget("phone", "original-adapter", store.companionId, store.bootId))
    }
    private fun query(request: ExecutionSubmit) = ExecutionProtocol.json.encodeToString(ExecutionQuery(request.request.identity(), request.target))
    private fun state(coordinator: ExecutionCoordinator, request: ExecutionSubmit): String =
        ExecutionProtocol.json.decodeFromString<ExecutionObservation>(coordinator.status(query(request), "ipc:12")).state
    private suspend fun waitState(coordinator: ExecutionCoordinator, request: ExecutionSubmit, expected: String) {
        withTimeout(5_000) { while (state(coordinator, request) != expected) delay(5) }
    }

    @Test fun trackedHandlerReturnDoesNotCloseUntilEveryChildActuallyCompletes() = runBlocking {
        val (store, coordinator) = setup(); val request = submit(store)
        val callback = CompletableDeferred<ExecutionChildren.Child>(); val dispatched = AtomicInteger()
        val response = async(Dispatchers.IO) {
            coordinator.submit(ExecutionProtocol.json.encodeToString(request), "ipc:12", { true }) {
                dispatched.incrementAndGet()
                callback.complete(coroutineContext[ExecutionChildren]!!.begin())
                CommandResult.success(null)
            }
        }
        val child = withTimeout(5_000) { callback.await() }
        assertEquals("running", state(coordinator, request))
        assertFalse(response.isCompleted)
        assertFalse(coordinator.legacy("execute_shell") { error("must not start") }.success)
        val duplicate = withTimeout(5_000) {
            coordinator.submit(ExecutionProtocol.json.encodeToString(request), "ipc:12", { true }) { error("duplicate dispatch") }
        }
        assertEquals("running", ExecutionProtocol.json.decodeFromString<ExecutionObservation>(duplicate).state)
        val sealed = coordinator.sealIfUnstarted(query(request), "ipc:12")
        assertEquals("running", ExecutionProtocol.json.decodeFromString<ExecutionObservation>(sealed).state)
        assertEquals("running", state(coordinator, request)); assertEquals(1, dispatched.get())
        child.complete(); child.complete() // Duplicate callback must not underflow/release another owner.
        val delivered = ExecutionProtocol.json.decodeFromString<ExecutionObservation>(withTimeout(5_000) { response.await() })
        assertEquals("closed", delivered.state)
        assertEquals(request.target, delivered.target)
        assertEquals(request.request.request_id, delivered.request_id)
        assertTrue(delivered.result_available)
        assertEquals(ExecutionProtocol.sha256(requireNotNull(delivered.result_json)), delivered.result_hash)
        assertEquals(delivered.closed_at_ms, ExecutionProtocol.json.decodeFromString<ExecutionObservation>(coordinator.status(query(request), "ipc:12")).closed_at_ms)
        assertTrue(coordinator.legacy("tap") { CommandResult.success(null) }.success)
    }

    @Test fun cancelledSubmitWaiterDoesNotCancelWorkerAndOriginalStatusDeliversClosure() = runBlocking {
        val (store, coordinator) = setup(); val request = submit(store)
        val callback = CompletableDeferred<ExecutionChildren.Child>(); val dispatched = AtomicInteger()
        val waiter = async(Dispatchers.IO) {
            coordinator.submit(ExecutionProtocol.json.encodeToString(request), "ipc:12", { true }) {
                dispatched.incrementAndGet()
                callback.complete(coroutineContext[ExecutionChildren]!!.begin())
                CommandResult.success(null)
            }
        }
        val child = withTimeout(5_000) { callback.await() }
        waiter.cancelAndJoin()
        assertEquals("running", state(coordinator, request))
        assertFalse(coordinator.legacy("tap") { error("original still open") }.success)
        coordinator.sealIfUnstarted(query(request), "ipc:12")
        assertEquals("running", state(coordinator, request))
        child.complete()
        waitState(coordinator, request, "closed")
        val original = coordinator.submit(ExecutionProtocol.json.encodeToString(request), "ipc:12", { true }) { error("must not replay") }
        val observation = ExecutionProtocol.json.decodeFromString<ExecutionObservation>(original)
        assertEquals("closed", observation.state)
        assertTrue(observation.result_available)
        assertEquals(1, dispatched.get())
    }

    @Test fun cancelledLegacyWaiterRetainsPhysicalAdmissionAndGestureChild() = runBlocking {
        val (_, coordinator) = setup()
        val callback = CompletableDeferred<ExecutionChildren.Child>()
        val waiter = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
            coordinator.legacy("input_gesture") {
                callback.complete(coroutineContext[ExecutionChildren]!!.begin())
                CommandResult.success(null)
            }
        }
        val child = withTimeout(5_000) { callback.await() }
        waiter.cancelAndJoin()
        assertFalse(coordinator.legacy("launch_intent") { error("late overlap") }.success)
        assertTrue(coordinator.legacy("observe") { CommandResult.success(null) }.success)
        child.complete()
        withTimeout(5_000) { while (journal!!.unresolved()) delay(5) }
        assertTrue(coordinator.legacy("tap") { CommandResult.success(null) }.success)
    }

    @Test fun revokedAdmissionBeforeActualWorkerProducesDurableNoDispatch() = runBlocking {
        val (store, coordinator) = setup(); val request = submit(store); val calls = AtomicInteger()
        coordinator.submit(ExecutionProtocol.json.encodeToString(request), "ipc:12", { calls.incrementAndGet() == 1 }) {
            error("revoked request must not execute")
        }
        waitState(coordinator, request, "no_dispatch")
        coordinator.submit(ExecutionProtocol.json.encodeToString(request), "ipc:12", { true }) { error("sealed request replay") }
        assertEquals("no_dispatch", state(coordinator, request))
    }

    @Test fun unsupportedTrackedActionClosesWithoutCallingLegacyFallback() = runBlocking {
        val (store, coordinator) = setup(); val request = submit(store, action = "screen_approve")
        coordinator.submit(ExecutionProtocol.json.encodeToString(request), "ipc:12", { true }) { error("unsupported effect") }
        assertEquals("no_dispatch", state(coordinator, request))
        assertFalse(store.unresolved())
    }
}

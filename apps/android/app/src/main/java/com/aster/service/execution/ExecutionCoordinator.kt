package com.aster.service.execution

import android.content.Context
import com.aster.data.model.Command
import com.aster.service.CommandResult
import com.aster.service.safety.PackagePolicyGuard
import com.aster.service.wire.WireParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** One physical control admission shared by Binder, remote WS and local MCP. */
class ExecutionCoordinator internal constructor(private val journal: ExecutionJournal) {
    constructor(context: Context) : this(ExecutionJournal(context))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class Owner(val coordinator: ExecutionCoordinator) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Owner>
    }

    private fun isControl(action: String) = action in PackagePolicyGuard.GATED_ACTIONS ||
        action in ExecutionProtocol.supportedActions || action in setOf(
            // Alternate legacy entrances that can move/control the foreground,
            // including shell `input`. They cannot bypass a tracked flight.
            "execute_shell", "make_call", "make_call_with_voice", "show_toast",
            "take_photo", "record_video", "set_alarm", "dismiss_alarm", "delete_alarm",
            "show_overlay", "hide_overlay", "hide_all_overlays",
        )

    fun capabilities(): String = ExecutionProtocol.json.encodeToString(ExecutionCapabilities(
        companion_id = journal.companionId, boot_id = journal.bootId, unresolved = journal.unresolved(),
    ))

    fun status(queryJson: String, owner: String): String {
        val query = ExecutionProtocol.decodeQuery(queryJson)
        return ExecutionProtocol.encode(journal.observe(query, owner))
    }

    fun sealIfUnstarted(queryJson: String, owner: String): String {
        val query = ExecutionProtocol.decodeQuery(queryJson)
        journal.seal(query, owner)
        return ExecutionProtocol.encode(journal.observe(query, owner))
    }

    suspend fun submit(
        requestJson: String,
        owner: String,
        canDispatch: () -> Boolean,
        dispatch: suspend (Command) -> CommandResult,
    ): String {
        val (query, original) = ExecutionProtocol.decodeSubmit(requestJson)
        val supported = ExecutionProtocol.supported(original)
        if (journal.prepare(query, owner, supported && canDispatch())) {
            // Independent of Binder return, WS collection, mode stop, and the
            // caller's coroutine. Original identity is durable before launch.
            val children = ExecutionChildren()
            scope.async(Owner(this) + children) {
                if (!canDispatch()) {
                    journal.seal(query, owner)
                    return@async
                }
                if (!journal.begin(query, owner)) return@async
                val result = try {
                    dispatch(original.copy(params = WireParams.normalize(original.params)))
                } catch (_: Exception) {
                    CommandResult.failure("The companion command ended with an error; its external effect is not inferred.")
                }
                // The handler returning/throwing is insufficient while Android
                // owns an outstanding gesture. Cancellation never bypasses it.
                withContext(NonCancellable) { children.awaitClosed() }
                try {
                    journal.finish(query, owner, ExecutionProtocol.json.encodeToString(
                        ExecutionResult(result.success, result.data, result.error),
                    ))
                } catch (_: Exception) {
                    // A failed terminal write cannot publish closure. Preserve
                    // the durable active/uncertain fence and require recovery.
                    runCatching { journal.uncertain(query, owner) }
                }
            }.await()
        }
        // The first submit delivers the original terminal observation after
        // every physical child and the durable finish. Losing this waiter does
        // not cancel scope's worker. A duplicate only reads the existing row;
        // it never starts or takes ownership of another execution.
        return ExecutionProtocol.encode(journal.observe(query, owner))
    }

    suspend fun legacy(action: String, block: suspend () -> CommandResult): CommandResult {
        if (!isControl(action) || coroutineContext[Owner]?.coordinator === this) return block()
        val id = journal.beginLegacy()
            ?: return CommandResult.failure("An original device execution is still unresolved. Inspect it before another control action.")
        val children = ExecutionChildren()
        // All three legacy transports traverse GuardedCommandHandler. An
        // aborted collector cannot detach a gesture and release this admission.
        return scope.async(Owner(this) + children) {
            val result = try { block() } catch (_: Exception) {
                CommandResult.failure("The companion command ended with an error.")
            }
            withContext(NonCancellable) { children.awaitClosed() }
            journal.finishLegacy(id)
            result
        }.await()
    }
}

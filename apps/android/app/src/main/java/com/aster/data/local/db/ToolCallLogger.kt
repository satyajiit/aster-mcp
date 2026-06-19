package com.aster.data.local.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ToolEvent {
    data class Started(
        val toolName: String,
        val connectionType: String,
        // Screen Control /goal P7 — overlay/audit context.
        val target: String? = null,
        val risk: String? = null,
        // App Automations /goal I4 — the EA's display name for the overlay footer
        // (null when the kernel didn't stamp one → overlay falls back to "Aster").
        val aiName: String? = null
    ) : ToolEvent
    data class Completed(
        val toolName: String,
        val connectionType: String,
        val success: Boolean,
        val durationMs: Long,
        // Screen Control /goal P7 — overlay/audit context.
        val target: String? = null,
        val resolvedBy: String? = null,
        val risk: String? = null,
        val approval: String? = null
    ) : ToolEvent
}

@Singleton
class ToolCallLogger @Inject constructor(
    private val dao: ToolCallLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _toolEvents = MutableSharedFlow<ToolEvent>(replay = 0, extraBufferCapacity = 16)
    val toolEvents: SharedFlow<ToolEvent> = _toolEvents.asSharedFlow()

    fun onToolStarted(
        action: String,
        connectionType: String,
        target: String? = null,
        risk: String? = null,
        aiName: String? = null
    ) {
        _toolEvents.tryEmit(ToolEvent.Started(action, connectionType, target, risk, aiName))
    }

    fun log(
        action: String,
        connectionType: String,
        success: Boolean,
        durationMs: Long = 0,
        errorMessage: String? = null,
        // Screen Control /goal P7 audit fields.
        target: String? = null,
        resolvedBy: String? = null,
        risk: String? = null,
        approval: String? = null
    ) {
        _toolEvents.tryEmit(
            ToolEvent.Completed(action, connectionType, success, durationMs, target, resolvedBy, risk, approval)
        )
        scope.launch {
            dao.insert(
                ToolCallLog(
                    action = action,
                    connectionType = connectionType,
                    success = success,
                    durationMs = durationMs,
                    errorMessage = errorMessage,
                    target = target,
                    resolvedBy = resolvedBy,
                    risk = risk,
                    approval = approval
                )
            )
        }
    }
}

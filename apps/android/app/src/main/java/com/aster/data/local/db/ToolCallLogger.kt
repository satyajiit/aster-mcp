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
    data class Started(val toolName: String, val connectionType: String) : ToolEvent
    data class Completed(
        val toolName: String,
        val connectionType: String,
        val success: Boolean,
        val durationMs: Long
    ) : ToolEvent
}

@Singleton
class ToolCallLogger @Inject constructor(
    private val dao: ToolCallLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _toolEvents = MutableSharedFlow<ToolEvent>(replay = 0, extraBufferCapacity = 16)
    val toolEvents: SharedFlow<ToolEvent> = _toolEvents.asSharedFlow()

    fun onToolStarted(action: String, connectionType: String) {
        _toolEvents.tryEmit(ToolEvent.Started(action, connectionType))
    }

    fun log(
        action: String,
        connectionType: String,
        success: Boolean,
        durationMs: Long = 0,
        errorMessage: String? = null
    ) {
        _toolEvents.tryEmit(
            ToolEvent.Completed(action, connectionType, success, durationMs)
        )
        scope.launch {
            dao.insert(
                ToolCallLog(
                    action = action,
                    connectionType = connectionType,
                    success = success,
                    durationMs = durationMs,
                    errorMessage = errorMessage
                )
            )
        }
    }
}

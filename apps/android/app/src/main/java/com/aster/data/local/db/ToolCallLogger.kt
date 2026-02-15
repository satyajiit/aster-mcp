package com.aster.data.local.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolCallLogger @Inject constructor(
    private val dao: ToolCallLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun log(
        action: String,
        connectionType: String,
        success: Boolean,
        durationMs: Long = 0,
        errorMessage: String? = null
    ) {
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

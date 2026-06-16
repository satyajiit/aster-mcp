package com.aster.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_call_logs")
data class ToolCallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val connectionType: String,  // "IPC", "LOCAL_MCP", "REMOTE_WS"
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val durationMs: Long = 0,
    val errorMessage: String? = null,
    // Screen Control /goal P7 audit fields.
    val target: String? = null,
    val resolvedBy: String? = null,
    val risk: String? = null,       // "low" | "high"
    val approval: String? = null    // "approved" | "denied" | null (n/a)
)

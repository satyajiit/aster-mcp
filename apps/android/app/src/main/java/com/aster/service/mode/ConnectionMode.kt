package com.aster.service.mode

import kotlinx.coroutines.flow.StateFlow

/**
 * Unified interface for the foreground service to manage any connection mode.
 */
interface ConnectionMode {
    val modeType: ModeType
    val displayName: String
    val statusFlow: StateFlow<ModeStatus>

    suspend fun start(config: ModeConfig)
    suspend fun stop()
    fun getAvailableTools(): List<ToolInfo>
}

enum class ModeType {
    IPC,
    LOCAL_MCP,
    REMOTE_WS;

    companion object {
        fun fromString(value: String): ModeType = when (value.uppercase()) {
            "IPC" -> IPC
            "LOCAL_MCP" -> LOCAL_MCP
            "REMOTE_WS" -> REMOTE_WS
            else -> REMOTE_WS
        }
    }
}

enum class ModeState {
    IDLE,
    STARTING,
    RUNNING,
    ERROR,
    STOPPING
}

data class ModeStatus(
    val state: ModeState = ModeState.IDLE,
    val message: String = "",
    val connectedClients: Int = 0
)

sealed class ModeConfig {
    data class IpcConfig(val token: String) : ModeConfig()
    data class McpConfig(val port: Int = 8080) : ModeConfig()
    data class RemoteConfig(val serverUrl: String) : ModeConfig()
}

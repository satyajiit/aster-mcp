package com.aster.service.mode

import android.util.Log
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.model.Command
import com.aster.data.model.ConnectionState
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.CommandHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Wraps existing AsterWebSocketClient in the ConnectionMode interface.
 */
class RemoteWsMode(
    private val webSocketClient: AsterWebSocketClient,
    private val commandHandlers: Map<String, CommandHandler>,
    private val toolCallLogger: ToolCallLogger
) : ConnectionMode {

    companion object {
        private const val TAG = "RemoteWsMode"
    }

    override val modeType = ModeType.REMOTE_WS
    override val displayName = "Remote WebSocket"

    private val _statusFlow = MutableStateFlow(ModeStatus())
    override val statusFlow: StateFlow<ModeStatus> = _statusFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var observeJob: Job? = null
    private var commandJob: Job? = null

    override suspend fun start(config: ModeConfig) {
        val remoteConfig = config as? ModeConfig.RemoteConfig
            ?: throw IllegalArgumentException("RemoteWsMode requires RemoteConfig")

        _statusFlow.value = ModeStatus(
            state = ModeState.STARTING,
            message = "Connecting to ${remoteConfig.serverUrl}..."
        )

        // Observe connection state and map to ModeStatus
        observeJob = scope.launch {
            webSocketClient.connectionState.collect { state ->
                _statusFlow.value = mapConnectionState(state, remoteConfig.serverUrl)
            }
        }

        // Observe incoming commands and handle them
        commandJob = scope.launch {
            webSocketClient.incomingCommands.collect { command ->
                handleCommand(command)
            }
        }

        webSocketClient.connect(remoteConfig.serverUrl)
    }

    override suspend fun stop() {
        _statusFlow.value = ModeStatus(state = ModeState.STOPPING, message = "Disconnecting...")
        observeJob?.cancel()
        commandJob?.cancel()
        webSocketClient.disconnect()
        _statusFlow.value = ModeStatus(state = ModeState.IDLE)
        Log.i(TAG, "Remote WS mode stopped")
    }

    override fun getAvailableTools(): List<ToolInfo> {
        return ToolCatalog.resolve(commandHandlers.keys)
    }

    private suspend fun handleCommand(command: Command) {
        val handler = commandHandlers[command.action]

        if (handler == null) {
            toolCallLogger.log(
                action = command.action,
                connectionType = "REMOTE_WS",
                success = false,
                errorMessage = "Unknown action: ${command.action}"
            )
            webSocketClient.sendCommandResponse(
                id = command.id,
                success = false,
                error = "Unknown action: ${command.action}"
            )
            return
        }

        val startTime = System.currentTimeMillis()
        try {
            val result = handler.handle(command)
            val duration = System.currentTimeMillis() - startTime
            toolCallLogger.log(
                action = command.action,
                connectionType = "REMOTE_WS",
                success = result.success,
                durationMs = duration,
                errorMessage = result.error
            )
            webSocketClient.sendCommandResponse(
                id = command.id,
                success = result.success,
                data = result.data,
                error = result.error
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            toolCallLogger.log(
                action = command.action,
                connectionType = "REMOTE_WS",
                success = false,
                durationMs = duration,
                errorMessage = e.message
            )
            Log.e(TAG, "Error handling command ${command.action}", e)
            webSocketClient.sendCommandResponse(
                id = command.id,
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    private fun mapConnectionState(state: ConnectionState, serverUrl: String): ModeStatus {
        return when (state) {
            ConnectionState.DISCONNECTED -> ModeStatus(
                state = ModeState.IDLE,
                message = "Disconnected"
            )

            ConnectionState.CONNECTING -> ModeStatus(
                state = ModeState.STARTING,
                message = "Connecting..."
            )

            ConnectionState.CONNECTED -> ModeStatus(
                state = ModeState.RUNNING,
                message = "Connected to $serverUrl"
            )

            ConnectionState.PENDING_APPROVAL -> ModeStatus(
                state = ModeState.RUNNING,
                message = "Awaiting approval..."
            )

            ConnectionState.APPROVED -> ModeStatus(
                state = ModeState.RUNNING,
                message = "Connected to $serverUrl",
                connectedClients = 1
            )

            ConnectionState.REJECTED -> ModeStatus(
                state = ModeState.ERROR,
                message = "Connection rejected"
            )

            ConnectionState.ERROR -> ModeStatus(
                state = ModeState.ERROR,
                message = "Connection error"
            )
        }
    }
}

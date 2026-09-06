package com.aster.service.mode

import android.util.Log
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.model.Command
import com.aster.data.model.ConnectionState
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.execution.ExecutionCoordinator
import com.aster.service.execution.ExecutionProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import com.aster.service.wire.WireParams
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json

/**
 * Wraps existing AsterWebSocketClient in the ConnectionMode interface.
 */
class RemoteWsMode(
    private val webSocketClient: AsterWebSocketClient,
    private val commandHandlers: Map<String, CommandHandler>,
    private val toolCallLogger: ToolCallLogger,
    private val executions: ExecutionCoordinator? = null,
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
    @Volatile private var executionOwner: String? = null

    override suspend fun start(config: ModeConfig) {
        val remoteConfig = config as? ModeConfig.RemoteConfig
            ?: throw IllegalArgumentException("RemoteWsMode requires RemoteConfig")
        executionOwner = "ws:" + ExecutionProtocol.sha256(remoteConfig.serverUrl)

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
        executionOwner = null
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

    private suspend fun handleCommand(incoming: Command) {
        if (incoming.action in setOf("execution_capabilities", "execution_submit", "execution_status", "execution_seal_if_unstarted")) {
            val owner = executionOwner ?: return
            // Submit can await the physical worker. Keep the collector free
            // for original status/seal reads and pin the response to this
            // principal and socket generation, even after mode replacement.
            scope.launch { handleExecution(incoming, owner) }
            return
        }
        // Normalise snake_case↔camelCase before dispatch, exactly as the IPC and
        // local-MCP paths do — a remote caller follows the published (snake_case)
        // tool schema while several handlers read camelCase. See [WireParams].
        val command = incoming.copy(params = WireParams.normalize(incoming.params))
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
        toolCallLogger.onToolStarted(command.action, "REMOTE_WS")
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

    private suspend fun handleExecution(command: Command, owner: String) {
        if (executionOwner != owner || !webSocketClient.isCurrentCommand(command) ||
            webSocketClient.connectionState.value != ConnectionState.APPROVED) return
        try {
            val coordinator = requireNotNull(executions) { "Tracked execution is unavailable" }
            val body = JsonObject(command.params ?: emptyMap()).toString()
            val response = when (command.action) {
                "execution_capabilities" -> {
                    require(command.params.isNullOrEmpty())
                    coordinator.capabilities()
                }
                "execution_status" -> coordinator.status(body, owner)
                "execution_seal_if_unstarted" -> coordinator.sealIfUnstarted(body, owner)
                "execution_submit" -> coordinator.submit(body, owner, {
                    executionOwner == owner && webSocketClient.isCurrentCommand(command) &&
                        webSocketClient.connectionState.value == ConnectionState.APPROVED
                }) { requested ->
                    commandHandlers[requested.action]?.handle(requested)
                        ?: CommandResult.failure("Unsupported tracked action")
                }
                else -> error("Unsupported execution operation")
            }
            webSocketClient.sendCommandResponse(command.id, true, Json.parseToJsonElement(response),
                expectedGeneration = command.connectionGeneration)
        } catch (_: Exception) {
            // A transport failure is not a no-dispatch proof. Never create a
            // substitute legacy command or disclose journal/result contents.
            webSocketClient.sendCommandResponse(command.id, false,
                error = "Original execution observation is unavailable",
                expectedGeneration = command.connectionGeneration)
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

            ConnectionState.RECONNECTING -> ModeStatus(
                state = ModeState.RUNNING,
                message = "Reconnecting…"
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

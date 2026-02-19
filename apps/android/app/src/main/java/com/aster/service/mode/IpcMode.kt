package com.aster.service.mode

import android.os.Binder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.model.Command
import com.aster.ipc.IAsterCallback
import com.aster.ipc.IAsterService
import com.aster.service.CommandHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

class IpcMode(
    private val commandHandlers: Map<String, CommandHandler>,
    private val toolCallLogger: ToolCallLogger
) : ConnectionMode {

    companion object {
        private const val TAG = "IpcMode"
        private const val TOKEN_LENGTH = 32
        /** Results above this byte size are offloaded to PFD pipe instead of Binder. */
        private const val LARGE_RESULT_THRESHOLD = 500_000 // ~500 KB
    }

    override val modeType = ModeType.IPC
    override val displayName = "IPC (Binder)"

    private val _statusFlow = MutableStateFlow(ModeStatus())
    override val statusFlow: StateFlow<ModeStatus> = _statusFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val authenticatedUids = ConcurrentHashMap<Int, Long>()
    private val callbacks = ConcurrentHashMap<Int, IAsterCallback>()
    private val largeResults = ConcurrentHashMap<String, ByteArray>()
    private var currentToken: String = ""

    val token: String get() = currentToken

    val binder: IAsterService.Stub = object : IAsterService.Stub() {

        override fun authenticate(token: String): String {
            val callingUid = Binder.getCallingUid()
            Log.d(TAG, "authenticate() from UID=$callingUid")

            if (!constantTimeEquals(token, currentToken)) {
                Log.w(TAG, "Authentication failed: invalid token from UID=$callingUid")
                throw SecurityException("Invalid authentication token")
            }

            authenticatedUids[callingUid] = System.currentTimeMillis()
            Log.i(TAG, "UID=$callingUid authenticated successfully")
            updateClientCount()
            return "authenticated"
        }

        override fun executeCommand(action: String, paramsJson: String): String {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)

            val handler = commandHandlers[action]
                ?: run {
                    toolCallLogger.log(
                        action = action,
                        connectionType = "IPC",
                        success = false,
                        errorMessage = "Unknown action: $action"
                    )
                    return Json.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject {
                            put("success", JsonPrimitive(false))
                            put("error", JsonPrimitive("Unknown action: $action"))
                        }
                    )
                }

            val params = if (paramsJson.isNotBlank()) {
                try {
                    Json.parseToJsonElement(paramsJson).jsonObject.mapValues { it.value }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val command = Command(
                type = "command",
                id = java.util.UUID.randomUUID().toString(),
                action = action,
                params = params
            )

            val startTime = System.currentTimeMillis()
            toolCallLogger.onToolStarted(action, "IPC")
            val result = runBlocking {
                handler.handle(command)
            }
            val duration = System.currentTimeMillis() - startTime

            toolCallLogger.log(
                action = action,
                connectionType = "IPC",
                success = result.success,
                durationMs = duration,
                errorMessage = result.error
            )

            val resultJson = Json.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("success", JsonPrimitive(result.success))
                    result.data?.let { put("data", it) }
                    result.error?.let { put("error", JsonPrimitive(it)) }
                }
            )

            val resultBytes = resultJson.toByteArray(Charsets.UTF_8)
            if (resultBytes.size > LARGE_RESULT_THRESHOLD) {
                val resultId = java.util.UUID.randomUUID().toString()
                largeResults[resultId] = resultBytes
                Log.d(TAG, "Large result for '$action' (${resultBytes.size} bytes) stored as $resultId")
                return """{"_largeResult":"$resultId"}"""
            }

            return resultJson
        }

        override fun readLargeResult(resultId: String): ParcelFileDescriptor {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)

            val data = largeResults.remove(resultId)
                ?: throw IllegalArgumentException("No large result found for ID: $resultId")

            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            Thread {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { out ->
                        out.write(data)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write large result to pipe", e)
                }
            }.start()

            Log.d(TAG, "Streaming large result $resultId (${data.size} bytes) via PFD pipe")
            return readSide
        }

        override fun registerCallback(callback: IAsterCallback) {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)
            callbacks[callingUid] = callback
            Log.d(TAG, "Callback registered for UID=$callingUid")
        }

        override fun unregisterCallback(callback: IAsterCallback) {
            val callingUid = Binder.getCallingUid()
            callbacks.remove(callingUid)
            Log.d(TAG, "Callback unregistered for UID=$callingUid")
        }

        override fun getAvailableTools(): List<String> {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)
            return commandHandlers.keys.toList()
        }

        override fun disconnect() {
            val callingUid = Binder.getCallingUid()
            authenticatedUids.remove(callingUid)
            callbacks.remove(callingUid)
            Log.i(TAG, "UID=$callingUid disconnected")
            updateClientCount()
        }
    }

    override suspend fun start(config: ModeConfig) {
        val ipcConfig = config as? ModeConfig.IpcConfig
            ?: throw IllegalArgumentException("IpcMode requires IpcConfig")

        _statusFlow.value = ModeStatus(state = ModeState.STARTING, message = "Initializing IPC...")
        currentToken = ipcConfig.token
        authenticatedUids.clear()
        callbacks.clear()
        _statusFlow.value =
            ModeStatus(state = ModeState.RUNNING, message = "IPC Active — Ready for aster-one")
        Log.i(TAG, "IPC mode started")
    }

    override suspend fun stop() {
        _statusFlow.value = ModeStatus(state = ModeState.STOPPING, message = "Stopping IPC...")
        authenticatedUids.clear()
        callbacks.clear()
        largeResults.clear()
        scope.coroutineContext.cancelChildren()
        _statusFlow.value = ModeStatus(state = ModeState.IDLE)
        Log.i(TAG, "IPC mode stopped")
    }

    override fun getAvailableTools(): List<ToolInfo> {
        return ToolCatalog.resolve(commandHandlers.keys)
    }

    fun generateToken(): String {
        val bytes = ByteArray(TOKEN_LENGTH / 2)
        SecureRandom().nextBytes(bytes)
        currentToken = bytes.joinToString("") { "%02x".format(it) }
        return currentToken
    }

    fun broadcastEvent(eventType: String, payloadJson: String) {
        callbacks.values.forEach { callback ->
            try {
                callback.onEvent(eventType, payloadJson)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send event to callback", e)
            }
        }
    }

    private fun requireAuthenticated(uid: Int) {
        if (!authenticatedUids.containsKey(uid)) {
            throw SecurityException("Caller UID=$uid is not authenticated")
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    private fun updateClientCount() {
        val current = _statusFlow.value
        if (current.state == ModeState.RUNNING) {
            _statusFlow.value = current.copy(connectedClients = authenticatedUids.size)
        }
    }
}

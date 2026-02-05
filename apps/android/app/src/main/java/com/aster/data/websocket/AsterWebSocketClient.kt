package com.aster.data.websocket

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.aster.BuildConfig
import com.aster.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsterWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val context: Context
) {
    companion object {
        private const val TAG = "AsterWebSocketClient"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val RECONNECT_BASE_DELAY_MS = 30_000L
        private const val RECONNECT_MAX_DELAY_MS = 30_000L
        private const val APP_VERSION = "1.0.0"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var currentServerUrl: String? = null
    private var shouldReconnect = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingCommands = MutableSharedFlow<Command>(extraBufferCapacity = 64)
    val incomingCommands: SharedFlow<Command> = _incomingCommands.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val _deviceStatus = MutableStateFlow(DeviceStatus.PENDING)
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    fun connect(serverUrl: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Connecting to: ${redactUrl(serverUrl)}")
        currentServerUrl = serverUrl
        shouldReconnect = true
        reconnectAttempts = 0

        disconnect(reconnect = false)
        establishConnection(serverUrl)
    }

    private fun establishConnection(serverUrl: String) {
        _connectionState.value = ConnectionState.CONNECTING

        val wsUrl = buildWsUrl(serverUrl)
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    private fun buildWsUrl(serverUrl: String): String {
        val cleanUrl = serverUrl.trim().removeSuffix("/")

        val wsUrl = when {
            cleanUrl.startsWith("ws://") || cleanUrl.startsWith("wss://") -> cleanUrl
            cleanUrl.startsWith("http://") -> cleanUrl.replace("http://", "ws://")
            cleanUrl.startsWith("https://") -> cleanUrl.replace("https://", "wss://")
            // Default to secure wss:// for security
            else -> "wss://$cleanUrl"
        }

        // Warn about insecure connections
        if (wsUrl.startsWith("ws://") && !isLocalhost(wsUrl)) {
            Log.w(TAG, "WARNING: Using insecure WebSocket connection (ws://). " +
                    "Consider using wss:// for production environments.")
        }

        return wsUrl
    }

    /**
     * Check if the URL is localhost/local network (where ws:// is acceptable)
     */
    private fun isLocalhost(url: String): Boolean {
        val host = url.removePrefix("ws://").removePrefix("wss://").substringBefore("/").substringBefore(":")
        return host == "localhost" ||
                host == "127.0.0.1" ||
                host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.startsWith("172.16.") ||
                host.endsWith(".local")
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket connected")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                sendAuthMessage()
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Received message (type: ${extractMessageType(text)})")
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket closed: $code - $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                scope.launch {
                    _errors.emit("Connection failed: ${t.message}")
                }
                handleDisconnect()
            }
        }
    }

    private fun sendAuthMessage() {
        val deviceId = getDeviceId()
        val authMessage = AuthMessage(
            deviceId = deviceId,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE,
            appVersion = APP_VERSION
        )

        val messageJson = json.encodeToString(authMessage)
        if (BuildConfig.DEBUG) Log.d(TAG, "Sending auth for device: ${deviceId.take(8)}...")
        val sent = webSocket?.send(messageJson) ?: false
        if (!sent) {
            Log.w(TAG, "Failed to send auth message")
        }
        _connectionState.value = ConnectionState.PENDING_APPROVAL
    }

    private fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-${System.currentTimeMillis()}"

        // Hash the Android ID for privacy (don't send raw device ID)
        return hashDeviceId(androidId)
    }

    /**
     * Hash the device ID for privacy - sends a consistent but non-reversible identifier
     */
    private fun hashDeviceId(id: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(id.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.take(32)
        } catch (e: Exception) {
            // Fallback to original ID if hashing fails
            id
        }
    }

    private fun handleMessage(text: String) {
        try {
            val incoming = json.decodeFromString<IncomingMessage>(text)

            when (incoming.type) {
                "auth_result" -> {
                    val authResult = json.decodeFromString<AuthResult>(text)
                    handleAuthResult(authResult)
                }
                "command" -> {
                    val command = json.decodeFromString<Command>(text)
                    if (BuildConfig.DEBUG) Log.d(TAG, "Received command: ${command.action}")
                    scope.launch {
                        _incomingCommands.emit(command)
                    }
                }
                "heartbeat_ack" -> {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Heartbeat acknowledged")
                }
                else -> {
                    if (BuildConfig.DEBUG) Log.w(TAG, "Unknown message type: ${incoming.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: ${e.message}", e)
        }
    }

    /**
     * Extract message type for logging without exposing full content
     */
    private fun extractMessageType(json: String): String {
        return try {
            val typeMatch = """"type"\s*:\s*"([^"]+)"""".toRegex().find(json)
            typeMatch?.groupValues?.get(1) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Redact URL for logging - show host but hide any credentials or tokens
     */
    private fun redactUrl(url: String): String {
        return try {
            val cleanUrl = url.trim()
            // Remove any credentials from URL
            val noCredentials = cleanUrl.replace(Regex("://[^@]+@"), "://***@")
            // Remove query params that might contain tokens
            noCredentials.substringBefore("?") + if (noCredentials.contains("?")) "?[redacted]" else ""
        } catch (e: Exception) {
            "[redacted]"
        }
    }

    private fun handleAuthResult(result: AuthResult) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Auth result: ${result.status}")

        when (result.status) {
            "approved" -> {
                _connectionState.value = ConnectionState.APPROVED
                _deviceStatus.value = DeviceStatus.APPROVED
            }
            "pending" -> {
                _connectionState.value = ConnectionState.PENDING_APPROVAL
                _deviceStatus.value = DeviceStatus.PENDING
            }
            "rejected" -> {
                _connectionState.value = ConnectionState.REJECTED
                _deviceStatus.value = DeviceStatus.REJECTED
                shouldReconnect = false
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        val heartbeat = HeartbeatMessage()
        val messageJson = json.encodeToString(heartbeat)
        val sent = webSocket?.send(messageJson) ?: false
        if (!sent && BuildConfig.DEBUG) {
            Log.w(TAG, "Failed to send heartbeat")
        }
    }

    fun sendCommandResponse(id: String, success: Boolean, data: kotlinx.serialization.json.JsonElement? = null, error: String? = null) {
        val response = CommandResponse(
            id = id,
            success = success,
            data = data,
            error = error
        )
        val messageJson = json.encodeToString(response)
        if (BuildConfig.DEBUG) Log.d(TAG, "Sending response for command: $id (success: $success)")
        val sent = webSocket?.send(messageJson) ?: false
        if (!sent) {
            Log.w(TAG, "Failed to send command response for: $id")
        }
    }

    fun sendEvent(eventType: String, data: Map<String, kotlinx.serialization.json.JsonElement>) {
        val event = EventMessage(
            eventType = eventType,
            data = data
        )
        val messageJson = json.encodeToString(event)
        if (BuildConfig.DEBUG) Log.d(TAG, "Sending event: $eventType")
        val sent = webSocket?.send(messageJson) ?: false
        if (!sent) {
            Log.w(TAG, "Failed to send event: $eventType")
        }
    }

    private fun handleDisconnect() {
        heartbeatJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED

        if (shouldReconnect) {
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = calculateReconnectDelay()
            if (BuildConfig.DEBUG) Log.d(TAG, "Reconnecting in ${delayMs}ms (attempt ${reconnectAttempts + 1})")
            delay(delayMs)

            currentServerUrl?.let { url ->
                reconnectAttempts++
                establishConnection(url)
            }
        }
    }

    private fun calculateReconnectDelay(): Long {
        val exponentialDelay = RECONNECT_BASE_DELAY_MS * (1 shl reconnectAttempts.coerceAtMost(5))
        return exponentialDelay.coerceAtMost(RECONNECT_MAX_DELAY_MS)
    }

    fun disconnect(reconnect: Boolean = false) {
        shouldReconnect = reconnect
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null

        if (!reconnect) {
            _connectionState.value = ConnectionState.DISCONNECTED
            _deviceStatus.value = DeviceStatus.PENDING
        }
    }

    fun isConnected(): Boolean {
        return connectionState.value == ConnectionState.APPROVED ||
               connectionState.value == ConnectionState.CONNECTED ||
               connectionState.value == ConnectionState.PENDING_APPROVAL
    }

    fun shutdown() {
        disconnect(reconnect = false)
        scope.cancel()
    }
}

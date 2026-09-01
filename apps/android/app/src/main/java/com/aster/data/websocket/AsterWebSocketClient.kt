package com.aster.data.websocket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.aster.BuildConfig
import com.aster.data.model.AuthMessage
import com.aster.data.model.AuthResult
import com.aster.data.model.Command
import com.aster.data.model.CommandResponse
import com.aster.data.model.ConnectionState
import com.aster.data.model.DeviceStatus
import com.aster.data.model.EventMessage
import com.aster.data.model.HeartbeatMessage
import com.aster.data.model.IncomingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.UnknownServiceException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

internal object ReconnectPolicy {
    const val HEARTBEAT_INTERVAL_MS = 30_000L
    const val RECONNECT_BASE_DELAY_MS = 2_000L
    const val RECONNECT_MAX_DELAY_MS = 30_000L

    fun reconnectDelay(attempts: Int): Long {
        val shift = attempts.coerceIn(0, 16)
        val exponentialDelay = RECONNECT_BASE_DELAY_MS * (1L shl shift)
        return exponentialDelay.coerceAtMost(RECONNECT_MAX_DELAY_MS)
    }

    fun shouldIgnoreStale(listenerGeneration: Int, currentGeneration: Int): Boolean {
        return listenerGeneration != currentGeneration
    }

    fun ackTimedOut(
        lastAckAt: Long,
        now: Long,
        intervalMs: Long = HEARTBEAT_INTERVAL_MS
    ): Boolean {
        if (lastAckAt == 0L) return false
        return now - lastAckAt > 2 * intervalMs
    }
}

/**
 * Scheme defaults for a typed server URL. Explicit ws(s)/http(s) are kept as-is.
 * Bare 192./10./100. (LAN + Tailscale CGNAT) default to ws://; everything else wss://.
 */
internal object WsUrlPolicy {
    fun buildWsUrl(serverUrl: String): String {
        val cleanUrl = serverUrl.trim().removeSuffix("/")
        return when {
            cleanUrl.startsWith("ws://") || cleanUrl.startsWith("wss://") -> cleanUrl
            cleanUrl.startsWith("http://") -> cleanUrl.replace("http://", "ws://")
            cleanUrl.startsWith("https://") -> cleanUrl.replace("https://", "wss://")
            isLanCleartextHost(extractHost(cleanUrl)) -> "ws://$cleanUrl"
            else -> "wss://$cleanUrl"
        }
    }

    fun extractHost(url: String): String {
        var rest = url.trim()
        val schemes = arrayOf("wss://", "ws://", "https://", "http://")
        for (scheme in schemes) {
            if (rest.startsWith(scheme)) {
                rest = rest.substring(scheme.length)
                break
            }
        }
        rest = rest.substringAfter("@", missingDelimiterValue = rest)
        val hostPort = rest.substringBefore("/").substringBefore("?")
        return if (hostPort.startsWith("[")) {
            hostPort.removePrefix("[").substringBefore("]")
        } else {
            hostPort.substringBefore(":")
        }
    }

    fun isLanCleartextHost(host: String): Boolean {
        return host.startsWith("192.") || host.startsWith("10.") || host.startsWith("100.")
    }
}

/** Maps OkHttp onFailure throwables to TLS / cleartext copy. */
internal object ConnectionFailureMapper {
    const val CLEARTEXT_MESSAGE = "This build blocks cleartext — update the Aster app."

    fun tlsMessage(host: String): String {
        val displayHost = host.ifBlank { "host" }
        return "Server at $displayHost doesn't speak TLS on this port. " +
            "Use `ws://` on the LAN, or the `wss://<magicdns>` URL printed by `aster status` when Tailscale is active."
    }

    fun map(throwable: Throwable, host: String): String {
        if (isCleartextPolicy(throwable)) return CLEARTEXT_MESSAGE
        if (isTlsOrHandshakeParse(throwable)) return tlsMessage(host)
        return "Connection failed: ${throwable.message}"
    }

    fun isDiagnostic(throwable: Throwable): Boolean {
        return isCleartextPolicy(throwable) || isTlsOrHandshakeParse(throwable)
    }

    fun isCleartextPolicy(throwable: Throwable): Boolean {
        return anyCause(throwable) { current ->
            val msg = current.message.orEmpty()
            (current is UnknownServiceException && msg.contains("CLEARTEXT", ignoreCase = true)) ||
                (msg.contains("CLEARTEXT", ignoreCase = true) &&
                    msg.contains("not permitted", ignoreCase = true)) ||
                msg.contains("cleartext traffic not permitted", ignoreCase = true)
        }
    }

    fun isTlsOrHandshakeParse(throwable: Throwable): Boolean {
        return anyCause(throwable) { current ->
            if (current is SSLException) return@anyCause true
            val msg = current.message.orEmpty()
            msg.contains("Unable to parse TLS packet header", ignoreCase = true) ||
                msg.contains("SSL handshake", ignoreCase = true) ||
                msg.contains("TLS handshake", ignoreCase = true)
        }
    }

    private fun anyCause(throwable: Throwable, predicate: (Throwable) -> Boolean): Boolean {
        var current: Throwable? = throwable
        val seen = HashSet<Throwable>()
        while (current != null && seen.add(current)) {
            if (predicate(current)) return true
            current = current.cause
        }
        return false
    }
}

@Singleton
class AsterWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val context: Context
) {
    companion object {
        private const val TAG = "AsterWebSocketClient"
        private val APP_VERSION = BuildConfig.VERSION_NAME
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val socketLock = Any()
    private val connectLock = Any()
    private val connectionGeneration = AtomicInteger(0)

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    @Volatile private var reconnectAttempts = 0
    @Volatile private var currentServerUrl: String? = null
    @Volatile private var shouldReconnect = false
    @Volatile private var lastAckAt = 0L

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var lastNetworkId: Long? = null

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
        synchronized(connectLock) {
            currentServerUrl = serverUrl
            reconnectAttempts = 0
            reconnectJob?.cancel()
            reconnectJob = null
            clearSocketOnly()
            shouldReconnect = true
            registerNetworkCallback()
            establishConnection(serverUrl)
        }
    }

    /**
     * Tear down the live socket without changing reconnect policy or emitting
     * [ConnectionState.DISCONNECTED].
     */
    private fun clearSocketOnly() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        val socket: WebSocket?
        synchronized(socketLock) {
            connectionGeneration.incrementAndGet()
            socket = webSocket
            webSocket = null
        }
        socket?.cancel()
    }

    /** Immediate reconnect for network-switch and heartbeat-watchdog paths. */
    private fun reconnectNow() {
        synchronized(connectLock) {
            if (!shouldReconnect) return
            val url = currentServerUrl ?: return
            reconnectJob?.cancel()
            reconnectJob = null
            clearSocketOnly()
            establishConnection(url)
        }
    }

    private fun establishConnection(serverUrl: String) {
        _connectionState.value = ConnectionState.CONNECTING

        val wsUrl = buildWsUrl(serverUrl)
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        val generation = connectionGeneration.get()
        val socket = okHttpClient.newWebSocket(request, createWebSocketListener(generation))
        synchronized(socketLock) {
            webSocket = socket
        }
    }

    private fun buildWsUrl(serverUrl: String): String {
        val wsUrl = WsUrlPolicy.buildWsUrl(serverUrl)

        if (wsUrl.startsWith("ws://") && !isLocalhost(wsUrl)) {
            Log.w(TAG, "WARNING: Using insecure WebSocket connection (ws://). " +
                    "Consider using wss:// for production environments.")
        }

        return wsUrl
    }

    private fun isLocalhost(url: String): Boolean {
        val host = WsUrlPolicy.extractHost(url)
        return host == "localhost" ||
                host == "127.0.0.1" ||
                WsUrlPolicy.isLanCleartextHost(host) ||
                host.startsWith("172.16.") ||
                host.endsWith(".local")
    }

    private fun createWebSocketListener(generation: Int): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (ReconnectPolicy.shouldIgnoreStale(generation, connectionGeneration.get())) return
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket connected")
                lastAckAt = System.currentTimeMillis()
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempts = 0
                sendAuthMessage()
                startHeartbeat()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (ReconnectPolicy.shouldIgnoreStale(generation, connectionGeneration.get())) return
                if (BuildConfig.DEBUG) Log.d(TAG, "Received message (type: ${extractMessageType(text)})")
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (ReconnectPolicy.shouldIgnoreStale(generation, connectionGeneration.get())) return
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (ReconnectPolicy.shouldIgnoreStale(generation, connectionGeneration.get())) return
                if (BuildConfig.DEBUG) Log.d(TAG, "WebSocket closed: $code - $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (ReconnectPolicy.shouldIgnoreStale(generation, connectionGeneration.get())) return
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                val host = WsUrlPolicy.extractHost(currentServerUrl.orEmpty())
                val message = ConnectionFailureMapper.map(t, host)
                if (!shouldReconnect) {
                    _connectionState.value = ConnectionState.ERROR
                }
                if (!shouldReconnect || ConnectionFailureMapper.isDiagnostic(t)) {
                    scope.launch {
                        _errors.emit(message)
                    }
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
                    lastAckAt = System.currentTimeMillis()
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
                delay(ReconnectPolicy.HEARTBEAT_INTERVAL_MS)
                if (!isActive) return@launch
                if (ReconnectPolicy.ackTimedOut(lastAckAt, System.currentTimeMillis())) {
                    Log.w(TAG, "Heartbeat ack timed out, reconnecting")
                    reconnectNow()
                    return@launch
                }
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
        if (shouldReconnect) {
            _connectionState.value = ConnectionState.RECONNECTING
            scheduleReconnect()
        } else if (_connectionState.value != ConnectionState.ERROR) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = ReconnectPolicy.reconnectDelay(reconnectAttempts)
            if (BuildConfig.DEBUG) Log.d(TAG, "Reconnecting in ${delayMs}ms (attempt ${reconnectAttempts + 1})")
            delay(delayMs)
            if (!isActive || !shouldReconnect) return@launch
            val url = currentServerUrl ?: return@launch
            reconnectAttempts++
            synchronized(connectLock) {
                if (!shouldReconnect) return@launch
                clearSocketOnly()
                establishConnection(url)
            }
        }
    }

    fun disconnect(reconnect: Boolean = false) {
        synchronized(connectLock) {
            shouldReconnect = reconnect
            reconnectJob?.cancel()
            reconnectJob = null
            if (!reconnect) {
                unregisterNetworkCallback()
            }
            clearSocketOnly()
            if (!reconnect) {
                _connectionState.value = ConnectionState.DISCONNECTED
                _deviceStatus.value = DeviceStatus.PENDING
            }
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

    private fun connectivityManager(): ConnectivityManager? {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val cm = connectivityManager() ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Sticky onAvailable fires on register; reconnect only when the id changes.
                val id = network.networkHandle
                val previous = lastNetworkId
                lastNetworkId = id
                if (previous != null && previous != id && shouldReconnect) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Default network changed, reconnecting")
                    scope.launch { reconnectNow() }
                }
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: RuntimeException) {
            Log.w(TAG, "Failed to register network callback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        networkCallback = null
        lastNetworkId = null
        try {
            connectivityManager()?.unregisterNetworkCallback(callback)
        } catch (_: RuntimeException) {
        }
    }
}

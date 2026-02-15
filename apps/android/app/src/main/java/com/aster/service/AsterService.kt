package com.aster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aster.BuildConfig
import com.aster.R
import com.aster.data.local.SettingsDataStore
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.receiver.SmsBroadcastReceiver
import com.aster.service.mode.ConnectionMode
import com.aster.service.mode.IpcMode
import com.aster.service.mode.McpMode
import com.aster.service.mode.ModeConfig
import com.aster.service.mode.ModeState
import com.aster.service.mode.ModeType
import com.aster.service.mode.RemoteWsMode
import com.aster.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

@AndroidEntryPoint
class AsterService : Service() {

    companion object {
        private const val TAG = "AsterService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "aster_service_channel"
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        private const val WAKE_LOCK_RENEWAL_INTERVAL_MS = 9 * 60 * 1000L

        const val ACTION_START = "com.aster.action.START"
        const val ACTION_STOP = "com.aster.action.STOP"
        const val ACTION_NOTIFICATION_DISMISSED = "com.aster.action.NOTIFICATION_DISMISSED"

        const val EXTRA_MODE_TYPE = "mode_type"
        const val EXTRA_CONFIG_JSON = "config_json"
        const val EXTRA_SERVER_URL = "server_url"

        @Volatile
        var isRunning: Boolean = false
            private set

        /** Set of currently active mode types (supports concurrent modes). */
        @Volatile
        var activeModeTypes: Set<ModeType> = emptySet()
            private set

        /** Legacy accessor — returns the first active mode or null. */
        val activeModeType: ModeType?
            get() = activeModeTypes.firstOrNull()

        fun startService(context: Context, modeType: ModeType, configJson: String) {
            val intent = Intent(context, AsterService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE_TYPE, modeType.name)
                putExtra(EXTRA_CONFIG_JSON, configJson)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startService(context: Context, serverUrl: String? = null) {
            val intent = Intent(context, AsterService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE_TYPE, ModeType.REMOTE_WS.name)
                serverUrl?.let { putExtra(EXTRA_SERVER_URL, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stop a specific mode. If it's the last active mode, the service stops. */
        fun stopMode(context: Context, modeType: ModeType) {
            val intent = Intent(context, AsterService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_MODE_TYPE, modeType.name)
            }
            context.startService(intent)
        }

        /** Stop all modes and the entire service. */
        fun stopService(context: Context) {
            val intent = Intent(context, AsterService::class.java).apply {
                action = ACTION_STOP
                // No EXTRA_MODE_TYPE = stop all
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var webSocketClient: AsterWebSocketClient
    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var ipcMode: IpcMode

    @Inject
    lateinit var mcpMode: McpMode

    @Inject
    lateinit var remoteWsMode: RemoteWsMode

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLockRenewalJob: Job? = null
    private var smsBroadcastReceiver: SmsBroadcastReceiver? = null

    /** Active modes keyed by type. */
    private val activeModes = mutableMapOf<ModeType, ConnectionMode>()

    /** Status observe jobs per mode. */
    private val statusObserveJobs = mutableMapOf<ModeType, Job>()

    /** Error auto-stop jobs per mode. */
    private val errorStopJobs = mutableMapOf<ModeType, Job>()

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        if (BuildConfig.DEBUG) Log.d(TAG, "Service created")

        createNotificationChannel()
        startForegroundWithType(createNotification("Aster", "Starting..."))
        setupEventForwarding()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service started with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                acquireWakeLock()
                acquireWifiLock()

                val modeTypeName = intent.getStringExtra(EXTRA_MODE_TYPE) ?: ModeType.REMOTE_WS.name
                val modeType = ModeType.fromString(modeTypeName)

                serviceScope.launch {
                    try {
                        // If this specific mode is already running, stop it first (restart)
                        activeModes[modeType]?.let { existingMode ->
                            existingMode.stop()
                            statusObserveJobs[modeType]?.cancel()
                            errorStopJobs[modeType]?.cancel()
                        }

                        val mode = getModeForType(modeType)
                        val config = buildConfig(modeType, intent)

                        activeModes[modeType] = mode
                        activeModeTypes = activeModes.keys.toSet()

                        // Observe status for notification updates + auto-stop on error
                        statusObserveJobs[modeType] = serviceScope.launch {
                            mode.statusFlow.collect { status ->
                                updateNotification()

                                if (status.state == ModeState.ERROR) {
                                    errorStopJobs[modeType]?.cancel()
                                    errorStopJobs[modeType] = serviceScope.launch {
                                        delay(5000)
                                        if (mode.statusFlow.value.state == ModeState.ERROR) {
                                            Log.i(
                                                TAG,
                                                "Auto-stopping $modeType after sustained error"
                                            )
                                            stopModeInternal(modeType)
                                        }
                                    }
                                } else {
                                    errorStopJobs[modeType]?.cancel()
                                    errorStopJobs.remove(modeType)
                                }
                            }
                        }

                        mode.start(config)

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start mode $modeType", e)
                        activeModes.remove(modeType)
                        activeModeTypes = activeModes.keys.toSet()
                        updateNotification()
                    }
                }
            }

            ACTION_STOP -> {
                val modeTypeName = intent?.getStringExtra(EXTRA_MODE_TYPE)

                serviceScope.launch {
                    if (modeTypeName != null) {
                        // Stop a specific mode
                        val modeType = ModeType.fromString(modeTypeName)
                        stopModeInternal(modeType)
                    } else {
                        // Stop all modes
                        stopAllModesAndShutdown()
                    }
                }
            }

            ACTION_NOTIFICATION_DISMISSED -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Notification dismissed, recreating...")
                updateNotification()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service destroyed")
        isRunning = false
        activeModeTypes = emptySet()

        AsterNotificationListenerService.onNotificationEvent = null
        EventDeduplicator.stopCleanup()
        smsBroadcastReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        smsBroadcastReceiver = null

        serviceScope.cancel()
        releaseWifiLock()
        releaseWakeLock()

        super.onDestroy()
    }

    /** Stop a single mode. If no modes remain, stop the service. */
    private suspend fun stopModeInternal(modeType: ModeType) {
        try {
            activeModes[modeType]?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping mode $modeType", e)
        }
        activeModes.remove(modeType)
        statusObserveJobs[modeType]?.cancel()
        statusObserveJobs.remove(modeType)
        errorStopJobs[modeType]?.cancel()
        errorStopJobs.remove(modeType)
        activeModeTypes = activeModes.keys.toSet()

        if (activeModes.isEmpty()) {
            releaseWifiLock()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification()
        }
    }

    private suspend fun stopAllModesAndShutdown() {
        activeModes.values.forEach { mode ->
            try {
                mode.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping mode", e)
            }
        }
        activeModes.clear()
        statusObserveJobs.values.forEach { it.cancel() }
        statusObserveJobs.clear()
        errorStopJobs.values.forEach { it.cancel() }
        errorStopJobs.clear()
        activeModeTypes = emptySet()
        releaseWifiLock()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun getModeForType(type: ModeType): ConnectionMode = when (type) {
        ModeType.IPC -> ipcMode
        ModeType.LOCAL_MCP -> mcpMode
        ModeType.REMOTE_WS -> remoteWsMode
    }

    private suspend fun buildConfig(type: ModeType, intent: Intent): ModeConfig {
        return when (type) {
            ModeType.IPC -> {
                val token = ipcMode.generateToken()
                ModeConfig.IpcConfig(token)
            }

            ModeType.LOCAL_MCP -> {
                val port = settingsDataStore.mcpPort.first()
                ModeConfig.McpConfig(port)
            }

            ModeType.REMOTE_WS -> {
                val url = intent.getStringExtra(EXTRA_SERVER_URL)
                    ?: intent.getStringExtra(EXTRA_CONFIG_JSON)
                    ?: settingsDataStore.serverUrl.first()
                    ?: ""
                ModeConfig.RemoteConfig(url)
            }
        }
    }

    private fun updateNotification() {
        if (activeModes.isEmpty()) return
        val (title, text) = buildNotificationText()
        startForegroundWithType(createNotification(title, text))
    }

    private fun buildNotificationText(): Pair<String, String> {
        if (activeModes.isEmpty()) return "Aster" to "Idle"

        // Check for errors first
        val errorMode =
            activeModes.entries.find { it.value.statusFlow.value.state == ModeState.ERROR }
        if (errorMode != null) {
            val otherRunning = activeModes.size - 1
            val suffix = if (otherRunning > 0) " (+$otherRunning active)" else ""
            return "Aster" to "Error: ${errorMode.value.statusFlow.value.message}$suffix"
        }

        // Build status text showing all active modes
        val runningModes = activeModes.entries
            .filter { it.value.statusFlow.value.state == ModeState.RUNNING }
            .map { entry ->
                when (entry.key) {
                    ModeType.IPC -> "IPC"
                    ModeType.LOCAL_MCP -> "MCP"
                    ModeType.REMOTE_WS -> "Remote"
                }
            }

        return if (runningModes.isNotEmpty()) {
            "Aster" to "Active: ${runningModes.joinToString(" + ")}"
        } else {
            "Aster" to "Starting..."
        }
    }

    private fun startForegroundWithType(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            } else {
                0
            }
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aster Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Maintains Aster service in the background"
                setShowBadge(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AsterService::class.java).apply {
                action = ACTION_NOTIFICATION_DISMISSED
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deleteIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Aster::ServiceWakeLock"
            ).apply {
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "Wake lock acquired")
            startWakeLockRenewal()
        }
    }

    private fun startWakeLockRenewal() {
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = serviceScope.launch {
            while (isActive) {
                delay(WAKE_LOCK_RENEWAL_INTERVAL_MS)
                wakeLock?.let {
                    if (it.isHeld) {
                        it.release()
                        it.acquire(WAKE_LOCK_TIMEOUT_MS)
                        if (BuildConfig.DEBUG) Log.d(TAG, "Wake lock renewed")
                    }
                }
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLockRenewalJob?.cancel()
        wakeLockRenewalJob = null
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                if (BuildConfig.DEBUG) Log.d(TAG, "Wake lock released")
            }
        }
        wakeLock = null
    }

    private fun acquireWifiLock() {
        if (wifiLock == null) {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "Aster::WifiLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "WiFi lock acquired")
        }
    }

    private fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) {
                it.release()
                if (BuildConfig.DEBUG) Log.d(TAG, "WiFi lock released")
            }
        }
        wifiLock = null
    }

    private fun setupEventForwarding() {
        EventDeduplicator.startCleanup(serviceScope)

        AsterNotificationListenerService.onNotificationEvent = { packageName, title, text, postTime ->
            val data = mapOf<String, JsonElement>(
                "packageName" to JsonPrimitive(packageName),
                "title" to JsonPrimitive(title),
                "text" to JsonPrimitive(text),
                "postTime" to JsonPrimitive(postTime)
            )
            forwardEvent("notification", data)
        }

        val receiver = SmsBroadcastReceiver()
        receiver.onSmsReceived = { sender, body, timestamp ->
            val data = mapOf<String, JsonElement>(
                "sender" to JsonPrimitive(sender),
                "body" to JsonPrimitive(body),
                "receivedAt" to JsonPrimitive(timestamp)
            )
            forwardEvent("sms_received", data)
        }
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        registerReceiver(receiver, filter)
        smsBroadcastReceiver = receiver

        Log.i(TAG, "Event forwarding set up")
    }

    /** Forward events to ALL active modes that support it. */
    private fun forwardEvent(eventType: String, data: Map<String, JsonElement>) {
        // IPC mode — push via callback
        if (activeModes.containsKey(ModeType.IPC)) {
            val json = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.JsonObject(data)
            )
            ipcMode.broadcastEvent(eventType, json)
        }

        // Remote WS — push via WebSocket
        if (activeModes.containsKey(ModeType.REMOTE_WS) && webSocketClient.isConnected()) {
            webSocketClient.sendEvent(eventType, data)
        }

        // MCP doesn't have server-initiated events
        if (activeModes.containsKey(ModeType.LOCAL_MCP)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Event $eventType in MCP mode (no push channel)")
        }
    }
}

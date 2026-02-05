package com.aster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aster.BuildConfig
import com.aster.R
import com.aster.data.local.SettingsDataStore
import com.aster.data.model.Command
import com.aster.data.model.ConnectionState
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.handlers.*
import com.aster.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class AsterService : Service() {

    companion object {
        private const val TAG = "AsterService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "aster_service_channel"
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes
        private const val WAKE_LOCK_RENEWAL_INTERVAL_MS = 9 * 60 * 1000L // Renew every 9 minutes

        const val ACTION_START = "com.aster.action.START"
        const val ACTION_STOP = "com.aster.action.STOP"
        const val ACTION_CONNECT = "com.aster.action.CONNECT"
        const val ACTION_DISCONNECT = "com.aster.action.DISCONNECT"
        const val ACTION_NOTIFICATION_DISMISSED = "com.aster.action.NOTIFICATION_DISMISSED"

        const val EXTRA_SERVER_URL = "server_url"

        /**
         * Track if the service is running. Updated in onCreate/onDestroy.
         * Use this instead of getRunningServices() which is deprecated.
         */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context, serverUrl: String? = null) {
            val intent = Intent(context, AsterService::class.java).apply {
                action = ACTION_START
                serverUrl?.let { putExtra(EXTRA_SERVER_URL, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AsterService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var webSocketClient: AsterWebSocketClient

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLockRenewalJob: Job? = null

    private val commandHandlers = mutableMapOf<String, CommandHandler>()
    private var mediaHandler: MediaHandler? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        if (BuildConfig.DEBUG) Log.d(TAG, "Service created")

        createNotificationChannel()

        // Call startForeground immediately in onCreate to meet Android's requirements
        // Must be called within 5 seconds of startForegroundService()
        startForegroundWithType(createNotification(ConnectionState.DISCONNECTED))

        registerCommandHandlers()
        observeConnectionState()
        observeCommands()
    }

    private fun startForegroundWithType(notification: Notification) {
        // Use ServiceCompat for better compatibility across Android versions
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service started with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START, ACTION_CONNECT -> {
                acquireWakeLock()
                acquireWifiLock()

                val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)
                if (serverUrl != null) {
                    webSocketClient.connect(serverUrl)
                } else {
                    // Try to connect with saved URL
                    serviceScope.launch {
                        val savedUrl = settingsDataStore.serverUrl.first()
                        if (!savedUrl.isNullOrBlank()) {
                            webSocketClient.connect(savedUrl)
                        }
                    }
                }
            }

            ACTION_STOP, ACTION_DISCONNECT -> {
                webSocketClient.disconnect()
                releaseWifiLock()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_NOTIFICATION_DISMISSED -> {
                // On Android 14+, users can dismiss foreground notifications.
                // Recreate the notification to keep it visible while service runs.
                if (BuildConfig.DEBUG) Log.d(TAG, "Notification dismissed by user, recreating...")
                val currentState = webSocketClient.connectionState.value
                startForegroundWithType(createNotification(currentState))
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "Service destroyed")
        isRunning = false
        serviceScope.cancel()
        releaseWifiLock()
        releaseWakeLock()
        webSocketClient.disconnect()

        // Release MediaHandler resources
        mediaHandler?.release()
        mediaHandler = null

        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aster Service",
                NotificationManager.IMPORTANCE_DEFAULT // Higher importance for visibility
            ).apply {
                description = "Maintains connection to MCP server"
                setShowBadge(false)
                setSound(null, null) // Silent but visible
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(state: ConnectionState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete intent to recreate notification when dismissed on Android 14+
        val deleteIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AsterService::class.java).apply {
                action = ACTION_NOTIFICATION_DISMISSED
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text) = when (state) {
            ConnectionState.DISCONNECTED -> "Aster" to "Disconnected"
            ConnectionState.CONNECTING -> "Aster" to "Connecting..."
            ConnectionState.CONNECTED -> "Aster" to "Connected"
            ConnectionState.PENDING_APPROVAL -> "Aster" to "Awaiting approval..."
            ConnectionState.APPROVED -> "Aster" to "Connected & Ready"
            ConnectionState.REJECTED -> "Aster" to "Connection rejected"
            ConnectionState.ERROR -> "Aster" to "Connection error"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deleteIntent) // Recreate when dismissed
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            // Show notification immediately without 10s delay on Android 12+
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(state: ConnectionState) {
        // Use startForeground instead of notify to keep notification associated
        // with the foreground service (especially important on Android 14+)
        startForegroundWithType(createNotification(state))
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Aster::ServiceWakeLock"
            ).apply {
                acquire(WAKE_LOCK_TIMEOUT_MS) // Acquire with timeout
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "Wake lock acquired with ${WAKE_LOCK_TIMEOUT_MS}ms timeout")

            // Start periodic renewal to keep wake lock active while service runs
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
                        // Release and re-acquire to reset the timeout
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

    private fun registerCommandHandlers() {
        // Create MediaHandler and keep reference for cleanup
        mediaHandler = MediaHandler(this)

        // Register all command handlers
        val handlers = listOf(
            DeviceInfoHandler(this),
            FileSystemHandler(this),
            PackageHandler(this),
            ClipboardHandler(this),
            mediaHandler!!,
            ShellHandler(),
            IntentHandler(this),
            // New handlers for accessibility, notifications, SMS, and overlay
            AccessibilityHandler(),
            NotificationHandler(),
            SmsHandler(this),
            OverlayHandler(this),
            StorageHandler(this)
        )

        handlers.forEach { handler ->
            handler.supportedActions().forEach { action ->
                commandHandlers[action] = handler
            }
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Registered ${commandHandlers.size} command actions")
    }

    private fun observeConnectionState() {
        serviceScope.launch {
            webSocketClient.connectionState.collect { state ->
                if (BuildConfig.DEBUG) Log.d(TAG, "Connection state: $state")
                updateNotification(state)
            }
        }
    }

    private fun observeCommands() {
        serviceScope.launch {
            webSocketClient.incomingCommands.collect { command ->
                handleCommand(command)
            }
        }
    }

    private suspend fun handleCommand(command: Command) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Handling command: ${command.action} (id: ${command.id})")

        val handler = commandHandlers[command.action]

        if (handler == null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "No handler for action: ${command.action}")
            webSocketClient.sendCommandResponse(
                id = command.id,
                success = false,
                error = "Unknown action: ${command.action}"
            )
            return
        }

        try {
            val result = handler.handle(command)
            webSocketClient.sendCommandResponse(
                id = command.id,
                success = result.success,
                data = result.data,
                error = result.error
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling command ${command.action}", e)
            webSocketClient.sendCommandResponse(
                id = command.id,
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
}

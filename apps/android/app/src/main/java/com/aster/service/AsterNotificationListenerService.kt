package com.aster.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aster.BuildConfig
import com.aster.R
import com.aster.receiver.NotificationActionReceiver
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Notification Listener Service for reading and managing notifications.
 *
 * Provides:
 * - Reading active notifications from status bar
 * - Posting local notifications with optional actions
 * - Notification event forwarding to MCP server
 */
class AsterNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "AsterNotificationListener"
        private const val POSTED_NOTIFICATION_CHANNEL_ID = "aster_posted_notifications"
        private const val POSTED_NOTIFICATION_BASE_ID = 2000

        @Volatile
        private var instance: AsterNotificationListenerService? = null

        fun getInstance(): AsterNotificationListenerService? = instance

        fun isServiceEnabled(): Boolean = instance != null

        // Track posted notification IDs for actions
        private val postedNotifications = ConcurrentHashMap<Int, NotificationActionCallback>()
        private var nextNotificationId = POSTED_NOTIFICATION_BASE_ID

        /**
         * Callback interface for notification actions.
         */
        interface NotificationActionCallback {
            fun onActionClicked(actionId: String)
            fun onDismissed()
        }
    }

    private val notificationHistory = mutableListOf<JsonObject>()
    private val maxHistorySize = 100

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Guard against multiple onListenerConnected calls
        if (instance != null) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Notification listener already connected, ignoring duplicate")
            return
        }
        instance = this
        createPostedNotificationChannel()
        if (BuildConfig.DEBUG) Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        instance = null
        if (BuildConfig.DEBUG) Log.d(TAG, "Notification listener disconnected")
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Skip our own notifications
        if (sbn.packageName == packageName) return

        val notificationJson = notificationToJson(sbn)
        synchronized(notificationHistory) {
            notificationHistory.add(0, notificationJson)
            if (notificationHistory.size > maxHistorySize) {
                notificationHistory.removeAt(notificationHistory.size - 1)
            }
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Notification posted from: ${sbn.packageName}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        if (BuildConfig.DEBUG) Log.d(TAG, "Notification removed from: ${sbn.packageName}")
    }

    /**
     * Get all active notifications.
     */
    fun getActiveNotifications(limit: Int = 50): JsonArray {
        return try {
            val notifications = activeNotifications ?: emptyArray()
            buildJsonArray {
                notifications.take(limit).forEach { sbn ->
                    add(notificationToJson(sbn))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active notifications", e)
            buildJsonArray { }
        }
    }

    /**
     * Get notification history (recently posted notifications).
     */
    fun getNotificationHistory(limit: Int = 50): JsonArray {
        return synchronized(notificationHistory) {
            buildJsonArray {
                notificationHistory.take(limit).forEach { add(it) }
            }
        }
    }

    /**
     * Convert a StatusBarNotification to JSON.
     */
    private fun notificationToJson(sbn: StatusBarNotification): JsonObject {
        val notification = sbn.notification
        val extras = notification.extras

        return buildJsonObject {
            put("key", sbn.key)
            put("id", sbn.id)
            put("packageName", sbn.packageName)
            put("postTime", sbn.postTime)
            put("isOngoing", sbn.isOngoing)
            put("isClearable", sbn.isClearable)

            // Extract notification content
            put("title", extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "")
            put("text", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "")
            put("bigText", extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "")
            put("subText", extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "")
            put("infoText", extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: "")

            // Category and priority
            put("category", notification.category ?: "")
            put("visibility", notification.visibility)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                put("channelId", notification.channelId ?: "")
            }

            // Actions
            notification.actions?.let { actions ->
                putJsonArray("actions") {
                    actions.forEach { action ->
                        add(buildJsonObject {
                            put("title", action.title?.toString() ?: "")
                        })
                    }
                }
            }

            // Group info
            put("group", notification.group ?: "")
            put("sortKey", notification.sortKey ?: "")
        }
    }

    /**
     * Post a local notification.
     */
    fun postNotification(
        title: String,
        body: String,
        actions: List<NotificationAction>? = null,
        channelId: String = POSTED_NOTIFICATION_CHANNEL_ID,
        callback: NotificationActionCallback? = null
    ): Int {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = nextNotificationId++

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Add actions if provided
        actions?.forEachIndexed { index, action ->
            val intent = Intent(this, NotificationActionReceiver::class.java).apply {
                this.action = "com.aster.NOTIFICATION_ACTION"
                putExtra("notification_id", notificationId)
                putExtra("action_id", action.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId * 10 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, action.label, pendingIntent)
        }

        // Store callback if provided
        if (callback != null) {
            postedNotifications[notificationId] = callback
        }

        notificationManager.notify(notificationId, builder.build())
        if (BuildConfig.DEBUG) Log.d(TAG, "Posted notification: $notificationId - $title")

        return notificationId
    }

    /**
     * Cancel a posted notification.
     */
    fun cancelPostedNotification(notificationId: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        postedNotifications.remove(notificationId)
    }

    /**
     * Dismiss a notification from another app by key.
     */
    fun dismissNotification(key: String): Boolean {
        return try {
            cancelNotification(key)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss notification: $key", e)
            false
        }
    }

    /**
     * Dismiss all clearable notifications.
     */
    fun dismissAllNotifications(): Boolean {
        return try {
            cancelAllNotifications()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss all notifications", e)
            false
        }
    }

    private fun createPostedNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                POSTED_NOTIFICATION_CHANNEL_ID,
                "Aster Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications posted by Aster MCP commands"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Data class for notification actions.
     */
    data class NotificationAction(
        val id: String,
        val label: String
    )

    /**
     * Handle action click from NotificationActionReceiver.
     */
    internal fun handleActionClick(notificationId: Int, actionId: String) {
        postedNotifications[notificationId]?.onActionClicked(actionId)
        cancelPostedNotification(notificationId)
    }
}

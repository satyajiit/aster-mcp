package com.aster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aster.service.AsterNotificationListenerService

/**
 * Broadcast receiver for handling notification action button clicks.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionRx"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.aster.NOTIFICATION_ACTION") {
            return
        }

        val notificationId = intent.getIntExtra("notification_id", -1)
        val actionId = intent.getStringExtra("action_id") ?: return

        Log.d(TAG, "Notification action received: notificationId=$notificationId, actionId=$actionId")

        // Forward to the notification listener service
        AsterNotificationListenerService.getInstance()?.handleActionClick(notificationId, actionId)
    }
}

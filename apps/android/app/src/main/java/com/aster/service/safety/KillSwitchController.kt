package com.aster.service.safety

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aster.R
import com.aster.receiver.KillSwitchReceiver
import com.aster.service.mode.IpcMode
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Always-reachable kill switch for screen control (Screen Control /goal P7).
 *
 * While a control session is active, shows a persistent high-importance
 * "AI is controlling your phone — STOP" notification with a STOP action. On
 * STOP it (1) broadcasts `screen_stop` + severs the IPC session via
 * [IpcMode.killActiveControl] (aborts the kernel loop within one action) and
 * (2) clears the activity overlay. The notification is the reliable fallback
 * because the overlay is gated by the draw-overlays permission.
 *
 * A static [instance] accessor (set in [attach]) lets [KillSwitchReceiver]
 * reach the singleton without Hilt receiver injection — parity with the
 * existing `NotificationActionReceiver`/`AsterNotificationListenerService`
 * getInstance pattern.
 */
@Singleton
class KillSwitchController @Inject constructor(
    // Provider (not direct) breaks a Hilt dependency cycle: IpcMode is built from
    // the @CommandHandlerMap, which includes ToolExecutionOverlay, which needs this
    // controller. IpcMode is @Singleton, so get() returns the same active instance;
    // it is only resolved lazily inside stop().
    private val ipcModeProvider: Provider<IpcMode>
) {
    companion object {
        private const val TAG = "KillSwitch"
        private const val CHANNEL_ID = "aster_screen_control_stop"
        private const val NOTIFICATION_ID = 1042
        const val ACTION_STOP = "com.aster.action.SCREEN_STOP"

        @Volatile
        private var instance: KillSwitchController? = null

        /** The attached singleton, for the broadcast receiver. */
        fun getInstance(): KillSwitchController? = instance
    }

    private var context: Context? = null
    private var onStopExtra: (() -> Unit)? = null

    fun attach(context: Context, onStop: (() -> Unit)? = null) {
        this.context = context.applicationContext
        this.onStopExtra = onStop
        instance = this
        createChannel()
    }

    fun detach() {
        hide()
        context = null
        onStopExtra = null
        instance = null
    }

    /** Show the persistent STOP notification (call when control starts). */
    fun showControlActive(currentApp: String?) {
        val ctx = context ?: return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(ctx, currentApp))
    }

    /** Remove the STOP notification (call when control ends). */
    fun hide() {
        val ctx = context ?: return
        ctx.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    /** Invoked by [KillSwitchReceiver] (STOP tap) or the overlay STOP button. */
    fun stop() {
        Log.w(TAG, "STOP requested — severing control")
        ipcModeProvider.get().killActiveControl()
        onStopExtra?.invoke()
        hide()
    }

    private fun buildNotification(ctx: Context, currentApp: String?): Notification {
        val stopIntent = Intent(ctx, KillSwitchReceiver::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getBroadcast(
            ctx, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (currentApp.isNullOrBlank()) {
            "AI is controlling your phone."
        } else {
            "AI is controlling your phone — $currentApp"
        }
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle("Screen control active")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "STOP", stopPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ctx = context ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen control kill switch",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Always-visible STOP for when the AI is driving your screen"
                setShowBadge(true)
            }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

package com.aster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aster.service.safety.KillSwitchController

/**
 * Receives the STOP action from the kill-switch notification and forwards to
 * [KillSwitchController] (Screen Control /goal P7). Mirrors
 * [NotificationActionReceiver]: reaches the singleton via its static
 * `getInstance()` accessor rather than Hilt receiver injection.
 */
class KillSwitchReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KillSwitchReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != KillSwitchController.ACTION_STOP) return
        Log.w(TAG, "STOP received")
        KillSwitchController.getInstance()?.stop()
    }
}

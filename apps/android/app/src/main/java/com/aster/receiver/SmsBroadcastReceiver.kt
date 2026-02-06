package com.aster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.aster.BuildConfig
import com.aster.service.EventDeduplicator

/**
 * BroadcastReceiver for incoming SMS messages.
 *
 * Registered dynamically in AsterService (not in manifest) so it is
 * tied to the service lifecycle. Groups multi-part SMS by sender and
 * records each message for deduplication before invoking the callback.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }

    var onSmsReceived: ((sender: String, body: String, timestamp: Long) -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // Group multi-part SMS by sender
        val grouped = mutableMapOf<String, StringBuilder>()
        for (msg in messages) {
            val sender = msg.originatingAddress ?: "unknown"
            grouped.getOrPut(sender) { StringBuilder() }.append(msg.messageBody ?: "")
        }

        val timestamp = System.currentTimeMillis()

        for ((sender, bodyBuilder) in grouped) {
            val body = bodyBuilder.toString()

            // Record for dedup BEFORE invoking callback
            EventDeduplicator.recordSms(sender, body)

            if (BuildConfig.DEBUG) Log.d(TAG, "SMS from $sender: ${body.take(50)}...")
            onSmsReceived?.invoke(sender, body, timestamp)
        }
    }
}

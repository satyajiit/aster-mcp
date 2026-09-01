@file:Suppress("DEPRECATION")

package com.aster.service.telephony

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class CallStateSnapshot(
    val state: Int,
    val number: String? = null,
)

/**
 * Process-local telephony state. Number is best-effort on API 26–30 and
 * always null on API 31+ (TelephonyCallback exposes state only).
 */
@Singleton
class CallStateMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "CallStateMonitor"
    }

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val _callState = MutableStateFlow(
        CallStateSnapshot(TelephonyManager.CALL_STATE_IDLE)
    )
    val callState: StateFlow<CallStateSnapshot> = _callState.asStateFlow()

    private var started = false
    private var api31Callback: Any? = null
    @Suppress("DEPRECATION")
    private var legacyListener: PhoneStateListener? = null

    @Synchronized
    fun start() {
        if (started) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerApi31()
            } else {
                registerLegacy()
            }
            started = true
        } catch (_: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE missing; call monitor not started")
        }
    }

    @Synchronized
    fun stop() {
        if (!started) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                unregisterApi31()
            } else {
                unregisterLegacy()
            }
        } catch (_: Exception) {
        }
        api31Callback = null
        legacyListener = null
        started = false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerApi31() {
        val callback = Api31Callback()
        telephonyManager.registerTelephonyCallback(
            ContextCompat.getMainExecutor(context),
            callback,
        )
        api31Callback = callback
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun unregisterApi31() {
        (api31Callback as? TelephonyCallback)?.let {
            telephonyManager.unregisterTelephonyCallback(it)
        }
    }

    @Suppress("DEPRECATION")
    private fun registerLegacy() {
        val listener = LegacyListener()
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        legacyListener = listener
    }

    @Suppress("DEPRECATION")
    private fun unregisterLegacy() {
        legacyListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
    }

    suspend fun waitForOffhook(timeoutMs: Long): Boolean {
        if (callState.value.state == TelephonyManager.CALL_STATE_OFFHOOK) return true
        return withTimeoutOrNull(timeoutMs) {
            callState.first { it.state == TelephonyManager.CALL_STATE_OFFHOOK }
            true
        } ?: false
    }

    private fun emit(state: Int, number: String?) {
        val normalized = number?.takeIf { it.isNotBlank() }
        _callState.value = CallStateSnapshot(state, normalized)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class Api31Callback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            emit(state, number = null)
        }
    }

    @Suppress("DEPRECATION")
    private inner class LegacyListener : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            emit(state, phoneNumber)
        }
    }
}

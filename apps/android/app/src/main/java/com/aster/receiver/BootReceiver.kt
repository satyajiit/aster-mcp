package com.aster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aster.data.local.SettingsDataStore
import com.aster.service.AsterService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d(TAG, "Boot completed, checking auto-start settings")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val autoStart = settingsDataStore.autoStartOnBoot.first()
                val serverUrl = settingsDataStore.serverUrl.first()

                if (autoStart && !serverUrl.isNullOrBlank()) {
                    Log.d(TAG, "Auto-starting Aster service")
                    AsterService.startService(context, serverUrl)
                } else {
                    Log.d(TAG, "Auto-start disabled or no server URL configured")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during boot handling", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

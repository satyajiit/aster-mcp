package com.aster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aster.data.local.SettingsDataStore
import com.aster.service.AsterService
import com.aster.service.mode.ModeType
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
                if (!autoStart) {
                    Log.d(TAG, "Auto-start disabled")
                    return@launch
                }

                val autoStartMode = settingsDataStore.autoStartMode.first()
                val modeType = if (autoStartMode != null) {
                    ModeType.fromString(autoStartMode)
                } else {
                    // Fallback: check last used mode or legacy server URL
                    val lastMode = settingsDataStore.lastMode.first()
                    if (lastMode != null) {
                        ModeType.fromString(lastMode)
                    } else {
                        val serverUrl = settingsDataStore.serverUrl.first()
                        if (!serverUrl.isNullOrBlank()) {
                            ModeType.REMOTE_WS
                        } else {
                            Log.d(TAG, "No mode configured for auto-start")
                            return@launch
                        }
                    }
                }

                Log.d(TAG, "Auto-starting Aster service with mode: $modeType")

                when (modeType) {
                    ModeType.REMOTE_WS -> {
                        val serverUrl = settingsDataStore.serverUrl.first()
                        if (!serverUrl.isNullOrBlank()) {
                            AsterService.startService(context, modeType, serverUrl)
                        } else {
                            Log.w(TAG, "Remote WS mode but no server URL configured")
                        }
                    }

                    ModeType.IPC -> {
                        AsterService.startService(context, modeType, "")
                    }

                    ModeType.LOCAL_MCP -> {
                        val port = settingsDataStore.mcpPort.first()
                        AsterService.startService(context, modeType, port.toString())
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during boot handling", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

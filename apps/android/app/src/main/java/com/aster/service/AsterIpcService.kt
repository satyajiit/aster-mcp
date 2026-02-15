package com.aster.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.aster.service.mode.IpcMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Bound service exposing the AIDL interface for cross-app IPC.
 * Separate from AsterService (the foreground service).
 * The foreground service starts/stops IPC mode which enables/disables this binder.
 */
@AndroidEntryPoint
class AsterIpcService : Service() {

    companion object {
        private const val TAG = "AsterIpcService"
    }

    @Inject
    lateinit var ipcMode: IpcMode

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: client binding from intent=$intent")
        return ipcMode.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: client unbound")
        return true // allow rebind
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind: client rebound")
    }
}

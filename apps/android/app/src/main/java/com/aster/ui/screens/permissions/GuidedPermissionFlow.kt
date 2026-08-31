package com.aster.ui.screens.permissions

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aster.util.PermissionType
import com.aster.util.PermissionUtils

/**
 * Drives the "Ask all together" guided grant flow: one batched system request
 * for every missing runtime permission, then each missing special-access
 * Settings screen in sequence, advancing when the user returns to the app.
 *
 * Each special-access screen is attempted once per run — returning without
 * granting moves on to the next instead of looping on the same screen.
 */
class GuidedPermissionFlow(
    private val launchRuntime: (Array<String>) -> Unit,
    private val launchSettings: (Intent) -> Unit
) {
    var isRunning by mutableStateOf(false)
        private set
    var stepsTotal by mutableStateOf(0)
        private set
    var stepsDone by mutableStateOf(0)
        private set
    var currentStepLabel by mutableStateOf<String?>(null)
        private set

    private var awaitingSettingsReturn = false
    private val attempted = mutableSetOf<PermissionType>()

    fun start(context: Context) {
        if (isRunning) return
        val runtime = PermissionUtils.missingRuntimePermissions(context)
        val specials = PermissionUtils.missingSpecialAccess(context)
        stepsTotal = (if (runtime.isNotEmpty()) 1 else 0) + specials.size
        stepsDone = 0
        attempted.clear()
        if (stepsTotal == 0) return
        isRunning = true
        if (runtime.isNotEmpty()) {
            currentStepLabel = "System permissions"
            launchRuntime(runtime.toTypedArray())
        } else {
            advance(context)
        }
    }

    /** Call from the RequestMultiplePermissions result callback. */
    fun onRuntimeResult(context: Context) {
        if (!isRunning) return
        stepsDone++
        advance(context)
    }

    /** Call on every ON_RESUME; advances past the Settings screen the user just left. */
    fun onResume(context: Context) {
        if (!isRunning || !awaitingSettingsReturn) return
        awaitingSettingsReturn = false
        stepsDone++
        advance(context)
    }

    private fun advance(context: Context) {
        val next = PermissionUtils.missingSpecialAccess(context)
            .firstOrNull { it !in attempted }
        if (next == null) {
            finish()
            return
        }
        attempted += next
        val intent = PermissionUtils.specialAccessIntent(context, next)
        if (intent == null) {
            advance(context)
            return
        }
        currentStepLabel = PermissionUtils.getPermissionName(next)
        awaitingSettingsReturn = true
        try {
            launchSettings(intent)
        } catch (_: Exception) {
            awaitingSettingsReturn = false
            advance(context)
        }
    }

    private fun finish() {
        isRunning = false
        awaitingSettingsReturn = false
        currentStepLabel = null
    }
}

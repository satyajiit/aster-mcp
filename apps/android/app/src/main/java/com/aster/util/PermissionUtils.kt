package com.aster.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Permission types tracked by Aster.
 */
enum class PermissionType {
    LOCATION,
    NOTIFICATIONS,
    PHONE_SMS,
    ACCESSIBILITY,
    NOTIFICATION_LISTENER,
    OVERLAY,
    STORAGE,
    BATTERY,
    CAMERA,
    CONTACTS
}

/**
 * Result of permission checks.
 */
data class PermissionCheckResult(
    val allGranted: Boolean,
    val permissions: Map<PermissionType, Boolean>,
    val missingPermissions: List<PermissionType>
) {
    val grantedCount: Int get() = permissions.count { it.value }
    val totalCount: Int get() = permissions.size
}

/**
 * Utility object for checking permissions.
 */
object PermissionUtils {

    /**
     * Check all permissions and return a comprehensive result.
     */
    fun checkAllPermissions(context: Context): PermissionCheckResult {
        val permissions = mapOf(
            PermissionType.LOCATION to checkLocationPermission(context),
            PermissionType.NOTIFICATIONS to checkNotificationPermission(context),
            PermissionType.PHONE_SMS to checkPhoneSmsPermission(context),
            PermissionType.ACCESSIBILITY to checkAccessibilityPermission(context),
            PermissionType.NOTIFICATION_LISTENER to checkNotificationListenerPermission(context),
            PermissionType.OVERLAY to checkOverlayPermission(context),
            PermissionType.STORAGE to checkStoragePermission(context),
            PermissionType.BATTERY to checkBatteryOptimization(context),
            PermissionType.CAMERA to checkCameraPermission(context),
            PermissionType.CONTACTS to checkContactsPermission(context)
        )

        val missing = permissions.filter { !it.value }.keys.toList()

        return PermissionCheckResult(
            allGranted = missing.isEmpty(),
            permissions = permissions,
            missingPermissions = missing
        )
    }

    /**
     * Check only critical permissions required for basic functionality.
     * These are: Accessibility, Notification Listener, and Battery Optimization.
     */
    fun checkCriticalPermissions(context: Context): PermissionCheckResult {
        val criticalTypes = listOf(
            PermissionType.ACCESSIBILITY,
            PermissionType.NOTIFICATION_LISTENER,
            PermissionType.BATTERY
        )

        val permissions = mapOf(
            PermissionType.ACCESSIBILITY to checkAccessibilityPermission(context),
            PermissionType.NOTIFICATION_LISTENER to checkNotificationListenerPermission(context),
            PermissionType.BATTERY to checkBatteryOptimization(context)
        )

        val missing = permissions.filter { !it.value }.keys.toList()

        return PermissionCheckResult(
            allGranted = missing.isEmpty(),
            permissions = permissions,
            missingPermissions = missing
        )
    }

    /**
     * Get a human-readable name for a permission type.
     */
    fun getPermissionName(type: PermissionType): String {
        return when (type) {
            PermissionType.LOCATION -> "Location Access"
            PermissionType.NOTIFICATIONS -> "Notifications"
            PermissionType.PHONE_SMS -> "Phone & SMS"
            PermissionType.ACCESSIBILITY -> "Accessibility Service"
            PermissionType.NOTIFICATION_LISTENER -> "Notification Listener"
            PermissionType.OVERLAY -> "Display Over Apps"
            PermissionType.STORAGE -> "Storage Access"
            PermissionType.BATTERY -> "Battery Optimization"
            PermissionType.CAMERA -> "Camera"
            PermissionType.CONTACTS -> "Contacts"
        }
    }

    fun checkLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkPhoneSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkAccessibilityPermission(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val packageName = context.packageName
        return enabledServices.contains(packageName)
    }

    fun checkNotificationListenerPermission(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val packageName = context.packageName
        return enabledListeners.contains(packageName)
    }

    fun checkOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun checkStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkBatteryOptimization(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    fun checkCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

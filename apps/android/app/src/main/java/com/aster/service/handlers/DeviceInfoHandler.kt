package com.aster.service.handlers

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*
import java.util.Locale
import java.util.TimeZone

class DeviceInfoHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf(
        "get_device_info",
        "get_battery",
        "get_location"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "get_device_info" -> getDeviceInfo()
            "get_battery" -> getBatteryInfo()
            "get_location" -> getLocation()
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun getDeviceInfo(): CommandResult {
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        // Get memory info
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        // Get storage info
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorageGB = statFs.totalBytes / (1024L * 1024L * 1024L)
        val availableStorageGB = statFs.availableBytes / (1024L * 1024L * 1024L)

        // Get display refresh rate
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        val refreshRate = windowManager.defaultDisplay.refreshRate

        val data = buildJsonObject {
            put("deviceId", deviceId)
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("device", Build.DEVICE)
            put("product", Build.PRODUCT)
            put("hardware", Build.HARDWARE)
            put("board", Build.BOARD)
            put("display", Build.DISPLAY)
            put("fingerprint", Build.FINGERPRINT)
            put("osVersion", Build.VERSION.RELEASE)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("buildId", Build.ID)
            put("bootloader", Build.BOOTLOADER)
            put("isEmulator", isEmulator())

            // NEW: CPU/ABI info
            put("cpuAbi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
            putJsonArray("supportedAbis") {
                Build.SUPPORTED_ABIS.forEach { add(it) }
            }

            // NEW: Build info
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                put("securityPatch", Build.VERSION.SECURITY_PATCH)
            } else {
                put("securityPatch", JsonNull)
            }
            put("buildType", Build.TYPE)
            put("buildTags", Build.TAGS)
            val radioVersion = Build.getRadioVersion()
            if (radioVersion != null) {
                put("radioVersion", radioVersion)
            } else {
                put("radioVersion", JsonNull)
            }

            // NEW: Memory info (in MB)
            put("totalRam", memInfo.totalMem / (1024L * 1024L))
            put("availableRam", memInfo.availMem / (1024L * 1024L))

            // NEW: Storage info (in GB)
            put("totalStorage", totalStorageGB)
            put("availableStorage", availableStorageGB)

            // NEW: Display refresh rate
            put("screenRefreshRate", refreshRate)

            // NEW: Battery capacity
            val batteryCapacity = getBatteryCapacity()
            if (batteryCapacity != null) {
                put("batteryCapacity", batteryCapacity)
            } else {
                put("batteryCapacity", JsonNull)
            }

            // NEW: System info
            put("uptimeMillis", SystemClock.elapsedRealtime())
            put("timezone", TimeZone.getDefault().id)
            put("locale", Locale.getDefault().toString())

            // Screen info
            val displayMetrics = context.resources.displayMetrics
            putJsonObject("screen") {
                put("widthPixels", displayMetrics.widthPixels)
                put("heightPixels", displayMetrics.heightPixels)
                put("density", displayMetrics.density)
                put("densityDpi", displayMetrics.densityDpi)
            }
        }

        return CommandResult.success(data)
    }

    private fun getBatteryCapacity(): Int? {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = (method.invoke(powerProfile) as Double).toInt()
            // Return null if capacity is 0 or negative (unavailable)
            if (capacity > 0) capacity else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getBatteryInfo(): CommandResult {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        if (batteryStatus == null) {
            return CommandResult.failure("Could not get battery status")
        }

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            -1
        }

        val status = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }

        val plugged = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> "unplugged"
        }

        val health = when (batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            else -> "unknown"
        }

        val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
        val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "unknown"

        val data = buildJsonObject {
            put("level", percentage)
            put("status", status)
            put("plugged", plugged)
            put("health", health)
            put("temperature", temperature)
            put("voltage", voltage)
            put("technology", technology)
            put("isCharging", status == "charging" || status == "full")
        }

        return CommandResult.success(data)
    }

    private fun getLocation(): CommandResult {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check if location is enabled
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            return CommandResult.failure("Location services are disabled")
        }

        // Try to get last known location
        val location: Location? = try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else {
                null
            }
        } catch (e: SecurityException) {
            null
        }

        if (location == null) {
            return CommandResult.failure("Location not available. Ensure location permission is granted.")
        }

        val data = buildJsonObject {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("altitude", location.altitude)
            put("accuracy", location.accuracy)
            put("bearing", location.bearing)
            put("speed", location.speed)
            put("provider", location.provider ?: "unknown")
            put("timestamp", location.time)
        }

        return CommandResult.success(data)
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }
}

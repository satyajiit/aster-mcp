package com.aster.service.handlers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Base64
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream

class PackageHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf(
        "list_packages"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "list_packages" -> listPackages(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun encodeIconBase64(packageManager: PackageManager, packageName: String): String? {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
            val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun listPackages(command: Command): CommandResult {
        val includeSystem = command.params?.get("includeSystem")?.jsonPrimitive?.booleanOrNull ?: false
        val includeIcons = command.params?.get("includeIcons")?.jsonPrimitive?.booleanOrNull ?: false

        val packageManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

        val filteredPackages = if (includeSystem) {
            packages
        } else {
            packages.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        }

        val packageList = buildJsonArray {
            filteredPackages.sortedBy {
                packageManager.getApplicationLabel(it).toString().lowercase()
            }.forEach { appInfo ->
                addJsonObject {
                    put("packageName", appInfo.packageName)
                    put("name", packageManager.getApplicationLabel(appInfo).toString())
                    put("isSystemApp", (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                    put("enabled", appInfo.enabled)
                    if (includeIcons) {
                        val iconData = encodeIconBase64(packageManager, appInfo.packageName)
                        if (iconData != null) put("icon", iconData) else put("icon", JsonNull)
                    }
                    put("targetSdkVersion", appInfo.targetSdkVersion)
                    put("minSdkVersion", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 0)
                    put("sourceDir", appInfo.sourceDir)
                    put("dataDir", appInfo.dataDir)

                    // Try to get version info
                    try {
                        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.getPackageInfo(appInfo.packageName, PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(appInfo.packageName, 0)
                        }
                        put("versionName", packageInfo.versionName ?: "unknown")
                        put("versionCode", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode.toLong()
                        })
                        put("firstInstallTime", packageInfo.firstInstallTime)
                        put("lastUpdateTime", packageInfo.lastUpdateTime)
                    } catch (e: Exception) {
                        put("versionName", "unknown")
                        put("versionCode", 0L)
                    }
                }
            }
        }

        val data = buildJsonObject {
            put("count", filteredPackages.size)
            put("includeSystem", includeSystem)
            put("packages", packageList)
        }

        return CommandResult.success(data)
    }
}

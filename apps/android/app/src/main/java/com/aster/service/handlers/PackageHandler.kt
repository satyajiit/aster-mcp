package com.aster.service.handlers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

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

    private fun listPackages(command: Command): CommandResult {
        val includeSystem = command.params?.get("includeSystem")?.jsonPrimitive?.booleanOrNull ?: false

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

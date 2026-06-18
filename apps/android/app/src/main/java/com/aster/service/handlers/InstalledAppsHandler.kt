package com.aster.service.handlers

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

/**
 * Handler for installed-app enumeration. Backs the OpenAlly kernel's
 * `apps.list` host-call (cortex-tools-apps device-apps sync): pages every
 * installed package with its label, version, install/update times, system
 * flag, on-disk sizes (StorageStatsManager), declared permissions, and an
 * optional `last_used` (UsageStatsManager, null when "Usage access" is not
 * granted). Read-only; uninstall is deliberately out of scope.
 */
class InstalledAppsHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf("list_installed_apps")

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "list_installed_apps" -> listInstalledApps(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun listInstalledApps(command: Command): CommandResult {
        // Accept both casings. Over IPC (com.aster on-device companion) RN
        // sends camelCase `includeSystem` (AsterDeviceManager); over MCP/WS the
        // handler.ts dispatch re-maps to snake_case `include_system`. Every
        // sibling handler reads camelCase, so prefer it and fall back to snake.
        val includeSystem =
            (command.params?.get("includeSystem") ?: command.params?.get("include_system"))
                ?.jsonPrimitive?.booleanOrNull ?: false
        val cursor = command.params?.get("cursor")?.jsonPrimitive?.intOrNull ?: 0
        val limit = (command.params?.get("limit")?.jsonPrimitive?.intOrNull ?: 100)
            .coerceIn(1, 500)

        return try {
            val pm = context.packageManager
            val all = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            val filtered = if (includeSystem) {
                all
            } else {
                all.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            }
            // Stable cursor order: lowercase label, package as tie-break.
            val sorted = filtered.sortedWith(
                compareBy({ pm.getApplicationLabel(it).toString().lowercase() }, { it.packageName })
            )

            // Page: fetch limit+1 to detect hasMore.
            val window = sorted.drop(cursor).take(limit + 1)
            val hasMore = window.size > limit
            val pageInfos = if (hasMore) window.take(limit) else window

            val usage = lastUsedMap()  // emptyMap() when Usage access not granted
            val storageStats = context.getSystemService(StorageStatsManager::class.java)
            val storageUuid = StorageManager.UUID_DEFAULT

            val apps = buildJsonArray {
                pageInfos.forEach { appInfo ->
                    addJsonObject {
                        put("package", appInfo.packageName)
                        put("label", pm.getApplicationLabel(appInfo).toString())
                        put("system", (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)

                        // Version + install/update times.
                        try {
                            val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pm.getPackageInfo(
                                    appInfo.packageName,
                                    PackageManager.PackageInfoFlags.of(
                                        PackageManager.GET_PERMISSIONS.toLong()
                                    )
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                            }
                            put("version", pi.versionName ?: "unknown")
                            put("install_time", pi.firstInstallTime)
                            put("update_time", pi.lastUpdateTime)
                            put("permissions", buildJsonArray {
                                pi.requestedPermissions?.forEach { add(it) }
                            })
                        } catch (e: Exception) {
                            put("version", "unknown")
                            put("install_time", 0L)
                            put("update_time", 0L)
                            put("permissions", buildJsonArray { })
                        }

                        // Sizes. StorageStatsManager needs the special op
                        // (GET_PACKAGE_SIZE / Usage access on newer OS); a
                        // SecurityException here degrades the three size
                        // fields to 0 rather than failing the whole page.
                        try {
                            val stats = storageStats.queryStatsForPackage(
                                storageUuid, appInfo.packageName, Process.myUserHandle()
                            )
                            put("size_app", stats.appBytes)
                            put("size_data", stats.dataBytes)
                            put("size_cache", stats.cacheBytes)
                        } catch (e: Exception) {
                            put("size_app", 0L)
                            put("size_data", 0L)
                            put("size_cache", 0L)
                        }

                        val lastUsed = usage[appInfo.packageName]
                        if (lastUsed != null) put("last_used", lastUsed) else put("last_used", JsonNull)
                    }
                }
            }

            CommandResult.success(buildJsonObject {
                put("apps", apps)
                put("next_cursor", if (hasMore) JsonPrimitive(cursor + limit) else JsonNull)
                put("has_more", hasMore)
                put("count", pageInfos.size)
            })
        } catch (e: SecurityException) {
            CommandResult.failure("Package query permission not granted (QUERY_ALL_PACKAGES).")
        } catch (e: Exception) {
            CommandResult.failure("Failed to list installed apps: ${e.message}")
        }
    }

    /**
     * package → last-used epoch-ms, over the last ~365 days. Empty when the
     * user hasn't granted "Usage access" (UsageStats op != MODE_ALLOWED) —
     * the caller then emits last_used:null for every app.
     */
    private fun lastUsedMap(): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - 365L * 24 * 60 * 60 * 1000
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            val out = HashMap<String, Long>()
            stats?.forEach { s ->
                val prev = out[s.packageName]
                if (s.lastTimeUsed > 0 && (prev == null || s.lastTimeUsed > prev)) {
                    out[s.packageName] = s.lastTimeUsed
                }
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun hasUsageAccess(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
}

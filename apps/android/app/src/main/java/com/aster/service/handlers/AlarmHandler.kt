package com.aster.service.handlers

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

class AlarmHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf(
        "get_alarms",
        "set_alarm",
        "dismiss_alarm",
        "delete_alarm"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "get_alarms" -> getAlarms()
            "set_alarm" -> setAlarm(command)
            "dismiss_alarm" -> dismissAlarm()
            "delete_alarm" -> deleteAlarm(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun getAlarms(): CommandResult {
        val alarms = mutableListOf<JsonObject>()
        var sourceProvider = ""

        // Try multiple clock content providers (stock Android + Samsung)
        val providers = listOf(
            "content://com.android.deskclock/alarm",
            "content://com.sec.android.app.clockpackage/alarm"
        )

        for (providerUri in providers) {
            if (alarms.isNotEmpty()) break
            try {
                val uri = android.net.Uri.parse(providerUri)
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val alarm = buildJsonObject {
                            for (i in 0 until cursor.columnCount) {
                                val colName = cursor.getColumnName(i)
                                val value = cursor.getString(i)
                                if (value != null) put(colName, value)
                            }
                        }
                        alarms.add(alarm)
                    }
                }
                if (alarms.isNotEmpty()) sourceProvider = providerUri
            } catch (_: Exception) {
                // This provider not available
            }
        }

        if (alarms.isNotEmpty()) {
            return CommandResult.success(buildJsonObject {
                put("source", sourceProvider)
                put("alarms", buildJsonArray { alarms.forEach { add(it) } })
                put("count", alarms.size)
            })
        }

        // Fallback: AlarmManager next alarm info
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextAlarm = alarmManager.nextAlarmClock

        if (nextAlarm != null) {
            val triggerTime = nextAlarm.triggerTime
            return CommandResult.success(buildJsonObject {
                put("source", "alarm_manager")
                put("nextAlarm", buildJsonObject {
                    put("triggerTimeMs", triggerTime)
                    put("triggerTimeFormatted", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(triggerTime)))
                })
                put("note", "Only the next upcoming alarm is available via this method. Stock clock provider not accessible on this device.")
            })
        }

        return CommandResult.success(buildJsonObject {
            put("source", "none")
            put("alarms", buildJsonArray {})
            put("count", 0)
            put("note", "No alarms found. Stock clock provider not accessible and no upcoming alarms scheduled.")
        })
    }

    private fun setAlarm(command: Command): CommandResult {
        val hour = command.params?.get("hour")?.jsonPrimitive?.intOrNull
            ?: return CommandResult.failure("Missing 'hour' parameter (0-23)")
        val minute = command.params?.get("minute")?.jsonPrimitive?.intOrNull
            ?: return CommandResult.failure("Missing 'minute' parameter (0-59)")
        val message = command.params?.get("message")?.jsonPrimitive?.contentOrNull
        val days = command.params?.get("days")?.let { element ->
            if (element is JsonArray) {
                element.map { it.jsonPrimitive.int }
            } else null
        }
        val skipUi = command.params?.get("skipUi")?.jsonPrimitive?.booleanOrNull ?: true

        if (hour !in 0..23) return CommandResult.failure("'hour' must be 0-23")
        if (minute !in 0..59) return CommandResult.failure("'minute' must be 0-59")

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)

            if (message != null) {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
            }

            if (days != null) {
                putExtra(AlarmClock.EXTRA_DAYS, ArrayList(days))
            }

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            CommandResult.success(buildJsonObject {
                put("set", true)
                put("hour", hour)
                put("minute", minute)
                if (message != null) put("message", message)
                if (days != null) put("days", buildJsonArray { days.forEach { add(it) } })
                put("skipUi", skipUi)
            })
        } catch (e: Exception) {
            CommandResult.failure("Failed to set alarm: ${e.message}")
        }
    }

    private fun dismissAlarm(): CommandResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return CommandResult.failure("Dismiss alarm requires Android 6.0 (API 23) or higher")
        }

        // Try standard ACTION_DISMISS_ALARM first
        val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check if any app can handle this intent
        if (intent.resolveActivity(context.packageManager) != null) {
            return try {
                context.startActivity(intent)
                CommandResult.success(buildJsonObject {
                    put("dismissed", true)
                    put("note", "Dismissed the next firing alarm")
                })
            } catch (e: Exception) {
                CommandResult.failure("Failed to dismiss alarm: ${e.message}")
            }
        }

        // Samsung fallback: try targeting Samsung Clock directly
        val samsungIntent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            setPackage("com.sec.android.app.clockpackage")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(samsungIntent)
            CommandResult.success(buildJsonObject {
                put("dismissed", true)
                put("note", "Dismissed via Samsung Clock")
            })
        } catch (e: Exception) {
            CommandResult.failure("Dismiss not supported on this device. ACTION_DISMISS_ALARM only works when an alarm is currently ringing. To remove a saved alarm, use delete_alarm with the alarm ID from get_alarms.")
        }
    }

    private fun deleteAlarm(command: Command): CommandResult {
        val alarmId = command.params?.get("alarmId")?.jsonPrimitive?.contentOrNull

        // Try Samsung content provider
        val samsungProviders = listOf(
            "content://com.sec.android.app.clockpackage/alarm",
            "content://com.android.deskclock/alarm"
        )

        for (providerUri in samsungProviders) {
            try {
                val uri = android.net.Uri.parse(providerUri)

                if (alarmId != null) {
                    // Delete specific alarm by ID
                    val deleteUri = android.net.Uri.withAppendedPath(uri, alarmId)
                    val deleted = context.contentResolver.delete(deleteUri, null, null)
                    if (deleted > 0) {
                        return CommandResult.success(buildJsonObject {
                            put("deleted", true)
                            put("alarmId", alarmId)
                            put("provider", providerUri)
                        })
                    }
                } else {
                    return CommandResult.failure("Missing 'alarmId' parameter. Use get_alarms to find alarm IDs first.")
                }
            } catch (_: Exception) {
                // This provider not available, try next
            }
        }

        return CommandResult.failure("Could not delete alarm. The clock app on this device does not expose a content provider for alarm deletion. Try opening the clock app manually.")
    }
}

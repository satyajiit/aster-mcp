package com.aster.service.handlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class IntentHandler(
    private val context: Context
) : CommandHandler {

    override fun supportedActions() = listOf(
        "launch_intent",
        "show_toast",
        "make_call"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "launch_intent" -> launchIntent(command)
            "show_toast" -> showToast(command)
            "make_call" -> makeCall(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun launchIntent(command: Command): CommandResult {
        val packageName = command.params?.get("package")?.jsonPrimitive?.contentOrNull
        val action = command.params?.get("action")?.jsonPrimitive?.contentOrNull
        val data = command.params?.get("data")?.jsonPrimitive?.contentOrNull

        // If only package name provided, launch the app
        if (packageName != null && action == null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return CommandResult.success(buildJsonObject {
                    put("launched", true)
                    put("package", packageName)
                })
            } else {
                return CommandResult.failure("Cannot launch package: $packageName")
            }
        }

        // Build custom intent
        val intent = Intent().apply {
            action?.let { setAction(it) }
            data?.let { setData(Uri.parse(it)) }
            packageName?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Add extras if provided
            command.params?.get("extras")?.jsonObject?.forEach { (key, value) ->
                when (value) {
                    is JsonPrimitive -> {
                        when {
                            value.isString -> putExtra(key, value.content)
                            value.booleanOrNull != null -> putExtra(key, value.boolean)
                            value.intOrNull != null -> putExtra(key, value.int)
                            value.longOrNull != null -> putExtra(key, value.long)
                            value.floatOrNull != null -> putExtra(key, value.float)
                        }
                    }
                    else -> putExtra(key, value.toString())
                }
            }
        }

        return try {
            context.startActivity(intent)
            CommandResult.success(buildJsonObject {
                put("launched", true)
                put("action", action)
                put("data", data)
                put("package", packageName)
            })
        } catch (e: Exception) {
            CommandResult.failure("Failed to launch intent: ${e.message}")
        }
    }

    private suspend fun showToast(command: Command): CommandResult = withContext(Dispatchers.Main) {
        val message = command.params?.get("message")?.jsonPrimitive?.contentOrNull
            ?: return@withContext CommandResult.failure("Missing 'message' parameter")

        val duration = when (command.params?.get("duration")?.jsonPrimitive?.contentOrNull) {
            "long" -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }

        Toast.makeText(context, message, duration).show()

        CommandResult.success(buildJsonObject {
            put("shown", true)
            put("message", message)
        })
    }

    private fun makeCall(command: Command): CommandResult {
        val number = command.params?.get("number")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'number' parameter")

        // Clean the phone number
        val cleanNumber = number.replace(Regex("[^0-9+*#]"), "")

        if (cleanNumber.isEmpty()) {
            return CommandResult.failure("Invalid phone number")
        }

        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$cleanNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            CommandResult.success(buildJsonObject {
                put("calling", true)
                put("number", cleanNumber)
            })
        } catch (e: SecurityException) {
            CommandResult.failure("CALL_PHONE permission not granted")
        } catch (e: Exception) {
            CommandResult.failure("Failed to make call: ${e.message}")
        }
    }
}

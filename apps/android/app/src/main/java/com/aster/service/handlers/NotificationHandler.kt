package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.AsterNotificationListenerService
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

/**
 * Handler for notification-related commands.
 * Handles reading notifications and posting local notifications.
 */
class NotificationHandler : CommandHandler {

    override fun supportedActions() = listOf(
        "read_notifications",
        "post_notification",
        "dismiss_notification",
        "dismiss_all_notifications"
    )

    override suspend fun handle(command: Command): CommandResult {
        val service = AsterNotificationListenerService.getInstance()

        return when (command.action) {
            "read_notifications" -> readNotifications(service, command)
            "post_notification" -> postNotification(service, command)
            "dismiss_notification" -> dismissNotification(service, command)
            "dismiss_all_notifications" -> dismissAllNotifications(service)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun readNotifications(
        service: AsterNotificationListenerService?,
        command: Command
    ): CommandResult {
        if (service == null) {
            return CommandResult.failure("Notification listener not enabled. Please enable it in Settings > Apps > Special app access > Notification access > Aster")
        }

        val limit = command.params?.get("limit")?.jsonPrimitive?.intOrNull ?: 50
        val includeHistory = command.params?.get("includeHistory")?.jsonPrimitive?.booleanOrNull ?: false

        val activeNotifications = service.getActiveNotifications(limit)

        val result = buildJsonObject {
            put("active", activeNotifications)
            put("count", activeNotifications.size)

            if (includeHistory) {
                put("history", service.getNotificationHistory(limit))
            }
        }

        return CommandResult.success(result)
    }

    private fun postNotification(
        service: AsterNotificationListenerService?,
        command: Command
    ): CommandResult {
        // For posting notifications, we don't strictly need the listener service
        // We can use the regular NotificationManager
        val title = command.params?.get("title")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'title' parameter")

        val body = command.params?.get("body")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'body' parameter")

        // Parse actions if provided
        val actionsJson = command.params?.get("actions")
        val actions: List<AsterNotificationListenerService.NotificationAction>? =
            if (actionsJson != null && actionsJson is JsonArray) {
                actionsJson.mapNotNull { actionJson ->
                    if (actionJson is JsonObject) {
                        val id = actionJson["id"]?.jsonPrimitive?.contentOrNull
                        val label = actionJson["label"]?.jsonPrimitive?.contentOrNull
                        if (id != null && label != null) {
                            AsterNotificationListenerService.NotificationAction(id, label)
                        } else null
                    } else null
                }
            } else null

        // If notification listener is available, use it; otherwise fall back
        if (service != null) {
            val notificationId = service.postNotification(title, body, actions)
            return CommandResult.success(buildJsonObject {
                put("notificationId", notificationId)
                put("title", title)
                put("body", body)
                put("posted", true)
            })
        } else {
            return CommandResult.failure("Notification service not available")
        }
    }

    private fun dismissNotification(
        service: AsterNotificationListenerService?,
        command: Command
    ): CommandResult {
        if (service == null) {
            return CommandResult.failure("Notification listener not enabled")
        }

        val key = command.params?.get("key")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'key' parameter")

        val success = service.dismissNotification(key)
        return if (success) {
            CommandResult.success(buildJsonObject {
                put("key", key)
                put("dismissed", true)
            })
        } else {
            CommandResult.failure("Failed to dismiss notification")
        }
    }

    private fun dismissAllNotifications(
        service: AsterNotificationListenerService?
    ): CommandResult {
        if (service == null) {
            return CommandResult.failure("Notification listener not enabled")
        }

        val success = service.dismissAllNotifications()
        return if (success) {
            CommandResult.success(buildJsonObject {
                put("dismissedAll", true)
            })
        } else {
            CommandResult.failure("Failed to dismiss all notifications")
        }
    }
}

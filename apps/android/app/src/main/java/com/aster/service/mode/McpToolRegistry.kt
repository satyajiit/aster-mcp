package com.aster.service.mode

import com.aster.data.local.db.ToolCallLogger
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Bridges CommandHandlers into MCP tool definitions.
 * Converts the 16 handler implementations (40+ actions) into MCP addTool calls.
 */
object McpToolRegistry {

    private val toolSchemas = mapOf(
        "get_device_info" to ToolDef("Get device information", emptyMap()),
        "get_battery_status" to ToolDef("Get battery status", emptyMap()),
        "get_network_info" to ToolDef("Get network information", emptyMap()),
        "get_location" to ToolDef("Get device GPS location", emptyMap()),
        "list_files" to ToolDef(
            "List files in directory",
            mapOf("path" to PropDef("string", "Directory path"))
        ),
        "read_file" to ToolDef(
            "Read file contents",
            mapOf("path" to PropDef("string", "File path"))
        ),
        "write_file" to ToolDef(
            "Write content to file",
            mapOf(
                "path" to PropDef("string", "File path"),
                "content" to PropDef("string", "File content")
            )
        ),
        "delete_file" to ToolDef("Delete a file", mapOf("path" to PropDef("string", "File path"))),
        "list_apps" to ToolDef("List installed applications", emptyMap()),
        "launch_app" to ToolDef(
            "Launch an application",
            mapOf("packageName" to PropDef("string", "App package name"))
        ),
        "get_clipboard" to ToolDef("Get clipboard content", emptyMap()),
        "set_clipboard" to ToolDef(
            "Set clipboard content",
            mapOf("text" to PropDef("string", "Text to copy"))
        ),
        "play_media" to ToolDef("Play media file", mapOf("uri" to PropDef("string", "Media URI"))),
        "stop_media" to ToolDef("Stop media playback", emptyMap()),
        "run_shell" to ToolDef(
            "Run shell command",
            mapOf("command" to PropDef("string", "Shell command"))
        ),
        "launch_intent" to ToolDef(
            "Launch Android intent",
            mapOf("action" to PropDef("string", "Intent action"))
        ),
        "get_accessibility_info" to ToolDef("Get accessibility tree info", emptyMap()),
        "perform_gesture" to ToolDef(
            "Perform accessibility gesture",
            mapOf("gesture" to PropDef("string", "Gesture type"))
        ),
        "get_notifications" to ToolDef("Get active notifications", emptyMap()),
        "dismiss_notification" to ToolDef(
            "Dismiss a notification",
            mapOf("key" to PropDef("string", "Notification key"))
        ),
        "send_sms" to ToolDef(
            "Send SMS message",
            mapOf(
                "to" to PropDef("string", "Phone number"),
                "body" to PropDef("string", "Message body")
            )
        ),
        "read_sms" to ToolDef("Read SMS messages", emptyMap()),
        "show_overlay" to ToolDef(
            "Show screen overlay",
            mapOf("text" to PropDef("string", "Overlay text"))
        ),
        "hide_overlay" to ToolDef("Hide screen overlay", emptyMap()),
        "get_storage_info" to ToolDef("Get storage information", emptyMap()),
        "get_volume" to ToolDef("Get volume levels", emptyMap()),
        "set_volume" to ToolDef(
            "Set volume level",
            mapOf(
                "stream" to PropDef("string", "Audio stream"),
                "level" to PropDef("integer", "Volume level")
            )
        ),
        "get_contacts" to ToolDef("Get contacts list", emptyMap()),
        "search_contacts" to ToolDef(
            "Search contacts",
            mapOf("query" to PropDef("string", "Search query"))
        ),
        "set_alarm" to ToolDef(
            "Set an alarm",
            mapOf(
                "hour" to PropDef("integer", "Hour (0-23)"),
                "minute" to PropDef("integer", "Minute (0-59)")
            )
        ),
        "take_photo" to ToolDef("Take a photo", emptyMap()),
        "take_screenshot" to ToolDef("Take a screenshot", emptyMap()),
    )

    fun registerTools(
        server: Server,
        commandHandlers: Map<String, CommandHandler>,
        toolCallLogger: ToolCallLogger
    ) {
        commandHandlers.keys.forEach { action ->
            val handler = commandHandlers[action] ?: return@forEach
            val def = toolSchemas[action]

            val schema = if (def != null && def.params.isNotEmpty()) {
                ToolSchema(
                    properties = buildJsonObject {
                        def.params.forEach { (name, prop) ->
                            putJsonObject(name) {
                                put("type", prop.type)
                                put("description", prop.description)
                            }
                        }
                    },
                    required = def.params.filter { it.key != "optional" }.keys.toList()
                )
            } else {
                ToolSchema(properties = buildJsonObject {}, required = emptyList())
            }

            server.addTool(
                name = action,
                description = def?.description ?: "Execute $action",
                inputSchema = schema
            ) { request ->
                val params = request.arguments?.mapValues { it.value }

                val command = Command(
                    type = "command",
                    id = java.util.UUID.randomUUID().toString(),
                    action = action,
                    params = params
                )

                val startTime = System.currentTimeMillis()
                try {
                    val result = handler.handle(command)
                    val duration = System.currentTimeMillis() - startTime
                    toolCallLogger.log(
                        action = action,
                        connectionType = "LOCAL_MCP",
                        success = result.success,
                        durationMs = duration,
                        errorMessage = result.error
                    )
                    if (result.success) {
                        val text = result.data?.toString() ?: "Success"
                        CallToolResult(content = listOf(TextContent(text)), isError = false)
                    } else {
                        CallToolResult(
                            content = listOf(TextContent(result.error ?: "Unknown error")),
                            isError = true
                        )
                    }
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    toolCallLogger.log(
                        action = action,
                        connectionType = "LOCAL_MCP",
                        success = false,
                        durationMs = duration,
                        errorMessage = e.message
                    )
                    CallToolResult(
                        content = listOf(TextContent("Error: ${e.message}")),
                        isError = true
                    )
                }
            }
        }
    }

    private data class ToolDef(val description: String, val params: Map<String, PropDef>)
    private data class PropDef(val type: String, val description: String)
}

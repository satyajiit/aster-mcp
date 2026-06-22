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
        // OpenAlly App Builder "host-dir": read/list a file within an owner-
        // approved folder (SAF tree URI or path) for a served kind=dir surface.
        "files.read" to ToolDef(
            "Read a file within an owner-approved host folder (App Builder host-dir)",
            mapOf(
                "host_dir" to PropDef("string", "Owner-approved folder: a SAF tree URI (content://…) or a filesystem path"),
                "path" to PropDef("string", "File path relative to host_dir (e.g. index.html, css/site.css)")
            )
        ),
        "files.list" to ToolDef(
            "List entries within an owner-approved host folder (App Builder host-dir)",
            mapOf(
                "host_dir" to PropDef("string", "Owner-approved folder: a SAF tree URI (content://…) or a filesystem path"),
                "path" to PropDef("string", "Folder path relative to host_dir (defaults to the root)")
            ),
            required = listOf("host_dir")
        ),
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
        "observe" to ToolDef(
            "Observe the current screen as a flat indexed list of actionable elements " +
                "with stable refs. Optional params (all optional): mode " +
                "(actionable|text|full, default actionable), searchText (narrow to " +
                "matching elements), maxElements (token-budget cap), ocr (boolean: omit " +
                "for auto — OCR only when the a11y tree is sparse; true forces on-device " +
                "OCR pseudo-elements, false disables them).",
            emptyMap()
        ),
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
        "count_sms" to ToolDef("Count SMS messages in a date window", emptyMap()),
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
        "take_screenshot" to ToolDef(
            "Take a screenshot (JPEG). Set annotate=true to overlay numbered Set-of-Marks boxes.",
            mapOf(
                "annotate" to PropDef("boolean", "Overlay numbered boxes aligned to observe refs")
            ),
            // annotate is optional — the handler defaults it to false. Mark nothing required so
            // a plain {} screenshot call is valid (matches the ref-action `required = emptyList()`
            // convention used elsewhere in this registry).
            required = emptyList()
        ),
        // -- P3: synchronization (SPEC §3.3) --
        "wait_for_idle" to ToolDef(
            "Wait until the screen stops changing",
            mapOf(
                "quietMs" to PropDef("integer", "Quiet window in ms before idle (default 500)"),
                "timeout" to PropDef("integer", "Max wait in ms (default 5000)")
            ),
            // Both optional in practice; the handler applies server-side defaults.
            required = emptyList()
        ),
        "wait_for" to ToolDef(
            "Wait until an element appears or disappears",
            mapOf(
                "text" to PropDef("string", "Element text substring to wait for"),
                "viewId" to PropDef("string", "Exact viewId to wait for"),
                "role" to PropDef("string", "Element role to wait for"),
                "gone" to PropDef("boolean", "Wait for the element to DISAPPEAR (default false)"),
                "timeout" to PropDef("integer", "Max wait in ms (default 5000)")
            ),
            // All optional; the handler enforces the at-least-one-target rule at runtime.
            // required = emptyList() dodges the generic "all params required" default that
            // would otherwise demand text+viewId+role+gone+timeout together.
            required = emptyList()
        ),
        // -- P2: ref-addressed screen actions (SPEC §3.2). ref/x/y are mutually exclusive,
        // so NONE of their params are marked required (each ref tool passes an empty
        // `required` list); marking ref/x/y/snapshot_id required would reject valid
        // ref-only/coord-only calls (SPEC §7, Task 9 done-bar).
        "tap" to ToolDef(
            "Tap an element by ref (preferred) or by x,y coordinates",
            mapOf(
                "ref" to PropDef("string", "Element ref from screen_observe"),
                "snapshot_id" to PropDef("string", "Snapshot id from screen_observe"),
                "x" to PropDef("number", "X coordinate (coordinate fallback)"),
                "y" to PropDef("number", "Y coordinate (coordinate fallback)")
            ),
            required = emptyList()
        ),
        "long_press" to ToolDef(
            "Long-press an element by ref or coordinates",
            mapOf(
                "ref" to PropDef("string", "Element ref from screen_observe"),
                "snapshot_id" to PropDef("string", "Snapshot id"),
                "x" to PropDef("number", "X coordinate"),
                "y" to PropDef("number", "Y coordinate"),
                "duration" to PropDef("integer", "Press duration ms (min 500)")
            ),
            required = emptyList()
        ),
        "set_text" to ToolDef(
            "Set text into a specific field by ref",
            mapOf(
                "ref" to PropDef("string", "Editable element ref"),
                "text" to PropDef("string", "Text to enter"),
                "snapshot_id" to PropDef("string", "Snapshot id"),
                "mode" to PropDef("string", "replace (default) | append"),
                "submit" to PropDef("boolean", "Press IME Enter after typing")
            ),
            required = emptyList()
        ),
        "set_toggle" to ToolDef(
            "Set a switch/checkbox on or off by ref",
            mapOf(
                "ref" to PropDef("string", "Checkable element ref"),
                "on" to PropDef("boolean", "Desired state"),
                "snapshot_id" to PropDef("string", "Snapshot id")
            ),
            required = emptyList()
        ),
        "perform" to ToolDef(
            "Invoke an accessibility action on an element by ref",
            mapOf(
                "ref" to PropDef("string", "Element ref"),
                "action" to PropDef("string", "Action name (e.g. expand, collapse, dismiss, select)"),
                "snapshot_id" to PropDef("string", "Snapshot id")
            ),
            required = emptyList()
        ),
        "press_key" to ToolDef(
            "Press a hardware/IME key",
            mapOf("key" to PropDef("string", "Key name: ENTER, BACK, TAB, DEL, DPAD_UP…"))
        ),
        // Screen Control /goal P7 — schemas for the two remaining SPEC §3.2
        // action names that the catalog already lists but the MCP registry did
        // not yet declare (the embedded-MCP transport now exposes them too).
        "scroll" to ToolDef(
            "Scroll a container by ref (or the auto-picked main scrollable); " +
                "untilText repeats scrolling until that text appears (scroll-to-find)",
            mapOf(
                "direction" to PropDef("string", "up | down | left | right"),
                "ref" to PropDef("string", "Scrollable container ref (optional; auto-picked if omitted)"),
                "amount" to PropDef("string", "page (default) | halfpage | toEdge"),
                "untilText" to PropDef("string", "Scroll-to-find: repeat until this text appears"),
                "snapshot_id" to PropDef("string", "Snapshot id from screen_observe")
            ),
            // direction is the only meaningful requirement; the handler validates it at runtime.
            required = emptyList()
        ),
        "global_action" to ToolDef(
            "Perform a system global action: BACK, HOME, RECENTS, NOTIFICATIONS, " +
                "QUICK_SETTINGS, POWER_DIALOG, LOCK_SCREEN, and IME actions",
            mapOf(
                "action" to PropDef("string", "BACK | HOME | RECENTS | NOTIFICATIONS | QUICK_SETTINGS | POWER_DIALOG | LOCK_SCREEN | …")
            )
        ),
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
                    // An explicit `required` list (incl. empty, used by the ref-addressed
                    // screen actions whose ref/x/y/snapshot_id are mutually exclusive and
                    // thus all optional) wins; otherwise default to all params required.
                    required = def.required ?: def.params.keys.toList()
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
                toolCallLogger.onToolStarted(action, "LOCAL_MCP")
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

    private data class ToolDef(
        val description: String,
        val params: Map<String, PropDef>,
        // null → all params required (default convention); an explicit list (incl. empty)
        // overrides it, e.g. ref-addressed actions mark nothing required.
        val required: List<String>? = null
    )
    private data class PropDef(val type: String, val description: String)
}

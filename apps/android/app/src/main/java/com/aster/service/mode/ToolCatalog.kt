package com.aster.service.mode

/**
 * Static catalog mapping action names to meaningful categories and descriptions.
 * Used by both IPC and MCP modes for dashboard display.
 */
object ToolCatalog {

    data class ToolEntry(
        val displayName: String,
        val description: String,
        val category: String
    )

    /** Ordered list of categories for display (controls section ordering). */
    val categoryOrder = listOf(
        "Screen Control",
        "Device",
        "Files",
        "Camera",
        "Communication",
        "Notifications",
        "Media",
        "Storage",
        "Apps",
        "System",
        "Overlays",
        "Alarms"
    )

    private val catalog = mapOf(
        // -- Screen Control --
        "get_screen_hierarchy" to ToolEntry(
            "Screen Hierarchy",
            "Read the UI accessibility tree",
            "Screen Control"
        ),
        "take_screenshot" to ToolEntry(
            "Screenshot",
            "Capture screen as PNG image",
            "Screen Control"
        ),
        "input_gesture" to ToolEntry(
            "Gesture",
            "Tap, swipe, or long-press on screen",
            "Screen Control"
        ),
        "input_text" to ToolEntry(
            "Type Text",
            "Type text into focused input field",
            "Screen Control"
        ),
        "find_element" to ToolEntry(
            "Find Element",
            "Search for UI elements by text",
            "Screen Control"
        ),
        "click_by_text" to ToolEntry(
            "Click by Text",
            "Find and tap element by visible text",
            "Screen Control"
        ),
        "click_by_view_id" to ToolEntry(
            "Click by ID",
            "Find and tap element by view ID",
            "Screen Control"
        ),
        "scroll" to ToolEntry("Scroll", "Scroll in any direction", "Screen Control"),
        "global_action" to ToolEntry(
            "System Action",
            "Home, back, recents, and other system actions",
            "Screen Control"
        ),

        // -- Device --
        "get_device_info" to ToolEntry(
            "Device Info",
            "Model, OS, RAM, storage, and hardware details",
            "Device"
        ),
        "get_battery" to ToolEntry(
            "Battery",
            "Battery level, charging status, and health",
            "Device"
        ),
        "get_location" to ToolEntry(
            "Location",
            "Current GPS coordinates and location data",
            "Device"
        ),

        // -- Files --
        "list_files" to ToolEntry("List Files", "Browse files and folders with metadata", "Files"),
        "read_file" to ToolEntry("Read File", "Read text or binary file contents", "Files"),
        "write_file" to ToolEntry("Write File", "Create or overwrite files on device", "Files"),
        "delete_file" to ToolEntry("Delete File", "Delete files or directories", "Files"),

        // -- Camera --
        "take_photo" to ToolEntry(
            "Take Photo",
            "Capture photo from front or back camera",
            "Camera"
        ),
        "record_video" to ToolEntry(
            "Record Video",
            "Record short video clip from camera",
            "Camera"
        ),

        // -- Communication --
        "send_sms" to ToolEntry("Send SMS", "Send text messages", "Communication"),
        "read_sms" to ToolEntry("Read SMS", "Read inbox, sent, or all messages", "Communication"),
        "make_call" to ToolEntry("Phone Call", "Initiate a phone call", "Communication"),
        "make_call_with_voice" to ToolEntry(
            "Call & Speak",
            "Call and speak text when answered",
            "Communication"
        ),
        "search_contacts" to ToolEntry(
            "Search Contacts",
            "Find contacts by name or phone number",
            "Communication"
        ),

        // -- Notifications --
        "read_notifications" to ToolEntry(
            "Read",
            "Get active and recent notifications",
            "Notifications"
        ),
        "post_notification" to ToolEntry("Post", "Show a local notification", "Notifications"),
        "dismiss_notification" to ToolEntry(
            "Dismiss",
            "Dismiss a specific notification",
            "Notifications"
        ),
        "dismiss_all_notifications" to ToolEntry(
            "Dismiss All",
            "Clear all active notifications",
            "Notifications"
        ),

        // -- Media --
        "play_audio" to ToolEntry("Play Audio", "Play audio from URL, file, or data", "Media"),
        "stop_audio" to ToolEntry("Stop Audio", "Stop current audio playback", "Media"),
        "speak_tts" to ToolEntry("Text-to-Speech", "Speak text aloud using TTS engine", "Media"),
        "vibrate" to ToolEntry("Vibrate", "Vibrate with custom patterns", "Media"),

        // -- Storage --
        "analyze_storage" to ToolEntry(
            "Analyze",
            "Disk usage breakdown by directory and type",
            "Storage"
        ),
        "find_large_files" to ToolEntry(
            "Large Files",
            "Find files above a size threshold",
            "Storage"
        ),
        "index_media_metadata" to ToolEntry(
            "Index Media",
            "Index photos/videos with EXIF and GPS",
            "Storage"
        ),
        "search_media" to ToolEntry(
            "Search Media",
            "Search by date, location, type, or camera",
            "Storage"
        ),

        // -- Apps --
        "list_packages" to ToolEntry("Installed Apps", "List all apps with version info", "Apps"),
        "launch_intent" to ToolEntry("Launch App", "Launch apps or custom Android intents", "Apps"),

        // -- System --
        "execute_shell" to ToolEntry("Shell Command", "Run commands in app sandbox", "System"),
        "get_clipboard" to ToolEntry("Get Clipboard", "Read current clipboard content", "System"),
        "set_clipboard" to ToolEntry("Set Clipboard", "Copy text to clipboard", "System"),
        "get_volume" to ToolEntry("Get Volume", "Volume levels for all audio streams", "System"),
        "set_volume" to ToolEntry("Set Volume", "Adjust volume or mute audio streams", "System"),
        "show_toast" to ToolEntry("Toast Message", "Show a brief on-screen message", "System"),

        // -- Overlays --
        "show_overlay" to ToolEntry(
            "Show Overlay",
            "Display floating HTML overlay on screen",
            "Overlays"
        ),
        "hide_overlay" to ToolEntry("Hide Overlay", "Hide a specific floating overlay", "Overlays"),
        "hide_all_overlays" to ToolEntry("Hide All", "Remove all active overlays", "Overlays"),
        "list_overlays" to ToolEntry("List Overlays", "List active overlay IDs", "Overlays"),

        // -- Alarms --
        "get_alarms" to ToolEntry("Get Alarms", "View scheduled alarms", "Alarms"),
        "set_alarm" to ToolEntry("Set Alarm", "Create a new alarm", "Alarms"),
        "dismiss_alarm" to ToolEntry("Dismiss Alarm", "Stop a ringing alarm", "Alarms"),
        "delete_alarm" to ToolEntry("Delete Alarm", "Remove a saved alarm", "Alarms"),
    )

    /**
     * Get enriched ToolInfo for a given action name.
     * Falls back to auto-generated info if action isn't in the catalog.
     */
    fun getToolInfo(action: String): ToolInfo {
        val entry = catalog[action]
        return if (entry != null) {
            ToolInfo(
                name = action,
                displayName = entry.displayName,
                description = entry.description,
                category = entry.category
            )
        } else {
            // Fallback: derive from action name
            val displayName = action.replace("_", " ").replaceFirstChar { it.uppercase() }
            ToolInfo(
                name = action,
                displayName = displayName,
                description = action,
                category = "Other"
            )
        }
    }

    /**
     * Convert a list of action names to sorted, categorized ToolInfo list.
     */
    fun resolve(actions: Collection<String>): List<ToolInfo> {
        return actions
            .map { getToolInfo(it) }
            .sortedWith(compareBy({
                categoryOrder.indexOf(it.category).let { i -> if (i == -1) 999 else i }
            }, { it.displayName }))
    }
}

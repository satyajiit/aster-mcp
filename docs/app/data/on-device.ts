/**
 * The ON-DEVICE action catalogue — the other half of the tool story.
 *
 * These are the 77 unprefixed actions the Android app dispatches itself. They
 * are NOT the 49 `aster_*` MCP tools; the two catalogues overlap but neither
 * contains the other (see TOOL_COUNTS in ./site for the exact arithmetic).
 *
 * Reachable two ways, both on the phone:
 *   - Binder IPC — an app on the same device (OpenAlly) calls Aster directly.
 *   - The on-device Ktor MCP server — McpMode registers straight from the
 *     handler map, so it advertises this same set.
 *
 * GENERATED FROM SOURCE, then reviewed by hand. Names and categories come from
 * the handler map in apps/android/.../di/ModeModule.kt and the display metadata
 * in apps/android/.../service/mode/ToolCatalog.kt; the eight actions ToolCatalog
 * does not describe carry hand-written summaries. docs/scripts/verify-facts.ts
 * re-derives the name list on every build and throws if this file drifts.
 *
 * `server` is the `aster_*` tool that reaches the same action from an MCP
 * client, or null when nothing does — those are the on-device-only ones.
 */

export interface OnDeviceAction {
  /** The unprefixed action name, exactly as the wire carries it. */
  action: string
  label: string
  summary: string
  /** The `aster_*` tool that dispatches it, or null if no MCP client can. */
  server: string | null
}

export interface OnDeviceCategory {
  name: string
  actions: OnDeviceAction[]
}

export const ON_DEVICE_CATEGORIES: OnDeviceCategory[] = [
  {
    name: 'Screen Control',
    actions: [
      { action: 'click_by_text', label: 'Click by Text', summary: 'Find and tap element by visible text', server: 'aster_click_by_text' },
      { action: 'click_by_view_id', label: 'Click by ID', summary: 'Find and tap element by view ID', server: 'aster_click_by_id' },
      { action: 'find_element', label: 'Find Element', summary: 'Search for UI elements by text', server: 'aster_find_element' },
      { action: 'get_screen_hierarchy', label: 'Screen Hierarchy', summary: 'Read the UI accessibility tree', server: 'aster_get_screen_hierarchy' },
      { action: 'global_action', label: 'System Action', summary: 'Home, back, recents, and other system actions', server: 'aster_global_action' },
      { action: 'input_gesture', label: 'Gesture', summary: 'Tap, swipe, or long-press on screen', server: 'aster_input_gesture' },
      { action: 'input_text', label: 'Type Text', summary: 'Type text into focused input field', server: 'aster_input_text' },
      { action: 'long_press', label: 'Long Press', summary: 'Long-press an element by ref or coordinates', server: null },
      { action: 'observe', label: 'Observe Screen', summary: 'Indexed actionable-element view of the current screen with stable refs; falls back to on-device OCR when the accessibility tree is sparse', server: null },
      { action: 'perform', label: 'Perform Action', summary: 'Invoke an accessibility action on an element by ref', server: null },
      { action: 'press_key', label: 'Press Key', summary: 'Press a hardware/IME key (Enter, Back, Tab, arrows…)', server: null },
      { action: 'screen_approve', label: 'Ask for Approval', summary: 'Put a blocking approve/deny card on screen and wait for the owner to decide.', server: null },
      { action: 'screen_capability', label: 'Screen Capability Probe', summary: 'Read-only preflight: reports whether the accessibility service is enabled and bound before an automation run starts.', server: null },
      { action: 'screen_handoff', label: 'Hand Back to the Owner', summary: 'Park an automation run at a step only a person should do — a payment, say — and show why.', server: null },
      { action: 'screen_prompt', label: 'Ask the Owner', summary: 'Put a blocking choice on screen and wait for the owner to pick. One prompt in flight at a time.', server: null },
      { action: 'screen_signin_wait', label: 'Pause for Sign-in', summary: 'Park an automation run at a login or register wall and wait for the owner to finish.', server: null },
      { action: 'scroll', label: 'Scroll', summary: 'Scroll in any direction', server: null },
      { action: 'set_text', label: 'Set Text', summary: 'Type text into a specific field by ref (replace or append)', server: null },
      { action: 'set_toggle', label: 'Set Toggle', summary: 'Set a switch or checkbox on/off by ref', server: null },
      { action: 'take_screenshot', label: 'Screenshot', summary: 'Capture screen as JPEG; optional annotate adds numbered boxes (Set-of-Marks)', server: 'aster_take_screenshot' },
      { action: 'tap', label: 'Tap', summary: 'Tap an element by ref or coordinates', server: null },
      { action: 'wait_for', label: 'Wait for Element', summary: 'Wait until an element appears or disappears', server: null },
      { action: 'wait_for_idle', label: 'Wait for Idle', summary: 'Wait until the screen stops changing', server: null },
    ],
  },
  {
    name: 'Device',
    actions: [
      { action: 'get_battery', label: 'Battery', summary: 'Battery level, charging status, and health', server: 'aster_get_battery' },
      { action: 'get_device_info', label: 'Device Info', summary: 'Model, OS, RAM, storage, and hardware details', server: 'aster_get_device_info' },
      { action: 'get_location', label: 'Location', summary: 'Current GPS coordinates and location data', server: 'aster_get_location' },
    ],
  },
  {
    name: 'Files',
    actions: [
      { action: 'delete_file', label: 'Delete File', summary: 'Delete files or directories', server: 'aster_delete_file' },
      { action: 'files.list', label: 'List Host Folder', summary: 'List a folder the owner approved for an app (App Builder host-dir)', server: null },
      { action: 'files.read', label: 'Read Host File', summary: 'Read a file from an owner-approved folder (App Builder host-dir)', server: null },
      { action: 'list_files', label: 'List Files', summary: 'Browse files and folders with metadata', server: 'aster_list_files' },
      { action: 'read_file', label: 'Read File', summary: 'Read text or binary file contents', server: 'aster_read_file' },
      { action: 'write_file', label: 'Write File', summary: 'Create or overwrite files on device', server: 'aster_write_file' },
    ],
  },
  {
    name: 'Camera',
    actions: [
      { action: 'record_video', label: 'Record Video', summary: 'Record short video clip from camera', server: 'aster_record_video' },
      { action: 'take_photo', label: 'Take Photo', summary: 'Capture photo from front or back camera', server: 'aster_take_photo' },
    ],
  },
  {
    name: 'Communication',
    actions: [
      { action: 'count_sms', label: 'Count SMS', summary: 'Count messages in a date window', server: null },
      { action: 'delete_contacts', label: 'Delete Contacts', summary: 'Delete contacts from the address book by id. Requires WRITE_CONTACTS.', server: 'aster_delete_contacts' },
      { action: 'list_contacts_full', label: 'List Contacts (full)', summary: 'Page through the whole address book — every number, email and account type.', server: 'aster_list_contacts_full' },
      { action: 'make_call', label: 'Phone Call', summary: 'Initiate a phone call', server: 'aster_make_call' },
      { action: 'make_call_with_voice', label: 'Call & Speak', summary: 'Call and speak text when answered', server: 'aster_make_call_with_voice' },
      { action: 'read_sms', label: 'Read SMS', summary: 'Read inbox, sent, or all messages', server: 'aster_read_sms' },
      { action: 'search_contacts', label: 'Search Contacts', summary: 'Find contacts by name or phone number', server: 'aster_search_contacts' },
      { action: 'send_sms', label: 'Send SMS', summary: 'Send text messages', server: 'aster_send_sms' },
    ],
  },
  {
    name: 'Notifications',
    actions: [
      { action: 'dismiss_all_notifications', label: 'Dismiss All', summary: 'Clear all active notifications', server: null },
      { action: 'dismiss_notification', label: 'Dismiss', summary: 'Dismiss a specific notification', server: null },
      { action: 'post_notification', label: 'Post', summary: 'Show a local notification', server: 'aster_post_notification' },
      { action: 'read_notifications', label: 'Read', summary: 'Get active and recent notifications', server: 'aster_read_notifications' },
    ],
  },
  {
    name: 'Media',
    actions: [
      { action: 'get_now_playing', label: 'Now Playing', summary: 'Read the currently-playing track (title, artist, source app) from the OS media sessions; reuses notification access, no extra permission', server: null },
      { action: 'play_audio', label: 'Play Audio', summary: 'Play audio from URL, file, or data', server: 'aster_play_audio' },
      { action: 'speak_tts', label: 'Text-to-Speech', summary: 'Speak text aloud using TTS engine', server: 'aster_speak_tts' },
      { action: 'stop_audio', label: 'Stop Audio', summary: 'Stop current audio playback', server: 'aster_stop_audio' },
      { action: 'vibrate', label: 'Vibrate', summary: 'Vibrate with custom patterns', server: 'aster_vibrate' },
    ],
  },
  {
    name: 'Storage',
    actions: [
      { action: 'analyze_storage', label: 'Analyze', summary: 'Disk usage breakdown by directory and type', server: 'aster_analyze_storage' },
      { action: 'find_large_files', label: 'Large Files', summary: 'Find files above a size threshold', server: 'aster_find_large_files' },
      { action: 'index_media_metadata', label: 'Index Media', summary: 'Index photos/videos with EXIF and GPS', server: 'aster_index_media_metadata' },
      { action: 'search_media', label: 'Search Media', summary: 'Search by date, location, type, or camera', server: 'aster_search_media' },
    ],
  },
  {
    name: 'Apps',
    actions: [
      { action: 'launch_intent', label: 'Launch App', summary: 'Launch apps or custom Android intents', server: 'aster_launch_intent' },
      { action: 'list_installed_apps', label: 'Installed Apps (full)', summary: 'Installed apps with package, version, install time, sizes, declared permissions and last-used time.', server: 'aster_list_installed_apps' },
      { action: 'list_packages', label: 'Installed Apps', summary: 'List all apps with version info', server: 'aster_list_packages' },
    ],
  },
  {
    name: 'System',
    actions: [
      { action: 'execute_shell', label: 'Shell Command', summary: 'Run commands in app sandbox', server: 'aster_execute_shell' },
      { action: 'get_clipboard', label: 'Get Clipboard', summary: 'Read current clipboard content', server: 'aster_get_clipboard' },
      { action: 'get_volume', label: 'Get Volume', summary: 'Volume levels for all audio streams', server: 'aster_get_volume' },
      { action: 'screen_set_policy', label: 'Sync App Policy', summary: 'Receive the owner\'s per-app screen-control allow/deny list from OpenAlly', server: null },
      { action: 'set_clipboard', label: 'Set Clipboard', summary: 'Copy text to clipboard', server: 'aster_set_clipboard' },
      { action: 'set_volume', label: 'Set Volume', summary: 'Adjust volume or mute audio streams', server: 'aster_set_volume' },
      { action: 'show_toast', label: 'Toast Message', summary: 'Show a brief on-screen message', server: 'aster_show_toast' },
    ],
  },
  {
    name: 'Overlays',
    actions: [
      { action: 'companion_overlay_hide', label: 'Hide Companion Face', summary: 'Take down OpenAlly\'s ambient companion face', server: null },
      { action: 'companion_overlay_recompute', label: 'Re-place Companion Face', summary: 'Recompute the companion face\'s position around the camera cutout', server: null },
      { action: 'companion_overlay_show', label: 'Show Companion Face', summary: 'Draw OpenAlly\'s ambient companion face beside the camera cutout', server: null },
      { action: 'companion_overlay_status', label: 'Companion Face Status', summary: 'Whether Aster can draw over other apps, and whether OpenAlly\'s companion face is up', server: null },
      { action: 'hide_all_overlays', label: 'Hide All', summary: 'Remove all active overlays', server: null },
      { action: 'hide_overlay', label: 'Hide Overlay', summary: 'Hide a specific floating overlay', server: null },
      { action: 'list_overlays', label: 'List Overlays', summary: 'List active overlay IDs', server: null },
      { action: 'show_overlay', label: 'Show Overlay', summary: 'Display floating HTML overlay on screen', server: 'aster_show_overlay' },
    ],
  },
  {
    name: 'Alarms',
    actions: [
      { action: 'delete_alarm', label: 'Delete Alarm', summary: 'Remove a saved alarm', server: 'aster_delete_alarm' },
      { action: 'dismiss_alarm', label: 'Dismiss Alarm', summary: 'Stop a ringing alarm', server: 'aster_dismiss_alarm' },
      { action: 'get_alarms', label: 'Get Alarms', summary: 'View scheduled alarms', server: 'aster_get_alarms' },
      { action: 'set_alarm', label: 'Set Alarm', summary: 'Create a new alarm', server: 'aster_set_alarm' },
    ],
  },
]

export const ALL_ON_DEVICE_ACTIONS: OnDeviceAction[] = ON_DEVICE_CATEGORIES.flatMap((c) => c.actions)

/** The ones no MCP client can reach — the reason the catalogues differ. */
export const ON_DEVICE_ONLY: OnDeviceAction[] = ALL_ON_DEVICE_ACTIONS.filter((a) => a.server === null)

/**
 * The MCP tool catalogue — derived from `mcp/src/mcp/tools.ts`, which is the
 * source of truth. Every entry here corresponds to one `name: 'aster_…'` in
 * that file's `TOOLS` array, and `summary` is a one-line paraphrase of that
 * tool's own `description`.
 *
 * INVARIANT: `ALL_TOOLS.length` must equal `TOOL_COUNTS.mcpServer` in
 * `app/data/site.ts` (currently 49). If the server adds or removes a tool,
 * update BOTH — `grep -c "name: 'aster_" mcp/src/mcp/tools.ts` is the check.
 *
 * NAMES HERE ARE UNPREFIXED. The server registers all of them with the
 * `aster_` prefix, so nothing on this list is callable as written; the prefix
 * is added at render time from `TOOL_PREFIX`. Printing a bare `take_screenshot`
 * anywhere user-visible is a bug — that is exactly how the site shipped 49 tool
 * names of which none resolved.
 *
 * `accent` is a palette key, not a class. `ToolsShowcase.vue` owns the key →
 * Tailwind class map so the class strings stay literal and scannable.
 */

export interface ToolDef {
  /** UNPREFIXED. Render as `${TOOL_PREFIX}${name}`. */
  name: string
  /** One line, accurate, no marketing. Paraphrases the server's description. */
  summary: string
  /**
   * Argument names, required ones first, exactly as they appear in the tool's
   * inputSchema in mcp/src/mcp/tools.ts. Every tool but aster_list_devices
   * takes at least `deviceId`.
   *
   * These are here because /architecture told readers the full catalogue "with
   * arguments" was on /tools, and /tools listed no argument for any of the 49 —
   * so the one question this page is the obvious citation for ("what arguments
   * does aster_send_sms take?") was unanswerable from the site.
   */
  args: { name: string; required: boolean }[]
}

export interface ToolCategory {
  name: string
  /** One sentence describing what this group of tools is for. */
  blurb: string
  /** Iconify name, literal so @nuxt/icon's bundle scanner finds it. */
  icon: string
  /** Palette key: see ACCENTS in ToolsShowcase.vue. */
  accent: string
  tools: ToolDef[]
}

export const TOOL_CATEGORIES: ToolCategory[] = [
  {
    name: 'Screen and input',
    blurb:
      'Read what is on screen through the accessibility tree, then drive it back with taps, swipes and typed text.',
    icon: 'lucide:monitor',
    accent: 'aster',
    tools: [
      { name: 'take_screenshot', summary: 'Capture the current screen as an image.', args: [{ name: 'deviceId', required: true }] },
      {
        name: 'get_screen_hierarchy',
        summary:
          'Return the accessibility node tree of the current screen, with filters to keep the payload small.',
        args: [{ name: 'deviceId', required: true }, { name: 'mode', required: false }, { name: 'maxDepth', required: false }, { name: 'includeInvisible', required: false }, { name: 'searchText', required: false }],
      },
      {
        name: 'find_element',
        summary:
          'Locate UI elements by text or content description, without pulling the whole hierarchy.',
        args: [{ name: 'deviceId', required: true }, { name: 'text', required: true }, { name: 'exact', required: false }],
      },
      { name: 'input_gesture', summary: 'Perform a tap, swipe or long press on the screen.', args: [{ name: 'deviceId', required: true }, { name: 'gestureType', required: true }, { name: 'points', required: true }, { name: 'duration', required: false }] },
      { name: 'input_text', summary: 'Type text into the currently focused field.', args: [{ name: 'deviceId', required: true }, { name: 'text', required: true }] },
      {
        name: 'click_by_text',
        summary: 'Find and click an element by its text or content description in one call.',
        args: [{ name: 'deviceId', required: true }, { name: 'text', required: true }],
      },
      { name: 'click_by_id', summary: 'Click an element by its view ID resource name.', args: [{ name: 'deviceId', required: true }, { name: 'viewId', required: true }] },
    ],
  },
  {
    name: 'Navigation and apps',
    blurb:
      'Move around the system, launch apps and intents, and take inventory of what is installed.',
    icon: 'lucide:compass',
    accent: 'violet',
    tools: [
      {
        name: 'global_action',
        summary: 'Trigger a global navigation action such as BACK, HOME or RECENTS.',
        args: [{ name: 'deviceId', required: true }, { name: 'action', required: true }],
      },
      { name: 'launch_intent', summary: 'Launch an app, or fire an Android intent, on the device.', args: [{ name: 'deviceId', required: true }, { name: 'packageName', required: false }, { name: 'action', required: false }, { name: 'data', required: false }] },
      {
        name: 'list_packages',
        summary: 'List installed Android packages, optionally including system ones.',
        args: [{ name: 'deviceId', required: true }, { name: 'includeSystem', required: false }],
      },
      {
        name: 'list_installed_apps',
        summary:
          'Page through installed apps with full metadata: label, version, install and update time, on-disk size, declared permissions, and last-used time when Usage access is granted.',
        args: [{ name: 'deviceId', required: true }, { name: 'includeSystem', required: false }, { name: 'cursor', required: false }, { name: 'limit', required: false }],
      },
      {
        name: 'execute_shell',
        summary:
          'Run a shell command inside the app sandbox as an unprivileged process — no root, a 30-second timeout and a 1 MB output cap, with no access to system files or other apps’ data.',
        args: [{ name: 'deviceId', required: true }, { name: 'command', required: true }],
      },
    ],
  },
  {
    name: 'Files and storage',
    blurb: 'Browse, read and write files, and work out where the storage actually went.',
    icon: 'lucide:hard-drive',
    accent: 'amber',
    tools: [
      { name: 'list_files', summary: 'List the files and directories at a path.', args: [{ name: 'deviceId', required: true }, { name: 'path', required: true }] },
      { name: 'read_file', summary: 'Read the contents of a file on the device.', args: [{ name: 'deviceId', required: true }, { name: 'path', required: true }] },
      { name: 'write_file', summary: 'Write content to a file on the device.', args: [{ name: 'deviceId', required: true }, { name: 'path', required: true }, { name: 'content', required: true }] },
      { name: 'delete_file', summary: 'Delete a file from the device.', args: [{ name: 'deviceId', required: true }, { name: 'path', required: true }] },
      {
        name: 'analyze_storage',
        summary:
          'Break storage down by directory and file type and flag the large files, as statistics an assistant can reason over.',
        args: [{ name: 'deviceId', required: true }, { name: 'path', required: false }, { name: 'maxDepth', required: false }, { name: 'minSizeMB', required: false }, { name: 'includeHidden', required: false }],
      },
      {
        name: 'find_large_files',
        summary: 'Fast search for large files, with an optional file-type filter.',
        args: [{ name: 'deviceId', required: true }, { name: 'minSizeMB', required: true }, { name: 'path', required: false }, { name: 'fileTypes', required: false }, { name: 'limit', required: false }],
      },
    ],
  },
  {
    name: 'Media and camera',
    blurb:
      'Search the photo and video library by meaning or by metadata, and capture new media on demand.',
    icon: 'lucide:image',
    accent: 'rose',
    tools: [
      {
        name: 'search_media',
        summary:
          'Search media with a natural-language query or explicit filters — "photos from last year same month", "pictures taken at mumbai" — or both together.',
        args: [{ name: 'deviceId', required: true }, { name: 'query', required: false }, { name: 'path', required: false }, { name: 'dateFrom', required: false }, { name: 'dateTo', required: false }, { name: 'location', required: false }, { name: 'fileTypes', required: false }, { name: 'minSizeMB', required: false }, { name: 'maxSizeMB', required: false }, { name: 'cameraModel', required: false }, { name: 'sortBy', required: false }, { name: 'limit', required: false }],
      },
      {
        name: 'index_media_metadata',
        summary:
          'Scan photos and videos recursively and extract EXIF: date taken, GPS location, camera and dimensions.',
        args: [{ name: 'deviceId', required: true }, { name: 'path', required: false }, { name: 'includeLocation', required: false }, { name: 'includeExif', required: false }, { name: 'limit', required: false }],
      },
      {
        name: 'take_photo',
        summary: 'Capture a photo with the device camera at 1280x720 and return the image.',
        args: [{ name: 'deviceId', required: true }, { name: 'camera', required: false }, { name: 'quality', required: false }],
      },
      {
        name: 'record_video',
        summary:
          'Record up to 8 seconds at 480p with no audio; returns a base64 MP4 under 5 MB, otherwise a file path.',
        args: [{ name: 'deviceId', required: true }, { name: 'camera', required: false }, { name: 'maxDuration', required: false }],
      },
    ],
  },
  {
    name: 'Communication',
    blurb: 'Notifications, SMS and phone calls — the surfaces a phone is actually for.',
    icon: 'lucide:message-circle',
    accent: 'blue',
    tools: [
      {
        name: 'read_notifications',
        summary: 'Read the notifications currently sitting in the shade.',
        args: [{ name: 'deviceId', required: true }, { name: 'limit', required: false }],
      },
      { name: 'read_sms', summary: 'Read SMS messages, filterable by conversation thread and date.', args: [{ name: 'deviceId', required: true }, { name: 'limit', required: false }, { name: 'threadId', required: false }, { name: 'sinceDate', required: false }] },
      { name: 'send_sms', summary: 'Send an SMS text message to a phone number.', args: [{ name: 'deviceId', required: true }, { name: 'number', required: true }, { name: 'message', required: true }] },
      { name: 'post_notification', summary: 'Post a notification on the device.', args: [{ name: 'deviceId', required: true }, { name: 'title', required: true }, { name: 'body', required: true }, { name: 'actions', required: false }] },
      { name: 'make_call', summary: 'Place a phone call.', args: [{ name: 'deviceId', required: true }, { name: 'number', required: true }] },
      {
        name: 'make_call_with_voice',
        summary:
          'Place a call and speak text over the loudspeaker. The audio is acoustic coupling — the loudspeaker plays into the call microphone, it is not routed into the call audio — so it works best in a quiet room and quality is device-dependent.',
        args: [{ name: 'deviceId', required: true }, { name: 'number', required: true }, { name: 'text', required: true }, { name: 'waitSeconds', required: false }],
      },
    ],
  },
  {
    name: 'Contacts',
    blurb: 'Search, page through and prune the device address book.',
    icon: 'lucide:users',
    accent: 'teal',
    tools: [
      {
        name: 'search_contacts',
        summary:
          'Search contacts by name or phone number; each match comes back with all of its numbers and emails.',
        args: [{ name: 'deviceId', required: true }, { name: 'name', required: false }, { name: 'number', required: false }, { name: 'limit', required: false }],
      },
      {
        name: 'list_contacts_full',
        summary:
          'Page through the whole address book — numbers, emails and account type per contact — with a cursor, for indexing.',
        args: [{ name: 'deviceId', required: true }, { name: 'cursor', required: false }, { name: 'limit', required: false }],
      },
      {
        name: 'delete_contacts',
        summary:
          'Delete contacts by id. Requires WRITE_CONTACTS and returns the count deleted plus a per-id failure list.',
        args: [{ name: 'deviceId', required: true }, { name: 'ids', required: true }],
      },
    ],
  },
  {
    name: 'Audio and alarms',
    blurb:
      'Speak, play and silence audio, control the individual volume streams, and manage the clock app’s alarms.',
    icon: 'lucide:volume-2',
    accent: 'green',
    tools: [
      { name: 'speak_tts', summary: 'Speak text through the device text-to-speech engine.', args: [{ name: 'deviceId', required: true }, { name: 'text', required: true }] },
      { name: 'play_audio', summary: 'Play audio from a URL or from base64 data.', args: [{ name: 'deviceId', required: true }, { name: 'source', required: true }] },
      { name: 'stop_audio', summary: 'Stop the audio currently playing on the device.', args: [{ name: 'deviceId', required: true }] },
      { name: 'vibrate', summary: 'Vibrate the device with a custom pattern.', args: [{ name: 'deviceId', required: true }, { name: 'pattern', required: true }] },
      {
        name: 'get_volume',
        summary:
          'Read every stream level — media, ring, notification, alarm, call, system — along with the ringer mode.',
        args: [{ name: 'deviceId', required: true }],
      },
      { name: 'set_volume', summary: 'Set the level, or mute and unmute, for one audio stream.', args: [{ name: 'deviceId', required: true }, { name: 'stream', required: true }, { name: 'level', required: false }, { name: 'mute', required: false }] },
      {
        name: 'get_alarms',
        summary:
          'List alarms from the stock clock provider, falling back to next-alarm-only. Not every OEM exposes the full list.',
        args: [{ name: 'deviceId', required: true }],
      },
      {
        name: 'set_alarm',
        summary:
          'Create an alarm through the device clock app. Android exposes no standard way to edit one, so changing an alarm means creating another.',
        args: [{ name: 'deviceId', required: true }, { name: 'hour', required: true }, { name: 'minute', required: true }, { name: 'message', required: false }, { name: 'days', required: false }, { name: 'skipUi', required: false }],
      },
      {
        name: 'dismiss_alarm',
        summary:
          'Dismiss an alarm while it is actually ringing. Does nothing otherwise; needs Android 6.0 or newer.',
        args: [{ name: 'deviceId', required: true }],
      },
      {
        name: 'delete_alarm',
        summary:
          'Delete a saved alarm by id, on devices whose clock content provider is reachable (stock Android, Samsung).',
        args: [{ name: 'deviceId', required: true }, { name: 'alarmId', required: true }],
      },
    ],
  },
  {
    name: 'Device and UI',
    blurb:
      'Device state — battery, location, clipboard — plus the overlay and toast surfaces Aster can draw on top of whatever is running.',
    icon: 'lucide:cpu',
    accent: 'cyan',
    tools: [
      {
        name: 'list_devices',
        summary: 'List every paired device with its connection status and basic details.',
        args: [],
      },
      {
        name: 'get_device_info',
        summary: 'Report one device’s battery, storage and system specifications.',
        args: [{ name: 'deviceId', required: true }],
      },
      { name: 'get_battery', summary: 'Return detailed battery information and statistics.', args: [{ name: 'deviceId', required: true }] },
      { name: 'get_location', summary: 'Get the current GPS or network location of the device.', args: [{ name: 'deviceId', required: true }] },
      {
        name: 'get_clipboard',
        summary:
          'Read the device clipboard. Android 10 and later only let the foreground app read it, so a backgrounded Aster reports an empty clipboard rather than the real contents.',
        args: [{ name: 'deviceId', required: true }],
      },
      { name: 'set_clipboard', summary: 'Copy text to the device clipboard.', args: [{ name: 'deviceId', required: true }, { name: 'text', required: true }] },
      {
        name: 'show_overlay',
        summary:
          'Draw a system overlay containing web content, with an optional close button and auto-timeout.',
        args: [{ name: 'deviceId', required: true }, { name: 'url', required: false }, { name: 'html', required: false }, { name: 'showCloseButton', required: false }, { name: 'timeout', required: false }],
      },
      { name: 'show_toast', summary: 'Show a toast message on the device.', args: [{ name: 'deviceId', required: true }, { name: 'message', required: true }, { name: 'duration', required: false }] },
    ],
  },
]

/** Flat, all 49, in category order. Length must equal TOOL_COUNTS.mcpServer. */
export const ALL_TOOLS: ToolDef[] = TOOL_CATEGORIES.flatMap((c) => c.tools)

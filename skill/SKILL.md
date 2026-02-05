---
name: aster
version: 0.1.0
description: Control your Android device with AI. Take screenshots, automate UI, read notifications, manage files, search media, and more — all through natural language via MCP.
homepage: https://github.com/satyajiit/aster-mcp
metadata: {"aster":{"category":"device-control","requires":{"bins":["node"]},"mcp":{"type":"http","url":"http://localhost:5988/mcp"}}}
---

# Aster - Android Device Control

Control your Android device from your AI assistant using MCP (Model Context Protocol).

---

## Setup

1. **Install and start the server**:
```bash
npm install -g aster-mcp
aster start
```

2. **Install the Aster Android app** on your device from [Releases](https://github.com/satyajiit/aster-mcp/releases) and connect to the server address shown in terminal.

3. **Configure MCP** in your `.mcp.json`:
```json
{
  "mcpServers": {
    "aster": {
      "type": "http",
      "url": "http://localhost:5988/mcp"
    }
  }
}
```

---

## Available Tools

### Device & Screen
- `aster_list_devices` - List connected devices
- `aster_get_device_info` - Get device details (battery, storage, specs)
- `aster_take_screenshot` - Capture screenshots
- `aster_get_screen_hierarchy` - Get UI accessibility tree

### Input & Interaction
- `aster_input_gesture` - Tap, swipe, long press
- `aster_input_text` - Type text into focused field
- `aster_click_by_text` - Click element by text
- `aster_click_by_id` - Click element by view ID
- `aster_find_element` - Find UI elements
- `aster_global_action` - Back, Home, Recents, etc.

### Apps & System
- `aster_launch_intent` - Launch apps or intents
- `aster_list_packages` - List installed apps
- `aster_read_notifications` - Read notifications
- `aster_read_sms` - Read SMS messages
- `aster_get_location` - Get GPS location
- `aster_execute_shell` - Run shell commands

### Files & Storage
- `aster_list_files` - List directory contents
- `aster_read_file` - Read file content
- `aster_write_file` - Write to file
- `aster_delete_file` - Delete file
- `aster_analyze_storage` - Storage analysis
- `aster_find_large_files` - Find large files
- `aster_search_media` - Search photos/videos with natural language

### Device Features
- `aster_get_battery` - Battery info
- `aster_get_clipboard` / `aster_set_clipboard` - Clipboard access
- `aster_show_toast` - Show toast message
- `aster_speak_tts` - Text-to-speech
- `aster_vibrate` - Vibrate device
- `aster_play_audio` - Play audio
- `aster_post_notification` - Post notification
- `aster_make_call` - Initiate phone call
- `aster_show_overlay` - Show web overlay on device

### Media Intelligence
- `aster_index_media_metadata` - Extract photo/video EXIF metadata
- `aster_search_media` - Search photos/videos with natural language queries

---

## Example Usage

**Open YouTube and search:**
```
1. aster_launch_intent with packageName "com.google.android.youtube"
2. aster_click_by_id with viewId "com.google.android.youtube:id/search_button"
3. aster_input_text with text "cooking videos"
4. aster_global_action with action "BACK" to dismiss keyboard
```

**Take screenshot and analyze UI:**
```
1. aster_take_screenshot to see current screen
2. aster_get_screen_hierarchy to get interactive elements
3. aster_click_by_text to tap on specific button
```

**Find photos from a trip:**
```
aster_search_media with query "photos from Mumbai last month"
```

---

## Requirements

- Node.js >= 20
- Android device with Aster app installed
- Device and server on same network (or use Tailscale)

---

**GitHub**: [github.com/satyajiit/aster-mcp](https://github.com/satyajiit/aster-mcp)

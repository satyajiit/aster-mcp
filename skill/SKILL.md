---
name: aster
version: 0.1.13
description: Your AI CoPilot on Mobile — or give your AI its own phone. Make calls, send SMS, speak via TTS on speakerphone, automate UI, manage files, search media, and 40+ more tools via MCP. Open source, self-hosted, privacy-first.
homepage: https://aster.theappstack.in
metadata: {"aster":{"category":"device-control","requires":{"bins":["node"]},"mcp":{"type":"http","url":"http://localhost:5988/mcp"}}}
---

# Aster - Your AI CoPilot on Mobile

Your AI CoPilot for any Android device using MCP (Model Context Protocol) — or give your AI a dedicated phone and let it call, text, and act on its own. Fully open source and privacy-first — your data never leaves your network.

**Website**: [aster.theappstack.in](https://aster.theappstack.in) | **GitHub**: [github.com/satyajiit/aster-mcp](https://github.com/satyajiit/aster-mcp)

---

For screenshots of the Android app and web dashboard, visit [aster.theappstack.in](https://aster.theappstack.in).

---

## Setup

1. **Install and start the server**:
```bash
npm install -g aster-mcp
aster start
```

2. **Install the Aster Android app** on any Android device — your daily phone or a spare one for your AI — from [Releases](https://github.com/satyajiit/aster-mcp/releases) and connect to the server address shown in terminal.

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

## Security & Privacy

Aster is built with a **security-first, privacy-first** architecture:

- **Self-Hosted** — Runs entirely on your local machine. No cloud servers, no third-party relays. Your data stays on your network.
- **Zero Telemetry** — No analytics, no tracking, no usage data collection. What you do stays with you.
- **Device Approval** — Every new device must be manually approved from the dashboard before it can connect or execute commands.
- **Tailscale Integration** — Optional encrypted mesh VPN via Tailscale with WireGuard. Enables secure remote access with automatic TLS (WSS) — no port forwarding required.
- **No Root Required** — Uses the official Android Accessibility Service API (same system powering screen readers). No rooting, no ADB hacks, no exploits. Every action is permission-gated and sandboxed.
- **Foreground Transparency** — Always-visible notification on your Android device when the service is running. No silent background access.
- **Local Storage Only** — All data (device info, logs) stored in a local SQLite database. Nothing is sent externally.
- **100% Open Source** — MIT licensed, fully auditable codebase. Inspect every line of code on [GitHub](https://github.com/satyajiit/aster-mcp).

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
- `aster_send_sms` - Send an SMS text message to a phone number
- `aster_get_location` - Get GPS location
- `aster_execute_shell` - Run shell commands in Android app sandbox (no root, restricted to app data directory and user-accessible storage, 30s timeout, 1MB output limit)

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
- `aster_make_call_with_voice` - Make a call, enable speakerphone, and speak AI text via TTS after pickup
- `aster_show_overlay` - Show web overlay on device

### Media Intelligence
- `aster_index_media_metadata` - Extract photo/video EXIF metadata
- `aster_search_media` - Search photos/videos with natural language queries

---

## Example Usage

**Your CoPilot on Mobile:**
```
"Open YouTube and search for cooking videos"
→ aster_launch_intent → aster_click_by_id → aster_input_text

"Find photos from my trip to Mumbai last month"
→ aster_search_media with query "photos from Mumbai last month"

"Take a screenshot and tell me what's on screen"
→ aster_take_screenshot → aster_get_screen_hierarchy
```

**AI's own phone — let it act for you:**
```
"Call me and tell me my flight is delayed"
→ aster_make_call_with_voice with number, text "Your flight is delayed 45 min, new gate B12", waitSeconds 8

"Text me when my delivery arrives"
→ aster_read_notifications → aster_send_sms with number and message

"Reply to the delivery guy: Thanks, I'll be home"
→ aster_send_sms with number and message
```

---

## Commands

```bash
aster start              # Start the server
aster stop               # Stop the server
aster status             # Show server and device status
aster dashboard          # Open web dashboard

aster devices list       # List connected devices
aster devices approve    # Approve a pending device
aster devices reject     # Reject a device
aster devices remove     # Remove a device
```

---

## Requirements

- Node.js >= 20
- Any Android device with Aster app installed (your phone or a dedicated AI device)
- Device and server on same network (or use [Tailscale](https://tailscale.com) for secure remote access)

---

**Website**: [aster.theappstack.in](https://aster.theappstack.in) | **GitHub**: [github.com/satyajiit/aster-mcp](https://github.com/satyajiit/aster-mcp)

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

## Proactive Event Forwarding (OpenClaw Callbacks)

Aster can push real-time events from the phone to your AI agent via webhook. When enabled, these events arrive as HTTP POST payloads — your agent doesn't need to poll, the phone tells you what's happening.

Configure via the dashboard at `/settings/openclaw` or CLI: `aster set-openclaw-callbacks`.

### Webhook Format

Events are sent as HTTP POST to the configured OpenClaw endpoint (`/hooks/agent` by default). The AI reads the `message` field. All event context is packed into `message` using standardized `[key] value` tags.

Example raw HTTP POST payload for a notification event:
```json
{
  "message": "[skill] aster\n[event] notification\n[device_id] 6241e40fb71c0cf7\n[model] samsung SM-S938B, Android 16\n[data-app] messaging\n[data-package] com.google.android.apps.messaging\n[data-title] John\n[data-text] Hey, are you free tonight?",
  "wakeMode": "now",
  "deliver": true,
  "channel": "whatsapp",
  "to": "+1234567890"
}
```

- `message` — structured event text with standard headers (this is what the AI reads)
- `wakeMode` — always `"now"` (wake the agent immediately)
- `deliver` — always `true` for real events, `false` for test pings
- `channel` / `to` — delivery channel and recipient, configured in the dashboard

### Event Format

Every event follows a standardized structure with 4 fixed headers and `[data-*]` fields:

```
[skill] aster
[event] <event_name>
[device_id] <device_uuid>
[model] <manufacturer model, Android version>
[data-key] value
[data-key] value
```

- `[skill]` — always `aster`
- `[event]` — event name: `sms`, `notification`, `device_online`, `device_offline`, `pairing`
- `[device_id]` — UUID of the device (use this to target the device with Aster tools)
- `[model]` — device manufacturer, model, and OS
- `[data-*]` — event-specific fields, each prefixed with `data-` (e.g. `[data-app]`, `[data-sender]`)

### Event Types

**`sms`** — Incoming SMS
```
[skill] aster
[event] sms
[device_id] a1b2c3d4-5678-90ab
[model] samsung SM-S938B, Android 15
[data-sender] +1234567890
[data-body] Hey are you free tonight?
```

**`notification`** — App notification (deduplicated against SMS)
```
[skill] aster
[event] notification
[device_id] a1b2c3d4-5678-90ab
[model] samsung SM-S938B, Android 15
[data-app] whatsapp
[data-package] com.whatsapp
[data-title] John
[data-text] Meeting moved to 3pm
```

**`device_online`** — Approved device came online
```
[skill] aster
[event] device_online
[device_id] a1b2c3d4-5678-90ab
[model] samsung SM-S938B, Android 15
[data-status] connected
```

**`device_offline`** — Device went offline
```
[skill] aster
[event] device_offline
[device_id] a1b2c3d4-5678-90ab
[model] samsung SM-S938B, Android 15
[data-status] disconnected
```

**`pairing`** — New device needs approval (use `[device_id]` to approve)
```
[skill] aster
[event] pairing
[device_id] e5f6g7h8-9012-cdef
[model] Samsung SM-S924B, Android 15
[data-status] pending_approval
[data-action] approve this device from the Aster dashboard or via aster devices approve
```

### How to React to Events

When you receive a message with `[skill] aster`, parse the `[event]` and `[device_id]` to determine what happened and which device to act on.

**SMS — reply, extract info, or escalate:**
```
[event] sms | [device_id] a1b2c3d4 | sender: +1234567890 | body: Running late, be there in 20
→ aster_send_sms (deviceId: a1b2c3d4) to +1234567890: "No worries, see you soon!"

[event] sms | [device_id] a1b2c3d4 | sender: +1800555 | body: Your OTP is 482913
→ Extract OTP "482913", use aster_input_text (deviceId: a1b2c3d4) to enter it
```

**Notifications — monitor and act on behalf of user:**
```
[event] notification | [device_id] a1b2c3d4 | app: driver | text: Your driver is arriving
→ aster_speak_tts (deviceId: a1b2c3d4) "Your Uber is almost here"

[event] notification | [device_id] a1b2c3d4 | app: mShop | text: Your package was delivered
→ aster_send_sms (deviceId: a1b2c3d4) to user: "Your Amazon package just arrived"
```

**Device lifecycle — manage connectivity:**
```
[event] device_offline | [device_id] a1b2c3d4
→ Pause pending automations for device a1b2c3d4

[event] device_online | [device_id] a1b2c3d4
→ Resume queued tasks, aster_read_notifications (deviceId: a1b2c3d4) to catch up
```

**Pairing — approve or alert:**
```
[event] pairing | [device_id] e5f6g7h8 | model: Samsung SM-S924B
→ If expected: approve device e5f6g7h8 via dashboard API
→ If unexpected: alert user "Unknown device SM-S924B trying to connect"
```

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

aster set-openclaw-callbacks  # Configure event forwarding to OpenClaw
```

---

## Requirements

- Node.js >= 20
- Any Android device with Aster app installed (your phone or a dedicated AI device)
- Device and server on same network (or use [Tailscale](https://tailscale.com) for secure remote access)

---

**Website**: [aster.theappstack.in](https://aster.theappstack.in) | **GitHub**: [github.com/satyajiit/aster-mcp](https://github.com/satyajiit/aster-mcp)

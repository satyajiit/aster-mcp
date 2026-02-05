# Aster

Control your Android device from Claude and other AI assistants via MCP (Model Context Protocol).

## Installation

```bash
npm install -g aster-mcp
```

## Quick Start

1. Install the Aster Android app on your device
2. Start the server: `aster start`
3. Connect your device using the address shown in terminal

## Commands

```bash
# Start the server
aster start

# Stop the server
aster stop

# Open the dashboard in browser
aster dashboard
```

### Device Management

```bash
# List all registered devices
aster devices list

# Approve a pending device
aster devices approve <deviceId>

# Reject a device
aster devices reject <deviceId>

# Remove a device
aster devices remove <deviceId>
```

## Claude Integration

1. Start the server: `aster start`
2. Add to your Claude settings (`.mcp.json` or `~/.claude/settings.json`):

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

## Available MCP Tools

| Tool | Description |
|------|-------------|
| `aster_list_devices` | List paired devices |
| `aster_get_device_info` | Get device details (battery, storage, specs) |
| `aster_take_screenshot` | Capture screenshots |
| `aster_get_screen_hierarchy` | Get accessibility tree for UI analysis |
| `aster_find_element` | Find UI elements by text |
| `aster_input_gesture` | Perform touch gestures (tap, swipe, long press) |
| `aster_input_text` | Input text into focused fields |
| `aster_click_by_text` | Click elements by text content |
| `aster_click_by_id` | Click elements by view ID |
| `aster_global_action` | Perform global actions (back, home, recents) |
| `aster_launch_intent` | Launch apps or intents |
| `aster_read_notifications` | Read device notifications |
| `aster_read_sms` | Read SMS messages |
| `aster_get_location` | Get device GPS location |
| `aster_execute_shell` | Execute shell commands |
| `aster_list_packages` | List installed apps |
| `aster_list_files` | List files on device |
| `aster_read_file` | Read file content |
| `aster_write_file` | Write file content |
| `aster_delete_file` | Delete a file |
| `aster_get_battery` | Get battery info |
| `aster_get_clipboard` | Read clipboard |
| `aster_set_clipboard` | Copy to clipboard |
| `aster_show_toast` | Show toast message |
| `aster_speak_tts` | Text-to-speech |
| `aster_vibrate` | Vibrate device |
| `aster_play_audio` | Play audio |
| `aster_post_notification` | Post a notification |
| `aster_make_call` | Initiate phone call |
| `aster_show_overlay` | Show web overlay |
| `aster_analyze_storage` | Storage analysis |
| `aster_find_large_files` | Find large files |
| `aster_index_media_metadata` | Extract photo/video EXIF metadata |
| `aster_search_media` | Search photos/videos with natural language |

## Requirements

- Node.js >= 20
- Android device with Aster app installed

## License

MIT

<p align="center">
  <img src="https://raw.githubusercontent.com/satyajiit/aster-mcp/main/assets/logo.png" alt="Aster Logo" width="100" />
</p>

<h1 align="center">Aster</h1>

<p align="center">
  <strong>Give your AI assistant hands. Control any Android device through natural language.</strong>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/aster-mcp"><img src="https://img.shields.io/npm/v/aster-mcp?style=flat-square&color=blue" alt="npm version" /></a>
  <a href="https://www.npmjs.com/package/aster-mcp"><img src="https://img.shields.io/npm/dm/aster-mcp?style=flat-square&color=green" alt="npm downloads" /></a>
  <a href="https://github.com/satyajiit/aster-mcp/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License" /></a>
  <a href="https://github.com/satyajiit/aster-mcp"><img src="https://img.shields.io/badge/MCP-compatible-brightgreen?style=flat-square" alt="MCP" /></a>
  <a href="https://clawhub.ai/satyajiit/aster"><img src="https://img.shields.io/badge/ClawHub-skill-purple?style=flat-square" alt="ClawHub" /></a>
  <a href="https://openclaw.ai"><img src="https://img.shields.io/badge/OpenClaw-compatible-orange?style=flat-square" alt="OpenClaw" /></a>
  <a href="https://github.com/satyajiit/aster-mcp"><img src="https://img.shields.io/github/stars/satyajiit/aster-mcp?style=flat-square" alt="GitHub stars" /></a>
</p>

<p align="center">
  <a href="#installation">Installation</a> &bull;
  <a href="#connect-your-ai-assistant">Connect AI</a> &bull;
  <a href="#what-can-it-do">Features</a> &bull;
  <a href="#available-mcp-tools">Tools</a>
</p>

---

Aster is an MCP server that bridges your Android device to AI assistants. Take screenshots, automate UI, manage files, read notifications, search photos, and more &mdash; all through natural language.

> **Built for [OpenClaw](https://openclaw.ai)** &mdash; works natively as a skill on OpenClaw, Moltbot, and Clawbot. Also supports Claude and any MCP-compatible client.

## Installation

```bash
npm install -g aster-mcp
```

## Quick Start

```bash
# 1. Start the server
aster start

# 2. Install the Aster app on your Android device
#    and connect using the address shown in terminal

# 3. Connect your AI assistant (see below)
```

## Connect Your AI Assistant

### OpenClaw / Moltbot / Clawbot (Recommended)

Aster is a first-class skill on [ClawHub](https://clawhub.ai/satyajiit/aster). One command to install:

```bash
clawhub install satyajiit/aster
```

That's it. Your AI assistant can now control your Android device. See the [skill configuration](https://github.com/satyajiit/aster-mcp/tree/main/skill) for advanced setup.

### Claude Code / Claude Desktop

Add to your `.mcp.json` or Claude settings:

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

### Any MCP-Compatible Client

Aster exposes a standard MCP HTTP endpoint at `http://localhost:5988/mcp`.

## What Can It Do?

Just talk to your AI assistant naturally:

> "Take a screenshot of my phone"

> "Open YouTube and search for cooking videos"

> "Read my latest notifications"

> "Find photos from my trip to Mumbai last month"

> "What apps are using the most storage?"

## Commands

```bash
aster start              # Start the server
aster stop               # Stop the server
aster dashboard          # Open web dashboard

aster devices list       # List connected devices
aster devices approve    # Approve a pending device
aster devices reject     # Reject a device
aster devices remove     # Remove a device
```

## Available MCP Tools

| Category | Tools |
|----------|-------|
| **Screen** | `take_screenshot`, `get_screen_hierarchy`, `find_element` |
| **Input** | `input_gesture`, `input_text`, `click_by_text`, `click_by_id` |
| **Navigation** | `global_action`, `launch_intent` |
| **Device** | `list_devices`, `get_device_info`, `get_battery`, `get_location` |
| **Notifications** | `read_notifications`, `read_sms`, `post_notification` |
| **Files** | `list_files`, `read_file`, `write_file`, `delete_file` |
| **Storage** | `analyze_storage`, `find_large_files`, `search_media` |
| **Clipboard** | `get_clipboard`, `set_clipboard` |
| **Audio** | `speak_tts`, `play_audio`, `vibrate` |
| **Apps** | `list_packages`, `execute_shell` |

## Requirements

- Node.js >= 20
- Android device with Aster app installed
- Device and server on same network (or [Tailscale](https://tailscale.com))

## License

MIT

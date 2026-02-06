<p align="center">
  <img src="https://raw.githubusercontent.com/satyajiit/aster-mcp/main/assets/logo.png" alt="Aster Logo" width="100" />
</p>

<h1 align="center">Aster</h1>

<p align="center">
  <strong>Control any Android with AI — or give your AI its own phone</strong>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/aster-mcp"><img src="https://img.shields.io/npm/v/aster-mcp?style=flat-square&color=blue" alt="npm version" /></a>
  <a href="https://www.npmjs.com/package/aster-mcp"><img src="https://img.shields.io/npm/dm/aster-mcp?style=flat-square&color=green" alt="npm downloads" /></a>
  <a href="https://github.com/satyajiit/aster-mcp/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License" /></a>
  <a href="https://github.com/satyajiit/aster-mcp"><img src="https://img.shields.io/badge/MCP-compatible-brightgreen?style=flat-square" alt="MCP" /></a>
  <a href="https://clawhub.ai/aster"><img src="https://img.shields.io/badge/ClawHub-skill-purple?style=flat-square" alt="ClawHub" /></a>
  <a href="https://openclaw.ai"><img src="https://img.shields.io/badge/OpenClaw-compatible-orange?style=flat-square" alt="OpenClaw" /></a>
  <a href="https://github.com/satyajiit/aster-mcp"><img src="https://img.shields.io/github/stars/satyajiit/aster-mcp?style=flat-square" alt="GitHub stars" /></a>
</p>

<p align="center">
  <a href="https://aster.theappstack.in">Website</a> &bull;
  <a href="#installation">Installation</a> &bull;
  <a href="#connect-your-ai-assistant">Connect AI</a> &bull;
  <a href="#what-can-it-do">Features</a> &bull;
  <a href="#available-mcp-tools">Tools</a> &bull;
  <a href="#security--privacy">Security</a>
</p>

---

Aster is an MCP server that bridges any Android device to AI assistants. Control your phone remotely &mdash; or give your AI a dedicated device and let it call you, text you, and act autonomously. Screenshots, UI automation, file management, notifications, media search, and 40+ tools &mdash; all through natural language. Fully open source and privacy-first.

> **Built for [OpenClaw](https://openclaw.ai)** &mdash; works natively as a skill on OpenClaw, Moltbot, and Clawbot. Also supports Claude and any MCP-compatible client.

## Web Dashboard

<p align="center">
  <img src="https://raw.githubusercontent.com/satyajiit/aster-mcp/main/mcp/dashboard/screenshots/dashboard-overview.png" width="49%" alt="Dashboard Overview" />
  &nbsp;
  <img src="https://raw.githubusercontent.com/satyajiit/aster-mcp/main/mcp/dashboard/screenshots/device-telemetry.png" width="49%" alt="Device Telemetry" />
</p>
<p align="center">
  <img src="https://raw.githubusercontent.com/satyajiit/aster-mcp/main/mcp/dashboard/screenshots/file-preview.png" width="49%" alt="File Browser & Preview" />
  &nbsp;
  <img src="https://raw.githubusercontent.com/satyajiit/aster-mcp/main/mcp/dashboard/screenshots/mcp-tool-explorer.png" width="49%" alt="MCP Tool Explorer" />
</p>

<p align="center">
  <sub>Dashboard Overview &nbsp;&bull;&nbsp; Device Telemetry &nbsp;&bull;&nbsp; File Browser & Preview &nbsp;&bull;&nbsp; MCP Tool Explorer</sub>
</p>

## Installation

```bash
npm install -g aster-mcp
```

## Quick Start

```bash
# 1. Start the server
aster start

# 2. Install the Aster app on any Android device
#    — your daily phone or a spare one for your AI
#    Connect using the address shown in terminal

# 3. Connect your AI assistant (see below)
```

## Connect Your AI Assistant

### OpenClaw / Moltbot / Clawbot (Recommended)

Aster is a first-class skill on [ClawHub](https://clawhub.ai/aster). One command to install:

```bash
clawhub install aster
```

That's it. Your AI assistant can now control your Android device — or use a dedicated phone as its own. See the [skill configuration](https://github.com/satyajiit/aster-mcp/tree/main/skill) for advanced setup.

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

**Control your phone** — talk to your AI assistant naturally:

> "Take a screenshot of my phone"

> "Open YouTube and search for cooking videos"

> "Read my latest notifications"

> "Find photos from my trip to Mumbai last month"

> "What apps are using the most storage?"

**Give your AI its own phone** — plug in a spare Android and let it act:

> "Call me if my flight gets delayed or the gate changes"

> "Monitor delivery notifications and text me when the package arrives"

> "Back up all new photos to Google Drive every night"

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

## Available MCP Tools

| Category | Tools |
|----------|-------|
| **Screen** | `take_screenshot`, `get_screen_hierarchy`, `find_element` |
| **Input** | `input_gesture`, `input_text`, `click_by_text`, `click_by_id` |
| **Navigation** | `global_action`, `launch_intent` |
| **Device** | `list_devices`, `get_device_info`, `get_battery`, `get_location` |
| **Notifications** | `read_notifications`, `read_sms`, `send_sms`, `post_notification` |
| **Files** | `list_files`, `read_file`, `write_file`, `delete_file` |
| **Storage** | `analyze_storage`, `find_large_files`, `search_media` |
| **Clipboard** | `get_clipboard`, `set_clipboard` |
| **Calls** | `make_call`, `make_call_with_voice` |
| **Audio** | `speak_tts`, `play_audio`, `vibrate` |
| **Apps** | `list_packages`, `execute_shell` (sandboxed, no root) |

## Security & Privacy

Aster is built with a security-first, privacy-first architecture:

- **Self-Hosted** — Runs entirely on your local machine. No cloud servers, no third-party relays.
- **Zero Telemetry** — No analytics, no tracking, no usage data collection.
- **Device Approval** — Every new device must be manually approved from the dashboard before it can connect.
- **Tailscale Integration** — Optional encrypted mesh VPN with WireGuard. Secure remote access with automatic TLS, no port forwarding.
- **No Root Required** — Uses the official Android Accessibility Service API. No rooting, no ADB hacks. Every action is permission-gated and sandboxed.
- **Local Storage Only** — All data stored in a local SQLite database. Nothing is sent externally.
- **100% Open Source** — MIT licensed, fully auditable codebase.

## Requirements

- Node.js >= 20
- Any Android device with Aster app installed (your phone or a dedicated AI device)
- Device and server on same network (or [Tailscale](https://tailscale.com) for secure remote access)

## License

MIT

---

<p align="center">
  <a href="https://aster.theappstack.in">Website</a> &bull;
  <a href="https://github.com/satyajiit/aster-mcp">GitHub</a>
</p>

<p align="center">
  <img src="./assets/logo.png" alt="Aster Logo" width="120" />
</p>

<h1 align="center">Aster</h1>

<p align="center">
  <strong>Control your Android with AI — or give your AI its own phone</strong>
</p>

<p align="center">
  <a href="https://www.npmjs.com/package/aster-mcp"><img src="https://img.shields.io/npm/v/aster-mcp?style=flat-square&color=blue" alt="npm version" /></a>
  <a href="https://www.npmjs.com/package/aster-mcp"><img src="https://img.shields.io/npm/dm/aster-mcp?style=flat-square&color=green" alt="npm downloads" /></a>
  <a href="https://github.com/satyajiit/aster-mcp/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" alt="License" /></a>
  <a href="https://clawhub.ai/aster"><img src="https://img.shields.io/badge/ClawHub-skill-purple?style=flat-square" alt="ClawHub" /></a>
  <a href="https://openclaw.ai"><img src="https://img.shields.io/badge/OpenClaw-compatible-orange?style=flat-square" alt="OpenClaw" /></a>
  <img src="https://img.shields.io/badge/40%2B-MCP_tools-2dd4bf?style=flat-square" alt="40+ MCP tools" />
  <img src="https://img.shields.io/badge/Android_7%2B-supported-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 7+" />
  <img src="https://img.shields.io/badge/no_root-required-ff6b6b?style=flat-square" alt="No root required" />
  <img src="https://img.shields.io/badge/self--hosted-privacy_first-gold?style=flat-square" alt="Self-hosted" />
</p>

<p align="center">
  <img src="https://forthebadge.com/badges/ages-20-30.svg" alt="Ages 20-30" />
  <img src="https://forthebadge.com/badges/approved-by-george-costanza.svg" alt="Approved by George Costanza" />
  <img src="https://forthebadge.com/badges/made-with-vue.svg" alt="Made with Vue" />
  <img src="https://forthebadge.com/api/badges/generate?panels=2&primaryLabel=MADE+WITH&secondaryLabel=TYPESCRIPT&primaryBGColor=%23429eff&secondaryBGColor=%233178c6&primaryTextColor=%23FFFFFF&primaryFontSize=12&primaryFontWeight=600&primaryLetterSpacing=2&primaryFontFamily=Roboto&primaryTextTransform=uppercase&secondaryTextColor=%23FFFFFF&secondaryFontSize=12&secondaryFontWeight=900&secondaryLetterSpacing=2&secondaryFontFamily=Montserrat&secondaryTextTransform=uppercase&secondaryIcon=typescript&secondaryIconColor=%23FFFFFF&secondaryIconSize=16&secondaryIconPosition=left" alt="Made with TypeScript" />
  <img src="https://forthebadge.com/api/badges/generate?panels=2&primaryLabel=BUILT&secondaryLabel=WITH+NUXT&primaryBGColor=%23000000&secondaryBGColor=%2309ce44&primaryTextColor=%23FFFFFF&primaryFontSize=12&primaryFontWeight=600&primaryLetterSpacing=2&primaryFontFamily=Roboto&primaryTextTransform=uppercase&secondaryTextColor=%23FFFFFF&secondaryFontSize=12&secondaryFontWeight=900&secondaryLetterSpacing=2&secondaryFontFamily=Montserrat&secondaryTextTransform=uppercase&secondaryIcon=nuxt&secondaryIconColor=%23FFFFFF&secondaryIconSize=16&secondaryIconPosition=right" alt="Built with Nuxt" />
  <img src="https://forthebadge.com/api/badges/generate?panels=2&primaryLabel=Porsche&secondaryLabel=Cayman&primaryBGColor=%23000000&secondaryBGColor=%23ffffff&tertiaryBGColor=%23ff0000&primaryTextColor=%23FFFFFF&primaryFontSize=15&primaryFontWeight=600&primaryLetterSpacing=2&primaryFontFamily=Roboto&primaryTextTransform=capitalize&secondaryTextColor=%23000000&secondaryFontSize=15&secondaryFontWeight=900&secondaryLetterSpacing=2&secondaryFontFamily=Montserrat&secondaryTextTransform=capitalize&primaryFontVariant=small-caps&secondaryTextShadowOffsetX=0.5&secondaryTextShadowOffsetY=1.5&secondaryTextShadowColor=%23ffffff&secondaryFontVariant=small-caps&borderRadius=10&scale=1.40&secondaryIcon=porsche&secondaryIconColor=%23ff0000&secondaryIconSize=16&secondaryIconPosition=right" alt="Porsche Cayman" />
  <img src="https://forthebadge.com/badges/fuck-it-ship-it.svg" alt="Fuck it, ship it" />
  <img src="https://forthebadge.com/badges/ages-18.svg" alt="Ages 18+" />
  <img src="https://forthebadge.com/badges/powered-by-jeffs-keyboard.svg" alt="Powered by Jeff's keyboard" />
  <img src="https://forthebadge.com/badges/open-source.svg" alt="Open Source" />
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#usage">Usage</a> •
  <a href="#integrations">Integrations</a> •
  <a href="#mcp-tools">MCP Tools</a>
</p>

<p align="center">
  <img src="./assets/aster_poster.png" alt="Aster — Control your Android with AI" width="100%" />
</p>

---

**Aster** bridges any Android device to AI assistants like Claude through the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/). Control your phone remotely — or plug a spare Android into a charger, install Aster, and give your AI its own device. It can call you, text you, monitor notifications, and act on its own. Screenshots, UI automation, file management, media search, and 40+ tools — all through natural language.

<p align="center">
  <img src="./apps/android/screenshots/connection-setup.jpg" width="200" alt="Connection Setup" />
  &nbsp;&nbsp;
  <img src="./apps/android/screenshots/device-dashboard.jpg" width="200" alt="Device Dashboard" />
  &nbsp;&nbsp;
  <img src="./apps/android/screenshots/services-logs.jpg" width="200" alt="Services & Logs" />
  &nbsp;&nbsp;
  <img src="./apps/android/screenshots/permissions.jpg" width="200" alt="Permissions" />
</p>

<p align="center">
  <sub>Connection Setup &nbsp;&bull;&nbsp; Device Dashboard &nbsp;&bull;&nbsp; Services & Activity Log &nbsp;&bull;&nbsp; Permissions</sub>
</p>

## Features

- **Screen Control** — Take screenshots, analyze UI hierarchy, tap, swipe, and type
- **App Automation** — Launch apps, click buttons by text/ID, navigate with gestures
- **Device Access** — Read notifications, SMS, clipboard, location, and battery info
- **File Management** — Browse, read, write, and delete files on device storage
- **Media Search** — Find photos and videos using natural language queries
- **Calls, SMS & Voice** — Make calls, send SMS, or use `make_call_with_voice` to dial and speak a TTS message on speakerphone
- **System Actions** — Back, Home, Recents, volume control, and more
- **AI's Own Phone** — Dedicate a spare Android to your AI. It monitors, calls, texts, and acts on its own

## Web Dashboard

Aster ships with a built-in web dashboard for managing devices, browsing files, and testing MCP tools.

<p align="center">
  <img src="./mcp/dashboard/screenshots/dashboard-overview.png" width="49%" alt="Dashboard Overview" />
  &nbsp;
  <img src="./mcp/dashboard/screenshots/device-telemetry.png" width="49%" alt="Device Telemetry" />
</p>
<p align="center">
  <img src="./mcp/dashboard/screenshots/file-preview.png" width="49%" alt="File Browser & Preview" />
  &nbsp;
  <img src="./mcp/dashboard/screenshots/mcp-tool-explorer.png" width="49%" alt="MCP Tool Explorer" />
</p>

<p align="center">
  <sub>Dashboard Overview &nbsp;&bull;&nbsp; Device Telemetry &nbsp;&bull;&nbsp; File Browser & Preview &nbsp;&bull;&nbsp; MCP Tool Explorer</sub>
</p>

<details>
<summary>More screenshots</summary>
<br>
<p align="center">
  <img src="./mcp/dashboard/screenshots/device-registry.png" width="49%" alt="Device Registry" />
  &nbsp;
  <img src="./mcp/dashboard/screenshots/device-system-info.png" width="49%" alt="System Info" />
</p>
<p align="center">
  <img src="./mcp/dashboard/screenshots/device-control.png" width="49%" alt="Device Control" />
  &nbsp;
  <img src="./mcp/dashboard/screenshots/file-browser.png" width="49%" alt="File Browser" />
</p>
<p align="center">
  <sub>Device Registry &nbsp;&bull;&nbsp; System Info &nbsp;&bull;&nbsp; Device Control &nbsp;&bull;&nbsp; File Browser</sub>
</p>
</details>

## Quick Start

### 1. Install the MCP Server

```bash
npm install -g aster-mcp
```

### 2. Install the Android App

Download the Aster companion app from [Releases](https://github.com/satyajiit/aster-mcp/releases) and install it on your Android device — your daily phone or a spare one you want to dedicate to your AI.

### 3. Start the Server

```bash
aster start
```

### 4. Connect Your Device

Open the Aster app on your Android device and enter the server address shown in the terminal. For a dedicated AI phone, just plug it into a charger and leave it connected — your AI now has its own device.

### 5. Configure Your AI Client

See [Integrations](#integrations) for Claude, OpenClaw, Moltbot, Clawbot, and other MCP clients.

## Usage

### CLI Commands

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

### Example Prompts

Once connected, try these with your AI assistant:

**Control your phone:**

> "Take a screenshot of my phone"

> "Open YouTube and search for cooking videos"

> "Read my latest notifications"

> "Find photos from my trip to Mumbai last month"

> "What apps are using the most storage?"

**AI's own phone — let it act for you:**

> "Call me if my flight gets delayed and tell me the new time" *(uses make_call_with_voice)*

> "Text me when my delivery arrives" *(uses send_sms)*

> "Back up new photos to Google Drive every night"

## Integrations

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

### OpenClaw / Moltbot / Clawbot

Aster is available as a skill on [ClawHub](https://clawhub.ai/aster). Install it directly:

```bash
clawhub install aster
```

Or add manually to your OpenClaw/Moltbot/Clawbot skills directory and configure the MCP endpoint.

### Any MCP-Compatible Client

Aster exposes a standard MCP HTTP endpoint at `http://localhost:5988/mcp` that works with any MCP-compatible AI client.

## MCP Tools

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
| **Apps** | `list_packages`, `execute_shell` |

## Architecture

```
┌─────────────────┐     WebSocket      ┌─────────────────┐
│                 │◄──────────────────►│                 │
│  Aster Server   │                    │  Android App    │
│  (Node.js)      │                    │  (Accessibility │
│                 │                    │   Service)      │
└────────┬────────┘                    └─────────────────┘
         │
         │ MCP (HTTP)
         │
┌────────▼────────┐
│                 │
│  Claude / AI    │
│                 │
└─────────────────┘
```

## Project Structure

```
Aster/
├── apps/android/     # Android companion app
├── mcp/              # MCP server (npm: aster-mcp)
└── skill/            # ClawHub skill
```

## Requirements

- **Server**: Node.js >= 20
- **Android**: Android 7.0+ with Accessibility Service enabled
- **Network**: Device and server on same network (or Tailscale)

## Tailscale Support

Aster automatically detects Tailscale and displays your Tailscale IP for easy remote connections without port forwarding. Perfect for a dedicated AI phone that stays plugged in at home while you're away.

## License

MIT © [Satyajit Pradhan](https://github.com/satyajiit)

---

<p align="center">
  <sub>Built with ❤️ for the AI-first future</sub>
</p>

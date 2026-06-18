# Aster ↔ Hermes Agent Integration

Connect [Aster](https://github.com/satyajiit/aster-mcp) as an MCP server to [Hermes Agent](https://github.com/NousResearch/hermes-agent) so Hermes can control your Android device — read SMS, take screenshots, make calls, manage files, and more.

## Quick Start

### 1. Start the Aster server

```bash
aster start
```

This starts the WebSocket server (port 5987) and the API/MCP HTTP endpoint (port 5988).

### 2. Pair your Android device

Install the [Aster Companion App](https://aster.theappstack.in) on your Android device, then connect to the server via the dashboard or Tailscale.

### 3. Add Aster to Hermes config

Edit `~/.hermes/config.yaml`:

```yaml
mcp_servers:
  aster:
    type: "http"
    url: "http://localhost:5988/mcp"
    tools: "*"
```

Or use the CLI helper:

```bash
aster configure-hermes
```

### 4. Restart Hermes

```bash
hermes chat
```

Verify the tools are connected:

```bash
hermes mcp list
```

You should see all 60+ Aster tools listed under `aster:`.

## Remote Setup (Tailscale)

If Aster runs on a different machine, use your Tailscale IP or DNS:

```yaml
mcp_servers:
  aster:
    type: "http"
    url: "http://100.123.45.67:5988/mcp"   # Replace with your Tailscale IP
    tools: "*"
```

> 💡 Run `aster status` to see your Tailscale endpoints.

## What Hermes Can Do with Aster

| Category | Example Prompts |
|----------|----------------|
| **Screen** | "Take a screenshot of my phone and describe what you see" |
| **Messages** | "Read my latest SMS messages" or "Send an SMS to Mom: I'll be home late" |
| **Calls** | "Call +1234567890 and tell them I'm running late" |
| **Files** | "List all photos in /sdcard/DCIM from last week" |
| **Storage** | "Find files larger than 100MB on my phone" |
| **Contacts** | "Search my contacts for 'John' and show his phone numbers" |
| **Calendar** | "Add a dentist appointment for next Tuesday at 10am" |
| **WiFi/BT** | "Is my WiFi connected? What's the signal strength?" |
| **Brightness** | "Set my screen brightness to 50%" |
| **Camera** | "Take a photo with the back camera" |
| **Notifications** | "Show me my recent notifications" |
| **Location** | "Where is my phone right now?" |
| **Apps** | "List all installed apps" or "Uninstall TikTok" |
| **Battery** | "What's my phone's battery level?" |
| **Alarms** | "Set an alarm for 7am tomorrow" |

## Tool Filtering

You can restrict which Aster tools Hermes has access to:

```yaml
mcp_servers:
  aster:
    type: "http"
    url: "http://localhost:5988/mcp"
    tools:
      include:
        - "aster_take_screenshot"
        - "aster_read_sms"
        - "aster_send_sms"
        - "aster_get_battery"
        - "aster_get_location"
      # All other Aster tools will be hidden from Hermes
```

## Safety Notes

- **Aster runs commands on your real Android device.** Be careful with destructive tools like `aster_delete_file`, `aster_uninstall_package`, or `aster_delete_contacts`.
- Hermes Agent's tool sandboxing (per-session tool sets) can restrict which tools are available per conversation.
- The Aster companion app runs in an app sandbox — it cannot modify system files, access other apps' data, or bypass Android permissions.
- Aster's built-in Kill Switch and PackagePolicyGuard provide additional safety layers.

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Connection refused" | Ensure Aster is running: `aster status` |
| Tools not showing in Hermes | Run `hermes mcp list` and check for errors |
| SSL/HTTPS errors | Aster's MCP endpoint is HTTP (not HTTPS). Use `http://` in the URL |
| Remote connection fails | Check Tailscale status and that port 5988 is reachable |
| Tools appear but fail | Check that your Android device is paired and online: `aster devices list` |

## Related

- [Aster GitHub](https://github.com/satyajiit/aster-mcp)
- [Aster Website](https://aster.theappstack.in)
- [Hermes Agent GitHub](https://github.com/NousResearch/hermes-agent)
- [Hermes MCP Docs](https://hermes-agent.nousresearch.com/docs/user-guide/features/mcp)

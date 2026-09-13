/**
 * The /setup route's content: the install procedure and the client matrix.
 *
 * Source of truth is README.md ("Quick Start", "Usage", "Integrations",
 * "Requirements") plus mcp/package.json. Every port, URL, version and Android
 * level is imported from ~/data/site — nothing here is retyped, because that is
 * exactly how the page came to print a `.../satyajiit/Aster/main/skill/SKILL.md`
 * raw link (the repo is `aster-mcp`; that URL is a hard 404) and an
 * "Android 7.0+" requirement the APK contradicts.
 *
 * Read by app/components/SetupSteps.vue, app/components/IntegrationsSection.vue,
 * app/pages/setup.vue and scripts/generate-machine-readable.ts (the /setup.md
 * twin and llms-full.txt). The generator reads `title`, `body`, `command`,
 * `lang`, `note`, `name` and `snippet` by name — do not rename a field without
 * changing it there too.
 */

import { ENDPOINTS, FACTS, LINKS, PATHS, PORTS, TOOL_COUNTS, TOOL_PREFIX } from './site'

export interface SetupStepDef {
  /** Stable anchor fragment: the <li> is `#step-<id>` and JSON-LD links to it. */
  id: string
  title: string
  /** One or two sentences of plain prose. Rendered as text, not HTML. */
  body: string
  /** Shell or config to run verbatim. Rendered in a copyable code block. */
  command?: string
  /**
   * Fence language for the markdown twin. Defaults to bash there, which is
   * right for five of the six steps and wrong for the one whose `command` is
   * the .mcp.json document — a JSON object fenced as bash reads as something to
   * paste into a terminal. Same field, same meaning, as ClientDef.lang.
   */
  lang?: string
  /** The caveat that bites people who skip it. */
  note?: string
}

export interface ClientDef {
  id: string
  name: string
  body: string
  snippet?: string
  /** Fence language for the twin, and the code block's label. */
  lang?: string
  icon: string
  /** CSS colour for the icon tile. A design token, never a raw hex. */
  accent: string
  /**
   * Render this client's real brand mark instead of the generic icon tile.
   *
   * A discriminator, NOT a file path: the geometry lives in the shared layer's
   * brand components (packages/aster-ui/app/components/brand/), which are the
   * single source of truth — OpenAllyMark's path is byte-identical to
   * OpenAllyWeb's Logo.tsx. Naming a URL here would fork that geometry into a
   * second copy that nothing keeps in sync.
   *
   * Only for marks we actually own. Claude, OpenClaw and AnythingLLM keep a
   * generic lucide tile on purpose — drawing our own approximation of someone
   * else's wordmark and shipping it as their brand is not ours to do. `icon`
   * stays required so every card has a fallback and the twins have a name.
   */
  brand?: 'openally'
  href?: string
}

/** The `.mcp.json` entry, reused by the Claude card and by the JSON-LD. */
const MCP_JSON = `{
  "mcpServers": {
    "aster": {
      "type": "http",
      "url": "${ENDPOINTS.mcp}"
    }
  }
}`

export const SETUP_STEPS: SetupStepDef[] = [
  {
    id: 'install-server',
    title: 'Install the MCP server',
    body: `The server is an npm package, ${FACTS.npmPackage}, currently version ${FACTS.serverVersion}. Install it globally on the machine that will stay awake — a laptop, a desktop, a NAS or a mini PC.`,
    command: `npm install -g ${FACTS.npmPackage}`,
    note: `Node.js ${FACTS.nodeRequirement} is required. Running the server inside Termux on the phone itself is untested and unsupported; use the app's on-device MCP mode instead.`,
  },
  {
    id: 'start-server',
    title: 'Start the server',
    body: `One command brings up all three listeners: ${PORTS.map((p) => `${p.port} for the ${p.name}`).join(', ')}. The terminal prints the exact WebSocket address to type into the phone.`,
    command: 'aster start',
    note: `Keep these ports on your LAN or your tailnet. Do not port-forward ${PORTS[0].port} or ${PORTS[1].port} to the public internet — the device link is plain ws:// and terminates no TLS of its own.`,
  },
  {
    id: 'install-app',
    title: 'Install the Android companion',
    body: `Download the latest APK from GitHub Releases and sideload it. It runs on ${FACTS.androidMinLabel} and needs no root. Use your daily phone, or a spare Android you want to hand over to your AI entirely.`,
    note: 'Accessibility Service is the one permission Aster cannot work without — it is what reads the screen and drives taps. Notifications, SMS, phone, location and storage are each optional and asked for separately.',
  },
  {
    id: 'connect-device',
    title: 'Point the phone at the server',
    body: `Open Aster on the phone, enter the WebSocket address the terminal printed — ${ENDPOINTS.deviceWs} — and tap Connect. For a dedicated AI phone, plug it into a charger and leave it there.`,
    note: 'The address is a LAN IP and is only valid on that network. If you move between Wi-Fi networks, join both ends to Tailscale and use the stable tailnet address instead.',
  },
  {
    id: 'approve-device',
    title: 'Approve the device',
    body: `A new device arrives as pending and can connect but not be commanded. Approve it from the CLI, or click Approve in the dashboard at ${ENDPOINTS.dashboard}.`,
    command: 'aster devices approve <deviceId>',
    note: 'A device left pending is the single most common setup failure: it shows as connected while every tool call times out. There are no tokens or per-device keys — approval is the whole access-control model.',
  },
  {
    id: 'connect-client',
    title: 'Point your AI client at the MCP endpoint',
    body: `Add Aster to your client as a streamable-HTTP MCP server at ${ENDPOINTS.mcp}. All ${TOOL_COUNTS.mcpServer} tools then appear namespaced ${TOOL_PREFIX}*, so a call looks like ${TOOL_PREFIX}take_screenshot.`,
    command: MCP_JSON,
    lang: 'json',
    note: `One address per audience: ${PORTS[0].port} goes in the phone, ${PORTS[1].port}/mcp goes in the AI client, ${PORTS[2].port} goes in your browser. Pasting the wrong port into the wrong app is the second most common setup failure.`,
  },
]

/**
 * "Before you start" — the three facts that decide whether the install can
 * work at all, and the note that answers the two questions people ask next.
 *
 * These were page-local consts in app/pages/setup.vue, so /setup.md never
 * carried them: the twin stated the Node and Android requirements in one
 * prose line and said nothing about the network, which is the requirement
 * that actually strands people.
 */
export const REQUIREMENTS: { term: string; value: string; detail: string; icon: string }[] = [
  {
    term: 'Server',
    value: `Node.js ${FACTS.nodeRequirement}`,
    detail: 'Any machine that stays awake: laptop, desktop, mini PC or NAS. macOS, Linux and Windows all work.',
    icon: 'lucide:server',
  },
  {
    term: 'Phone',
    value: FACTS.androidMinLabel,
    detail: `The companion APK pins minSdk ${FACTS.androidMinApi}. Anything older cannot install it, and no root is needed on anything newer.`,
    icon: 'lucide:smartphone',
  },
  {
    term: 'Network',
    value: 'Same LAN, or Tailscale',
    detail: 'The phone dials out to the server, so the two only need to see each other — no inbound port forwarding.',
    icon: 'lucide:wifi',
  },
]

/**
 * Split around the one hyperlink in it. The page renders `link` as an anchor to
 * LINKS.tailscale and the twin renders it as a markdown link, so the sentence
 * exists once rather than once per surface.
 */
export const REQUIREMENTS_NOTE = {
  before:
    'No root, no ADB cable and no Google account are required at any point. If the phone and the server cannot sit on the same LAN, join both to ',
  link: 'Tailscale',
  href: LINKS.tailscale,
  after: ' and use the tailnet address everywhere this page says “server address”.',
} as const

/**
 * What each verification command PROVES, which is the half that matters.
 *
 * The twin used to print two bare commands under "Checking it works" and none
 * of the interpretation, dropping the third check entirely — and that third one
 * is the only place on the site that maps a symptom to a cause: an empty tool
 * list means the client never reached the MCP endpoint, a hanging call means
 * the device is still pending.
 */
export const VERIFY_INTRO =
  'Two commands separate “the server is up” from “the phone is reachable”. Run both before blaming the AI client.'

export const VERIFY_COMMANDS = `aster status
curl ${ENDPOINTS.health}`

export const VERIFY_CHECKS: { term: string; def: string }[] = [
  {
    term: 'aster status',
    def: `Prints the human-readable snapshot the running server writes to ${PATHS.status}: the advertised addresses, and every device with its approval state. A device listed as pending is connected but cannot be commanded yet.`,
  },
  {
    term: `curl ${ENDPOINTS.health}`,
    def: 'Returns { "status": "ok", "timestamp": ... }. This is the endpoint to poll from scripts and monitors; GET /api/stats next to it returns device counts.',
  },
  {
    term: 'Your AI client',
    def: `Ask it to list its tools. You should see ${TOOL_COUNTS.mcpServer} names, all beginning ${TOOL_PREFIX} — ${TOOL_PREFIX}take_screenshot is the cheapest one to try first. If the list is empty, the client never reached ${ENDPOINTS.mcp}; if a call hangs, the device is still pending.`,
  },
]

export const CLIENTS: ClientDef[] = [
  {
    id: 'claude',
    name: 'Claude Code and Claude Desktop',
    body: `Add the entry below to your project's .mcp.json, or to the Claude Desktop MCP settings, and restart the client. Aster registers ${TOOL_COUNTS.mcpServer} tools, every one of them prefixed ${TOOL_PREFIX}.`,
    snippet: MCP_JSON,
    lang: 'json',
    icon: 'lucide:brain',
    accent: 'var(--color-warning)',
  },
  {
    id: 'openclaw',
    name: 'OpenClaw, MoltBot and ClawdBot',
    body: 'Aster ships as a skill on ClawHub, the registry agent clients such as OpenClaw, MoltBot and ClawdBot install skills from. Install it from the hub, or fetch the skill file straight from the repository if your client takes a raw URL.',
    snippet: `clawhub install aster

# or take the skill file directly:
curl -O ${LINKS.skillRaw}`,
    lang: 'bash',
    icon: 'lucide:terminal-square',
    accent: 'var(--color-info)',
    href: LINKS.clawhub,
  },
  {
    id: 'any-mcp',
    name: 'AnythingLLM and any MCP client',
    body: 'There is nothing Aster-specific to install. The server speaks standard streamable-HTTP MCP, so any compliant client works by pointing it at the endpoint. On a LAN, swap localhost for the server machine’s address.',
    snippet: ENDPOINTS.mcp,
    lang: 'text',
    icon: 'lucide:plug',
    accent: 'var(--color-mode-remote)',
  },
  {
    id: 'openally',
    name: 'OpenAlly',
    body: `OpenAlly runs on the same phone and drives Aster over Android Binder IPC — no server, no network hop, nothing to configure. Install both apps, approve the on-device handshake once, and OpenAlly has the full on-device action catalog (${TOOL_COUNTS.onDevice} actions, unprefixed). This is the only path that needs no MCP endpoint at all.`,
    icon: 'lucide:smartphone',
    accent: 'var(--color-primary)',
    brand: 'openally',
    href: LINKS.openally,
  },
]

/**
 * Aster's facts, in one place.
 *
 * EVERY number, version, port and URL the site prints comes from here, and so
 * do the generated sitemap, llms.txt and the per-route markdown twins
 * (scripts/generate-machine-readable.ts). If a fact is not in this file, it is
 * about to drift — that is exactly how the site came to claim "Android 7.0+"
 * while apps/android/app/build.gradle.kts:16 pinned minSdk = 26.
 *
 * Provenance for each field is noted inline. Check it against the cited file
 * before changing a value.
 */

export const SITE = {
  name: 'Aster',
  /** No trailing slash. Canonicals, og:url and the sitemap all build from this. */
  origin: 'https://aster.matterwardlabs.com',
  tagline: 'Your AI copilot on mobile — or give your AI its own phone',
  author: 'Satyajit Pradhan',
  authorUrl: 'https://github.com/satyajiit',
  publisher: 'Matterward Labs',
  publisherUrl: 'https://matterwardlabs.com',
  ogImage: 'https://aster.matterwardlabs.com/og-card.png',
  ogImageWidth: 1200,
  ogImageHeight: 630,
} as const

export const LINKS = {
  repo: 'https://github.com/satyajiit/aster-mcp',
  releases: 'https://github.com/satyajiit/aster-mcp/releases',
  issues: 'https://github.com/satyajiit/aster-mcp/issues',
  npm: 'https://www.npmjs.com/package/aster-mcp',
  clawhub: 'https://clawhub.ai/satyajit/aster',
  /** Repo is `aster-mcp`, not `Aster`. The old URL 404s. */
  skillRaw: 'https://raw.githubusercontent.com/satyajiit/aster-mcp/main/skill/SKILL.md',
  openally: 'https://openally.ai',
  mcpSpec: 'https://modelcontextprotocol.io/',
  youtube: 'https://youtube.com/@GamesPatch',
  tailscale: 'https://tailscale.com/',
} as const

export const FACTS = {
  /** mcp/package.json → name */
  npmPackage: 'aster-mcp',
  /** mcp/package.json → version */
  serverVersion: '0.1.16',
  /** apps/android/app/build.gradle.kts → versionName */
  appVersion: '1.7.1',
  /** mcp/package.json → engines.node */
  nodeRequirement: '>= 20',
  /**
   * apps/android/app/build.gradle.kts:16 → minSdk = 26.
   * API 26 is Android 8.0 Oreo. The README badge and llms.txt both said
   * "Android 7+"; both were wrong and are corrected from this constant.
   */
  androidMinApi: 26,
  androidMinVersion: '8.0',
  androidMinLabel: 'Android 8.0+ (API 26)',
  /** apps/android/app/build.gradle.kts → targetSdk / compileSdk */
  androidTargetApi: 36,
  license: 'MIT',
  price: '0',
  priceCurrency: 'USD',
  rootRequired: false,
  telemetry: false,
} as const

export const PORTS = [
  { port: 5987, name: 'Device WebSocket', detail: 'The Android companion connects here.' },
  { port: 5988, name: 'API + MCP HTTP', detail: 'Streamable-HTTP MCP endpoint at /mcp; REST API at /api.' },
  { port: 5989, name: 'Web dashboard', detail: 'Device registry, approvals, live screen control, logs.' },
] as const

export const ENDPOINTS = {
  mcp: 'http://localhost:5988/mcp',
  health: 'http://localhost:5988/api/health',
  dashboard: 'http://localhost:5989',
  deviceWs: 'ws://<server-ip>:5987',
} as const

/**
 * Tool counts differ BY SURFACE. Saying "49 tools" without naming the surface
 * is false in Local-MCP and IPC mode.
 *
 * - mcpServer: `grep -c "name: 'aster_" mcp/src/mcp/tools.ts` → 49. These are
 *   the names an MCP client displays, and they all carry the `aster_` prefix.
 *
 * - onDevice: 77. The authority is NOT ToolCatalog.kt (that file only carries
 *   display metadata, 69 rows, and is used to label the app's own dashboard).
 *   It is the handler map built in apps/android/.../di/ModeModule.kt →
 *   provideCommandHandlers(), which registers 24 handlers and keys the map by
 *   every name each one returns from supportedActions(); the union is 77 unique
 *   actions. Both on-device surfaces expose exactly that map: McpMode calls
 *   McpToolRegistry.registerTools(commandHandlers.keys) so the on-device Ktor
 *   MCP server advertises all 77, and IpcMode dispatches Binder calls through
 *   the same map.
 *
 *   Do NOT re-derive this by eye. docs/scripts/verify-facts.ts recomputes it
 *   from the Kotlin source on every build and throws if it drifts, so a wrong
 *   number here fails CI instead of shipping. (A naive
 *   `sed -n '/supportedActions()/,/)/p'` under-counts to 69 — it stops at the
 *   first `)`, truncating AccessibilityHandler's 18-name list.)
 *
 * - The two catalogues are DIFFERENT SETS, not nested. Count by REACHABILITY,
 *   not by string equality, or the arithmetic does not close:
 *
 *     48 on-device actions are reachable from an MCP client — 47 share a name
 *        with an aster_* tool, and click_by_view_id is reached by
 *        aster_click_by_id, which is a rename rather than an extra capability
 *        (mcp/src/mcp/handler.ts → sendCommand(deviceId, 'click_by_view_id', …)).
 *     29 on-device actions no MCP client can reach — observe, tap, scroll,
 *        set_text, set_toggle, perform, press_key, wait_for, wait_for_idle,
 *        long_press, get_now_playing, count_sms, the notification-dismissal
 *        pair, three of the four overlay verbs (show_overlay IS reachable), the
 *        four companion_overlay_* verbs, the six screen_* control verbs, and
 *        the two dotted files.* host-directory actions.
 *      1 server tool has no on-device action behind it: aster_list_devices (it
 *        brokers between phones, which is meaningless on the phone itself).
 *
 *   Both sums close: 48 + 29 = 77, and 48 + 1 = 49. An earlier revision defined
 *   `shared` as name equality (47), which made the published prose read
 *   "47 appear in both … 29 unreachable … 1 server-only" — 76 and 48, neither of
 *   which is a catalogue size. verify-facts.ts now derives `shared` from the
 *   server mapping in ./on-device and asserts both sums, so the definitions
 *   cannot drift apart again.
 */
export const TOOL_COUNTS = {
  mcpServer: 49,
  onDevice: 77,
  /**
   * On-device actions an MCP client can reach. Counted by reachability, not by
   * name: 47 identical names plus click_by_view_id via aster_click_by_id.
   */
  shared: 48,
  /** On-device actions with no `aster_*` equivalent. */
  onDeviceOnly: 29,
  /** Server tools with no on-device action behind them: aster_list_devices. */
  serverOnly: 1,
} as const

/**
 * One sentence that resolves the name, for every surface that needs it.
 *
 * "Aster" collides with much larger entities — Aster DM Healthcare, Aster Data
 * Systems, the ASTER instrument on NASA's Terra satellite — so a bare "Aster"
 * query has no reason to land here. The home page says this in prose and in its
 * SoftwareApplication disambiguatingDescription, but an assistant that fetches
 * /tools.md or /security.md never sees either, so the twins and llms.txt carry
 * it too.
 */
export const DISAMBIGUATION =
  'Aster here is the open-source Model Context Protocol server and Android companion app for AI-driven phone control, published on npm as aster-mcp. It is unrelated to Aster DM Healthcare, Aster Data Systems or the ASTER instrument on NASA\u2019s Terra satellite.'

/**
 * Where the server keeps things on your machine. Answers "where is my data and
 * how do I delete it", which nothing on the site could answer before.
 *
 * Note the split, which is easy to get wrong: the SQLite database is resolved
 * RELATIVE TO THE WORKING DIRECTORY (mcp/src/index.ts:113 —
 * `process.env.DB_PATH || './aster.db'`), so it lands wherever you ran
 * `aster start`. Only the runtime state files live under ~/.aster
 * (mcp/src/index.ts:23, websocket/index.ts:32,36, event-forwarding/index.ts:54).
 */
export const PATHS = {
  db: './aster.db',
  dbEnv: 'DB_PATH',
  stateDir: '~/.aster',
  status: '~/.aster/status.json',
  pid: '~/.aster/aster.pid',
  eventForwarding: '~/.aster/event-forwarding.json',
} as const

/** Namespace rule, stated once and reused wherever tool names are printed. */
export const TOOL_PREFIX = 'aster_'

export const CONNECTION_MODES = [
  {
    id: 'remote',
    name: 'Remote WebSocket',
    badge: 'Default',
    summary:
      'The phone holds a WebSocket to the Node server on your machine; your AI client speaks MCP over HTTP to that same server.',
    toolNamespace: `${TOOL_PREFIX}* (${TOOL_COUNTS.mcpServer} tools)`,
  },
  {
    id: 'mcp',
    name: 'On-device MCP server',
    badge: 'Standalone',
    summary:
      'The app runs its own Streamable-HTTP MCP server (Ktor, default port 8080) on the phone. No desktop server involved.',
    toolNamespace: `unprefixed actions (${TOOL_COUNTS.onDevice} in the on-device catalog)`,
  },
  {
    id: 'ipc',
    name: 'Binder IPC',
    badge: 'Same device',
    summary:
      'An agent running on the same phone — such as OpenAlly — calls Aster directly over Android Binder IPC, with no network hop.',
    toolNamespace: `unprefixed actions (${TOOL_COUNTS.onDevice} in the on-device catalog)`,
  },
] as const

/** Rendered as a <dl> on the home route and as the Quick facts block in the twins. */
export const QUICK_FACTS: { term: string; def: string }[] = [
  { term: 'What it is', def: 'An MCP server plus an Android companion app that lets an AI assistant see and control a phone.' },
  { term: 'Server', def: `npm \`${FACTS.npmPackage}\`, Node.js ${FACTS.nodeRequirement}` },
  { term: 'Phone', def: `${FACTS.androidMinLabel}, no root required` },
  { term: 'Tools', def: `${TOOL_COUNTS.mcpServer} MCP tools, all namespaced \`${TOOL_PREFIX}*\`` },
  { term: 'Clients', def: 'Any MCP client: Claude Code, Claude Desktop, AnythingLLM, and the open-source agent clients OpenClaw, MoltBot and ClawdBot' },
  { term: 'Ports', def: PORTS.map((p) => `${p.port} ${p.name.toLowerCase()}`).join(', ') },
  { term: 'Licence', def: `${FACTS.license}, free and open source` },
  { term: 'Hosting', def: 'Self-hosted on your own machine. No account, no telemetry, no vendor relay.' },
]

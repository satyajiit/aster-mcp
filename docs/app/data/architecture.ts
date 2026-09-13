/**
 * How a command reaches the phone — the /architecture route's content.
 *
 * Provenance for every claim below:
 *   - the hop sequence: mcp/src/mcp/handler.ts (schema parse + device resolve)
 *     and mcp/src/websocket/index.ts (sendCommand: UUID command id, the
 *     {type:'command', id, action, params} frame, the 30 000 ms default
 *     timeout, the `status !== 'approved'` rejection)
 *   - the three transports: README.md "Connection Modes"
 *   - tool names and counts: app/data/site.ts, which cites mcp/src/mcp/tools.ts
 *     and apps/android/.../mode/ToolCatalog.kt
 *
 * Two corrections to earlier copy are load-bearing here and must not regress:
 *
 * 1. The MCP tool is `aster_take_screenshot`; the WebSocket action is
 *    `take_screenshot`. The server strips the prefix on dispatch
 *    (handler.ts → sendCommand(deviceId, 'take_screenshot', …)). The site used
 *    to print the unprefixed form as though a client could call it. It cannot.
 *
 * 2. README.md says all three modes "share the same 49 tools". They do not.
 *    The server exposes TOOL_COUNTS.mcpServer prefixed tools; the on-device
 *    catalog holds TOOL_COUNTS.onDevice unprefixed actions, which is a
 *    DIFFERENT set — it adds actions the server never exposes and omits five
 *    that only the server has. See the note in site.ts → TOOL_COUNTS.
 */

// Relative, not `~/data/site`: scripts/generate-machine-readable.ts imports this
// module from Node during the nitro build, where the Nuxt `~` alias does not
// exist. The generator itself imports the data dir relatively for the same
// reason. A `~` here builds in dev and fails the production generate.
import { ENDPOINTS, PORTS, TOOL_COUNTS, TOOL_PREFIX } from './site'

export interface TraceStep {
  /** Who is acting at this hop. */
  actor: string
  /** What actually happens, in one or two sentences. */
  detail: string
  /** Iconify name. */
  icon: string
  /** CSS colour for the hop's accent. A design token, never a raw hex. */
  accent: string
  /** Timing that is documented in code or measured, not guessed. Omit when neither. */
  latency?: string
}

export interface Transport {
  id: string
  name: string
  /** Where the thing a client talks to actually runs. */
  host: string
  /** The tool names a client sees on this transport. They are NOT the same set. */
  namespace: string
  /** When to pick this one. */
  when: string
}

export interface WorkedExample {
  id: string
  /** What a person types at their assistant. */
  prompt: string
  steps: { label: string; detail: string }[]
  /** Full, callable tool names — prefixed, exactly as an MCP client lists them. */
  tools: string[]
}

/**
 * One command, six hops. This is the Remote WebSocket path, which is the
 * default and the only one with a device-approval gate.
 */
export const COMMAND_TRACE: TraceStep[] = [
  {
    actor: 'Your AI client',
    detail:
      `The assistant picks a tool and calls it by its full name — ${TOOL_PREFIX}take_screenshot, not take_screenshot — over Streamable HTTP to ${ENDPOINTS.mcp}. Arguments travel as JSON. Nothing in this hop knows a phone exists.`,
    icon: 'lucide:brain',
    accent: 'var(--color-info-bright)',
  },
  {
    actor: 'Aster server — admit',
    detail:
      'The MCP handler parses the arguments against that tool\'s schema and resolves the target device. A device that was never approved, or that has no live socket in this process, is refused here. A rejected call never reaches the phone and never touches a permission.',
    icon: 'lucide:shield-check',
    accent: 'var(--color-primary)',
  },
  {
    actor: 'Aster server — dispatch',
    detail:
      `sendCommand mints a UUID, drops the ${TOOL_PREFIX} prefix and pushes a single JSON frame down the device WebSocket on port ${PORTS[0].port}. The promise it returns is held open against that id.`,
    icon: 'lucide:radio-tower',
    accent: 'var(--color-mode-remote)',
    latency: '30 s timeout',
  },
  {
    actor: 'Android companion',
    detail:
      'The command handler on the phone dispatches the action to whichever subsystem owns it — the accessibility service, MediaStore, telephony, CameraX. The kill-switch notification and the fail-closed package policy apply before any screen control runs, in every transport.',
    icon: 'lucide:smartphone',
    accent: 'var(--color-success-bright)',
    latency: '~90 ms to ~10 s',
  },
  {
    actor: 'Result frame',
    detail:
      'The phone answers on the same socket, carrying the id it was handed. The server matches that id to the waiting promise and clears the timeout. An answer that arrives late, or for a command nobody is waiting on, is dropped rather than resolved.',
    icon: 'lucide:corner-up-left',
    accent: 'var(--color-primary)',
  },
  {
    actor: 'Back to the model',
    detail:
      'The handler shapes the payload into MCP content — text for structured results, a base64 image for screenshots and photos — and returns it as the tool result. It goes to your client and nowhere else; the server keeps no copy off your machine.',
    icon: 'lucide:check-circle',
    accent: 'var(--color-primary)',
  },
]

/**
 * Three transports. They differ in where the server lives, how trust is
 * established, and — the part earlier copy got wrong — which tool names a
 * client actually sees.
 */
export const TRANSPORTS: Transport[] = [
  {
    id: 'remote',
    name: 'Remote WebSocket',
    host: `Node server on your machine. Phone dials out to port ${PORTS[0].port}; clients speak MCP over HTTP on port ${PORTS[1].port}.`,
    namespace: `${TOOL_PREFIX}* — ${TOOL_COUNTS.mcpServer} MCP tools`,
    when: 'The default. You run the server on a laptop or a home box and point any MCP client at it. The only mode with the server-side device-approval gate.',
  },
  {
    id: 'mcp',
    name: 'On-device MCP server',
    host: 'Ktor plus the MCP Kotlin SDK, embedded in the app and running on the phone itself. Default port 8080. No Node server in the middle.',
    namespace: `unprefixed actions — ${TOOL_COUNTS.onDevice} in the on-device catalog`,
    when: 'You want a client to reach the phone directly, on the LAN or over a private mesh. Trust is whatever your own network controls give you.',
  },
  {
    id: 'ipc',
    name: 'Binder IPC',
    host: 'Same device, no network hop at all. An app on the phone — OpenAlly, for example — binds Aster\'s service directly.',
    namespace: `unprefixed actions — ${TOOL_COUNTS.onDevice} in the on-device catalog`,
    when: 'An agent already running on the phone drives it locally: a 32-character token checked in constant time plus an on-device approval prompt. Lowest latency, works with the radio off.',
  },
]

/**
 * Five traces, not six. The old page ran a separate "AI calls you" scenario
 * that was mechanically identical to the voice call — same tool, same server
 * route, same device execution, same latency — so it is folded into the voice
 * call's result line instead of repeated.
 */
export const WORKED_EXAMPLES: WorkedExample[] = [
  {
    id: 'screenshot',
    prompt: 'Take a screenshot of my phone',
    steps: [
      { label: 'Tool call', detail: `${TOOL_PREFIX}take_screenshot with the device id — the only required argument.` },
      { label: 'Server', detail: 'Resolves the approved device and sends the action take_screenshot down the socket.' },
      // JPEG, not PNG: AsterAccessibilityService.kt:1359 is
      // `bitmapToWrite.compress(Bitmap.CompressFormat.JPEG, 75, out)`, and
      // ToolCatalog.kt:46 says "Capture screen as JPEG" too. image/png survives
      // only as a fallback default in mcp/src/mcp/handler.ts:396, used when the
      // device reports no mime type at all.
      { label: 'Device', detail: 'The accessibility service grabs the current frame and encodes it as a JPEG at quality 75.' },
      { label: 'Result', detail: 'The capture comes back as base64 image content the model can actually look at. Large frames are written to device storage first and fetched with a follow-up read, so a big screenshot cannot blow up the WebSocket frame.' },
    ],
    tools: [`${TOOL_PREFIX}take_screenshot`, `${TOOL_PREFIX}get_screen_hierarchy`],
  },
  {
    id: 'media',
    prompt: 'Find all my beach photos from last December',
    steps: [
      { label: 'Tool call', detail: `${TOOL_PREFIX}search_media with the sentence as written; the server parses it into a date window plus keywords.` },
      { label: 'Server', detail: 'Sends search_media with the parsed filter rather than the raw sentence, so the phone does no language work.' },
      { label: 'Device', detail: 'A MediaStore query, then an EXIF and location pass over the matches.' },
      { label: 'Result', detail: 'Matching photos come back as metadata with timestamps and locations intact. The images themselves stay on the phone until you ask for one.' },
    ],
    tools: [`${TOOL_PREFIX}index_media_metadata`, `${TOOL_PREFIX}search_media`],
  },
  {
    id: 'vibrate',
    prompt: 'Vibrate my phone, I dropped it behind the couch',
    steps: [
      { label: 'Tool call', detail: `${TOOL_PREFIX}vibrate with a waveform pattern of [0, 500, 200, 500].` },
      { label: 'Server', detail: 'Nothing to marshal beyond the pattern array — the shortest path through the handler.' },
      { label: 'Device', detail: 'Vibrator.vibrate with a waveform effect built from the pattern.' },
      { label: 'Result', detail: 'Two 500 ms pulses with a 200 ms gap. The fastest trace on this page, about 90 ms round trip.' },
    ],
    tools: [`${TOOL_PREFIX}vibrate`, `${TOOL_PREFIX}play_audio`],
  },
  {
    id: 'voice-call',
    prompt: 'Call Mom and tell her I will be about 20 minutes late',
    steps: [
      { label: 'Tool call', detail: `${TOOL_PREFIX}make_call_with_voice with the number, the sentence to speak, and waitSeconds: 8.` },
      { label: 'Server', detail: 'Routes make_call_with_voice and then waits. The eight seconds are spent on the device while the call connects, not in the server.' },
      { label: 'Device', detail: 'A call intent, speakerphone forced on, then text-to-speech says the sentence once the line is up.' },
      { label: 'Result', detail: 'The message is delivered on speakerphone after the wait. About 10 s end to end, nearly all of it call setup. This is also the tool a dedicated AI phone uses to ring you unprompted — same call, nobody typed the prompt.' },
    ],
    tools: [`${TOOL_PREFIX}make_call_with_voice`, `${TOOL_PREFIX}make_call`, `${TOOL_PREFIX}speak_tts`],
  },
  {
    id: 'notifications',
    prompt: 'Anything urgent on my phone?',
    steps: [
      { label: 'Tool call', detail: `${TOOL_PREFIX}read_notifications with a priority filter.` },
      { label: 'Server', detail: 'Sends read_notifications; the filtering happens against what the phone reports, not a cached copy.' },
      { label: 'Device', detail: 'The notification listener reads the shade and ranks what is there — 14 active at the time of this call.' },
      { label: 'Result', detail: 'Two urgent messages, one missed call, one delivery window. About 150 ms round trip, which is why this is the one people leave running.' },
    ],
    tools: [`${TOOL_PREFIX}read_notifications`, `${TOOL_PREFIX}post_notification`],
  },
]

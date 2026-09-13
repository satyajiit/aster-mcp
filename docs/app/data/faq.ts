/**
 * The FAQ corpus — one answer per question, each one self-contained.
 *
 * Read by app/pages/faq.vue (visible accordion + FAQPage JSON-LD) and by
 * scripts/generate-machine-readable.ts (the /faq.md twin and llms-full.txt).
 *
 * THREE RULES, because this file is consumed by answer engines:
 *
 * 1. Every answer must survive being lifted out of the page on its own. An
 *    answer engine quotes one `a` string with no surrounding context, so each
 *    one restates the question in its first clause instead of opening with
 *    "Yes" or "It depends" and leaning on the heading above it.
 * 2. Plain prose only — no markdown, no HTML, no pipe characters. The same
 *    string is rendered as text in the accordion, embedded verbatim in the
 *    FAQPage JSON-LD (Google requires the two to match exactly) and pasted
 *    into a markdown twin whose comparison table is pipe-delimited.
 * 3. Never hardcode a fact. Ports, versions, tool counts and the tool-name
 *    prefix are interpolated from ~/data/site so a change there cannot leave a
 *    stale number stranded in an answer.
 *
 * Provenance: the first seven entries are the Troubleshooting & FAQ block in
 * the repo README, converted faithfully. The rest are questions the site
 * answered nowhere — including the Android version, which the README badge
 * still gets wrong (it says Android 7; apps/android/app/build.gradle.kts:16
 * sets minSdk = 26, which is Android 8.0).
 */

// Relative, not `~/data/site`. This module is imported twice: by Vue components
// through Vite (where `~` resolves) and by scripts/generate-machine-readable.ts
// through the jiti-loaded nitro hook in nuxt.config.ts (where it may not). A
// relative specifier is the one form both resolvers agree on.
import { ENDPOINTS, FACTS, LINKS, PATHS, PORTS, TOOL_COUNTS, TOOL_PREFIX } from './site'

export interface Faq {
  /** Stable slug. Used as the DOM id, so in-page links survive a reorder. */
  id: string
  q: string
  a: string
  /** Accordion group heading. Groups render in first-appearance order. */
  topic: string
}

export interface Comparison {
  columns: string[]
  rows: { label: string; values: string[] }[]
}

/**
 * The four accordion groups. `topic` is what both renderers group on: the
 * accordion on the page, and now the /faq.md twin, which used to emit a flat
 * list — losing the four headings AND reordering all 18 questions relative to
 * the page, so the two could not be read against each other. See FAQ_TOPICS.
 */
const TOPIC_REQUIREMENTS = 'Requirements and cost'
const TOPIC_CONNECTION = 'Connecting and troubleshooting'
const TOPIC_PRIVACY = 'Security and privacy'
const TOPIC_TOOLS = 'Tools and behaviour'

/** The page lede. It states where every answer below comes from. */
export const FAQ_LEDE =
  "Everything here comes out of the source: the repo's troubleshooting notes, the tool definitions on the MCP server, and the Android module's own build configuration. Where a published claim disagrees with the code, the code wins and the answer says so."

/** The paragraph that frames COMPARISON. Without it the table reads as a pitch. */
export const COMPARISON_INTRO =
  'Both are excellent — for a person at a keyboard with a USB cable. Aster solves a different problem: it is not a screen mirror but a tool layer an AI agent calls by name, with the approval gate and kill switch a mirror was never meant to have.'

/**
 * The three triage commands the page closes with. This lived twice: as a
 * page-local `diagnostics` const in app/pages/faq.vue and as a hand-copied
 * DIAGNOSTICS const in the generator, which is the shape that drifts.
 */
export const DIAGNOSTIC_COMMANDS = `aster status
aster devices list
curl ${ENDPOINTS.health}`

export const FAQS: Faq[] = [
  // ── Requirements and cost ────────────────────────────────────────────────
  {
    id: 'android-version',
    topic: TOPIC_REQUIREMENTS,
    q: 'What Android version do I need to run the Aster companion app?',
    a:
      `The Aster Android companion app requires ${FACTS.androidMinLabel}, because apps/android/app/build.gradle.kts sets minSdk = ${FACTS.androidMinApi} and API ${FACTS.androidMinApi} is Android ${FACTS.androidMinVersion} Oreo. ` +
      `A phone on Android 7 cannot install it at all. ` +
      `The app compiles against and targets API ${FACTS.androidTargetApi}, ships an arm64-v8a build, and needs its Accessibility Service switched on by hand after install — that toggle, not the OS version, is what actually unlocks screen control.`,
  },
  {
    id: 'root-required',
    topic: TOPIC_REQUIREMENTS,
    q: 'Does Aster need root, a custom ROM or USB debugging?',
    a:
      `Aster needs none of those: it runs on a stock, unrooted phone. ` +
      `Screen control comes from an Android AccessibilityService, notifications from a NotificationListenerService, and everything else from ordinary runtime permissions you grant in the app — all of them public Android APIs. ` +
      `There is no root requirement, no custom ROM, no ADB shell and no USB cable in the loop; the phone reaches the server over the network, which is the main practical difference from scrcpy and raw ADB.`,
  },
  {
    id: 'is-it-free',
    topic: TOPIC_REQUIREMENTS,
    q: 'Is Aster free? What is the licence?',
    a:
      `Aster is free and open source under the ${FACTS.license} licence — both halves of it, the Node MCP server published to npm as ${FACTS.npmPackage} and the Android companion app. ` +
      `There is no account to create, no paid tier, no licence key and no usage metering, because Aster is self-hosted: you run the server on your own machine, so there is nothing for anyone to bill. ` +
      `The only costs you can incur are the ones your own AI client already has, such as tokens billed by whichever model provider it talks to.`,
  },

  // ── Connecting and troubleshooting ───────────────────────────────────────
  {
    id: 'device-pending',
    topic: TOPIC_CONNECTION,
    q: 'My device shows as connected but every command fails or times out. Why?',
    a:
      `A device that shows as connected in Aster but times out on every command is almost always still pending approval, because connecting and being approved are two separate states. ` +
      `A new phone self-reports its identity (a hashed Android ID, its name and model) and lands as pending: connected, but hard-blocked — the server refuses every command for any device whose status is not approved. ` +
      `Approve it from the dashboard at ${ENDPOINTS.dashboard}, or run "aster devices approve DEVICE_ID" on the server. A device you reject instead is disconnected with WebSocket close code 4003.`,
  },
  {
    id: 'network-switch',
    topic: TOPIC_CONNECTION,
    q: 'The device disconnects when I switch between Wi-Fi and mobile data. How do I fix it?',
    a:
      `The Aster companion app now reconnects by itself after a Wi-Fi or mobile-data switch: it backs off and retries, showing "Reconnecting" in the app, instead of sitting on a socket that died with the old network. ` +
      `If it never comes back, the problem is the address rather than the socket — "aster status" advertises a LAN IP that is only valid on the network the server was started on, so the phone is retrying an address that no longer exists. ` +
      `Keep both ends on one network, or point the app at a stable Tailscale address so the server's address survives the switch.`,
  },
  {
    id: 'firewall-ports',
    topic: TOPIC_CONNECTION,
    q: 'Which ports does Aster need open in my firewall?',
    a:
      `Aster listens on three local ports: ${PORTS[0].port} is the WebSocket device link the phone connects to, ${PORTS[1].port} is the API plus MCP HTTP endpoint that serves POST /mcp and the health route, and ${PORTS[2].port} serves the web dashboard on the server machine. ` +
      `In practice the phone needs to reach ${PORTS[0].port} and your AI client needs to reach ${PORTS[1].port}; ${PORTS[2].port} only has to be reachable from whatever browser you open the dashboard in. ` +
      `Allow those on your LAN or your Tailscale network, and do not port-forward any of them to the public internet — there is no shared secret on the device link, so exposure is the whole risk.`,
  },
  {
    id: 'health-check',
    topic: TOPIC_CONNECTION,
    q: 'How do I check that the Aster server is reachable from a script?',
    a:
      `To check an Aster server programmatically, request its health endpoint: "curl ${ENDPOINTS.health}" returns a small JSON body with a status of ok and a timestamp, so a non-zero exit or a missing ok is your liveness signal. ` +
      `For device counts, GET /api/stats on the same port returns the registry totals. ` +
      `For a human-readable snapshot instead, run "aster status" on the server: it prints the advertised LAN address, the listening ports and the connected devices with their approval state.`,
  },

  // ── Security and privacy ─────────────────────────────────────────────────
  {
    id: 'encryption-ssl',
    topic: TOPIC_PRIVACY,
    q: 'Is the connection encrypted? Do I need SSL?',
    a:
      `The Aster device link is not encrypted by default: on a trusted LAN the phone connects over plain ws:// to port ${PORTS[0].port}, and the Node ws server does not terminate TLS itself, so aiming wss:// at that port with nothing in front of it fails with a TLS parse error rather than upgrading. ` +
      `For remote or encrypted access you put the encryption in front of the socket — Tailscale Serve gives the app a wss:// MagicDNS address while MCP stays plain HTTP on the Tailscale IP at port ${PORTS[1].port}, or you terminate TLS at Traefik or Caddy and proxy to ws:// on localhost. ` +
      `Encryption for remote control therefore comes from WireGuard or from your own TLS terminator, never from a padlock on the Node socket.`,
  },
  {
    id: 'data-leaves',
    topic: TOPIC_PRIVACY,
    q: 'Does Aster send my data anywhere?',
    a:
      `Aster sends nothing anywhere: it has no telemetry, no analytics and no vendor relay. ` +
      `The server runs on hardware you own, the device registry and logs live in a local SQLite file next to it, and your AI client reaches the tools over a local MCP HTTP endpoint on your own network. ` +
      `The single outbound call Aster can make is the optional event-forwarding webhook, which stays off until you run "aster set-event-forwarding" and then POSTs only to the URL you supplied. ` +
      `Your AI client is a separate question: if it is backed by a hosted model, that client still sends your prompts — and whatever screenshots or text Aster hands back — to its own model provider.`,
  },
  {
    id: 'banking-apps',
    topic: TOPIC_PRIVACY,
    q: 'Can the AI open or control my banking apps?',
    a:
      `Aster blocks AI screen control over banking and payments apps by default, through an on-device guard called PackagePolicyGuard that runs regardless of what the AI asks for. ` +
      `It is fail-closed: it ships a bundled denylist covering apps such as PhonePe, Paytm, PayPal, Venmo and Binance, it refuses control actions while one of those is in the foreground unless you have explicitly allowed that app, and it also refuses when the foreground app cannot be identified at all. ` +
      `The guard gates acting rather than looking — read-only actions such as observe, screenshot, view hierarchy and find element stay permitted — so treat it as a control block rather than a blindfold, and use the persistent kill-switch notification to end a screen-control session in one tap.`,
  },

  {
    id: 'ios-support',
    topic: TOPIC_REQUIREMENTS,
    q: 'Does Aster work with an iPhone or an iPad?',
    a:
      `No. Aster is Android only, and not by omission — it drives the phone through Android's AccessibilityService and NotificationListenerService, and reads SMS, contacts and call state through Android content providers. iOS exposes no equivalent to a third-party app, so there is nothing to port. The companion is an Android APK (${FACTS.androidMinLabel}, arm64-v8a); the ${FACTS.npmPackage} server itself runs on macOS, Linux or Windows, but the phone at the other end has to be an Android device.`,
  },
  {
    id: 'multiple-devices',
    topic: TOPIC_REQUIREMENTS,
    q: 'Can one server drive more than one phone?',
    a:
      `Yes. Devices are addressed individually: every tool but one takes a deviceId, and the exception is ${TOOL_PREFIX}list_devices, which takes nothing and returns every paired phone with its connection status so your assistant can pick one. Each phone holds its own WebSocket to the same server and is approved separately, so a spare handset dedicated to the AI and your daily phone can be paired at once with different approval states. This is the one thing the on-device modes cannot do — ${TOOL_PREFIX}list_devices is the single server tool with no on-device equivalent, because brokering between phones is meaningless on the phone itself.`,
  },
  {
    id: 'install-source',
    topic: TOPIC_REQUIREMENTS,
    q: 'Is the Aster app on the Google Play Store?',
    a:
      `No. The companion app is distributed as an APK from GitHub Releases and installed by sideloading — an accessibility app that can read the screen and drive other apps is not a comfortable fit for Play's policies, and shipping it as a signed release you install deliberately is the honest packaging. Android will ask you to allow installs from that source once. The server half is on npm as ${FACTS.npmPackage}, which needs no such step.`,
  },
  {
    id: 'upgrading',
    topic: TOPIC_REQUIREMENTS,
    q: 'How do I update Aster once it is installed?',
    a:
      `The two halves update separately. For the server, \`npm install -g ${FACTS.npmPackage}\` again — it is an ordinary global npm package, so the same command installs and upgrades; \`aster --version\` tells you where you are. For the phone, download the newer APK from Releases and install it over the existing one; the signature matches, so it upgrades in place and keeps your approvals, permission grants and the accessibility toggle. Approved devices survive both, because the approval lives in the server's database, not on the handset.`,
  },
  {
    id: 'data-location',
    topic: TOPIC_PRIVACY,
    q: 'Where does Aster store my data, and how do I delete it?',
    a:
      `Two plain files on the machine running the server, and nothing anywhere else. Devices, approvals and tool-call logs go into a SQLite database at \`${PATHS.db}\` — resolved relative to the directory you ran \`aster start\` from, or wherever the \`${PATHS.dbEnv}\` environment variable points. Runtime state lives in \`${PATHS.stateDir}\`: \`${PATHS.status}\`, \`${PATHS.pid}\`, and \`${PATHS.eventForwarding}\` if you turned forwarding on. Stop the server and delete both and nothing of Aster's remains. There is no cloud copy, no account and therefore no deletion request to file.`,
  },

  // ── Tools and behaviour ──────────────────────────────────────────────────
  {
    id: 'tool-count',
    topic: TOPIC_TOOLS,
    q: 'How many tools does Aster have, and what are they called?',
    a:
      `The number depends on which surface you are talking to. The Aster MCP server registers ${TOOL_COUNTS.mcpServer} tools, and every one of them carries the ${TOOL_PREFIX} prefix — an MCP client sees ${TOOL_PREFIX}take_screenshot, ${TOOL_PREFIX}send_sms and ${TOOL_PREFIX}make_call_with_voice, so calling take_screenshot without the prefix is not a valid tool name. ` +
      `The on-device catalog used by the app's own MCP server and by Binder IPC is a different set of ${TOOL_COUNTS.onDevice} unprefixed actions, and neither catalogue contains the other. ` +
      `${TOOL_COUNTS.shared} of the on-device actions are reachable from an MCP client, ${TOOL_COUNTS.onDeviceOnly} are not — observe, tap, scroll, set_text, perform, press_key, the wait verbs, three of the four overlay verbs, the companion-face verbs, and the screen_* human-in-the-loop verbs. Going the other way, exactly ${TOOL_COUNTS.serverOnly} server tool has no on-device counterpart, ${TOOL_PREFIX}list_devices, because brokering between phones is meaningless on the phone itself; ${TOOL_PREFIX}click_by_id looks like a second one but is only a rename of the device action click_by_view_id. ` +
      `Quoting a single number without naming the surface is what makes tool counts wrong.`,
  },
  {
    id: 'call-no-audio',
    topic: TOPIC_TOOLS,
    q: 'The AI placed a call but the other side heard no audio. What went wrong?',
    a:
      `When ${TOOL_PREFIX}make_call_with_voice dials successfully but nobody hears the message, that is usually the design rather than a fault: the text is spoken by the phone's own text-to-speech over the loudspeaker and reaches the call by acoustic coupling — the loudspeaker feeding the call microphone — because Android does not let an app inject audio into the call path. ` +
      `It follows that quality is device-dependent and a quiet room matters a great deal. ` +
      `Check that CALL_PHONE, READ_PHONE_STATE and MODIFY_AUDIO_SETTINGS are granted, that the phone is not on silent and the call audio is not routed to a headset or a car, and remember the tool waits for the call to be answered before it speaks (8 seconds by default, adjustable with the waitSeconds parameter).`,
  },
  {
    id: 'no-chat-screen',
    topic: TOPIC_TOOLS,
    q: 'Why does the Aster app have no chat screen?',
    a:
      `The Aster app has no chat screen on purpose. Aster is the device-side companion that holds the sensitive permissions — accessibility, SMS, contacts, files, camera — so it stays deliberately small and auditable, with no model providers, no API keys and no conversation storage inside the app that holds those permissions. ` +
      `The app you chat with is OpenAlly (${LINKS.openally}), which drives Aster on the same phone over Binder IPC, so in practice there is no app-switching: you type in OpenAlly and Aster does the device work. ` +
      `Any other MCP client — Claude Code, Claude Desktop, AnythingLLM, OpenClaw — reaches exactly the same tools through the server or through the on-device MCP mode.`,
  },
]

/**
 * Group order: first appearance in FAQS, derived rather than listed.
 *
 * app/components/FaqAccordion.vue computes exactly this, so deriving it here is
 * what keeps the twin's four `##` sections in the page's order. A hand-written
 * list would silently disagree the first time a question is moved.
 */
export const FAQ_TOPICS: string[] = FAQS.reduce<string[]>((acc, f) => {
  if (!acc.includes(f.topic)) acc.push(f.topic)
  return acc
}, [])

/**
 * "Why not just scrcpy or ADB?" as structured data.
 *
 * Faithful to the README table, with two corrections: the tool names carry the
 * real ${TOOL_PREFIX} prefix an MCP client would have to type, and no cell
 * contains a pipe character, because the markdown twin renders these rows into
 * a pipe-delimited table.
 */
export const COMPARISON: Comparison = {
  columns: ['scrcpy / raw ADB', 'Aster'],
  rows: [
    {
      label: 'Connection',
      values: [
        'A USB cable, or USB debugging enabled over the LAN',
        'A companion app over the network, with no USB debugging',
      ],
    },
    {
      label: 'Remote access',
      values: [
        'Needs tunnelling or ADB-over-TCP set up by hand',
        'Works from anywhere over Tailscale, with no port forwarding',
      ],
    },
    {
      label: 'Interface',
      values: [
        'A mirrored screen plus raw shell commands',
        `High-level MCP tools an AI calls by name: ${TOOL_PREFIX}take_screenshot, ${TOOL_PREFIX}click_by_text, ${TOOL_PREFIX}send_sms, ${TOOL_PREFIX}make_call_with_voice`,
      ],
    },
    {
      label: 'Built for',
      values: [
        'A person driving the phone manually at a keyboard',
        'An AI agent acting on its own, behind an approval gate and a kill switch',
      ],
    },
    {
      label: 'Safety rails',
      values: [
        'None beyond what the shell itself enforces',
        'Per-device approval, a fail-closed banking denylist, and a persistent stop notification',
      ],
    },
    {
      label: 'Events',
      values: [
        'Pull only — you ask, it answers',
        'Pushes SMS and notification events to your AI as they arrive',
      ],
    },
  ],
}

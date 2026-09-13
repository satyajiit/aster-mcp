/**
 * The Android companion, described as a thing you download rather than as the
 * far end of the server's WebSocket.
 *
 * Home talked about Aster as "an MCP server plus an app" throughout and never
 * said the app is independently useful: the only Releases link on the whole
 * route was in the footer, and nothing on it said the phone can serve MCP by
 * itself. A reader who wanted just the app had no path.
 *
 * Every number here is derived from a gated fact — FACTS.appVersion and
 * FACTS.androidMinApi are both re-checked against build.gradle.kts by
 * scripts/verify-facts.ts, and TOOL_COUNTS is asserted to close arithmetically.
 * Nothing in this file is hand-counted.
 */
import { FACTS, ON_DEVICE_PORT, TOOL_COUNTS } from './site'

export const ANDROID_APP_LEDE =
  `The companion is a normal Android app, and it does not need the server to be useful. Sideload it and the phone can serve MCP on its own: the app embeds a Streamable-HTTP MCP server and exposes ${TOOL_COUNTS.onDevice} on-device actions to any client that can reach it. The Node server is what adds multi-device management, the approval gate and the ${TOOL_COUNTS.mcpServer} prefixed ${'`aster_*`'} tools — it is not what makes the phone controllable.`

export interface AppCapability {
  title: string
  detail: string
  icon: string
}

/** What you get from the APK alone, with no Node server anywhere. */
export const ANDROID_APP_STANDALONE: AppCapability[] = [
  {
    title: 'Serves MCP from the phone',
    detail: `An embedded Ktor server speaks Streamable-HTTP MCP on port ${ON_DEVICE_PORT} by default. Point a client on your LAN at the handset and it sees ${TOOL_COUNTS.onDevice} unprefixed actions — no desktop, no Node, no account.`,
    icon: 'lucide:server',
  },
  {
    title: 'Answers apps on the same device',
    detail: 'An agent already running on the phone binds the service directly over Android Binder IPC. No network hop at all, so it keeps working with the radio off.',
    icon: 'lucide:smartphone',
  },
  {
    title: 'Runs its own console',
    detail: 'The app carries a device dashboard, a guided pass that requests every permission in one go, a live log of each tool call with its arguments and result, and a panel for the on-device server.',
    icon: 'lucide:monitor-smartphone',
  },
]

/** The honest small print on a sideload. Each line is a fact, not a hedge. */
export const ANDROID_APP_FACTS: { term: string; def: string }[] = [
  { term: 'Version', def: `${FACTS.appVersion}, the current GitHub release` },
  { term: 'Requires', def: `${FACTS.androidMinLabel}. Root is not required and is not used.` },
  { term: 'Distribution', def: 'A signed APK on GitHub Releases. It is not on the Google Play Store, so Android will ask you to allow installs from your browser or file manager the first time.' },
  { term: 'Licence', def: `${FACTS.license}, same as the server. The Android source is in the same repository.` },
]

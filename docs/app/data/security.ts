/**
 * The /security route's content, and the source for its markdown twin
 * (scripts/generate-machine-readable.ts → twinSecurity()).
 *
 * Every claim below is checked against a file in this repo and cited inline.
 * Three claims the previous copy made did NOT survive that check and have been
 * rewritten rather than softened:
 *
 *  1. "File access outside public storage is limited to directories you've
 *     explicitly shared." FALSE on the MCP path.
 *     apps/android/.../handlers/HostDirHandler.kt:53 serves only the actions
 *     `files.read` / `files.list` — the OpenAlly host-dir IPC feature. The MCP
 *     file tools land in FileSystemHandler.kt, whose resolvePath() (line 198)
 *     returns any string starting with `/` unchanged, and the manifest holds
 *     MANAGE_EXTERNAL_STORAGE. mcp/src/mcp/tools.ts:411-458 declares
 *     aster_list_files / aster_read_file / aster_write_file / aster_delete_file
 *     with `{ deviceId, path }` and no scoping parameter.
 *
 *  2. "Every new device must be manually approved before connecting." FALSE.
 *     mcp/src/websocket/index.ts:378-434 registers an unknown device with
 *     status `pending`, keeps the socket open and replies
 *     `{ type: 'auth_result', success: true, status: 'pending' }`. The gate is
 *     at command dispatch — index.ts:127 throws "Device … is not approved".
 *
 *  3. "Your data never leaves your network." Unscoped. True of Aster itself;
 *     not a statement Aster can make about the AI client's own model provider.
 *
 * The encryption caveat is deliberate and load-bearing:
 * mcp/src/websocket/index.ts:163 is `new WebSocketServer({ port })` — plain ws,
 * no TLS termination — and apps/android/.../res/xml/network_security_config.xml
 * permits cleartext so that LAN IP literals can be reached at all.
 */

// Relative, not the `~` alias: this module is also imported by
// scripts/generate-machine-readable.ts, which runs outside Nuxt's resolver.
import { ENDPOINTS, FACTS, PATHS, TOOL_PREFIX } from './site'

export interface SecurityPillar {
  title: string
  description: string
  /** Iconify name (lucide set), rendered by <Icon>. */
  icon: string
  /** Accent key. SecuritySection.vue maps it to literal Tailwind classes. */
  accent: string
  /**
   * Whether this claim is a protection or an admitted limit.
   *
   * The list deliberately mixes both, and /security renders them in one run so
   * the limits cannot be skipped. The home summary needs to tell them apart to
   * label them honestly, and deriving that from array position would silently
   * mislabel every pillar the moment someone reorders the list.
   */
  kind: 'protection' | 'limit'
}

export interface PermissionRow {
  /** The constant as it appears in AndroidManifest.xml, without the package prefix. */
  name: string
  /** One line. Written for someone deciding whether to install the app. */
  why: string
  /**
   * True when Aster cannot run without it, so it is requested up front.
   * False means the grant is per-feature or a separate Settings toggle, and
   * declining it costs you only the tools that need it.
   */
  required: boolean
}

/**
 * Seven claims, each either a genuine protection or a genuine limit. The two
 * limits sit in the same list as the protections on purpose: a security page
 * that only lists wins is not a security page.
 */
export const SECURITY_PILLARS: SecurityPillar[] = [
  {
    title: 'No root, no ADB exploit',
    description:
      'Aster drives the phone through the official Android Accessibility Service API — the same system that powers TalkBack and every screen reader. There is no rooting step, no unlocked bootloader, no shell exploit and no ADB persistence. The service is a toggle in Android Settings that you turn on yourself and can turn off at any time, and Android shows its own warning screen before it will let you enable it.',
    icon: 'lucide:shield-check',
    accent: 'green',
    kind: 'protection',
  },
  {
    title: 'Self-hosted, no account, no telemetry',
    description:
      'The server is an npm package you run on your own machine. There is no sign-up, no licence check, no vendor relay and no analytics call: device records, approvals and logs live in a local SQLite file on that machine. What Aster does not send, nobody can leak. There is exactly one outbound call Aster can make, and it is off until you turn it on: the optional event-forwarding webhook (`aster set-event-forwarding`). Know what it carries before you enable it — six event kinds, to the URL you supply and to nothing else: notification text, SMS sender and body, incoming-call number and resolved contact name, and device connected / disconnected / pairing-required status. Incoming calls in particular default to ON once forwarding is enabled; switch them off with `events.incomingCalls: false` in `~/.aster/event-forwarding.json`. Note the boundary carefully — this is a statement about Aster, not about your AI client. Whatever you type into Claude, OpenClaw or any other MCP client, and whatever the phone returns to it, is sent to that client’s own model provider under that provider’s terms. Aster neither sees nor governs that hop.',
    icon: 'lucide:server',
    accent: 'aster',
    kind: 'protection',
  },
  {
    title: 'An unapproved device connects, but cannot be commanded',
    description:
      'Approval is a gate on commands, not on the socket. A new phone that reaches the server is registered with status “pending”, keeps its WebSocket open and is told the handshake succeeded — it can be listed and named in the dashboard, which is how you recognise it well enough to approve it. What it cannot do is act: every command dispatch checks the device status first and refuses a device that is not approved. Nothing runs on the phone until you press approve.',
    icon: 'lucide:fingerprint',
    accent: 'amber',
    kind: 'protection',
  },
  {
    title: 'A kill switch you can always reach',
    description:
      'While the AI is driving the screen, Aster posts a persistent high-importance notification reading “AI is controlling your phone” with a STOP action. One tap severs the control session and aborts the agent loop within a single action. The notification is deliberately the primary control rather than the on-screen overlay, because the overlay depends on the draw-over-other-apps permission and the notification does not.',
    icon: 'lucide:octagon-x',
    accent: 'rose',
    kind: 'protection',
  },
  {
    title: 'Banking and payment apps are refused by default',
    description:
      'A companion-side guard blocks screen-control actions — tap, type, scroll, gestures, key presses and app launches — whenever the foreground app matches its bundled banking and payments denylist (bank, upi, wallet, payment, phonepe, paytm, paypal, venmo, cashapp, revolut, coinbase, binance and similar). It is fail-closed: if the live foreground package cannot be read at all, a gated action is refused rather than allowed. The guard ships with that denylist already loaded, so it protects a phone that has never synced a policy. Screen reads, device info and Aster’s own prompts stay ungated, because blinding the agent is not the same as protecting you.',
    icon: 'lucide:shield-ban',
    accent: 'red',
    kind: 'protection',
  },
  {
    title: 'The file tools are not path-scoped — know this before you approve',
    description:
      `This is the sharpest edge in Aster, and the previous version of this page described it wrongly. On an approved device the MCP file tools reach any path the app itself can read. ${TOOL_PREFIX}list_files, ${TOOL_PREFIX}read_file, ${TOOL_PREFIX}write_file and ${TOOL_PREFIX}delete_file each take only a device id and a path; there is no folder parameter to narrow, and the app holds MANAGE_EXTERNAL_STORAGE, so an absolute path is used as given. Owner-approved folders are real, but they scope a different surface: the on-device IPC host-directory feature, which serves its own files.read and files.list actions to a local agent such as OpenAlly. Approving a device for MCP is therefore a grant over that device’s storage, not over one folder in it. Approve devices you own, on networks you trust.`,
    icon: 'lucide:folder-open',
    accent: 'orange',
    kind: 'limit',
  },
  {
    title: 'The device link is plain ws:// by default',
    description:
      'The phone-to-server WebSocket is unencrypted. The server opens a plain ws:// listener and terminates no TLS of its own, and the Android app ships a network-security config that permits cleartext, because Android cannot pin a certificate to a bare LAN IP literal. On a home or office network you control, that is a considered trade rather than an oversight. Off it, it is not acceptable: put the phone and the server on a Tailscale tailnet, which carries the same traffic inside an encrypted WireGuard tunnel, needs no port forwarding, and exposes nothing to the public internet. Aster detects the Tailscale CLI and reports the tailnet address for you to use.',
    icon: 'lucide:unlock',
    accent: 'sky',
    kind: 'limit',
  },
]

/**
 * Every `uses-permission` in apps/android/app/src/main/AndroidManifest.xml, in
 * manifest order, plus the two special-access service grants that are declared
 * as `android:permission` on a <service> rather than as a uses-permission —
 * BIND_ACCESSIBILITY_SERVICE and BIND_NOTIFICATION_LISTENER_SERVICE. Those two
 * are the largest grants in the app and omitting them from a permissions table
 * would be the most misleading thing on this page.
 *
 * `required: true` is reserved for the plumbing Aster cannot start without.
 * Everything else is a per-feature grant you can decline.
 */
export const PERMISSIONS: PermissionRow[] = [
  {
    name: 'BIND_ACCESSIBILITY_SERVICE',
    why: 'The accessibility service that reads the screen hierarchy and performs taps, text entry and gestures. This is how screen control works at all. It is a separate toggle in Android Settings, off until you enable it, and Android shows its own warning first.',
    required: false,
  },
  {
    name: 'BIND_NOTIFICATION_LISTENER_SERVICE',
    why: 'Reads incoming notifications so the AI can answer “what did I miss” and react to alerts. Also a separate Settings toggle; without it the notification tools return nothing.',
    required: false,
  },
  {
    name: 'INTERNET',
    why: 'Opens the WebSocket to your server and, in on-device MCP mode, serves the local HTTP endpoint. Aster contacts no address you have not configured.',
    required: true,
  },
  {
    name: 'FOREGROUND_SERVICE',
    why: 'Keeps the companion running with a visible, permanent notification instead of silently in the background. The notification is the point: you can always see that Aster is up.',
    required: true,
  },
  {
    name: 'FOREGROUND_SERVICE_SPECIAL_USE',
    why: 'The Android 14+ subtype declaration for that service, registered as device_control. Android requires the category to be stated in the manifest.',
    required: true,
  },
  {
    name: 'RECEIVE_BOOT_COMPLETED',
    why: 'Restarts the companion after a reboot so a phone left on a charger reconnects without you unlocking it. Decline it and you simply reopen the app yourself.',
    required: false,
  },
  {
    name: 'WAKE_LOCK',
    why: 'Holds the CPU awake long enough to finish a command that arrives while the screen is off, so a request does not die halfway through.',
    required: true,
  },
  {
    name: 'ACCESS_WIFI_STATE',
    why: 'Reads the current network so the app can show which Wi-Fi it is on and reach the server by LAN address.',
    required: true,
  },
  {
    name: 'ACCESS_NETWORK_STATE',
    why: 'Detects when connectivity drops so the WebSocket reconnects instead of hanging.',
    required: true,
  },
  {
    name: 'REQUEST_IGNORE_BATTERY_OPTIMIZATIONS',
    why: 'Lets you opt the companion out of Doze, so an idle phone stays reachable. It only opens the system dialog — you decide, and the answer is reversible in Settings.',
    required: false,
  },
  {
    name: 'POST_NOTIFICATIONS',
    why: 'Shows the foreground-service notification and the screen-control STOP notification, and backs the aster_post_notification tool, so a remote caller can also post a notification of its own. Refusing it removes the kill switch from your notification shade, so grant this one.',
    required: true,
  },
  {
    name: 'READ_SMS',
    why: 'Backs the read-messages tools, so you can ask what a code was or what someone texted. Nothing is uploaded anywhere: a message is read only when a tool call asks for it, and the reply goes to the AI client you connected.',
    required: false,
  },
  {
    name: 'SEND_SMS',
    why: 'Sends a text on your instruction — the “tell her I am running late” case. Every send is an explicit tool call.',
    required: false,
  },
  {
    name: 'RECEIVE_SMS',
    why: 'Surfaces an incoming message as an event, so an agent can react to a one-time code or an alert without polling.',
    required: false,
  },
  {
    name: 'CALL_PHONE',
    why: 'Places a call, including the call-and-speak-a-message flow. Declining it leaves every other tool working.',
    required: false,
  },
  {
    name: 'READ_PHONE_STATE',
    why: 'Tells Aster whether a call is ringing, active or ended, so a spoken message is not played into a dead line.',
    required: false,
  },
  {
    name: 'READ_CALL_LOG',
    why: 'Supplies the caller number on incoming-call events for API 28 to 30 only. Android 12 and newer never deliver the number to an app this way, so on a current phone this grant buys nothing and can be declined.',
    required: false,
  },
  {
    name: 'MODIFY_AUDIO_SETTINGS',
    why: 'Routes audio to the earpiece or speaker for the call-and-speak flow, and restores your setting afterwards.',
    required: false,
  },
  {
    name: 'ACCESS_FINE_LOCATION',
    why: 'Answers “where is my phone” and location-aware requests with a precise fix. Location is read on a tool call and returned to your client; Aster stores no location history.',
    required: false,
  },
  {
    name: 'ACCESS_COARSE_LOCATION',
    why: 'The approximate fallback when you grant only coarse location, or when a precise fix is unavailable.',
    required: false,
  },
  {
    name: 'MANAGE_EXTERNAL_STORAGE',
    why: 'All-files access. This is the permission behind the file tools, and the reason those tools are not path-scoped: with it granted, an absolute path is read or written as given. It is a special-access grant you make in Settings, and it is the single grant to think hardest about.',
    required: false,
  },
  {
    name: 'READ_EXTERNAL_STORAGE',
    why: 'The legacy read path, capped at Android 12L and below. On newer versions the all-files grant supersedes it.',
    required: false,
  },
  {
    name: 'WRITE_EXTERNAL_STORAGE',
    why: 'The legacy write path, capped at Android 10 and below. Ignored entirely on anything newer.',
    required: false,
  },
  {
    name: 'QUERY_ALL_PACKAGES',
    why: 'Lists installed apps so the AI can open the right one by name, and so the banking denylist can identify the foreground package it must refuse to drive.',
    required: false,
  },
  {
    name: 'PACKAGE_USAGE_STATS',
    why: 'Adds a last-used time to the installed-apps list, for “which apps have I not opened in months”. Declaring it is not granting it: you must enable Usage access in Settings, and until you do, last_used comes back empty.',
    required: false,
  },
  {
    name: 'READ_CONTACTS',
    why: 'Resolves a name to a number, so “call Priya” works without you reciting digits.',
    required: false,
  },
  {
    name: 'WRITE_CONTACTS',
    why: 'Required by exactly one tool, and it is the destructive one: aster_delete_contacts, which permanently removes contacts from the address book. There is no save or edit path — ContactHandler.kt dispatches only search_contacts, list_contacts_full and delete_contacts, and checks this permission solely inside the delete. Decline it and the contact tools become read-only.',
    required: false,
  },
  {
    name: 'CAMERA',
    why: 'Captures a photo when a tool call asks for one. There is no background or continuous capture: the camera runs for that single request and stops.',
    required: false,
  },
  {
    name: 'FOREGROUND_SERVICE_CAMERA',
    why: 'The Android 14+ subtype that must be declared for the service to use the camera at all. Camera hardware is marked optional, so Aster installs on a phone without one.',
    required: false,
  },
  {
    name: 'SET_ALARM',
    why: 'Hands an alarm or timer to your existing clock app. Aster does not replace it or run a clock of its own.',
    required: false,
  },
  {
    name: 'VIBRATE',
    why: 'Haptic feedback in the app’s own interface, and the aster_vibrate tool — so a remote caller with an approved device can buzz the phone with a custom pattern (MediaHandler.kt dispatches the `vibrate` action).',
    required: false,
  },
  {
    name: 'SYSTEM_ALERT_WINDOW',
    why: 'Draws the interactive approval overlay and the companion face above other apps. The face itself is decorative — a bare tap on it opens a normal, tap-jacking-protected activity and performs nothing privileged. Without the grant, Aster falls back to a full activity for the same prompts.',
    required: false,
  },
]


// --------------------------------------------------------------- FAQs ------

/**
 * The Q&A rendered on /security AND emitted as FAQPage JSON-LD there.
 *
 * These lived as a page-local const in app/pages/security.vue, which meant the
 * generator could not see them: security.md and llms-full.txt shipped without
 * the seven answers the search engines were being handed. Data that a twin must
 * carry belongs in app/data, not in the page.
 */
export interface SecurityFaq {
  q: string
  a: string
}

export const SECURITY_FAQS: SecurityFaq[] = [
  {
    q: 'Does Aster need root access?',
    a: `No. Aster drives the phone through the official Android Accessibility Service API — the same system that powers TalkBack and every other screen reader — so there is no rooting step, no unlocked bootloader, no ADB exploit and no shell persistence. It runs on ${FACTS.androidMinLabel}. Enabling the service is a toggle in Android Settings that you turn on yourself, behind Android's own warning screen, and can turn off at any time.`,
  },
  {
    q: 'Is the connection between the phone and the server encrypted?',
    a: 'Not by default. The phone-to-server WebSocket is plain ws:// and the server terminates no TLS of its own; the Android app ships a network-security config that permits cleartext because Android cannot pin a certificate to a bare LAN IP address. On a home or office network you control, that is a considered trade. Off it, it is not acceptable — join the phone and the server to a Tailscale tailnet so the same traffic rides an encrypted WireGuard tunnel, with no port forwarding and nothing exposed to the public internet.',
  },
  {
    q: 'Can Aster read or control my banking apps?',
    a: 'Driving them is refused by default. A companion-side guard blocks screen-control actions — tap, type, scroll, gestures, key presses and app launches — whenever the foreground app matches its bundled banking and payments denylist, and it is fail-closed: if the live foreground package cannot be read at all, a gated action is refused rather than allowed. The guard ships with that denylist loaded, so it protects a phone that has never synced a policy. Screen reads are deliberately not gated by it, because blinding the agent would also blind the check that decides whether acting is safe; the guard stops the agent acting, not looking.',
  },
  {
    q: 'Which folders can the file tools reach?',
    a: `Any path the app itself can read, on a device you have approved. ${TOOL_PREFIX}list_files, ${TOOL_PREFIX}read_file, ${TOOL_PREFIX}write_file and ${TOOL_PREFIX}delete_file each take only a device id and a path — there is no folder parameter to narrow — and the app holds the all-files MANAGE_EXTERNAL_STORAGE grant, so an absolute path is used as given. Owner-approved folders are real but scope a different surface: the on-device IPC host-directory feature used by a local agent such as OpenAlly. Withholding all-files access is what bounds this, and it costs more than the four file tools — see the next answer.`,
  },
  {
    q: 'What exactly stops working if I decline all-files access?',
    a: `Eight tools, not four. MANAGE_EXTERNAL_STORAGE is a separate Settings grant, and declining it disables the four file tools — ${TOOL_PREFIX}list_files, ${TOOL_PREFIX}read_file, ${TOOL_PREFIX}write_file, ${TOOL_PREFIX}delete_file — and also the four storage and media tools that walk the same external storage: ${TOOL_PREFIX}analyze_storage, ${TOOL_PREFIX}find_large_files, ${TOOL_PREFIX}index_media_metadata and ${TOOL_PREFIX}search_media. So "find what is eating my storage" and "find my beach photos from December" stop working too. Messages, calls, contacts, notifications, alarms, audio, camera capture and screen control are unaffected.`,
  },
  {
    q: 'Does a new phone get access as soon as it connects?',
    a: 'No. Approval gates commands, not the socket. An unknown phone that reaches the server is registered with status "pending", keeps its WebSocket open and is told the handshake succeeded, so you can see and name it in the dashboard well enough to recognise it. It cannot act: every command dispatch checks the device status first and refuses anything that is not approved. Nothing runs on the phone until you press approve.',
  },
  {
    q: 'What happens if I lose the phone?',
    a: 'Reject the device in the dashboard. That flips its stored status, pushes a rejection to the phone and closes its WebSocket, and any later reconnection is refused at command dispatch — so the AI can no longer act on it even if the handset is powered on and online. Because the link only works across your own LAN or tailnet, a phone off that network cannot be commanded in the first place. Aster holds no cloud account to compromise; the device records live in a local SQLite file on your own machine.',
  },
  {
    q: 'Does Aster phone home?',
    a: `No, with one opt-in exception you turn on yourself. There is no sign-up, no licence check, no analytics call and no vendor relay: the server is a ${FACTS.license}-licensed npm package you run yourself, and device records, approvals and logs stay in a local SQLite file on that machine. The one outbound call Aster can make is the optional event-forwarding webhook — off until you run \`aster set-event-forwarding\`, and then it POSTs only to the URL you supplied. It carries six event kinds, and the list is worth reading before you switch it on: notification text, SMS sender and body, the number and resolved contact name of an incoming call, and device connected / disconnected / pairing-required status. Incoming-call forwarding is on by default once forwarding is enabled — set \`events.incomingCalls: false\` in \`${PATHS.eventForwarding}\` to stop it. Scope the rest of the claim correctly, too: it is about Aster, not about your AI client. Whatever you type into Claude, OpenClaw or any other MCP client, and whatever the phone returns to it, still goes to that client's own model provider under that provider's terms. Aster neither sees nor governs that hop.`,
  },
  {
    q: 'Where is my Aster data stored, and how do I delete it?',
    a: `Two places, both on the machine running the server, and both plain files you can delete. The database — devices, approvals, tool-call logs — is SQLite at \`${PATHS.db}\`, resolved relative to the directory you ran \`aster start\` from, or wherever \`${PATHS.dbEnv}\` points. Runtime state lives in \`${PATHS.stateDir}\`: \`${PATHS.status}\`, \`${PATHS.pid}\` and, if you enabled forwarding, \`${PATHS.eventForwarding}\`. Stop the server, delete both, and nothing of Aster's is left. There is no cloud copy to request.`,
  },
]

// ---------------------------------------------------------- hardening ------

/**
 * The hardening checklist rendered on /security. Same reason as SECURITY_FAQS:
 * it used to be a component-local const in SecuritySection.vue and never
 * reached the markdown twin.
 */
export interface HardeningStep {
  title: string
  body: string
  code?: string
  codeLabel?: string
}

export const HARDENING_STEPS: HardeningStep[] = [
  {
    title: 'Approve only devices you own',
    body: 'Approval is the whole trust boundary. An approved device can be commanded, and on the file tools that means any path the app can read. A pending device is harmless — it can connect and be named, but every command dispatch refuses it. Check the name and model in the dashboard before you press approve, and revoke anything you do not recognise.',
  },
  {
    title: 'Encrypt the link with Tailscale before leaving your LAN',
    body: 'The phone-to-server WebSocket is plain and unencrypted, and the server terminates no TLS of its own. On a network you control that is a considered trade. Anywhere else it is not: join both machines to a tailnet and point the phone at the tailnet address, so the same traffic rides an encrypted WireGuard tunnel with no port forwarding and no public exposure. Aster detects the Tailscale CLI and reports the address to use.',
    code: `tailscale status\n# then point the phone at the tailnet host instead of ${ENDPOINTS.deviceWs}`,
    codeLabel: 'On the server',
  },
  {
    title: 'Withhold all-files access unless you want the file and storage tools',
    body: `MANAGE_EXTERNAL_STORAGE is a separate Settings grant, and it is the one that makes the file tools unbounded. Withholding it is the real scoping control — but be clear about the price: it disables eight tools, not four. The four file tools stop reading, and so do ${TOOL_PREFIX}analyze_storage, ${TOOL_PREFIX}find_large_files, ${TOOL_PREFIX}index_media_metadata and ${TOOL_PREFIX}search_media, which walk the same external storage. Messages, calls, contacts, notifications, alarms, audio, camera capture and screen control are unaffected.`,
  },
  {
    title: 'Keep notifications enabled so the kill switch stays reachable',
    body: 'The STOP control for screen control lives in a persistent notification, deliberately, because it does not depend on the draw-over-other-apps permission the way the on-screen overlay does. Silencing Aster’s notifications removes your fastest way to stop a session mid-action.',
  },
  {
    title: 'Leave the banking denylist alone, and audit any allow-override',
    body: 'The companion refuses to drive a foreground app matching its bundled banking and payments denylist, and refuses outright when it cannot read the foreground package at all. Owner overrides replace that policy wholesale rather than merging into it, so an override list is the exact thing to re-read after any change.',
  },
  {
    title: 'Leave event forwarding off unless you want it, and check where it points',
    body: `Forwarding is the only way Aster makes an outbound call, and it is off until you enable it. Once on, it POSTs six kinds of event to whatever URL is recorded in \`${PATHS.eventForwarding}\`: notification text, SMS sender AND body, the number and resolved contact name of an incoming call, and device connected / disconnected / pairing-required status. Note the asymmetry in that file — every other event kind is opt-in, but incoming calls forward unless \`events.incomingCalls\` is explicitly \`false\`. Read the file before trusting a phone that has forwarding enabled, and prefer a destination on your own network.`,
  },
]

/**
 * The eight capability cards on the home route.
 *
 * DELIBERATELY EIGHT. The grid used to carry sixteen, two of which restated
 * /security word for word ("Safety Rails" and "Owner-Approved Folders"); those
 * now live only on /security, which is where someone asking "is this safe?"
 * lands. Everything else that was cut — shell execution, audio and haptics,
 * location and battery, the three connection modes, the permissions flow — is
 * covered in full on /tools and /architecture.
 *
 * Two rows here are capabilities the site never mentioned at all: App
 * Automations and the Companion Face overlay.
 *
 * The App Automations row previously described a record-and-replay macro
 * recorder, which does not exist. There is no record or replay action in
 * mcp/src/mcp/tools.ts, none in the on-device handler map, and nothing in
 * apps/android that captures a flow. What does exist is the human-in-the-loop
 * half: InteractiveOverlayHandler.kt serves `screen_prompt` / `screen_approve`
 * (a blocking on-screen choice), SignInWaitHandler.kt serves
 * `screen_signin_wait` / `screen_handoff` (park the run at a login wall or a
 * payment), CapabilityHandler.kt serves `screen_capability` (preflight) and
 * PolicyHandler.kt serves `screen_set_policy`. The replay engine that drives
 * them lives in OpenAlly's kernel, not in Aster. All six are on-device only —
 * they are in the 77-action catalogue and NOT among the 49 MCP tools, so the
 * card must not imply an MCP client can call them.
 *
 * Tool names printed in a description MUST carry the aster_ prefix — that is
 * the name an MCP client actually calls (mcp/src/mcp/tools.ts). Build them from
 * TOOL_PREFIX so a rename cannot leave a stale, uncallable name on the page.
 */

import { TOOL_PREFIX } from './site'

export interface Feature {
  id: string
  title: string
  description: string
  icon: string
  accent: string
}

const t = (name: string) => `${TOOL_PREFIX}${name}`

export const FEATURES: Feature[] = [
  {
    id: 'screen-control',
    title: 'Screen control',
    description: `Read the live UI tree with ${t('get_screen_hierarchy')}, then tap, swipe, type and navigate. ${t('take_screenshot')} sends back what the phone is showing right now.`,
    icon: 'lucide:monitor-smartphone',
    accent: 'from-aster to-cyan-400',
  },
  {
    id: 'app-automations',
    title: 'App automations, with a human in the loop',
    description:
      'When an automation run hits something only you should do, Aster puts it on screen and waits: a choice, an approve-or-deny card, a sign-in wall, a payment hand-off. These are on-device actions over Binder IPC — the runner is OpenAlly, not an MCP client.',
    icon: 'lucide:clapperboard',
    accent: 'from-cyan-400 to-sky-400',
  },
  {
    id: 'media-intelligence',
    title: 'Media intelligence',
    description: `${t('index_media_metadata')} builds a local index of the camera roll; ${t('search_media')} queries it by date, place and content without uploading a single photo.`,
    icon: 'lucide:images',
    accent: 'from-violet-400 to-purple-400',
  },
  {
    id: 'files-storage',
    title: 'Files and storage',
    description: `List, read, write and delete any path the app can reach — the file tools are not folder-scoped, and withholding all-files access is what bounds them (see /security). ${t('analyze_storage')} and ${t('find_large_files')} explain where the free space went.`,
    icon: 'lucide:folder-search',
    accent: 'from-amber-400 to-orange-400',
  },
  {
    id: 'calls-sms-voice',
    title: 'Calls, SMS and voice',
    description: `${t('send_sms')} and ${t('make_call')} do the obvious thing. ${t('make_call_with_voice')} dials a number, turns on the speaker and speaks a message through text to speech.`,
    icon: 'lucide:phone-call',
    accent: 'from-sky-400 to-blue-400',
  },
  {
    id: 'proactive-events',
    title: 'Notifications and events',
    description: `${t('read_notifications')} reads the shade on demand. Event forwarding pushes new SMS, notifications, calls and device-status changes to a webhook, so the assistant acts without being asked.`,
    icon: 'lucide:radio',
    accent: 'from-fuchsia-400 to-pink-400',
  },
  {
    id: 'camera-video',
    title: 'Camera and video',
    description: `${t('take_photo')} and ${t('record_video')} run either camera on request or on an event. A charging spare phone becomes a pet cam, a doorbell or a parcel watch.`,
    icon: 'lucide:camera',
    accent: 'from-orange-400 to-red-400',
  },
  {
    id: 'companion-face',
    title: 'Companion face',
    description:
      'An animated on-screen companion that lip-syncs to text-to-speech and reacts to whatever is playing, driven by an on-device event classifier that never sends audio anywhere. Like App automations, this is driven on-device by OpenAlly over Binder IPC — the companion_overlay_* verbs are not among the 49 aster_* tools.',
    icon: 'lucide:smile',
    accent: 'from-pink-400 to-rose-400',
  },
]

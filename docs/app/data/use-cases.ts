/**
 * The use-case catalogue — one row per thing somebody asks their assistant to do.
 *
 * Read by app/components/UseCasesSection.vue and by
 * scripts/generate-machine-readable.ts (the /use-cases.md twin and llms-full.txt).
 *
 * RULES THIS FILE ENFORCES
 *
 * 1. `tools` holds UNPREFIXED tool names, and every one of them MUST exist in
 *    mcp/src/mcp/tools.ts. Verify with:
 *      grep -oE "name: 'aster_[a-z_]+'" mcp/src/mcp/tools.ts | sort
 *    The previous inline version of this list shipped three names that are not
 *    tools at all — `event_forwarding`, `sms_event` and `notification_event`.
 *    They are event kinds the companion pushes, not callable tools; an MCP
 *    client asked for `aster_sms_event` gets "unknown tool". They are gone.
 *    Renderers print `${TOOL_PREFIX}${name}`, which is the callable name.
 *
 * 2. Aster DOES NOT SCHEDULE. It exposes tools and pushes events; the AI client
 *    decides when to act. Any response text that implies a recurring job is
 *    attributed to the assistant, never to Aster.
 *
 * 3. NO AUDIO CAPTURE ANYWHERE. apps/android/app/src/main/AndroidManifest.xml
 *    declares no RECORD_AUDIO permission, the app contains no AudioRecord or
 *    MediaRecorder audio path, and none of the 49 MCP tools captures or analyses
 *    sound. The old "baby monitor listens for crying" card claimed a capability
 *    that does not exist and has been rewritten around a scheduled photo check.
 *    `aster_record_video` records video from the camera; it is not a listening
 *    device and is never described as one.
 *
 * 4. `response` is ILLUSTRATIVE. Counts, file sizes, place names and flight
 *    numbers are written to read well, not measured. Every surface that prints
 *    these strings says so in visible copy, so that an answer engine does not
 *    quote "47 duplicate sets" as a product statistic.
 */

// Relative, not `~/data/site`: this module is loaded both through Vite and
// through the jiti-loaded nitro hook, and a relative specifier is the one form
// both resolvers agree on. Same rule as ./faq and ./setup.
import { FACTS, TOOL_COUNTS, TOOL_PREFIX } from './site'

export interface UseCase {
  /** Stable slug. Used as the list key and as the ItemList element id. */
  id: string
  /** What the person says, in their words. */
  prompt: string
  /** Illustrative reply. Not recorded output — see rule 4. */
  response: string
  /** Unprefixed tool names that exist in mcp/src/mcp/tools.ts. See rule 1. */
  tools: string[]
  /** Must match one of USE_CASE_CATEGORIES. */
  category: string
  /** Iconify name for the reply glyph. */
  icon: string
  /** Tailwind colour family the renderer maps to a class set. */
  accent: string
}

export const USE_CASES: UseCase[] = [
  // ── Photos & media ────────────────────────────────────────────────────────
  {
    id: 'duplicate-photos',
    prompt: 'Find the duplicate photos on my phone and show me which ones to delete',
    response:
      'Indexed the media library and compared metadata. There are 47 duplicate sets across 2,104 photos. I can show you each set side by side before anything is removed.',
    tools: ['search_media', 'index_media_metadata'],
    category: 'Photos & media',
    icon: 'lucide:copy',
    accent: 'violet',
  },
  {
    id: 'photos-by-place',
    prompt: 'Show me the photos I took in Goa last December',
    response:
      '83 photos from December carry a location near Goa, including 12 beach shots and 6 sunset panoramas. Want them pulled onto this machine?',
    tools: ['search_media'],
    category: 'Photos & media',
    icon: 'lucide:map-pin',
    accent: 'violet',
  },
  {
    id: 'blurry-screenshots',
    prompt: 'Find the screenshots I took this week and bin the blurry ones',
    response:
      '23 screenshots from the last seven days. Five look blurry or unreadable. Say the word and they go; the rest stay untouched.',
    tools: ['search_media', 'delete_file'],
    category: 'Photos & media',
    icon: 'lucide:trash-2',
    accent: 'violet',
  },

  // ── Reminders & alerts ────────────────────────────────────────────────────
  {
    id: 'standup-alarm',
    prompt: 'Set an alarm for 10 AM tomorrow for the standup',
    response:
      'Alarm set on the device for 10:00, labelled Standup. I also posted a notification so it is visible on the lock screen tonight.',
    tools: ['set_alarm', 'post_notification'],
    category: 'Reminders & alerts',
    icon: 'lucide:alarm-clock',
    accent: 'amber',
  },
  {
    id: 'find-my-phone',
    prompt: "Buzz my phone — I can't find it under the couch",
    response:
      'Turning the ringer up, playing a tone and vibrating in a strong three-pulse pattern. Follow the sound; I will keep it going until you tell me to stop.',
    tools: ['set_volume', 'play_audio', 'vibrate', 'stop_audio'],
    category: 'Reminders & alerts',
    icon: 'lucide:vibrate',
    accent: 'amber',
  },
  {
    id: 'triage-notifications',
    prompt: 'Read my unread notifications and tell me if anything is urgent',
    response:
      '14 waiting. Two from Slack flagged urgent, one missed call from Mum, and the rest are promotions. Want the two Slack threads summarised?',
    tools: ['read_notifications'],
    category: 'Reminders & alerts',
    icon: 'lucide:bell-ring',
    accent: 'amber',
  },

  // ── Files & cleanup ───────────────────────────────────────────────────────
  {
    id: 'pull-a-file',
    prompt: 'Copy meeting-notes.pdf off my phone and onto this machine',
    response:
      'Found meeting-notes.pdf in Downloads, 2.3 MB, modified on Tuesday. Reading it off the device now so it lands in your working folder.',
    tools: ['list_files', 'read_file'],
    category: 'Files & cleanup',
    icon: 'lucide:send',
    accent: 'teal',
  },
  {
    id: 'storage-hogs',
    prompt: "I'm out of storage — find what is eating the space",
    response:
      'WhatsApp media 8.2 GB, cached data 3.1 GB, leftover installers 1.4 GB. The installers are the easy win. Shall I start there?',
    tools: ['analyze_storage', 'find_large_files'],
    category: 'Files & cleanup',
    icon: 'lucide:hard-drive',
    accent: 'teal',
  },
  {
    id: 'old-downloads',
    prompt: 'Delete the downloaded PDFs older than six months',
    response:
      '34 PDFs in Downloads have not been opened in six months, 890 MB between them. Here is the list — confirm and I will remove exactly those.',
    tools: ['list_files', 'delete_file'],
    category: 'Files & cleanup',
    icon: 'lucide:trash-2',
    accent: 'teal',
  },

  // ── Communication ─────────────────────────────────────────────────────────
  {
    id: 'call-and-speak',
    prompt: "Call Mum and tell her I'll be twenty minutes late",
    response:
      'Dialling now with the speaker on. When she picks up I will say your message out loud, then end the call and tell you how it went.',
    tools: ['search_contacts', 'make_call_with_voice'],
    category: 'Communication',
    icon: 'lucide:phone-call',
    accent: 'blue',
  },
  {
    id: 'read-and-reply-sms',
    prompt: 'Read the last text from the delivery driver and reply that I am home',
    response:
      'Last message: "Package arriving between 2 and 4 PM." Reply sent: "Thanks, I will be home." Nothing else in the thread needs an answer.',
    tools: ['read_sms', 'send_sms'],
    category: 'Communication',
    icon: 'lucide:message-square',
    accent: 'blue',
  },
  {
    id: 'screen-notice',
    prompt: 'Put "Do not disturb, in a meeting" on my phone screen',
    response:
      'The notice is drawn over whatever is on screen and stays until you dismiss it. I dropped the ringer volume to match.',
    tools: ['show_overlay', 'set_volume'],
    category: 'Communication',
    icon: 'lucide:monitor',
    accent: 'blue',
  },

  // ── Daily automations ─────────────────────────────────────────────────────
  {
    id: 'whats-on-screen',
    prompt: 'What is on my phone screen right now?',
    response:
      'A video app is in the foreground, playing a lo-fi mix. Battery is at 62% and Wi-Fi is connected. Screenshot attached if you want to see it.',
    tools: ['take_screenshot', 'get_screen_hierarchy', 'get_battery'],
    category: 'Daily automations',
    icon: 'lucide:scan',
    accent: 'rose',
  },
  {
    id: 'battery-check',
    prompt: 'What is my battery at? Should I charge before heading out?',
    response:
      '34% and not charging. At the rate it is draining that is roughly two hours. Worth a top-up before you leave.',
    tools: ['get_battery', 'get_device_info'],
    category: 'Daily automations',
    icon: 'lucide:battery-low',
    accent: 'rose',
  },
  {
    id: 'drive-an-app',
    prompt: 'Open Maps and search for the nearest coffee shop',
    response:
      'Launched Maps, tapped the search field, typed "coffee shop near me" and submitted. Three results within a ten-minute walk.',
    tools: ['launch_intent', 'click_by_text', 'input_text', 'get_screen_hierarchy'],
    category: 'Daily automations',
    icon: 'lucide:coffee',
    accent: 'rose',
  },

  // ── Proactive monitoring ──────────────────────────────────────────────────
  {
    id: 'pet-check-in',
    prompt: 'Dog is home alone — check on him every half hour and send me the photo',
    response:
      'I am holding the half-hour schedule on my side. Each time it comes round I ask the phone for a camera shot and text it to you. Aster itself runs nothing on a timer.',
    tools: ['take_photo', 'send_sms'],
    category: 'Proactive monitoring',
    icon: 'lucide:camera',
    accent: 'fuchsia',
  },
  {
    id: 'driving-autoreply',
    prompt: 'While I am driving, tell anyone who texts that I will call back',
    response:
      'I will keep checking the inbox while drive mode is on. Alex asked "Are you free?" — answered with "Driving right now, will call you back in twenty minutes."',
    tools: ['read_sms', 'send_sms'],
    category: 'Proactive monitoring',
    icon: 'lucide:message-circle',
    accent: 'fuchsia',
  },
  {
    id: 'announce-delivery',
    prompt: 'When the delivery app says the rider is near, announce it out loud',
    response:
      'Spotted the notification — "Your rider is 1 minute away" — turned the media volume up and had the phone say it on the speaker.',
    tools: ['read_notifications', 'set_volume', 'speak_tts'],
    category: 'Proactive monitoring',
    icon: 'lucide:volume-2',
    accent: 'fuchsia',
  },

  // ── AI-owned phone ────────────────────────────────────────────────────────
  {
    id: 'flight-delay-call',
    prompt: 'Ring me if my flight is delayed, and tell me the new time',
    response:
      'I am watching the airline app on my own phone and checking in on it periodically. AI-302 has slipped 45 minutes, so I am calling your number to say so.',
    tools: ['read_notifications', 'make_call_with_voice'],
    category: 'AI-owned phone',
    icon: 'lucide:phone-outgoing',
    accent: 'orange',
  },
  {
    id: 'doorbell-clip',
    prompt: 'If the doorbell app goes off, record five seconds and send it to me',
    response:
      'Doorbell notification picked up on the spare phone. Recorded a five-second clip from the camera and texted it to you.',
    tools: ['read_notifications', 'record_video', 'send_sms'],
    category: 'AI-owned phone',
    icon: 'lucide:video',
    accent: 'orange',
  },
  {
    id: 'nursery-photo-check',
    prompt: 'Leave the spare phone in the nursery and text me a photo check every twenty minutes',
    response:
      'I hold the twenty-minute schedule. Each round I ask for a still from the camera and text it over. Aster has no microphone permission and no audio-capture tool, so nothing in the room is being listened to.',
    tools: ['take_photo', 'send_sms'],
    category: 'AI-owned phone',
    icon: 'lucide:baby',
    accent: 'orange',
  },
]

/**
 * The route's citable paragraph, and its hedge. Both are exported because both
 * belong to the twin as much as to the page.
 *
 * /use-cases.md used to open on a generic sentence and carry a hedge that named
 * a transcript the twin does not contain, while every "Result:" line below it
 * printed hard invented numbers. That is the worst possible split: the figures
 * travel and the warning does not. The lede is the one self-contained,
 * non-disclaimed passage on the route — what Aster is, in a form an answer
 * engine can quote — and the disclaimer is what stops "47 duplicate sets across
 * 2,104 photos" being quoted as a measurement.
 *
 * Backticks are deliberate: markdown consumes them as code, and the Vue
 * renderers split on them the way app/pages/index.vue splits QUICK_FACTS.
 */
export const USE_CASES_LEDE = `Aster is an open-source Model Context Protocol server plus an Android companion app: it gives an AI client such as Claude Code, OpenClaw or MoltBot ${TOOL_COUNTS.mcpServer} tools, all named \`${TOOL_PREFIX}*\`, for reading and driving a phone on ${FACTS.androidMinLabel} — no root, no cloud account. The ${USE_CASES.length} prompts below are real things people ask it for, each one listing the exact tools that run.`

/**
 * The hedge over the invented figures, in parts, because /use-cases renders two
 * sections that each carry invented figures — LiveChatSection's worked
 * conversation and UseCasesSection's grid — and they are a screenful apart.
 *
 * Both need the caveat adjacent to the numbers: an answer engine lifting the
 * grid as a passage does not get a sentence sitting 2,000px above it in a
 * different section. But printing the identical paragraph twice on one page is
 * duplicate content and reads as a stutter.
 *
 * So the parts are separate and composed: LiveChatSection prints the whole
 * thing, the grid prints REPLIES alone (the clause that actually hedges the
 * figures in the cards), and the twin emits the whole thing once. One source,
 * so the two cannot drift the way they already did once — the twin gained
 * "Tool names are exact." while the hand-written page copy did not.
 */
export const USE_CASES_DISCLAIMER_PARTS = {
  prompts: 'The prompts are real.',
  replies:
    'The replies are illustrative — counts, file sizes, place names and flight numbers are written as examples, not measured, and are not product statistics.',
  tools: 'Tool names are exact.',
} as const

export const USE_CASES_DISCLAIMER = Object.values(USE_CASES_DISCLAIMER_PARTS).join(' ')

/**
 * Categories in first-appearance order. Derived, never hand-maintained, so a
 * new row cannot introduce a category the filter does not know about.
 */
export const USE_CASE_CATEGORIES: string[] = USE_CASES.reduce<string[]>((acc, u) => {
  if (!acc.includes(u.category)) acc.push(u.category)
  return acc
}, [])

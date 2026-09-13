/**
 * Proactive event forwarding, and the scenarios the "AI's own phone" route is
 * built around.
 *
 * Read by app/components/ProactiveSection.vue, app/pages/ai-phone.vue and
 * scripts/generate-machine-readable.ts (the /ai-phone.md twin and llms-full).
 * The generator prints tool names as `${TOOL_PREFIX}${t}`, so every entry in a
 * scenario's `tools` array is stored UNPREFIXED and rendered prefixed.
 *
 * Provenance — check these before editing a string:
 *   - the event set and the on/off defaults: mcp/src/event-forwarding/index.ts
 *     (`AgentEventForwardingConfig.events`, `buildEventText`, `forwardAgentEvent`)
 *   - the delivery channels: mcp/src/event-forwarding/channels/{openclaw,mattermost}.ts
 *   - the CLI command: mcp/bin/aster.ts:408 → `.command('set-event-forwarding')`
 *   - every tool name: mcp/src/mcp/tools.ts
 *
 * TWO claims used to be on this page and were both false:
 *   1. "Schedule triggers every 30 minutes" / "Periodic check every 15 minutes".
 *      Aster ships NO scheduler. It exposes tools and pushes events; the cadence
 *      is the AI client's (a cron job, a scheduled agent run, a reminder the
 *      assistant holds). Every recurring scenario below says so.
 *   2. `make_call_with_voice` being "hands-free". Its own description in
 *      mcp/src/mcp/tools.ts:648 says the audio is acoustic coupling — the device
 *      loudspeaker into the call microphone — not routed into the call audio,
 *      best in a quiet room, and device-dependent. Any scenario where the AI
 *      phones you has to carry that caveat.
 */

export interface ProactiveEvent {
  /** The term, as it reads in the <dl> on the page and in the markdown twin. */
  event: string
  detail: string
  /** Iconify name. Re-declared as a literal in the .vue that renders it — the
   *  @nuxt/icon clientBundle scanner only globs .vue/.md, never .ts. */
  icon: string
  /** A design-token CSS variable. Used for icon tiles, borders and fills only —
   *  never as a text colour, because these tones measure under 4.5:1 on the
   *  raised surface. */
  accent: string
}

export interface AiPhoneScenario {
  id: string
  title: string
  description: string
  /** UNPREFIXED tool names from mcp/src/mcp/tools.ts. Printed with TOOL_PREFIX. */
  tools: string[]
  icon: string
  accent: string
}

/**
 * What the phone pushes. Off by default for every event: forwarding only runs
 * once `config.enabled` is true, which `aster set-event-forwarding` sets.
 */
export const PROACTIVE_EVENTS: ProactiveEvent[] = [
  {
    event: 'Incoming SMS',
    detail:
      'Tagged [event] sms, carrying the sender and the full message body, the moment the phone receives it. The agent answers from the push instead of polling an inbox.',
    icon: 'lucide:message-square',
    accent: 'var(--color-mode-remote)',
  },
  {
    event: 'App notifications',
    detail:
      'Tagged [event] notification, carrying the app name, its package id, and the notification title and text. This is the one that catches flight delays, delivery updates and the ride that just pulled up — anything an app already tells you about.',
    icon: 'lucide:bell',
    accent: 'var(--color-info)',
  },
  {
    event: 'Incoming call',
    detail:
      'Tagged [event] incoming_call, carrying the number and the contact name when the phone can resolve one, while it is still ringing. This is the one event that is on unless you explicitly switch it off: a missing events.incomingCalls key does not drop RINGING events, only an explicit off does.',
    icon: 'lucide:phone-incoming',
    accent: 'var(--color-warning)',
  },
  {
    event: 'Device online and offline',
    detail:
      'Tagged [event] device_online or [event] device_offline when the companion\'s WebSocket connects or drops. An agent that has to know whether the phone is reachable before it queues work reads these.',
    icon: 'lucide:wifi',
    accent: 'var(--color-success)',
  },
  {
    event: 'New pairing request',
    detail:
      'Tagged [event] pairing with the status pending_approval when an unknown device asks to join. The payload names the next step — approve it from the dashboard, or with aster devices approve — so nobody quietly joins your server.',
    icon: 'lucide:user-round-plus',
    accent: 'var(--color-primary)',
  },
]

/**
 * Six things people actually build on a phone the AI owns. Each names the exact
 * tools the agent calls, and each recurring one states whose clock it runs on.
 */
export const AI_PHONE_SCENARIOS: AiPhoneScenario[] = [
  {
    id: 'doorbell-clip',
    title: 'Record a clip when someone is at the door',
    description:
      'Leave the phone facing the entrance. When your doorbell or intercom app posts a notification, that event reaches the agent, which records a few seconds of video and texts it to you. The trigger is the notification the other app already posts, not motion detection — if that app stays silent, so does this.',
    tools: ['record_video', 'send_sms'],
    icon: 'lucide:video',
    accent: 'var(--color-error)',
  },
  {
    id: 'flight-change',
    title: 'Be called when your flight changes',
    description:
      'The airline app posts a gate change or a delay, the agent reads the forwarded notification and phones you to say it out loud — useful precisely when you are not looking at a screen. Understand how that audio works: Aster speaks through the loudspeaker and the call microphone picks it up, so it is acoustic coupling, not audio routed into the call stream. Keep the phone in a quiet room, expect quality to vary by device, and treat it as a nudge that gets through rather than a hands-free conversation.',
    tools: ['read_notifications', 'make_call_with_voice'],
    icon: 'lucide:plane',
    accent: 'var(--color-info)',
  },
  {
    id: 'ride-arrival',
    title: 'Hear your ride arrive from the next room',
    description:
      'The ride-hailing app says the driver is two minutes out. The agent gets the notification, speaks the line aloud on the spare phone and texts you the driver and plate so you are not reading a map on the pavement.',
    tools: ['read_notifications', 'speak_tts', 'send_sms'],
    icon: 'lucide:car',
    accent: 'var(--color-mode-remote)',
  },
  {
    id: 'pet-check',
    title: 'Look in on a pet while you are out',
    description:
      'Prop the phone where the dog usually sleeps and have the agent take a photo and send it to you. Aster runs no scheduler, so the cadence is your AI client\'s: a cron job, a scheduled agent run, or a standing instruction in the assistant itself. Aster\'s side of it is one camera call and one message per run.',
    tools: ['take_photo', 'send_sms'],
    icon: 'lucide:camera',
    accent: 'var(--color-warning)',
  },
  {
    id: 'nursery-check',
    title: 'Check the nursery without opening the door',
    description:
      'The same shape as the pet check, and quieter than walking in. A photo goes out on whatever rhythm your agent keeps — again, that clock belongs to the AI client, not to Aster. Point the phone at the cot, not at the room, and remember the photo travels wherever you told the agent to send it.',
    tools: ['take_photo', 'send_sms'],
    icon: 'lucide:moon',
    accent: 'var(--color-primary)',
  },
  {
    id: 'busy-reply',
    title: 'Answer for you while you are driving or in a meeting',
    description:
      'An SMS lands, the agent reads it, decides whether it can be answered without you, and replies with something true — that you are driving and will call back — rather than a canned auto-reply. You set the rules for what it may answer and what it must hold for you.',
    tools: ['read_sms', 'send_sms'],
    icon: 'lucide:message-circle',
    accent: 'var(--color-success)',
  },
]

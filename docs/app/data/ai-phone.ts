/**
 * The /ai-phone route's content: the definition, the four requirements, the
 * three changes, the acoustic-coupling limit and the five-step install.
 *
 * All of it used to be page-local and component-local `const`s — NEEDS and
 * CHANGES inside app/components/AiPhoneHero.vue, SETUP_STEPS inside
 * app/pages/ai-phone.vue — which made it invisible to
 * scripts/generate-machine-readable.ts. /ai-phone.md shipped at 48% of the
 * rendered page: no requirements, no "what changes", and no install procedure
 * at all, while the same page emitted a HowTo JSON-LD node built from those
 * five steps. The structured data and the plain-text twin therefore disagreed
 * about whether the route contains a procedure.
 *
 * Read by app/components/AiPhoneHero.vue, app/pages/ai-phone.vue (visible <ol>
 * AND the HowTo steps) and scripts/generate-machine-readable.ts. The generator
 * reads `title`, `detail`, `name`, `text`, `code` and `codeLabel` by name — do
 * not rename a field without changing it there too; verify-facts.ts fails the
 * build on the literal "undefined" that a rename would emit.
 *
 * Relative './site', not '~/data/site': this module is imported twice, by Vue
 * components through Vite (where `~` resolves) and by the generator through the
 * jiti-loaded nitro hook (where it may not). Same rule as ./faq and ./setup.
 */

import { ENDPOINTS, FACTS, LINKS, PORTS } from './site'

export interface AiPhoneCard {
  title: string
  /** Iconify name. Also listed in AiPhoneHero.vue's scanner comment — see below. */
  icon: string
  /** A design token, never a raw hex. */
  accent: string
  detail: string
}

export interface AiPhoneSetupStep {
  /** Imperative, and the HowToStep `name`. */
  name: string
  /** One or two sentences. Rendered as text, and as the HowToStep `text`. */
  text: string
  /** Shell or config to run verbatim, or null when the step is manual. */
  code: string | null
  codeLabel: string | null
  /**
   * Fence language for the markdown twin. Defaults to bash there, which is
   * right for four of the five steps and wrong for the one whose `code` is the
   * .mcp.json document — a JSON object fenced as bash reads as something to
   * paste into a terminal. Same field, same meaning, as SetupStepDef.lang.
   */
  lang?: string
  link: { href: string; label: string } | null
}

/**
 * The lede, split the way the page renders it. `definition` is the entity
 * definition for the route — the sentence an answer engine quotes when asked
 * what an AI's own phone is — so it leads both the page and the twin.
 */
export const AI_PHONE_LEDE = {
  definition:
    'An AI’s own phone is a spare Android that belongs to your assistant rather than to you: it runs the Aster companion app, keeps its own SIM and number, and sits on a charger so the agent can see what arrives on it and act without being asked first.',
  contrast:
    'It is the same Aster, pointed the other way round. In copilot mode you drive and the AI works your phone for you. Here the phone is the AI’s, the events come to it, and the thing you notice is your own phone ringing — because your assistant decided the gate change was worth a call.',
} as const

export const AI_PHONE_NEEDS_INTRO =
  'Four things, none of them bought for the purpose. The drawer phone most people already own is the whole hardware budget.'

export const AI_PHONE_NEEDS: AiPhoneCard[] = [
  {
    title: 'A spare Android',
    icon: 'lucide:smartphone',
    accent: 'var(--color-primary)',
    detail: `Anything running ${FACTS.androidMinLabel}. No root, no custom ROM, no unlocked bootloader — the companion app is sideloaded and asks only for the permissions the tools it exposes actually use.`,
  },
  {
    title: 'A charger it never leaves',
    icon: 'lucide:plug',
    accent: 'var(--color-warning)',
    detail:
      'A phone the AI owns is a phone that is always awake. Leave it plugged in on Wi-Fi, screen off, and let Android keep the companion running in the foreground service it already uses.',
  },
  {
    title: 'The Aster server, somewhere always on',
    icon: 'lucide:server',
    accent: 'var(--color-mode-remote)',
    detail: `The npm package ${FACTS.npmPackage} on Node ${FACTS.nodeRequirement} — a laptop that stays open, a mini PC, a home server. The phone holds a WebSocket to it on port ${PORTS[0].port}; your AI client speaks MCP to the same machine at ${ENDPOINTS.mcp}.`,
  },
  {
    title: 'A SIM, if you want it to call and text',
    icon: 'lucide:phone-call',
    accent: 'var(--color-info)',
    detail:
      'Photos, notifications and screen work need no SIM at all. Giving the phone its own number is what lets your assistant call you, text you, and answer for you from an identity that is not yours.',
  },
]

export const AI_PHONE_CHANGES_INTRO =
  'A phone the AI holds stops being a thing it operates on request and becomes a thing it watches.'

export const AI_PHONE_CHANGES: AiPhoneCard[] = [
  {
    title: 'It sees what arrives',
    icon: 'lucide:bell-ring',
    accent: 'var(--color-info)',
    detail:
      'Texts, app notifications, a ringing call, the phone dropping offline. With event forwarding on, those reach your agent as they happen instead of waiting for you to ask what is new.',
  },
  {
    title: 'It can act first',
    icon: 'lucide:hand',
    accent: 'var(--color-success)',
    detail:
      'An event that matters becomes a photo taken, a clip recorded, a reply sent, a call placed to you. Which events matter is your agent’s rule, not a setting buried in Aster.',
  },
  {
    title: 'It uses a number that is not yours',
    icon: 'lucide:phone-call',
    accent: 'var(--color-warning)',
    detail:
      'Deliveries, verification codes and the noise you would rather not receive can live on the assistant’s line. Your own phone hears from it only when the assistant decides you should.',
  },
]

/**
 * The one capability caveat on the route, kept as data rather than as template
 * prose because it is the thing a reader most needs carried into the twin:
 * make_call_with_voice works by acoustic coupling, and a reader who plans for a
 * hands-free conversation has planned for the wrong product.
 */
export const AI_PHONE_LIMIT = {
  title: 'One honest limit.',
  body: 'When the AI phones you, it speaks through the device loudspeaker and the call microphone picks that up — acoustic coupling, not audio routed into the call stream. It works, and it works best with the phone in a quiet room; quality depends on the handset. Plan for a call that reliably gets your attention, not a hands-free conversation.',
} as const

export const AI_PHONE_SETUP_INTRO =
  'Five steps, start to finish. The first four are the ordinary Aster install — the fifth is the one that turns a phone your AI can use into a phone your AI watches.'

/**
 * ONE array, rendered three times: the visible <ol> on /ai-phone, the HowTo
 * steps in that page's JSON-LD, and the "Setting one up" section of
 * /ai-phone.md. A HowTo whose steps do not match the visible procedure is a
 * structured-data violation, and a twin that omits the procedure entirely is
 * how the two came to disagree. Commands are from README.md "Quick Start"
 * (§253) and mcp/bin/aster.ts.
 */
export const AI_PHONE_SETUP_STEPS: AiPhoneSetupStep[] = [
  {
    name: 'Install the server on a machine that stays on',
    text: `Aster's server is an npm package. Put it on whatever is already awake at 3am — a laptop that stays open, a mini PC, a home server — not on the phone. It needs Node ${FACTS.nodeRequirement}.`,
    code: `npm install -g ${FACTS.npmPackage}\naster start`,
    codeLabel: 'On the always-on machine',
    link: null,
  },
  {
    name: 'Sideload the companion app on the spare phone',
    text: `Grab the APK from the releases page and install it on the phone you are dedicating. Anything on ${FACTS.androidMinLabel} works; no root, no custom ROM. Grant the permissions for the things you actually want it to do — camera, SMS and notification access are each optional.`,
    code: null,
    codeLabel: null,
    link: { href: LINKS.releases, label: 'Download the companion APK' },
  },
  {
    name: 'Point the phone at the server and approve it',
    text: `Open the app and enter the server address the terminal printed — the device WebSocket listens on port ${PORTS[0].port}. The device arrives as pending and stays inert until you approve it, from the dashboard at ${ENDPOINTS.dashboard} or with the CLI.`,
    code: 'aster devices list\naster devices approve <deviceId>',
    codeLabel: 'Approve the device',
    link: null,
  },
  {
    name: 'Leave it on the charger and connect your AI client',
    text: `Plug the phone in, screen off, and leave it. Then point Claude Code, Claude Desktop, OpenClaw, MoltBot, ClawdBot or any MCP client at the server's Streamable-HTTP endpoint. That is the copilot half working — your AI can now use the phone.`,
    code: `{\n  "mcpServers": {\n    "aster": {\n      "type": "http",\n      "url": "${ENDPOINTS.mcp}"\n    }\n  }\n}`,
    codeLabel: 'MCP client config',
    lang: 'json',
    link: null,
  },
  {
    name: 'Turn on event forwarding',
    text: "This is the step that makes it the AI's phone rather than a phone it borrows. Choose an agent webhook with a Bearer token, or a Mattermost incoming webhook, pick which events to forward, and the phone starts pushing them the moment they happen.",
    code: 'aster set-event-forwarding',
    codeLabel: 'Enable proactive events',
    link: null,
  },
]

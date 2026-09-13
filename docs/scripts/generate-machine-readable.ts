import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { ROUTES, href } from '../app/data/routes'
import { SITE, LINKS, FACTS, PORTS, ENDPOINTS, PATHS, TOOL_COUNTS, TOOL_PREFIX, CONNECTION_MODES, QUICK_FACTS, DISAMBIGUATION } from '../app/data/site'
import { TOOL_CATEGORIES, ALL_TOOLS } from '../app/data/tools'
import { ON_DEVICE_CATEGORIES } from '../app/data/on-device'
import { FEATURES } from '../app/data/features'
import { USE_CASES, USE_CASES_LEDE, USE_CASES_DISCLAIMER } from '../app/data/use-cases'
import { PROACTIVE_EVENTS, AI_PHONE_SCENARIOS } from '../app/data/proactive'
import {
  AI_PHONE_LEDE,
  AI_PHONE_NEEDS,
  AI_PHONE_NEEDS_INTRO,
  AI_PHONE_CHANGES,
  AI_PHONE_CHANGES_INTRO,
  AI_PHONE_LIMIT,
  AI_PHONE_SETUP_INTRO,
  AI_PHONE_SETUP_STEPS,
} from '../app/data/ai-phone'
import { SECURITY_PILLARS, PERMISSIONS, SECURITY_FAQS, HARDENING_STEPS } from '../app/data/security'
import {
  SETUP_STEPS,
  CLIENTS,
  REQUIREMENTS,
  REQUIREMENTS_NOTE,
  VERIFY_INTRO,
  VERIFY_COMMANDS,
  VERIFY_CHECKS,
} from '../app/data/setup'
import { COMMAND_TRACE, TRANSPORTS, WORKED_EXAMPLES } from '../app/data/architecture'
import { FAQS, FAQ_TOPICS, FAQ_LEDE, COMPARISON, COMPARISON_INTRO, DIAGNOSTIC_COMMANDS } from '../app/data/faq'

/**
 * Emits every machine-readable artifact the site publishes: sitemap.xml,
 * robots.txt, llms.txt, llms-full.txt and one markdown twin per route.
 *
 * All of it is DERIVED from app/data/*.ts, so it cannot drift from the pages.
 * The previous sitemap.xml and llms.txt were hand-written in public/ and had
 * already drifted (1 URL; an "Android 7.0+" requirement the APK contradicts;
 * 49 tool names with the aster_ prefix stripped, none of them callable).
 *
 * Called from a nitro:build:public-assets hook in nuxt.config.ts — NOT from an
 * npm script. CI runs `pnpm nuxt generate` directly, which never fires a
 * package.json pre/post hook, so a generator chained there would run on a dev
 * machine and silently never run in production.
 *
 * A twin must carry EVERY section its route renders. The recurring failure mode
 * is not a wrong string, it is a missing import: content that lives in a
 * page-local or component-local const is invisible here, so the twin ships a
 * silently shortened page while the HTML looks complete. That is how
 * security.md lost the FAQ and the hardening checklist, architecture.md lost
 * the five worked traces, and index.md lost the capability grid. When you add a
 * section to a page, put its data in app/data and render it here too.
 *
 * A later audit measured every twin against its rendered page and found the
 * same failure again, at scale: ai-phone.md was at 48% of the rendered body
 * (no requirements, no "what changes", and no install procedure — while the
 * page emitted a HowTo node built from exactly those five steps), setup.md at
 * 68%, index.md at 79%. Those sections now live in app/data/ai-phone.ts and in
 * new exports on app/data/setup.ts, app/data/use-cases.ts and app/data/faq.ts.
 * What remains hand-copied here is named and fenced below with the component it
 * came from; there is no third category.
 */

const BUILD_DATE = (process.env.SOURCE_DATE_EPOCH
  ? new Date(Number(process.env.SOURCE_DATE_EPOCH) * 1000)
  : new Date()
).toISOString().slice(0, 10)

/**
 * Absolute, non-redirecting URL. Routes get a trailing slash because that is
 * what GitHub Pages serves; `/tools.md` and `/llms.txt` are files and are left
 * alone. Same rule as abs() in app/composables/useRouteSeo.ts — both delegate
 * to href() so the sitemap and the canonicals cannot disagree.
 */
function absUrl(path: string): string {
  return SITE.origin + href(path)
}

// ---------------------------------------------------------------- sitemap ---

function sitemap(): string {
  const urls = ROUTES.map(
    (r) => `  <url>
    <loc>${absUrl(r.path)}</loc>
    <lastmod>${BUILD_DATE}</lastmod>
    <priority>${r.priority.toFixed(1)}</priority>
  </url>`,
  ).join('\n')
  // changefreq is omitted deliberately: Google ignores it, and a stale weekly
  // hint on a page that has not changed in months is worse than no hint.
  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls}
</urlset>
`
}

// ----------------------------------------------------------------- robots ---

function robots(): string {
  // A wildcard Allow already permits every documented AI crawler; the explicit
  // groups below add nothing mechanically. They are here as a readable, auditable
  // statement of intent, so that narrowing access later is a one-line edit in a
  // named group rather than a rewrite. No crawler is disallowed.
  const agents = [
    'GPTBot', 'OAI-SearchBot', 'ChatGPT-User',
    'ClaudeBot', 'Claude-User', 'Claude-SearchBot', 'anthropic-ai',
    'PerplexityBot', 'Perplexity-User',
    'Google-Extended', 'Googlebot', 'Bingbot', 'Applebot', 'Applebot-Extended',
    'Amazonbot', 'meta-externalagent', 'cohere-ai', 'YouBot', 'CCBot', 'Diffbot', 'Timpibot', 'omgili',
  ]
  return `# https://aster.matterwardlabs.com
# Aster is open source and self-hosted. Everything here is public; nothing is
# disallowed. See /llms.txt for a machine-readable summary and /llms-full.txt
# for the full text.

User-agent: *
Allow: /

${agents.map((a) => `User-agent: ${a}\nAllow: /`).join('\n\n')}

Sitemap: ${SITE.origin}/sitemap.xml
`
}

// --------------------------------------------------------------- llms.txt ---

/**
 * The llms.txt index: H1, one blockquote, then flat H2 sections.
 *
 * Shape follows llmstxt.org — an index that points at the full text, not a copy
 * of it. This file used to inline every server tool name under eight H3
 * subsections, which duplicated /tools.md, buried the sections that exist only
 * here, and nested deeper than the format allows. The tool list now lives in
 * the twin and this file links to it.
 *
 * llms.txt is not a ranking signal and no crawler is required to read it. It is
 * here because a fetched index is cheaper for an assistant than eight page
 * fetches, which is also why it must stay short.
 *
 * DELIBERATE DEVIATION, so the next reviewer does not re-open it: llmstxt.org
 * describes H2 sections as lists of links, and five of the nine below — Install,
 * Key facts, Connection modes, Ports, Links — are prose bullets instead. They
 * stay that way. The alternative the spec allows is free prose BEFORE the first
 * H2, which would put the install command, the tool-namespace rule and the port
 * table into one unheaded slab that no fetcher can address or excerpt. Headed
 * bullets are worse spec compliance and better retrieval, and llms.txt is a
 * convenience index, not a ranking factor, so retrieval wins. Pages, Tools and
 * Optional are link lists, which is the part of the shape that carries weight.
 */
function llms(): string {
  return `# Aster

> ${SITE.name} connects any Android device to AI assistants over the Model Context Protocol (MCP). Use it as your AI copilot on mobile, or dedicate a spare Android to your AI and let it call, text, watch notifications and act on its own. ${TOOL_COUNTS.mcpServer} MCP tools, open source (${FACTS.license}), self-hosted: no account, no telemetry, no vendor relay.

${DISAMBIGUATION}

Aster is two pieces: a self-hosted Node.js MCP server (npm \`${FACTS.npmPackage}\`) and an Android companion app (no root, ${FACTS.androidMinLabel}). The phone holds a WebSocket to the server; AI clients speak MCP over HTTP to the server. The app can also run standalone via an on-device MCP server (Ktor) or Android Binder IPC for same-device agents. Built by ${SITE.publisher}. OpenAlly is a separate app, not a parent platform: it runs on the same phone and drives Aster over Binder IPC, which makes it a consumer of Aster like any other MCP client.

IMPORTANT — tool namespace: every tool the MCP server registers carries the \`${TOOL_PREFIX}\` prefix. The callable name is \`${TOOL_PREFIX}take_screenshot\`, not \`take_screenshot\`. Tool sets differ per connection mode; see "Tools" below.

## Pages

${ROUTES.map((r) => `- [${r.title}](${absUrl(r.path)}): ${r.description} Plain text: ${absUrl(r.twin)}`).join('\n')}

## Install

- Server: \`npm install -g ${FACTS.npmPackage}\`, then \`aster start\`
- Android app: install the APK from [GitHub Releases](${LINKS.releases}), open it, enter the server address (\`${ENDPOINTS.deviceWs}\`), then approve the device in the dashboard
- MCP clients (Claude Code, Claude Desktop, AnythingLLM, any MCP client): Streamable-HTTP endpoint at \`${ENDPOINTS.mcp}\`
- OpenClaw / MoltBot / ClawdBot: \`clawhub install aster\` — [skill on ClawHub](${LINKS.clawhub})

## Tools

Tool counts are surface-specific. Do not state a single number without naming the surface. Both catalogues, with a one-line description each, are in the /tools twin.

- [MCP server — ${TOOL_COUNTS.mcpServer} tools, every name \`${TOOL_PREFIX}\`-prefixed](${absUrl('/tools.md')}): what an MCP client over HTTP sees, in ${TOOL_CATEGORIES.length} categories.
- [On-device catalogue — ${TOOL_COUNTS.onDevice} unprefixed actions](${absUrl('/tools.md')}): what the on-device Ktor MCP server and Binder IPC expose, in ${ON_DEVICE_CATEGORIES.length} categories.
- The two overlap without either containing the other: ${TOOL_COUNTS.shared} names appear in both; ${TOOL_COUNTS.onDeviceOnly} on-device actions have no \`${TOOL_PREFIX}\` tool (screen observation, the companion overlay, the screen_* control verbs, the host-directory files.* pair); exactly ${TOOL_COUNTS.serverOnly} server tool has no on-device action behind it, \`${TOOL_PREFIX}list_devices\`, because brokering between phones is meaningless on the phone itself.
- \`${TOOL_PREFIX}click_by_id\` is a rename, not an extra capability: it dispatches the device action \`click_by_view_id\`.

## Key facts

${QUICK_FACTS.map((f) => `- **${f.term}**: ${f.def}`).join('\n')}

## Connection modes

${CONNECTION_MODES.map((m) => `- **${m.name}** (${m.badge}) — ${m.summary} Tools: ${m.toolNamespace}`).join('\n')}

## Ports

${PORTS.map((p) => `- \`${p.port}\` — ${p.name}. ${p.detail}`).join('\n')}

## Links

- Website: ${SITE.origin}
- GitHub: ${LINKS.repo}
- npm: ${LINKS.npm}
- Releases (APK): ${LINKS.releases}
- ClawHub skill: ${LINKS.clawhub}
- OpenAlly: ${LINKS.openally}
- Model Context Protocol: ${LINKS.mcpSpec}

## Optional

- Full text of every page in one file: ${SITE.origin}/llms-full.txt
- Sitemap: ${SITE.origin}/sitemap.xml
- Python runtime manifest (used by the Android app): ${SITE.origin}/runtimes/python/index.json
- Whisper model manifest (used by the Android app): ${SITE.origin}/runtimes/whisper/index.json
`
}

// -------------------------------------------------------- markdown twins ---

/**
 * Every twin opens with the same four lines, DISAMBIGUATION among them.
 *
 * A twin is fetched on its own: an assistant that pulls /tools.md or
 * /security.md sees no other page on this site, and "Aster" collides with Aster
 * DM Healthcare, Aster Data Systems and the ASTER instrument on NASA's Terra
 * satellite. The sentence existed in exactly one file in the whole artifact —
 * index.html — so seven of the eight routes shipped with no entity anchor at
 * all. One line per twin is cheap; being cited as a hospital group is not.
 *
 * twinIndex() repeats it under "What Aster is" ON PURPOSE, and llms() carries
 * it in the intro. A retriever chunks on H2 boundaries, so a header chunk and a
 * "what is this" chunk are different chunks and each needs the anchor. Do not
 * dedupe them.
 */
function frontMatter(path: string): string {
  const r = ROUTES.find((x) => x.path === path)!
  return `# ${r.title}

> ${r.description}

${DISAMBIGUATION}

Source: ${absUrl(r.path)} · Part of ${SITE.name} (${LINKS.repo}) · Last generated ${BUILD_DATE}
`
}

/**
 * Home-route prose that still lives as template literals in components this
 * lane does not own: app/components/HeroSection.vue (CONVERSATIONS), the
 * dashboard sentence in app/components/ScreenshotsSection.vue, and the closing
 * CTA in app/pages/index.vue. index.md was at 79% of the rendered body without
 * them, and the dashboard sentence in particular is the ONLY place the twin
 * layer could learn what port 5989 is for.
 *
 * Copied verbatim from those three files. This is the same stopgap shape as
 * AGENT_WEBHOOK_DEFAULT below and carries the same obligation: if you change
 * the copy in the component, change it here. When one of those files is next
 * opened, move its strings into app/data and delete the copy here.
 */
const INDEX_HERO_PROMPTS = [
  { prompt: 'Find duplicate photos on my phone and free up space', tools: ['index_media_metadata', 'search_media'] },
  { prompt: 'Read my notifications — anything urgent?', tools: ['read_notifications'] },
  { prompt: 'My storage is full. What is eating all the space?', tools: ['analyze_storage', 'find_large_files'] },
  { prompt: 'Open Maps and find the nearest coffee shop', tools: ['launch_intent', 'input_text', 'click_by_text'] },
  { prompt: 'If my flight is delayed, call me and tell me the new time', tools: ['make_call_with_voice'], viaEvent: true },
]

const INDEX_DASHBOARD_SENTENCE = `Aster ships an Android companion app and a web dashboard at ${ENDPOINTS.dashboard} for device approval, file browsing, live screen control and MCP tool testing. Both ship in a dark and a light theme.`

const INDEX_INSTALL_CTA = `Three commands and a sideload. Install ${FACTS.npmPackage} from npm on any machine running Node ${FACTS.nodeRequirement}, start the server, install the companion app on ${FACTS.androidMinLabel}, approve the device, and point your MCP client at ${ENDPOINTS.mcp}. ${FACTS.license} licensed, self-hosted, no account and no telemetry. Root is not required.`

/** The Today / With-Aster contrast rows from app/components/EmbraceSection.vue. */
const INDEX_CONTRAST = {
  before: {
    heading: 'Today — your AI can talk',
    rows: [
      { label: 'Chats on WhatsApp and Telegram', detail: 'It reads and answers messages for you' },
      { label: 'Drafts email and documents', detail: 'It writes on your behalf' },
      { label: 'Schedules and organises', detail: 'It keeps track of your time' },
      { label: 'Writes and reviews code', detail: 'It builds software alongside you' },
    ],
  },
  after: {
    heading: 'With Aster — your AI can act',
    rows: [
      { label: 'Taps, swipes and types on the phone', detail: 'Full screen control through the accessibility service' },
      { label: 'Takes photos and records video', detail: 'Either camera, on request or when an event fires' },
      { label: 'Reacts to events as they happen', detail: 'New SMS, notifications and device status, pushed to your agent' },
      { label: 'Reads notifications, sends SMS, places calls', detail: 'Including a call that speaks a message out loud' },
    ],
  },
}

function twinIndex(): string {
  return `${frontMatter('/')}
## What Aster is

Aster is an MCP server plus an Android companion app. It gives an AI assistant — Claude, OpenClaw, MoltBot, ClawdBot, or any MCP client — the ability to see and control an Android phone: take screenshots, read the UI tree, tap and type, read notifications and SMS, send SMS, place calls, take photos, search media, and manage files.

${DISAMBIGUATION}

Two ways people run it:

1. **Copilot on your own phone.** Your assistant acts on the device you already carry.
2. **A phone of its own.** Plug a spare Android into a charger. The AI watches it, calls you, texts you, and acts without being asked.

## What people ask it

Five example prompts and the tools each one runs. The replies these produce are illustrative; the tool names are exact.

${INDEX_HERO_PROMPTS.map((p) => `- "${p.prompt}" — ${p.viaEvent ? 'event forwarding, then ' : ''}${p.tools.map((t) => `\`${TOOL_PREFIX}${t}\``).join(' → ')}`).join('\n')}

## What changes when your AI can act

### ${INDEX_CONTRAST.before.heading}

${INDEX_CONTRAST.before.rows.map((r) => `- **${r.label}** — ${r.detail}`).join('\n')}

"Set an alarm for 6 AM" — and then you pick up the phone and do it yourself.

### ${INDEX_CONTRAST.after.heading}

${INDEX_CONTRAST.after.rows.map((r) => `- **${r.label}** — ${r.detail}`).join('\n')}

"Set an alarm for 6 AM" — and it just does it.

## What it can do

The eight capability groups the home route leads with. Tool names are the callable, prefixed ones; the app-automation and companion-face rows are on-device actions an MCP client cannot call — see ${absUrl('/tools.md')}.

${FEATURES.map((f) => `### ${f.title}\n\n${f.description}`).join('\n\n')}

## Quick facts

${QUICK_FACTS.map((f) => `- **${f.term}**: ${f.def}`).join('\n')}

## How a command reaches the phone

The phone never accepts a connection — it dials out and holds the socket open, which is why Aster works behind a home router with nothing forwarded. Three transports, and which one you use decides which tool names your client sees.

${CONNECTION_MODES.map((m, i) => `${i + 1}. **${m.name}** (${m.badge}) — ${m.summary} Tools it sees: ${m.toolNamespace}.`).join('\n')}

Each transport is walked end to end, with measured latencies, at ${absUrl('/architecture.md')}.

## What it protects, and what it does not

Aster's own security claims, protections and admitted limits in one list. Each line is a section heading at ${absUrl('/security.md')}, where it is explained in full.

**What it protects:**

${SECURITY_PILLARS.filter((x) => x.kind === 'protection').map((x) => `- ${x.title}`).join('\n')}

**What it does not:**

${SECURITY_PILLARS.filter((x) => x.kind === 'limit').map((x) => `- ${x.title}`).join('\n')}

Both limits are fixable, and ${absUrl('/security.md')} carries the ${HARDENING_STEPS.length} hardening steps plus the reason for each of the ${PERMISSIONS.length} Android permissions.

## What ships with it

${INDEX_DASHBOARD_SENTENCE}

## Installing it

${INDEX_INSTALL_CTA}

## Where to go next

${ROUTES.filter((r) => r.path !== '/').map((r) => `- [${r.title}](${absUrl(r.path)}) — ${r.description}`).join('\n')}
`
}

/**
 * Two strings do the work here, and both are imported rather than written.
 *
 * USE_CASES_LEDE is the route's citable passage — what Aster is, in a sentence
 * an answer engine can lift. USE_CASES_DISCLAIMER is the hedge the page prints
 * twice and this file used to print not at all: the old intro line named a
 * transcript the twin does not even contain, so all 21 **Result:** lines
 * travelled with their invented figures ("47 duplicate sets across 2,104
 * photos") and no warning attached.
 *
 * The per-entry label says "(illustrative)" for the same reason. A twin is read
 * in fragments, and a disclaimer three screens up is not attached to the number
 * a retriever actually lifted.
 */
function twinUseCases(): string {
  return `${frontMatter('/use-cases')}
${USE_CASES_LEDE}

${USE_CASES_DISCLAIMER}

Each entry is a prompt someone gives their assistant, the Aster tools that run, and what comes back.

${USE_CASES.map(
    (u) => `## ${u.prompt}

- **Tools used:** ${u.tools.map((t) => `\`${TOOL_PREFIX}${t}\``).join(', ')}
- **Category:** ${u.category}
- **Result (illustrative):** ${u.response}`,
  ).join('\n\n')}
`
}

/**
 * The delivery details /ai-phone renders under "Where the events go".
 *
 * These three live as literals in app/components/ProactiveSection.vue and
 * nowhere else — they describe the shape of one integration rather than a
 * product-wide fact, so they were never lifted into app/data. Copied here
 * verbatim from that component (the default endpoint and path, the Mattermost
 * body, the settings sub-route and the sample payload) so the twin stops
 * dropping the concrete half of the page. Provenance for the values themselves
 * is mcp/src/event-forwarding/channels/{openclaw,mattermost}.ts; if you change
 * one, change it in the component too.
 */
const AGENT_WEBHOOK_DEFAULT = 'http://localhost:18789/hooks/agent'
const EVENT_FORWARDING_DASHBOARD_PATH = '/settings/event-forwarding'
const EVENT_PAYLOAD_SAMPLE = `{
  "message": "[skill] aster\\n[event] incoming_call\\n[device_id] …\\n[model] …\\n[data-number] +15551212\\n[data-contact] Jane",
  "wakeMode": "now",
  "deliver": true,
  "channel": "whatsapp",
  "to": "+15550001111"
}`

function twinAiPhone(): string {
  return `${frontMatter('/ai-phone')}
## What "its own phone" means

${AI_PHONE_LEDE.definition}

${AI_PHONE_LEDE.contrast}

Scheduling belongs to your AI client, not to Aster. Aster exposes tools and pushes events; the agent decides when to poll or act.

## What it takes

${AI_PHONE_NEEDS_INTRO}

${AI_PHONE_NEEDS.map((n) => `### ${n.title}\n\n${n.detail}`).join('\n\n')}

## What changes

${AI_PHONE_CHANGES_INTRO}

${AI_PHONE_CHANGES.map((c) => `### ${c.title}\n\n${c.detail}`).join('\n\n')}

**${AI_PHONE_LIMIT.title}** ${AI_PHONE_LIMIT.body}

## Proactive event forwarding

Off by default. Turn it on with \`aster set-event-forwarding\` or in the dashboard. Aster POSTs tagged event text to either an OpenClaw-style agent webhook (with a Bearer token) or a Mattermost incoming webhook.

${PROACTIVE_EVENTS.map((e) => `- **${e.event}** — ${e.detail}`).join('\n')}

## Where the events go

Aster POSTs to one destination you name. There is no Aster relay in the middle and no account to create: the server on your machine talks straight to the endpoint.

- **An agent webhook** — the OpenClaw-style hook shape, which ClawdBot and MoltBot also speak. Aster POSTs to \`{endpoint}{webhookPath}\`, by default \`${AGENT_WEBHOOK_DEFAULT}\`, with an \`Authorization: Bearer\` token that has to match the token configured on the gateway.
- **A Mattermost incoming webhook** — the same tagged text posted as \`{ "text": "..." }\`, with no Bearer token. Create the webhook under Integrations, paste the URL in, and optionally override the channel it lands in.

Configure it with \`aster set-event-forwarding\` (the alias \`aster set-openclaw-callbacks\` still works) or in the dashboard at \`${ENDPOINTS.dashboard}${EVENT_FORWARDING_DASHBOARD_PATH}\`. The settings are written to \`${PATHS.eventForwarding}\` — read that file before trusting a phone that has forwarding enabled, because it names the URL every SMS and notification body is sent to.

What the agent receives: one tagged text block per event, no schema and no SDK.

\`\`\`json
${EVENT_PAYLOAD_SAMPLE}
\`\`\`

## Scenarios

${AI_PHONE_SCENARIOS.map(
    (s) => `### ${s.title}

${s.description}

- **Tools:** ${s.tools.map((t) => `\`${TOOL_PREFIX}${t}\``).join(', ')}`,
  ).join('\n\n')}

## Setting one up

${AI_PHONE_SETUP_INTRO}

${AI_PHONE_SETUP_STEPS.map(
    (s, i) => `### Step ${i + 1} — ${s.name}

${s.text}
${s.code ? `\n${s.codeLabel ? `${s.codeLabel}:\n` : ''}\n\`\`\`${s.lang ?? 'bash'}\n${s.code}\n\`\`\`\n` : ''}${s.link ? `\n${s.link.label}: ${s.link.href}\n` : ''}`,
  ).join('\n')}

The full walkthrough — permissions, the device approval gate, pointing an MCP client at the server, and what to do when the device sits on pending — is on ${absUrl('/setup')}.
`
}

/**
 * Arguments as the twin prints them: required names bare, optional ones
 * parenthesised. The page marks the same distinction with weight, which a
 * plain-text reader cannot see, so the twin has to say it in words.
 */
function toolArgs(tool: { args: { name: string; required: boolean }[] }): string {
  if (!tool.args.length) return '_none_'
  return tool.args.map((a) => (a.required ? `\`${a.name}\`` : `\`${a.name}\` (optional)`)).join(', ')
}

function twinTools(): string {
  return `${frontMatter('/tools')}
Every tool below is registered by the MCP server with the \`${TOOL_PREFIX}\` prefix. The callable name is \`${TOOL_PREFIX}take_screenshot\`; \`take_screenshot\` alone will not resolve.

${TOOL_COUNTS.mcpServer} tools over ${TOOL_CATEGORIES.length} categories. The on-device MCP server and the Binder IPC surface expose a DIFFERENT set of ${TOOL_COUNTS.onDevice} unprefixed actions; that catalogue is listed in full after this one, and the transports are traced in ${absUrl('/architecture')}.

## Which catalogue you get, by connection mode

Tool counts are surface-specific: the mode decides which of the two catalogues below is on the wire.

${CONNECTION_MODES.map((m) => `- **${m.name}** (${m.badge}) — ${m.summary} Tools: ${m.toolNamespace}`).join('\n')}

${TOOL_CATEGORIES.map(
    (c) => `## ${c.name}

${c.blurb}

| Tool | Arguments | What it does |
|---|---|---|
${c.tools.map((t) => `| \`${TOOL_PREFIX}${t.name}\` | ${toolArgs(t)} | ${t.summary} |`).join('\n')}`,
  ).join('\n\n')}

## Flat list

${ALL_TOOLS.map((t) => `- \`${TOOL_PREFIX}${t.name}\`${t.args.length ? ` — ${t.args.map((a) => a.name + (a.required ? '' : '?')).join(', ')}` : ''}`).join('\n')}

## The on-device catalogue — ${TOOL_COUNTS.onDevice} unprefixed actions

A second, different catalogue. These are the actions the Android app dispatches itself, reachable two ways and both on the phone: an app on the same device over Binder IPC (OpenAlly, for example), or the on-device Ktor MCP server. They carry NO \`${TOOL_PREFIX}\` prefix — the wire name is the name below.

Neither catalogue contains the other. ${TOOL_COUNTS.shared} names appear in both; ${TOOL_COUNTS.onDeviceOnly} of the actions below cannot be reached from an MCP client at all; exactly ${TOOL_COUNTS.serverOnly} server tool (\`${TOOL_PREFIX}list_devices\`) has no on-device action behind it, because brokering between phones is meaningless on the phone itself.

The "From MCP" column carries the \`${TOOL_PREFIX}\` tool that dispatches the action, or "on-device only" when nothing does. Note \`click_by_view_id\`, which an MCP client calls as \`${TOOL_PREFIX}click_by_id\`: a rename, not a second capability.

${ON_DEVICE_CATEGORIES.map(
    (c) => `### ${c.name}

| Action | What it does | From MCP |
|---|---|---|
${c.actions.map((a) => `| \`${a.action}\` | ${a.summary} | ${a.server ? `\`${a.server}\`` : 'on-device only'} |`).join('\n')}`,
  ).join('\n\n')}
`
}

/**
 * The two blocks /architecture prints around its ports table.
 *
 * Both are template prose in app/pages/architecture.vue (ENDPOINT_ROWS is a
 * page-local const there), so the twin listed 5987/5988/5989 unqualified and
 * was wrong by omission: in on-device MCP mode none of the three exists, and
 * Binder IPC opens no port at all. The addresses went missing with them, which
 * is why the health endpoint and the device WebSocket address appeared nowhere
 * in architecture.md. Values come from app/data/site.ts; only the framing is
 * copied, and it must be changed in both places.
 */
const ENDPOINT_ROWS: { term: string; def: string }[] = [
  { term: 'MCP endpoint for clients', def: ENDPOINTS.mcp },
  { term: 'Health check', def: ENDPOINTS.health },
  { term: 'Web dashboard', def: ENDPOINTS.dashboard },
  { term: 'Device WebSocket', def: ENDPOINTS.deviceWs },
]

const PORTS_CAVEAT =
  'The on-device MCP server is the exception: it runs inside the app on the phone and listens on port 8080, so none of the three ports above exist in that mode. Binder IPC opens no port at all.'

function twinArchitecture(): string {
  return `${frontMatter('/architecture')}
## How one command reaches the phone

${COMMAND_TRACE.map((s, i) => `${i + 1}. **${s.actor}** — ${s.detail}${s.latency ? ` (${s.latency})` : ''}`).join('\n')}

## Five commands, traced

Five real commands through the hops above, each with the tools involved and a round-trip figure. The prompt is what a person types; everything after it is what the machinery does.

${WORKED_EXAMPLES.map(
    (e) => `### ${e.prompt}

${e.steps.map((s) => `- **${s.label}:** ${s.detail}`).join('\n')}
- **Tools:** ${e.tools.map((t) => `\`${t}\``).join(', ')}`,
  ).join('\n\n')}

## Transports

| Mode | Where the MCP server runs | Tool namespace | When to use it |
|---|---|---|---|
${TRANSPORTS.map((t) => `| ${t.name} | ${t.host} | ${t.namespace} | ${t.when} |`).join('\n')}

## Ports

Running \`aster start\` binds three ports on the machine you run it on. The phone only needs to reach the first one; you only need to reach the other two. Open nothing to the public internet — put the link on your LAN or a private mesh.

${PORTS.map((p) => `- \`${p.port}\` — ${p.name}. ${p.detail}`).join('\n')}

${PORTS_CAVEAT}

## Addresses

${ENDPOINT_ROWS.map((r) => `- **${r.term}:** \`${r.def}\``).join('\n')}
`
}

/**
 * Four sections, in the page's order: pillars, permissions, hardening, Q&A.
 *
 * The permissions table carries three columns because PermissionRow carries
 * `required`, and a two-column twin could answer "why does Aster want
 * READ_CALL_LOG" but not "which of these can I decline" — which is the question
 * someone reading a permission list is actually asking.
 *
 * The last two used to be invisible here — SECURITY_FAQS was a const inside
 * app/pages/security.vue and HARDENING_STEPS one inside SecuritySection.vue —
 * so this twin shipped at roughly half the rendered page and dropped, among
 * other things, the answer to "what stops working if I decline all-files
 * access" (eight tools, not four). Both now live in app/data/security.
 */
function twinSecurity(): string {
  return `${frontMatter('/security')}
${SECURITY_PILLARS.map((p) => `## ${p.title}\n\n${p.description}`).join('\n\n')}

## Permissions the app asks for

Only the rows marked **Always** are needed for Aster to run at all; decline any of the rest and you lose only the tools that depend on it.

| Permission | Needed | Why |
|---|---|---|
${PERMISSIONS.map((p) => `| ${p.name} | ${p.required ? 'Always' : 'Per feature'} | ${p.why} |`).join('\n')}

## Hardening

Everything above is what Aster does on its own. These ${HARDENING_STEPS.length} changes to a default install are yours to make, in the order they matter.

${HARDENING_STEPS.map(
    (s, i) => `### ${i + 1}. ${s.title}

${s.body}
${s.code ? `\n${s.codeLabel ? `${s.codeLabel}:\n` : ''}\n\`\`\`bash\n${s.code}\n\`\`\`\n` : ''}`,
  ).join('\n')}

## Common questions

${SECURITY_FAQS.map((f) => `### ${f.q}\n\n${f.a}`).join('\n\n')}
`
}

/**
 * `note` is not decoration: two of the six are the security warnings on the
 * page (do not port-forward the device port, and a pending device looks
 * connected while every call times out). Dropping them shipped a setup guide
 * that omitted exactly the two things that hurt.
 *
 * The fence honours `s.lang` because the last step's `command` is the .mcp.json
 * document, not a shell line; a hardcoded ```bash told every reader that a JSON
 * object was something to paste into a terminal.
 *
 * "Before you start" and the verify INTERPRETATION were page-local consts in
 * app/pages/setup.vue, so this twin shipped at 68% of the rendered page: one
 * prose line of requirements that never mentioned the network, and two bare
 * commands with nothing saying what their output means. The third check is the
 * one that maps a symptom to a cause — an empty tool list means the client never
 * reached the MCP endpoint, a hanging call means the device is still pending —
 * and it was absent entirely.
 */
function twinSetup(): string {
  return `${frontMatter('/setup')}
## Before you start

${REQUIREMENTS.map((r) => `- **${r.term} — ${r.value}.** ${r.detail}`).join('\n')}

${REQUIREMENTS_NOTE.before}[${REQUIREMENTS_NOTE.link}](${REQUIREMENTS_NOTE.href})${REQUIREMENTS_NOTE.after}

${SETUP_STEPS.map(
    (s, i) => `## Step ${i + 1} — ${s.title}

${s.body}
${s.command ? `\n\`\`\`${s.lang ?? 'bash'}\n${s.command}\n\`\`\`\n` : ''}${s.note ? `\nNote: ${s.note}\n` : ''}`,
  ).join('\n')}

## Connecting an AI client

${CLIENTS.map((c) => `### ${c.name}\n\n${c.body}\n${c.snippet ? `\n\`\`\`${c.lang ?? ''}\n${c.snippet}\n\`\`\`\n` : ''}`).join('\n')}

## Checking it works

${VERIFY_INTRO}

\`\`\`bash
${VERIFY_COMMANDS}
\`\`\`

What each check proves:

${VERIFY_CHECKS.map((c) => `- **${c.term}** — ${c.def}`).join('\n')}
`
}

/**
 * Grouped under the same four headings the accordion renders.
 *
 * A flat list here did two things wrong: it dropped the four categories, and it
 * reordered all 18 questions relative to the page, so the twin and the page
 * could not be lined up against each other. FAQS carries `topic` already — the
 * page groups on it — so the grouping is read, never restated.
 *
 * The triage commands are now DIAGNOSTIC_COMMANDS in app/data/faq.ts. They used
 * to be hand-copied here from a page-local const, which is the shape this whole
 * file exists to warn about.
 */
function twinFaq(): string {
  return `${frontMatter('/faq')}
${FAQ_LEDE}

${FAQ_TOPICS.map(
    (topic) => `## ${topic}

${FAQS.filter((f) => f.topic === topic)
      .map((f) => `### ${f.q}\n\n${f.a}`)
      .join('\n\n')}`,
  ).join('\n\n')}

## Still stuck?

Run these three checks on the machine hosting the server before opening an issue. Between them they separate the three failures that account for nearly everything: the server is not running, the device is connected but not approved, or the phone is pointed at an address that no longer exists.

1. Confirm the server is up, and note the LAN address it advertises — that is the address the phone must be able to reach.
2. Check the device's approval state. A device listed as pending is connected and still refused on every command.
3. Hit the health endpoint from wherever your AI client runs, not just from the server itself — that is what proves the path, not the process.

\`\`\`bash
${DIAGNOSTIC_COMMANDS}
\`\`\`

## Aster compared to scrcpy and raw ADB

${COMPARISON_INTRO}

| | ${COMPARISON.columns.join(' | ')} |
|---|${COMPARISON.columns.map(() => '---').join('|')}|
${COMPARISON.rows.map((r) => `| **${r.label}** | ${r.values.join(' | ')} |`).join('\n')}
`
}

const TWINS: Record<string, () => string> = {
  '/index.md': twinIndex,
  '/use-cases.md': twinUseCases,
  '/ai-phone.md': twinAiPhone,
  '/tools.md': twinTools,
  '/architecture.md': twinArchitecture,
  '/security.md': twinSecurity,
  '/setup.md': twinSetup,
  '/faq.md': twinFaq,
}

function llmsFull(): string {
  return `# Aster — full text

> Every page of ${SITE.origin} concatenated, generated ${BUILD_DATE}. Individual pages are listed in ${SITE.origin}/llms.txt.

${ROUTES.map((r) => TWINS[r.twin]()).join('\n\n---\n\n')}
`
}

// ------------------------------------------------------------------ write ---

export async function writeMachineReadable(publicDir: string): Promise<void> {
  const files: Record<string, string> = {
    'sitemap.xml': sitemap(),
    'robots.txt': robots(),
    'llms.txt': llms(),
    'llms-full.txt': llmsFull(),
  }
  for (const [route, render] of Object.entries(TWINS)) {
    files[route.replace(/^\//, '')] = render()
  }

  for (const [name, body] of Object.entries(files)) {
    const target = join(publicDir, name)
    await mkdir(dirname(target), { recursive: true })
    await writeFile(target, body, 'utf8')
  }

  console.log(
    `[machine-readable] wrote ${Object.keys(files).length} files: sitemap.xml (${ROUTES.length} urls), robots.txt, llms.txt, llms-full.txt, ${Object.keys(TWINS).length} markdown twins`,
  )
}

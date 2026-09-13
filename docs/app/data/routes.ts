/**
 * The route registry — the ONE list.
 *
 * Read by nuxt.config.ts (prerender.routes), NavBar.vue (top nav), the sitemap
 * generator, llms.txt and the markdown twins. Adding a page means adding a row
 * here and a file in app/pages/; forgetting the row means the route is not
 * prerendered and GitHub Pages serves the soft-404 shell in its place.
 *
 * `path` is the ROUTER path (leading slash, no trailing slash) — what vue-router
 * matches and what nitro prerenders. `href()` is the PUBLISHED form, and it
 * carries a trailing slash.
 *
 * The two differ because of the host. A prerendered route lands on disk as
 * `<route>/index.html`, and the GitHub Pages static server answers a directory
 * requested without a trailing slash with a 301 to the slashed form (verified
 * against other Pages sites: `/typedoc` -> 301 -> `/typedoc/`). So `/tools` is
 * not a URL this site serves — it is a redirect to one. Declaring it as the
 * canonical pointed every canonical, every sitemap <loc> and every og:url at a
 * redirect, while the URL Google would actually index was the slashed form.
 *
 * Rule: route matching and prerendering use `path`; anything published to the
 * outside world — canonical, og:url, sitemap, llms.txt, JSON-LD item URLs and
 * nav hrefs — uses `href()`.
 *
 * `title` and `description` here are the source for the sitemap and the twins;
 * each page passes the same strings to useRouteSeo(). Keep descriptions at or
 * under 160 characters.
 */

export interface RouteDef {
  path: string
  /** Nav label. Null keeps the route out of the top nav (home is the logo). */
  nav: string | null
  title: string
  description: string
  /** Plain-text twin an LLM can fetch. */
  twin: string
  /** What someone would ask an assistant to land here. Drives the twin's intro. */
  intent: string
  /**
   * Subject terms for the route's structured data.
   *
   * Deliberately NOT `intent`. That field is an authoring aid — the questions we
   * want the route to answer — and emitting a list of search phrases verbatim as
   * schema.org `keywords` reads as query stuffing rather than as a topic list.
   * These are the things the page is about.
   */
  topics: string[]
  /** Sitemap hint only; Google ignores changefreq and priority. */
  priority: number
}

export const ROUTES: RouteDef[] = [
  {
    path: '/',
    nav: null,
    title: 'Aster — AI copilot for Android, or give your AI its own phone',
    description:
      'Aster connects any Android to Claude, OpenClaw or MoltBot over MCP. 49 MCP tools, open source, self-hosted — your AI copilot on mobile, or its own phone.',
    twin: '/index.md',
    intent: 'what is aster mcp, android ai copilot, connect an android phone to claude',
    topics: ['Model Context Protocol', 'Android automation', 'AI agent tooling', 'Self-hosted software'],
    priority: 1.0,
  },
  {
    path: '/use-cases',
    nav: 'Use cases',
    title: 'Use cases — what people actually ask their AI to do',
    description:
      'Real prompts people give Aster: free up storage, find duplicate photos, read notifications, call someone and speak a message, auto-reply while driving.',
    twin: '/use-cases.md',
    intent: 'what can an ai assistant do on my android phone, ai phone automation examples',
    topics: ['Android automation', 'AI assistant workflows', 'Mobile productivity'],
    priority: 0.9,
  },
  {
    path: '/ai-phone',
    nav: 'AI phone',
    title: 'Give your AI its own phone',
    description:
      'Plug a spare Android into a charger and give your AI its own number. It watches notifications, calls you, texts you and acts without being asked.',
    twin: '/ai-phone.md',
    intent: 'give ai its own phone, can an ai call me, forward android notifications to an agent webhook',
    topics: ['Autonomous agents', 'Event forwarding', 'Webhooks', 'Android notifications'],
    priority: 0.9,
  },
  {
    path: '/tools',
    nav: 'Tools',
    title: 'All 49 Aster MCP tools, with arguments',
    description:
      'All 49 aster_* MCP tools with their exact names and arguments, plus the separate catalogue of 77 unprefixed actions the Android app dispatches on-device.',
    twin: '/tools.md',
    intent: 'aster mcp tools list, android mcp server tools, what arguments does aster_send_sms take, on-device action catalog',
    topics: ['Model Context Protocol', 'Tool catalogue', 'Android device control', 'Binder IPC'],
    priority: 0.9,
  },
  {
    path: '/architecture',
    nav: 'Architecture',
    title: 'Architecture — how a command reaches the phone',
    description:
      'How one command travels from your AI to the phone, and the three transports Aster speaks: remote WebSocket, on-device Ktor MCP server, and Binder IPC.',
    twin: '/architecture.md',
    intent: 'how does aster work, how does mcp control an android phone, mcp over binder ipc',
    topics: ['Model Context Protocol', 'WebSocket transport', 'Binder IPC', 'Software architecture'],
    priority: 0.8,
  },
  {
    path: '/security',
    nav: 'Security',
    title: 'Is Aster safe? No root, self-hosted, zero telemetry',
    description:
      'Self-hosted with zero telemetry and no root: device approval gate, screen-control kill switch, banking apps blocked, and where the link is not encrypted.',
    twin: '/security.md',
    intent: 'is aster mcp safe, does android mcp need root, why does aster need SMS permission',
    topics: ['Application security', 'Android runtime permissions', 'Privacy', 'Self-hosting'],
    priority: 0.8,
  },
  {
    path: '/setup',
    nav: 'Setup',
    title: 'Set up Aster — MCP server, Android app, AI client',
    description:
      'Install aster-mcp from npm, run aster start, sideload the Android app, approve the device and point Claude Code at http://localhost:5988/mcp.',
    twin: '/setup.md',
    intent: 'aster mcp install, npm install -g aster-mcp, connect claude code to android',
    topics: ['Installation', 'npm packages', 'Android sideloading', 'MCP client configuration'],
    priority: 0.9,
  },
  {
    path: '/faq',
    nav: 'FAQ',
    title: 'FAQ and troubleshooting',
    description:
      'Device stuck on pending, ws:// versus wss://, which ports to open, no audio on a voice call, why Aster has no chat screen, and how it compares to scrcpy.',
    twin: '/faq.md',
    intent: 'aster device pending commands timeout, does aster need ssl, scrcpy vs mcp',
    topics: ['Troubleshooting', 'Model Context Protocol', 'Android automation'],
    priority: 0.7,
  },
]

export const NAV_ROUTES = ROUTES.filter((r) => r.nav !== null)

/**
 * The published, non-redirecting form of a site-relative path.
 *
 * Adds the trailing slash GitHub Pages redirects to, but only for routes: a
 * path with a file extension (`/tools.md`, `/llms.txt`) is a real file and must
 * be left exactly as it is.
 */
export function href(path: string): string {
  if (path === '/') return '/'
  if (/\.[a-z0-9]+$/i.test(path)) return path
  return path.endsWith('/') ? path : path + '/'
}

export function routeByPath(path: string): RouteDef {
  const found = ROUTES.find((r) => r.path === path)
  if (!found) throw new Error(`Unknown route ${path} — add it to app/data/routes.ts`)
  return found
}

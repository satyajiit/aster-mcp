<template>
  <section id="screenshots" class="relative py-20 sm:py-28 px-6 overflow-hidden">
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-surface-raised/30 to-transparent" aria-hidden="true" />

    <div class="relative max-w-6xl mx-auto">
      <div class="text-center mb-10">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">App and dashboard</p>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Companion apps included
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Aster ships an Android companion app and a web dashboard at <code class="font-mono text-text-primary">{{ ENDPOINTS.dashboard }}</code> for device approval,
          file browsing, live screen control and MCP tool testing. Both ship in a dark and a light theme.
        </p>
      </div>

      <!-- Surface tabs -->
      <div
        ref="tablistRef"
        role="tablist"
        aria-label="Screenshot surface"
        class="flex items-center justify-center gap-2 mb-6"
        @keydown="onTabKeydown"
      >
        <button
          v-for="tab in TABS"
          :id="`shots-tab-${tab.id}`"
          :key="tab.id"
          type="button"
          role="tab"
          :aria-selected="activeTab === tab.id"
          :aria-controls="`shots-panel-${tab.id}`"
          :tabindex="activeTab === tab.id ? 0 : -1"
          class="inline-flex items-center min-h-11 px-5 rounded-xl text-xs font-semibold uppercase tracking-[0.1em] border transition-colors duration-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
          :class="activeTab === tab.id
            ? 'bg-aster/10 border-aster/40 text-aster'
            : 'border-border-dim text-text-tertiary hover:text-text-secondary hover:border-border-subtle'"
          @click="activeTab = tab.id"
        >
          <Icon :name="tab.icon" class="mr-1.5 text-sm" aria-hidden="true" />
          {{ tab.label }}
        </button>
      </div>

      <!-- Theme variant. A pressed pair, not a bare ref: the control has to say
           which variant is currently showing, not just look different. -->
      <div class="flex items-center justify-center gap-2 mb-10" role="group" aria-label="Screenshot theme">
        <button
          v-for="theme in THEMES"
          :key="theme.id"
          type="button"
          :aria-pressed="shotTheme === theme.id"
          class="inline-flex items-center min-h-9 px-4 rounded-lg text-[11px] font-semibold uppercase tracking-[0.1em] border transition-colors duration-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
          :class="shotTheme === theme.id
            ? 'bg-aster/10 border-aster/40 text-aster'
            : 'border-border-dim text-text-tertiary hover:text-text-secondary hover:border-border-subtle'"
          @click="shotTheme = theme.id"
        >
          <Icon :name="theme.icon" class="mr-1 text-xs" aria-hidden="true" />
          {{ theme.label }}
          <span class="sr-only"> theme screenshots</span>
        </button>
      </div>

      <!-- Both panels stay in the markup so the prerendered HTML carries every
           screenshot and its alt text, not just the tab that happens to open. -->
      <div
        v-show="activeTab === 'app'"
        id="shots-panel-app"
        role="tabpanel"
        aria-labelledby="shots-tab-app"
        tabindex="0"
      >
        <!-- A grid, not a 1200px row inside a 1152px container with
             sm:overflow-x-visible clipping the difference. -->
        <ul class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 sm:gap-4">
          <li
            v-for="shot in APP_SCREENSHOTS"
            :key="shot.file"
            class="screenshot-card relative rounded-2xl overflow-hidden border border-border-dim bg-surface-raised/60"
          >
            <img
              :src="src('app', shot.file)"
              :alt="shot.alt"
              width="720"
              height="1560"
              loading="lazy"
              decoding="async"
              class="w-full h-auto block"
            />
            <!-- An opaque plate, not a gradient. The gradient was near-transparent
                 exactly where the text sits, so the label's real background was the
                 image: the light screenshots are near-white there (device-dashboard
                 samples rgb(230,236,244)) and white-on-white failed WCAG 1.4.3.
                 Surface tokens make the contrast a property of the theme, not of
                 whichever capture happens to be showing. -->
            <p class="absolute bottom-0 inset-x-0 bg-surface-raised border-t border-border-dim px-3 py-3">
              <span class="text-[10px] font-semibold uppercase tracking-[0.12em] text-text-primary">{{ shot.label }}</span>
            </p>
          </li>
        </ul>
      </div>

      <div
        v-show="activeTab === 'dashboard'"
        id="shots-panel-dashboard"
        role="tabpanel"
        aria-labelledby="shots-tab-dashboard"
        tabindex="0"
      >
        <ul class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <li
            v-for="shot in DASHBOARD_SCREENSHOTS"
            :key="shot.file"
            class="screenshot-card relative rounded-2xl overflow-hidden border border-border-dim bg-surface-raised/60"
          >
            <img
              :src="src('dashboard', shot.file)"
              :alt="shot.alt"
              :width="shot.width"
              :height="shot.height"
              loading="lazy"
              decoding="async"
              class="w-full h-auto block"
            />
            <!-- Opaque for the same reason as the phone captions above: the light
                 dashboard captures are near-white along their bottom edge
                 (logs samples rgb(250,251,251)). -->
            <p class="absolute bottom-0 inset-x-0 bg-surface-raised border-t border-border-dim px-4 py-3">
              <span class="text-[11px] font-semibold uppercase tracking-[0.12em] text-text-primary">{{ shot.label }}</span>
            </p>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ENDPOINTS } from '~/data/site'

type TabId = 'app' | 'dashboard'
type ThemeId = 'dark' | 'light'

const activeTab = ref<TabId>('app')
const shotTheme = ref<ThemeId>('dark')
const tablistRef = ref<HTMLElement | null>(null)

const TABS = [
  { id: 'app' as const, label: 'Android app', icon: 'lucide:smartphone' },
  { id: 'dashboard' as const, label: 'Web dashboard', icon: 'lucide:monitor' },
]

const THEMES = [
  { id: 'dark' as const, label: 'Dark', icon: 'lucide:moon' },
  { id: 'light' as const, label: 'Light', icon: 'lucide:sun' },
]

/** Light variants live in a `light/` sibling of each folder, same filenames. */
function src(surface: 'app' | 'dashboard', file: string): string {
  const dir = shotTheme.value === 'light' ? `${surface}/light` : surface
  return `/screenshots/${dir}/${file}`
}

/** WAI-ARIA tabs: Left/Right (and Home/End) move between tabs, not Tab. */
function onTabKeydown(event: KeyboardEvent) {
  const order: TabId[] = TABS.map((tab) => tab.id)
  const index = order.indexOf(activeTab.value)
  let next: TabId | undefined

  if (event.key === 'ArrowRight') next = order[(index + 1) % order.length]
  else if (event.key === 'ArrowLeft') next = order[(index - 1 + order.length) % order.length]
  else if (event.key === 'Home') next = order[0]
  else if (event.key === 'End') next = order[order.length - 1]
  if (!next) return

  event.preventDefault()
  activeTab.value = next
  const target = next
  nextTick(() => {
    tablistRef.value?.querySelector<HTMLElement>(`#shots-tab-${target}`)?.focus()
  })
}

/**
 * Alt text describes what the screen SHOWS. It used to be the tab label
 * repeated ("Overview"), which tells a screen-reader user nothing they could
 * not already read from the caption two lines below.
 *
 * All six phone captures are 720x1560; the dashboard captures vary, so each
 * carries its own intrinsic size. Both are set as width/height attributes so
 * the browser reserves the box before the image arrives (no layout shift).
 */
const APP_SCREENSHOTS = [
  {
    file: 'connection-setup.webp',
    label: 'Connection setup',
    alt: 'The Aster app connection screen: a server address and port field, a pairing code, and a Connect button with the current link state underneath.',
  },
  {
    file: 'device-dashboard.webp',
    label: 'Device dashboard',
    alt: 'The app home screen listing the running services, the connection status, battery and storage readouts, and shortcuts to permissions and logs.',
  },
  {
    file: 'services-logs.webp',
    label: 'Tool call logs',
    alt: 'A chronological log of tool calls received from the assistant, each row showing the tool name, its arguments and whether it succeeded.',
  },
  {
    file: 'permissions.webp',
    label: 'Permissions',
    alt: 'The permissions screen with each Android runtime and special-access grant listed and its state, plus the button that requests all of them in one guided pass.',
  },
  {
    file: 'companion-overlay.webp',
    label: 'Companion face',
    alt: 'The animated companion face drawn as an overlay on the home screen, mouth open mid-sentence while text to speech plays.',
  },
  {
    file: 'on-device-mcp.webp',
    label: 'On-device MCP',
    alt: 'The on-device MCP server panel showing the local Ktor server switched on, its listening address, and the tool count it exposes.',
  },
]

// Captured by tools/screenshots (`node run.mjs`), which also syncs them here.
// Every name must exist under both /screenshots/dashboard/ and .../light/.
const DASHBOARD_SCREENSHOTS = [
  {
    file: 'dashboard-overview.webp',
    label: 'Overview',
    width: 1440,
    height: 1340,
    alt: 'The dashboard overview page: connected device count, server uptime, recent tool calls and a panel of quick actions.',
  },
  {
    file: 'device-registry.webp',
    label: 'Device registry',
    width: 1440,
    height: 900,
    alt: 'The device registry table, one row per paired phone with its name, last-seen time and approval state, and an approve or revoke control per row.',
  },
  {
    file: 'device-telemetry.webp',
    label: 'Device telemetry',
    width: 1440,
    height: 1668,
    alt: 'Live telemetry for one phone: battery level and charging state, storage use, network type and the running Aster services.',
  },
  {
    file: 'device-screen-control.webp',
    label: 'Screen control',
    width: 1440,
    height: 900,
    alt: 'The live screen control view mirroring the phone display, with tap, swipe, type and hardware-key controls beside it.',
  },
  {
    file: 'panel-messages.webp',
    label: 'Messages',
    width: 1440,
    height: 900,
    alt: 'The messages panel listing SMS threads read from the phone, with each conversation opened in a reading pane.',
  },
  {
    file: 'panel-apps.webp',
    label: 'Apps inventory',
    width: 1440,
    height: 900,
    alt: 'The installed apps inventory, listing every package on the phone with its label, package name and install date.',
  },
  {
    file: 'panel-storage.webp',
    label: 'Storage and media',
    width: 1440,
    height: 900,
    alt: 'The storage panel breaking free space down by category, alongside the indexed media library and its search box.',
  },
  {
    file: 'logs.webp',
    label: 'Logs',
    width: 1440,
    height: 900,
    alt: 'The server log stream, one line per request with its timestamp, level, source and message, filterable by level.',
  },
  {
    file: 'connect.webp',
    label: 'Connect',
    width: 1440,
    height: 1444,
    alt: 'The connect page with copy-ready MCP client configuration for Claude Code and Claude Desktop, and the endpoint the phone should dial.',
  },
  {
    file: 'file-browser.webp',
    label: 'File browser',
    width: 1440,
    height: 900,
    alt: 'The file browser showing an approved folder on the phone, with a directory tree on the left and file names, sizes and dates on the right.',
  },
  {
    file: 'mcp-tool-explorer.webp',
    label: 'MCP tool explorer',
    width: 1440,
    height: 900,
    alt: 'The MCP tool explorer: every aster_ tool listed with its JSON schema, a form to fill the arguments, and the raw response it returned.',
  },
  {
    file: 'event-forwarding.webp',
    label: 'Event forwarding',
    width: 1440,
    height: 900,
    alt: 'The event forwarding settings, with toggles for SMS, notification, call and device-status events and the webhook URL each one posts to.',
  },
]
</script>

<style scoped>
.screenshot-card {
  transition:
    transform 0.4s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.4s ease,
    box-shadow 0.4s ease;
}

.screenshot-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in oklab, var(--color-primary) 25%, transparent);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.4);
}

@media (prefers-reduced-motion: reduce) {
  .screenshot-card,
  .screenshot-card:hover {
    transition: none;
    transform: none;
  }
}
</style>

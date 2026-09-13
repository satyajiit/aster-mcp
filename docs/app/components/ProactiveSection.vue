<template>
  <!--
    Icon names are repeated here as literals on purpose. @nuxt/icon's
    clientBundle scanner globs **/*.{vue,jsx,tsx,md,mdc,mdx,yml,yaml} only, so
    a name that lives solely in app/data/proactive.ts would miss the bundle and
    be fetched from the Iconify CDN at runtime — a third-party request on a site
    that advertises no third-party relay.
    lucide:message-square lucide:bell lucide:phone-incoming lucide:wifi
    lucide:user-round-plus lucide:video lucide:plane lucide:car lucide:camera
    lucide:moon lucide:message-circle
  -->
  <section id="proactive" class="relative px-6 py-24 sm:py-28 overflow-hidden">
    <div aria-hidden="true" class="absolute inset-0 pointer-events-none">
      <div class="absolute inset-0 bg-gradient-to-b from-surface via-surface-raised to-surface" />
      <div
        class="absolute top-1/4 left-1/2 -translate-x-1/2 w-[760px] max-w-full h-[520px] rounded-full blur-[80px]"
        style="background: radial-gradient(circle, var(--color-aster-glow) 0%, transparent 70%)"
      />
    </div>

    <div class="relative max-w-5xl mx-auto">
      <header class="observe-fade max-w-3xl">
        <p class="font-mono text-xs uppercase tracking-[0.2em] text-aster">Proactive event forwarding</p>
        <h2 class="mt-4 text-3xl sm:text-4xl font-bold tracking-tight text-text-primary leading-[1.15]">
          Your phone tells your AI what just happened
        </h2>
        <p class="mt-5 text-lg leading-relaxed text-text-secondary">
          A copilot waits to be asked. A phone that belongs to your AI does not have to.
          Aster can push the things that arrive on the device &mdash; a text, a notification, a ringing
          call, the phone itself going offline &mdash; straight to your agent as they happen, so the
          agent reacts instead of polling.
        </p>
        <p class="mt-4 text-base leading-relaxed text-text-secondary">
          Forwarding is <strong class="text-text-primary font-semibold">off until you turn it on</strong>.
          Nothing leaves the phone until you run
          <code class="font-mono text-aster">aster set-event-forwarding</code> and name a destination
          you control.
        </p>
      </header>

      <!-- The path one event takes -->
      <ol class="observe-fade mt-12 grid gap-4 sm:grid-cols-3" data-delay="80">
        <li
          v-for="(hop, i) in FLOW"
          :key="hop.name"
          class="relative rounded-2xl border border-border-dim bg-surface-raised p-6"
        >
          <div class="flex items-center gap-3">
            <AIconTile :icon="hop.icon" :accent="hop.accent" :size="36" />
            <span class="font-mono text-xs uppercase tracking-[0.14em] text-text-tertiary">
              Step {{ i + 1 }}
            </span>
          </div>
          <h3 class="mt-4 text-base font-semibold text-text-primary">{{ hop.name }}</h3>
          <p class="mt-2 text-sm leading-relaxed text-text-secondary">{{ hop.detail }}</p>
        </li>
      </ol>

      <!-- What gets forwarded -->
      <div class="observe-fade mt-20" data-delay="60">
        <h3 class="text-2xl font-bold tracking-tight text-text-primary">What gets forwarded</h3>
        <p class="mt-3 max-w-2xl text-base leading-relaxed text-text-secondary">
          Five event kinds, each switchable on its own. Every one arrives as tagged plain text an
          agent can read without a parser.
        </p>

        <dl class="mt-8 divide-y divide-border-dim rounded-2xl border border-border-dim bg-surface-raised">
          <div v-for="event in PROACTIVE_EVENTS" :key="event.event" class="flex gap-4 p-5 sm:p-6">
            <AIconTile :icon="event.icon" :accent="event.accent" :size="36" class="shrink-0" />
            <div>
              <dt class="text-base font-semibold text-text-primary">{{ event.event }}</dt>
              <dd class="mt-1.5 text-sm leading-relaxed text-text-secondary">{{ event.detail }}</dd>
            </div>
          </div>
        </dl>
      </div>

      <!-- Delivery -->
      <div class="observe-fade mt-20" data-delay="60">
        <h3 class="text-2xl font-bold tracking-tight text-text-primary">Where the events go</h3>
        <p class="mt-3 max-w-2xl text-base leading-relaxed text-text-secondary">
          Aster POSTs to one destination of your choosing. There is no Aster relay in the middle and
          no account to create &mdash; the server on your machine talks directly to the endpoint you
          named.
        </p>

        <ul class="mt-8 grid gap-4 sm:grid-cols-2">
          <li class="rounded-2xl border border-border-dim bg-surface-raised p-6">
            <div class="flex items-center gap-3">
              <AIconTile icon="lucide:webhook" accent="var(--color-primary)" :size="36" />
              <h4 class="text-base font-semibold text-text-primary">An agent webhook</h4>
            </div>
            <p class="mt-3 text-sm leading-relaxed text-text-secondary">
              The OpenClaw-style hook shape, which ClawdBot and MoltBot also speak. Aster POSTs to
              <code class="font-mono text-aster">{endpoint}{webhookPath}</code>
              &mdash; by default
              <code class="font-mono text-aster">http://localhost:18789/hooks/agent</code> &mdash;
              with an <code class="font-mono text-aster">Authorization: Bearer</code> token that has
              to match the token configured on the gateway.
            </p>
          </li>
          <li class="rounded-2xl border border-border-dim bg-surface-raised p-6">
            <div class="flex items-center gap-3">
              <AIconTile icon="lucide:message-square" accent="var(--color-info)" :size="36" />
              <h4 class="text-base font-semibold text-text-primary">A Mattermost incoming webhook</h4>
            </div>
            <p class="mt-3 text-sm leading-relaxed text-text-secondary">
              The same tagged text posted as
              <code class="font-mono text-aster">{ "text": "..." }</code>, with no Bearer token.
              Create the webhook in Mattermost under Integrations, paste the URL into the dashboard,
              and optionally override the channel it lands in.
            </p>
          </li>
        </ul>

        <!-- min-w-0 on both cells: a grid item's default min-width:auto lets the
             <pre> min-content width win, so the item never shrinks below the
             snippet's widest line. Below `lg` that overflowed the 375px viewport
             (measured 919px) past this section's overflow-hidden, and because the
             <pre> itself was never narrower than its content ACodeBlock's own
             overflow-x produced no scrollbar — the payload example was simply gone. -->
        <div class="mt-6 grid gap-4 lg:grid-cols-2">
          <div class="min-w-0">
            <ACodeBlock label="Turn it on" :code="CLI_SNIPPET" />
            <p class="mt-3 text-sm leading-relaxed text-text-tertiary">
              Or configure it in the dashboard at
              <code class="font-mono text-aster">{{ ENDPOINTS.dashboard }}/settings/event-forwarding</code>.
              The alias <code class="font-mono">aster set-openclaw-callbacks</code> still works.
            </p>
          </div>
          <div class="min-w-0">
            <ACodeBlock label="What your agent receives" :code="PAYLOAD_SNIPPET" />
            <p class="mt-3 text-sm leading-relaxed text-text-tertiary">
              Every event is one tagged text block. An agent that can read a line can route it &mdash;
              no schema, no SDK.
            </p>
          </div>
        </div>
      </div>

      <!-- Scenarios -->
      <div class="observe-fade mt-20" data-delay="60">
        <h3 class="text-2xl font-bold tracking-tight text-text-primary">What people build with it</h3>
        <p class="mt-3 max-w-2xl text-base leading-relaxed text-text-secondary">
          Each of these is a forwarded event plus one or two tool calls. The tool names are the
          callable ones, exactly as an MCP client lists them.
        </p>

        <ul class="mt-8 grid gap-4 sm:grid-cols-2">
          <li
            v-for="scenario in AI_PHONE_SCENARIOS"
            :id="scenario.id"
            :key="scenario.id"
            class="rounded-2xl border border-border-dim bg-surface-raised p-6"
          >
            <div class="flex items-center gap-3">
              <AIconTile :icon="scenario.icon" :accent="scenario.accent" :size="36" />
              <h4 class="text-base font-semibold leading-snug text-text-primary">{{ scenario.title }}</h4>
            </div>
            <p class="mt-4 text-sm leading-relaxed text-text-secondary">{{ scenario.description }}</p>
            <p class="mt-4 font-mono text-xs uppercase tracking-[0.14em] text-text-tertiary">Tools used</p>
            <ul class="mt-2 flex flex-wrap gap-2">
              <li
                v-for="tool in scenario.tools"
                :key="tool"
                class="rounded-md border border-aster/25 bg-aster/10 px-2 py-1 font-mono text-xs text-aster"
              >
                {{ TOOL_PREFIX }}{{ tool }}
              </li>
            </ul>
          </li>
        </ul>

        <p class="mt-8 max-w-3xl text-sm leading-relaxed text-text-tertiary">
          Aster ships no scheduler and no rules engine. It exposes
          {{ TOOL_COUNTS.mcpServer }} tools and pushes these events; deciding when to act, and what
          counts as worth waking you for, stays with your AI client.
          <NuxtLink to="/tools/" class="text-aster underline underline-offset-4">
            See every tool Aster exposes
          </NuxtLink>.
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { AI_PHONE_SCENARIOS, PROACTIVE_EVENTS } from '~/data/proactive'
import { ENDPOINTS, TOOL_COUNTS, TOOL_PREFIX } from '~/data/site'

/**
 * The three hops one event makes. Kept local because it describes this
 * section's diagram rather than a product fact.
 */
const FLOW = [
  {
    name: 'The phone',
    icon: 'lucide:smartphone',
    accent: 'var(--color-mode-remote)',
    detail:
      'A text lands, an app posts a notification, or the phone starts ringing. The companion app sees it and sends it up its existing WebSocket.',
  },
  {
    name: 'The Aster server',
    icon: 'lucide:server',
    accent: 'var(--color-primary)',
    detail:
      'Your own machine, not ours. It filters the event against the kinds you enabled, formats it as tagged text, and forwards it.',
  },
  {
    name: 'Your AI',
    icon: 'lucide:bot',
    accent: 'var(--color-warning)',
    detail:
      'The agent wakes on the webhook, reads the tags, and decides whether to act, to tell you, or to do nothing at all.',
  },
]

const CLI_SNIPPET = `$ aster set-event-forwarding

  Channel type      agent webhook
  Endpoint          http://localhost:18789
  Webhook path      /hooks/agent
  Bearer token      ****  (must match the gateway)
  Events            sms, notifications, incoming calls,
                    device online/offline, pairing requests

  Saved to ~/.aster/event-forwarding.json`

const PAYLOAD_SNIPPET = `{
  "message": "[skill] aster\\n[event] incoming_call\\n[device_id] …\\n[model] …\\n[data-number] +15551212\\n[data-contact] Jane",
  "wakeMode": "now",
  "deliver": true,
  "channel": "whatsapp",
  "to": "+15550001111"
}`

// Prerender-safe reveal: the HTML above is painted fully visible, and this only
// arms the entrance animation on elements still below the fold at hydration.
// See app/composables/useObserveFade.ts for why the old opacity-0 CSS was a bug.
useObserveFade({ selector: '#proactive .observe-fade' })
</script>

<style scoped>
/* No opacity here by default — the dimmed start state is added by script only
   (`.fade-armed`), so a crawler, an LLM fetcher or a JS-less browser reads the
   section exactly as it was prerendered. */
.observe-fade.fade-armed {
  opacity: 0;
  transform: translateY(24px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

.observe-fade.fade-armed.fade-in {
  opacity: 1;
  transform: translateY(0);
}
</style>

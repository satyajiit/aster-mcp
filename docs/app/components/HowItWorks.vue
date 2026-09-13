<template>
  <section id="how-it-works" class="relative py-24 sm:py-28 px-6 overflow-hidden">
    <!-- Decorative grid + wash. Behind the content, never over it. -->
    <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
      <div
        class="absolute inset-0 opacity-[0.018]"
        style="background-image: linear-gradient(color-mix(in oklab, var(--color-primary) 25%, transparent) 1px, transparent 1px), linear-gradient(90deg, color-mix(in oklab, var(--color-primary) 25%, transparent) 1px, transparent 1px); background-size: 48px 48px;"
      />
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_50%_30%,_rgba(45,212,191,0.05)_0%,_transparent_55%)]" />
    </div>

    <div class="relative max-w-4xl mx-auto">
      <header class="mb-12">
        <span class="text-[11px] font-mono uppercase tracking-[0.28em] text-aster mb-4 block">Command trace</span>
        <!-- Deliberately NOT "How a command reaches the phone": that is the h1
             of /architecture, the page this section opens, and two adjacent
             headings with identical text read as a duplicate to an outline
             reader and waste the only section title this block gets. -->
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Six hops, end to end
        </h2>
        <p class="mt-4 text-[15px] text-text-secondary leading-relaxed max-w-2xl">
          Ask for a screenshot and six things happen, in order, on hardware you own. Your assistant
          calls an MCP tool; the Aster server admits or refuses it; one JSON frame crosses a
          WebSocket to the phone; Android does the work; the answer comes back on the same socket.
          No vendor relay sits in the middle of any hop.
        </p>
      </header>

      <!-- The actual wire format. This is what the server sends, not a sketch of it. -->
      <div class="mb-12">
        <ACodeBlock :code="COMMAND_FRAME" label="The frame on the device socket" />
        <p class="mt-3 text-[13px] text-text-tertiary leading-relaxed">
          Note the <code class="font-mono text-text-secondary">action</code>: the server strips the
          <code class="font-mono text-aster">{{ TOOL_PREFIX }}</code> prefix on dispatch. Your client
          calls <code class="font-mono text-aster">{{ TOOL_PREFIX }}take_screenshot</code>; the phone
          receives <code class="font-mono text-text-secondary">take_screenshot</code>. Only the
          prefixed name is callable from an MCP client.
        </p>
      </div>

      <!-- Hop sequence. A real <ol>: it is a procedure and the order is the point. -->
      <ol class="relative space-y-3 mb-16">
        <li
          v-for="(step, i) in COMMAND_TRACE"
          :key="step.actor"
          class="relative rounded-xl bg-surface-raised border border-border-dim p-4 sm:p-5"
        >
          <div class="flex items-start gap-4">
            <div class="flex flex-col items-center flex-shrink-0">
              <AIconTile :icon="step.icon" :accent="step.accent" :size="40" />
              <span
                v-if="i < COMMAND_TRACE.length - 1"
                class="mt-2 w-px flex-1 min-h-4 bg-border-subtle"
                aria-hidden="true"
              />
            </div>
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <span class="font-mono text-[11px] text-text-tertiary tabular-nums">
                  {{ String(i + 1).padStart(2, '0') }}
                </span>
                <h3 class="text-sm font-semibold" :style="{ color: step.accent }">
                  {{ step.actor }}
                </h3>
                <span
                  v-if="step.latency"
                  class="ml-auto font-mono text-[11px] text-text-tertiary"
                >{{ step.latency }}</span>
              </div>
              <p class="mt-2 text-[14px] text-text-secondary leading-relaxed">
                {{ step.detail }}
              </p>
            </div>
          </div>
        </li>
      </ol>

      <!-- Transports -->
      <h3 class="text-2xl font-bold tracking-tight text-text-primary">
        Three ways a client reaches the phone
      </h3>
      <p class="mt-3 mb-6 text-[15px] text-text-secondary leading-relaxed max-w-2xl">
        The trace above is the default path. The companion app speaks three transports and you pick
        one per device. They differ in where the server runs, how the phone decides to trust the
        caller, and &mdash; the part that bites people &mdash; which tool names a client actually sees.
      </p>

      <!-- The table is min-w-[46rem]; below that it scrolls. A scroll container
           with no tabindex is unreachable by keyboard, so a user who cannot swipe
           never sees the right-hand columns (WCAG 2.1.1). role=region + a name +
           tabindex=0 is the same pattern ComparisonTable uses. -->
      <div
        :ref="(el) => (transportsRegion.el.value = el as HTMLElement)"
        v-bind="transportsRegion.attrs.value"
        class="overflow-x-auto rounded-xl border border-border-dim focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
      >
        <table class="w-full min-w-[46rem] text-left border-collapse">
          <caption class="sr-only">
            Aster's three transports compared by where the server runs, the tool names exposed, and when to choose each.
          </caption>
          <thead>
            <tr class="bg-surface-raised">
              <th scope="col" class="px-4 py-3 text-[11px] font-mono uppercase tracking-wider text-text-tertiary border-b border-border-dim">Transport</th>
              <th scope="col" class="px-4 py-3 text-[11px] font-mono uppercase tracking-wider text-text-tertiary border-b border-border-dim">Where the server runs</th>
              <th scope="col" class="px-4 py-3 text-[11px] font-mono uppercase tracking-wider text-text-tertiary border-b border-border-dim">Tool names a client sees</th>
              <th scope="col" class="px-4 py-3 text-[11px] font-mono uppercase tracking-wider text-text-tertiary border-b border-border-dim">Choose it when</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="t in TRANSPORTS"
              :key="t.id"
              class="align-top border-b border-border-dim last:border-b-0"
            >
              <th scope="row" class="px-4 py-4 text-sm font-semibold text-text-primary whitespace-nowrap">
                {{ t.name }}
              </th>
              <td class="px-4 py-4 text-[13px] text-text-secondary leading-relaxed">{{ t.host }}</td>
              <td class="px-4 py-4 text-[13px] font-mono text-aster leading-relaxed">{{ t.namespace }}</td>
              <td class="px-4 py-4 text-[13px] text-text-secondary leading-relaxed">{{ t.when }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="mt-4 text-[13px] text-text-tertiary leading-relaxed max-w-2xl">
        The catalogues are not the same list. The server exposes
        {{ TOOL_COUNTS.mcpServer }} prefixed tools; the on-device catalogue holds
        {{ TOOL_COUNTS.onDevice }} unprefixed actions. {{ TOOL_COUNTS.shared }} of those are
        reachable from an MCP client and {{ TOOL_COUNTS.onDeviceOnly }} are not; going the other
        way, exactly {{ TOOL_COUNTS.serverOnly }} server tool has no on-device action behind it.
        Anything that tells you all three modes share one set of
        {{ TOOL_COUNTS.mcpServer }} tools is out of date.
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { COMMAND_TRACE, TRANSPORTS } from '~/data/architecture'
import { PORTS, TOOL_COUNTS, TOOL_PREFIX } from '~/data/site'

const transportsRegion = useScrollableRegion('Aster transports compared')

/**
 * The literal frame mcp/src/websocket/index.ts puts on the wire: a UUID id, the
 * unprefixed action, and the tool's params. Shown verbatim so the prefix rule
 * below it is checkable rather than asserted.
 */
const COMMAND_FRAME = `// ws://<server-ip>:${PORTS[0].port}  server -> phone
{
  "type": "command",
  "id": "6f1c9f2e-6d3a-4a51-9a0e-2b7c5d8e1f43",
  "action": "take_screenshot",
  "params": {}
}`
</script>

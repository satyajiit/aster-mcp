<template>
  <section id="tools" class="relative py-24 px-6">
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-aster/[0.015] to-transparent" />

    <div class="relative max-w-5xl mx-auto">
      <div class="mb-12 max-w-2xl">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">MCP tools</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          {{ TOOL_COUNTS.mcpServer }} tools, grouped by what they touch
        </h2>
        <p class="mt-4 text-text-secondary">
          Every tool is registered with the <code class="font-mono text-text-primary">{{ TOOL_PREFIX }}</code> prefix, so
          the callable name is <code class="font-mono text-text-primary">{{ TOOL_PREFIX }}take_screenshot</code> &mdash;
          <code class="font-mono text-text-primary">take_screenshot</code> on its own will not resolve.
        </p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <section
          v-for="category in TOOL_CATEGORIES"
          :key="category.name"
          class="p-6 rounded-2xl bg-surface-raised border border-border-dim"
        >
          <div class="flex items-start gap-3">
            <div
              class="w-9 h-9 shrink-0 rounded-lg flex items-center justify-center"
              :class="ACCENTS[category.accent]?.tile"
            >
              <Icon :name="category.icon" class="text-base" :class="ACCENTS[category.accent]?.mark" aria-hidden="true" />
            </div>
            <div>
              <h3 class="text-base font-semibold text-text-primary">
                {{ category.name }}
                <span class="ml-1 text-sm font-normal text-text-tertiary">({{ category.tools.length }})</span>
              </h3>
              <p class="mt-1 text-sm text-text-secondary">{{ category.blurb }}</p>
            </div>
          </div>

          <ul class="mt-5 space-y-3">
            <!-- The id is what makes the ItemList's per-tool `url` resolve, and
                 what lets anyone (or any answer engine) link to one tool rather
                 than to a page of 49. -->
            <li
              v-for="tool in category.tools"
              :id="`tool-${TOOL_PREFIX}${tool.name}`"
              :key="tool.name"
              class="scroll-mt-24 border-t border-border-dim pt-3"
            >
              <code class="block font-mono text-sm text-aster break-all">{{ TOOL_PREFIX }}{{ tool.name }}</code>
              <p class="mt-1 text-sm text-text-secondary">{{ tool.summary }}</p>
              <!-- The arguments an MCP client must actually send. /architecture
                   promised these were here long before they were; without them
                   "what arguments does aster_send_sms take" was unanswerable on
                   the one page that should own the answer. Required names are
                   emphasised, optional ones are not, and a screen reader is told
                   which is which rather than being left to infer it from weight.
                   Names come from each tool's inputSchema and are re-checked
                   against it at build time by scripts/verify-facts.ts. -->
              <p v-if="tool.args.length" class="mt-1.5 text-xs text-text-tertiary">
                <span class="sr-only">Arguments: </span>
                <template v-for="(arg, i) in tool.args" :key="arg.name">
                  <span v-if="i > 0" aria-hidden="true">, </span><code
                    class="font-mono"
                    :class="arg.required ? 'text-text-secondary font-semibold' : 'text-text-tertiary'"
                  >{{ arg.name }}</code><span class="sr-only">{{ arg.required ? ' (required)' : ' (optional)' }}</span>
                </template>
              </p>
              <p v-else class="mt-1.5 text-xs text-text-tertiary">No arguments.</p>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { TOOL_CATEGORIES } from '~/data/tools'
import { TOOL_PREFIX, TOOL_COUNTS } from '~/data/site'

/**
 * Palette key → class pair. Kept here, as literal strings, so Tailwind's source
 * scan sees them; a class assembled from data at runtime would never be built.
 * Opacity modifiers are on the tile FILL only — never on the icon or on text.
 */
const ACCENTS: Record<string, { tile: string; mark: string }> = {
  aster: { tile: 'bg-aster/10', mark: 'text-aster' },
  violet: { tile: 'bg-violet-500/10', mark: 'text-violet-400' },
  amber: { tile: 'bg-amber-500/10', mark: 'text-amber-400' },
  rose: { tile: 'bg-rose-500/10', mark: 'text-rose-400' },
  blue: { tile: 'bg-blue-500/10', mark: 'text-blue-400' },
  teal: { tile: 'bg-teal-500/10', mark: 'text-teal-400' },
  green: { tile: 'bg-green-500/10', mark: 'text-green-400' },
  cyan: { tile: 'bg-cyan-500/10', mark: 'text-cyan-400' },
}
</script>

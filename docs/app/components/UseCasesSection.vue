<template>
  <section id="use-cases" class="relative py-28 px-6 overflow-hidden">
    <!-- Atmospheric background -->
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-surface-raised/40 to-transparent" aria-hidden="true" />
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] rounded-full bg-aster/[0.02] blur-[120px] pointer-events-none" aria-hidden="true" />

    <div class="relative max-w-6xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-10">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Real-world use cases</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Things you will actually say &mdash; and things your AI will
        </h2>
        <p class="mt-4 text-text-secondary max-w-2xl mx-auto">
          {{ USE_CASES.length }} prompts across {{ USE_CASE_CATEGORIES.length }} categories. Each one lists the real
          <code class="font-mono text-text-primary">{{ TOOL_PREFIX }}*</code> tools that run, exactly as an MCP client
          names them.
        </p>
        <p class="mt-3 text-sm text-text-tertiary max-w-2xl mx-auto">
          <!-- The .replies clause only, not the whole disclaimer: LiveChatSection
               prints the full hedge a screenful above, and repeating it verbatim
               here would be duplicate content. This keeps the caveat adjacent to
               the invented figures in the cards, which is what an answer engine
               lifting this section as a passage needs. Read from app/data so the
               page and the twin cannot drift. -->
          {{ USE_CASES_DISCLAIMER_PARTS.replies }}
        </p>
      </div>

      <!-- Category filter -->
      <div class="mb-10">
        <p :id="`${uid}-filter-label`" class="sr-only">Filter use cases by category</p>
        <div
          role="tablist"
          :aria-labelledby="`${uid}-filter-label`"
          class="flex flex-wrap justify-center gap-2"
        >
          <button
            v-for="(tab, i) in tabs"
            :key="tab"
            :ref="(el) => registerTab(el, i)"
            type="button"
            role="tab"
            :id="`${uid}-tab-${i}`"
            :aria-selected="active === tab"
            :aria-controls="`${uid}-panel`"
            :tabindex="active === tab ? 0 : -1"
            class="px-4 py-2 rounded-full border text-sm font-medium transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
            :class="active === tab
              ? 'border-aster/50 bg-aster/10 text-aster'
              : 'border-border-dim bg-surface-raised text-text-secondary hover:border-border-subtle hover:text-text-primary'"
            @click="active = tab"
            @keydown="onTabKeydown($event, i)"
          >
            {{ tab }}
            <span class="ml-1.5 text-text-tertiary tabular-nums">{{ countFor(tab) }}</span>
          </button>
        </div>
      </div>

      <!-- Cards, grouped by category.
           tabindex=0, not -1: the cards are plain <li>s with no link or button
           inside them, so the panel holds nothing focusable and a keyboard user
           leaving the tablist would skip all of the prompts. The APG puts the
           tabpanel itself in the tab sequence in exactly that case. -->
      <div
        :id="`${uid}-panel`"
        role="tabpanel"
        :aria-labelledby="`${uid}-tab-${activeIndex}`"
        tabindex="0"
        class="space-y-10 rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-aster"
      >
        <section v-for="group in visibleGroups" :key="group.category" class="space-y-4">
          <h3 class="flex items-center gap-3 pl-1 text-xs font-semibold uppercase tracking-[0.15em]" :class="accentOf(group.accent).text">
            <span class="w-7 h-7 rounded-lg flex items-center justify-center" :class="accentOf(group.accent).tile" aria-hidden="true">
              <Icon :name="CATEGORY_ICONS[group.category] ?? 'lucide:sparkles'" class="text-sm" />
            </span>
            {{ group.category }}
          </h3>

          <ul class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3 list-none p-0 m-0">
            <li
              v-for="card in group.cards"
              :key="card.id"
              :id="`use-case-${card.id}`"
              class="use-card group relative rounded-2xl border border-border-dim bg-surface-raised/80 backdrop-blur-sm hover:border-border-subtle transition-all duration-400 overflow-hidden"
            >
              <!-- Top accent line -->
              <div
                class="absolute top-0 inset-x-0 h-px bg-gradient-to-r opacity-0 group-hover:opacity-100 transition-opacity duration-500"
                :class="accentOf(card.accent).line"
                aria-hidden="true"
              />

              <div class="p-5">
                <!-- The human prompt -->
                <div class="flex items-start gap-3 mb-4">
                  <span
                    class="flex-shrink-0 mt-0.5 w-6 h-6 rounded-full bg-aster/15 border border-aster/25 flex items-center justify-center"
                    aria-hidden="true"
                  >
                    <Icon name="lucide:user" class="text-[10px] text-aster" />
                  </span>
                  <p class="text-[13.5px] leading-relaxed text-text-primary font-medium">
                    <span class="sr-only">You say: </span>&ldquo;{{ card.prompt }}&rdquo;
                  </p>
                </div>

                <!-- The illustrative reply -->
                <div class="flex items-start gap-3 ml-0.5">
                  <span
                    class="flex-shrink-0 mt-0.5 w-5 h-5 rounded-full flex items-center justify-center"
                    :class="[accentOf(card.accent).tile, accentOf(card.accent).text]"
                    aria-hidden="true"
                  >
                    <Icon :name="card.icon" class="text-[10px]" />
                  </span>
                  <p class="text-xs leading-relaxed text-text-tertiary">
                    <span class="sr-only">Illustrative reply: </span>{{ card.response }}
                  </p>
                </div>
              </div>

              <!-- Tool tags -->
              <div class="px-5 pb-4">
                <h4 class="sr-only">Tools used</h4>
                <ul class="flex flex-wrap gap-1.5 list-none p-0 m-0">
                  <li
                    v-for="tool in card.tools"
                    :key="tool"
                    class="px-2 py-0.5 rounded-md text-[10px] font-mono border"
                    :class="accentOf(card.accent).tag"
                  >
                    {{ TOOL_PREFIX }}{{ tool }}
                  </li>
                </ul>
              </div>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { USE_CASES, USE_CASE_CATEGORIES, USE_CASES_DISCLAIMER_PARTS, type UseCase } from '~/data/use-cases'
import { TOOL_PREFIX } from '~/data/site'

/**
 * The catalogue lives in app/data/use-cases.ts and is shared with the markdown
 * twin generator, so the page and /use-cases.md cannot drift.
 *
 * PRERENDER: the default tab is "All", so every card is present in the static
 * HTML. The filter narrows what is shown; it never supplies the content.
 *
 * CONTRAST: no opacity modifier appears on any text colour class here. The old
 * tool tags rendered at `text-violet-400/50` and friends, which measured
 * between 2.50:1 and 3.49:1 against the page background. Tints stay on the
 * borders and fills, where opacity is harmless.
 */

const ALL = 'All'
const tabs = [ALL, ...USE_CASE_CATEGORIES]
const active = ref<string>(ALL)
const activeIndex = computed(() => Math.max(0, tabs.indexOf(active.value)))

/**
 * Stable id prefix, not a generated one: the tab ids, the panel id and the
 * per-card `use-case-<slug>` anchors are all linkable, and a hydration-time id
 * would change between builds. This section is rendered once, on /use-cases.
 */
const uid = 'use-cases'

const CATEGORY_ICONS: Record<string, string> = {
  'Photos & media': 'lucide:image',
  'Reminders & alerts': 'lucide:bell',
  'Files & cleanup': 'lucide:folder',
  Communication: 'lucide:message-circle',
  'Daily automations': 'lucide:zap',
  'Proactive monitoring': 'lucide:radio',
  'AI-owned phone': 'lucide:smartphone-charging',
}

/**
 * Full literal class strings — Tailwind scans source text, so a class assembled
 * from a variable (`text-${accent}-400`) would never be generated.
 */
interface AccentClasses { text: string; tile: string; tag: string; line: string }

const ACCENTS: Record<string, AccentClasses> = {
  violet: {
    text: 'text-violet-400',
    tile: 'bg-violet-500/10',
    tag: 'border-violet-500/25 bg-violet-500/10 text-violet-300',
    line: 'from-transparent via-violet-400/60 to-transparent',
  },
  amber: {
    text: 'text-amber-400',
    tile: 'bg-amber-500/10',
    tag: 'border-amber-500/25 bg-amber-500/10 text-amber-300',
    line: 'from-transparent via-amber-400/60 to-transparent',
  },
  teal: {
    text: 'text-teal-400',
    tile: 'bg-teal-500/10',
    tag: 'border-teal-500/25 bg-teal-500/10 text-teal-300',
    line: 'from-transparent via-teal-400/60 to-transparent',
  },
  blue: {
    text: 'text-blue-400',
    tile: 'bg-blue-500/10',
    tag: 'border-blue-500/25 bg-blue-500/10 text-blue-300',
    line: 'from-transparent via-blue-400/60 to-transparent',
  },
  rose: {
    text: 'text-rose-400',
    tile: 'bg-rose-500/10',
    tag: 'border-rose-500/25 bg-rose-500/10 text-rose-300',
    line: 'from-transparent via-rose-400/60 to-transparent',
  },
  fuchsia: {
    text: 'text-fuchsia-400',
    tile: 'bg-fuchsia-500/10',
    tag: 'border-fuchsia-500/25 bg-fuchsia-500/10 text-fuchsia-300',
    line: 'from-transparent via-fuchsia-400/60 to-transparent',
  },
  orange: {
    text: 'text-orange-400',
    tile: 'bg-orange-500/10',
    tag: 'border-orange-500/25 bg-orange-500/10 text-orange-300',
    line: 'from-transparent via-orange-400/60 to-transparent',
  },
}

const FALLBACK_ACCENT: AccentClasses = {
  text: 'text-aster',
  tile: 'bg-aster/10',
  tag: 'border-aster/25 bg-aster/10 text-aster',
  line: 'from-transparent via-aster/60 to-transparent',
}

function accentOf(accent: string): AccentClasses {
  return ACCENTS[accent] ?? FALLBACK_ACCENT
}

interface Group { category: string; accent: string; cards: UseCase[] }

const allGroups: Group[] = USE_CASE_CATEGORIES.map((category) => {
  const cards = USE_CASES.filter((u) => u.category === category)
  return { category, accent: cards[0]?.accent ?? 'teal', cards }
})

const visibleGroups = computed<Group[]>(() =>
  active.value === ALL ? allGroups : allGroups.filter((g) => g.category === active.value),
)

function countFor(tab: string): number {
  return tab === ALL ? USE_CASES.length : USE_CASES.filter((u) => u.category === tab).length
}

// ── Tablist keyboard behaviour (WAI-ARIA roving tabindex) ───────────────────
const tabEls: (HTMLElement | null)[] = []

function registerTab(el: unknown, i: number) {
  // Never runs during SSR, so touching HTMLElement here is safe.
  tabEls[i] = el instanceof HTMLElement ? el : null
}

function focusTab(i: number) {
  const next = tabs[i]
  if (next === undefined) return
  active.value = next
  nextTick(() => tabEls[i]?.focus())
}

function onTabKeydown(event: KeyboardEvent, i: number) {
  const last = tabs.length - 1
  switch (event.key) {
    case 'ArrowRight':
    case 'ArrowDown':
      event.preventDefault()
      focusTab(i === last ? 0 : i + 1)
      break
    case 'ArrowLeft':
    case 'ArrowUp':
      event.preventDefault()
      focusTab(i === 0 ? last : i - 1)
      break
    case 'Home':
      event.preventDefault()
      focusTab(0)
      break
    case 'End':
      event.preventDefault()
      focusTab(last)
      break
  }
}
</script>

<style scoped>
.use-card {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
              border-color 0.4s ease,
              box-shadow 0.4s ease;
}
.use-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.3);
}

@media (prefers-reduced-motion: reduce) {
  .use-card,
  .use-card:hover {
    transition: none;
    transform: none;
  }
}
</style>

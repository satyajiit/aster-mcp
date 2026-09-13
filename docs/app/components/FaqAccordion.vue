<script setup lang="ts">
/**
 * The FAQ list, built on native <details>/<summary>.
 *
 * Deliberately NOT a JavaScript accordion. This site is prerendered and read by
 * fetchers that never run JS: with native disclosure widgets every answer is
 * present in the static HTML, keyboard and screen-reader behaviour is the
 * platform's (correct by construction — no roles, no aria-expanded to keep in
 * sync, no arrow-key handler to get wrong), and the expanded/collapsed state
 * survives find-in-page in browsers that search inside closed details.
 *
 * The only scripted parts are conveniences that degrade to nothing: an
 * expand-all toggle, and opening whichever entry a #fragment points at.
 */
import type { Faq } from '~/data/faq'

const props = defineProps<{ faqs: Faq[] }>()

/** Groups in first-appearance order — the data file controls the reading order. */
const groups = computed(() => {
  const order: string[] = []
  const byTopic = new Map<string, Faq[]>()
  for (const faq of props.faqs) {
    const bucket = byTopic.get(faq.topic)
    if (bucket) {
      bucket.push(faq)
    } else {
      order.push(faq.topic)
      byTopic.set(faq.topic, [faq])
    }
  }
  return order.map((topic) => ({ topic, items: byTopic.get(topic) as Faq[] }))
})

/**
 * Closed on both server and client for a stable first render; every mutation
 * below happens after mount, so there is no hydration mismatch.
 */
const openState = reactive<Record<string, boolean>>(
  Object.fromEntries(props.faqs.map((f) => [f.id, false])),
)

const allOpen = computed(() => props.faqs.every((f) => openState[f.id]))

function onToggle(id: string, event: Event) {
  openState[id] = (event.target as HTMLDetailsElement).open
}

function toggleAll() {
  const next = !allOpen.value
  for (const faq of props.faqs) openState[faq.id] = next
}

onMounted(() => {
  // Landing on /faq#banking-apps should show the answer, not a closed row.
  // decodeURIComponent throws on a malformed hash, which must not take the
  // whole page's hydration with it.
  let id = ''
  try {
    id = decodeURIComponent(window.location.hash.replace(/^#/, ''))
  } catch {
    id = ''
  }
  if (id && id in openState) openState[id] = true
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <p class="text-sm text-text-tertiary">
        {{ faqs.length }} questions, answered from the source.
      </p>
      <button
        type="button"
        class="shrink-0 inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-border-dim bg-surface-raised text-sm font-medium text-text-secondary hover:text-text-primary hover:border-border-subtle transition-colors"
        :aria-pressed="allOpen"
        @click="toggleAll"
      >
        {{ allOpen ? 'Collapse all' : 'Expand all' }}
      </button>
    </div>

    <div class="space-y-10">
      <section v-for="group in groups" :key="group.topic">
        <h2 class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">
          {{ group.topic }}
        </h2>

        <div class="space-y-2">
          <details
            v-for="faq in group.items"
            :id="faq.id"
            :key="faq.id"
            class="faq rounded-2xl border border-border-dim bg-surface-raised/60 hover:border-border-subtle transition-colors"
            :open="openState[faq.id]"
            @toggle="onToggle(faq.id, $event)"
          >
            <!-- One h3 and nothing else: <summary> takes either phrasing content
                 OR a single h1-h6, so a heading with a sibling <span> is neither.
                 The chevron is .faq__summary::after instead. -->
            <summary class="faq__summary">
              <h3 class="text-base font-semibold text-text-primary leading-snug">
                {{ faq.q }}
              </h3>
            </summary>
            <div class="px-5 pb-5 -mt-1">
              <p class="text-sm leading-relaxed text-text-secondary">{{ faq.a }}</p>
            </div>
          </details>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* Clears the fixed 4rem NavBar when an in-page anchor lands on an entry. */
.faq {
  scroll-margin-top: 6rem;
}

.faq__summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  /* Comfortably past the 24x24 CSS px minimum of WCAG 2.5.8. */
  padding: 1rem 1.25rem;
  cursor: pointer;
  list-style: none;
}

/* The default disclosure triangle, removed in both engines. The chevron below
   replaces it; without this Safari draws two markers. */
.faq__summary::-webkit-details-marker {
  display: none;
}

.faq__summary::marker {
  content: '';
}

.faq__summary:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
  border-radius: var(--radius-card, 1rem);
}

/* Drawn as a pseudo-element rather than a <span>, so <summary> can hold the
   single h3 the content model allows. It is still a flex item of .faq__summary,
   so the layout is unchanged. */
.faq__summary::after {
  content: '';
  flex: none;
  width: 0.5rem;
  height: 0.5rem;
  margin-top: 0.4rem;
  border-right: 2px solid var(--color-text-tertiary);
  border-bottom: 2px solid var(--color-text-tertiary);
  transform: rotate(45deg);
  transform-origin: center;
  transition: transform var(--dur-fast, 150ms) ease;
}

details[open] .faq__summary::after {
  transform: rotate(-135deg);
}

@media (prefers-reduced-motion: reduce) {
  .faq__summary::after {
    transition: none;
  }
}
</style>

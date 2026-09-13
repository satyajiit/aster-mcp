<template>
  <section id="security" class="relative py-24 sm:py-32 px-6 overflow-hidden">
    <!-- Background texture. Decorative only. -->
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-surface-raised/30 to-transparent" aria-hidden="true" />
    <div
      class="absolute inset-0 opacity-[0.015]"
      aria-hidden="true"
      style="background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,0.3) 1px, transparent 0); background-size: 32px 32px;"
    />

    <div class="relative max-w-5xl mx-auto">
      <!-- ── Header ──────────────────────────────────────────────────────── -->
      <header class="max-w-3xl">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Security and privacy</span>
        <component :is="as" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          What Aster protects, and what it does not
        </component>
        <p class="mt-5 text-text-secondary leading-relaxed">
          Aster itself has no account, no telemetry and no vendor relay: the server is a package you run on your own
          machine, and its records live in a local SQLite file on that machine. That is a claim about Aster, not about
          your AI client &mdash; whatever you send to Claude, OpenClaw or any other MCP client still goes to that
          client&rsquo;s own model provider under that provider&rsquo;s terms.
        </p>
        <p class="mt-4 text-text-secondary leading-relaxed">
          Below is the whole picture: the guarantees, the two real limits, every permission the Android app asks for and
          why, and the steps that turn a default install into a hardened one.
        </p>
      </header>

      <!-- ── At a glance ─────────────────────────────────────────────────── -->
      <dl class="mt-10 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <div
          v-for="fact in glance"
          :key="fact.term"
          class="rounded-2xl bg-surface-raised/60 border border-border-dim p-4"
        >
          <dt class="text-[11px] font-semibold uppercase tracking-[0.14em] text-text-tertiary">{{ fact.term }}</dt>
          <dd class="mt-1.5 text-sm text-text-primary leading-snug">{{ fact.def }}</dd>
        </div>
      </dl>

      <!-- ── Pillars ─────────────────────────────────────────────────────── -->
      <h2 id="guarantees" class="mt-20 text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
        Seven things worth knowing before you install it
      </h2>
      <p class="mt-3 text-text-secondary max-w-2xl">
        Five are protections. Two are limits, and they are in the same list on purpose &mdash; a security page that only
        lists wins is not a security page.
      </p>

      <ul class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-3 list-none p-0">
        <li
          v-for="pillar in SECURITY_PILLARS"
          :key="pillar.title"
          class="rounded-2xl bg-surface-raised/60 border p-6"
          :class="isLimit(pillar) ? accentOf(pillar).border : 'border-border-dim'"
        >
          <div class="flex items-start gap-3">
            <span
              class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
              :class="accentOf(pillar).tile"
              aria-hidden="true"
            >
              <Icon :name="pillar.icon" class="text-lg" :class="accentOf(pillar).icon" />
            </span>
            <div class="min-w-0">
              <ABadge :tone="isLimit(pillar) ? 'warning' : 'success'" variant="outline">
                {{ isLimit(pillar) ? 'Limit' : 'Protection' }}
              </ABadge>
              <h3 class="mt-2 text-base font-semibold text-text-primary leading-snug">{{ pillar.title }}</h3>
            </div>
          </div>
          <p class="mt-4 text-sm text-text-secondary leading-relaxed">{{ pillar.description }}</p>
        </li>
      </ul>

      <!-- ── Permissions table ───────────────────────────────────────────── -->
      <h2 id="permissions" class="mt-20 text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
        Every permission the Android app asks for
      </h2>
      <p class="mt-3 text-text-secondary max-w-2xl">
        Taken straight from the app&rsquo;s <code class="font-mono text-text-primary">AndroidManifest.xml</code>, with the
        two special-access service grants that are the largest of the lot. Only the rows marked
        <span class="text-text-primary font-medium">Always</span> are needed for Aster to run at all; decline any of the
        rest and you lose only the tools that depend on it.
      </p>

      <!-- The table is min-w-[42rem]; on a phone it scrolls, and a scroll container
           with no tabindex cannot be reached or moved by keyboard, so the "Needed to
           run" column is unreachable without a pointer (WCAG 2.1.1). role=region +
           an accessible name + tabindex=0 is the pattern ComparisonTable uses. -->
      <div
        :ref="(el) => (permissionsRegion.el.value = el as HTMLElement)"
        v-bind="permissionsRegion.attrs.value"
        class="mt-6 rounded-2xl border border-border-dim bg-surface-raised/40 overflow-x-auto focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
      >
        <table class="w-full min-w-[42rem] border-collapse text-left">
          <caption class="sr-only">
            Android permissions declared by the Aster companion app, why each is requested, and whether it is required
            for the app to run.
          </caption>
          <thead>
            <tr class="border-b border-border-subtle">
              <th scope="col" class="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-text-tertiary w-[18rem]">
                Permission
              </th>
              <th scope="col" class="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-text-tertiary">
                Why Aster asks for it
              </th>
              <th scope="col" class="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-text-tertiary w-[7rem]">
                Needed to run
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="permission in PERMISSIONS"
              :key="permission.name"
              class="border-b border-border-dim last:border-b-0 align-top"
            >
              <th scope="row" class="px-4 py-4 font-mono text-xs font-medium text-text-primary break-words">
                {{ permission.name }}
              </th>
              <td class="px-4 py-4 text-sm text-text-secondary leading-relaxed">{{ permission.why }}</td>
              <td class="px-4 py-4">
                <ABadge :tone="permission.required ? 'primary' : 'neutral'" variant="outline">
                  {{ permission.required ? 'Always' : 'Per feature' }}
                </ABadge>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ── Hardening checklist ─────────────────────────────────────────── -->
      <h2 id="hardening" class="mt-20 text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
        Hardening a default install
      </h2>
      <p class="mt-3 text-text-secondary max-w-2xl">
        {{ HARDENING_STEPS.length }} steps, in the order that removes the most risk first.
      </p>

      <ol class="mt-8 space-y-3 list-none p-0">
        <li
          v-for="(step, i) in HARDENING_STEPS"
          :key="step.title"
          class="rounded-2xl bg-surface-raised/60 border border-border-dim p-6 flex gap-4"
        >
          <span
            class="w-8 h-8 rounded-lg bg-aster/10 text-aster flex items-center justify-center text-sm font-semibold flex-shrink-0"
            aria-hidden="true"
          >{{ i + 1 }}</span>
          <div class="min-w-0 flex-1">
            <h3 class="text-base font-semibold text-text-primary">{{ step.title }}</h3>
            <p class="mt-2 text-sm text-text-secondary leading-relaxed">{{ step.body }}</p>
            <ACodeBlock v-if="step.code" class="mt-4" :code="step.code" :label="step.codeLabel" />
          </div>
        </li>
      </ol>

      <!-- ── FAQ ─────────────────────────────────────────────────────────── -->
      <!-- Rendered from the SAME array the route feeds to its FAQPage JSON-LD.
           A structured-data answer that is not visible on the page is a policy
           violation as well as a lie, so there is exactly one copy of the text. -->
      <template v-if="faqs.length">
        <h2 id="faq" class="mt-20 text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
          Questions people actually ask
        </h2>
        <div class="mt-8 space-y-2">
          <details
            v-for="faq in faqs"
            :key="faq.q"
            class="group rounded-2xl bg-surface-raised/60 border border-border-dim overflow-hidden"
          >
            <summary
              class="flex items-center justify-between gap-4 px-6 py-4 cursor-pointer list-none text-base font-semibold text-text-primary hover:text-aster transition-colors"
            >
              {{ faq.q }}
              <Icon
                name="lucide:chevron-down"
                class="text-lg text-text-tertiary flex-shrink-0 transition-transform group-open:rotate-180"
                aria-hidden="true"
              />
            </summary>
            <p class="px-6 pb-5 text-sm text-text-secondary leading-relaxed">{{ faq.a }}</p>
          </details>
        </div>
      </template>

      <!-- ── Audit it yourself ───────────────────────────────────────────── -->
      <h2 id="audit" class="mt-20 text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
        Audit it yourself
      </h2>
      <div class="mt-6 p-6 rounded-2xl bg-surface-raised/40 border border-border-dim flex flex-col sm:flex-row sm:items-center justify-between gap-5">
        <div class="flex items-start gap-4">
          <span class="w-10 h-10 rounded-xl bg-aster/10 flex items-center justify-center flex-shrink-0" aria-hidden="true">
            <Icon name="lucide:code" class="text-lg text-aster" />
          </span>
          <p class="text-sm text-text-secondary leading-relaxed max-w-xl">
            Every claim on this page is a file in the repository. The server, the Android companion and this site are all
            {{ FACTS.license }}-licensed and open: read the approval gate, the denylist, the kill switch and the file
            handlers, and check them against what is written here.
          </p>
        </div>
        <a
          :href="LINKS.repo"
          target="_blank"
          rel="noopener"
          class="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg border border-border-subtle text-sm text-text-secondary hover:text-aster hover:border-aster/40 transition-colors whitespace-nowrap self-start sm:self-auto"
        >
          <Icon name="mdi:github" class="text-base" aria-hidden="true" />
          Read the source
        </a>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The /security route's body.
 *
 * Everything factual comes from ~/data/security and ~/data/site — nothing here
 * hardcodes a port, a version, a tool name or an Android version.
 *
 * Two rules this file exists to keep:
 *
 *  - NO opacity modifier on a text colour class. The previous version faded the
 *    "permission layers" stack to `text-green-300/35` (about 1.5:1) and several
 *    pillar descriptions to a reduced-opacity tertiary. Depth is expressed with
 *    background tint and border only; every text value is full opacity, and
 *    `text-text-tertiary` is the dimmest step permitted.
 *  - Everything renders in the static HTML. No IntersectionObserver gating
 *    visibility, no arrays seeded empty and filled in onMounted, no timers.
 */
import { SECURITY_PILLARS, PERMISSIONS, HARDENING_STEPS, type SecurityPillar } from '~/data/security'
import { ENDPOINTS, FACTS, LINKS } from '~/data/site'

const permissionsRegion = useScrollableRegion('Android permissions Aster requests')

/** Not exported: `<script setup>` may not carry ES module exports. */
interface SecurityFaq {
  q: string
  a: string
}

withDefaults(
  defineProps<{
    /**
     * Heading level for the section title. The /security route renders this
     * section alone, so it owns the page's single h1; pass 'h2' if the section
     * is ever embedded under another page's h1.
     */
    as?: 'h1' | 'h2'
    /**
     * The questions the route also emits as FAQPage JSON-LD. Passed in rather
     * than declared here so that one array feeds both the visible <details>
     * list and the structured data — they cannot drift apart.
     */
    faqs?: SecurityFaq[]
  }>(),
  { as: 'h1', faqs: () => [] },
)

/**
 * Accent key → literal Tailwind classes. Written out in full rather than
 * composed at runtime, because Tailwind only emits classes it can see in source.
 */
const ACCENTS: Record<string, { tile: string; icon: string; border: string }> = {
  aster: { tile: 'bg-aster/10', icon: 'text-aster', border: 'border-aster/30' },
  green: { tile: 'bg-green-500/10', icon: 'text-green-400', border: 'border-green-500/30' },
  amber: { tile: 'bg-amber-500/10', icon: 'text-amber-400', border: 'border-amber-500/30' },
  rose: { tile: 'bg-rose-500/10', icon: 'text-rose-400', border: 'border-rose-500/30' },
  red: { tile: 'bg-red-500/10', icon: 'text-red-400', border: 'border-red-500/30' },
  orange: { tile: 'bg-orange-500/10', icon: 'text-orange-400', border: 'border-orange-500/30' },
  sky: { tile: 'bg-sky-500/10', icon: 'text-sky-400', border: 'border-sky-500/30' },
}

const FALLBACK_ACCENT = ACCENTS.aster!

function accentOf(pillar: SecurityPillar) {
  return ACCENTS[pillar.accent] ?? FALLBACK_ACCENT
}

/** The two pillars that describe a limit rather than a protection. */
const LIMIT_ACCENTS = new Set(['orange', 'sky'])

function isLimit(pillar: SecurityPillar) {
  return LIMIT_ACCENTS.has(pillar.accent)
}

const glance: { term: string; def: string }[] = [
  { term: 'Root required', def: FACTS.rootRequired ? 'Yes' : 'No. Android Accessibility Service only.' },
  { term: 'Telemetry', def: FACTS.telemetry ? 'Yes' : 'None. No analytics, no account, no vendor relay.' },
  { term: 'Data at rest', def: 'A local SQLite file on the machine running the server.' },
  { term: 'Device link', def: `Plain ${ENDPOINTS.deviceWs.split(':')[0]}:// on your LAN. Use Tailscale to encrypt it.` },
]

</script>

<style scoped>
/* The default disclosure triangle, removed in both engines — the chevron icon in
   the summary replaces it. WebKit still draws the legacy marker pseudo, and the
   standards ::marker survives `list-style: none` in some engines, so both rules
   are needed or two markers appear. Same pair as FaqAccordion. */
summary::-webkit-details-marker {
  display: none;
}

summary::marker {
  content: '';
}
</style>

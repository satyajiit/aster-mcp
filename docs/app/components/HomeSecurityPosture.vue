<template>
  <section id="posture" aria-labelledby="posture-heading" class="relative py-20 sm:py-28 px-6 scroll-mt-20">
    <div class="max-w-5xl mx-auto">
      <div class="text-center mb-12">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">Before you install it</p>
        <h2 id="posture-heading" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          What it protects, and what it does not
        </h2>
        <p class="mt-4 text-text-secondary max-w-2xl mx-auto leading-relaxed">
          Handing an AI a phone is a real grant, so here is the short version with the awkward parts
          left in. Nothing below is a summary of a summary &mdash; each line is the heading of a
          section on the security page that explains it in full.
        </p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <h3 class="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.15em] text-green-400 mb-4">
            <Icon name="lucide:shield-check" aria-hidden="true" />
            What it protects
          </h3>
          <ul class="space-y-2.5 list-none p-0">
            <li
              v-for="pillar in PROTECTIONS"
              :key="pillar.title"
              class="flex gap-3 rounded-xl border border-border-dim bg-surface-raised px-4 py-3"
            >
              <Icon name="ph:check-circle" class="shrink-0 mt-0.5 text-green-400" aria-hidden="true" />
              <span class="text-sm text-text-secondary leading-relaxed">{{ pillar.title }}</span>
            </li>
          </ul>
        </div>

        <div>
          <h3 class="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.15em] text-amber-400 mb-4">
            <Icon name="lucide:shield-ban" aria-hidden="true" />
            What it does not
          </h3>
          <ul class="space-y-2.5 list-none p-0">
            <li
              v-for="pillar in LIMITS"
              :key="pillar.title"
              class="flex gap-3 rounded-xl border border-amber-500/25 bg-amber-500/[0.06] px-4 py-3"
            >
              <Icon name="ph:warning-circle" class="shrink-0 mt-0.5 text-amber-400" aria-hidden="true" />
              <span class="text-sm text-text-secondary leading-relaxed">{{ pillar.title }}</span>
            </li>
          </ul>
          <p class="mt-4 text-sm text-text-tertiary leading-relaxed">
            Both limits are fixable and the security page says how &mdash; withhold all-files access,
            and put the link on a private mesh before it leaves your LAN.
          </p>
        </div>
      </div>

      <p class="mt-8 text-sm text-text-secondary text-center">
        <NuxtLink :to="href('/security')" class="text-aster underline underline-offset-4 hover:text-aster-light">
          Read the full security page
        </NuxtLink>
        &mdash; {{ PERMISSIONS.length }} permissions with the reason for each, and {{ HARDENING_STEPS.length }} hardening steps.
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The security posture, on the home route.
 *
 * Home previously said "no telemetry" in the closing CTA and nothing else — a
 * landing page for software that takes control of a phone should not make a
 * reader visit a second page to learn what the grant costs. This lists the
 * pillar TITLES only; every description stays on /security, so this is a table
 * of contents rather than a second copy of the page.
 *
 * The split is driven by each pillar's `kind`, never by array position: the two
 * limits are deliberately interleaved with the protections in the source list,
 * and slicing by index would relabel them the moment anyone reorders it.
 */
import { HARDENING_STEPS, PERMISSIONS, SECURITY_PILLARS } from '~/data/security'
import { href } from '~/data/routes'

const PROTECTIONS = SECURITY_PILLARS.filter((p) => p.kind === 'protection')
const LIMITS = SECURITY_PILLARS.filter((p) => p.kind === 'limit')
</script>

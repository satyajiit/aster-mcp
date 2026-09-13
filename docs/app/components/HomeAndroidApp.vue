<template>
  <section id="android-app" aria-labelledby="android-app-heading" class="relative py-20 sm:py-28 px-6 scroll-mt-20">
    <div class="max-w-5xl mx-auto">
      <div class="text-center mb-12">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">The Android app</p>
        <h2 id="android-app-heading" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          The phone half works on its own
        </h2>
        <p class="mt-4 text-text-secondary max-w-2xl mx-auto leading-relaxed">
          <template v-for="(part, i) in segments(ANDROID_APP_LEDE)" :key="i">
            <code v-if="part.code" class="font-mono text-[0.9em] text-aster">{{ part.text }}</code>
            <template v-else>{{ part.text }}</template>
          </template>
        </p>
      </div>

      <ul class="grid grid-cols-1 md:grid-cols-3 gap-4 list-none p-0">
        <li v-for="cap in ANDROID_APP_STANDALONE" :key="cap.title">
          <ACard variant="hero" class="!p-6 h-full">
            <AIconTile :icon="cap.icon" accent="var(--color-primary)" :size="40" aria-hidden="true" />
            <h3 class="mt-4 text-base font-semibold text-text-primary">{{ cap.title }}</h3>
            <p class="mt-2 text-sm text-text-secondary leading-relaxed">{{ cap.detail }}</p>
          </ACard>
        </li>
      </ul>

      <div class="mt-10 rounded-2xl border border-border-dim bg-surface-raised overflow-hidden">
        <div class="flex flex-col gap-5 p-6 sm:p-8 sm:flex-row sm:items-center sm:justify-between">
          <div class="min-w-0">
            <h3 class="text-lg font-bold text-text-primary">Download the companion app</h3>
            <p class="mt-1.5 text-sm text-text-secondary leading-relaxed">
              Version {{ FACTS.appVersion }} for {{ FACTS.androidMinLabel }}. Sideloaded from GitHub, not the Play Store.
            </p>
          </div>

          <div class="flex flex-col sm:flex-row gap-3 sm:shrink-0">
            <a
              :href="LINKS.releasesLatest"
              target="_blank"
              rel="noopener"
              class="inline-flex items-center justify-center gap-2 min-h-12 px-6 rounded-xl bg-aster text-surface font-semibold text-sm tracking-wide hover:bg-aster-light transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster whitespace-nowrap"
            >
              <Icon name="lucide:download" class="text-base" aria-hidden="true" />
              Get the APK
              <span class="sr-only">from GitHub Releases (opens in a new tab)</span>
            </a>
            <NuxtLink
              :to="href('/setup')"
              class="inline-flex items-center justify-center gap-2 min-h-12 px-6 rounded-xl border border-border-subtle text-text-secondary font-medium text-sm hover:border-aster/30 hover:text-text-primary transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster whitespace-nowrap"
            >
              <Icon name="lucide:list-checks" class="text-base" aria-hidden="true" />
              How to install it
            </NuxtLink>
          </div>
        </div>

        <dl class="grid grid-cols-1 sm:grid-cols-2 border-t border-border-dim">
          <!-- Two bottom-border rules, because the grid changes shape. One column
               at mobile: only the final row sits against the container's rounded
               edge, so `last:` clears it. Two columns from sm: the final ROW is
               the last two children, so both must clear it. With only the sm rule
               the mobile layout drew a stray rule under "Licence". -->
          <div
            v-for="(fact, i) in ANDROID_APP_FACTS"
            :key="fact.term"
            class="px-6 sm:px-8 py-4 border-b border-border-dim last:border-b-0 sm:[&:nth-last-child(-n+2)]:border-b-0"
            :class="i % 2 === 0 ? 'sm:border-r sm:border-r-border-dim' : ''"
          >
            <dt class="text-xs font-semibold uppercase tracking-[0.15em] text-text-tertiary">{{ fact.term }}</dt>
            <dd class="mt-1.5 text-sm text-text-secondary leading-relaxed">{{ fact.def }}</dd>
          </div>
        </dl>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The companion app as a thing you can download, not as the far end of the
 * server's socket.
 *
 * Measured on the artifact before this existed: the ONLY link to GitHub
 * Releases anywhere on the home route was the footer's "Releases" list item,
 * and no sentence on the page said the phone can serve MCP without a Node
 * server. A reader who wanted just the app had no path and no download.
 *
 * This does not restate HomeConnectionModes above it. That section is about
 * which transport a command travels over; this one is about the artifact —
 * what the APK gives you alone, what it costs to install, and where it is.
 */
import { ANDROID_APP_FACTS, ANDROID_APP_LEDE, ANDROID_APP_STANDALONE } from '~/data/android-app'
import { FACTS, LINKS } from '~/data/site'
import { href } from '~/data/routes'

/** Same backtick split index.vue uses for QUICK_FACTS; no v-html. */
function segments(def: string): { text: string; code: boolean }[] {
  return def
    .split(/`([^`]+)`/)
    .map((text, i) => ({ text, code: i % 2 === 1 }))
    .filter((part) => part.text.length > 0)
}
</script>

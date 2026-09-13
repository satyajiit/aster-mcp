<template>
  <section id="how-it-works" aria-labelledby="modes-heading" class="relative py-20 sm:py-28 px-6 scroll-mt-20">
    <div class="max-w-5xl mx-auto">
      <div class="text-center mb-12">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">How it works</p>
        <h2 id="modes-heading" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Three ways a command reaches the phone
        </h2>
        <p class="mt-4 text-text-secondary max-w-2xl mx-auto leading-relaxed">
          The phone never accepts a connection. It dials out and holds the socket open, which is why
          Aster works behind a home router with nothing forwarded. Which transport you get decides
          which tool names your client sees &mdash; and that is the one detail worth reading twice.
        </p>
      </div>

      <ol class="grid grid-cols-1 md:grid-cols-3 gap-4 list-none p-0">
        <li v-for="(mode, i) in CONNECTION_MODES" :key="mode.id">
          <ACard variant="hero" class="!p-6 h-full flex flex-col">
            <div class="flex items-center gap-3 mb-3">
              <span
                class="shrink-0 w-7 h-7 rounded-lg bg-aster/10 border border-aster/25 flex items-center justify-center text-xs font-semibold text-aster"
                aria-hidden="true"
              >{{ i + 1 }}</span>
              <ABadge>{{ mode.badge }}</ABadge>
            </div>
            <h3 class="text-base font-semibold text-text-primary">{{ mode.name }}</h3>
            <p class="mt-2 text-sm text-text-secondary leading-relaxed flex-1">{{ mode.summary }}</p>
            <p class="mt-4 pt-3 border-t border-border-dim text-xs text-text-tertiary">
              Tools it sees:
              <span class="font-mono text-text-secondary">{{ mode.toolNamespace }}</span>
            </p>
          </ACard>
        </li>
      </ol>

      <p class="mt-8 text-sm text-text-secondary text-center max-w-2xl mx-auto">
        <NuxtLink :to="href('/architecture')" class="text-aster underline underline-offset-4 hover:text-aster-light">
          The architecture page
        </NuxtLink>
        walks one command end to end through each transport, with the real latencies.
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The three transports, on the home route.
 *
 * Home previously named Binder IPC exactly once — inside the Quick facts table —
 * and never explained the mechanism at all, so the landing page answered "what
 * is Aster" and "what can it do" but not "how does it reach my phone", which is
 * the question every reader asks third. The data is CONNECTION_MODES, the same
 * array the twins read; this renders it rather than restating it, so the
 * summary here cannot drift from /architecture.
 */
import { CONNECTION_MODES } from '~/data/site'
import { href } from '~/data/routes'
</script>

<template>
  <section id="features" class="relative py-20 sm:py-28 px-6">
    <div class="max-w-6xl mx-auto">
      <div class="text-center mb-12">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">Capabilities</p>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          {{ TOOL_COUNTS.mcpServer }} tools. Your phone, or its own.
        </h2>
        <p class="mt-4 text-text-secondary max-w-2xl mx-auto">
          Every tool an MCP client sees is namespaced <code class="font-mono text-text-primary">{{ TOOL_PREFIX }}*</code>, so
          nothing here collides with the other servers you have connected. Eight capability groups are below;
          <NuxtLink to="/tools/" class="text-aster underline underline-offset-4 hover:text-aster-light">the full list of {{ TOOL_COUNTS.mcpServer }} tools lives on the tools page</NuxtLink>.
        </p>
      </div>

      <ul class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <FeatureCard
          v-for="feature in FEATURES"
          :key="feature.id"
          :icon="feature.icon"
          :title="feature.title"
          :description="feature.description"
          :accent="feature.accent"
        />
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * Eight cards, down from sixteen. The two that were cut outright — "Safety
 * Rails" and "Owner-Approved Folders" — restated /security almost word for
 * word, and a visitor asking whether this is safe should be reading that page,
 * not a card. The rest of what was here is covered on /tools and /architecture.
 * The card copy itself lives in app/data/features.ts.
 */
import { FEATURES } from '~/data/features'
import { TOOL_COUNTS, TOOL_PREFIX } from '~/data/site'
</script>

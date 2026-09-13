<template>
  <section aria-labelledby="author-heading" class="relative py-16 px-6">
    <div class="max-w-3xl mx-auto">
      <div class="flex flex-col sm:flex-row items-center gap-6 p-6 sm:p-8 rounded-2xl bg-surface-raised border border-border-dim text-center sm:text-left">
        <div class="flex-shrink-0 w-14 h-14 rounded-2xl bg-red-500/10 border border-red-500/30 flex items-center justify-center">
          <Icon name="mdi:youtube" class="text-3xl text-red-400" aria-hidden="true" />
        </div>

        <div class="flex-1">
          <h2 id="author-heading" class="text-lg font-bold text-text-primary mb-1">Follow the author</h2>
          <p class="text-sm text-text-secondary leading-relaxed">
            Aster is built by <span class="text-text-primary font-medium">{{ SITE.author }}</span>.
            Tutorials, demos and release notes go up on YouTube.
          </p>
        </div>

        <a
          :href="LINKS.youtube"
          target="_blank"
          rel="noopener"
          class="group inline-flex items-center gap-2 min-h-12 px-5 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 font-semibold text-sm hover:bg-red-500/20 hover:border-red-500/50 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster whitespace-nowrap"
        >
          <Icon name="mdi:youtube" class="text-lg" aria-hidden="true" />
          Subscribe on YouTube
          <Icon name="lucide:external-link" class="text-xs transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
        </a>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { LINKS, SITE } from '~/data/site'
</script>

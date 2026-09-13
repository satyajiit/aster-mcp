<template>
  <section class="relative px-6 py-28 sm:py-36">
    <div class="max-w-2xl mx-auto text-center">
      <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">{{ statusCode }}</p>
      <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight text-text-primary">
        {{ isNotFound ? 'That page is not here' : 'Something went wrong' }}
      </h1>
      <p class="mt-5 text-lg text-text-secondary leading-relaxed">
        <template v-if="isNotFound">
          Aster used to be one long page; it is eight now. If you followed an old link, the section you
          wanted is almost certainly one of these.
        </template>
        <template v-else>
          The page could not be rendered. Every route below is a static file, so one of them will load.
        </template>
      </p>

      <nav class="mt-10" aria-label="All pages">
        <ul class="flex flex-wrap justify-center gap-2">
          <li>
            <NuxtLink
              to="/"
              class="inline-flex items-center min-h-11 px-4 rounded-xl bg-aster text-surface font-semibold text-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
            >Home</NuxtLink>
          </li>
          <li v-for="r in NAV_ROUTES" :key="r.path">
            <NuxtLink
              :to="href(r.path)"
              class="inline-flex items-center min-h-11 px-4 rounded-xl border border-border-dim text-text-secondary text-sm hover:text-aster hover:border-aster/40 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
            >{{ r.nav }}</NuxtLink>
          </li>
        </ul>
      </nav>

      <p class="mt-10 text-sm text-text-tertiary">
        Looking for the machine-readable copy? It is at
        <a href="/llms.txt" class="text-text-secondary hover:text-aster underline underline-offset-4">/llms.txt</a>.
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { NAV_ROUTES, href } from '~/data/routes'

/**
 * The body of the 404, shared by two callers that reach it very differently:
 *
 *  - app/pages/404.vue, prerendered to 404/index.html and then copied over
 *    404.html by the nitro hook in nuxt.config.ts. This is the copy GitHub Pages
 *    actually serves, and the only one a crawler with no JavaScript ever sees.
 *  - app/error.vue, which handles a genuine client-side routing error.
 *
 * Keeping one component means the two cannot drift into saying different things.
 */
const props = withDefaults(defineProps<{ statusCode?: number }>(), { statusCode: 404 })

const isNotFound = computed(() => props.statusCode === 404)
</script>

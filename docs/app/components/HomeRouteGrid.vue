<template>
  <section id="start-here" aria-labelledby="start-here-heading" class="relative py-20 sm:py-28 px-6 scroll-mt-20">
    <div class="max-w-5xl mx-auto">
      <div class="text-center mb-12">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">Start here</p>
        <h2 id="start-here-heading" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          The rest of the documentation
        </h2>
        <p class="mt-4 text-text-secondary max-w-2xl mx-auto leading-relaxed">
          Seven pages, each answering one question end to end. Every one of them is a plain
          prerendered document, and every one has a
          <a href="/llms.txt" class="text-aster underline underline-offset-4 hover:text-aster-light">markdown twin</a>
          an AI client can fetch instead of the HTML.
        </p>
      </div>

      <ul class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 list-none p-0">
        <li v-for="route in NAV_ROUTES" :key="route.path">
          <NuxtLink
            :to="href(route.path)"
            class="group flex flex-col h-full rounded-2xl border border-border-dim bg-surface-raised p-5 transition-colors hover:border-aster/30 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
          >
            <span class="flex items-center justify-between gap-3">
              <span class="text-base font-semibold text-text-primary group-hover:text-aster transition-colors">
                {{ route.nav }}
              </span>
              <Icon
                name="lucide:arrow-right"
                class="shrink-0 text-text-tertiary transition-transform group-hover:translate-x-0.5 group-hover:text-aster"
                aria-hidden="true"
              />
            </span>
            <span class="mt-2 text-sm text-text-secondary leading-relaxed">{{ route.description }}</span>
          </NuxtLink>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The link grid the adopted route map called for and that never got built.
 *
 * Measured on the shipped artifact before this existed: home carried ZERO
 * in-content links to /use-cases, /architecture and /faq. They were reachable
 * only through the top nav (which collapses into a JS drawer on mobile) and the
 * footer, so the landing page never pointed at three of its own seven children.
 *
 * The card copy is `description` straight from the route registry — the same
 * string that is already this route's meta description, its sitemap entry and
 * its twin's front matter. Rendering the registry means the teaser cannot
 * disagree with the page it links to, which is the failure mode that made the
 * round-1 audit reject hand-written teaser blocks.
 */
import { NAV_ROUTES, href } from '~/data/routes'
</script>

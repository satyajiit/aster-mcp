<script setup lang="ts">
/**
 * /faq — troubleshooting and the questions the rest of the site never answered.
 *
 * This is the one route that genuinely earns FAQPage markup: every question
 * below is a real question with a single answer, and the `text` in the JSON-LD
 * is the identical string the accordion renders, which is what Google's
 * structured-data policy requires. Both come from app/data/faq.ts, so they
 * cannot drift apart.
 */
import { COMPARISON, COMPARISON_INTRO, DIAGNOSTIC_COMMANDS, FAQ_LEDE, FAQS } from '~/data/faq'
import { routeByPath } from '~/data/routes'
import { LINKS } from '~/data/site'

const route = routeByPath('/faq')

useRouteSeo({
  title: route.title,
  description: route.description,
  path: route.path,
  textTwin: route.twin,
  breadcrumb: route.nav ?? undefined,
  schema: [
    {
      '@context': 'https://schema.org',
      '@type': 'FAQPage',
      name: route.title,
      description: route.description,
      mainEntity: FAQS.map((faq) => ({
        '@type': 'Question',
        name: faq.q,
        acceptedAnswer: { '@type': 'Answer', text: faq.a },
      })),
    },
  ],
})
</script>

<template>
  <article>
    <!-- Page header -->
    <section class="relative px-6 pt-32 pb-12">
      <div class="max-w-3xl mx-auto">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">
          FAQ and troubleshooting
        </span>
        <h1 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Answers to the questions people actually hit
        </h1>
        <p class="mt-4 text-text-secondary leading-relaxed">
          {{ FAQ_LEDE }}
        </p>
      </div>
    </section>

    <!-- The questions -->
    <section id="questions" class="relative px-6 pb-20">
      <div class="max-w-3xl mx-auto">
        <FaqAccordion :faqs="FAQS" />
      </div>
    </section>

    <!-- scrcpy / ADB comparison -->
    <section id="comparison" class="relative px-6 pb-20">
      <div class="max-w-3xl mx-auto">
        <h2 class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
          Why not just scrcpy or ADB?
        </h2>
        <p class="mt-3 mb-6 text-text-secondary leading-relaxed">
          {{ COMPARISON_INTRO }}
        </p>
        <ComparisonTable :comparison="COMPARISON" />
      </div>
    </section>

    <!-- Still stuck -->
    <section id="still-stuck" class="relative px-6 pb-32">
      <div class="max-w-3xl mx-auto rounded-2xl border border-border-dim bg-surface-raised/60 p-6 sm:p-8">
        <h2 class="text-xl font-bold tracking-tight text-text-primary">Still stuck?</h2>
        <p class="mt-3 text-text-secondary leading-relaxed">
          Run these three checks on the machine hosting the server before you open an issue. Between
          them they separate the three failures that account for nearly everything: the server is not
          running, the device is connected but not approved, or the phone is pointed at an address
          that no longer exists.
        </p>

        <ol class="mt-6 space-y-3 text-sm text-text-secondary list-decimal pl-5 marker:text-text-tertiary">
          <li>
            <span class="text-text-primary font-medium">Confirm the server is up</span> and note the
            LAN address it advertises — that is the address the phone must be able to reach.
          </li>
          <li>
            <span class="text-text-primary font-medium">Check the device's approval state.</span>
            A device listed as pending is connected and still refused on every command.
          </li>
          <li>
            <span class="text-text-primary font-medium">Hit the health endpoint</span> from wherever
            your AI client runs, not just from the server itself — that is what proves the path, not
            the process.
          </li>
        </ol>

        <div class="mt-6">
          <ACodeBlock :code="DIAGNOSTIC_COMMANDS" label="Diagnostics" />
        </div>

        <h3 class="mt-8 text-sm font-semibold uppercase tracking-[0.15em] text-text-tertiary">
          Where to go next
        </h3>
        <ul class="mt-4 grid gap-2 sm:grid-cols-2">
          <li>
            <NuxtLink
              to="/setup/"
              class="block px-4 py-3 rounded-xl border border-border-dim bg-surface text-sm text-text-secondary hover:text-text-primary hover:border-border-subtle transition-colors"
            >
              Set up the server and the app
            </NuxtLink>
          </li>
          <li>
            <NuxtLink
              to="/security/"
              class="block px-4 py-3 rounded-xl border border-border-dim bg-surface text-sm text-text-secondary hover:text-text-primary hover:border-border-subtle transition-colors"
            >
              How the link is secured, and where it is not
            </NuxtLink>
          </li>
          <li>
            <a
              :href="LINKS.issues"
              rel="noopener"
              class="block px-4 py-3 rounded-xl border border-border-dim bg-surface text-sm text-text-secondary hover:text-text-primary hover:border-border-subtle transition-colors"
            >
              Open an issue on GitHub
            </a>
          </li>
          <li>
            <a
              :href="LINKS.repo"
              rel="noopener"
              class="block px-4 py-3 rounded-xl border border-border-dim bg-surface text-sm text-text-secondary hover:text-text-primary hover:border-border-subtle transition-colors"
            >
              Read the source on GitHub
            </a>
          </li>
        </ul>
      </div>
    </section>
  </article>
</template>

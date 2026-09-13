<template>
  <HeroSection />

  <!-- Quick facts sits SECOND, directly under the hero, and that placement is
       the point. It used to sit 65% of the way down the rendered text, behind
       ~1,200 characters of "you gave your AI a voice / now give it hands" —
       so the one block that actually answers what Aster is, what it runs on,
       how many tools and which ports was below the fold of any extractor that
       truncates. The narrative reads just as well after the facts as before
       them; the facts do not survive being second.

       It is also the only list, table or definition list on the route: it is
       built from the same QUICK_FACTS the markdown twins use, so the page and
       /index.md cannot disagree about what Aster is, what it runs on, how many
       tools it has or which ports it binds. -->
  <section id="quick-facts" aria-labelledby="quick-facts-heading" class="relative py-20 sm:py-28 px-6">
    <div class="max-w-4xl mx-auto">
      <div class="text-center mb-10">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">In one screen</p>
        <h2 id="quick-facts-heading" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Quick facts
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Everything worth knowing before you install it, with nothing rounded off.
        </p>
      </div>

      <dl class="rounded-2xl border border-border-dim bg-surface-raised overflow-hidden">
        <div
          v-for="fact in QUICK_FACTS"
          :key="fact.term"
          class="grid grid-cols-1 sm:grid-cols-[10rem_1fr] gap-1 sm:gap-6 px-5 sm:px-6 py-4 border-b border-border-dim last:border-b-0"
        >
          <dt class="text-sm font-semibold text-text-primary">{{ fact.term }}</dt>
          <dd class="text-sm text-text-secondary leading-relaxed">
            <template v-for="(part, i) in segments(fact.def)" :key="i">
              <code v-if="part.code" class="font-mono text-[0.8125rem] text-aster">{{ part.text }}</code>
              <template v-else>{{ part.text }}</template>
            </template>
          </dd>
        </div>
      </dl>
    </div>
  </section>

  <EmbraceSection />

  <!-- How it works, then what it costs you, then where to go next.
       Home answered "what is Aster" and "what can it do" and stopped: it named
       Binder IPC once inside a table, said "no telemetry" once in the closing
       CTA, and linked to three of its seven child routes not at all. All three
       sections below render data that already exists — CONNECTION_MODES,
       SECURITY_PILLARS and the route registry — rather than restating it, so
       none of them can drift from the pages they summarise. -->
  <HomeConnectionModes />
  <FeaturesGrid />
  <HomeSecurityPosture />
  <ScreenshotsSection />
  <HomeAndroidApp />
  <HomeRouteGrid />
  <AuthorSection />

  <!-- Closing CTA -->
  <section aria-labelledby="cta-heading" class="relative py-20 sm:py-28 px-6">
    <div class="max-w-3xl mx-auto text-center">
      <div class="relative rounded-3xl border border-aster/20 bg-surface-raised px-6 sm:px-10 py-12 overflow-hidden">
        <div class="absolute -top-24 left-1/2 -translate-x-1/2 w-[420px] h-[420px] max-w-full rounded-full bg-[radial-gradient(circle,_rgba(45,212,191,0.10)_0%,_transparent_70%)] pointer-events-none" aria-hidden="true" />

        <div class="relative">
          <h2 id="cta-heading" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
            Three commands and a sideload.
          </h2>
          <p class="mt-4 text-text-secondary max-w-xl mx-auto leading-relaxed">
            Install <code class="font-mono text-text-primary">{{ FACTS.npmPackage }}</code> from npm on any machine running Node {{ FACTS.nodeRequirement }},
            start the server, install the companion app on {{ FACTS.androidMinLabel }}, approve the device,
            and point your MCP client at <code class="font-mono text-text-primary">{{ ENDPOINTS.mcp }}</code>.
          </p>

          <div class="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
            <NuxtLink
              to="/setup/"
              class="inline-flex items-center gap-2 min-h-12 px-7 rounded-xl bg-aster text-surface font-semibold text-sm tracking-wide hover:bg-aster-light transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
            >
              <Icon name="lucide:terminal" class="text-base" aria-hidden="true" />
              Read the setup guide
            </NuxtLink>
            <NuxtLink
              to="/security/"
              class="inline-flex items-center gap-2 min-h-12 px-7 rounded-xl border border-border-subtle text-text-secondary font-medium text-sm hover:border-aster/30 hover:text-text-primary transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
            >
              <Icon name="lucide:shield-check" class="text-base" aria-hidden="true" />
              What it can and cannot do
            </NuxtLink>
          </div>

          <p class="mt-6 text-xs text-text-tertiary">
            {{ FACTS.license }} licensed, self-hosted, no account and no telemetry. Root is not required.
          </p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { NAV_ROUTES, routeByPath } from '~/data/routes'
import { DISAMBIGUATION, ENDPOINTS, FACTS, LINKS, QUICK_FACTS, SITE, TOOL_COUNTS, TOOL_PREFIX } from '~/data/site'

const route = routeByPath('/')

/**
 * QUICK_FACTS carries backticks because the markdown twins consume the same
 * strings. Split on them here rather than shipping literal backticks into the
 * rendered page — and without v-html, so there is no injection surface.
 */
function segments(def: string): { text: string; code: boolean }[] {
  // A split on a capturing group alternates plain text and captured text, so
  // odd indices are exactly the backticked runs. Map first, filter after —
  // filtering first would shift the parity and mis-mark every segment.
  return def
    .split(/`([^`]+)`/)
    .map((text, i) => ({ text, code: i % 2 === 1 }))
    .filter((part) => part.text.length > 0)
}

useRouteSeo({
  title: route.title,
  description: route.description,
  path: route.path,
  textTwin: route.twin,
  schema: [
    {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      name: SITE.name,
      alternateName: FACTS.npmPackage,
      description: route.description,
      applicationCategory: 'DeveloperApplication',
      applicationSubCategory: 'Model Context Protocol server',
      // "Aster" collides with much larger entities — Aster DM Healthcare, Aster
      // Data Systems, the ASTER instrument on NASA's Terra satellite. Without
      // this, a bare "Aster" query has no reason to resolve to this project.
      disambiguatingDescription: DISAMBIGUATION,
      operatingSystem: `Server: macOS, Linux or Windows with Node.js ${FACTS.nodeRequirement}. Phone: ${FACTS.androidMinLabel}.`,
      // The npm package version, because THIS node is the npm package
      // (alternateName aster-mcp, installUrl the npm page). The Android
      // companion ships on its own version and gets its own node below; the two
      // were previously conflated, so the page's structured data said 1.7.1
      // while its own visible hero said 0.1.16.
      softwareVersion: FACTS.serverVersion,
      softwareRequirements: `Node.js ${FACTS.nodeRequirement} on the host machine; ${FACTS.androidMinLabel} on the phone; an MCP-capable AI client. No root access required.`,
      featureList: [
        `${TOOL_COUNTS.mcpServer} MCP tools, all namespaced ${TOOL_PREFIX}*`,
        'Screen control: screenshots, UI tree, tap, swipe and type',
        'Media indexing and natural-language photo search',
        'Calls and SMS, including a call that speaks a message aloud',
        'Notification, SMS and device-status event forwarding to a webhook',
        'Camera stills and video on demand or on an event',
      ],
      url: abs('/'),
      downloadUrl: LINKS.releases,
      installUrl: LINKS.npm,
      license: 'https://opensource.org/licenses/MIT',
      isAccessibleForFree: true,
      offers: {
        '@type': 'Offer',
        price: FACTS.price,
        priceCurrency: FACTS.priceCurrency,
        availability: 'https://schema.org/InStock',
      },
      author: {
        '@type': 'Person',
        name: SITE.author,
        url: SITE.authorUrl,
      },
      publisher: {
        '@type': 'Organization',
        name: SITE.publisher,
        url: SITE.publisherUrl,
      },
      sameAs: [LINKS.repo, LINKS.npm],
    },
    {
      '@context': 'https://schema.org',
      '@type': 'MobileApplication',
      name: `${SITE.name} companion app`,
      description: `The Android half of Aster: a sideloaded companion app that gives the ${FACTS.npmPackage} server a device to act on. Not on the Google Play Store \u2014 it is installed from a GitHub release.`,
      applicationCategory: 'UtilitiesApplication',
      operatingSystem: FACTS.androidMinLabel,
      softwareVersion: FACTS.appVersion,
      // The same target the visible "Get the APK" button uses. Structured data
      // that points somewhere the page does not is the kind of mismatch a
      // rich-result check flags, and it is pointless besides.
      downloadUrl: LINKS.releasesLatest,
      installUrl: LINKS.releasesLatest,
      softwareRequirements: `${FACTS.androidMinLabel}. No root. Sideloaded from GitHub Releases; not distributed through the Google Play Store.`,
      license: 'https://opensource.org/licenses/MIT',
      isAccessibleForFree: true,
      offers: {
        '@type': 'Offer',
        price: FACTS.price,
        priceCurrency: FACTS.priceCurrency,
        availability: 'https://schema.org/InStock',
      },
      publisher: {
        '@type': 'Organization',
        name: SITE.publisher,
        url: SITE.publisherUrl,
      },
      sameAs: [LINKS.repo],
    },
    {
      '@context': 'https://schema.org',
      '@type': 'WebSite',
      name: SITE.name,
      url: abs('/'),
      description: route.description,
      inLanguage: 'en',
      publisher: {
        '@type': 'Organization',
        name: SITE.publisher,
        url: SITE.publisherUrl,
      },
    },
    {
      '@context': 'https://schema.org',
      '@type': 'ItemList',
      '@id': `${abs('/')}#start-here`,
      name: 'Aster documentation',
      description: 'The seven documentation pages, each answering one question end to end.',
      itemListOrder: 'https://schema.org/ItemListOrderAscending',
      numberOfItems: NAV_ROUTES.length,
      itemListElement: NAV_ROUTES.map((r, i) => ({
        '@type': 'ListItem',
        position: i + 1,
        name: r.nav,
        description: r.description,
        url: abs(r.path),
      })),
    },
    {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      name: SITE.publisher,
      url: SITE.publisherUrl,
      sameAs: [LINKS.repo, LINKS.openally],
      founder: {
        '@type': 'Person',
        name: SITE.author,
        url: SITE.authorUrl,
      },
    },
  ],
})
</script>

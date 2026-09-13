<template>
  <!-- NavBar, <main> and FooterSection belong to app/layouts/default.vue.
       A page renders only its own sections. -->

  <!-- This header exists because the page used to open on LiveChatSection,
       whose h1 is the slogan "Like texting a super-powered assistant" and whose
       first 1,900 characters are a transcript the page itself labels
       illustrative. That left /use-cases the one route with no self-contained,
       non-disclaimed fact anywhere on it: nothing an answer engine could quote,
       and an h1 naming neither the product nor the topic. The lede below is the
       page's citable passage; the transcript now follows it as illustration. -->
  <section class="relative px-6 pt-28 pb-4">
    <div class="max-w-3xl mx-auto text-center">
      <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4">Use cases</p>
      <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight text-text-primary">
        What people ask Aster to do on an Android phone
      </h1>
      <!-- One string, rendered here and emitted verbatim into /use-cases.md.
           Backticked runs become <code>; see segments() below. -->
      <p class="mt-5 text-lg text-text-secondary leading-relaxed">
        <template v-for="(part, i) in segments(USE_CASES_LEDE)" :key="i">
          <code v-if="part.code" class="font-mono text-text-primary">{{ part.text }}</code>
          <template v-else>{{ part.text }}</template>
        </template>
      </p>
    </div>
  </section>

  <LiveChatSection :heading-level="2" />
  <UseCasesSection />
</template>

<script setup lang="ts">
import { routeByPath } from '~/data/routes'
import { SITE, TOOL_PREFIX } from '~/data/site'
import { USE_CASES, USE_CASES_LEDE } from '~/data/use-cases'
import { abs, useRouteSeo } from '~/composables/useRouteSeo'

/**
 * /use-cases — what people ask their AI to do.
 *
 * LiveChatSection opens the page because one worked conversation sells the idea
 * faster than a grid does, and it carries the <h1> for that reason. The
 * catalogue follows.
 *
 * JSON-LD is a CollectionPage wrapping an ItemList of every use case. The list
 * is generated from app/data/use-cases.ts — the same array the page renders and
 * the same one scripts/generate-machine-readable.ts turns into /use-cases.md,
 * so the structured data cannot describe a page that is not there.
 */

const route = routeByPath('/use-cases')

/**
 * USE_CASES_LEDE carries backticks because the markdown twin consumes the same
 * string. Split on them here rather than shipping literal backticks into the
 * rendered page, and without v-html, so there is no injection surface. Odd
 * indices of a split on a capturing group are exactly the backticked runs.
 */
function segments(text: string): { text: string; code: boolean }[] {
  return text
    .split(/`([^`]+)`/)
    .map((part, i) => ({ text: part, code: i % 2 === 1 }))
    .filter((part) => part.text.length > 0)
}

const itemList = {
  '@type': 'ItemList',
  name: 'Aster use cases',
  description:
    'Prompts people give an AI assistant that is connected to an Android phone through Aster, with the MCP tools each one calls.',
  numberOfItems: USE_CASES.length,
  itemListOrder: 'https://schema.org/ItemListUnordered',
  itemListElement: USE_CASES.map((useCase, i) => ({
    '@type': 'ListItem',
    position: i + 1,
    url: `${abs(route.path)}#use-case-${useCase.id}`,
    // These were typed HowTo, which is wrong: `step` is HowTo's defining
    // property and all 21 shipped without one, so the markup described 21
    // procedures containing no procedure. What each entry actually is, is a
    // question someone asks and the answer they get back — Question/Answer says
    // that honestly and needs no invented steps.
    item: {
      '@type': 'Question',
      name: useCase.prompt,
      answerCount: 1,
      acceptedAnswer: {
        '@type': 'Answer',
        text: useCase.response,
      },
      // The tools are the exact names an MCP client shows. Unprefixed names are
      // not callable, so the structured data carries the prefix too.
      mentions: useCase.tools.map((tool) => ({ '@type': 'SoftwareApplication', name: `${TOOL_PREFIX}${tool}` })),
      // schema.org `about` has range Thing, not Text. Every other page on the
      // site models it that way; this one disagreed with itself.
      about: { '@type': 'Thing', name: useCase.category },
    },
  })),
}

useRouteSeo({
  title: route.title,
  description: route.description,
  path: route.path,
  textTwin: route.twin,
  breadcrumb: route.nav ?? undefined,
  schema: [
    {
      '@context': 'https://schema.org',
      '@type': 'CollectionPage',
      '@id': abs(route.path),
      url: abs(route.path),
      name: route.title,
      description: route.description,
      inLanguage: 'en',
      isPartOf: { '@type': 'WebSite', '@id': abs('/'), name: SITE.name, url: abs('/') },
      about: {
        '@type': 'SoftwareApplication',
        name: SITE.name,
        applicationCategory: 'DeveloperApplication',
        operatingSystem: 'Android',
      },
      // Stated in the page copy as well: the replies are written examples, not
      // measured output, and must not be quoted as product statistics.
      disambiguatingDescription:
        'Example prompts with illustrative assistant replies. Tool names are exact; figures and place names in the replies are written examples, not recorded results.',
      mainEntity: itemList,
    },
  ],
})
</script>

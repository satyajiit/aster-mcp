<template>
  <!-- Icon literals for the @nuxt/icon clientBundle scanner: lucide:arrow-right -->
  <AiPhoneHero />
  <ProactiveSection />

  <section id="ai-phone-setup" class="relative px-6 py-24 sm:py-28">
    <div class="max-w-3xl mx-auto">
      <p class="font-mono text-xs uppercase tracking-[0.2em] text-aster">Setting one up</p>
      <h2 class="mt-4 text-3xl sm:text-4xl font-bold tracking-tight text-text-primary leading-[1.15]">
        How to give your AI its own phone
      </h2>
      <p class="mt-5 text-lg leading-relaxed text-text-secondary">
        {{ AI_PHONE_SETUP_INTRO }}
      </p>

      <ol class="mt-10 space-y-6">
        <li
          v-for="(step, i) in AI_PHONE_SETUP_STEPS"
          :key="step.name"
          class="relative rounded-2xl border border-border-dim bg-surface-raised p-6 pl-16"
        >
          <span
            class="absolute left-6 top-6 flex h-8 w-8 items-center justify-center rounded-lg border border-aster/30 bg-aster/10 font-mono text-sm font-semibold text-aster"
            aria-hidden="true"
          >
            {{ i + 1 }}
          </span>
          <h3 class="text-lg font-semibold text-text-primary">{{ step.name }}</h3>
          <p class="mt-2 text-base leading-relaxed text-text-secondary">{{ step.text }}</p>
          <ACodeBlock v-if="step.code" class="mt-4" :code="step.code" :label="step.codeLabel" />
          <a
            v-if="step.link"
            :href="step.link.href"
            class="mt-4 inline-flex min-h-11 items-center gap-2 text-sm font-semibold text-aster underline underline-offset-4"
            rel="noopener"
          >
            {{ step.link.label }}
            <Icon name="lucide:arrow-right" aria-hidden="true" />
          </a>
        </li>
      </ol>

      <p class="mt-10 text-base leading-relaxed text-text-secondary">
        The full walkthrough &mdash; permissions, the device approval gate, pointing an MCP client at
        the server, and what to do when the device sits on <em>pending</em> &mdash; lives on the setup
        page.
      </p>

      <div class="mt-6 flex flex-wrap gap-3">
        <NuxtLink
          to="/setup/"
          class="inline-flex min-h-11 items-center gap-2 rounded-xl border border-aster/40 bg-aster/10 px-5 py-3 text-sm font-semibold text-text-primary transition-colors hover:bg-aster/20"
        >
          Read the full setup guide
          <Icon name="lucide:arrow-right" aria-hidden="true" />
        </NuxtLink>
        <NuxtLink
          to="/security/"
          class="inline-flex min-h-11 items-center gap-2 rounded-xl border border-border-subtle bg-surface-raised px-5 py-3 text-sm font-semibold text-text-primary transition-colors hover:border-aster/40"
        >
          What a dedicated phone exposes
          <Icon name="lucide:arrow-right" aria-hidden="true" />
        </NuxtLink>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { AI_PHONE_SETUP_INTRO, AI_PHONE_SETUP_STEPS } from '~/data/ai-phone'
import { AI_PHONE_SCENARIOS } from '~/data/proactive'
import { routeByPath } from '~/data/routes'
import { FACTS, SITE } from '~/data/site'
import { abs } from '~/composables/useRouteSeo'

const route = routeByPath('/ai-phone')

useRouteSeo({
  title: route.title,
  description: route.description,
  path: route.path,
  textTwin: route.twin,
  breadcrumb: route.nav ?? undefined,
  schema: [
    {
      '@context': 'https://schema.org',
      '@type': 'TechArticle',
      headline: route.title,
      description: route.description,
      url: abs(route.path),
      mainEntityOfPage: { '@type': 'WebPage', '@id': abs(route.path) },
      inLanguage: 'en',
      isAccessibleForFree: true,
      author: { '@type': 'Person', name: SITE.author, url: SITE.authorUrl },
      publisher: { '@type': 'Organization', name: SITE.publisher, url: SITE.publisherUrl },
      about: [
        { '@type': 'Thing', name: 'Model Context Protocol' },
        { '@type': 'Thing', name: 'Android automation' },
        { '@type': 'Thing', name: 'Proactive AI agents' },
      ],
      // route.topics, not route.intent: `intent` is the authoring note about what
      // someone would ASK to land here, and publishing a list of questions as
      // schema.org keywords reads as query stuffing.
      keywords: route.topics.join(', '),
    },
    {
      '@context': 'https://schema.org',
      '@type': 'HowTo',
      name: 'Give your AI its own phone',
      description:
        'Dedicate a spare Android to an AI agent: install the Aster server, sideload the companion app, approve the device, connect an MCP client and enable proactive event forwarding.',
      url: abs('/ai-phone') + '#ai-phone-setup',
      totalTime: 'PT20M',
      estimatedCost: { '@type': 'MonetaryAmount', currency: FACTS.priceCurrency, value: FACTS.price },
      supply: [
        { '@type': 'HowToSupply', name: `A spare Android phone running ${FACTS.androidMinLabel}` },
        { '@type': 'HowToSupply', name: 'A charger and a stable Wi-Fi connection' },
        { '@type': 'HowToSupply', name: 'Optionally a SIM, so the phone can call and text' },
      ],
      tool: [
        { '@type': 'HowToTool', name: `The ${FACTS.npmPackage} server on Node ${FACTS.nodeRequirement}` },
        { '@type': 'HowToTool', name: 'The Aster Android companion app' },
        { '@type': 'HowToTool', name: 'An MCP client such as Claude Code, OpenClaw or MoltBot' },
      ],
      step: AI_PHONE_SETUP_STEPS.map((step, i) => ({
        '@type': 'HowToStep',
        position: i + 1,
        name: step.name,
        text: step.text,
        url: `${abs('/ai-phone')}#ai-phone-setup`,
      })),
    },
    {
      '@context': 'https://schema.org',
      '@type': 'ItemList',
      name: 'What people build on a phone the AI owns',
      description:
        'Scenarios combining Aster proactive event forwarding with one or two MCP tool calls on a dedicated Android.',
      itemListOrder: 'https://schema.org/ItemListUnordered',
      numberOfItems: AI_PHONE_SCENARIOS.length,
      itemListElement: AI_PHONE_SCENARIOS.map((scenario, i) => ({
        '@type': 'ListItem',
        position: i + 1,
        name: scenario.title,
        description: scenario.description,
        url: `${abs('/ai-phone')}#${scenario.id}`,
      })),
    },
  ],
})
</script>

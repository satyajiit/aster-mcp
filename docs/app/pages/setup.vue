<template>
  <!-- The layout owns .noise, NavBar, <main> and the footer. This page renders
       only its own sections.

       Icon literals for the @nuxt/icon clientBundle scanner (it globs .vue,
       never .ts): lucide:server lucide:smartphone lucide:wifi — the three
       REQUIREMENTS icons, which now live in ~/data/setup so /setup.md can
       carry "Before you start". The scanner cannot follow them there. -->
  <section class="relative px-6 pt-28 pb-4 sm:pt-32">
    <div class="max-w-3xl mx-auto">
      <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-3 block">Setup</span>
      <h1 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
        Set up Aster
      </h1>
      <p class="mt-4 text-lg text-text-secondary leading-relaxed">
        Install the server from npm, sideload the Android companion, approve the device once,
        and point your AI client at
        <code class="font-mono text-aster">{{ ENDPOINTS.mcp }}</code>. Nothing signs up, nothing
        phones home, and the whole path runs on hardware you own.
      </p>
    </div>
  </section>

  <!-- Requirements: the page never used to state these, so people found out
       mid-install that the APK needs Android 8.0, not 7.0. -->
  <section id="requirements" class="relative px-6 py-10 scroll-mt-20">
    <div class="max-w-3xl mx-auto">
      <h2 class="text-xl font-semibold tracking-tight text-text-primary mb-4">Before you start</h2>

      <ACard variant="hero" class="!p-6">
        <dl class="grid gap-5 sm:grid-cols-3">
          <div v-for="req in REQUIREMENTS" :key="req.term">
            <dt class="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.12em] text-text-tertiary">
              <Icon :name="req.icon" class="text-aster" aria-hidden="true" />
              {{ req.term }}
            </dt>
            <dd class="mt-2 text-sm text-text-primary font-medium">{{ req.value }}</dd>
            <dd class="mt-1 text-sm text-text-secondary leading-relaxed">{{ req.detail }}</dd>
          </div>
        </dl>

        <p class="mt-6 text-sm text-text-secondary leading-relaxed">
          {{ REQUIREMENTS_NOTE.before
          }}<a :href="REQUIREMENTS_NOTE.href" target="_blank" rel="noopener" class="text-aster underline underline-offset-2">{{ REQUIREMENTS_NOTE.link }}</a>{{ REQUIREMENTS_NOTE.after }}
        </p>
      </ACard>
    </div>
  </section>

  <SetupSteps />

  <IntegrationsSection />

  <!-- Verification. The old page stopped at "configure your client" and never
       said how to tell whether any of it worked. -->
  <section id="verify" class="relative px-6 py-20 sm:py-24 scroll-mt-20">
    <div class="max-w-3xl mx-auto">
      <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-3 block">Verify</span>
      <h2 class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
        Checking it actually works
      </h2>
      <p class="mt-3 text-text-secondary max-w-2xl">
        {{ VERIFY_INTRO }}
      </p>

      <ACodeBlock
        class="mt-6"
        label="terminal"
        :code="VERIFY_COMMANDS"
      />

      <dl class="mt-6 space-y-4">
        <div v-for="check in VERIFY_CHECKS" :key="check.term">
          <dt class="text-sm font-semibold text-text-primary">{{ check.term }}</dt>
          <dd class="mt-1 text-sm text-text-secondary leading-relaxed">{{ check.def }}</dd>
        </div>
      </dl>

      <p class="mt-8 text-sm text-text-secondary">
        Still stuck? The
        <NuxtLink to="/faq/" class="text-aster underline underline-offset-2">FAQ and troubleshooting page</NuxtLink>
        covers pending devices, ws:// versus wss://, firewall ports and silent voice calls.
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { routeByPath } from '~/data/routes'
import {
  CLIENTS,
  REQUIREMENTS,
  REQUIREMENTS_NOTE,
  SETUP_STEPS,
  VERIFY_CHECKS,
  VERIFY_COMMANDS,
  VERIFY_INTRO,
} from '~/data/setup'
import { ENDPOINTS, FACTS, SITE } from '~/data/site'

const route = routeByPath('/setup')

const mcpJsonSnippet = CLIENTS.find((c) => c.id === 'claude')?.snippet ?? ''

useRouteSeo({
  title: route.title,
  description: route.description,
  path: route.path,
  textTwin: route.twin,
  breadcrumb: route.nav ?? undefined,
  schema: [
    {
      '@context': 'https://schema.org',
      '@type': 'HowTo',
      name: 'Set up Aster: MCP server, Android companion and AI client',
      description: route.description,
      url: abs(route.path),
      inLanguage: 'en',
      totalTime: 'PT15M',
      estimatedCost: { '@type': 'MonetaryAmount', currency: FACTS.priceCurrency, value: FACTS.price },
      supply: [
        { '@type': 'HowToSupply', name: `An Android phone or tablet running ${FACTS.androidMinLabel}, no root required` },
        { '@type': 'HowToSupply', name: `A computer running Node.js ${FACTS.nodeRequirement}` },
      ],
      tool: [
        { '@type': 'HowToTool', name: `${FACTS.npmPackage} (npm)` },
        { '@type': 'HowToTool', name: 'The Aster Android companion app' },
        { '@type': 'HowToTool', name: 'An MCP client such as Claude Code, Claude Desktop, OpenClaw or AnythingLLM' },
      ],
      step: SETUP_STEPS.map((s, i) => ({
        '@type': 'HowToStep',
        position: i + 1,
        name: s.title,
        url: `${abs(route.path)}#step-${s.id}`,
        text: [s.body, s.command, s.note].filter(Boolean).join(' '),
      })),
    },
    {
      '@context': 'https://schema.org',
      '@type': 'SoftwareSourceCode',
      name: '.mcp.json entry for the Aster MCP server',
      description: `The streamable-HTTP MCP server entry that points Claude Code or Claude Desktop at Aster on ${ENDPOINTS.mcp}.`,
      programmingLanguage: 'JSON',
      codeSampleType: 'snippet',
      text: mcpJsonSnippet,
      url: `${abs(route.path)}#integrations`,
      license: 'https://opensource.org/licenses/MIT',
      about: {
        '@type': 'SoftwareApplication',
        name: SITE.name,
        applicationCategory: 'DeveloperApplication',
        operatingSystem: 'Android',
      },
    },
  ],
})
</script>

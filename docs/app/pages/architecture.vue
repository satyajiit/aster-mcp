<template>
  <article>
    <!-- Page head -->
    <section class="relative px-6 pt-20 pb-10 sm:pt-24">
      <div class="max-w-4xl mx-auto">
        <span class="text-[11px] font-mono uppercase tracking-[0.28em] text-aster mb-4 block">Architecture</span>
        <h1 class="text-4xl sm:text-5xl font-bold tracking-tight text-text-primary">
          How a command reaches the phone
        </h1>
        <p class="mt-5 text-base sm:text-lg text-text-secondary leading-relaxed max-w-2xl">
          {{ SITE.name }} is two pieces of software and one socket between them: an MCP server you
          run on your own machine, and an Android companion app that holds the permissions. This
          page traces a single command across both, then lays out the three transports a client can
          use to get to the phone and the ports each one occupies.
        </p>
        <p class="mt-4 text-[14px] text-text-tertiary leading-relaxed max-w-2xl">
          Nothing here goes through a hosted service. The server binds to your machine, the phone
          dials out to it, and the tool result goes back to your client. There is no account and no
          telemetry in the path.
        </p>
      </div>
    </section>

    <!-- Command trace + transports table -->
    <HowItWorks />

    <!-- Worked examples -->
    <section id="worked-examples" class="px-6 py-20 border-t border-border-dim">
      <div class="max-w-4xl mx-auto">
        <h2 class="text-3xl font-bold tracking-tight text-text-primary">Five commands, traced</h2>
        <p class="mt-4 mb-10 text-[15px] text-text-secondary leading-relaxed max-w-2xl">
          The same six hops, with the details filled in. Each of these is a real prompt, the tool it
          resolves to, and what comes back. Round-trip figures are from a phone and a server on the
          same network; a phone on mobile data is slower, and a voice call is dominated by the
          seconds the call itself takes to connect.
        </p>

        <ul class="space-y-6">
          <li
            v-for="ex in WORKED_EXAMPLES"
            :key="ex.id"
            :id="`example-${ex.id}`"
            class="rounded-2xl bg-surface-raised border border-border-dim overflow-hidden"
          >
            <div class="flex items-start gap-3 px-5 py-4 border-b border-border-dim">
              <Icon name="lucide:message-square-quote" class="text-base text-aster mt-0.5 flex-shrink-0" />
              <h3 class="text-[15px] font-semibold text-text-primary leading-snug">
                &ldquo;{{ ex.prompt }}&rdquo;
              </h3>
            </div>

            <ol class="px-5 py-4 space-y-3">
              <li
                v-for="(step, i) in ex.steps"
                :key="step.label"
                class="flex items-start gap-3"
              >
                <span class="font-mono text-[11px] text-text-tertiary tabular-nums mt-0.5 w-4 flex-shrink-0">
                  {{ i + 1 }}
                </span>
                <p class="text-[14px] text-text-secondary leading-relaxed">
                  <strong class="font-semibold text-text-primary">{{ step.label }}.</strong>
                  {{ step.detail }}
                </p>
              </li>
            </ol>

            <div class="px-5 pb-4 pt-1">
              <h4 class="text-[11px] font-mono uppercase tracking-wider text-text-tertiary mb-2">
                Tools involved
              </h4>
              <ul class="flex flex-wrap gap-2">
                <li
                  v-for="tool in ex.tools"
                  :key="tool"
                  class="font-mono text-[12px] text-aster bg-aster/10 border border-aster/20 rounded-md px-2 py-1"
                >
                  {{ tool }}
                </li>
              </ul>
            </div>
          </li>
        </ul>

        <p class="mt-8 text-[14px] text-text-secondary leading-relaxed max-w-2xl">
          The full catalogue of {{ TOOL_COUNTS.mcpServer }} tools, each with its arguments, is on the
          <NuxtLink :to="href('/tools')" class="text-aster underline underline-offset-4 hover:text-text-primary">tools page</NuxtLink>,
          and the separate {{ TOOL_COUNTS.onDevice }}-action on-device catalogue is
          <NuxtLink :to="`${href('/tools')}#on-device`" class="text-aster underline underline-offset-4 hover:text-text-primary">listed there too</NuxtLink>.
          What a phone will refuse to do, and why, is on the
          <NuxtLink :to="href('/security')" class="text-aster underline underline-offset-4 hover:text-text-primary">security page</NuxtLink>.
        </p>
      </div>
    </section>

    <!-- Ports -->
    <section id="ports" class="px-6 py-20 border-t border-border-dim">
      <div class="max-w-4xl mx-auto">
        <h2 class="text-3xl font-bold tracking-tight text-text-primary">Ports the server opens</h2>
        <p class="mt-4 mb-8 text-[15px] text-text-secondary leading-relaxed max-w-2xl">
          Running <code class="font-mono text-aster">aster start</code> binds three ports on the
          machine you run it on. The phone only needs to reach the first one; you only need to reach
          the other two. Open nothing to the public internet &mdash; put the link on your LAN or a
          private mesh.
        </p>

        <div
          :ref="(el) => (portsRegion.el.value = el as HTMLElement)"
          v-bind="portsRegion.attrs.value"
          class="overflow-x-auto rounded-xl border border-border-dim focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
        >
          <table class="w-full min-w-[34rem] text-left border-collapse">
            <caption class="sr-only">
              The three ports the Aster server binds, what listens on each, and how each is reached.
            </caption>
            <thead>
              <tr class="bg-surface-raised">
                <th scope="col" class="px-4 py-3 text-[11px] font-mono uppercase tracking-wider text-text-tertiary border-b border-border-dim">Port</th>
                <th scope="col" class="px-4 py-3 text-[11px] font-mono uppercase tracking-wider text-text-tertiary border-b border-border-dim">What listens</th>
                <th scope="col" class="px-4 py-3 text-[11px] font-mono uppercase tracking-wider text-text-tertiary border-b border-border-dim">What it is for</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="p in PORTS"
                :key="p.port"
                class="align-top border-b border-border-dim last:border-b-0"
              >
                <th scope="row" class="px-4 py-4 font-mono text-sm text-aster whitespace-nowrap">
                  {{ p.port }}
                </th>
                <td class="px-4 py-4 text-[14px] font-medium text-text-primary">{{ p.name }}</td>
                <td class="px-4 py-4 text-[13px] text-text-secondary leading-relaxed">{{ p.detail }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <h3 class="mt-10 text-lg font-semibold text-text-primary">Addresses in full</h3>
        <dl class="mt-4 divide-y divide-border-dim rounded-xl border border-border-dim bg-surface-raised">
          <div v-for="row in ENDPOINT_ROWS" :key="row.term" class="px-4 py-3 sm:flex sm:gap-6">
            <dt class="text-[13px] font-medium text-text-primary sm:w-52 sm:flex-shrink-0">{{ row.term }}</dt>
            <dd class="mt-1 sm:mt-0 font-mono text-[13px] text-text-secondary break-all">{{ row.def }}</dd>
          </div>
        </dl>

        <p class="mt-6 text-[14px] text-text-secondary leading-relaxed max-w-2xl">
          The on-device MCP server is the exception: it runs inside the app on the phone and listens
          on port {{ ON_DEVICE_PORT }} by default, so none of the three ports above exist in that mode. Binder IPC opens no
          port at all. Installing the server and pointing a client at it is covered on the
          <NuxtLink to="/setup/" class="text-aster underline underline-offset-4 hover:text-text-primary">setup page</NuxtLink>.
        </p>
      </div>
    </section>
  </article>
</template>

<script setup lang="ts">
import { WORKED_EXAMPLES } from '~/data/architecture'
import { routeByPath, href } from '~/data/routes'
import { ENDPOINTS, FACTS, LINKS, ON_DEVICE_PORT, PORTS, SITE, TOOL_COUNTS } from '~/data/site'

const portsRegion = useScrollableRegion('Ports the Aster server opens')

const route = routeByPath('/architecture')

/** Rendered as a <dl>: term/definition is what these are. */
const ENDPOINT_ROWS: { term: string; def: string }[] = [
  { term: 'MCP endpoint for clients', def: ENDPOINTS.mcp },
  { term: 'Health check', def: ENDPOINTS.health },
  { term: 'Web dashboard', def: ENDPOINTS.dashboard },
  { term: 'Device WebSocket', def: ENDPOINTS.deviceWs },
]

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
      headline: 'Architecture — how a command reaches the phone',
      description: route.description,
      url: abs(route.path),
      inLanguage: 'en',
      isAccessibleForFree: true,
      about: [
        { '@type': 'Thing', name: 'Model Context Protocol' },
        { '@type': 'Thing', name: 'Android accessibility automation' },
        { '@type': 'Thing', name: 'WebSocket transport' },
      ],
      proficiencyLevel: 'Expert',
      dependencies: `Node.js ${FACTS.nodeRequirement}, ${FACTS.androidMinLabel}`,
      // route.topics, not route.intent: `intent` is the authoring note about what
      // someone would ASK to land here, and publishing a list of questions as
      // schema.org keywords reads as query stuffing.
      keywords: route.topics.join(', '),
      author: { '@type': 'Person', name: SITE.author, url: SITE.authorUrl },
      publisher: { '@type': 'Organization', name: SITE.publisher, url: SITE.publisherUrl },
      mainEntityOfPage: { '@type': 'WebPage', '@id': abs(route.path) },
      image: SITE.ogImage,
      codeRepository: LINKS.repo,
    },
  ],
})
</script>

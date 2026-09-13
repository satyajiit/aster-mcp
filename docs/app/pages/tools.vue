<template>
  <section class="relative px-6 pt-24 pb-6">
    <div class="max-w-5xl mx-auto">
      <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Tool catalogue</span>
      <h1 class="text-4xl sm:text-5xl font-bold tracking-tight text-text-primary">
        All {{ TOOL_COUNTS.mcpServer }} Aster MCP tools
      </h1>
      <p class="mt-5 text-lg text-text-secondary max-w-2xl">
        This is the full set an MCP client sees when it connects to the Aster server over HTTP: every tool the AI can
        call on a paired {{ FACTS.androidMinLabel }} phone, across {{ TOOL_CATEGORIES.length }} categories, with the
        exact name it must send.
      </p>

      <div class="mt-8 max-w-2xl">
        <ACodeBlock :code="callExample" label="MCP tools/call" />
      </div>
    </div>
  </section>

  <ToolsShowcase />

  <section id="surfaces" class="relative px-6 py-20 border-t border-border-dim">
    <div class="max-w-5xl mx-auto">
      <h2 class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
        The on-device surface is a different set
      </h2>
      <p class="mt-4 text-text-secondary max-w-2xl">
        The {{ TOOL_COUNTS.mcpServer }} <code class="font-mono text-text-primary">{{ TOOL_PREFIX }}*</code> tools above
        belong to the Node server. When the phone runs its own MCP server, or when an agent on the same phone calls in
        over Binder IPC, it exposes {{ TOOL_COUNTS.onDevice }} unprefixed actions instead &mdash; a different catalogue,
        not a superset. {{ TOOL_COUNTS.shared }} of the on-device actions are reachable from an MCP client and
        {{ TOOL_COUNTS.onDeviceOnly }} are not ({{ TOOL_COUNTS.shared }} + {{ TOOL_COUNTS.onDeviceOnly }} =
        {{ TOOL_COUNTS.onDevice }}); going the other way exactly
        {{ TOOL_COUNTS.serverOnly }} server tool has no on-device action behind it &mdash;
        <code class="font-mono text-text-primary">{{ TOOL_PREFIX }}list_devices</code>, which only means anything when a
        server is brokering several phones.
        (<code class="font-mono text-text-primary">{{ TOOL_PREFIX }}click_by_id</code> looks like a second one, but it
        is just the server's name for the device action <code class="font-mono text-text-primary">click_by_view_id</code>.)
      </p>

      <dl class="mt-8 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div
          v-for="mode in CONNECTION_MODES"
          :key="mode.id"
          class="p-5 rounded-2xl bg-surface-raised border border-border-dim"
        >
          <dt class="text-sm font-semibold text-text-primary">
            {{ mode.name }}
            <span class="ml-2 align-middle text-xs font-medium uppercase tracking-wider text-text-tertiary">
              {{ mode.badge }}
            </span>
          </dt>
          <dd class="mt-2 text-sm text-text-secondary">
            {{ mode.summary }}
            <span class="mt-3 block font-mono text-sm text-aster">{{ mode.toolNamespace }}</span>
          </dd>
        </div>
      </dl>

      <p class="mt-8 text-text-secondary">
        <NuxtLink :to="href('/architecture')" class="text-aster underline underline-offset-4 hover:no-underline">
          How a command reaches the phone
        </NuxtLink>
        walks the three transports end to end, and explains which one you get by default.
      </p>
    </div>
  </section>

  <!-- The second catalogue, in full.

       The site insisted on three separate routes that the on-device set is a
       DIFFERENT set of actions, not a superset — and then never listed it, so
       "what can OpenAlly actually call over Binder IPC?" was a question the
       site raised and no page answered. The distinction only earns credibility
       once the other list is on the page. -->
  <section id="on-device" class="relative px-6 py-20 border-t border-border-dim">
    <div class="max-w-5xl mx-auto">
      <h2 class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
        All {{ TOOL_COUNTS.onDevice }} on-device actions
      </h2>
      <p class="mt-4 text-text-secondary max-w-2xl">
        Unprefixed, and dispatched by the phone itself &mdash; reachable two ways, both on the device: over Binder IPC
        from an app such as OpenAlly, or from the companion's own MCP server.
        {{ TOOL_COUNTS.shared }} of them are <em>also</em> reachable from a remote MCP client; the other
        {{ TOOL_COUNTS.onDeviceOnly }} are not. Each row below says which.
      </p>

      <div class="mt-10 grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-10">
        <section v-for="group in ON_DEVICE_CATEGORIES" :key="group.name" class="min-w-0">
          <h3 class="text-xs font-semibold uppercase tracking-[0.15em] text-aster">
            {{ group.name }}
            <span class="ml-2 text-text-tertiary">{{ group.actions.length }}</span>
          </h3>
          <ul class="mt-4 space-y-3">
            <li
              v-for="action in group.actions"
              :id="`od-${action.action}`"
              :key="action.action"
              class="scroll-mt-24 border-t border-border-dim pt-3"
            >
              <code class="block font-mono text-sm text-text-primary break-all">{{ action.action }}</code>
              <p class="mt-1 text-sm text-text-secondary">{{ action.summary }}</p>
              <p class="mt-1.5 text-xs">
                <span v-if="action.server" class="text-text-tertiary">
                  Also as <code class="font-mono text-aster">{{ action.server }}</code>
                </span>
                <span v-else class="text-amber-400">On-device only</span>
              </p>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { routeByPath, href } from '~/data/routes'
import { SITE, FACTS, TOOL_PREFIX, TOOL_COUNTS, CONNECTION_MODES } from '~/data/site'
import { TOOL_CATEGORIES, ALL_TOOLS } from '~/data/tools'
import { ON_DEVICE_CATEGORIES, ALL_ON_DEVICE_ACTIONS, ON_DEVICE_ONLY } from '~/data/on-device'

const route = routeByPath('/tools')

/** Shows the prefixed, callable name in the shape a client actually sends. */
const callExample = `{
  "method": "tools/call",
  "params": {
    "name": "${TOOL_PREFIX}take_screenshot",
    "arguments": { "deviceId": "<your-device-id>" }
  }
}`

useRouteSeo({
  title: route.title,
  description: route.description,
  path: route.path,
  textTwin: route.twin,
  breadcrumb: route.nav ?? undefined,
  schema: [
    {
      '@context': 'https://schema.org',
      '@type': 'ItemList',
      name: `Aster MCP tools (${TOOL_COUNTS.mcpServer})`,
      description: `Every tool the Aster MCP server registers, all namespaced ${TOOL_PREFIX}*.`,
      url: abs(route.path),
      numberOfItems: ALL_TOOLS.length,
      itemListOrder: 'https://schema.org/ItemListUnordered',
      itemListElement: ALL_TOOLS.map((tool, i) => ({
        '@type': 'ListItem',
        position: i + 1,
        url: `${abs(route.path)}#tool-${TOOL_PREFIX}${tool.name}`,
        name: `${TOOL_PREFIX}${tool.name}`,
        description: tool.args.length
          ? `${tool.summary} Arguments: ${tool.args.map((a) => `${a.name}${a.required ? '' : ' (optional)'}`).join(', ')}.`
          : `${tool.summary} Takes no arguments.`,
      })),
    },
    {
      // The other catalogue, as its own list. Named and counted on three routes
      // before this existed, which made the claim uncheckable.
      '@context': 'https://schema.org',
      '@type': 'ItemList',
      name: `Aster on-device actions (${TOOL_COUNTS.onDevice})`,
      description: `Unprefixed actions the Aster Android companion dispatches itself, over Binder IPC or its own on-device MCP server. A different catalogue from the ${TOOL_COUNTS.mcpServer} ${TOOL_PREFIX}* tools, and neither contains the other: ${TOOL_COUNTS.shared} of these ${TOOL_COUNTS.onDevice} actions are also reachable from a remote MCP client and ${ON_DEVICE_ONLY.length} are not.`,
      url: `${abs(route.path)}#on-device`,
      numberOfItems: ALL_ON_DEVICE_ACTIONS.length,
      itemListOrder: 'https://schema.org/ItemListUnordered',
      itemListElement: ALL_ON_DEVICE_ACTIONS.map((action, i) => ({
        '@type': 'ListItem',
        position: i + 1,
        url: `${abs(route.path)}#od-${action.action}`,
        name: action.action,
        // The summaries come from ToolCatalog.kt and do not end in a full stop,
        // so join with one rather than running two sentences together.
        description: action.server
          ? `${action.summary.replace(/\.?$/, '.')} Reachable remotely as ${action.server}.`
          : `${action.summary.replace(/\.?$/, '.')} On-device only — no MCP tool reaches it.`,
      })),
    },
    {
      '@context': 'https://schema.org',
      '@type': 'TechArticle',
      headline: route.title,
      description: route.description,
      url: abs(route.path),
      inLanguage: 'en',
      image: SITE.ogImage,
      about: TOOL_CATEGORIES.map((c) => ({ '@type': 'Thing', name: c.name })),
      author: { '@type': 'Person', name: SITE.author, url: SITE.authorUrl },
      publisher: { '@type': 'Organization', name: SITE.publisher, url: SITE.publisherUrl },
      isPartOf: { '@type': 'WebSite', name: SITE.name, url: abs('/') },
    },
  ],
})
</script>

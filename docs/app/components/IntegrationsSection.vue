<template>
  <section id="integrations" class="relative py-20 sm:py-24 px-6 scroll-mt-20">
    <div class="max-w-3xl mx-auto">
      <div class="mb-10">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-3 block">Clients</span>
        <h2 class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
          Connecting your AI client
        </h2>
        <p class="mt-3 text-text-secondary max-w-2xl">
          Aster speaks standard MCP, so there is no Aster-specific plugin to install anywhere.
          Three of these four paths are the same endpoint written in a different file; the
          fourth needs no endpoint at all.
        </p>
      </div>

      <ul class="space-y-5">
        <li v-for="client in CLIENTS" :key="client.id">
          <ACard variant="hero" class="!p-6">
            <div class="flex items-start gap-4">
              <!-- The real mark wins over a generic tile where we own one. The
                   component carries role="img" aria-label="OpenAlly" of its own, so
                   aria-hidden is essential here: the client's name is the <h3> right
                   beside it and a screen reader would otherwise say it twice. -->
              <OpenAllyMark v-if="client.brand === 'openally'" :size="40" aria-hidden="true" />
              <AIconTile v-else :icon="client.icon" :accent="client.accent" :size="40" aria-hidden="true" />
              <div class="min-w-0 flex-1">
                <h3 class="text-base font-semibold text-text-primary">{{ client.name }}</h3>
                <p class="mt-1.5 text-sm text-text-secondary leading-relaxed">{{ client.body }}</p>

                <ACodeBlock
                  v-if="client.snippet"
                  class="mt-4"
                  :code="client.snippet"
                  :label="labelFor(client)"
                />

                <p v-if="client.href" class="mt-4">
                  <a
                    :href="client.href"
                    target="_blank"
                    rel="noopener"
                    class="inline-flex items-center gap-1.5 px-3 py-2 -mx-1 rounded-lg text-sm font-medium text-aster hover:bg-aster/10 transition-colors"
                  >
                    {{ linkLabel(client) }}
                    <Icon name="lucide:arrow-up-right" aria-hidden="true" />
                  </a>
                </p>
              </div>
            </div>
          </ACard>
        </li>
      </ul>

      <p class="mt-8 text-sm text-text-tertiary">
        Every path above reaches the same {{ TOOL_COUNTS.mcpServer }} server-side tools except
        OpenAlly, which talks to the phone directly and therefore sees the on-device catalog
        instead. The server itself is on npm as
        <a :href="LINKS.npm" target="_blank" rel="noopener" class="text-aster underline underline-offset-2">{{ FACTS.npmPackage }}</a>.
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The client matrix for /setup.
 *
 * The previous version hard-coded a raw ClawHub SKILL.md URL under a
 * capital-Aster repo path as one of only two documented install paths for
 * OpenClaw-family clients. No such repo exists — the repo is `aster-mcp` — so
 * that URL was a hard 404. The working one lives in LINKS.skillRaw, which is
 * now the only place it is written down; do not re-spell it here.
 */
import { CLIENTS, type ClientDef } from '~/data/setup'
import { FACTS, LINKS, TOOL_COUNTS } from '~/data/site'

function labelFor(client: ClientDef): string {
  if (client.lang === 'json') return '.mcp.json'
  if (client.lang === 'text') return 'endpoint'
  return 'terminal'
}

function linkLabel(client: ClientDef): string {
  switch (client.id) {
    case 'openally':
      return 'OpenAlly.ai'
    case 'openclaw':
      return 'The Aster skill on ClawHub'
    default:
      return client.name
  }
}
</script>

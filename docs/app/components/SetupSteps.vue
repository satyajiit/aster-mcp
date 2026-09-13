<template>
  <section id="setup" class="relative py-20 sm:py-24 px-6 scroll-mt-20">
    <div class="max-w-3xl mx-auto">
      <div class="mb-12">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-3 block">Install</span>
        <h2 class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary">
          Six steps, start to first tool call
        </h2>
        <p class="mt-3 text-text-secondary max-w-2xl">
          The server runs on your machine, the companion runs on the phone, and your AI client
          talks to the server. Do them in this order. Step five is the one people skip: an
          unapproved device connects happily and then times out on every single tool call.
        </p>
      </div>

      <ol class="space-y-10">
        <SetupStep
          v-for="(step, i) in SETUP_STEPS"
          :id="step.id"
          :key="step.id"
          :number="i + 1"
          :title="step.title"
          :body="step.body"
          :note="step.note"
          :is-last="i === SETUP_STEPS.length - 1"
        >
          <ACodeBlock v-if="step.command" class="mt-4" :code="step.command" :label="labelFor(step.id)" />

          <p v-if="step.id === 'install-app'" class="mt-4">
            <a
              :href="LINKS.releases"
              target="_blank"
              rel="noopener"
              class="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-surface-raised border border-border-subtle text-sm font-medium text-text-primary hover:border-aster transition-colors"
            >
              <Icon name="lucide:download" aria-hidden="true" />
              Download the APK from GitHub Releases
            </a>
          </p>

          <p v-if="step.id === 'connect-client'" class="mt-3 text-sm text-text-secondary">
            Per-client instructions —
            <a href="#integrations" class="text-aster underline underline-offset-2">Claude, OpenClaw, AnythingLLM and OpenAlly</a>
            — are below.
          </p>
        </SetupStep>
      </ol>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The install procedure, as a real <ol>.
 *
 * It used to be a stack of divs holding v-html'd terminal markup, which meant
 * the page shipped no extractable procedure at all — no list element, and the
 * commands were unselectable spans rather than copyable code. Every command now
 * goes through ACodeBlock from the shared aster-ui layer, which carries a copy
 * button.
 */
import { SETUP_STEPS } from '~/data/setup'
import { LINKS } from '~/data/site'

/** The one step whose "command" is a config file rather than a shell line. */
function labelFor(id: string): string {
  return id === 'connect-client' ? '.mcp.json' : 'terminal'
}
</script>

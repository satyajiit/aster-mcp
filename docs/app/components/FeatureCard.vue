<template>
  <li class="group relative p-6 rounded-2xl bg-surface-raised border border-border-dim hover:border-border-subtle transition-colors duration-300">
    <div class="absolute inset-0 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500 bg-gradient-to-br from-aster/[0.03] to-transparent pointer-events-none" aria-hidden="true" />

    <div class="relative">
      <div class="w-10 h-10 rounded-xl bg-gradient-to-br flex items-center justify-center mb-4" :class="accent">
        <Icon :name="icon" class="text-lg text-surface" aria-hidden="true" />
      </div>
      <h3 class="text-base font-semibold text-text-primary mb-2">{{ title }}</h3>
      <p class="text-sm text-text-secondary leading-relaxed">
        <template v-for="(part, i) in parts" :key="i">
          <code v-if="part.code" class="font-mono text-[0.8125rem] text-aster break-words">{{ part.text }}</code>
          <template v-else>{{ part.text }}</template>
        </template>
      </p>
    </div>
  </li>
</template>

<script setup lang="ts">
import { TOOL_PREFIX } from '~/data/site'

const props = defineProps<{
  icon: string
  title: string
  description: string
  accent: string
}>()

/**
 * Tool names in the copy are set as <code>. They are printed with the aster_
 * prefix because that is the name an MCP client actually calls — the grid used
 * to print them bare, so none of them was callable as written.
 */
const TOOL_NAME = new RegExp(`(${TOOL_PREFIX}[a-z0-9_]+)`, 'g')

const parts = computed(() =>
  props.description
    .split(TOOL_NAME)
    .filter((chunk) => chunk.length > 0)
    .map((chunk) => ({ text: chunk, code: chunk.startsWith(TOOL_PREFIX) })),
)
</script>

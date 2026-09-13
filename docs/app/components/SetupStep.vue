<template>
  <li :id="`step-${id}`" class="relative flex gap-4 sm:gap-5 scroll-mt-24">
    <!-- Decorative. The position is already conveyed by the enclosing <ol>. -->
    <div class="flex-shrink-0 relative" aria-hidden="true">
      <div class="w-12 h-12 rounded-xl bg-aster/10 border border-aster/20 flex items-center justify-center">
        <span class="text-sm font-bold text-aster font-mono">{{ number }}</span>
      </div>
      <div v-if="!isLast" class="step-line" />
    </div>

    <div class="flex-1 min-w-0 pb-2">
      <h3 class="text-lg font-semibold text-text-primary mb-1.5">{{ title }}</h3>
      <p class="text-sm text-text-secondary leading-relaxed">{{ body }}</p>

      <slot />

      <p v-if="note" class="mt-3 flex gap-2 text-sm text-text-tertiary leading-relaxed">
        <Icon name="lucide:info" class="mt-0.5 flex-shrink-0 text-aster" aria-hidden="true" />
        <span>{{ note }}</span>
      </p>
    </div>
  </li>
</template>

<script setup lang="ts">
/**
 * One row of the /setup procedure. Renders as an <li> and is only valid inside
 * the <ol> in SetupSteps.vue — the number tile is decorative and marked
 * aria-hidden, because the list itself already announces the position.
 */
defineProps<{
  /** SetupStepDef.id. Becomes the `#step-<id>` anchor the JSON-LD points at. */
  id: string
  number: number
  title: string
  body: string
  note?: string
  isLast: boolean
}>()
</script>

<template>
  <div class="relative flex gap-5">
    <!-- Step indicator -->
    <div class="flex-shrink-0 relative">
      <div class="w-12 h-12 rounded-xl bg-aster/10 border border-aster/20 flex items-center justify-center">
        <span class="text-sm font-bold text-aster font-mono">{{ number }}</span>
      </div>
      <div v-if="!isLast" class="step-line" />
    </div>

    <!-- Content -->
    <div class="flex-1 pb-2">
      <h3 class="text-lg font-semibold text-text-primary mb-1">{{ title }}</h3>
      <p class="text-sm text-text-secondary leading-relaxed">{{ description }}</p>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  number: number
  title: string
  description: string
  isLast: boolean
}>()
</script>

<template>
  <div class="group relative p-6 rounded-2xl bg-surface-raised border border-border-dim hover:border-border-subtle transition-all duration-300">
    <!-- Subtle glow on hover -->
    <div class="absolute inset-0 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500 bg-gradient-to-br from-aster/[0.03] to-transparent" />

    <div class="relative">
      <div class="w-10 h-10 rounded-xl bg-gradient-to-br flex items-center justify-center mb-4" :class="accent">
        <Icon :name="icon" class="text-lg text-surface" />
      </div>
      <h3 class="text-base font-semibold text-text-primary mb-2">{{ title }}</h3>
      <p class="text-sm text-text-secondary leading-relaxed">{{ description }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon: string
  title: string
  description: string
  accent: string
}>()
</script>

<script setup lang="ts">
defineProps<{
  label: string;
  value: number | string;
  icon?: string;
  color?: 'cyan' | 'emerald' | 'amber' | 'violet' | 'rose' | 'blue';
}>();
</script>

<template>
  <div class="stat-card" :class="`stat-card--${color || 'cyan'}`">
    <div class="flex items-start justify-between gap-4">
      <div class="flex-1">
        <div class="stat-card-label mb-3">
          {{ label }}
        </div>
        <div class="stat-card-value">
          {{ value }}
        </div>
      </div>
      <div v-if="icon" class="stat-card-icon">
        {{ icon }}
      </div>
    </div>
    <div class="stat-card-accent"></div>
  </div>
</template>

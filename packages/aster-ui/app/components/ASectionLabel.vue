<script setup lang="ts">
/**
 * Uppercase letterspaced section header with an optional count pill.
 * Mirrors AsterSectionHeader in the Android app.
 */
defineProps<{ label: string; count?: number | null }>();
</script>

<template>
  <div class="section-label">
    <h2 class="section-label__text">{{ label }}</h2>
    <span v-if="count != null" class="section-label__count">{{ count }}</span>
    <div class="section-label__actions"><slot name="actions" /></div>
  </div>
</template>

<style scoped>
.section-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.section-label__text {
  margin: 0;
  text-transform: uppercase;
  font-size: var(--text-label-lg);
  line-height: var(--text-label-lg--line-height);
  letter-spacing: 0.08em;
  font-weight: 600;
  color: var(--color-fg-subtle);
}

.section-label__count {
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-lg);
  background: var(--color-primary);
  color: var(--color-bg);
  font-size: var(--text-label-sm);
  font-weight: 700;
  padding: 0.125rem 0.5rem;
}

.section-label__actions {
  margin-left: auto;
}
</style>

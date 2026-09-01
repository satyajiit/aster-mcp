<script setup lang="ts">
/** Mirrors AsterStatCard: 12px card, 40px icon tile, headline value, muted label. */
withDefaults(
  defineProps<{
    label: string;
    value: string | number;
    icon?: string;
    accent?: string;
    hint?: string | null;
    to?: string | null;
  }>(),
  { accent: 'var(--color-primary)', hint: null, to: null },
);
</script>

<template>
  <component :is="to ? resolveComponent('NuxtLink') : 'div'" :to="to ?? undefined" class="stat">
    <ACard :interactive="!!to">
      <div class="stat__row">
        <AIconTile v-if="icon" :icon="icon" :accent="accent" />
        <div class="stat__body">
          <div class="stat__value">{{ value }}</div>
          <div class="stat__label">{{ label }}</div>
          <div v-if="hint" class="stat__hint">{{ hint }}</div>
        </div>
      </div>
    </ACard>
  </component>
</template>

<style scoped>
.stat {
  display: block;
  text-decoration: none;
  color: inherit;
}

.stat__row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stat__body {
  min-width: 0;
}

.stat__value {
  font-size: var(--text-headline-md);
  line-height: var(--text-headline-md--line-height);
  font-weight: 700;
  color: var(--color-fg);
}

.stat__label {
  font-size: var(--text-label-md);
  line-height: var(--text-label-md--line-height);
  letter-spacing: var(--text-label-md--letter-spacing);
  color: var(--color-fg-subtle);
}

.stat__hint {
  margin-top: 0.125rem;
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}
</style>

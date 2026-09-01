<script setup lang="ts">
/**
 * The base surface. Mirrors AsterCard in the Android app: 12px radius,
 * surface-1 fill, 1px border, 16px padding, and deliberately NO shadow —
 * depth is a surface step plus a border.
 */
withDefaults(
  defineProps<{
    /** `hero` is the 16px-radius variant used for mode/feature cards. */
    variant?: 'default' | 'hero' | 'flush';
    /** Tints the border and adds a soft inner wash, for "active" states. */
    accent?: string | null;
    interactive?: boolean;
  }>(),
  { variant: 'default', accent: null, interactive: false },
);
</script>

<template>
  <div
    class="card"
    :class="[`card--${variant}`, { 'card--interactive': interactive, 'card--accented': accent }]"
    :style="accent ? { '--card-accent': accent } : undefined"
  >
    <slot />
  </div>
</template>

<style scoped>
.card {
  position: relative;
  border-radius: var(--radius-card);
  background: var(--color-surface-1);
  border: 1px solid var(--color-border);
  padding: 1rem;
}

.card--hero {
  border-radius: var(--radius-hero);
}

.card--flush {
  padding: 0;
  overflow: hidden;
}

.card--accented {
  border-color: color-mix(in oklab, var(--card-accent) 45%, var(--color-border));
}

.card--accented::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--card-accent);
  opacity: 0.06;
  pointer-events: none;
}

.card--interactive {
  cursor: pointer;
  transition:
    border-color var(--dur-base) var(--ease-out-soft),
    background-color var(--dur-base) var(--ease-out-soft);
}

.card--interactive:hover {
  border-color: var(--color-border-bright);
  background: var(--color-surface-2);
}

.card--interactive:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
</style>

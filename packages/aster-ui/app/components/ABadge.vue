<script setup lang="ts">
/**
 * Small pill. `solid` matches the Android BadgeItem (solid fill, white bold
 * label); `soft` is the tinted variant used for counts and tags.
 */
withDefaults(
  defineProps<{
    tone?: 'neutral' | 'primary' | 'success' | 'warning' | 'error' | 'info';
    variant?: 'soft' | 'solid' | 'outline';
    color?: string | null;
  }>(),
  { tone: 'neutral', variant: 'soft', color: null },
);

const TONE_COLORS: Record<string, string> = {
  neutral: 'var(--color-fg-subtle)',
  primary: 'var(--color-primary)',
  success: 'var(--color-success)',
  warning: 'var(--color-warning)',
  error: 'var(--color-error)',
  info: 'var(--color-info)',
};
</script>

<template>
  <span
    class="badge"
    :class="`badge--${variant}`"
    :style="{ '--badge-color': color ?? TONE_COLORS[tone] }"
  >
    <slot />
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  border-radius: var(--radius-sm);
  padding: 0.125rem 0.5rem;
  font-size: var(--text-label-sm);
  line-height: var(--text-label-sm--line-height);
  letter-spacing: var(--text-label-sm--letter-spacing);
  font-weight: 600;
  white-space: nowrap;
}

.badge--soft {
  background: color-mix(in oklab, var(--badge-color) 14%, transparent);
  color: var(--badge-color);
}

.badge--solid {
  background: var(--badge-color);
  color: #fff;
}

.badge--outline {
  border: 1px solid color-mix(in oklab, var(--badge-color) 30%, transparent);
  background: var(--color-surface-2);
  color: var(--color-fg-subtle);
}
</style>

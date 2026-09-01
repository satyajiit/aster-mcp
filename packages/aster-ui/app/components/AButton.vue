<script setup lang="ts">
/**
 * Mirrors AsterButton in the Android app: 12px radius, 48px min-height,
 * 24px horizontal padding, label-lg SemiBold.
 *
 * The primary variant reproduces the Compose `drawBehind` glow — a soft circle
 * of the accent at 15% alpha centred on the bottom edge.
 */
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
    size?: 'md' | 'sm';
    icon?: string;
    loading?: boolean;
    disabled?: boolean;
    type?: 'button' | 'submit';
  }>(),
  { variant: 'secondary', size: 'md', loading: false, disabled: false, type: 'button' },
);
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[`btn--${variant}`, `btn--${size}`]"
    :disabled="disabled || loading"
  >
    <ASpinner v-if="loading" :size="size === 'sm' ? 14 : 16" class="btn__spinner" />
    <Icon v-else-if="icon" :name="icon" class="btn__icon" />
    <span class="btn__label"><slot /></span>
  </button>
</template>

<style scoped>
.btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border-radius: var(--radius-card);
  border: 1px solid transparent;
  font-family: inherit;
  font-size: var(--text-label-lg);
  line-height: var(--text-label-lg--line-height);
  font-weight: 600;
  cursor: pointer;
  overflow: hidden;
  transition:
    background-color var(--dur-fast) var(--ease-out-soft),
    border-color var(--dur-fast) var(--ease-out-soft),
    opacity var(--dur-fast) var(--ease-out-soft);
}

.btn--md {
  min-height: 48px;
  padding: 0 1.5rem;
}

.btn--sm {
  min-height: 36px;
  padding: 0 0.875rem;
  font-size: var(--text-label-md);
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Primary — accent fill, background-coloured label, bottom-edge glow. */
.btn--primary {
  background: var(--color-primary);
  color: var(--color-bg);
}

.btn--primary::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 120%;
  aspect-ratio: 1;
  transform: translate(-50%, 50%);
  background: radial-gradient(
    circle,
    color-mix(in oklab, var(--color-primary) 15%, transparent) 0%,
    transparent 70%
  );
  pointer-events: none;
}

.btn--primary:hover:not(:disabled) {
  background: color-mix(in oklab, var(--color-primary) 88%, white);
}

.btn--secondary {
  background: var(--color-surface-2);
  border-color: var(--color-border);
  color: var(--color-fg);
}

.btn--secondary:hover:not(:disabled) {
  background: var(--color-surface-3);
  border-color: var(--color-border-bright);
}

.btn--danger {
  background: var(--color-error);
  color: #fff;
}

.btn--danger:hover:not(:disabled) {
  background: var(--color-error-dim);
}

.btn--ghost {
  background: transparent;
  color: var(--color-fg-subtle);
}

.btn--ghost:hover:not(:disabled) {
  background: var(--color-surface-2);
  color: var(--color-fg);
}

.btn__icon {
  font-size: 1.125em;
  flex: none;
}

.btn__label {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.btn__label:empty {
  display: none;
}
</style>

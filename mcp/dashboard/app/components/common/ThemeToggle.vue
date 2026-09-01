<script setup lang="ts">
const { scheme, set } = useColorScheme();

const OPTIONS = [
  { value: 'dark' as const, icon: 'ph:moon', label: 'Dark' },
  { value: 'light' as const, icon: 'ph:sun', label: 'Light' },
  { value: 'system' as const, icon: 'ph:circle-half', label: 'System' },
];
</script>

<template>
  <div class="themes" role="group" aria-label="Colour scheme">
    <button
      v-for="opt in OPTIONS"
      :key="opt.value"
      type="button"
      class="themes__btn"
      :class="{ 'themes__btn--active': scheme === opt.value }"
      :aria-pressed="scheme === opt.value"
      :title="opt.label"
      @click="set(opt.value)"
    >
      <Icon :name="opt.icon" />
      <span class="sr-only">{{ opt.label }}</span>
    </button>
  </div>
</template>

<style scoped>
.themes {
  display: inline-flex;
  gap: 0.125rem;
  padding: 0.125rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
}

.themes__btn {
  border: none;
  background: none;
  color: var(--color-fg-muted);
  cursor: pointer;
  padding: 0.3125rem;
  border-radius: var(--radius-sm);
  font-size: 1rem;
  display: inline-flex;
}

.themes__btn:hover {
  color: var(--color-fg);
}

.themes__btn--active {
  background: var(--color-surface-3);
  color: var(--color-primary);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
}
</style>

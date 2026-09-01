<script setup lang="ts">
const model = defineModel<boolean>({ default: false });
defineProps<{ label?: string; description?: string; disabled?: boolean }>();
</script>

<template>
  <label class="toggle" :class="{ 'toggle--disabled': disabled }">
    <input v-model="model" type="checkbox" class="toggle__input" :disabled="disabled" />
    <span class="toggle__track"><span class="toggle__thumb" /></span>
    <span v-if="label || description" class="toggle__text">
      <span v-if="label" class="toggle__label">{{ label }}</span>
      <span v-if="description" class="toggle__desc">{{ description }}</span>
    </span>
  </label>
</template>

<style scoped>
.toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.625rem;
  cursor: pointer;
}

.toggle--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle__input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle__track {
  position: relative;
  width: 42px;
  height: 24px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  transition: background-color var(--dur-fast) var(--ease-out-soft);
  flex: none;
}

.toggle__thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--color-fg-muted);
  transition:
    transform var(--dur-base) var(--ease-out-soft),
    background-color var(--dur-fast) var(--ease-out-soft);
}

.toggle__input:checked + .toggle__track {
  background: color-mix(in oklab, var(--color-primary) 35%, transparent);
  border-color: var(--color-primary);
}

.toggle__input:checked + .toggle__track .toggle__thumb {
  transform: translateX(18px);
  background: var(--color-primary);
}

.toggle__input:focus-visible + .toggle__track {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.toggle__text {
  display: flex;
  flex-direction: column;
}

.toggle__label {
  font-size: var(--text-body-md);
  color: var(--color-fg);
}

.toggle__desc {
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
}
</style>

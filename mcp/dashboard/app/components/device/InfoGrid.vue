<script setup lang="ts">
export interface InfoItem {
  label: string;
  value: string | number | null | undefined;
  mono?: boolean;
  icon?: string;
}

defineProps<{ items: InfoItem[] }>();
</script>

<template>
  <dl class="ig">
    <div v-for="item in items" :key="item.label" class="ig__row">
      <dt class="ig__label">
        <Icon v-if="item.icon" :name="item.icon" />
        {{ item.label }}
      </dt>
      <dd class="ig__value" :class="{ mono: item.mono }">
        {{ item.value === null || item.value === undefined || item.value === '' ? '—' : item.value }}
      </dd>
    </div>
  </dl>
</template>

<style scoped>
.ig {
  margin: 0;
  display: grid;
  gap: 1px;
  background: var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.ig__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-surface-1);
}

.ig__label {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
  flex: none;
}

.ig__value {
  margin: 0;
  font-size: var(--text-body-md);
  color: var(--color-fg);
  text-align: right;
  overflow-wrap: anywhere;
}
</style>

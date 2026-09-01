<script setup lang="ts">
/** Common chrome for a device panel: title, actions, loading and error state. */
defineProps<{
  title: string;
  description?: string;
  loading?: boolean;
  error?: string | null;
  empty?: boolean;
  emptyTitle?: string;
  emptyIcon?: string;
}>();
</script>

<template>
  <ACard>
    <div class="head">
      <div>
        <h2 class="head__title">{{ title }}</h2>
        <p v-if="description" class="head__desc">{{ description }}</p>
      </div>
      <div class="head__actions"><slot name="actions" /></div>
    </div>

    <slot name="controls" />

    <div v-if="loading" class="state"><ASpinner /> Working…</div>
    <AEmptyState
      v-else-if="error"
      icon="ph:warning-circle"
      tone="error"
      title="Request failed"
      :description="error"
    />
    <AEmptyState
      v-else-if="empty"
      :icon="emptyIcon ?? 'ph:tray'"
      :title="emptyTitle ?? 'Nothing to show yet'"
    />
    <slot v-else />
  </ACard>
</template>

<style scoped>
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.head__title {
  margin: 0;
  font-size: var(--text-title-lg);
  font-weight: 600;
}

.head__desc {
  margin: 0.125rem 0 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
}

.head__actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}

.state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: center;
  padding: 2rem;
  color: var(--color-fg-subtle);
}
</style>

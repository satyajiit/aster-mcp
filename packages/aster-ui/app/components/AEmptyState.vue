<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string;
    description?: string | null;
    icon?: string;
    tone?: 'neutral' | 'error';
  }>(),
  { description: null, icon: 'ph:tray', tone: 'neutral' },
);
</script>

<template>
  <div class="empty" :class="`empty--${tone}`">
    <AIconTile
      :icon="icon"
      :size="56"
      :accent="tone === 'error' ? 'var(--color-error)' : 'var(--color-fg-muted)'"
    />
    <p class="empty__title">{{ title }}</p>
    <p v-if="description" class="empty__desc">{{ description }}</p>
    <div class="empty__action"><slot /></div>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 2.5rem 1.5rem;
  gap: 0.5rem;
}

.empty__title {
  margin: 0.5rem 0 0;
  font-size: var(--text-title-md);
  font-weight: 600;
  color: var(--color-fg);
}

.empty__desc {
  margin: 0;
  max-width: 34ch;
  font-size: var(--text-body-md);
  line-height: var(--text-body-md--line-height);
  color: var(--color-fg-subtle);
}

.empty__action:empty {
  display: none;
}

.empty__action {
  margin-top: 0.75rem;
}
</style>

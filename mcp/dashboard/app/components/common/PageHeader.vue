<script setup lang="ts">
/** Replaces the four competing page-header implementations. */
defineProps<{
  title: string;
  description?: string;
  backTo?: string;
  backLabel?: string;
}>();
</script>

<template>
  <header class="ph">
    <div class="ph__lead">
      <NuxtLink v-if="backTo" :to="backTo" class="ph__back">
        <Icon name="ph:arrow-left" />
        {{ backLabel ?? 'Back' }}
      </NuxtLink>
      <div class="ph__titles">
        <h1 class="ph__title">{{ title }}</h1>
        <p v-if="description" class="ph__desc">{{ description }}</p>
      </div>
    </div>
    <div class="ph__actions"><slot name="actions" /></div>
  </header>
</template>

<style scoped>
.ph {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.ph__lead {
  min-width: 0;
}

.ph__back {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--color-fg-subtle);
  text-decoration: none;
  font-size: var(--text-label-md);
  margin-bottom: 0.375rem;
}

.ph__back:hover {
  color: var(--color-primary);
}

.ph__title {
  margin: 0;
  font-size: var(--text-headline-lg);
  line-height: var(--text-headline-lg--line-height);
  letter-spacing: var(--text-headline-lg--letter-spacing);
  font-weight: 700;
  color: var(--color-fg);
}

.ph__desc {
  margin: 0.25rem 0 0;
  font-size: var(--text-body-md);
  color: var(--color-fg-subtle);
}

.ph__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
</style>

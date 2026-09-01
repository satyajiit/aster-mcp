<script setup lang="ts">
/**
 * An invitation to star the repo, not a nag: dismissible, and the dismissal
 * persists so it does not come back on the next visit.
 */
const REPO = 'https://github.com/satyajiit/aster-mcp';
const STORAGE_KEY = 'aster-star-dismissed';

const dismissed = ref(true);

onMounted(() => {
  dismissed.value = localStorage.getItem(STORAGE_KEY) === '1';
});

function dismiss() {
  dismissed.value = true;
  localStorage.setItem(STORAGE_KEY, '1');
}

// Starring is the point, so treat opening the repo as intent and stop asking.
function acknowledge() {
  localStorage.setItem(STORAGE_KEY, '1');
}
</script>

<template>
  <AAnimatedEntrance v-if="!dismissed" :delay="240">
    <ACard class="star" accent="var(--color-warning)">
      <div class="star__row">
        <AIconTile icon="ph:star" :size="44" accent="var(--color-warning)" />

        <div class="star__body">
          <p class="star__title">Enjoying Aster?</p>
          <p class="star__desc">
            Aster is free and open source. A star on GitHub helps other people find it.
          </p>
        </div>

        <div class="star__actions">
          <a :href="REPO" target="_blank" rel="noopener" @click="acknowledge">
            <AButton variant="primary" size="sm" icon="ph:star">Star on GitHub</AButton>
          </a>
          <AButton variant="ghost" size="sm" @click="dismiss">Not now</AButton>
        </div>
      </div>
    </ACard>
  </AAnimatedEntrance>
</template>

<style scoped>
.star {
  margin-bottom: 1.75rem;
}

.star__row {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  flex-wrap: wrap;
}

.star__body {
  flex: 1;
  min-width: 14rem;
}

.star__title {
  margin: 0;
  font-size: var(--text-title-md);
  font-weight: 600;
  color: var(--color-fg);
}

.star__desc {
  margin: 0.125rem 0 0;
  font-size: var(--text-body-sm);
  color: var(--color-fg-subtle);
}

.star__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.star__actions a {
  text-decoration: none;
}
</style>

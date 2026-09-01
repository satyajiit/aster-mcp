<script setup lang="ts">
/**
 * The co-branded lockup: Aster, by OpenAlly(TM).
 *
 * Mirrors the Android launcher label, which is already "Aster by OpenAlly"
 * (apps/android/app/src/main/res/values/strings.xml).
 *
 * The (TM) rides brand surfaces only — app bar, footer, About. Per the rule
 * stated in OpenAllyWeb/app/components/shared/Logo.tsx it must never appear on
 * buttons, in metadata, in alt text, or in error copy.
 */
withDefaults(
  defineProps<{
    /** `full` shows the tagline beneath; `compact` is the app-bar form. */
    variant?: 'full' | 'compact';
    /** Tagline under the wordmark. Only rendered by `full`. */
    tagline?: string;
    /** Suppress the endorsement line where the lockup sits next to one already. */
    showEndorsement?: boolean;
  }>(),
  {
    variant: 'compact',
    tagline: 'Android Device Controller',
    showEndorsement: true,
  },
);
</script>

<template>
  <div class="lockup" :class="`lockup--${variant}`">
    <AsterMark :size="variant === 'full' ? 34 : 26" class="lockup__bolt" />

    <div class="lockup__type">
      <div class="lockup__row">
        <span class="lockup__word">Aster</span>
        <span v-if="showEndorsement" class="lockup__by">
          <span class="lockup__by-label">by</span>
          <OpenAllyMark :size="variant === 'full' ? 18 : 15" />
          <span class="lockup__openally">
            OpenAlly<sup class="lockup__tm">&trade;</sup>
          </span>
        </span>
      </div>
      <p v-if="variant === 'full' && tagline" class="lockup__tagline">{{ tagline }}</p>
    </div>
  </div>
</template>

<style scoped>
.lockup {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  min-width: 0;
}

.lockup__type {
  min-width: 0;
}

.lockup__row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.lockup__word {
  font-size: var(--text-headline-sm);
  line-height: var(--text-headline-sm--line-height);
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--color-fg);
}

.lockup--full .lockup__word {
  font-size: var(--text-headline-lg);
  line-height: var(--text-headline-lg--line-height);
}

.lockup__by {
  display: inline-flex;
  align-items: center;
  gap: 0.3125rem;
  color: var(--color-fg-subtle);
}

.lockup__by-label {
  font-size: var(--text-label-md);
  letter-spacing: var(--text-label-md--letter-spacing);
}

.lockup__openally {
  font-size: var(--text-label-lg);
  font-weight: 600;
  color: var(--color-fg-subtle);
  white-space: nowrap;
}

.lockup__tm {
  font-size: 0.5em;
  font-weight: 600;
  vertical-align: super;
  margin-left: 0.0625rem;
}

.lockup__tagline {
  margin: 0.125rem 0 0;
  font-size: var(--text-label-md);
  line-height: var(--text-label-md--line-height);
  letter-spacing: var(--text-label-md--letter-spacing);
  color: var(--color-fg-muted);
}
</style>

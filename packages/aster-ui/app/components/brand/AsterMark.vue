<script setup lang="ts">
/**
 * The Aster bolt. Ported verbatim from the Android app's only vector drawable,
 * apps/android/app/src/main/res/drawable/bolt.xml, so web and phone render the
 * identical glyph. Inherits currentColor — tint it with the primary token.
 */
withDefaults(defineProps<{ size?: number | string }>(), { size: 24 });
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 100 100"
    role="img"
    aria-label="Aster"
    class="aster-mark"
  >
    <path
      fill="currentColor"
      d="m82.39,2.5h-41.09c-0.73,0 -1.38,0.42 -1.68,1.08l-23.69,52.11c-0.56,1.22 0.34,2.61 1.68,2.61h27.06l-11.85,36.76c-0.62,1.93 1.86,3.31 3.17,1.76l47.2,-55.78c1.02,-1.2 0.16,-3.04 -1.41,-3.04h-18.76l20.92,-32.67c0.79,-1.23 -0.1,-2.85 -1.56,-2.85z"
    />
  </svg>
</template>

<style scoped>
.aster-mark {
  display: block;
  flex: none;
  color: var(--color-primary);
}
</style>

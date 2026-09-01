<script setup lang="ts">
/**
 * The one spinner. Replaces seven separate implementations and four
 * re-declarations of `@keyframes spin` across the old dashboard.
 */
withDefaults(defineProps<{ size?: number }>(), { size: 20 });
</script>

<template>
  <span
    class="spinner"
    role="status"
    aria-label="Loading"
    :style="{ width: `${size}px`, height: `${size}px` }"
  />
</template>

<style scoped>
.spinner {
  display: inline-block;
  flex: none;
  border: 2px solid color-mix(in oklab, currentColor 25%, transparent);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spinner-rotate 0.7s linear infinite;
}

@keyframes spinner-rotate {
  to {
    transform: rotate(360deg);
  }
}
</style>

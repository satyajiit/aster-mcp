<script setup lang="ts">
/**
 * The site-wide stagger primitive, ported from AnimatedEntrance.kt:
 * fade in with a 24px rise over 400ms, with a configurable delay.
 * Honours prefers-reduced-motion via the global reset in tokens.css.
 */
withDefaults(defineProps<{ delay?: number; duration?: number }>(), {
  delay: 0,
  duration: 400,
});
</script>

<template>
  <div
    class="entrance"
    :style="{ animationDelay: `${delay}ms`, animationDuration: `${duration}ms` }"
  >
    <slot />
  </div>
</template>

<style scoped>
.entrance {
  animation-name: entrance-rise;
  animation-timing-function: var(--ease-out-soft);
  animation-fill-mode: both;
}

@keyframes entrance-rise {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

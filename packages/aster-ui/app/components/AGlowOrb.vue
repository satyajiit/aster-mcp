<script setup lang="ts">
/**
 * Ported from GlowOrb.kt: an outer halo at 15% alpha breathing 1.0 -> 1.3 on a
 * 2s reversing tween, with a solid inner disc at half the diameter.
 */
withDefaults(
  defineProps<{ size?: number; color?: string; active?: boolean }>(),
  { size: 48, color: 'var(--color-primary)', active: true },
);
</script>

<template>
  <span
    class="orb"
    :style="{ width: `${size}px`, height: `${size}px`, '--orb-color': color }"
  >
    <span class="orb__halo" :class="{ 'orb__halo--active': active }" />
    <span class="orb__core" :style="{ width: `${size / 2}px`, height: `${size / 2}px` }" />
  </span>
</template>

<style scoped>
.orb {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.orb__halo {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: color-mix(in oklab, var(--orb-color) 15%, transparent);
}

.orb__halo--active {
  animation: orb-breathe 2s var(--ease-in-out-soft) infinite alternate;
}

.orb__core {
  position: relative;
  border-radius: 50%;
  background: var(--orb-color);
}

@keyframes orb-breathe {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.3);
  }
}
</style>

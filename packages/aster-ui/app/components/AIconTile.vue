<script setup lang="ts">
/**
 * The tinted rounded-square icon container that defines the Android look:
 * a 40dp tile at 10dp radius filled with the accent at 12% alpha.
 */
withDefaults(
  defineProps<{
    icon?: string;
    accent?: string;
    size?: number;
  }>(),
  { accent: 'var(--color-primary)', size: 40 },
);
</script>

<template>
  <div
    class="icon-tile"
    :style="{
      '--tile-accent': accent,
      width: `${size}px`,
      height: `${size}px`,
      borderRadius: size <= 32 ? 'var(--radius-md)' : 'var(--radius-lg)',
    }"
  >
    <slot>
      <Icon v-if="icon" :name="icon" :style="{ fontSize: `${Math.round(size * 0.55)}px` }" />
    </slot>
  </div>
</template>

<style scoped>
.icon-tile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  background: color-mix(in oklab, var(--tile-accent) 12%, transparent);
  color: var(--tile-accent);
}
</style>

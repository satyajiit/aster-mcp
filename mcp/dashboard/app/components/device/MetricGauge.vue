<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    percent: number;
    accent?: string;
    size?: number;
    label?: string;
  }>(),
  { accent: 'var(--color-primary)', size: 104 },
);

const RADIUS = 44;
const CIRC = 2 * Math.PI * RADIUS;

const clamped = computed(() => Math.max(0, Math.min(100, props.percent || 0)));
const dash = computed(() => `${(clamped.value / 100) * CIRC} ${CIRC}`);
</script>

<template>
  <div class="gauge" :style="{ width: `${size}px`, height: `${size}px`, '--gauge-size': `${size}px`, '--gauge-accent': accent }">
    <svg viewBox="0 0 100 100" class="gauge__svg" role="img" :aria-label="`${label ?? 'Usage'}: ${Math.round(clamped)}%`">
      <circle class="gauge__track" cx="50" cy="50" :r="RADIUS" />
      <circle
        class="gauge__fill"
        cx="50"
        cy="50"
        :r="RADIUS"
        :stroke-dasharray="dash"
        transform="rotate(-90 50 50)"
      />
    </svg>
    <div class="gauge__value">
      <span class="gauge__line"
        ><span class="gauge__num">{{ Math.round(clamped) }}</span
        ><span class="gauge__pct">%</span></span
      >
    </div>
  </div>
</template>

<style scoped>
.gauge {
  position: relative;
  flex: none;
  --gauge-size: 104px;
}

.gauge__svg {
  width: 100%;
  height: 100%;
}

.gauge__track {
  fill: none;
  stroke: var(--color-surface-3);
  stroke-width: 8;
}

.gauge__fill {
  fill: none;
  stroke: var(--gauge-accent);
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray var(--dur-slow) var(--ease-out-soft);
}

/*
 * Two nested boxes on purpose. `align-items: baseline` on a box that is
 * `inset: 0` parks the text at the TOP of the ring — flex baseline alignment
 * puts the aligned group at the cross-axis start, not the middle — which is
 * how the value ended up touching the track. The grid centres the line box;
 * the line box keeps the "%" sitting on the number's baseline.
 */
.gauge__value {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  pointer-events: none;
}

.gauge__line {
  display: flex;
  align-items: baseline;
  gap: 1px;
  line-height: 1;
}

.gauge__num {
  /* Clamped to the ring so a smaller gauge cannot overflow its own track. */
  font-size: min(var(--text-headline-md), calc(var(--gauge-size) * 0.24));
  font-weight: 700;
  line-height: 1;
  color: var(--color-fg);
  font-variant-numeric: tabular-nums;
}

.gauge__pct {
  font-size: min(var(--text-label-md), calc(var(--gauge-size) * 0.13));
  line-height: 1;
  color: var(--color-fg-muted);
}
</style>

<script setup lang="ts">
/**
 * Dot + label status chip. Replaces the five competing `.status-dot` /
 * `.status-chip` / `.chip-*` systems in the old dashboard.
 */
const props = withDefaults(
  defineProps<{
    status: 'online' | 'offline' | 'pending' | 'approved' | 'rejected' | 'error' | 'running';
    label?: string | null;
    pulse?: boolean;
  }>(),
  { label: null, pulse: true },
);

const MAP: Record<string, { color: string; label: string }> = {
  online: { color: 'var(--color-success)', label: 'Online' },
  running: { color: 'var(--color-success)', label: 'Running' },
  approved: { color: 'var(--color-primary)', label: 'Approved' },
  pending: { color: 'var(--color-warning)', label: 'Pending' },
  offline: { color: 'var(--color-fg-muted)', label: 'Offline' },
  rejected: { color: 'var(--color-error)', label: 'Rejected' },
  error: { color: 'var(--color-error)', label: 'Error' },
};

const meta = computed(() => MAP[props.status] ?? MAP.offline);
const shouldPulse = computed(
  () => props.pulse && (props.status === 'online' || props.status === 'running'),
);
</script>

<template>
  <span class="pill" :style="{ '--pill-color': meta.color }">
    <span class="pill__dot" :class="{ 'pill__dot--pulse': shouldPulse }" />
    {{ label ?? meta.label }}
  </span>
</template>

<style scoped>
.pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  border-radius: var(--radius-md);
  padding: 0.1875rem 0.5rem;
  background: color-mix(in oklab, var(--pill-color) 12%, transparent);
  border: 1px solid color-mix(in oklab, var(--pill-color) 30%, transparent);
  color: var(--pill-color);
  font-size: var(--text-label-md);
  line-height: var(--text-label-md--line-height);
  font-weight: 600;
  white-space: nowrap;
}

.pill__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pill-color);
  flex: none;
}

.pill__dot--pulse {
  animation: pill-pulse 2s var(--ease-in-out-soft) infinite;
}

@keyframes pill-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 color-mix(in oklab, var(--pill-color) 45%, transparent);
  }
  50% {
    box-shadow: 0 0 0 4px color-mix(in oklab, var(--pill-color) 0%, transparent);
  }
}
</style>

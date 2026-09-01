<script setup lang="ts">
import type { LogEntry } from '~/composables/useApi';

const props = defineProps<{ log: LogEntry; showDevice?: boolean }>();

const LEVELS: Record<LogEntry['level'], { icon: string; color: string }> = {
  debug: { icon: 'ph:bug', color: 'var(--color-fg-muted)' },
  info: { icon: 'ph:info', color: 'var(--color-info)' },
  warn: { icon: 'ph:warning', color: 'var(--color-warning)' },
  error: { icon: 'ph:x-circle', color: 'var(--color-error)' },
};

const meta = computed(() => LEVELS[props.log.level] ?? LEVELS.info);

/**
 * The `data` column carries the structured payload — SMS sender and body,
 * notification title and text, incoming-call numbers, command params, error
 * detail. The old dashboard fetched it on every request and rendered none of
 * it, so all of that was written to the DB and never seen.
 */
const payload = computed(() => {
  if (!props.log.data) return null;
  try {
    return JSON.stringify(JSON.parse(props.log.data), null, 2);
  } catch {
    return props.log.data;
  }
});

const expanded = ref(false);
</script>

<template>
  <div class="row" :style="{ '--level-color': meta.color }">
    <button
      type="button"
      class="row__head"
      :class="{ 'row__head--expandable': payload }"
      :aria-expanded="payload ? expanded : undefined"
      @click="payload && (expanded = !expanded)"
    >
      <Icon :name="meta.icon" class="row__icon" />
      <time class="row__time mono">{{ formatClockTime(log.timestamp) }}</time>
      <span v-if="showDevice" class="row__device mono">{{ shortId(log.deviceId) }}</span>
      <span class="row__msg">{{ log.message }}</span>
      <Icon
        v-if="payload"
        :name="expanded ? 'ph:caret-up' : 'ph:caret-down'"
        class="row__caret"
      />
    </button>

    <pre v-if="payload && expanded" class="row__payload">{{ payload }}</pre>
  </div>
</template>

<style scoped>
.row {
  border-bottom: 1px solid var(--color-border);
}

.row:last-child {
  border-bottom: none;
}

.row__head {
  display: flex;
  align-items: baseline;
  gap: 0.625rem;
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: none;
  background: none;
  text-align: left;
  font: inherit;
  color: inherit;
  cursor: default;
}

.row__head--expandable {
  cursor: pointer;
}

.row__head--expandable:hover {
  background: var(--color-surface-2);
}

.row__icon {
  color: var(--level-color);
  font-size: 1rem;
  flex: none;
  align-self: center;
}

.row__time {
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
  flex: none;
}

.row__device {
  font-size: var(--text-label-sm);
  color: var(--color-fg-subtle);
  background: var(--color-surface-2);
  border-radius: var(--radius-xs);
  padding: 0.0625rem 0.3125rem;
  flex: none;
}

.row__msg {
  font-size: var(--text-body-md);
  color: var(--color-fg);
  overflow-wrap: anywhere;
  flex: 1;
}

.row__caret {
  color: var(--color-fg-muted);
  flex: none;
  align-self: center;
}

.row__payload {
  margin: 0;
  padding: 0.625rem 0.75rem 0.875rem 2.5rem;
  font-family: var(--font-mono);
  font-size: var(--text-body-sm);
  line-height: 1.6;
  color: var(--color-fg-subtle);
  background: var(--color-surface-2);
  overflow-x: auto;
}
</style>

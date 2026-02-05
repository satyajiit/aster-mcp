<script setup lang="ts">
import type { LogEntry } from '~/composables/useApi';

defineProps<{
  log: LogEntry;
  showDevice?: boolean;
}>();

function formatTime(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString('en-US', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

const levelIcons: Record<string, string> = {
  debug: '◌',
  info: '●',
  warn: '▲',
  error: '✖',
};
</script>

<template>
  <div class="log-entry" :class="'level-' + log.level">
    <span class="log-time">{{ formatTime(log.timestamp) }}</span>

    <span class="log-level">
      <span class="level-icon">{{ levelIcons[log.level] }}</span>
      <span class="level-text">{{ log.level.toUpperCase() }}</span>
    </span>

    <span v-if="showDevice" class="log-device" :title="log.deviceId">
      {{ log.deviceId.slice(0, 8) }}
    </span>

    <span class="log-message">{{ log.message }}</span>
  </div>
</template>

<style scoped>
.log-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  font-size: 12px;
  border-left: 2px solid transparent;
  transition: all 0.15s ease;
}

.log-entry:hover {
  background: rgba(148, 163, 184, 0.03);
}

.level-error {
  border-left-color: #ef4444;
  background: rgba(239, 68, 68, 0.02);
}

.level-warn {
  border-left-color: #f59e0b;
  background: rgba(245, 158, 11, 0.02);
}

.log-time {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-terminal-dim);
  min-width: 65px;
  font-variant-numeric: tabular-nums;
}

.log-level {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 55px;
}

.level-icon {
  font-size: 10px;
}

.level-text {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.level-debug .log-level { color: var(--color-terminal-dim); }
.level-info .log-level { color: var(--color-primary); }
.level-warn .log-level { color: #f59e0b; }
.level-error .log-level { color: #ef4444; }

.log-device {
  font-family: var(--font-mono);
  font-size: 10px;
  padding: 2px 6px;
  background: rgba(34, 211, 238, 0.08);
  border-radius: 3px;
  color: var(--color-primary);
}

.log-message {
  flex: 1;
  color: var(--color-terminal-text);
  word-break: break-all;
  line-height: 1.4;
}
</style>

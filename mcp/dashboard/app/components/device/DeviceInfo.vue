<script setup lang="ts">
import type { Device } from '~/composables/useApi';

defineProps<{
  device: Device;
}>();

function formatDate(timestamp: number): string {
  return new Date(timestamp).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
</script>

<template>
  <div class="device-info">
    <div class="grid grid-cols-2 gap-x-8 gap-y-4">
      <div class="info-row">
        <span class="info-label">Device ID</span>
        <code class="info-value text-primary">{{ device.id }}</code>
      </div>

      <div class="info-row">
        <span class="info-label">Status</span>
        <StatusBadge :status="device.online ? 'online' : 'offline'" />
      </div>

      <div class="info-row">
        <span class="info-label">Name</span>
        <span class="info-value">{{ device.name }}</span>
      </div>

      <div class="info-row">
        <span class="info-label">Approval</span>
        <StatusBadge :status="device.status" :show-dot="false" />
      </div>

      <div class="info-row">
        <span class="info-label">Platform</span>
        <span class="info-value capitalize">{{ device.platform }} {{ device.osVersion }}</span>
      </div>

      <div class="info-row">
        <span class="info-label">Device</span>
        <span class="info-value">{{ device.manufacturer }} {{ device.model }}</span>
      </div>

      <div class="info-row">
        <span class="info-label">Registered</span>
        <span class="info-value text-terminal-muted">{{ formatDate(device.createdAt) }}</span>
      </div>

      <div class="info-row">
        <span class="info-label">Last Seen</span>
        <span class="info-value text-terminal-muted">{{ formatDate(device.lastSeen) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-terminal-muted);
}

.info-value {
  font-size: 13px;
  color: var(--color-terminal-text-bright);
}
</style>

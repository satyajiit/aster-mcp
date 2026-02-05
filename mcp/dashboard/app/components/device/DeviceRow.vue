<script setup lang="ts">
import type { Device } from '~/composables/useApi';

const props = defineProps<{
  device: Device;
}>();

const emit = defineEmits<{
  approve: [id: string];
  reject: [id: string];
}>();

function formatLastSeen(timestamp: number): string {
  const diff = Date.now() - timestamp;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  return `${days}d ago`;
}

const platformIcons: Record<string, string> = {
  android: '🤖',
  ios: '🍎',
};
</script>

<template>
  <tr class="device-row group">
    <td class="w-8">
      <span class="status-dot" :class="device.online ? 'status-online' : 'status-offline'"></span>
    </td>

    <td>
      <NuxtLink
        :to="`/devices/${device.id}`"
        class="flex flex-col gap-0.5 group-hover:text-primary transition-colors"
      >
        <span class="text-terminal-text-bright font-medium">{{ device.name }}</span>
        <span class="text-[11px] text-terminal-muted">
          {{ device.id.slice(0, 12) }}...
        </span>
      </NuxtLink>
    </td>

    <td>
      <div class="flex items-center gap-2">
        <span class="text-lg">{{ platformIcons[device.platform] || '📱' }}</span>
        <div class="flex flex-col">
          <span class="text-terminal-text">{{ device.manufacturer }} {{ device.model }}</span>
          <span class="text-[11px] text-terminal-muted">{{ device.platform }} {{ device.osVersion }}</span>
        </div>
      </div>
    </td>

    <td>
      <StatusBadge :status="device.status" />
    </td>

    <td class="text-terminal-muted text-[12px] tabular-nums">
      {{ formatLastSeen(device.lastSeen) }}
    </td>

    <td class="text-right">
      <div v-if="device.status === 'pending'" class="flex gap-2 justify-end opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          class="btn-terminal btn-terminal-success text-[11px] px-3 py-1"
          @click.stop="emit('approve', device.id)"
        >
          APPROVE
        </button>
        <button
          class="btn-terminal btn-terminal-danger text-[11px] px-3 py-1"
          @click.stop="emit('reject', device.id)"
        >
          REJECT
        </button>
      </div>
      <NuxtLink
        v-else
        :to="`/devices/${device.id}`"
        class="btn-terminal text-[11px] px-3 py-1 opacity-0 group-hover:opacity-100 transition-opacity inline-block"
      >
        VIEW →
      </NuxtLink>
    </td>
  </tr>
</template>

<style scoped>
.device-row td {
  transition: background 0.15s ease;
}
</style>

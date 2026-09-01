<script setup lang="ts">
import type { Device } from '~/composables/useApi';

const route = useRoute();
const api = useApi();
const deviceId = route.params.id as string;

const device = ref<Device | null>(null);
const loading = ref(true);

useHead({ title: () => (device.value ? `Files · ${device.value.name}` : 'Files') });

async function load() {
  device.value = await api.getDevice(deviceId);
  loading.value = false;
}
usePolling(load, 8000);
</script>

<template>
  <div>
    <PageHeader
      title="Files"
      description="Browse and manage the device filesystem."
      :back-to="`/devices/${deviceId}`"
      back-label="Device"
    >
      <template #actions>
        <AStatusPill v-if="device" :status="device.online ? 'online' : 'offline'" />
      </template>
    </PageHeader>

    <div v-if="loading" class="state"><ASpinner /> Loading…</div>
    <AEmptyState
      v-else-if="!device?.online"
      icon="ph:wifi-slash"
      title="Device is offline"
      description="Files can only be browsed while the device is connected."
    />
    <FileBrowser v-else :device-id="deviceId" :online="device.online" />
  </div>
</template>

<style scoped>
.state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: center;
  padding: 3rem;
  color: var(--color-fg-subtle);
}
</style>

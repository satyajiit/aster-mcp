<script setup lang="ts">
import type { Device } from '~/composables/useApi';

const api = useApi();
const devices = ref<Device[]>([]);
const loading = ref(true);
const filter = ref<'all' | 'online' | 'pending'>('all');

onMounted(async () => {
  await fetchDevices();
  setInterval(fetchDevices, 5000);
});

async function fetchDevices() {
  try {
    devices.value = await api.getDevices();
  } finally {
    loading.value = false;
  }
}

const filteredDevices = computed(() => {
  switch (filter.value) {
    case 'online':
      return devices.value.filter(d => d.online);
    case 'pending':
      return devices.value.filter(d => d.status === 'pending');
    default:
      return devices.value;
  }
});

const counts = computed(() => ({
  all: devices.value.length,
  online: devices.value.filter(d => d.online).length,
  pending: devices.value.filter(d => d.status === 'pending').length,
}));

async function handleApprove(id: string) {
  await api.approveDevice(id);
  await fetchDevices();
}

async function handleReject(id: string) {
  await api.rejectDevice(id);
  await fetchDevices();
}
</script>

<template>
  <div class="min-h-screen p-6">
    <div class="max-w-[1400px] mx-auto space-y-6">
      <!-- Header -->
      <TerminalPageHeader
        description="Device Registry // Manage Connected Devices"
        :loading="loading"
        :show-refresh="false"
        @refresh="fetchDevices"
      >
        <template #left>
          <NuxtLink to="/" class="btn-terminal text-[11px] px-3 py-2 inline-flex items-center gap-2">
            <Icon name="ph:arrow-left" size="14" />
            DASHBOARD
          </NuxtLink>
        </template>
        <template #actions>
          <button
            class="btn-terminal text-[11px] px-3 py-2 inline-flex items-center gap-2"
            :disabled="loading"
            @click="fetchDevices"
          >
            <Icon name="ph:arrows-clockwise" size="14" :class="{ 'animate-spin': loading }" />
            {{ loading ? 'SYNCING' : 'REFRESH' }}
          </button>
        </template>
      </TerminalPageHeader>

      <!-- Quick Stats -->
      <div class="grid grid-cols-3 gap-4">
        <StatCard
          label="Approved"
          :value="devices.filter(d => d.status === 'approved').length"
          icon="✓"
          color="emerald"
        />
        <StatCard
          label="Pending"
          :value="devices.filter(d => d.status === 'pending').length"
          icon="◐"
          color="amber"
        />
        <StatCard
          label="Rejected"
          :value="devices.filter(d => d.status === 'rejected').length"
          icon="✕"
          color="rose"
        />
      </div>

      <!-- Filter Tabs -->
      <div class="terminal-panel p-1 inline-flex gap-1">
        <button
          v-for="f in ['all', 'online', 'pending'] as const"
          :key="f"
          class="px-4 py-2 text-[11px] uppercase tracking-wider transition-all rounded-sm"
          :class="filter === f
            ? 'bg-terminal-surface-elevated text-primary'
            : 'text-terminal-muted hover:text-terminal-text'"
          @click="filter = f"
        >
          {{ f }}
          <span class="ml-2 text-terminal-dim">({{ counts[f] }})</span>
        </button>
      </div>

      <!-- Devices Table -->
      <TerminalWindow title="device registry" class="animate-fade-in">
        <div v-if="loading" class="py-12 text-center">
          <div class="text-primary animate-pulse">Loading...</div>
        </div>

        <div v-else-if="filteredDevices.length === 0" class="py-12 text-center">
          <div class="text-terminal-muted text-4xl mb-4">
            {{ filter === 'pending' ? '◐' : '◌' }}
          </div>
          <div class="text-terminal-muted">
            {{ filter === 'all' ? 'No devices registered' : `No ${filter} devices` }}
          </div>
          <div v-if="filter !== 'all'" class="mt-4">
            <button
              class="btn-terminal text-[11px]"
              @click="filter = 'all'"
            >
              Show all devices
            </button>
          </div>
        </div>

        <table v-else class="terminal-table">
          <thead>
            <tr>
              <th class="w-8">
                <span class="sr-only">Status</span>
              </th>
              <th>Device</th>
              <th>Model</th>
              <th>Status</th>
              <th>Last Seen</th>
              <th class="w-40"></th>
            </tr>
          </thead>
          <tbody>
            <DeviceRow
              v-for="device in filteredDevices"
              :key="device.id"
              :device="device"
              @approve="handleApprove"
              @reject="handleReject"
            />
          </tbody>
        </table>
      </TerminalWindow>
    </div>
  </div>
</template>

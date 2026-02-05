<script setup lang="ts">
import type { Device, Stats, LogEntry } from '~/composables/useApi';

const api = useApi();

const stats = ref<Stats | null>(null);
const devices = ref<Device[]>([]);
const logs = ref<LogEntry[]>([]);
const loading = ref(true);
const serverOnline = ref(false);
const currentTime = ref(new Date());

// Update time every second
let timeInterval: ReturnType<typeof setInterval>;

onMounted(async () => {
  timeInterval = setInterval(() => {
    currentTime.value = new Date();
  }, 1000);

  await fetchData();

  // Poll for updates
  setInterval(fetchData, 5000);
});

onUnmounted(() => {
  clearInterval(timeInterval);
});

async function fetchData() {
  try {
    const [statsData, devicesData, logsData] = await Promise.all([
      api.getStats(),
      api.getDevices(),
      api.getLogs(20),
    ]);

    stats.value = statsData;
    devices.value = devicesData;
    logs.value = logsData;
    serverOnline.value = true;
  } catch {
    serverOnline.value = false;
  } finally {
    loading.value = false;
  }
}

async function handleApprove(id: string) {
  await api.approveDevice(id);
  await fetchData();
}

async function handleReject(id: string) {
  await api.rejectDevice(id);
  await fetchData();
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString('en-US', { hour12: false });
}
</script>

<template>
  <div class="min-h-screen p-6">
    <div class="max-w-[1536px] mx-auto space-y-6">
      <!-- Header -->
      <TerminalPageHeader
        description="MCP Server // Device Control Bridge"
        :loading="loading"
        @refresh="fetchData"
      >
        <template #actions>
          <div class="flex items-center gap-4">
            <div class="flex items-center gap-3">
              <span class="status-dot" :class="serverOnline ? 'status-online' : 'status-error'"></span>
              <span class="text-[11px] uppercase tracking-wider" :class="serverOnline ? 'text-emerald' : 'text-rose'">
                {{ serverOnline ? 'Online' : 'Offline' }}
              </span>
            </div>
            <div class="text-terminal-dim">|</div>
            <div class="text-terminal-muted text-[12px] tabular-nums">
              {{ formatTime(currentTime) }}
            </div>
          </div>
        </template>
      </TerminalPageHeader>

      <!-- Stats Grid -->
      <div v-if="stats" class="grid grid-cols-4 gap-4 animate-fade-in">
        <StatCard
          label="Total Devices"
          :value="stats.totalDevices"
          icon="◎"
          color="blue"
          class="stagger-1"
        />
        <StatCard
          label="Online"
          :value="stats.onlineDevices"
          icon="●"
          color="emerald"
          class="stagger-2"
        />
        <StatCard
          label="Pending Approval"
          :value="stats.pendingDevices"
          icon="◐"
          color="amber"
          class="stagger-3"
        />
        <StatCard
          label="Approved"
          :value="stats.approvedDevices"
          icon="✓"
          color="violet"
          class="stagger-4"
        />
      </div>

      <!-- Loading skeleton -->
      <div v-else-if="loading" class="grid grid-cols-4 gap-4">
        <div v-for="i in 4" :key="i" class="terminal-panel p-4 h-24 animate-pulse">
          <div class="h-3 bg-terminal-border rounded w-20 mb-4"></div>
          <div class="h-8 bg-terminal-border rounded w-12"></div>
        </div>
      </div>

      <!-- Main Content Grid -->
      <div class="grid grid-cols-3 gap-6">
        <!-- Devices List -->
        <div class="col-span-2">
          <TerminalWindow title="devices" class="animate-slide-up stagger-2">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2 text-[11px]">
                <span class="text-terminal-muted">{{ devices.length }} devices registered</span>
                <span class="text-terminal-dim">|</span>
                <span class="text-emerald">{{ devices.filter(d => d.online).length }} online</span>
              </div>
              <NuxtLink to="/devices" class="btn-terminal text-[11px] px-3 py-1">
                VIEW ALL →
              </NuxtLink>
            </div>

            <div v-if="devices.length === 0 && !loading" class="py-12 text-center">
              <div class="text-terminal-muted text-4xl mb-4">◌</div>
              <div class="text-terminal-muted">No devices connected</div>
              <div class="text-[11px] text-terminal-dim mt-2">
                Connect an Android device to get started
              </div>
            </div>

            <table v-else class="terminal-table">
              <thead>
                <tr>
                  <th class="w-8"></th>
                  <th>Device</th>
                  <th>Model</th>
                  <th>Status</th>
                  <th>Last Seen</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <DeviceRow
                  v-for="device in devices.slice(0, 5)"
                  :key="device.id"
                  :device="device"
                  @approve="handleApprove"
                  @reject="handleReject"
                />
              </tbody>
            </table>
          </TerminalWindow>
        </div>

        <!-- Activity Log -->
        <div class="col-span-1">
          <TerminalWindow title="activity log" class="animate-slide-up stagger-3">
            <div class="space-y-0 max-h-[400px] overflow-y-auto">
              <div v-if="logs.length === 0 && !loading" class="py-8 text-center text-terminal-muted text-[12px]">
                No activity yet
              </div>
              <LogEntry
                v-for="log in logs"
                :key="log.id"
                :log="log"
                show-device
              />
            </div>
          </TerminalWindow>
        </div>
      </div>

      <!-- Footer -->
      <footer class="pt-6 border-t border-terminal-border">
        <div class="flex items-center justify-between text-[11px] text-terminal-muted">
          <div class="flex items-center gap-4">
            <span>ASTER v0.1.0</span>
            <span class="text-terminal-dim">|</span>
            <span>MCP Protocol</span>
          </div>
          <div class="flex items-center gap-4">
            <NuxtLink to="/devices" class="hover:text-primary transition-colors">Devices</NuxtLink>
            <span class="text-terminal-dim">|</span>
            <a href="#" class="hover:text-primary transition-colors">Documentation</a>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>


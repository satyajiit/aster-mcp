<script setup lang="ts">
import type { Device, Stats, LogEntry } from '~/composables/useApi';

const api = useApi();

const stats = ref<Stats | null>(null);
const devices = ref<Device[]>([]);
const logs = ref<LogEntry[]>([]);
const loading = ref(true);
const serverOnline = ref(false);
const currentTime = ref(new Date());
const openclawEnabled = ref<boolean | null>(null);

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
    const [statsData, devicesData, logsData, openclawData] = await Promise.all([
      api.getStats(),
      api.getDevices(),
      api.getLogs(20),
      api.getOpenClawConfig().catch(() => null),
    ]);

    stats.value = statsData;
    devices.value = devicesData;
    logs.value = logsData;
    openclawEnabled.value = openclawData?.config?.enabled ?? null;
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

      <!-- OpenClaw Integration CTA -->
      <NuxtLink
        to="/settings/openclaw"
        class="openclaw-cta animate-fade-in stagger-4"
      >
        <div class="cta-content">
          <div class="flex items-center gap-3">
            <div class="cta-icon">
              <span>&#9889;</span>
            </div>
            <div>
              <div class="cta-title">OpenClaw Integration</div>
              <div class="cta-subtitle">
                <template v-if="openclawEnabled === true">
                  Event forwarding is active
                </template>
                <template v-else-if="openclawEnabled === false">
                  Event forwarding is disabled
                </template>
                <template v-else>
                  Forward notifications & SMS to Claude in real-time
                </template>
              </div>
            </div>
          </div>
          <div class="cta-right">
            <span
              v-if="openclawEnabled !== null"
              class="badge text-[9px]"
              :class="openclawEnabled ? 'badge-emerald' : 'badge-amber'"
            >
              {{ openclawEnabled ? 'ACTIVE' : 'DISABLED' }}
            </span>
            <span v-else class="badge badge-muted text-[9px]">SETUP</span>
            <span class="cta-arrow">&#8594;</span>
          </div>
        </div>
      </NuxtLink>

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
            <NuxtLink to="/settings/openclaw" class="hover:text-primary transition-colors">OpenClaw</NuxtLink>
            <span class="text-terminal-dim">|</span>
            <a href="#" class="hover:text-primary transition-colors">Documentation</a>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.openclaw-cta {
  display: block;
  background: rgba(30, 41, 59, 0.95);
  border: 1px solid var(--color-terminal-border);
  border-radius: 2px;
  padding: 16px 20px;
  transition: all 0.2s ease;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
}

.openclaw-cta:hover {
  border-color: rgba(139, 92, 246, 0.35);
  background: rgba(139, 92, 246, 0.04);
  transform: translateY(-1px);
}

.cta-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cta-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 4px;
  font-size: 16px;
  flex-shrink: 0;
}

.cta-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-terminal-text);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.cta-subtitle {
  font-size: 11px;
  color: var(--color-terminal-dim);
  margin-top: 2px;
}

.cta-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cta-arrow {
  color: var(--color-terminal-dim);
  font-size: 14px;
  transition: all 0.2s ease;
}

.openclaw-cta:hover .cta-arrow {
  color: var(--color-violet);
  transform: translateX(3px);
}
</style>

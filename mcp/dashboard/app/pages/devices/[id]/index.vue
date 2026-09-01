<script setup lang="ts">
import type { DeviceWithLiveInfo, LogEntry } from '~/composables/useApi';
import { toolJson } from '~/composables/useApi';
import type { InfoItem } from '~/components/device/InfoGrid.vue';

const route = useRoute();
const api = useApi();
const toast = useToast();
const deviceId = route.params.id as string;

const device = ref<DeviceWithLiveInfo | null>(null);
const logs = ref<LogEntry[]>([]);
const battery = ref<{ level?: number; charging?: boolean; temperature?: number } | null>(null);
const loading = ref(true);
const telemetryError = ref<string | null>(null);
const telemetryNote = ref<string | null>(null);
const refreshingTelemetry = ref(false);

useHead({ title: () => device.value?.name ?? 'Device' });

const info = computed(() => device.value?.liveInfo ?? device.value?.extendedInfo ?? null);

async function loadDevice() {
  device.value = (await api.getDevice(deviceId)) as DeviceWithLiveInfo;
}

async function loadLogs() {
  const page = await api.getDeviceLogs(deviceId, { limit: 50 });
  logs.value = page.logs;
}

async function loadTelemetry() {
  if (!device.value?.online || device.value.status !== 'approved') return;
  refreshingTelemetry.value = true;
  try {
    const res = await api.getDeviceInfo(deviceId);
    device.value = res;
    // These were in the type and silently dropped, so a failed telemetry
    // refresh looked identical to a successful one.
    telemetryError.value = res.error ?? null;
    telemetryNote.value = res.message ?? null;
  } catch (e) {
    telemetryError.value = e instanceof Error ? e.message : String(e);
  } finally {
    refreshingTelemetry.value = false;
  }
}

async function loadBattery() {
  if (!device.value?.online || device.value.status !== 'approved') return;
  try {
    const result = await api.executeTool(deviceId, 'aster_get_battery', {});
    battery.value = toolJson(result);
  } catch {
    battery.value = null;
  }
}

async function refreshAll() {
  await loadDevice();
  await Promise.all([loadLogs(), loadTelemetry(), loadBattery()]);
}

onMounted(async () => {
  await refreshAll();
  loading.value = false;
});

usePolling(async () => {
  await loadDevice();
  await loadLogs();
}, 5000, { immediate: false });

const memory = computed(() => {
  if (!info.value?.totalRam) return null;
  const used = info.value.totalRam - info.value.availableRam;
  return {
    percent: (used / info.value.totalRam) * 100,
    used: formatBytes(used * 1024 * 1024),
    free: formatBytes(info.value.availableRam * 1024 * 1024),
    total: formatBytes(info.value.totalRam * 1024 * 1024),
  };
});

const storage = computed(() => {
  if (!info.value?.totalStorage) return null;
  const used = info.value.totalStorage - info.value.availableStorage;
  return {
    percent: (used / info.value.totalStorage) * 100,
    used: formatBytes(used * 1024 ** 3),
    free: formatBytes(info.value.availableStorage * 1024 ** 3),
    total: formatBytes(info.value.totalStorage * 1024 ** 3),
  };
});

// Every ExtendedDeviceInfo field, including supportedAbis, buildTags and
// radioVersion — all three came down the wire and were never rendered.
const systemInfo = computed<InfoItem[]>(() => {
  const d = device.value;
  const i = info.value;
  if (!d) return [];
  return [
    { label: 'Device ID', value: d.id, mono: true, icon: 'ph:fingerprint' },
    { label: 'Manufacturer', value: d.manufacturer, icon: 'ph:factory' },
    { label: 'Model', value: d.model, icon: 'ph:device-mobile' },
    { label: 'Platform', value: `${d.platform} ${d.osVersion}`, icon: 'ph:android-logo' },
    { label: 'CPU ABI', value: i?.cpuAbi, mono: true, icon: 'ph:cpu' },
    { label: 'Supported ABIs', value: i?.supportedAbis?.join(', '), mono: true, icon: 'ph:stack' },
    { label: 'Security patch', value: i?.securityPatch, icon: 'ph:shield-check' },
    { label: 'Build type', value: i?.buildType, icon: 'ph:wrench' },
    { label: 'Build tags', value: i?.buildTags, mono: true, icon: 'ph:tag' },
    { label: 'Radio version', value: i?.radioVersion, mono: true, icon: 'ph:cell-signal-full' },
    { label: 'Registered', value: d.createdAt ? new Date(d.createdAt).toLocaleString() : null, icon: 'ph:calendar-plus' },
  ];
});

const runtimeInfo = computed<InfoItem[]>(() => {
  const i = info.value;
  return [
    { label: 'Uptime', value: i?.uptimeMillis ? formatDuration(i.uptimeMillis) : null, icon: 'ph:timer' },
    { label: 'Timezone', value: i?.timezone, icon: 'ph:globe' },
    { label: 'Locale', value: i?.locale, icon: 'ph:translate' },
    { label: 'Last seen', value: device.value ? formatRelativeTime(device.value.lastSeen) : null, icon: 'ph:clock' },
  ];
});

const displayInfo = computed<InfoItem[]>(() => {
  const i = info.value;
  if (!i?.screen) return [];
  return [
    { label: 'Resolution', value: `${i.screen.widthPixels} × ${i.screen.heightPixels}`, mono: true },
    { label: 'Density', value: `${i.screen.densityDpi} dpi`, mono: true },
    { label: 'Scale', value: `${i.screen.density}×`, mono: true },
    { label: 'Refresh rate', value: i.screenRefreshRate ? `${Math.round(i.screenRefreshRate)} Hz` : null, mono: true },
  ];
});

const canControl = computed(() => device.value?.online && device.value.status === 'approved');

async function approve() {
  await api.approveDevice(deviceId);
  toast.success('Device approved');
  await refreshAll();
}
</script>

<template>
  <div>
    <PageHeader
      :title="device?.name ?? 'Device'"
      :description="device ? `${device.manufacturer} ${device.model} · ${device.platform} ${device.osVersion}` : ''"
      back-to="/devices"
      back-label="Devices"
    >
      <template #actions>
        <AStatusPill v-if="device" :status="device.online ? 'online' : 'offline'" />
        <AStatusPill v-if="device" :status="device.status" />
        <AButton
          size="sm"
          icon="ph:arrows-clockwise"
          :loading="refreshingTelemetry"
          @click="refreshAll"
        >
          Refresh
        </AButton>
      </template>
    </PageHeader>

    <div v-if="loading" class="loading"><ASpinner /> Loading device…</div>

    <template v-else-if="device">
      <ACard v-if="device.status === 'pending'" class="banner" accent="var(--color-warning)">
        <div class="banner__row">
          <AIconTile icon="ph:hourglass" accent="var(--color-warning)" />
          <div>
            <p class="banner__title">Waiting for approval</p>
            <p class="banner__desc">This device cannot be controlled until you approve it.</p>
          </div>
          <AButton variant="primary" size="sm" @click="approve">Approve</AButton>
        </div>
      </ACard>

      <!-- Previously the empty catch swallowed these, so a failed telemetry
           read was indistinguishable from a successful one. -->
      <ACard v-if="telemetryError" class="banner" accent="var(--color-error)">
        <div class="banner__row">
          <AIconTile icon="ph:warning-circle" accent="var(--color-error)" />
          <div>
            <p class="banner__title">Telemetry unavailable</p>
            <p class="banner__desc">{{ telemetryError }}</p>
          </div>
        </div>
      </ACard>

      <ACard v-else-if="telemetryNote" class="banner">
        <div class="banner__row">
          <AIconTile icon="ph:info" accent="var(--color-info)" />
          <p class="banner__desc">{{ telemetryNote }}</p>
        </div>
      </ACard>

      <div class="actions">
        <NuxtLink v-if="canControl" :to="`/devices/${deviceId}/screen`" class="action focus-ring">
          <AIconTile icon="ph:cursor-click" accent="var(--color-mode-remote)" />
          <span>Screen control</span>
        </NuxtLink>
        <NuxtLink v-if="canControl" :to="`/devices/${deviceId}/files`" class="action focus-ring">
          <AIconTile icon="ph:folder-open" accent="var(--color-warning)" />
          <span>Files</span>
        </NuxtLink>
        <NuxtLink v-if="canControl" :to="`/devices/${deviceId}/panels`" class="action focus-ring">
          <AIconTile icon="ph:squares-four" accent="var(--color-accent)" />
          <span>Device panels</span>
        </NuxtLink>
        <NuxtLink v-if="canControl" :to="`/devices/${deviceId}/control`" class="action focus-ring">
          <AIconTile icon="ph:terminal-window" accent="var(--color-mode-mcp)" />
          <span>Tool explorer</span>
        </NuxtLink>
      </div>

      <section class="section">
        <ASectionLabel label="Telemetry" />
        <div class="metrics">
          <ACard>
            <p class="metric__label">Memory</p>
            <div class="metric__body">
              <MetricGauge v-if="memory" :percent="memory.percent" label="Memory" />
              <div v-if="memory" class="metric__legend">
                <div><span>Used</span><b>{{ memory.used }}</b></div>
                <div><span>Free</span><b>{{ memory.free }}</b></div>
                <div><span>Total</span><b>{{ memory.total }}</b></div>
              </div>
              <p v-else class="metric__none">No telemetry</p>
            </div>
          </ACard>

          <ACard>
            <p class="metric__label">Storage</p>
            <div class="metric__body">
              <MetricGauge
                v-if="storage"
                :percent="storage.percent"
                accent="var(--color-info)"
                label="Storage"
              />
              <div v-if="storage" class="metric__legend">
                <div><span>Used</span><b>{{ storage.used }}</b></div>
                <div><span>Free</span><b>{{ storage.free }}</b></div>
                <div><span>Total</span><b>{{ storage.total }}</b></div>
              </div>
              <p v-else class="metric__none">No telemetry</p>
            </div>
          </ACard>

          <ACard>
            <p class="metric__label">Battery</p>
            <div class="metric__body">
              <!-- Real charge level from aster_get_battery. The old card drew a
                   fill hardcoded to height:75%, which was permanently a lie. -->
              <MetricGauge
                v-if="battery?.level != null"
                :percent="battery.level"
                :accent="battery.level < 20 ? 'var(--color-error)' : 'var(--color-success)'"
                label="Battery"
              />
              <div class="metric__legend">
                <div v-if="battery?.charging != null">
                  <span>State</span><b>{{ battery.charging ? 'Charging' : 'Discharging' }}</b>
                </div>
                <div v-if="battery?.temperature != null">
                  <span>Temp</span><b>{{ battery.temperature }}°C</b>
                </div>
                <div v-if="info?.batteryCapacity">
                  <span>Capacity</span><b>{{ info.batteryCapacity }} mAh</b>
                </div>
                <p v-if="!battery && !info?.batteryCapacity" class="metric__none">No telemetry</p>
              </div>
            </div>
          </ACard>

          <ACard>
            <p class="metric__label">Display</p>
            <InfoGrid v-if="displayInfo.length" :items="displayInfo" />
            <p v-else class="metric__none">No telemetry</p>
          </ACard>
        </div>
      </section>

      <div class="split">
        <section>
          <ASectionLabel label="System" />
          <InfoGrid :items="systemInfo" />
        </section>
        <section>
          <ASectionLabel label="Runtime" />
          <InfoGrid :items="runtimeInfo" />
        </section>
      </div>

      <section class="section">
        <ASectionLabel label="Recent activity">
          <template #actions>
            <NuxtLink :to="`/logs?device=${deviceId}`" class="more">Open in logs</NuxtLink>
          </template>
        </ASectionLabel>
        <ACard variant="flush">
          <AEmptyState v-if="logs.length === 0" icon="ph:list-dashes" title="No activity yet" />
          <LogRow v-for="log in logs" :key="log.id" :log="log" />
        </ACard>
      </section>
    </template>

    <AEmptyState v-else icon="ph:warning-circle" tone="error" title="Device not found" />
  </div>
</template>

<style scoped>
.loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: center;
  padding: 3rem;
  color: var(--color-fg-subtle);
}

.banner {
  margin-bottom: 1rem;
}

.banner__row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.banner__title {
  margin: 0;
  font-size: var(--text-body-md);
  font-weight: 600;
}

.banner__desc {
  margin: 0.125rem 0 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
  overflow-wrap: anywhere;
}

.banner__row > :last-child:not(p) {
  margin-left: auto;
}

.actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1.75rem;
}

.action {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.875rem;
  border-radius: var(--radius-card);
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
  text-decoration: none;
  color: var(--color-fg);
  font-size: var(--text-body-md);
  font-weight: 500;
  transition: border-color var(--dur-base) var(--ease-out-soft);
}

.action:hover {
  border-color: var(--color-border-bright);
  background: var(--color-surface-2);
}

.section {
  margin-bottom: 1.75rem;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 0.75rem;
}

.metric__label {
  margin: 0 0 0.75rem;
  font-size: var(--text-label-md);
  letter-spacing: var(--text-label-md--letter-spacing);
  text-transform: uppercase;
  color: var(--color-fg-subtle);
}

.metric__body {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.metric__legend {
  display: grid;
  gap: 0.25rem;
  flex: 1;
  min-width: 0;
}

.metric__legend div {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  font-size: var(--text-label-md);
}

.metric__legend span {
  color: var(--color-fg-subtle);
}

.metric__legend b {
  color: var(--color-fg);
  font-weight: 600;
}

.metric__none {
  margin: 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-muted);
}

.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  margin-bottom: 1.75rem;
}

@media (max-width: 900px) {
  .split {
    grid-template-columns: 1fr;
  }
}

.more {
  font-size: var(--text-label-md);
  color: var(--color-primary);
  text-decoration: none;
}
</style>

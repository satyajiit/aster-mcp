<script setup lang="ts">
import type { Device, LogEntry, ExtendedDeviceInfo } from '~/composables/useApi';

const route = useRoute();
const api = useApi();

const device = ref<Device | null>(null);
const logs = ref<LogEntry[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const autoScroll = ref(true);

// Extended device info state
const liveInfo = ref<ExtendedDeviceInfo | null>(null);
const loadingLiveInfo = ref(false);

const deviceId = computed(() => route.params.id as string);
const logsContainer = ref<HTMLElement | null>(null);

onMounted(async () => {
  await fetchData();
  setInterval(fetchLogs, 3000);
});

async function fetchData() {
  try {
    const [deviceData, logsData] = await Promise.all([
      api.getDevice(deviceId.value),
      api.getDeviceLogs(deviceId.value, 50),
    ]);
    device.value = deviceData;
    logs.value = logsData;

    // Auto-fetch live info if device is online and approved
    if (deviceData.online && deviceData.status === 'approved') {
      fetchLiveInfo();
    } else if (deviceData.extendedInfo) {
      liveInfo.value = deviceData.extendedInfo;
    }
  } catch (e) {
    error.value = 'Device not found';
  } finally {
    loading.value = false;
  }
}

async function fetchLiveInfo() {
  if (!device.value?.online || device.value?.status !== 'approved') return;

  loadingLiveInfo.value = true;
  try {
    const data = await api.getDeviceInfo(deviceId.value);
    if (data.liveInfo) {
      liveInfo.value = data.liveInfo;
    }
  } catch {
    // Ignore errors, keep showing cached info
  } finally {
    loadingLiveInfo.value = false;
  }
}

async function fetchLogs() {
  try {
    logs.value = await api.getDeviceLogs(deviceId.value, 50);
    if (autoScroll.value && logsContainer.value) {
      logsContainer.value.scrollTop = logsContainer.value.scrollHeight;
    }
  } catch {
    // Ignore errors during polling
  }
}

async function handleApprove() {
  if (!device.value) return;
  await api.approveDevice(device.value.id);
  await fetchData();
}

async function handleReject() {
  if (!device.value) return;
  await api.rejectDevice(device.value.id);
  await fetchData();
}

function formatDate(timestamp: number): string {
  return new Date(timestamp).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function getTimeSince(timestamp: number): string {
  const diff = Date.now() - timestamp;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  return `${days}d ago`;
}

function formatBytes(mb: number): string {
  if (mb >= 1024) return `${(mb / 1024).toFixed(1)} GB`;
  return `${mb.toFixed(0)} MB`;
}

function formatUptime(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);

  const parts = [];
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0) parts.push(`${minutes}m`);
  return parts.join(' ') || '< 1m';
}

function formatLogTime(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString('en-US', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function getLevelIcon(level: string): string {
  const icons: Record<string, string> = {
    debug: '◌',
    info: '●',
    warn: '▲',
    error: '✖',
  };
  return icons[level] || '○';
}

// Computed properties for gauge displays
const ramUsagePercent = computed(() => {
  if (!liveInfo.value) return 0;
  const used = liveInfo.value.totalRam - liveInfo.value.availableRam;
  return Math.round((used / liveInfo.value.totalRam) * 100);
});

const storageUsagePercent = computed(() => {
  if (!liveInfo.value) return 0;
  const used = liveInfo.value.totalStorage - liveInfo.value.availableStorage;
  return Math.round((used / liveInfo.value.totalStorage) * 100);
});

const ramUsageColor = computed(() => {
  const percent = ramUsagePercent.value;
  if (percent >= 90) return 'gauge-critical';
  if (percent >= 70) return 'gauge-warning';
  return 'gauge-healthy';
});

const storageUsageColor = computed(() => {
  const percent = storageUsagePercent.value;
  if (percent >= 90) return 'gauge-critical';
  if (percent >= 70) return 'gauge-warning';
  return 'gauge-healthy';
});
</script>

<template>
  <div class="device-detail-page">
    <!-- Animated grid background -->
    <div class="grid-bg"></div>
    <div class="ambient-glow" :class="device?.online ? 'glow-online' : 'glow-offline'"></div>

    <div class="relative z-10 p-4 lg:p-6">
      <div class="max-w-[1600px] mx-auto space-y-5">
        <!-- Minimal Header Bar -->
        <header class="header-bar">
          <div class="header-left">
            <NuxtLink to="/devices" class="back-link">
              <Icon name="ph:caret-left-bold" size="16" />
              <span>DEVICES</span>
            </NuxtLink>
            <div class="header-divider"></div>
            <div v-if="device" class="breadcrumb">
              <span class="breadcrumb-id">{{ device.id.slice(0, 8) }}</span>
            </div>
          </div>

          <div class="header-center" v-if="device">
            <div class="connection-indicator" :class="device.online ? 'is-online' : 'is-offline'">
              <div class="indicator-dot"></div>
              <span>{{ device.online ? 'CONNECTED' : 'DISCONNECTED' }}</span>
            </div>
          </div>

          <div class="header-right">
            <button
              class="icon-btn"
              :disabled="loading"
              @click="fetchData"
              title="Refresh"
            >
              <Icon name="ph:arrows-clockwise" size="18" :class="{ 'animate-spin': loading }" />
            </button>
          </div>
        </header>

        <!-- Error State -->
        <div v-if="error" class="error-state">
          <div class="error-glitch" data-text="ERROR">ERROR</div>
          <div class="error-code">404</div>
          <p class="error-message">{{ error }}</p>
          <NuxtLink to="/devices" class="error-back-btn">
            <Icon name="ph:arrow-left" size="16" />
            <span>Return to Device List</span>
          </NuxtLink>
        </div>

        <!-- Loading State -->
        <div v-else-if="loading" class="loading-state">
          <div class="loading-rings">
            <div class="ring ring-1"></div>
            <div class="ring ring-2"></div>
            <div class="ring ring-3"></div>
            <div class="loading-core"></div>
          </div>
          <div class="loading-text">
            <span class="loading-label">ESTABLISHING CONNECTION</span>
            <span class="loading-dots">...</span>
          </div>
        </div>

        <!-- Device Content -->
        <template v-else-if="device">
          <!-- Device Identity Banner -->
          <div class="identity-banner">
            <div class="identity-main">
              <div class="device-icon-wrapper" :class="device.online ? 'icon-online' : 'icon-offline'">
                <Icon :name="device.platform === 'ios' ? 'ph:apple-logo-fill' : 'ph:android-logo-fill'" size="32" />
                <div class="icon-ring"></div>
                <div class="icon-pulse" v-if="device.online"></div>
              </div>

              <div class="device-info">
                <div class="device-name-row">
                  <h1 class="device-name">{{ device.name }}</h1>
                  <div class="status-chips">
                    <span class="status-chip" :class="'chip-' + device.status">
                      {{ device.status }}
                    </span>
                  </div>
                </div>
                <div class="device-specs">
                  <span class="spec-item">
                    <Icon name="ph:device-mobile" size="14" />
                    {{ device.manufacturer }} {{ device.model }}
                  </span>
                  <span class="spec-divider">•</span>
                  <span class="spec-item">
                    <Icon name="ph:cpu" size="14" />
                    {{ device.platform }} {{ device.osVersion }}
                  </span>
                  <span class="spec-divider">•</span>
                  <span class="spec-item">
                    <Icon name="ph:clock" size="14" />
                    {{ getTimeSince(device.lastSeen) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Quick Actions -->
            <div class="identity-actions" v-if="device.status === 'pending'">
              <button class="action-btn action-approve" @click="handleApprove">
                <Icon name="ph:check-bold" size="18" />
                <span>APPROVE</span>
              </button>
              <button class="action-btn action-reject" @click="handleReject">
                <Icon name="ph:x-bold" size="18" />
                <span>REJECT</span>
              </button>
            </div>

            <!-- Approved Device Actions -->
            <div class="identity-actions" v-else-if="device.status === 'approved'">
              <NuxtLink :to="`/devices/${device.id}/files`" class="action-btn action-files">
                <Icon name="ph:folder-open" size="18" />
                <span>BROWSE FILES</span>
              </NuxtLink>
            </div>

            <!-- Decorative Elements -->
            <div class="banner-decoration">
              <div class="deco-line"></div>
              <div class="deco-corner deco-corner-tr"></div>
              <div class="deco-corner deco-corner-br"></div>
            </div>
          </div>

          <!-- Tab Content Wrapper -->
          <div class="tab-content-wrapper">
            <!-- Telemetry Station -->
            <div class="telemetry-panel">
              <!-- Top Row: Key Metrics -->
              <div class="metrics-row">
                <!-- Memory Gauge -->
                <div class="metric-card metric-memory">
                  <div class="metric-header">
                    <div class="metric-icon">
                      <Icon name="ph:memory" size="18" />
                    </div>
                    <span class="metric-title">MEMORY</span>
                    <span v-if="liveInfo" class="metric-badge" :class="ramUsageColor">
                      {{ ramUsagePercent }}%
                    </span>
                  </div>
                  <div v-if="liveInfo" class="metric-body">
                    <div class="radial-gauge" :style="{ '--progress': ramUsagePercent / 100, '--gauge-color': ramUsagePercent >= 90 ? '#ef4444' : ramUsagePercent >= 70 ? '#f59e0b' : '#10b981' }">
                      <svg viewBox="0 0 120 120">
                        <circle cx="60" cy="60" r="52" class="gauge-bg" />
                        <circle cx="60" cy="60" r="52" class="gauge-progress" />
                        <circle cx="60" cy="60" r="40" class="gauge-inner" />
                      </svg>
                      <div class="gauge-value">
                        <span class="value-num">{{ ramUsagePercent }}</span>
                        <span class="value-unit">%</span>
                      </div>
                    </div>
                    <div class="metric-stats">
                      <div class="stat-row">
                        <span class="stat-dot stat-used"></span>
                        <span class="stat-label">Used</span>
                        <span class="stat-value">{{ formatBytes(liveInfo.totalRam - liveInfo.availableRam) }}</span>
                      </div>
                      <div class="stat-row">
                        <span class="stat-dot stat-free"></span>
                        <span class="stat-label">Free</span>
                        <span class="stat-value">{{ formatBytes(liveInfo.availableRam) }}</span>
                      </div>
                      <div class="stat-row stat-total">
                        <span class="stat-label">Total</span>
                        <span class="stat-value">{{ formatBytes(liveInfo.totalRam) }}</span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="metric-placeholder">
                    <div class="placeholder-pulse"></div>
                    <span>Awaiting telemetry...</span>
                  </div>
                </div>

                <!-- Storage Gauge -->
                <div class="metric-card metric-storage">
                  <div class="metric-header">
                    <div class="metric-icon">
                      <Icon name="ph:hard-drives" size="18" />
                    </div>
                    <span class="metric-title">STORAGE</span>
                    <span v-if="liveInfo" class="metric-badge" :class="storageUsageColor">
                      {{ storageUsagePercent }}%
                    </span>
                  </div>
                  <div v-if="liveInfo" class="metric-body">
                    <div class="radial-gauge" :style="{ '--progress': storageUsagePercent / 100, '--gauge-color': storageUsagePercent >= 90 ? '#ef4444' : storageUsagePercent >= 70 ? '#f59e0b' : '#8b5cf6' }">
                      <svg viewBox="0 0 120 120">
                        <circle cx="60" cy="60" r="52" class="gauge-bg" />
                        <circle cx="60" cy="60" r="52" class="gauge-progress" />
                        <circle cx="60" cy="60" r="40" class="gauge-inner" />
                      </svg>
                      <div class="gauge-value">
                        <span class="value-num">{{ storageUsagePercent }}</span>
                        <span class="value-unit">%</span>
                      </div>
                    </div>
                    <div class="metric-stats">
                      <div class="stat-row">
                        <span class="stat-dot stat-used-storage"></span>
                        <span class="stat-label">Used</span>
                        <span class="stat-value">{{ liveInfo.totalStorage - liveInfo.availableStorage }} GB</span>
                      </div>
                      <div class="stat-row">
                        <span class="stat-dot stat-free"></span>
                        <span class="stat-label">Free</span>
                        <span class="stat-value">{{ liveInfo.availableStorage }} GB</span>
                      </div>
                      <div class="stat-row stat-total">
                        <span class="stat-label">Total</span>
                        <span class="stat-value">{{ liveInfo.totalStorage }} GB</span>
                      </div>
                    </div>
                  </div>
                  <div v-else class="metric-placeholder">
                    <div class="placeholder-pulse"></div>
                    <span>Awaiting telemetry...</span>
                  </div>
                </div>

                <!-- Display Info -->
                <div class="metric-card metric-display">
                  <div class="metric-header">
                    <div class="metric-icon">
                      <Icon name="ph:monitor" size="18" />
                    </div>
                    <span class="metric-title">DISPLAY</span>
                    <span v-if="liveInfo?.screen" class="metric-badge metric-badge-info">
                      {{ liveInfo.screenRefreshRate.toFixed(0) }}Hz
                    </span>
                  </div>
                  <div v-if="liveInfo?.screen" class="metric-body display-body">
                    <div class="display-preview">
                      <div class="display-frame" :style="{ aspectRatio: liveInfo.screen.widthPixels / liveInfo.screen.heightPixels }">
                        <div class="display-screen">
                          <div class="screen-glare"></div>
                          <div class="screen-content">
                            <span class="res-text">{{ liveInfo.screen.widthPixels }}×{{ liveInfo.screen.heightPixels }}</span>
                          </div>
                        </div>
                      </div>
                      <div class="display-stand">
                        <div class="stand-neck"></div>
                        <div class="stand-base"></div>
                      </div>
                    </div>
                    <div class="display-specs">
                      <div class="spec-row">
                        <Icon name="ph:frame-corners" size="14" />
                        <span>{{ liveInfo.screen.widthPixels }} × {{ liveInfo.screen.heightPixels }}</span>
                      </div>
                      <div class="spec-row">
                        <Icon name="ph:dots-nine" size="14" />
                        <span>{{ liveInfo.screen.densityDpi }} dpi</span>
                      </div>
                      <div class="spec-row">
                        <Icon name="ph:selection-background" size="14" />
                        <span>{{ liveInfo.screen.density.toFixed(1) }}× scale</span>
                      </div>
                      <div class="megapixel-tag">
                        {{ ((liveInfo.screen.widthPixels * liveInfo.screen.heightPixels) / 1000000).toFixed(1) }} MP
                      </div>
                    </div>
                  </div>
                  <div v-else class="metric-placeholder">
                    <div class="placeholder-pulse"></div>
                    <span>Awaiting telemetry...</span>
                  </div>
                </div>

                <!-- Battery (if available) -->
                <div v-if="liveInfo?.batteryCapacity" class="metric-card metric-battery">
                  <div class="metric-header">
                    <div class="metric-icon">
                      <Icon name="ph:battery-charging-vertical" size="18" />
                    </div>
                    <span class="metric-title">BATTERY</span>
                  </div>
                  <div class="metric-body battery-body">
                    <div class="battery-visual">
                      <div class="battery-cap"></div>
                      <div class="battery-body-shell">
                        <div class="battery-level" style="height: 75%"></div>
                      </div>
                    </div>
                    <div class="battery-stats">
                      <div class="battery-capacity">
                        <span class="cap-value">{{ liveInfo.batteryCapacity }}</span>
                        <span class="cap-unit">mAh</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Bottom Section: Data Panels -->
              <div class="data-panels">
                <!-- Left: System Info -->
                <div class="data-panel panel-system">
                  <div class="panel-header">
                    <Icon name="ph:gear-six" size="16" />
                    <span>SYSTEM INFORMATION</span>
                  </div>
                  <div class="panel-content">
                    <div class="data-grid">
                      <div class="data-item">
                        <div class="data-icon" style="--icon-color: #3b82f6;">
                          <Icon name="ph:fingerprint" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">Device ID</span>
                          <code class="data-value data-id">{{ device.id }}</code>
                        </div>
                      </div>
                      <div class="data-item">
                        <div class="data-icon" style="--icon-color: #8b5cf6;">
                          <Icon name="ph:tag" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">Name</span>
                          <span class="data-value">{{ device.name }}</span>
                        </div>
                      </div>
                      <div class="data-item">
                        <div class="data-icon" style="--icon-color: #f59e0b;">
                          <Icon name="ph:factory" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">Manufacturer</span>
                          <span class="data-value">{{ device.manufacturer }}</span>
                        </div>
                      </div>
                      <div class="data-item">
                        <div class="data-icon" style="--icon-color: #10b981;">
                          <Icon name="ph:device-mobile" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">Model</span>
                          <span class="data-value">{{ device.model }}</span>
                        </div>
                      </div>
                      <div class="data-item">
                        <div class="data-icon" style="--icon-color: #06b6d4;">
                          <Icon name="ph:cpu" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">Platform</span>
                          <span class="data-value capitalize">{{ device.platform }} {{ device.osVersion }}</span>
                        </div>
                      </div>
                      <div class="data-item" v-if="liveInfo?.cpuAbi">
                        <div class="data-icon" style="--icon-color: #ec4899;">
                          <Icon name="ph:code" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">CPU ABI</span>
                          <span class="data-value">{{ liveInfo.cpuAbi }}</span>
                        </div>
                      </div>
                      <div class="data-item" v-if="liveInfo?.securityPatch">
                        <div class="data-icon" style="--icon-color: #ef4444;">
                          <Icon name="ph:shield-check" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">Security Patch</span>
                          <span class="data-value">{{ liveInfo.securityPatch }}</span>
                        </div>
                      </div>
                      <div class="data-item" v-if="liveInfo?.buildType">
                        <div class="data-icon" style="--icon-color: #6366f1;">
                          <Icon name="ph:hammer" size="18" />
                        </div>
                        <div class="data-info">
                          <span class="data-key">Build Type</span>
                          <span class="data-value">{{ liveInfo.buildType }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Right: Runtime Info -->
                <div class="data-panel panel-runtime">
                  <div class="panel-header">
                    <Icon name="ph:clock-countdown" size="16" />
                    <span>RUNTIME STATUS</span>
                  </div>
                  <div class="panel-content">
                    <div class="runtime-grid">
                      <div class="runtime-item">
                        <div class="runtime-icon" style="--icon-color: #10b981;">
                          <Icon name="ph:timer" size="20" />
                        </div>
                        <div class="runtime-data">
                          <span class="runtime-label">Uptime</span>
                          <span class="runtime-value">{{ liveInfo ? formatUptime(liveInfo.uptimeMillis) : '—' }}</span>
                        </div>
                      </div>
                      <div class="runtime-item">
                        <div class="runtime-icon" style="--icon-color: #3b82f6;">
                          <Icon name="ph:globe" size="20" />
                        </div>
                        <div class="runtime-data">
                          <span class="runtime-label">Timezone</span>
                          <span class="runtime-value">{{ liveInfo?.timezone || '—' }}</span>
                        </div>
                      </div>
                      <div class="runtime-item">
                        <div class="runtime-icon" style="--icon-color: #f59e0b;">
                          <Icon name="ph:translate" size="20" />
                        </div>
                        <div class="runtime-data">
                          <span class="runtime-label">Locale</span>
                          <span class="runtime-value">{{ liveInfo?.locale || '—' }}</span>
                        </div>
                      </div>
                      <div class="runtime-item">
                        <div class="runtime-icon" style="--icon-color: #8b5cf6;">
                          <Icon name="ph:calendar-check" size="20" />
                        </div>
                        <div class="runtime-data">
                          <span class="runtime-label">Registered</span>
                          <span class="runtime-value">{{ formatDate(device.createdAt) }}</span>
                        </div>
                      </div>
                      <div class="runtime-item">
                        <div class="runtime-icon" style="--icon-color: #06b6d4;">
                          <Icon name="ph:eye" size="20" />
                        </div>
                        <div class="runtime-data">
                          <span class="runtime-label">Last Seen</span>
                          <span class="runtime-value">{{ getTimeSince(device.lastSeen) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Refresh Action Bar -->
              <div class="telemetry-actions">
                <div class="action-status">
                  <div class="status-indicator" :class="device.online ? 'is-live' : 'is-cached'">
                    <div class="status-dot"></div>
                    <span>{{ device.online ? 'LIVE TELEMETRY' : 'CACHED DATA' }}</span>
                  </div>
                </div>
                <button
                  v-if="device.online && device.status === 'approved'"
                  class="refresh-telemetry-btn"
                  :disabled="loadingLiveInfo"
                  @click="fetchLiveInfo"
                >
                  <Icon name="ph:arrows-clockwise" size="16" :class="{ 'animate-spin': loadingLiveInfo }" />
                  <span>{{ loadingLiveInfo ? 'SYNCING...' : 'REFRESH TELEMETRY' }}</span>
                </button>
              </div>
            </div>

            <!-- Control CTA Card -->
            <div class="control-cta-card">
              <div class="cta-glow"></div>
              <div class="cta-content">
                <div class="cta-icon">
                  <Icon name="ph:game-controller" size="32" />
                </div>
                <div class="cta-text">
                  <h3 class="cta-title">Device Control</h3>
                  <p class="cta-description">
                    Execute commands and control device remotely via MCP tools
                  </p>
                </div>
                <NuxtLink
                  :to="`/devices/${deviceId}/control`"
                  class="cta-button"
                  :class="{ 'cta-disabled': !device?.online }"
                  :disabled="!device?.online"
                >
                  <span>Open Control Panel</span>
                  <Icon name="ph:arrow-right" size="20" />
                </NuxtLink>
              </div>
              <div v-if="!device?.online" class="cta-offline-overlay">
                <Icon name="ph:warning" size="20" />
                <span>Device must be online to access controls</span>
              </div>
            </div>

            <!-- Datastream Terminal -->
            <div class="datastream-panel">
              <!-- Terminal Header -->
              <div class="terminal-chrome">
                <div class="chrome-left">
                  <div class="chrome-title">
                    <Icon name="ph:terminal-window" size="14" />
                    <span>device_{{ device.id.slice(0, 8) }}_logs</span>
                  </div>
                </div>
                <div class="chrome-center">
                  <div class="stream-indicator" :class="{ 'is-streaming': logs.length > 0 }">
                    <span class="stream-dot"></span>
                    <span>DATASTREAM {{ logs.length > 0 ? 'ACTIVE' : 'IDLE' }}</span>
                  </div>
                </div>
                <div class="chrome-right">
                  <span class="log-counter">{{ logs.length }} entries</span>
                  <label class="autoscroll-control">
                    <input v-model="autoScroll" type="checkbox" class="sr-only" />
                    <div class="autoscroll-toggle" :class="{ 'is-active': autoScroll }">
                      <Icon name="ph:arrow-line-down" size="14" />
                    </div>
                    <span class="autoscroll-label">AUTO</span>
                  </label>
                </div>
              </div>

              <!-- Log Stream -->
              <div ref="logsContainer" class="log-stream">
                <!-- Empty State -->
                <div v-if="logs.length === 0" class="stream-empty">
                  <div class="empty-visual">
                    <div class="empty-ring"></div>
                    <Icon name="ph:broadcast" size="32" />
                  </div>
                  <div class="empty-text">
                    <span class="empty-title">Awaiting Data</span>
                    <span class="empty-desc">Log entries will stream here in real-time</span>
                  </div>
                  <div class="empty-cursor">_</div>
                </div>

                <!-- Log Entries -->
                <div v-else class="log-entries">
                  <div
                    v-for="(log, index) in [...logs].reverse()"
                    :key="log.id"
                    class="log-line"
                    :class="'log-level-' + log.level"
                    :style="{ '--delay': index * 0.02 + 's' }"
                  >
                    <span class="log-timestamp">{{ formatLogTime(log.timestamp) }}</span>
                    <span class="log-level-badge">
                      <span class="level-icon">{{ getLevelIcon(log.level) }}</span>
                      <span class="level-text">{{ log.level.toUpperCase() }}</span>
                    </span>
                    <span class="log-message">{{ log.message }}</span>
                  </div>
                </div>

                <!-- Scanline Effect -->
                <div class="stream-scanline"></div>
              </div>

              <!-- Terminal Footer -->
              <div class="terminal-footer">
                <div class="footer-prompt">
                  <span class="prompt-char">❯</span>
                  <span class="prompt-cursor"></span>
                </div>
                <div class="footer-stats">
                  <span class="stat">
                    <Icon name="ph:clock" size="12" />
                    Polling: 3s
                  </span>
                  <span class="stat">
                    <Icon name="ph:database" size="12" />
                    Buffer: {{ logs.length }}/50
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "tailwindcss";

/* ========================================
   MISSION CONTROL - BASE LAYOUT
   ======================================== */
.device-detail-page {
  position: relative;
  height: 100vh;
  background: #080c14;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Main content wrapper - targets the Tailwind divs */
.device-detail-page > .relative {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  width: 100%;
}

.device-detail-page > .relative > div {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  width: 100%;
}

/* Animated Grid Background */
.grid-bg {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(34, 211, 238, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(34, 211, 238, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: grid-scroll 20s linear infinite;
  pointer-events: none;
}

@keyframes grid-scroll {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.ambient-glow {
  position: fixed;
  top: -300px;
  left: 50%;
  transform: translateX(-50%);
  width: 1000px;
  height: 600px;
  border-radius: 100%;
  filter: blur(120px);
  opacity: 0.12;
  pointer-events: none;
  transition: background 0.8s ease;
}

.glow-online {
  background: radial-gradient(ellipse, #10b981 0%, #059669 30%, transparent 70%);
}

.glow-offline {
  background: radial-gradient(ellipse, #475569 0%, #334155 30%, transparent 70%);
}

/* ========================================
   HEADER BAR
   ======================================== */
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 4px;
  backdrop-filter: blur(12px);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--color-terminal-muted);
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 4px;
  transition: all 0.2s ease;
}

.back-link:hover {
  color: var(--color-primary);
  border-color: rgba(34, 211, 238, 0.3);
  background: rgba(34, 211, 238, 0.05);
}

.header-divider {
  width: 1px;
  height: 20px;
  background: rgba(148, 163, 184, 0.15);
}

.breadcrumb-id {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-terminal-dim);
  padding: 4px 8px;
  background: rgba(34, 211, 238, 0.05);
  border-radius: 2px;
}

.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.connection-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  border-radius: 20px;
  border: 1px solid;
}

.connection-indicator.is-online {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
}

.connection-indicator.is-offline {
  color: var(--color-terminal-dim);
  background: rgba(148, 163, 184, 0.05);
  border-color: rgba(148, 163, 184, 0.1);
}

.connection-indicator .indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.is-online .indicator-dot {
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; box-shadow: 0 0 0 0 currentColor; }
  50% { opacity: 0.8; box-shadow: 0 0 0 4px transparent; }
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-terminal-muted);
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: rgba(34, 211, 238, 0.3);
  background: rgba(34, 211, 238, 0.05);
}

.icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ========================================
   ERROR & LOADING STATES
   ======================================== */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  text-align: center;
}

.error-glitch {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.3em;
  color: #ef4444;
  position: relative;
  animation: glitch 2s infinite;
}

@keyframes glitch {
  0%, 90%, 100% { transform: translate(0); }
  92% { transform: translate(-2px, 1px); }
  94% { transform: translate(2px, -1px); }
  96% { transform: translate(-1px, 2px); }
  98% { transform: translate(1px, -2px); }
}

.error-code {
  font-size: 120px;
  font-weight: 900;
  line-height: 1;
  color: transparent;
  -webkit-text-stroke: 2px rgba(239, 68, 68, 0.3);
  margin: 20px 0;
}

.error-message {
  font-size: 14px;
  color: var(--color-terminal-muted);
  margin-bottom: 30px;
}

.error-back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-terminal-text);
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 4px;
  transition: all 0.2s ease;
}

.error-back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
}

.loading-rings {
  position: relative;
  width: 120px;
  height: 120px;
}

.ring {
  position: absolute;
  inset: 0;
  border: 2px solid transparent;
  border-radius: 50%;
}

.ring-1 {
  border-top-color: var(--color-primary);
  animation: spin 1.5s linear infinite;
}

.ring-2 {
  inset: 10px;
  border-right-color: #10b981;
  animation: spin 2s linear infinite reverse;
}

.ring-3 {
  inset: 20px;
  border-bottom-color: #8b5cf6;
  animation: spin 2.5s linear infinite;
}

.loading-core {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 50%;
  animation: core-pulse 1.5s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes core-pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
  50% { transform: translate(-50%, -50%) scale(0.8); opacity: 0.6; }
}

.loading-text {
  margin-top: 30px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.15em;
  color: var(--color-terminal-muted);
}

.loading-dots {
  color: var(--color-primary);
  animation: dots 1.5s steps(4, end) infinite;
}

@keyframes dots {
  0% { content: ''; }
  25% { content: '.'; }
  50% { content: '..'; }
  75% { content: '...'; }
}

/* ========================================
   IDENTITY BANNER
   ======================================== */
.identity-banner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.9) 100%);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.identity-main {
  display: flex;
  align-items: center;
  gap: 20px;
  z-index: 1;
}

.device-icon-wrapper {
  position: relative;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  color: var(--color-terminal-muted);
  transition: all 0.3s ease;
}

.device-icon-wrapper.icon-online {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.25);
  color: #10b981;
}

.icon-ring {
  position: absolute;
  inset: -2px;
  border: 1px solid currentColor;
  border-radius: 18px;
  opacity: 0.3;
}

.icon-pulse {
  position: absolute;
  inset: -6px;
  border: 2px solid currentColor;
  border-radius: 22px;
  animation: icon-pulse 2s ease-out infinite;
}

@keyframes icon-pulse {
  0% { transform: scale(1); opacity: 0.4; }
  100% { transform: scale(1.2); opacity: 0; }
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.device-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-terminal-text-bright);
  letter-spacing: -0.02em;
}

.status-chips {
  display: flex;
  gap: 6px;
}

.status-chip {
  padding: 4px 10px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-radius: 4px;
  border: 1px solid;
}

.chip-approved {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
}

.chip-pending {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.3);
}

.chip-rejected {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
}

.device-specs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--color-terminal-muted);
}

.spec-divider {
  color: var(--color-terminal-dim);
  font-size: 8px;
}

.identity-actions {
  display: flex;
  gap: 8px;
  z-index: 1;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  border-radius: 4px;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-approve {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
}

.action-approve:hover {
  background: rgba(16, 185, 129, 0.2);
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.2);
}

.action-reject {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
}

.action-reject:hover {
  background: rgba(239, 68, 68, 0.2);
  box-shadow: 0 0 20px rgba(239, 68, 68, 0.2);
}

.action-files {
  color: #22d3ee;
  background: rgba(34, 211, 238, 0.1);
  border-color: rgba(34, 211, 238, 0.3);
  text-decoration: none;
}

.action-files:hover {
  background: rgba(34, 211, 238, 0.2);
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.2);
  color: #22d3ee;
}

.banner-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.deco-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.3), transparent);
}

.deco-corner {
  position: absolute;
  width: 60px;
  height: 60px;
  border: 1px solid rgba(34, 211, 238, 0.1);
}

.deco-corner-tr {
  top: -30px;
  right: -30px;
  border-radius: 50%;
}

.deco-corner-br {
  bottom: -30px;
  right: 60px;
  border-radius: 50%;
}

/* ========================================
   STATION NAVIGATION
   ======================================== */
.station-nav {
  position: relative;
  flex-shrink: 0;
}

.station-track {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 6px;
}

.station-btn {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

.station-btn:hover {
  background: rgba(148, 163, 184, 0.05);
}

.station-btn.station-active {
  background: rgba(34, 211, 238, 0.06);
  border-color: rgba(34, 211, 238, 0.2);
}

.station-index {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-terminal-dim);
  opacity: 0.5;
}

.station-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 8px;
  color: var(--color-terminal-muted);
  transition: all 0.25s ease;
}

.station-active .station-icon {
  background: rgba(34, 211, 238, 0.1);
  border-color: rgba(34, 211, 238, 0.3);
  color: var(--color-primary);
}

.station-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.station-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-terminal-muted);
  transition: color 0.25s ease;
}

.station-active .station-label {
  color: var(--color-primary);
}

.station-desc {
  font-size: 10px;
  color: var(--color-terminal-dim);
}

.station-badge {
  margin-left: auto;
  padding: 3px 8px;
  font-size: 10px;
  font-weight: 700;
  color: var(--color-primary);
  background: rgba(34, 211, 238, 0.1);
  border-radius: 10px;
}

.station-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: var(--color-primary);
  transition: width 0.25s ease;
  border-radius: 1px;
}

.station-active .station-indicator {
  width: 60%;
}

/* ========================================
   TAB CONTENT WRAPPER
   ======================================== */
.tab-content-wrapper {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 6px;
  overflow: visible;
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* ========================================
   TELEMETRY PANEL (INFO TAB)
   ======================================== */
.telemetry-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Metrics Row */
.metrics-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.metric-card {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.metric-card:hover {
  border-color: rgba(148, 163, 184, 0.2);
  transform: translateY(-2px);
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: rgba(148, 163, 184, 0.03);
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
}

.metric-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.15);
  border-radius: 8px;
  color: var(--color-primary);
}

.metric-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--color-terminal-muted);
}

.metric-badge {
  margin-left: auto;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 700;
  border-radius: 4px;
}

.metric-badge.gauge-healthy {
  color: #10b981;
  background: rgba(16, 185, 129, 0.15);
}

.metric-badge.gauge-warning {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.15);
}

.metric-badge.gauge-critical {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.15);
}

.metric-badge-info {
  color: var(--color-primary);
  background: rgba(34, 211, 238, 0.15);
}

.metric-body {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.metric-placeholder {
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--color-terminal-dim);
  font-size: 12px;
}

.placeholder-pulse {
  width: 60px;
  height: 60px;
  border: 2px solid rgba(148, 163, 184, 0.1);
  border-radius: 50%;
  animation: placeholder-pulse 2s ease-in-out infinite;
}

@keyframes placeholder-pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.05); opacity: 0.8; }
}

/* Radial Gauge */
.radial-gauge {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.radial-gauge svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.gauge-bg {
  fill: none;
  stroke: rgba(148, 163, 184, 0.1);
  stroke-width: 8;
}

.gauge-progress {
  fill: none;
  stroke: var(--gauge-color, #10b981);
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dasharray: 327;
  stroke-dashoffset: calc(327 - (327 * var(--progress, 0)));
  transition: stroke-dashoffset 1s ease-out;
  filter: drop-shadow(0 0 6px var(--gauge-color, #10b981));
}

.gauge-inner {
  fill: rgba(15, 23, 42, 0.8);
  stroke: none;
}

.gauge-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: baseline;
}

.value-num {
  font-size: 24px;
  font-weight: 800;
  color: var(--color-terminal-text-bright);
  font-variant-numeric: tabular-nums;
}

.value-unit {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-terminal-dim);
  margin-left: 1px;
}

/* Metric Stats */
.metric-stats {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(148, 163, 184, 0.03);
  border-radius: 4px;
}

.stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.stat-used { background: #f59e0b; }
.stat-used-storage { background: #8b5cf6; }
.stat-free { background: #10b981; }

.stat-row .stat-label {
  font-size: 11px;
  color: var(--color-terminal-muted);
}

.stat-row .stat-value {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-terminal-text-bright);
  font-variant-numeric: tabular-nums;
}

.stat-total {
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  padding-top: 12px;
  margin-top: 4px;
}

/* Display Body */
.display-body {
  flex-direction: column;
  gap: 16px;
}

.display-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.display-frame {
  width: 80px;
  max-height: 140px;
  padding: 4px;
  background: linear-gradient(145deg, #1e293b, #0f172a);
  border: 2px solid rgba(148, 163, 184, 0.15);
  border-radius: 6px;
}

.display-screen {
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #0a0f1a 0%, #0f172a 100%);
  border-radius: 3px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.screen-glare {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 30%;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03) 0%, transparent 100%);
}

.screen-content {
  position: relative;
  z-index: 1;
}

.res-text {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-primary);
  text-shadow: 0 0 10px rgba(34, 211, 238, 0.5);
}

.display-stand .stand-neck {
  width: 16px;
  height: 12px;
  background: linear-gradient(180deg, #334155, #1e293b);
  margin: 0 auto;
}

.display-stand .stand-base {
  width: 40px;
  height: 6px;
  background: linear-gradient(180deg, #475569, #334155);
  border-radius: 0 0 4px 4px;
  margin: 0 auto;
}

.display-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.spec-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(148, 163, 184, 0.05);
  border-radius: 4px;
  font-size: 11px;
  color: var(--color-terminal-muted);
}

.megapixel-tag {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 4px;
}

/* Battery Body */
.battery-body {
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.battery-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.battery-cap {
  width: 20px;
  height: 6px;
  background: rgba(148, 163, 184, 0.2);
  border-radius: 2px 2px 0 0;
}

.battery-body-shell {
  width: 50px;
  height: 80px;
  background: rgba(148, 163, 184, 0.1);
  border: 2px solid rgba(148, 163, 184, 0.2);
  border-radius: 4px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.battery-level {
  background: linear-gradient(180deg, #22c55e, #10b981);
  border-radius: 2px;
  transition: height 0.5s ease;
}

.battery-stats {
  text-align: center;
}

.battery-capacity .cap-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--color-terminal-text-bright);
}

.battery-capacity .cap-unit {
  font-size: 12px;
  color: var(--color-terminal-dim);
  margin-left: 4px;
}

/* Data Panels */
.data-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 1024px) {
  .data-panels {
    grid-template-columns: 1fr;
  }
}

.data-panel {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(148, 163, 184, 0.03);
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--color-terminal-muted);
}

.panel-content {
  padding: 16px;
}

.data-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.data-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: rgba(148, 163, 184, 0.03);
  border-radius: 6px;
  transition: all 0.2s ease;
}

.data-item:hover {
  background: rgba(148, 163, 184, 0.06);
}

.data-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--icon-color) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--icon-color) 20%, transparent);
  border-radius: 8px;
  color: var(--icon-color);
  flex-shrink: 0;
}

.data-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.data-key {
  font-size: 10px;
  color: var(--color-terminal-dim);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.data-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-terminal-text-bright);
}

.data-id {
  font-size: 11px;
  font-family: 'Courier New', monospace;
  padding: 4px 8px;
  background: rgba(59, 130, 246, 0.12);
  border-radius: 4px;
  color: #3b82f6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Runtime Grid */
.runtime-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.runtime-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: rgba(148, 163, 184, 0.03);
  border-radius: 6px;
  transition: all 0.2s ease;
}

.runtime-item:hover {
  background: rgba(148, 163, 184, 0.06);
}

.runtime-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--icon-color) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--icon-color) 20%, transparent);
  border-radius: 8px;
  color: var(--icon-color);
  flex-shrink: 0;
}

.runtime-data {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.runtime-label {
  font-size: 10px;
  color: var(--color-terminal-dim);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.runtime-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-terminal-text-bright);
}

/* Telemetry Actions */
.telemetry-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 6px;
}

.action-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.status-indicator .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-indicator.is-live {
  color: #10b981;
}

.status-indicator.is-live .status-dot {
  background: #10b981;
  animation: live-pulse 2s ease-in-out infinite;
}

.status-indicator.is-cached {
  color: var(--color-terminal-dim);
}

.status-indicator.is-cached .status-dot {
  background: var(--color-terminal-dim);
}

@keyframes live-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
}

.refresh-telemetry-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--color-primary);
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.25);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-telemetry-btn:hover:not(:disabled) {
  background: rgba(34, 211, 238, 0.12);
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.15);
}

.refresh-telemetry-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.control-cta-card {
  @apply relative rounded-xl border border-violet-500/30 bg-gradient-to-br from-violet-950/40 to-indigo-950/40
    backdrop-blur-sm overflow-hidden transition-all hover:border-violet-500/50 hover:shadow-lg hover:shadow-violet-500/20;
  margin: 20px 20px 20px 20px;
}

.cta-glow {
  @apply absolute inset-0 opacity-0 transition-opacity;
  background: radial-gradient(circle at 50% 0%, rgba(139, 92, 246, 0.15), transparent 70%);
}

.control-cta-card:hover .cta-glow {
  @apply opacity-100;
}

.cta-content {
  @apply relative z-10 p-6 flex items-center gap-6;
}

.cta-icon {
  @apply flex-shrink-0 w-16 h-16 rounded-xl bg-violet-500/20 flex items-center justify-center
    border border-violet-500/30 text-violet-400;
}

.cta-text {
  @apply flex-1;
}

.cta-title {
  @apply text-xl font-bold text-white mb-1;
}

.cta-description {
  @apply text-sm text-gray-400;
}

.cta-button {
  @apply flex items-center gap-2 px-6 py-3 rounded-lg bg-violet-600 hover:bg-violet-500
    text-white font-semibold transition-all hover:gap-3 hover:shadow-lg hover:shadow-violet-500/30;
}

.cta-disabled {
  @apply opacity-50 cursor-not-allowed pointer-events-none;
}

.cta-offline-overlay {
  @apply absolute inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center gap-2
    text-amber-400 text-sm font-medium;
}

@media (max-width: 768px) {
  .cta-content {
    @apply flex-col items-start gap-4;
  }

  .cta-button {
    @apply w-full justify-center;
  }
}

/* ========================================
   DATASTREAM PANEL (LOGS TAB)
   ======================================== */
.datastream-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 400px;
}

.terminal-chrome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: rgba(15, 23, 42, 0.95);
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  flex-shrink: 0;
}

.chrome-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chrome-dots {
  display: flex;
  gap: 6px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot-red { background: #ff5f57; }
.dot-yellow { background: #febc2e; }
.dot-green { background: #28c840; }

.chrome-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--color-terminal-dim);
}

.chrome-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.stream-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: var(--color-terminal-dim);
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 20px;
}

.stream-indicator.is-streaming {
  color: #10b981;
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.2);
}

.stream-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.is-streaming .stream-dot {
  animation: stream-pulse 1.5s ease-in-out infinite;
}

@keyframes stream-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.chrome-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.log-counter {
  font-size: 10px;
  color: var(--color-terminal-dim);
  padding: 4px 10px;
  background: rgba(148, 163, 184, 0.05);
  border-radius: 4px;
}

.autoscroll-control {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.autoscroll-toggle {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 4px;
  color: var(--color-terminal-dim);
  transition: all 0.2s ease;
}

.autoscroll-toggle.is-active {
  background: rgba(34, 211, 238, 0.1);
  border-color: rgba(34, 211, 238, 0.3);
  color: var(--color-primary);
}

.autoscroll-label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--color-terminal-dim);
}

/* Log Stream */
.log-stream {
  overflow-y: auto;
  background: #0a0e17;
  position: relative;
  flex: 1;
  min-height: 0;
}

.stream-scanline {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.3), transparent);
  animation: scanline 4s linear infinite;
  pointer-events: none;
}

@keyframes scanline {
  0% { top: 0; opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

.stream-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.empty-visual {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-terminal-dim);
}

.empty-ring {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(148, 163, 184, 0.1);
  border-radius: 50%;
  animation: empty-ring-pulse 3s ease-in-out infinite;
}

@keyframes empty-ring-pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.2; }
}

.empty-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.empty-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-terminal-muted);
}

.empty-desc {
  font-size: 11px;
  color: var(--color-terminal-dim);
}

.empty-cursor {
  font-size: 14px;
  color: var(--color-primary);
  animation: cursor-blink 1s step-end infinite;
}

@keyframes cursor-blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* Log Entries */
.log-entries {
  padding: 8px 0;
}

.log-line {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  font-size: 12px;
  border-left: 2px solid transparent;
  transition: all 0.15s ease;
  animation: log-appear 0.3s ease-out;
  animation-delay: var(--delay, 0s);
  animation-fill-mode: backwards;
}

@keyframes log-appear {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.log-line:hover {
  background: rgba(148, 163, 184, 0.03);
}

.log-level-error {
  border-left-color: #ef4444;
  background: rgba(239, 68, 68, 0.03);
}

.log-level-warn {
  border-left-color: #f59e0b;
  background: rgba(245, 158, 11, 0.02);
}

.log-timestamp {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-terminal-dim);
  min-width: 70px;
  font-variant-numeric: tabular-nums;
}

.log-level-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 60px;
}

.level-icon {
  font-size: 10px;
}

.level-text {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.log-level-debug .log-level-badge { color: var(--color-terminal-dim); }
.log-level-info .log-level-badge { color: var(--color-primary); }
.log-level-warn .log-level-badge { color: #f59e0b; }
.log-level-error .log-level-badge { color: #ef4444; }

.log-message {
  flex: 1;
  color: var(--color-terminal-text);
  word-break: break-all;
}

/* Terminal Footer */
.terminal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: rgba(15, 23, 42, 0.95);
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  flex-shrink: 0;
}

.footer-prompt {
  display: flex;
  align-items: center;
  gap: 4px;
}

.prompt-char {
  color: var(--color-primary);
  font-size: 12px;
}

.prompt-cursor {
  width: 8px;
  height: 14px;
  background: var(--color-primary);
  animation: cursor-blink 1s step-end infinite;
}

.footer-stats {
  display: flex;
  gap: 16px;
}

.footer-stats .stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: var(--color-terminal-dim);
}

/* ========================================
   CONTROL PANEL (COMMANDS TAB)
   ======================================== */
.control-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.control-layout {
  display: grid;
  grid-template-columns: 280px 1fr 200px;
  gap: 0;
  flex: 1;
  min-height: 0;
}

@media (max-width: 1200px) {
  .control-layout {
    grid-template-columns: 240px 1fr;
  }
  .history-sidebar {
    display: none;
  }
}

@media (max-width: 768px) {
  .control-layout {
    grid-template-columns: 1fr;
    height: auto;
  }
  .command-sidebar {
    max-height: 300px;
  }
}

/* Command Sidebar */
.command-sidebar {
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.95);
  border-right: 1px solid rgba(148, 163, 184, 0.1);
  overflow: hidden;
  min-height: 0;
}

.sidebar-search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.search-icon {
  color: var(--color-terminal-dim);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 12px;
  color: var(--color-terminal-text);
}

.search-input::placeholder {
  color: var(--color-terminal-dim);
}

.search-hint {
  padding: 2px 6px;
  font-size: 10px;
  color: var(--color-terminal-dim);
  background: rgba(148, 163, 184, 0.1);
  border-radius: 3px;
}

.command-list {
  overflow-y: auto;
  padding: 8px 0;
  flex: 1;
  min-height: 0;
}

.command-category {
  margin-bottom: 4px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--color-terminal-dim);
  text-transform: uppercase;
}

.category-count {
  margin-left: auto;
  padding: 1px 6px;
  font-size: 9px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 8px;
}

.category-commands {
  display: flex;
  flex-direction: column;
}

.command-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px 10px 28px;
  font-size: 12px;
  color: var(--color-terminal-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
}

.command-item:hover {
  background: rgba(148, 163, 184, 0.05);
  color: var(--color-terminal-text);
}

.command-item.is-selected {
  background: rgba(34, 211, 238, 0.08);
  color: var(--color-primary);
  border-right: 2px solid var(--color-primary);
}

.command-name {
  font-family: var(--font-mono);
}

.command-arrow {
  opacity: 0;
  transition: opacity 0.15s ease;
}

.command-item:hover .command-arrow,
.command-item.is-selected .command-arrow {
  opacity: 1;
}

.sidebar-stats {
  display: flex;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  flex-shrink: 0;
}

.sidebar-stats .stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 12px;
}

.sidebar-stats .stat-item + .stat-item {
  border-left: 1px solid rgba(148, 163, 184, 0.1);
}

.sidebar-stats .stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.sidebar-stats .stat-label {
  font-size: 9px;
  color: var(--color-terminal-dim);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Command Main */
.command-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.command-form-section {
  flex-shrink: 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  overflow-y: auto;
}

.command-form {
  padding: 20px;
}

.form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.form-title {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.command-code {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.command-desc {
  font-size: 12px;
  color: var(--color-terminal-muted);
  line-height: 1.5;
}

.form-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 4px;
  color: var(--color-terminal-dim);
  cursor: pointer;
  transition: all 0.2s ease;
}

.form-close:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.form-params {
  margin-bottom: 20px;
}

.params-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-terminal-dim);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.params-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.param-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
}

.param-name {
  color: var(--color-terminal-text);
  font-weight: 500;
}

.param-required {
  color: #ef4444;
  font-size: 10px;
}

.param-type {
  margin-left: auto;
  font-size: 9px;
  color: var(--color-terminal-dim);
  padding: 2px 6px;
  background: rgba(148, 163, 184, 0.05);
  border-radius: 3px;
}

.param-input,
.param-select,
.param-textarea {
  padding: 10px 12px;
  font-size: 12px;
  font-family: var(--font-mono);
  color: var(--color-terminal-text);
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 4px;
  outline: none;
  transition: all 0.2s ease;
}

.param-input:focus,
.param-select:focus,
.param-textarea:focus {
  border-color: rgba(34, 211, 238, 0.4);
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.1);
}

.param-input::placeholder,
.param-textarea::placeholder {
  color: var(--color-terminal-dim);
}

.param-input-wrap {
  position: relative;
}

.param-select {
  width: 100%;
  appearance: none;
  cursor: pointer;
}

.select-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-terminal-dim);
  pointer-events: none;
}

.param-textarea {
  resize: none;
}

/* Vibration Pattern Input */
.vibrate-pattern-input {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.preset-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--color-terminal-muted);
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.preset-btn:hover {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(34, 211, 238, 0.3);
  color: var(--color-terminal-text);
}

.preset-btn.is-active {
  background: rgba(34, 211, 238, 0.1);
  border-color: rgba(34, 211, 238, 0.5);
  color: var(--color-terminal-accent);
}

.preset-btn.is-active :deep(svg) {
  color: var(--color-terminal-accent);
}

.custom-pattern-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.custom-label {
  font-size: 10px;
  font-family: var(--font-mono);
  color: var(--color-terminal-dim);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.current-pattern {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 4px;
  font-size: 11px;
}

.pattern-label {
  color: var(--color-terminal-dim);
  font-family: var(--font-mono);
}

.pattern-value {
  color: var(--color-terminal-accent);
  font-family: var(--font-mono);
  font-size: 11px;
}

.param-toggle-wrap {
  display: flex;
}

.param-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.param-toggle.is-on {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
}

.toggle-track {
  width: 32px;
  height: 18px;
  background: rgba(148, 163, 184, 0.2);
  border-radius: 9px;
  position: relative;
  transition: background 0.2s ease;
}

.param-toggle.is-on .toggle-track {
  background: rgba(16, 185, 129, 0.3);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  background: var(--color-terminal-muted);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.param-toggle.is-on .toggle-thumb {
  left: 16px;
  background: #10b981;
}

.toggle-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-terminal-dim);
}

.param-toggle.is-on .toggle-label {
  color: #10b981;
}

.param-hint {
  font-size: 10px;
  color: var(--color-terminal-dim);
  font-style: italic;
}

.no-params {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  background: rgba(16, 185, 129, 0.05);
  border: 1px solid rgba(16, 185, 129, 0.15);
  border-radius: 4px;
  font-size: 12px;
  color: #10b981;
  margin-bottom: 20px;
}

.execute-btn {
  position: relative;
  width: 100%;
  padding: 14px 20px;
  background: rgba(34, 211, 238, 0.08);
  border: 1px solid rgba(34, 211, 238, 0.25);
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
}

.execute-btn:hover:not(:disabled) {
  background: rgba(34, 211, 238, 0.12);
  border-color: rgba(34, 211, 238, 0.4);
  box-shadow: 0 0 30px rgba(34, 211, 238, 0.15);
}

.execute-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.execute-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.1), transparent);
  transform: translateX(-100%);
}

.execute-btn.is-executing .execute-bg {
  animation: execute-sweep 1.5s ease-in-out infinite;
}

@keyframes execute-sweep {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.execute-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--color-primary);
}

.execute-disabled-reason {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: var(--color-terminal-dim);
}

.form-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-illustration {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-terminal-dim);
  margin-bottom: 20px;
}

.illustration-ring {
  position: absolute;
  inset: 0;
  border: 2px dashed rgba(148, 163, 184, 0.15);
  border-radius: 50%;
  animation: illustration-spin 20s linear infinite;
}

@keyframes illustration-spin {
  to { transform: rotate(360deg); }
}

.form-empty h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-terminal-text);
  margin-bottom: 8px;
}

.form-empty p {
  font-size: 12px;
  color: var(--color-terminal-dim);
}

/* Output Section */
.output-section {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.output-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: rgba(15, 23, 42, 0.8);
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  flex-shrink: 0;
}

.output-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-terminal-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.output-actions {
  display: flex;
  gap: 8px;
}

.output-action {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 10px;
  color: var(--color-terminal-dim);
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.output-action:hover {
  color: var(--color-primary);
  border-color: rgba(34, 211, 238, 0.3);
}

.output-console {
  overflow-y: auto;
  background: #0a0e17;
  padding: 16px;
  flex: 1;
  min-height: 0;
}

.console-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.console-prompt {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.prompt-symbol {
  color: var(--color-primary);
}

.prompt-text {
  color: var(--color-terminal-dim);
}

.prompt-cursor {
  width: 8px;
  height: 16px;
  background: var(--color-primary);
  animation: cursor-blink 1s step-end infinite;
}

.console-executing {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.executing-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.exec-symbol {
  color: var(--color-primary);
}

.exec-command {
  color: var(--color-terminal-text);
  font-family: var(--font-mono);
}

.executing-progress {
  height: 2px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 1px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  width: 30%;
  background: var(--color-primary);
  animation: progress-slide 1s ease-in-out infinite;
}

@keyframes progress-slide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(400%); }
}

.executing-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--color-terminal-dim);
}

.console-result {
  animation: result-appear 0.3s ease-out;
}

@keyframes result-appear {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.result-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.result-symbol {
  font-size: 14px;
}

.console-result:not(.is-error) .result-symbol { color: #10b981; }
.console-result.is-error .result-symbol { color: #ef4444; }

.result-command {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-terminal-text);
}

.result-status {
  margin-left: auto;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
  padding: 3px 8px;
  border-radius: 3px;
}

.console-result:not(.is-error) .result-status {
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
}

.console-result.is-error .result-status {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-text {
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-terminal-text);
  white-space: pre-wrap;
  word-break: break-all;
}

.console-result.is-error .result-text {
  color: #fca5a5;
}

.result-image {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-wrapper {
  display: inline-block;
  padding: 4px;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.1);
  border-radius: 4px;
}

.image-wrapper img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 2px;
}

.image-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: var(--color-terminal-dim);
}

/* History Sidebar */
.history-sidebar {
  display: flex;
  flex-direction: column;
  background: rgba(15, 23, 42, 0.95);
  border-left: 1px solid rgba(148, 163, 184, 0.1);
  overflow: hidden;
  min-height: 0;
}

.history-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-terminal-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.history-list {
  overflow-y: auto;
  padding: 8px 0;
  flex: 1;
  min-height: 0;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  transition: background 0.15s ease;
}

.history-item:hover {
  background: rgba(148, 163, 184, 0.03);
}

.history-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.history-item.is-success .history-icon {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}

.history-item.is-error .history-icon {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.history-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.history-name {
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--color-terminal-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-time {
  font-size: 9px;
  color: var(--color-terminal-dim);
}

.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 16px;
  color: var(--color-terminal-dim);
  font-size: 11px;
}

/* ========================================
   RESPONSIVE
   ======================================== */
@media (max-width: 1200px) {
  .metrics-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .identity-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .identity-actions {
    width: 100%;
  }

  .action-btn {
    flex: 1;
    justify-content: center;
  }

  .station-track {
    flex-direction: column;
  }

  .station-btn {
    justify-content: flex-start;
  }

  .metrics-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .header-bar {
    flex-wrap: wrap;
    gap: 12px;
  }

  .header-center {
    position: relative;
    left: auto;
    transform: none;
    width: 100%;
    order: 3;
    justify-content: center;
    display: flex;
  }

  .device-name {
    font-size: 18px;
  }

  .device-specs {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .spec-divider {
    display: none;
  }

  .terminal-chrome {
    flex-wrap: wrap;
    gap: 10px;
  }

  .chrome-center {
    position: relative;
    left: auto;
    transform: none;
    order: 3;
    width: 100%;
    display: flex;
    justify-content: center;
  }

  .telemetry-actions {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .refresh-telemetry-btn {
    justify-content: center;
  }
}

/* Scrollbar Styling */
.telemetry-panel::-webkit-scrollbar,
.log-stream::-webkit-scrollbar,
.command-list::-webkit-scrollbar,
.output-console::-webkit-scrollbar,
.history-list::-webkit-scrollbar {
  width: 6px;
}

.telemetry-panel::-webkit-scrollbar-track,
.log-stream::-webkit-scrollbar-track,
.command-list::-webkit-scrollbar-track,
.output-console::-webkit-scrollbar-track,
.history-list::-webkit-scrollbar-track {
  background: transparent;
}

.telemetry-panel::-webkit-scrollbar-thumb,
.log-stream::-webkit-scrollbar-thumb,
.command-list::-webkit-scrollbar-thumb,
.output-console::-webkit-scrollbar-thumb,
.history-list::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.15);
  border-radius: 3px;
}

.telemetry-panel::-webkit-scrollbar-thumb:hover,
.log-stream::-webkit-scrollbar-thumb:hover,
.command-list::-webkit-scrollbar-thumb:hover,
.output-console::-webkit-scrollbar-thumb:hover,
.history-list::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.25);
}
</style>

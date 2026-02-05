<script setup lang="ts">
import type { Device } from '~/composables/useApi';

const route = useRoute();
const api = useApi();

const device = ref<Device | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

const deviceId = computed(() => route.params.id as string);

async function loadDevice() {
  loading.value = true;
  error.value = null;

  try {
    device.value = await api.getDevice(deviceId.value);
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load device';
  } finally {
    loading.value = false;
  }
}

// Poll for device status
let pollInterval: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  loadDevice();
  pollInterval = setInterval(loadDevice, 5000);
});

onUnmounted(() => {
  if (pollInterval) {
    clearInterval(pollInterval);
  }
});

// Page meta
useHead({
  title: computed(() => device.value ? `Files - ${device.value.name}` : 'File Browser'),
});
</script>

<template>
  <div class="files-page">
    <!-- Header -->
    <TerminalPageHeader description="Browse and manage device files" :show-refresh="false">
      <template #left>
        <NuxtLink :to="`/devices/${deviceId}`" class="back-btn">
          <Icon name="ph:arrow-left" size="14" />
          <span>BACK TO DEVICE</span>
        </NuxtLink>
      </template>
      <template #actions>
        <div v-if="device" class="device-status">
          <span class="status-dot" :class="device.online ? 'status-online' : 'status-offline'"></span>
          <span class="device-name">{{ device.name }}</span>
        </div>
      </template>
    </TerminalPageHeader>

    <!-- Loading State -->
    <div v-if="loading && !device" class="loading-state">
      <div class="loading-spinner"></div>
      <span>Loading device...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <span class="error-icon">[!]</span>
      <span class="error-message">{{ error }}</span>
      <button class="btn-terminal btn-terminal-primary" @click="loadDevice">
        RETRY
      </button>
    </div>

    <!-- File Browser -->
    <div v-else-if="device" class="browser-container">
      <FileBrowser
        :device-id="deviceId"
        :device-online="device.online"
      />
    </div>
  </div>
</template>

<style scoped>
.files-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 24px;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: transparent;
  border: 1px solid var(--color-terminal-border-bright);
  border-radius: 2px;
  color: var(--color-terminal-muted);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-decoration: none;
  transition: all 0.2s ease;
}

.back-btn:hover {
  color: var(--color-primary);
  border-color: rgba(34, 211, 238, 0.4);
  background: rgba(34, 211, 238, 0.05);
}

.device-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid var(--color-terminal-border);
  border-radius: 2px;
  font-size: 11px;
}

.device-name {
  color: var(--color-terminal-muted);
  font-weight: 500;
}

.browser-container {
  flex: 1;
  min-height: 0;
}

/* Loading State */
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--color-terminal-dim);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 2px solid var(--color-terminal-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Error State */
.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.error-icon {
  font-size: 32px;
  color: var(--color-error);
  font-weight: 700;
}

.error-message {
  color: var(--color-error);
  font-size: 13px;
}
</style>

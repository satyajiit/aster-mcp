<template>
  <section id="screenshots" class="relative py-32 px-6 overflow-hidden">
    <!-- Background -->
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-surface-raised/30 to-transparent" />

    <div class="relative max-w-6xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">App &amp; Dashboard</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Companion apps included
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Aster ships with an Android companion app and a web dashboard for device management, file browsing, and MCP tool testing.
        </p>
      </div>

      <!-- Tab switcher -->
      <div class="flex items-center justify-center gap-2 mb-12">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="px-5 py-2.5 rounded-xl text-xs font-semibold uppercase tracking-[0.1em] border transition-all duration-300"
          :class="activeTab === tab.id
            ? 'bg-aster/10 border-aster/30 text-aster shadow-[0_0_20px_rgba(45,212,191,0.1)]'
            : 'border-border-dim text-text-tertiary hover:text-text-secondary hover:border-border-subtle'"
          @click="activeTab = tab.id"
        >
          <Icon :name="tab.icon" class="inline mr-1.5 text-sm" />
          {{ tab.label }}
        </button>
      </div>

      <!-- Theme switcher — both the app and the dashboard ship in both themes -->
      <div class="flex items-center justify-center gap-2 mb-8">
        <button
          v-for="theme in themes"
          :key="theme.id"
          class="px-4 py-1.5 rounded-lg text-[11px] font-semibold uppercase tracking-[0.1em] border transition-all duration-300"
          :class="shotTheme === theme.id
            ? 'bg-aster/10 border-aster/30 text-aster'
            : 'border-border-dim text-text-tertiary hover:text-text-secondary hover:border-border-subtle'"
          @click="shotTheme = theme.id"
        >
          <Icon :name="theme.icon" class="inline mr-1 text-xs" />
          {{ theme.label }}
        </button>
      </div>

      <!-- Android App Screenshots -->
      <Transition name="tab" mode="out-in">
        <div v-if="activeTab === 'app'" key="app">
          <div class="flex justify-start sm:justify-center gap-4 sm:gap-6 overflow-x-auto pb-4 sm:pb-0 sm:overflow-x-visible snap-x snap-mandatory">
            <div
              v-for="shot in appScreenshots"
              :key="shotTheme + shot.src"
              class="screenshot-card group relative rounded-2xl overflow-hidden border border-border-dim bg-surface-raised/60 backdrop-blur-sm hover:border-aster/20 transition-all duration-500 flex-shrink-0 snap-center"
              style="width: 180px;"
            >
              <img
                :src="shotTheme === 'light' ? shot.src.replace('/app/', '/app/light/') : shot.src"
                :alt="shot.label"
                class="w-full h-auto block transition-transform duration-500 group-hover:scale-[1.02]"
                loading="lazy"
              />
              <div class="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent px-3 py-3">
                <span class="text-[10px] font-semibold uppercase tracking-[0.12em] text-white/90">{{ shot.label }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Dashboard Screenshots -->
        <div v-else key="dashboard">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div
              v-for="shot in dashboardScreenshots"
              :key="shotTheme + shot.src"
              class="screenshot-card group relative rounded-2xl overflow-hidden border border-border-dim bg-surface-raised/60 backdrop-blur-sm hover:border-aster/20 transition-all duration-500"
            >
              <img
                :src="shotTheme === 'light' ? shot.src.replace('/dashboard/', '/dashboard/light/') : shot.src"
                :alt="shot.label"
                class="w-full h-auto block transition-transform duration-500 group-hover:scale-[1.01]"
                loading="lazy"
              />
              <div class="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent px-4 py-3">
                <span class="text-[11px] font-semibold uppercase tracking-[0.12em] text-white/90">{{ shot.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </section>
</template>

<script setup lang="ts">
const activeTab = ref('app')
const shotTheme = ref('dark')

const tabs = [
  { id: 'app', label: 'Android App', icon: 'lucide:smartphone' },
  { id: 'dashboard', label: 'Web Dashboard', icon: 'lucide:monitor' },
]

const themes = [
  { id: 'dark', label: 'Dark', icon: 'lucide:moon' },
  { id: 'light', label: 'Light', icon: 'lucide:sun' },
]

const appScreenshots = [
  { src: '/screenshots/app/connection-setup.jpg', label: 'Connection Setup' },
  { src: '/screenshots/app/device-dashboard.jpg', label: 'Device Dashboard' },
  { src: '/screenshots/app/services-logs.jpg', label: 'Tool Call Logs' },
  { src: '/screenshots/app/permissions.jpg', label: 'Permissions' },
  { src: '/screenshots/app/companion-overlay.jpg', label: 'Companion Face' },
  { src: '/screenshots/app/on-device-mcp.jpg', label: 'On-device MCP' },
]

// Captured by tools/screenshots (`node run.mjs`), which also syncs them here.
// Every name must exist under both /screenshots/dashboard/ and .../light/.
const dashboardScreenshots = [
  { src: '/screenshots/dashboard/dashboard-overview.png', label: 'Overview' },
  { src: '/screenshots/dashboard/device-registry.png', label: 'Device Registry' },
  { src: '/screenshots/dashboard/device-telemetry.png', label: 'Device Telemetry' },
  { src: '/screenshots/dashboard/device-screen-control.png', label: 'Screen Control' },
  { src: '/screenshots/dashboard/panel-messages.png', label: 'Messages' },
  { src: '/screenshots/dashboard/panel-apps.png', label: 'Apps Inventory' },
  { src: '/screenshots/dashboard/panel-storage.png', label: 'Storage & Media' },
  { src: '/screenshots/dashboard/logs.png', label: 'Logs' },
  { src: '/screenshots/dashboard/connect.png', label: 'Connect' },
  { src: '/screenshots/dashboard/file-browser.png', label: 'File Browser' },
  { src: '/screenshots/dashboard/mcp-tool-explorer.png', label: 'MCP Tool Explorer' },
  { src: '/screenshots/dashboard/event-forwarding.png', label: 'Event Forwarding' },
]
</script>

<style scoped>
.screenshot-card {
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1),
              border-color 0.4s ease,
              box-shadow 0.4s ease;
}
.screenshot-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.4);
}

.tab-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.tab-leave-active {
  transition: all 0.2s cubic-bezier(0.55, 0, 1, 0.45);
}
.tab-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.tab-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>

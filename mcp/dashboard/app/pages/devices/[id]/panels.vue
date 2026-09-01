<script setup lang="ts">
import type { Device } from '~/composables/useApi';
// Imported as component objects, NOT by name. `<component :is="'AppsPanel'">`
// cannot work here: Nuxt registers auto-imported components from what the
// template statically references, and a dynamic string is invisible to that
// transform — Vue then treats the name as an unknown native tag and renders
// an empty <appspanel></appspanel>. That silently blanked all ten panels.
import MessagesPanel from '~/components/panels/MessagesPanel.vue';
import NotificationsPanel from '~/components/panels/NotificationsPanel.vue';
import ContactsPanel from '~/components/panels/ContactsPanel.vue';
import AlarmsPanel from '~/components/panels/AlarmsPanel.vue';
import AppsPanel from '~/components/panels/AppsPanel.vue';
import StoragePanel from '~/components/panels/StoragePanel.vue';
import AudioPanel from '~/components/panels/AudioPanel.vue';
import LocationPanel from '~/components/panels/LocationPanel.vue';
import ShellPanel from '~/components/panels/ShellPanel.vue';
import UtilitiesPanel from '~/components/panels/UtilitiesPanel.vue';

const route = useRoute();
const router = useRouter();
const api = useApi();
const deviceId = route.params.id as string;

const PANELS = [
  { key: 'messages', label: 'Messages', icon: 'ph:chat-circle', component: markRaw(MessagesPanel) },
  { key: 'notifications', label: 'Notifications', icon: 'ph:bell', component: markRaw(NotificationsPanel) },
  { key: 'contacts', label: 'Contacts', icon: 'ph:address-book', component: markRaw(ContactsPanel) },
  { key: 'alarms', label: 'Alarms', icon: 'ph:alarm', component: markRaw(AlarmsPanel) },
  { key: 'apps', label: 'Apps', icon: 'ph:package', component: markRaw(AppsPanel) },
  { key: 'storage', label: 'Storage', icon: 'ph:hard-drive', component: markRaw(StoragePanel) },
  { key: 'audio', label: 'Audio', icon: 'ph:speaker-high', component: markRaw(AudioPanel) },
  { key: 'location', label: 'Location', icon: 'ph:map-pin', component: markRaw(LocationPanel) },
  { key: 'shell', label: 'Shell', icon: 'ph:terminal', component: markRaw(ShellPanel) },
  { key: 'utilities', label: 'Utilities', icon: 'ph:wrench', component: markRaw(UtilitiesPanel) },
] as const;

const active = ref<string>((route.query.panel as string) || 'messages');
const device = ref<Device | null>(null);

useHead({ title: () => PANELS.find((p) => p.key === active.value)?.label ?? 'Panels' });

watch(active, (v) => router.replace({ query: { ...route.query, panel: v } }));

onMounted(async () => {
  device.value = await api.getDevice(deviceId);
});

const current = computed(() => PANELS.find((p) => p.key === active.value) ?? PANELS[0]);
</script>

<template>
  <div>
    <PageHeader
      title="Device panels"
      description="Purpose-built views over the device's capabilities."
      :back-to="`/devices/${deviceId}`"
      back-label="Device"
    >
      <template #actions>
        <AStatusPill v-if="device" :status="device.online ? 'online' : 'offline'" />
      </template>
    </PageHeader>

    <nav class="tabs">
      <button
        v-for="p in PANELS"
        :key="p.key"
        type="button"
        class="tab"
        :class="{ 'tab--on': active === p.key }"
        @click="active = p.key"
      >
        <Icon :name="p.icon" />
        {{ p.label }}
      </button>
    </nav>

    <AEmptyState
      v-if="device && !device.online"
      icon="ph:wifi-slash"
      title="Device is offline"
      description="Panels read live data and need a connected device."
    />
    <!-- Keep each panel mounted per tab so its fetched state survives switching. -->
    <component :is="current.component" v-else :key="current.key" :device-id="deviceId" />
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.25rem;
  margin-bottom: 1.25rem;
  border-radius: var(--radius-md);
  background: var(--color-surface-1);
  border: 1px solid var(--color-border);
  overflow-x: auto;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  border: none;
  background: none;
  color: var(--color-fg-subtle);
  font: inherit;
  font-size: var(--text-label-lg);
  padding: 0.4375rem 0.75rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
}

.tab:hover {
  color: var(--color-fg);
}

.tab--on {
  background: color-mix(in oklab, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
}
</style>

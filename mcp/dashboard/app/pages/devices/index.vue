<script setup lang="ts">
import type { Device } from '~/composables/useApi';

useHead({ title: 'Devices' });

const api = useApi();
const toast = useToast();

const devices = ref<Device[]>([]);
const loading = ref(true);
const filter = ref<'all' | 'online' | 'pending' | 'rejected'>('all');
const search = ref('');
const removing = ref<Device | null>(null);

async function load() {
  devices.value = await api.getDevices();
  loading.value = false;
}

const { refresh } = usePolling(load, 5000);

const counts = computed(() => ({
  all: devices.value.length,
  online: devices.value.filter((d) => d.online).length,
  pending: devices.value.filter((d) => d.status === 'pending').length,
  rejected: devices.value.filter((d) => d.status === 'rejected').length,
}));

const visible = computed(() => {
  let list = devices.value;
  if (filter.value === 'online') list = list.filter((d) => d.online);
  else if (filter.value === 'pending') list = list.filter((d) => d.status === 'pending');
  else if (filter.value === 'rejected') list = list.filter((d) => d.status === 'rejected');

  const q = search.value.trim().toLowerCase();
  if (q) {
    list = list.filter((d) =>
      [d.name, d.model, d.manufacturer, d.id].some((v) => v.toLowerCase().includes(q)),
    );
  }
  return list;
});

const TABS = [
  { key: 'all', label: 'All' },
  { key: 'online', label: 'Online' },
  { key: 'pending', label: 'Pending' },
  { key: 'rejected', label: 'Rejected' },
] as const;

async function approve(id: string) {
  await api.approveDevice(id);
  toast.success('Device approved');
  await refresh();
}

async function reject(id: string) {
  await api.rejectDevice(id);
  toast.info('Device rejected');
  await refresh();
}

async function unreject(id: string) {
  await api.unrejectDevice(id);
  toast.success('Device restored to pending');
  await refresh();
}

function askRemove(id: string) {
  removing.value = devices.value.find((d) => d.id === id) ?? null;
}

async function confirmRemove() {
  if (!removing.value) return;
  const name = removing.value.name;
  try {
    await api.deleteDevice(removing.value.id);
    toast.success(`Removed ${name}`);
  } catch (e) {
    toast.error('Could not remove device', e instanceof Error ? e.message : String(e));
  } finally {
    removing.value = null;
    await refresh();
  }
}
</script>

<template>
  <div>
    <PageHeader title="Devices" description="Every device that has paired with this server.">
      <template #actions>
        <AButton size="sm" icon="ph:arrows-clockwise" @click="refresh">Refresh</AButton>
      </template>
    </PageHeader>

    <div class="toolbar">
      <div class="tabs" role="tablist">
        <button
          v-for="tab in TABS"
          :key="tab.key"
          type="button"
          role="tab"
          class="tab"
          :class="{ 'tab--active': filter === tab.key }"
          :aria-selected="filter === tab.key"
          @click="filter = tab.key"
        >
          {{ tab.label }}
          <span class="tab__count">{{ counts[tab.key] }}</span>
        </button>
      </div>

      <label class="search">
        <Icon name="ph:magnifying-glass" />
        <input v-model="search" type="search" placeholder="Search name, model or id" />
      </label>
    </div>

    <ACard variant="flush">
      <div v-if="loading" class="loading"><ASpinner /> Loading devices…</div>
      <AEmptyState
        v-else-if="visible.length === 0"
        icon="ph:device-mobile-slash"
        :title="devices.length === 0 ? 'No devices registered' : 'No devices match this filter'"
        :description="
          devices.length === 0
            ? 'Install the Aster companion and point it at this server to pair a device.'
            : null
        "
      />
      <DeviceRow
        v-for="d in visible"
        :key="d.id"
        :device="d"
        @approve="approve"
        @reject="reject"
        @unreject="unreject"
        @remove="askRemove"
      />
    </ACard>

    <AModal
      :open="!!removing"
      title="Remove device?"
      :description="`${removing?.name ?? ''} will be deleted along with its logs. The device can pair again later.`"
      @update:open="(v) => !v && (removing = null)"
    >
      <template #footer>
        <AButton size="sm" @click="removing = null">Cancel</AButton>
        <AButton variant="danger" size="sm" @click="confirmRemove">Remove</AButton>
      </template>
    </AModal>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 0.875rem;
}

.tabs {
  display: flex;
  gap: 0.25rem;
  padding: 0.1875rem;
  border-radius: var(--radius-md);
  background: var(--color-surface-1);
  border: 1px solid var(--color-border);
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
  padding: 0.3125rem 0.625rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.tab--active {
  background: var(--color-surface-3);
  color: var(--color-fg);
}

.tab__count {
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

.search {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0 0.75rem;
  min-height: 38px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
  color: var(--color-fg-muted);
}

.search:focus-within {
  border-color: var(--color-primary);
}

.search input {
  border: none;
  background: none;
  color: var(--color-fg);
  font: inherit;
  font-size: var(--text-body-md);
  outline: none;
  min-width: 16rem;
}

.loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  justify-content: center;
  color: var(--color-fg-subtle);
}
</style>

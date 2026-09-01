<script setup lang="ts">
import type { LogEntry } from '~/composables/useApi';

useHead({ title: 'Logs' });

const api = useApi();
const route = useRoute();
const router = useRouter();

const logs = ref<LogEntry[]>([]);
const total = ref(0);
const offset = ref(0);
const limit = 100;
const loading = ref(true);
const deviceIds = ref<string[]>([]);

const levels = ref<LogEntry['level'][]>([]);
const search = ref('');
const deviceFilter = ref<string>((route.query.device as string) ?? '');
const live = ref(true);

const LEVELS: { value: LogEntry['level']; label: string; color: string }[] = [
  { value: 'debug', label: 'Debug', color: 'var(--color-fg-muted)' },
  { value: 'info', label: 'Info', color: 'var(--color-info)' },
  { value: 'warn', label: 'Warn', color: 'var(--color-warning)' },
  { value: 'error', label: 'Error', color: 'var(--color-error)' },
];

function toggleLevel(level: LogEntry['level']) {
  const i = levels.value.indexOf(level);
  if (i === -1) levels.value.push(level);
  else levels.value.splice(i, 1);
  offset.value = 0;
}

async function load() {
  const filters = { limit, offset: offset.value, levels: levels.value, search: search.value };
  const page = deviceFilter.value
    ? await api.getDeviceLogs(deviceFilter.value, filters)
    : await api.getLogs(filters);
  logs.value = page.logs;
  total.value = page.total;
  loading.value = false;
}

const { refresh } = usePolling(() => (live.value ? load() : Promise.resolve()), 4000);

// Debounce the search so typing doesn't fire a request per keystroke.
let searchTimer: ReturnType<typeof setTimeout>;
watch(search, () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    offset.value = 0;
    void load();
  }, 300);
});
onScopeDispose(() => clearTimeout(searchTimer));

watch([levels, deviceFilter, offset], () => void load(), { deep: true });

watch(deviceFilter, (v) => {
  router.replace({ query: v ? { device: v } : {} });
});

onMounted(async () => {
  deviceIds.value = await api.getLogDeviceIds().catch(() => []);
});

const page = computed(() => Math.floor(offset.value / limit) + 1);
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / limit)));
const hasFilters = computed(
  () => levels.value.length > 0 || !!search.value || !!deviceFilter.value,
);

function clearFilters() {
  levels.value = [];
  search.value = '';
  deviceFilter.value = '';
  offset.value = 0;
}
</script>

<template>
  <div>
    <PageHeader title="Logs" description="Every command, response and device event.">
      <template #actions>
        <AToggle v-model="live" label="Live" />
        <AButton size="sm" icon="ph:arrows-clockwise" @click="refresh">Refresh</AButton>
      </template>
    </PageHeader>

    <div class="filters">
      <div class="levels">
        <button
          v-for="l in LEVELS"
          :key="l.value"
          type="button"
          class="level"
          :class="{ 'level--on': levels.includes(l.value) }"
          :style="{ '--level-color': l.color }"
          :aria-pressed="levels.includes(l.value)"
          @click="toggleLevel(l.value)"
        >
          <span class="level__dot" />
          {{ l.label }}
        </button>
      </div>

      <select v-model="deviceFilter" class="select" aria-label="Filter by device">
        <option value="">All devices</option>
        <option v-for="id in deviceIds" :key="id" :value="id">{{ shortId(id, 12) }}</option>
      </select>

      <label class="search">
        <Icon name="ph:magnifying-glass" />
        <input v-model="search" type="search" placeholder="Search message or payload" />
      </label>

      <AButton v-if="hasFilters" size="sm" variant="ghost" icon="ph:x" @click="clearFilters">
        Clear
      </AButton>
    </div>

    <ACard variant="flush">
      <div v-if="loading" class="loading"><ASpinner /> Loading logs…</div>
      <AEmptyState
        v-else-if="logs.length === 0"
        icon="ph:list-magnifying-glass"
        :title="hasFilters ? 'No logs match these filters' : 'No logs yet'"
        :description="hasFilters ? 'Try widening the level or clearing the search.' : null"
      />
      <LogRow v-for="log in logs" :key="log.id" :log="log" show-device />
    </ACard>

    <div v-if="total > limit" class="pager">
      <AButton size="sm" :disabled="offset === 0" @click="offset = Math.max(0, offset - limit)">
        Previous
      </AButton>
      <span class="pager__label">
        Page {{ page }} of {{ pageCount }} · {{ total }} entries
      </span>
      <AButton
        size="sm"
        :disabled="offset + limit >= total"
        @click="offset = offset + limit"
      >
        Next
      </AButton>
    </div>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex-wrap: wrap;
  margin-bottom: 0.875rem;
}

.levels {
  display: flex;
  gap: 0.25rem;
  padding: 0.1875rem;
  border-radius: var(--radius-md);
  background: var(--color-surface-1);
  border: 1px solid var(--color-border);
}

.level {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  border: none;
  background: none;
  color: var(--color-fg-subtle);
  font: inherit;
  font-size: var(--text-label-md);
  padding: 0.3125rem 0.5625rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.level--on {
  background: color-mix(in oklab, var(--level-color) 15%, transparent);
  color: var(--level-color);
}

.level__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--level-color);
}

.select,
.search {
  min-height: 38px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
  color: var(--color-fg);
  font: inherit;
  font-size: var(--text-body-md);
  padding: 0 0.75rem;
}

.search {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-fg-muted);
}

.search:focus-within,
.select:focus {
  border-color: var(--color-primary);
  outline: none;
}

.search input {
  border: none;
  background: none;
  color: var(--color-fg);
  font: inherit;
  outline: none;
  min-width: 14rem;
}

.loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
  justify-content: center;
  color: var(--color-fg-subtle);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1rem;
}

.pager__label {
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
}
</style>

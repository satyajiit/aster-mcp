<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

/**
 * Shape as `aster_list_installed_apps` actually returns it — snake_case, and
 * `package` rather than `packageName`. Verified against a live device; the
 * previous camelCase interface was modelled on `aster_list_packages`, which
 * this panel never calls, so every field below the label rendered blank and
 * Launch sent an undefined package.
 */
interface App {
  package?: string; label?: string; version?: string;
  install_time?: number; update_time?: number; last_used?: number;
  size_app?: number; size_data?: number; size_cache?: number;
  system?: boolean; permissions?: string[];
}

const { data, loading, error, run, toast } = useDeviceTool<any>(props.deviceId);
const apps = ref<App[]>([]);
const cursor = ref<string | null>(null);
const includeSystem = ref(false);
const search = ref('');
const detail = ref<App | null>(null);

function extract(d: any) {
  if (!d) return { apps: [] as App[], cursor: null };
  const list = Array.isArray(d) ? d : (d.apps ?? []);
  return { apps: list as App[], cursor: d.next_cursor ?? null };
}

/** The device reports three separate byte counts, never a total. */
function sizeOf(a: App): number | null {
  const total = (a.size_app ?? 0) + (a.size_data ?? 0) + (a.size_cache ?? 0);
  return total > 0 ? total : null;
}

async function load(reset = false) {
  if (reset) { apps.value = []; cursor.value = null; }
  const d = await run(
    'aster_list_installed_apps',
    { includeSystem: includeSystem.value, cursor: cursor.value ?? undefined, limit: 100 },
    'List apps',
  );
  const { apps: page, cursor: next } = extract(d);
  apps.value = [...apps.value, ...page];
  cursor.value = next;
}

const visible = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return apps.value;
  return apps.value.filter((a) =>
    [a.label, a.package].some((v) => v?.toLowerCase().includes(q)),
  );
});

function nameOf(a: App) { return a.label || a.package || 'Unknown'; }

async function launch(a: App) {
  if (!a.package) return;
  const ok = await run('aster_launch_intent', { packageName: a.package }, 'Launch app');
  if (ok !== null) toast.success(`Launched ${nameOf(a)}`);
}

watch(includeSystem, () => load(true));
onMounted(() => load(true));
</script>

<template>
  <PanelShell
    title="Installed apps"
    :description="`${apps.length} loaded${cursor ? ' — more available' : ''}`"
    :loading="loading && apps.length === 0"
    :error="error"
    :empty="!loading && apps.length === 0"
    empty-title="No apps found"
    empty-icon="ph:package"
  >
    <template #actions>
      <AToggle v-model="includeSystem" label="System apps" />
      <AButton size="sm" icon="ph:arrows-clockwise" @click="load(true)">Reload</AButton>
    </template>

    <template #controls>
      <label class="search">
        <Icon name="ph:magnifying-glass" />
        <input v-model="search" type="search" placeholder="Filter by name or package" />
      </label>
    </template>

    <div class="list">
      <div v-for="(a, i) in visible" :key="a.package ?? i" class="row">
        <AIconTile icon="ph:package" :size="34" accent="var(--color-mode-ipc)" />
        <div class="row__body" @click="detail = a">
          <p class="row__name">{{ nameOf(a) }}</p>
          <code class="row__pkg">{{ a.package }}</code>
        </div>
        <span v-if="sizeOf(a)" class="row__size">{{ formatBytes(sizeOf(a)!) }}</span>
        <ABadge v-if="a.system" tone="neutral">system</ABadge>
        <AButton size="sm" variant="ghost" icon="ph:play" title="Launch" @click="launch(a)" />
      </div>
    </div>

    <AButton v-if="cursor" class="more" :loading="loading" @click="load()">Load more</AButton>

    <AModal
      :open="!!detail"
      :title="detail ? nameOf(detail) : ''"
      :description="detail?.package"
      :width="520"
      @update:open="(v) => !v && (detail = null)"
    >
      <InfoGrid
        v-if="detail"
        :items="[
          { label: 'Version', value: detail.version, mono: true },
          { label: 'Size', value: sizeOf(detail) ? formatBytes(sizeOf(detail)!) : null },
          { label: 'Installed', value: detail.install_time ? new Date(detail.install_time).toLocaleString() : null },
          { label: 'Updated', value: detail.update_time ? new Date(detail.update_time).toLocaleString() : null },
          { label: 'Last used', value: detail.last_used ? formatRelativeTime(detail.last_used) : null },
          { label: 'Permissions', value: detail.permissions?.length ?? null },
        ]"
      />
      <details v-if="detail?.permissions?.length" class="perms">
        <summary>{{ detail.permissions.length }} permissions</summary>
        <ul><li v-for="p in detail.permissions" :key="p">{{ p }}</li></ul>
      </details>
    </AModal>
  </PanelShell>
</template>

<style scoped>
.search { display: flex; align-items: center; gap: 0.5rem; padding: 0.4375rem 0.625rem; margin-bottom: 1rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg-muted); }
.search input { flex: 1; border: none; background: none; color: var(--color-fg); font: inherit; outline: none; }
.list { display: grid; }
.row { display: flex; align-items: center; gap: 0.625rem; padding: 0.4375rem 0; border-bottom: 1px solid var(--color-border); }
.row:last-child { border-bottom: none; }
.row__body { flex: 1; min-width: 0; cursor: pointer; }
.row__name { margin: 0; font-size: var(--text-body-md); }
.row__pkg { font-family: var(--font-mono); font-size: var(--text-label-sm); color: var(--color-fg-muted); }
.row__size { font-size: var(--text-label-sm); color: var(--color-fg-muted); white-space: nowrap; }
.more { width: 100%; margin-top: 0.75rem; }
.perms { margin-top: 0.875rem; font-size: var(--text-body-sm); color: var(--color-fg-subtle); }
.perms ul { margin: 0.5rem 0 0; padding-left: 1.25rem; font-family: var(--font-mono); font-size: var(--text-label-sm); max-height: 220px; overflow: auto; }
</style>

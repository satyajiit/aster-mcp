<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

interface LargeFile { path?: string; name?: string; size?: number }
interface MediaItem {
  path?: string; name?: string; size?: number; dateTaken?: number;
  location?: { city?: string; country?: string };
  camera?: { make?: string; model?: string };
}
interface DirEntry { path?: string; size?: number; fileCount?: number; percentage?: number }
interface Analysis {
  path?: string; totalSize?: number; fileCount?: number;
  breakdown?: { byDirectory?: DirEntry[]; byFileType?: unknown; largeFiles?: unknown };
}

/**
 * The storage tools report sizes in MEGABYTES, not bytes — `aster_analyze_storage`
 * answered totalSize 141.23 for a 141 MB tree, and `aster_find_large_files` takes
 * its threshold as `minSizeMB`. Feeding those straight to formatBytes rendered a
 * 512 MB video as "512 B".
 */
function formatMB(mb?: number | null): string {
  if (mb == null) return '—';
  return formatBytes(mb * 1024 * 1024);
}

const { data, loading, error, run } = useDeviceTool<any>(props.deviceId);
const mode = ref<'analyze' | 'large' | 'media'>('analyze');
const rootPath = ref('/sdcard');
const minSizeMB = ref(50);
const mediaQuery = ref('');

const analysis = ref<Analysis | null>(null);
const largeFiles = ref<LargeFile[]>([]);
const media = ref<MediaItem[]>([]);

async function analyze() {
  const d = await run('aster_analyze_storage', { path: rootPath.value, maxDepth: 3 }, 'Analyze storage');
  analysis.value = d;
}

async function findLarge() {
  const d = await run('aster_find_large_files', { path: rootPath.value, minSizeMB: minSizeMB.value, limit: 50 }, 'Find large files');
  largeFiles.value = Array.isArray(d) ? d : (d?.files ?? []);
}

async function searchMedia() {
  const d = await run(
    'aster_search_media',
    { query: mediaQuery.value || undefined, path: rootPath.value, limit: 50 },
    'Search media',
  );
  // `aster_search_media` returns its rows under `files`.
  media.value = Array.isArray(d) ? d : (d?.files ?? []);
}

// `breakdown` is an OBJECT — {byDirectory, byFileType, largeFiles} — so the old
// Array.isArray guard rejected it and every run fell through to a raw JSON dump.
const breakdown = computed<DirEntry[]>(() => {
  const b = analysis.value?.breakdown;
  const dirs = Array.isArray(b) ? b : (b?.byDirectory ?? []);
  return Array.isArray(dirs) ? dirs.slice(0, 12) : [];
});

const largest = computed(() => breakdown.value[0]?.size ?? 1);

function run_() {
  if (mode.value === 'analyze') return analyze();
  if (mode.value === 'large') return findLarge();
  return searchMedia();
}

onMounted(analyze);
</script>

<template>
  <PanelShell
    title="Storage & media"
    description="Disk breakdown, large files, and natural-language media search."
    :loading="loading"
    :error="error"
  >
    <template #actions>
      <AButton size="sm" icon="ph:play" @click="run_">Run</AButton>
    </template>

    <template #controls>
      <div class="bar">
        <div class="modes">
          <button
            v-for="m in [
              { key: 'analyze', label: 'Breakdown' },
              { key: 'large', label: 'Large files' },
              { key: 'media', label: 'Media search' },
            ]"
            :key="m.key"
            type="button"
            class="mode"
            :class="{ 'mode--on': mode === m.key }"
            @click="mode = m.key as any"
          >
            {{ m.label }}
          </button>
        </div>
        <input v-model="rootPath" class="control" placeholder="Path" />
        <input
          v-if="mode === 'large'"
          v-model.number="minSizeMB"
          type="number"
          class="control control--sm"
          aria-label="Minimum size MB"
        />
        <input
          v-if="mode === 'media'"
          v-model="mediaQuery"
          class="control"
          placeholder="e.g. photos from Goa last summer"
        />
      </div>
    </template>

    <!-- Breakdown -->
    <div v-if="mode === 'analyze'">
      <AEmptyState v-if="!analysis" icon="ph:hard-drive" title="Run a breakdown to see disk usage" />
      <template v-else>
        <div class="summary">
          <AStatCard icon="ph:folder" label="Path" :value="analysis.path ?? rootPath" />
          <AStatCard icon="ph:hard-drive" label="Total" :value="formatMB(analysis.totalSize)" />
          <AStatCard icon="ph:files" label="Files" :value="(analysis.fileCount ?? 0).toLocaleString()" />
        </div>
        <div v-if="breakdown.length" class="bars">
          <div v-for="(d, i) in breakdown" :key="d.path ?? i" class="barrow">
            <span class="barrow__name mono" :title="d.path">{{ d.path }}</span>
            <div class="barrow__track">
              <div
                class="barrow__fill"
                :style="{ width: `${Math.min(100, ((d.size ?? 0) / largest) * 100)}%` }"
              />
            </div>
            <span class="barrow__size">
              {{ formatMB(d.size) }}
              <i v-if="d.percentage != null" class="barrow__pct">{{ d.percentage.toFixed(1) }}%</i>
            </span>
          </div>
        </div>
        <AEmptyState v-else icon="ph:folder-open" title="Nothing to break down at this path" />
      </template>
    </div>

    <!-- Large files -->
    <div v-else-if="mode === 'large'">
      <AEmptyState v-if="largeFiles.length === 0" icon="ph:files" title="No large files found" />
      <div v-else class="list">
        <div v-for="(f, i) in largeFiles" :key="i" class="row">
          <AIconTile icon="ph:file" :size="30" accent="var(--color-warning)" />
          <span class="row__path mono">{{ f.path ?? f.name }}</span>
          <span class="row__size">{{ formatMB(f.size) }}</span>
        </div>
      </div>
    </div>

    <!-- Media search -->
    <div v-else>
      <AEmptyState
        v-if="media.length === 0"
        icon="ph:image-square"
        title="No media matched"
        description="Try a natural-language query — the server parses dates, places and file types."
      />
      <div v-else class="list">
        <div v-for="(m, i) in media" :key="i" class="row">
          <AIconTile icon="ph:image" :size="30" accent="var(--color-info)" />
          <div class="row__body">
            <span class="row__path mono">{{ m.path ?? m.name }}</span>
            <span v-if="m.location?.city || m.camera?.model" class="row__meta">
              {{ [m.location?.city, m.location?.country,
                  [m.camera?.make, m.camera?.model].filter(Boolean).join(' ')].filter(Boolean).join(' · ') }}
            </span>
          </div>
          <span class="row__size">{{ formatMB(m.size) }}</span>
        </div>
      </div>
    </div>
  </PanelShell>
</template>

<style scoped>
.bar { display: flex; gap: 0.5rem; margin-bottom: 1rem; flex-wrap: wrap; }
.modes { display: flex; gap: 0.25rem; padding: 0.1875rem; border-radius: var(--radius-md); background: var(--color-surface-2); border: 1px solid var(--color-border); }
.mode { border: none; background: none; color: var(--color-fg-subtle); font: inherit; font-size: var(--text-label-md); padding: 0.3125rem 0.625rem; border-radius: var(--radius-sm); cursor: pointer; }
.mode--on { background: var(--color-surface-3); color: var(--color-fg); }
.control { flex: 1; min-width: 8rem; min-height: 36px; padding: 0.375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; }
.control--sm { flex: 0 0 6rem; }
.control:focus { outline: none; border-color: var(--color-primary); }
.summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 0.625rem; margin-bottom: 1rem; }
.bars { display: grid; gap: 0.5rem; }
.barrow__pct { display: block; font-style: normal; font-size: var(--text-label-sm); color: var(--color-fg-subtle); text-align: right; }
.barrow { display: grid; grid-template-columns: minmax(6rem, 14rem) 1fr auto; gap: 0.625rem; align-items: center; }
.barrow__name { font-size: var(--text-label-md); color: var(--color-fg-subtle); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.barrow__track { height: 8px; border-radius: var(--radius-pill); background: var(--color-surface-3); overflow: hidden; }
.barrow__fill { height: 100%; background: var(--color-primary); border-radius: inherit; }
.barrow__size { font-size: var(--text-label-sm); color: var(--color-fg-muted); white-space: nowrap; }
.list { display: grid; }
.row { display: flex; align-items: center; gap: 0.625rem; padding: 0.4375rem 0; border-bottom: 1px solid var(--color-border); }
.row:last-child { border-bottom: none; }
.row__body { flex: 1; min-width: 0; display: grid; }
.row__path { flex: 1; font-size: var(--text-body-sm); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row__meta { font-size: var(--text-label-sm); color: var(--color-fg-muted); }
.row__size { font-size: var(--text-label-sm); color: var(--color-fg-muted); white-space: nowrap; }
.raw { margin: 0; padding: 0.75rem; border-radius: var(--radius-md); background: var(--color-surface-2); font-family: var(--font-mono); font-size: var(--text-body-sm); overflow: auto; max-height: 420px; }
</style>

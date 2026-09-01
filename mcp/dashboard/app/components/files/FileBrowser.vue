<script setup lang="ts">
import type { FileContentResult, FileEntry, FileListResult } from '~/composables/useApi';
import { toolJson, toolText } from '~/composables/useApi';

const props = defineProps<{ deviceId: string; online: boolean }>();

const api = useApi();
const toast = useToast();

const QUICK_PATHS = [
  { path: '/sdcard', label: 'Internal', icon: 'ph:house' },
  { path: '/sdcard/Download', label: 'Downloads', icon: 'ph:download-simple' },
  { path: '/sdcard/DCIM', label: 'Camera', icon: 'ph:camera' },
  { path: '/sdcard/Pictures', label: 'Pictures', icon: 'ph:image' },
  { path: '/sdcard/Documents', label: 'Documents', icon: 'ph:file-text' },
];

const path = ref('/sdcard');
const files = ref<FileEntry[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const selected = ref<FileEntry | null>(null);
const preview = ref<FileContentResult | null>(null);
const previewLoading = ref(false);
const showHidden = ref(false);

const creatingFolder = ref(false);
const newFolderName = ref('');
const deleting = ref<FileEntry | null>(null);
const busy = ref(false);

const EXT_ICON: Record<string, string> = {
  jpg: 'ph:image', jpeg: 'ph:image', png: 'ph:image', gif: 'ph:image', webp: 'ph:image',
  mp4: 'ph:film-strip', mkv: 'ph:film-strip', mov: 'ph:film-strip',
  mp3: 'ph:music-note', wav: 'ph:music-note', ogg: 'ph:music-note', m4a: 'ph:music-note',
  pdf: 'ph:file-pdf', zip: 'ph:file-zip', apk: 'ph:package',
  txt: 'ph:file-text', json: 'ph:brackets-curly', xml: 'ph:code', log: 'ph:file-text',
};

function iconFor(f: FileEntry) {
  if (f.isDirectory) return 'ph:folder';
  return EXT_ICON[(f.extension ?? '').toLowerCase()] ?? 'ph:file';
}

async function list(next = path.value) {
  loading.value = true;
  error.value = null;
  selected.value = null;
  preview.value = null;
  try {
    const result = await api.executeTool(props.deviceId, 'aster_list_files', { path: next });
    if (result.isError) throw new Error(toolText(result) || 'Could not list files');
    const parsed = toolJson<FileListResult>(result);
    files.value = parsed?.files ?? [];
    path.value = next;
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
    files.value = [];
  } finally {
    loading.value = false;
  }
}

const visible = computed(() => {
  const list = showHidden.value ? files.value : files.value.filter((f) => !f.isHidden);
  return [...list].sort((a, b) => {
    if (a.isDirectory !== b.isDirectory) return a.isDirectory ? -1 : 1;
    return a.name.localeCompare(b.name);
  });
});

const parent = computed(() => {
  if (path.value === '/' || !path.value.includes('/')) return null;
  const up = path.value.split('/').slice(0, -1).join('/');
  return up || '/';
});

const crumbs = computed(() => {
  const parts = path.value.split('/').filter(Boolean);
  return parts.map((name, i) => ({ name, path: '/' + parts.slice(0, i + 1).join('/') }));
});

async function open(f: FileEntry) {
  if (f.isDirectory) return list(f.path);
  selected.value = f;
  previewLoading.value = true;
  preview.value = null;
  try {
    const result = await api.executeTool(props.deviceId, 'aster_read_file', { path: f.path });
    if (result.isError) throw new Error(toolText(result) || 'Could not read file');
    preview.value = toolJson<FileContentResult>(result);
  } catch (e) {
    toast.error('Could not open file', e instanceof Error ? e.message : String(e));
  } finally {
    previewLoading.value = false;
  }
}

const previewIsImage = computed(
  () => preview.value?.encoding === 'base64' && !!preview.value.mimeType?.startsWith('image/'),
);

const previewSrc = computed(() =>
  preview.value ? `data:${preview.value.mimeType || 'image/png'};base64,${preview.value.content}` : '',
);

function download() {
  const f = selected.value;
  const p = preview.value;
  if (!f || !p) return;
  try {
    const blob =
      p.encoding === 'base64'
        ? new Blob(
            [Uint8Array.from(atob(p.content), (c) => c.charCodeAt(0))],
            { type: p.mimeType || 'application/octet-stream' },
          )
        : new Blob([p.content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = f.name;
    a.click();
    URL.revokeObjectURL(url);
  } catch (e) {
    // Previously a native alert(); now a toast like every other failure path.
    toast.error('Download failed', e instanceof Error ? e.message : String(e));
  }
}

async function createFolder() {
  const name = newFolderName.value.trim();
  if (!name) return;
  busy.value = true;
  try {
    // Folder creation is emulated by writing a .keep file; there is no mkdir tool.
    const target = `${path.value}/${name}/.keep`;
    const result = await api.executeTool(props.deviceId, 'aster_write_file', {
      path: target,
      content: '',
    });
    if (result.isError) throw new Error(toolText(result));
    toast.success(`Created ${name}`);
    creatingFolder.value = false;
    newFolderName.value = '';
    await list();
  } catch (e) {
    toast.error('Could not create folder', e instanceof Error ? e.message : String(e));
  } finally {
    busy.value = false;
  }
}

async function confirmDelete() {
  const f = deleting.value;
  if (!f) return;
  busy.value = true;
  try {
    const result = await api.executeTool(props.deviceId, 'aster_delete_file', { path: f.path });
    if (result.isError) throw new Error(toolText(result));
    toast.success(`Deleted ${f.name}`);
    deleting.value = null;
    await list();
  } catch (e) {
    toast.error('Could not delete', e instanceof Error ? e.message : String(e));
  } finally {
    busy.value = false;
  }
}

onMounted(() => list());
</script>

<template>
  <div class="fb">
    <div class="fb__quick">
      <button
        v-for="q in QUICK_PATHS"
        :key="q.path"
        type="button"
        class="quick"
        :class="{ 'quick--active': path === q.path }"
        @click="list(q.path)"
      >
        <Icon :name="q.icon" />
        {{ q.label }}
      </button>
      <div class="fb__quick-end">
        <AToggle v-model="showHidden" label="Hidden" />
        <AButton size="sm" icon="ph:folder-plus" @click="creatingFolder = true">New folder</AButton>
      </div>
    </div>

    <div class="fb__body">
      <ACard variant="flush" class="fb__list">
        <nav class="crumbs">
          <button type="button" class="crumb" @click="list('/')">/</button>
          <template v-for="c in crumbs" :key="c.path">
            <span class="crumb__sep">/</span>
            <button type="button" class="crumb" @click="list(c.path)">{{ c.name }}</button>
          </template>
          <AButton
            size="sm"
            variant="ghost"
            icon="ph:arrows-clockwise"
            class="crumbs__refresh"
            @click="list()"
          />
        </nav>

        <div v-if="loading" class="state"><ASpinner /> Loading…</div>
        <AEmptyState
          v-else-if="error"
          icon="ph:warning-circle"
          tone="error"
          title="Could not list this folder"
          :description="error"
        >
          <AButton size="sm" @click="list()">Retry</AButton>
        </AEmptyState>
        <AEmptyState v-else-if="visible.length === 0" icon="ph:folder-open" title="Empty folder" />

        <div v-else class="rows">
          <button v-if="parent" type="button" class="row" @click="list(parent)">
            <Icon name="ph:arrow-up" class="row__icon" />
            <span class="row__name">..</span>
          </button>

          <button
            v-for="f in visible"
            :key="f.path"
            type="button"
            class="row"
            :class="{ 'row--active': selected?.path === f.path }"
            @click="open(f)"
          >
            <Icon :name="iconFor(f)" class="row__icon" :class="{ 'row__icon--dir': f.isDirectory }" />
            <span class="row__name">{{ f.name }}</span>

            <!-- These five fields were in the payload all along and none were
                 shown, so hidden, read-only and permission state were invisible. -->
            <span class="row__flags">
              <ABadge v-if="f.isHidden" tone="neutral">hidden</ABadge>
              <ABadge v-if="f.canWrite === false" tone="warning">read-only</ABadge>
              <ABadge v-if="f.canRead === false" tone="error">no access</ABadge>
            </span>

            <span class="row__size">{{ f.isDirectory ? '—' : formatBytes(f.size) }}</span>
            <span class="row__date">{{ f.lastModified }}</span>
            <span class="row__actions">
              <AButton
                size="sm"
                variant="ghost"
                icon="ph:trash"
                title="Delete"
                @click.stop="deleting = f"
              />
            </span>
          </button>
        </div>
      </ACard>

      <ACard v-if="selected" class="fb__preview">
        <header class="pv__head">
          <div>
            <p class="pv__name">{{ selected.name }}</p>
            <p class="pv__meta mono">{{ selected.path }}</p>
          </div>
          <AButton size="sm" icon="ph:download-simple" :disabled="!preview" @click="download">
            Download
          </AButton>
        </header>

        <InfoGrid
          :items="[
            { label: 'Size', value: formatBytes(selected.size) },
            { label: 'Modified', value: selected.lastModified },
            { label: 'Extension', value: selected.extension || null, mono: true },
            { label: 'Readable', value: selected.canRead === false ? 'No' : 'Yes' },
            { label: 'Writable', value: selected.canWrite === false ? 'No' : 'Yes' },
            { label: 'Hidden', value: selected.isHidden ? 'Yes' : 'No' },
          ]"
          class="pv__info"
        />

        <div v-if="previewLoading" class="state"><ASpinner /> Reading…</div>
        <template v-else-if="preview">
          <img v-if="previewIsImage" :src="previewSrc" :alt="selected.name" class="pv__img" />
          <pre v-else-if="preview.encoding === 'text'" class="pv__text">{{ preview.content }}</pre>
          <p v-else class="pv__binary">Binary file — download to inspect.</p>
          <p v-if="preview.truncated" class="pv__trunc">Preview truncated.</p>
        </template>
      </ACard>
    </div>

    <AModal v-model:open="creatingFolder" title="New folder" :description="path">
      <input
        v-model="newFolderName"
        class="control"
        placeholder="Folder name"
        @keyup.enter="createFolder"
      />
      <template #footer>
        <AButton size="sm" @click="creatingFolder = false">Cancel</AButton>
        <AButton variant="primary" size="sm" :loading="busy" @click="createFolder">Create</AButton>
      </template>
    </AModal>

    <AModal
      :open="!!deleting"
      title="Delete file?"
      :description="deleting?.path"
      @update:open="(v) => !v && (deleting = null)"
    >
      <p class="warn">This cannot be undone.</p>
      <template #footer>
        <AButton size="sm" @click="deleting = null">Cancel</AButton>
        <AButton variant="danger" size="sm" :loading="busy" @click="confirmDelete">Delete</AButton>
      </template>
    </AModal>
  </div>
</template>

<style scoped>
.fb__quick {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  flex-wrap: wrap;
  margin-bottom: 0.875rem;
}

.fb__quick-end {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.quick {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.625rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
  color: var(--color-fg-subtle);
  font: inherit;
  font-size: var(--text-label-md);
  cursor: pointer;
}

.quick--active,
.quick:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.fb__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 380px);
  gap: 1rem;
  align-items: start;
}

.fb__body:has(.fb__preview) {
  grid-template-columns: minmax(0, 1fr) minmax(300px, 380px);
}

.fb__list {
  min-width: 0;
}

@media (max-width: 1000px) {
  .fb__body {
    grid-template-columns: 1fr;
  }
}

.crumbs {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;
}

.crumb {
  border: none;
  background: none;
  color: var(--color-fg-subtle);
  font-family: var(--font-mono);
  font-size: var(--text-body-sm);
  cursor: pointer;
  padding: 0.125rem 0.25rem;
  border-radius: var(--radius-xs);
}

.crumb:hover {
  color: var(--color-primary);
  background: var(--color-surface-2);
}

.crumb__sep {
  color: var(--color-fg-muted);
}

.crumbs__refresh {
  margin-left: auto;
}

.rows {
  display: grid;
}

.row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto auto auto;
  align-items: center;
  gap: 0.625rem;
  padding: 0.4375rem 0.75rem;
  border: none;
  border-bottom: 1px solid var(--color-border);
  background: none;
  color: var(--color-fg);
  font: inherit;
  font-size: var(--text-body-sm);
  text-align: left;
  cursor: pointer;
}

.row:last-child {
  border-bottom: none;
}

.row:hover {
  background: var(--color-surface-2);
}

.row--active {
  background: color-mix(in oklab, var(--color-primary) 12%, transparent);
}

.row__icon {
  color: var(--color-fg-muted);
  font-size: 1.125rem;
}

.row__icon--dir {
  color: var(--color-warning);
}

.row__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row__flags {
  display: flex;
  gap: 0.25rem;
}

.row__size,
.row__date {
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
  white-space: nowrap;
}

@media (max-width: 800px) {
  .row {
    grid-template-columns: auto minmax(0, 1fr) auto auto;
  }

  .row__date,
  .row__flags {
    display: none;
  }
}

.pv__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.875rem;
}

.pv__name {
  margin: 0;
  font-size: var(--text-title-md);
  font-weight: 600;
  overflow-wrap: anywhere;
}

.pv__meta {
  margin: 0.125rem 0 0;
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
  overflow-wrap: anywhere;
}

.pv__info {
  margin-bottom: 0.875rem;
}

.pv__img {
  display: block;
  max-width: 100%;
  border-radius: var(--radius-md);
  background: var(--color-surface-2);
}

.pv__text {
  margin: 0;
  padding: 0.75rem;
  max-height: 420px;
  overflow: auto;
  border-radius: var(--radius-md);
  background: var(--color-surface-2);
  font-family: var(--font-mono);
  font-size: var(--text-body-sm);
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.pv__binary,
.pv__trunc {
  margin: 0.5rem 0 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-muted);
}

.state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: center;
  padding: 2rem;
  color: var(--color-fg-subtle);
}

.control {
  width: 100%;
  min-height: 40px;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  color: var(--color-fg);
  font: inherit;
}

.control:focus {
  outline: none;
  border-color: var(--color-primary);
}

.warn {
  margin: 0;
  font-size: var(--text-body-md);
  color: var(--color-fg-subtle);
}
</style>

<script setup lang="ts">
import type { FileEntry } from '~/composables/useApi';

defineProps<{
  files: FileEntry[];
  selectedFile: FileEntry | null;
  loading?: boolean;
  error?: string | null;
  currentPath: string;
}>();

const emit = defineEmits<{
  select: [file: FileEntry];
  open: [file: FileEntry];
  navigateUp: [];
}>();

// Sort files: directories first, then by newest modified
function sortFiles(files: FileEntry[]): FileEntry[] {
  return [...files].sort((a, b) => {
    if (a.isDirectory && !b.isDirectory) return -1;
    if (!a.isDirectory && b.isDirectory) return 1;
    return (b.lastModified || '').localeCompare(a.lastModified || '');
  });
}

function getBreadcrumbs(path: string): Array<{ name: string; path: string }> {
  const parts = path.split('/').filter(Boolean);
  const crumbs: Array<{ name: string; path: string }> = [];
  let currentPath = '';

  for (const part of parts) {
    currentPath += '/' + part;
    crumbs.push({ name: part, path: currentPath });
  }

  return crumbs;
}

function canGoUp(path: string): boolean {
  return path !== '/' && path !== '';
}
</script>

<template>
  <div class="file-list">
    <!-- Breadcrumb Path -->
    <div class="file-list-header">
      <div class="breadcrumb">
        <span class="breadcrumb-prefix">$</span>
        <button
          class="breadcrumb-item breadcrumb-root"
          @click="emit('open', { name: '/', path: '/', isDirectory: true, size: 0, modified: 0 })"
        >
          /
        </button>
        <template v-for="(crumb, index) in getBreadcrumbs(currentPath)" :key="crumb.path">
          <span class="breadcrumb-separator">/</span>
          <button
            class="breadcrumb-item"
            :class="{ active: index === getBreadcrumbs(currentPath).length - 1 }"
            @click="emit('open', { name: crumb.name, path: crumb.path, isDirectory: true, size: 0, modified: 0 })"
          >
            {{ crumb.name }}
          </button>
        </template>
        <span class="cursor"></span>
      </div>
    </div>

    <!-- Column Headers -->
    <div class="file-list-columns">
      <span class="col-icon"></span>
      <span class="col-name">NAME</span>
      <span class="col-size">SIZE</span>
      <span class="col-modified">MODIFIED</span>
    </div>

    <!-- File List Content -->
    <div class="file-list-content">
      <!-- Loading State -->
      <div v-if="loading" class="file-list-loading">
        <div v-for="i in 8" :key="i" class="skeleton-row">
          <div class="skeleton skeleton-icon"></div>
          <div class="skeleton skeleton-name"></div>
          <div class="skeleton skeleton-size"></div>
          <div class="skeleton skeleton-date"></div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="file-list-error">
        <span class="error-icon">[!]</span>
        <span class="error-message">{{ error }}</span>
      </div>

      <!-- Empty State -->
      <div v-else-if="files.length === 0" class="file-list-empty">
        <span class="empty-icon">[_]</span>
        <span class="empty-message">This folder is empty</span>
      </div>

      <!-- File Rows -->
      <template v-else>
        <!-- Parent Directory -->
        <div
          v-if="canGoUp(currentPath)"
          class="file-row parent-dir"
          @click="emit('navigateUp')"
          @dblclick="emit('navigateUp')"
        >
          <span class="file-icon icon-folder">[^]</span>
          <span class="file-name">..</span>
          <span class="file-size"></span>
          <span class="file-modified"></span>
        </div>

        <FileRow
          v-for="file in sortFiles(files)"
          :key="file.path"
          :file="file"
          :selected="selectedFile?.path === file.path"
          @select="emit('select', $event)"
          @open="emit('open', $event)"
        />
      </template>
    </div>
  </div>
</template>

<style scoped>
.file-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.file-list-header {
  padding: 12px 14px;
  border-bottom: 1px solid var(--color-terminal-border);
  background: rgba(148, 163, 184, 0.02);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  overflow-x: auto;
  white-space: nowrap;
}

.breadcrumb-prefix {
  color: var(--color-primary);
  font-weight: 700;
  margin-right: 8px;
}

.breadcrumb-item {
  background: none;
  border: none;
  color: var(--color-terminal-muted);
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 2px 4px;
  cursor: pointer;
  transition: color 0.15s ease;
  border-radius: 2px;
}

.breadcrumb-item:hover {
  color: var(--color-primary);
  background: rgba(34, 211, 238, 0.1);
}

.breadcrumb-item.active {
  color: var(--color-terminal-text);
  font-weight: 600;
}

.breadcrumb-root {
  color: var(--color-emerald);
}

.breadcrumb-separator {
  color: var(--color-terminal-dim);
}

.file-list-columns {
  display: grid;
  grid-template-columns: 32px 1fr 80px 70px;
  gap: 8px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--color-terminal-border-bright);
  background: rgba(148, 163, 184, 0.03);
}

.file-list-columns span {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-terminal-dim);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.col-size, .col-modified {
  text-align: right;
}

.file-list-content {
  flex: 1;
  overflow-y: auto;
}

/* Parent Directory Row */
.parent-dir {
  display: grid;
  grid-template-columns: 32px 1fr 80px 70px;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.1s ease;
  border-bottom: 1px solid var(--color-terminal-border);
}

.parent-dir:hover {
  background: var(--color-terminal-hover);
}

.parent-dir .file-icon {
  color: var(--color-amber);
  font-weight: 700;
  font-size: 11px;
}

.parent-dir .file-name {
  color: var(--color-amber);
  font-weight: 500;
}

/* Loading State */
.file-list-loading {
  padding: 8px 0;
}

.skeleton-row {
  display: grid;
  grid-template-columns: 32px 1fr 80px 70px;
  gap: 8px;
  padding: 12px 14px;
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-terminal-surface) 25%,
    var(--color-terminal-surface-elevated) 50%,
    var(--color-terminal-surface) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 2px;
}

.skeleton-icon {
  width: 20px;
  height: 14px;
}

.skeleton-name {
  height: 14px;
  width: 60%;
}

.skeleton-size {
  height: 14px;
  width: 50px;
  margin-left: auto;
}

.skeleton-date {
  height: 14px;
  width: 50px;
  margin-left: auto;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Error State */
.file-list-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  gap: 12px;
}

.error-icon {
  font-size: 24px;
  color: var(--color-error);
  font-weight: 700;
}

.error-message {
  color: var(--color-error);
  font-size: 12px;
  text-align: center;
}

/* Empty State */
.file-list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  gap: 12px;
}

.empty-icon {
  font-size: 24px;
  color: var(--color-terminal-dim);
  font-weight: 700;
}

.empty-message {
  color: var(--color-terminal-dim);
  font-size: 12px;
}
</style>

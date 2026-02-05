<script setup lang="ts">
import type { FileEntry } from '~/composables/useApi';

const props = defineProps<{
  file: FileEntry;
  selected?: boolean;
}>();

const emit = defineEmits<{
  select: [file: FileEntry];
  open: [file: FileEntry];
}>();

// Double-click detection with timeout
let clickCount = 0;
let clickTimer: ReturnType<typeof setTimeout> | null = null;
const DOUBLE_CLICK_DELAY = 300;

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '';

  // Parse "2026-01-09 12:54:48" format
  const date = new Date(dateStr.replace(' ', 'T'));
  if (isNaN(date.getTime())) return dateStr;

  const now = new Date();
  const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } else if (diffDays < 7) {
    return date.toLocaleDateString([], { weekday: 'short' });
  } else {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }
}

function getFileIcon(file: FileEntry): string {
  if (file.isDirectory) return '[+]';

  const ext = file.name.split('.').pop()?.toLowerCase() || '';
  const iconMap: Record<string, string> = {
    // Images
    jpg: '[@]', jpeg: '[@]', png: '[@]', gif: '[@]', webp: '[@]', svg: '[@]',
    // Documents
    pdf: '[#]', doc: '[#]', docx: '[#]', txt: '[=]', md: '[=]',
    // Code
    js: '[>]', ts: '[>]', json: '[>]', xml: '[>]', html: '[>]', css: '[>]',
    // Archives
    zip: '[%]', tar: '[%]', gz: '[%]', rar: '[%]', '7z': '[%]',
    // Media
    mp3: '[~]', wav: '[~]', mp4: '[~]', mkv: '[~]', avi: '[~]',
    // APK
    apk: '[*]',
  };

  return iconMap[ext] || '[-]';
}

function handleClick() {
  clickCount++;

  if (clickCount === 1) {
    // First click - start timer
    clickTimer = setTimeout(() => {
      // Single click - just select
      emit('select', props.file);
      clickCount = 0;
    }, DOUBLE_CLICK_DELAY);
  } else if (clickCount === 2) {
    // Double click - cancel timer and open
    if (clickTimer) {
      clearTimeout(clickTimer);
      clickTimer = null;
    }
    clickCount = 0;
    emit('select', props.file);
    emit('open', props.file);
  }
}
</script>

<template>
  <div
    class="file-row"
    :class="{ selected, directory: file.isDirectory }"
    @click="handleClick"
  >
    <span class="file-icon" :class="{ 'icon-folder': file.isDirectory }">
      {{ getFileIcon(file) }}
    </span>
    <span class="file-name">{{ file.name }}</span>
    <span v-if="!file.isDirectory" class="file-size">{{ formatSize(file.size) }}</span>
    <span v-else class="file-size file-dir-indicator">DIR</span>
    <span class="file-modified">{{ formatDate(file.lastModified) }}</span>
  </div>
</template>

<style scoped>
.file-row {
  display: grid;
  grid-template-columns: 32px 1fr 80px 70px;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.1s ease;
  border-bottom: 1px solid var(--color-terminal-border);
}

.file-row:hover {
  background: var(--color-terminal-hover);
}

.file-row.selected {
  background: rgba(34, 211, 238, 0.08);
  border-color: rgba(34, 211, 238, 0.2);
}

.file-row.directory {
  font-weight: 500;
}

.file-icon {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  color: var(--color-terminal-dim);
}

.file-icon.icon-folder {
  color: var(--color-amber);
}

.file-name {
  font-size: 13px;
  color: var(--color-terminal-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-row.directory .file-name {
  color: var(--color-amber);
}

.file-size {
  font-size: 11px;
  color: var(--color-terminal-dim);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.file-dir-indicator {
  color: var(--color-terminal-dim);
  opacity: 0.5;
}

.file-modified {
  font-size: 11px;
  color: var(--color-terminal-dim);
  text-align: right;
}
</style>

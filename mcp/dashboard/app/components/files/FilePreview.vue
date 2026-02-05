<script setup lang="ts">
import type { FileEntry } from '~/composables/useApi';

const props = defineProps<{
  file: FileEntry | null;
  content: string | null;
  loading?: boolean;
  error?: string | null;
  truncated?: boolean;
  encoding?: 'text' | 'base64';
  mimeType?: string;
}>();

const emit = defineEmits<{
  download: [file: FileEntry];
  delete: [file: FileEntry];
}>();

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function getFileExtension(filename: string): string {
  return filename.split('.').pop()?.toLowerCase() || '';
}

function isImageFile(filename: string): boolean {
  const ext = getFileExtension(filename);
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'].includes(ext);
}

function isTextFile(filename: string): boolean {
  const ext = getFileExtension(filename);
  const textExts = [
    'txt', 'md', 'json', 'xml', 'html', 'css', 'js', 'ts', 'jsx', 'tsx',
    'py', 'rb', 'java', 'kt', 'swift', 'c', 'cpp', 'h', 'go', 'rs',
    'sh', 'bash', 'zsh', 'yml', 'yaml', 'toml', 'ini', 'conf', 'cfg',
    'log', 'csv', 'sql', 'graphql', 'vue', 'svelte', 'php', 'pl', 'pm'
  ];
  return textExts.includes(ext);
}

function getLineNumbers(content: string): number[] {
  const lines = content.split('\n');
  return Array.from({ length: lines.length }, (_, i) => i + 1);
}

function getContentLines(content: string): string[] {
  return content.split('\n');
}

const imageDataUrl = computed(() => {
  if (!props.file || !props.content) return null;

  // If it's an image file
  if (isImageFile(props.file.name)) {
    // If explicitly marked as base64
    if (props.encoding === 'base64') {
      const ext = getFileExtension(props.file.name);
      const mime = props.mimeType || (ext === 'jpg' ? 'image/jpeg' : `image/${ext}`);
      return `data:${mime};base64,${props.content}`;
    }
    // Also try if content looks like base64 (fallback)
    const cleanContent = props.content.replace(/\s/g, '');
    if (/^[A-Za-z0-9+/]+=*$/.test(cleanContent) && cleanContent.length > 100) {
      const ext = getFileExtension(props.file.name);
      const mime = props.mimeType || (ext === 'jpg' ? 'image/jpeg' : `image/${ext}`);
      return `data:${mime};base64,${cleanContent}`;
    }
  }

  return null;
});
</script>

<template>
  <div class="file-preview">
    <!-- No File Selected -->
    <div v-if="!file" class="preview-empty">
      <span class="empty-icon">[?]</span>
      <span class="empty-text">Select a file to preview</span>
    </div>

    <!-- File Selected -->
    <template v-else>
      <!-- Preview Header -->
      <div class="preview-header">
        <div class="preview-file-info">
          <span class="preview-filename">{{ file.name }}</span>
          <span class="preview-size">({{ formatSize(file.size) }})</span>
        </div>
        <div class="preview-actions">
          <button
            class="btn-terminal btn-terminal-sm btn-terminal-primary"
            @click="emit('download', file)"
          >
            DOWNLOAD
          </button>
          <button
            class="btn-terminal btn-terminal-sm btn-terminal-danger"
            @click="emit('delete', file)"
          >
            DELETE
          </button>
        </div>
      </div>

      <!-- Preview Content -->
      <div class="preview-content">
        <!-- Loading -->
        <div v-if="loading" class="preview-loading">
          <div class="loading-spinner"></div>
          <span>Loading preview...</span>
        </div>

        <!-- Error -->
        <div v-else-if="error" class="preview-error">
          <span class="error-icon">[!]</span>
          <span class="error-text">{{ error }}</span>
        </div>

        <!-- Image Preview -->
        <div v-else-if="imageDataUrl" class="preview-image">
          <img :src="imageDataUrl" :alt="file.name" />
        </div>

        <!-- Text Preview -->
        <div v-else-if="content && (isTextFile(file.name) || encoding === 'text')" class="preview-text">
          <div class="line-numbers">
            <span v-for="num in getLineNumbers(content)" :key="num">{{ num }}</span>
          </div>
          <pre class="code-content"><code>{{ content }}</code></pre>
        </div>

        <!-- Binary / Unsupported -->
        <div v-else-if="content" class="preview-binary">
          <span class="binary-icon">[#]</span>
          <span class="binary-text">Binary file - Preview not available</span>
          <span class="binary-hint">Click "Download" to save the file</span>
        </div>

        <!-- Directory Selected -->
        <div v-else-if="file.isDirectory" class="preview-directory">
          <span class="dir-icon">[+]</span>
          <span class="dir-text">{{ file.name }}</span>
          <span class="dir-hint">Double-click to open directory</span>
        </div>

        <!-- No Content -->
        <div v-else class="preview-no-content">
          <span class="no-content-text">No preview available</span>
        </div>
      </div>

      <!-- Truncation Notice -->
      <div v-if="truncated" class="preview-truncated">
        <span class="truncated-icon">[...]</span>
        <span class="truncated-text">Preview truncated - showing first 1000 lines</span>
        <button
          class="btn-terminal btn-terminal-sm btn-terminal-ghost"
          @click="emit('download', file)"
        >
          Download Full File
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.file-preview {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* Empty State */
.preview-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--color-terminal-dim);
}

.empty-icon {
  font-size: 32px;
  font-weight: 700;
}

.empty-text {
  font-size: 12px;
}

/* Header */
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-terminal-border);
  background: rgba(148, 163, 184, 0.03);
  gap: 16px;
}

.preview-file-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.preview-filename {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-terminal-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-size {
  font-size: 11px;
  color: var(--color-terminal-dim);
  flex-shrink: 0;
}

.preview-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* Content Area */
.preview-content {
  flex: 1;
  overflow: auto;
  position: relative;
}

/* Loading */
.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: var(--color-terminal-dim);
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-terminal-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Error */
.preview-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  padding: 24px;
}

.preview-error .error-icon {
  font-size: 24px;
  color: var(--color-error);
  font-weight: 700;
}

.preview-error .error-text {
  font-size: 12px;
  color: var(--color-error);
  text-align: center;
}

/* Image Preview */
.preview-image {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 16px;
  background: repeating-conic-gradient(
    var(--color-terminal-surface) 0% 25%,
    var(--color-terminal-surface-elevated) 0% 50%
  ) 50% / 16px 16px;
}

.preview-image img {
  max-width: 100%;
  max-height: 70vh;
  width: auto;
  height: auto;
  object-fit: contain;
  border: 1px solid var(--color-terminal-border);
  border-radius: 4px;
}

/* Text Preview */
.preview-text {
  display: flex;
  height: 100%;
  font-size: 12px;
  line-height: 1.6;
}

.line-numbers {
  display: flex;
  flex-direction: column;
  padding: 12px 8px;
  background: rgba(148, 163, 184, 0.03);
  border-right: 1px solid var(--color-terminal-border);
  color: var(--color-terminal-dim);
  text-align: right;
  user-select: none;
  min-width: 48px;
}

.line-numbers span {
  font-size: 11px;
  line-height: 1.6;
}

.code-content {
  flex: 1;
  padding: 12px 16px;
  margin: 0;
  overflow: auto;
  white-space: pre;
  color: var(--color-terminal-text);
  font-family: var(--font-mono);
}

/* Binary Preview */
.preview-binary {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.binary-icon {
  font-size: 32px;
  color: var(--color-violet);
  font-weight: 700;
}

.binary-text {
  font-size: 13px;
  color: var(--color-terminal-text);
}

.binary-hint {
  font-size: 11px;
  color: var(--color-terminal-dim);
}

/* Directory Preview */
.preview-directory {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.dir-icon {
  font-size: 32px;
  color: var(--color-amber);
  font-weight: 700;
}

.dir-text {
  font-size: 13px;
  color: var(--color-amber);
  font-weight: 600;
}

.dir-hint {
  font-size: 11px;
  color: var(--color-terminal-dim);
}

/* No Content */
.preview-no-content {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-terminal-dim);
  font-size: 12px;
}

/* Truncation Notice */
.preview-truncated {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: rgba(245, 158, 11, 0.08);
  border-top: 1px solid rgba(245, 158, 11, 0.2);
}

.truncated-icon {
  color: var(--color-amber);
  font-weight: 700;
}

.truncated-text {
  flex: 1;
  font-size: 11px;
  color: var(--color-amber);
}
</style>

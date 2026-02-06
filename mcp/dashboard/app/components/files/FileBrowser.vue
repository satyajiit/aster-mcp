<script setup lang="ts">
import type { FileEntry, ToolResult } from '~/composables/useApi';

const props = defineProps<{
  deviceId: string;
  deviceOnline: boolean;
}>();

const api = useApi();

// State
const currentPath = ref('/sdcard');
const files = ref<FileEntry[]>([]);
const selectedFile = ref<FileEntry | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

// Preview state
const previewContent = ref<string | null>(null);
const previewLoading = ref(false);
const previewError = ref<string | null>(null);
const previewTruncated = ref(false);
const previewEncoding = ref<'text' | 'base64'>('text');
const previewMimeType = ref<string | undefined>(undefined);

// Modal state
const showCreateFolder = ref(false);
const creatingFolder = ref(false);
const showDeleteConfirm = ref(false);
const deletingFile = ref(false);
const fileToDelete = ref<FileEntry | null>(null);

// Constants
const MAX_PREVIEW_LINES = 1000;
const MAX_PREVIEW_SIZE = 100 * 1024; // 100KB

// Load directory contents
async function loadDirectory(path: string) {
  if (!props.deviceOnline) {
    error.value = 'Device is offline';
    return;
  }

  loading.value = true;
  error.value = null;
  selectedFile.value = null;
  previewContent.value = null;

  try {
    const result = await api.executeTool(props.deviceId, 'aster_list_files', { path });

    if (result.isError) {
      const errorText = result.content[0]?.text || 'Failed to list directory';
      error.value = errorText;
      return;
    }

    const responseText = result.content[0]?.text;
    if (responseText) {
      const parsed = JSON.parse(responseText);
      files.value = parsed.files || [];
      currentPath.value = path;
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load directory';
  } finally {
    loading.value = false;
  }
}

// Check if file is an image based on extension
function isImageFile(filename: string): boolean {
  const ext = filename.split('.').pop()?.toLowerCase() || '';
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext);
}

// Get mime type from extension
function getMimeType(filename: string): string {
  const ext = filename.split('.').pop()?.toLowerCase() || '';
  const mimeMap: Record<string, string> = {
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    gif: 'image/gif',
    webp: 'image/webp',
    bmp: 'image/bmp',
  };
  return mimeMap[ext] || 'application/octet-stream';
}

// Load file preview
async function loadPreview(file: FileEntry) {
  if (file.isDirectory) {
    previewContent.value = null;
    previewError.value = null;
    return;
  }

  previewLoading.value = true;
  previewError.value = null;
  previewContent.value = null;
  previewTruncated.value = false;

  try {
    const result = await api.executeTool(props.deviceId, 'aster_read_file', { path: file.path });

    if (result.isError) {
      const errorText = result.content[0]?.text || 'Failed to read file';
      previewError.value = errorText;
      return;
    }

    // Check for explicit image content type
    const imageContent = result.content.find(c => c.type === 'image');
    if (imageContent && imageContent.data) {
      previewContent.value = imageContent.data;
      previewEncoding.value = 'base64';
      previewMimeType.value = imageContent.mimeType;
      return;
    }

    // Text content
    const textContent = result.content.find(c => c.type === 'text');
    if (textContent?.text) {
      let content = textContent.text;

      // Try to parse as JSON (API wraps file content in JSON structure)
      try {
        const parsed = JSON.parse(content);
        if (parsed && typeof parsed === 'object') {
          // Check if it's a base64 encoded file response
          if (parsed.encoding === 'base64' && parsed.content) {
            previewContent.value = parsed.content;
            previewEncoding.value = 'base64';
            previewMimeType.value = getMimeType(file.name);
            return;
          }
          // If it has a content field with text encoding
          if (parsed.content && parsed.encoding !== 'base64') {
            content = parsed.content;
          }
        }
      } catch {
        // Not JSON, treat as raw content
      }

      // If this is an image file, check if content is raw base64
      if (isImageFile(file.name)) {
        const cleanedContent = content.replace(/\s/g, '');
        if (/^[A-Za-z0-9+/]+=*$/.test(cleanedContent)) {
          previewContent.value = cleanedContent;
          previewEncoding.value = 'base64';
          previewMimeType.value = getMimeType(file.name);
          return;
        }
      }

      // Regular text content
      previewEncoding.value = 'text';

      // Truncate if too large
      const lines = content.split('\n');
      if (lines.length > MAX_PREVIEW_LINES) {
        content = lines.slice(0, MAX_PREVIEW_LINES).join('\n');
        previewTruncated.value = true;
      } else if (content.length > MAX_PREVIEW_SIZE) {
        content = content.slice(0, MAX_PREVIEW_SIZE);
        previewTruncated.value = true;
      }

      previewContent.value = content;
    }
  } catch (e) {
    previewError.value = e instanceof Error ? e.message : 'Failed to load preview';
  } finally {
    previewLoading.value = false;
  }
}

// Handle file/folder selection
function handleSelect(file: FileEntry) {
  selectedFile.value = file;
  if (!file.isDirectory) {
    loadPreview(file);
  }
}

// Handle file/folder open (double-click)
function handleOpen(file: FileEntry) {
  if (file.isDirectory) {
    loadDirectory(file.path);
  } else {
    handleSelect(file);
  }
}

// Navigate up one directory
function navigateUp() {
  const parentPath = currentPath.value.split('/').slice(0, -1).join('/') || '/';
  loadDirectory(parentPath);
}

// Navigate to quick access path
function navigateToPath(path: string) {
  loadDirectory(path);
}

// Download file
async function downloadFile(file: FileEntry) {
  if (file.isDirectory) return;

  try {
    const result = await api.executeTool(props.deviceId, 'aster_read_file', { path: file.path });

    if (result.isError) {
      alert('Failed to download file: ' + (result.content[0]?.text || 'Unknown error'));
      return;
    }

    let blob: Blob;
    const imageContent = result.content.find(c => c.type === 'image');
    const textContent = result.content.find(c => c.type === 'text');

    if (imageContent?.data) {
      // Base64 image
      const byteString = atob(imageContent.data);
      const bytes = new Uint8Array(byteString.length);
      for (let i = 0; i < byteString.length; i++) {
        bytes[i] = byteString.charCodeAt(i);
      }
      blob = new Blob([bytes], { type: imageContent.mimeType || 'application/octet-stream' });
    } else if (textContent?.text) {
      let content = textContent.text;
      let isBase64 = false;

      // Try to parse as JSON (API wraps file content in JSON structure)
      try {
        const parsed = JSON.parse(content);
        if (parsed && typeof parsed === 'object' && parsed.content) {
          if (parsed.encoding === 'base64') {
            content = parsed.content;
            isBase64 = true;
          } else {
            content = parsed.content;
          }
        }
      } catch {
        // Not JSON, treat as raw content
      }

      // Check if it's base64 binary content
      if (isBase64 || (content.match(/^[A-Za-z0-9+/]+=*$/) && content.length > 100)) {
        try {
          const byteString = atob(content);
          const bytes = new Uint8Array(byteString.length);
          for (let i = 0; i < byteString.length; i++) {
            bytes[i] = byteString.charCodeAt(i);
          }
          blob = new Blob([bytes], { type: getMimeType(file.name) });
        } catch {
          blob = new Blob([content], { type: 'text/plain' });
        }
      } else {
        blob = new Blob([content], { type: 'text/plain' });
      }
    } else {
      alert('No content to download');
      return;
    }

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = file.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch (e) {
    alert('Download failed: ' + (e instanceof Error ? e.message : 'Unknown error'));
  }
}

// Delete file
function confirmDelete(file: FileEntry) {
  fileToDelete.value = file;
  showDeleteConfirm.value = true;
}

async function executeDelete() {
  if (!fileToDelete.value) return;

  deletingFile.value = true;

  try {
    const result = await api.executeTool(props.deviceId, 'aster_delete_file', {
      path: fileToDelete.value.path
    });

    if (result.isError) {
      alert('Failed to delete: ' + (result.content[0]?.text || 'Unknown error'));
      return;
    }

    // Refresh directory
    await loadDirectory(currentPath.value);

    // Clear selection if deleted file was selected
    if (selectedFile.value?.path === fileToDelete.value.path) {
      selectedFile.value = null;
      previewContent.value = null;
    }

    showDeleteConfirm.value = false;
    fileToDelete.value = null;
  } catch (e) {
    alert('Delete failed: ' + (e instanceof Error ? e.message : 'Unknown error'));
  } finally {
    deletingFile.value = false;
  }
}

// Create folder
async function createFolder(name: string) {
  creatingFolder.value = true;

  try {
    const folderPath = `${currentPath.value}/${name}`;
    // Create folder by writing a .keep file inside it
    const result = await api.executeTool(props.deviceId, 'aster_write_file', {
      path: `${folderPath}/.keep`,
      content: ''
    });

    if (result.isError) {
      alert('Failed to create folder: ' + (result.content[0]?.text || 'Unknown error'));
      return;
    }

    // Refresh directory
    await loadDirectory(currentPath.value);
    showCreateFolder.value = false;
  } catch (e) {
    alert('Create folder failed: ' + (e instanceof Error ? e.message : 'Unknown error'));
  } finally {
    creatingFolder.value = false;
  }
}

// Keyboard navigation
function handleKeydown(event: KeyboardEvent) {
  if (showCreateFolder.value || showDeleteConfirm.value) return;

  const currentIndex = selectedFile.value
    ? files.value.findIndex(f => f.path === selectedFile.value?.path)
    : -1;

  switch (event.key) {
    case 'ArrowUp':
      event.preventDefault();
      if (currentIndex > 0) {
        handleSelect(files.value[currentIndex - 1]);
      } else if (currentIndex === -1 && files.value.length > 0) {
        handleSelect(files.value[files.value.length - 1]);
      }
      break;
    case 'ArrowDown':
      event.preventDefault();
      if (currentIndex < files.value.length - 1) {
        handleSelect(files.value[currentIndex + 1]);
      } else if (currentIndex === -1 && files.value.length > 0) {
        handleSelect(files.value[0]);
      }
      break;
    case 'Enter':
      if (selectedFile.value) {
        handleOpen(selectedFile.value);
      }
      break;
    case 'Backspace':
      if (currentPath.value !== '/' && currentPath.value !== '') {
        navigateUp();
      }
      break;
    case 'Delete':
      if (selectedFile.value) {
        confirmDelete(selectedFile.value);
      }
      break;
  }
}

// Watch for device online status changes
watch(() => props.deviceOnline, (online) => {
  if (!online) {
    error.value = 'Device is offline';
  } else if (error.value === 'Device is offline') {
    loadDirectory(currentPath.value);
  }
});

// Initial load
onMounted(() => {
  loadDirectory(currentPath.value);
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<template>
  <div class="file-browser" :class="{ offline: !deviceOnline }">
    <!-- Offline Banner -->
    <div v-if="!deviceOnline" class="offline-banner">
      <span class="offline-icon">[!]</span>
      <span>Device is offline - File operations disabled</span>
    </div>

    <!-- Quick Access Bar -->
    <QuickAccessBar
      :current-path="currentPath"
      @navigate="navigateToPath"
    />

    <!-- Main Content -->
    <div class="browser-content">
      <!-- File List Panel -->
      <div class="file-list-panel">
        <div class="panel-header">
          <button
            class="btn-terminal btn-terminal-sm btn-terminal-ghost"
            :disabled="currentPath === '/' || currentPath === '' || !deviceOnline"
            @click="navigateUp"
          >
            [^] UP
          </button>
          <button
            class="btn-terminal btn-terminal-sm btn-terminal-primary"
            :disabled="!deviceOnline"
            @click="showCreateFolder = true"
          >
            [+] NEW FOLDER
          </button>
        </div>

        <FileList
          :files="files"
          :selected-file="selectedFile"
          :loading="loading"
          :error="error"
          :current-path="currentPath"
          @select="handleSelect"
          @open="handleOpen"
          @navigate-up="navigateUp"
        />
      </div>

      <!-- Preview Panel -->
      <div class="preview-panel">
        <FilePreview
          :file="selectedFile"
          :content="previewContent"
          :loading="previewLoading"
          :error="previewError"
          :truncated="previewTruncated"
          :encoding="previewEncoding"
          :mime-type="previewMimeType"
          @download="downloadFile"
          @delete="confirmDelete"
        />
      </div>
    </div>

    <!-- Create Folder Modal -->
    <CreateFolderModal
      :open="showCreateFolder"
      :current-path="currentPath"
      :creating="creatingFolder"
      @close="showCreateFolder = false"
      @create="createFolder"
    />

    <!-- Delete Confirm Modal -->
    <DeleteConfirmModal
      :open="showDeleteConfirm"
      :file="fileToDelete"
      :deleting="deletingFile"
      @close="showDeleteConfirm = false; fileToDelete = null"
      @confirm="executeDelete"
    />
  </div>
</template>

<style scoped>
.file-browser {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-terminal-surface);
  border: 1px solid var(--color-terminal-border);
  border-radius: 2px;
  overflow: hidden;
}

.file-browser.offline {
  opacity: 0.7;
}

.offline-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: rgba(239, 68, 68, 0.1);
  border-bottom: 1px solid rgba(239, 68, 68, 0.2);
  color: var(--color-error);
  font-size: 12px;
}

.offline-icon {
  font-weight: 700;
}

.browser-content {
  display: flex;
  flex: 1;
  min-height: 0;
}

.file-list-panel {
  display: flex;
  flex-direction: column;
  width: 45%;
  min-width: 300px;
  max-width: 500px;
  border-right: 1px solid var(--color-terminal-border);
  overflow: hidden;
}

.panel-header {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--color-terminal-border);
  background: rgba(148, 163, 184, 0.02);
}

.preview-panel {
  flex: 1;
  min-width: 0;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .browser-content {
    flex-direction: column;
  }

  .file-list-panel {
    width: 100%;
    max-width: none;
    min-width: 0;
    height: 50%;
    border-right: none;
    border-bottom: 1px solid var(--color-terminal-border);
  }

  .preview-panel {
    height: 50%;
  }
}
</style>

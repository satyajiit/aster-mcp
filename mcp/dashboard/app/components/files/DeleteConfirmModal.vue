<script setup lang="ts">
import type { FileEntry } from '~/composables/useApi';

const props = defineProps<{
  open: boolean;
  file: FileEntry | null;
  deleting?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  confirm: [];
}>();

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    emit('close');
  } else if (event.key === 'Enter' && !props.deleting) {
    emit('confirm');
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open && file" class="modal-overlay" @click.self="emit('close')" @keydown="handleKeydown">
        <div class="modal-content terminal-window">
          <div class="terminal-titlebar">
            <div class="terminal-dots">
              <span class="terminal-dot terminal-dot-close" @click="emit('close')"></span>
              <span class="terminal-dot terminal-dot-minimize"></span>
              <span class="terminal-dot terminal-dot-maximize"></span>
            </div>
            <div class="terminal-title">CONFIRM DELETE</div>
            <div style="width: 52px;"></div>
          </div>

          <div class="modal-body">
            <div class="delete-warning">
              <span class="warning-icon">[!]</span>
              <div class="warning-content">
                <p class="warning-title">Delete {{ file.isDirectory ? 'folder' : 'file' }}?</p>
                <p class="warning-filename">{{ file.name }}</p>
                <p class="warning-text">This action cannot be undone.</p>
              </div>
            </div>

            <div class="modal-actions">
              <button
                class="btn-terminal btn-terminal-ghost"
                :disabled="deleting"
                @click="emit('close')"
              >
                CANCEL
              </button>
              <button
                class="btn-terminal btn-terminal-danger"
                :disabled="deleting"
                @click="emit('confirm')"
              >
                <span v-if="deleting" class="deleting-spinner"></span>
                {{ deleting ? 'DELETING...' : 'DELETE' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  width: 100%;
  max-width: 400px;
  margin: 16px;
}

.modal-body {
  padding: 20px;
}

.delete-warning {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.warning-icon {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-error);
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
}

.warning-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-terminal-text);
  margin-bottom: 8px;
}

.warning-filename {
  font-size: 13px;
  color: var(--color-primary);
  background: rgba(34, 211, 238, 0.1);
  padding: 8px 12px;
  border-radius: 2px;
  border: 1px solid rgba(34, 211, 238, 0.2);
  margin-bottom: 12px;
  word-break: break-all;
}

.warning-text {
  font-size: 12px;
  color: var(--color-terminal-dim);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.deleting-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(239, 68, 68, 0.3);
  border-top-color: var(--color-error);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-content,
.modal-leave-to .modal-content {
  transform: scale(0.95) translateY(-10px);
}
</style>

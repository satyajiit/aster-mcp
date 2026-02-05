<script setup lang="ts">
const props = defineProps<{
  open: boolean;
  currentPath: string;
  creating?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  create: [name: string];
}>();

const folderName = ref('');
const inputRef = ref<HTMLInputElement | null>(null);

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    folderName.value = '';
    nextTick(() => {
      inputRef.value?.focus();
    });
  }
});

function handleSubmit() {
  const name = folderName.value.trim();
  if (name && !props.creating) {
    emit('create', name);
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    emit('close');
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-overlay" @click.self="emit('close')" @keydown="handleKeydown">
        <div class="modal-content terminal-window">
          <div class="terminal-titlebar">
            <div class="terminal-dots">
              <span class="terminal-dot terminal-dot-close" @click="emit('close')"></span>
              <span class="terminal-dot terminal-dot-minimize"></span>
              <span class="terminal-dot terminal-dot-maximize"></span>
            </div>
            <div class="terminal-title">CREATE FOLDER</div>
            <div style="width: 52px;"></div>
          </div>

          <div class="modal-body">
            <div class="modal-prompt">
              <span class="prompt-symbol">$</span>
              <span class="prompt-text">mkdir</span>
              <span class="prompt-path">{{ currentPath }}/</span>
            </div>

            <form @submit.prevent="handleSubmit">
              <input
                ref="inputRef"
                v-model="folderName"
                type="text"
                class="input-terminal folder-input"
                placeholder="folder_name"
                :disabled="creating"
                pattern="[^/\\:*?\"<>|]+"
                required
              />

              <div class="modal-actions">
                <button
                  type="button"
                  class="btn-terminal btn-terminal-ghost"
                  :disabled="creating"
                  @click="emit('close')"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  class="btn-terminal btn-terminal-primary"
                  :disabled="!folderName.trim() || creating"
                >
                  <span v-if="creating" class="creating-spinner"></span>
                  {{ creating ? 'CREATING...' : 'CREATE' }}
                </button>
              </div>
            </form>
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
  max-width: 420px;
  margin: 16px;
}

.modal-body {
  padding: 20px;
}

.modal-prompt {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 13px;
}

.prompt-symbol {
  color: var(--color-primary);
  font-weight: 700;
}

.prompt-text {
  color: var(--color-emerald);
}

.prompt-path {
  color: var(--color-terminal-muted);
}

.folder-input {
  margin-bottom: 20px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.creating-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(34, 211, 238, 0.3);
  border-top-color: var(--color-primary);
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

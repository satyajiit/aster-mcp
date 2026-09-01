<script setup lang="ts">
/** One modal shell, replacing the ~80 lines of overlay CSS copy-pasted between
 *  CreateFolderModal.vue and DeleteConfirmModal.vue. */
const open = defineModel<boolean>('open', { default: false });

withDefaults(
  defineProps<{ title: string; description?: string | null; width?: number }>(),
  { description: null, width: 420 },
);

function close() {
  open.value = false;
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && open.value) close();
}

onMounted(() => window.addEventListener('keydown', onKeydown));
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown));
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal" role="dialog" aria-modal="true" @click.self="close">
        <div class="modal__panel" :style="{ maxWidth: `${width}px` }">
          <header class="modal__head">
            <div>
              <h2 class="modal__title">{{ title }}</h2>
              <p v-if="description" class="modal__desc">{{ description }}</p>
            </div>
            <button type="button" class="modal__close" aria-label="Close" @click="close">
              <Icon name="ph:x" />
            </button>
          </header>
          <div class="modal__body"><slot /></div>
          <footer v-if="$slots.footer" class="modal__foot"><slot name="footer" /></footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  background: color-mix(in oklab, var(--color-bg) 75%, transparent);
  backdrop-filter: blur(6px);
}

.modal__panel {
  width: 100%;
  border-radius: var(--radius-hero);
  background: var(--color-surface-1);
  border: 1px solid var(--color-border-bright);
  overflow: hidden;
}

.modal__head {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 1rem 0.75rem;
}

.modal__title {
  margin: 0;
  font-size: var(--text-title-lg);
  font-weight: 600;
  color: var(--color-fg);
}

.modal__desc {
  margin: 0.25rem 0 0;
  font-size: var(--text-body-md);
  color: var(--color-fg-subtle);
}

.modal__close {
  margin-left: auto;
  border: none;
  background: none;
  color: var(--color-fg-muted);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--radius-xs);
  font-size: 1.125rem;
}

.modal__close:hover {
  color: var(--color-fg);
  background: var(--color-surface-2);
}

.modal__body {
  padding: 0 1rem 1rem;
}

.modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-2);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity var(--dur-base) var(--ease-out-soft);
}

.modal-enter-active .modal__panel,
.modal-leave-active .modal__panel {
  transition: transform var(--dur-base) var(--ease-out-soft);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal__panel,
.modal-leave-to .modal__panel {
  transform: translateY(12px) scale(0.98);
}
</style>

<script setup lang="ts">
/** Renders the toast queue. Mount once in the layout.
 *  Replaces the five native alert() calls in the old FileBrowser. */
const { toasts, dismiss } = useToast();

const ICONS: Record<string, string> = {
  success: 'ph:check-circle',
  error: 'ph:warning-circle',
  info: 'ph:info',
};

const TONES: Record<string, string> = {
  success: 'var(--color-success)',
  error: 'var(--color-error)',
  info: 'var(--color-info)',
};
</script>

<template>
  <Teleport to="body">
    <div class="toasts" role="region" aria-live="polite">
      <TransitionGroup name="toast">
        <div
          v-for="t in toasts"
          :key="t.id"
          class="toast"
          :style="{ '--toast-color': TONES[t.type] }"
        >
          <Icon :name="ICONS[t.type]" class="toast__icon" />
          <div class="toast__body">
            <p class="toast__title">{{ t.title }}</p>
            <p v-if="t.description" class="toast__desc">{{ t.description }}</p>
          </div>
          <button type="button" class="toast__close" aria-label="Dismiss" @click="dismiss(t.id)">
            <Icon name="ph:x" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toasts {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: min(360px, calc(100vw - 2rem));
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.75rem;
  border-radius: var(--radius-card);
  background: var(--color-surface-2);
  border: 1px solid color-mix(in oklab, var(--toast-color) 35%, var(--color-border));
  pointer-events: auto;
}

.toast__icon {
  color: var(--toast-color);
  font-size: 1.125rem;
  flex: none;
  margin-top: 1px;
}

.toast__body {
  min-width: 0;
  flex: 1;
}

.toast__title {
  margin: 0;
  font-size: var(--text-body-md);
  font-weight: 600;
  color: var(--color-fg);
}

.toast__desc {
  margin: 0.125rem 0 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
  overflow-wrap: anywhere;
}

.toast__close {
  border: none;
  background: none;
  color: var(--color-fg-muted);
  cursor: pointer;
  flex: none;
}

.toast__close:hover {
  color: var(--color-fg);
}

.toast-enter-active,
.toast-leave-active {
  transition: all var(--dur-base) var(--ease-out-soft);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(16px);
}
</style>

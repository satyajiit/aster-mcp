<script setup lang="ts">
interface Props {
  description: string;
  loading?: boolean;
  showRefresh?: boolean;
}

withDefaults(defineProps<Props>(), {
  loading: false,
  showRefresh: true,
});

defineEmits<{
  refresh: [];
}>();

const ascii = `
 █████╗ ███████╗████████╗███████╗██████╗
██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔══██╗
███████║███████╗   ██║   █████╗  ██████╔╝
██╔══██║╚════██║   ██║   ██╔══╝  ██╔══██╗
██║  ██║███████║   ██║   ███████╗██║  ██║
╚═╝  ╚═╝╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝
`.trim();
</script>

<template>
  <header class="terminal-page-header">
    <div class="terminal-frame">
      <div class="terminal-content">
        <div class="header-row">
          <!-- Left: Actions slot or Refresh -->
          <div class="header-left">
            <slot name="left">
              <button
                v-if="showRefresh"
                class="refresh-btn"
                :class="{ 'is-loading': loading }"
                :disabled="loading"
                @click="$emit('refresh')"
              >
                <Icon name="ph:arrows-clockwise" class="refresh-icon" size="14" />
                <span class="refresh-text">{{ loading ? 'SYNCING' : 'REFRESH' }}</span>
              </button>
            </slot>
          </div>

          <!-- Center: ASCII Logo + Description -->
          <div class="logo-block">
            <pre class="ascii-logo">{{ ascii }}</pre>
            <p class="terminal-description">{{ description }}</p>
          </div>

          <!-- Right: Actions -->
          <div class="header-right">
            <slot name="actions" />
          </div>
        </div>
      </div>
    </div>
    <div class="scanlines"></div>
  </header>
</template>

<style scoped>
.terminal-page-header {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
}

.terminal-frame {
  position: relative;
  background: rgba(30, 41, 59, 0.95);
  border: 1px solid var(--color-terminal-border);
  border-radius: 2px;
  overflow: hidden;
}

.terminal-content {
  padding: 20px 24px;
}

.header-row {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

/* Logo Block */
.logo-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.ascii-logo {
  font-size: 10px;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: #22d3ee;
  user-select: none;
  text-shadow:
    0 0 5px rgba(34, 211, 238, 0.4),
    0 0 20px rgba(34, 211, 238, 0.2),
    0 0 40px rgba(34, 211, 238, 0.1);
  margin: 0;
  font-family: var(--font-mono);
}

.terminal-description {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-terminal-muted);
  margin: 0;
  line-height: 1.4;
  text-transform: uppercase;
  letter-spacing: 0.2em;
}

.terminal-description::before {
  content: '// ';
  opacity: 0.5;
}

/* Header Right */
.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

/* Refresh Button */
.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: transparent;
  border: 1px solid var(--color-terminal-border-bright);
  border-radius: 2px;
  color: var(--color-terminal-muted);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  color: var(--color-primary);
  border-color: rgba(34, 211, 238, 0.4);
  background: rgba(34, 211, 238, 0.05);
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.refresh-icon {
  transition: transform 0.3s ease;
}

.refresh-btn.is-loading .refresh-icon {
  animation: spin 1s linear infinite;
}

:deep(.refresh-icon) {
  flex-shrink: 0;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Scanlines */
.scanlines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 2px,
    rgba(148, 163, 184, 0.015) 2px,
    rgba(148, 163, 184, 0.015) 4px
  );
  border-radius: 2px;
  z-index: 10;
}

/* Responsive */
@media (max-width: 768px) {
  .terminal-content {
    padding: 16px;
  }

  .header-row {
    grid-template-columns: 1fr;
    gap: 16px;
    justify-items: center;
  }

  .header-left {
    order: 2;
    justify-content: center;
  }

  .logo-block {
    order: 1;
  }

  .header-right {
    order: 3;
    justify-content: center;
  }

  .ascii-logo {
    font-size: 8px;
  }
}
</style>

<script setup lang="ts">
interface Props {
  title: string;
  id?: string;
  idLabel?: string;
  icon?: string;
  status?: string;
  statusColor?: 'success' | 'warning' | 'error' | 'info' | 'default';
  loading?: boolean;
}

withDefaults(defineProps<Props>(), {
  icon: '◎',
  idLabel: 'ID',
  statusColor: 'default',
  loading: false,
});
</script>

<template>
  <header class="terminal-detail-header">
    <div class="terminal-frame">
      <div class="terminal-content">
        <!-- Main Header Row -->
        <div class="header-row">
          <div class="header-left">
            <!-- ID Badge -->
            <div v-if="id" class="id-badge">
              <div class="badge-icon">
                <span>{{ icon }}</span>
              </div>
              <div class="badge-info">
                <span class="badge-label">{{ idLabel }}</span>
                <span class="badge-value">{{ id }}</span>
              </div>
            </div>

            <!-- Title & Status -->
            <div class="title-block">
              <h1 class="terminal-title">
                {{ title || 'Loading...' }}
                <span v-if="loading" class="title-loader">_</span>
              </h1>
              <div v-if="status" class="meta-tags">
                <div class="status-chip" :class="`status-${statusColor}`">
                  <div class="status-dot"></div>
                  <span>{{ status }}</span>
                </div>
                <slot name="meta"></slot>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="header-right">
            <slot name="actions"></slot>
          </div>
        </div>

        <!-- Secondary Row (optional) -->
        <div v-if="$slots['secondary']" class="header-secondary">
          <slot name="secondary"></slot>
        </div>
      </div>
    </div>
    <div class="scanlines"></div>
  </header>
</template>

<style scoped>
.terminal-detail-header {
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

/* ID Badge */
.id-badge {
  display: flex;
  align-items: stretch;
  background: rgba(148, 163, 184, 0.03);
  border: 1px solid var(--color-terminal-border);
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.badge-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  background: rgba(34, 211, 238, 0.1);
  color: var(--color-primary);
  font-size: 20px;
  border-right: 1px solid var(--color-terminal-border);
}

.badge-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px 14px;
}

.badge-label {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 500;
  text-transform: uppercase;
  color: var(--color-terminal-muted);
  letter-spacing: 0.1em;
  line-height: 1.2;
}

.badge-value {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  color: var(--color-terminal-text-bright);
  letter-spacing: 0.03em;
}

/* Title Block */
.title-block {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.terminal-title {
  font-family: var(--font-mono);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-terminal-text-bright);
  margin: 0;
  line-height: 1.2;
}

.title-loader {
  animation: blink 1s step-end infinite;
  color: var(--color-primary);
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Meta Tags */
.meta-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

/* Status Chips */
.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 2px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: rgba(148, 163, 184, 0.05);
  color: var(--color-terminal-muted);
  border: 1px solid var(--color-terminal-border);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-success {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.25);
}

.status-success .status-dot {
  box-shadow: 0 0 6px #10b981;
}

.status-warning {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.25);
}

.status-warning .status-dot {
  box-shadow: 0 0 6px #f59e0b;
}

.status-error {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.25);
}

.status-error .status-dot {
  box-shadow: 0 0 6px #ef4444;
}

.status-info {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.25);
}

/* Header Right (Actions) */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

/* Secondary Row */
.header-secondary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--color-terminal-border);
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
    flex-direction: column;
    align-items: stretch;
  }

  .header-left {
    flex-direction: column;
    gap: 16px;
  }

  .header-right {
    justify-content: flex-start;
    padding-top: 16px;
    border-top: 1px solid var(--color-terminal-border);
  }

  .badge-icon {
    width: 40px;
  }

  .terminal-title {
    font-size: 1.25rem;
  }
}
</style>

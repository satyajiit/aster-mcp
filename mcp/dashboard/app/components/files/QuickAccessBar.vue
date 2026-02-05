<script setup lang="ts">
defineProps<{
  currentPath: string;
}>();

const emit = defineEmits<{
  navigate: [path: string];
}>();

const quickAccessPaths = [
  { label: 'Storage', path: '/sdcard', icon: '~' },
  { label: 'Downloads', path: '/sdcard/Download', icon: 'v' },
  { label: 'DCIM', path: '/sdcard/DCIM', icon: '#' },
  { label: 'Documents', path: '/sdcard/Documents', icon: '@' },
  { label: 'Pictures', path: '/sdcard/Pictures', icon: '*' },
];

function isActive(path: string, currentPath: string): boolean {
  return currentPath === path || currentPath.startsWith(path + '/');
}
</script>

<template>
  <div class="quick-access-bar">
    <span class="quick-access-label">// QUICK ACCESS</span>
    <div class="quick-access-buttons">
      <button
        v-for="item in quickAccessPaths"
        :key="item.path"
        class="quick-access-btn"
        :class="{ active: isActive(item.path, currentPath) }"
        @click="emit('navigate', item.path)"
      >
        <span class="quick-access-icon">{{ item.icon }}</span>
        {{ item.label }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.quick-access-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: rgba(148, 163, 184, 0.03);
  border-bottom: 1px solid var(--color-terminal-border);
}

.quick-access-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-terminal-dim);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  flex-shrink: 0;
}

.quick-access-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.quick-access-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--color-terminal-border);
  color: var(--color-terminal-muted);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border-radius: 2px;
}

.quick-access-btn:hover {
  background: var(--color-terminal-hover);
  border-color: var(--color-terminal-border-bright);
  color: var(--color-terminal-text);
}

.quick-access-btn.active {
  background: rgba(34, 211, 238, 0.1);
  border-color: rgba(34, 211, 238, 0.3);
  color: var(--color-primary);
}

.quick-access-icon {
  color: var(--color-primary);
  font-weight: 700;
}
</style>

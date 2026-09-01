<script setup lang="ts">
/** Mirrors CodeBlock.kt: 8px radius, 1px border, surface-2, mono, copy affordance. */
const props = withDefaults(
  defineProps<{ code: string; label?: string | null; copyable?: boolean }>(),
  { label: null, copyable: true },
);

const copied = ref(false);
let resetTimer: ReturnType<typeof setTimeout> | undefined;

async function copy() {
  try {
    await navigator.clipboard.writeText(props.code);
    copied.value = true;
    clearTimeout(resetTimer);
    resetTimer = setTimeout(() => (copied.value = false), 1600);
  } catch {
    copied.value = false;
  }
}

onScopeDispose(() => clearTimeout(resetTimer));
</script>

<template>
  <div class="code">
    <div v-if="label || copyable" class="code__bar">
      <span v-if="label" class="code__label">{{ label }}</span>
      <button v-if="copyable" type="button" class="code__copy" @click="copy">
        <Icon :name="copied ? 'ph:check' : 'ph:copy'" />
        {{ copied ? 'Copied' : 'Copy' }}
      </button>
    </div>
    <pre class="code__pre"><code>{{ code }}</code></pre>
  </div>
</template>

<style scoped>
.code {
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  overflow: hidden;
}

.code__bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.code__label {
  font-size: var(--text-label-sm);
  letter-spacing: var(--text-label-sm--letter-spacing);
  text-transform: uppercase;
  color: var(--color-fg-subtle);
}

.code__copy {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  border: none;
  background: none;
  color: var(--color-fg-subtle);
  font: inherit;
  font-size: var(--text-label-sm);
  cursor: pointer;
  padding: 0.125rem 0.25rem;
  border-radius: var(--radius-xs);
}

.code__copy:hover {
  color: var(--color-primary);
}

.code__pre {
  margin: 0;
  padding: 0.75rem;
  overflow-x: auto;
  font-family: var(--font-mono);
  font-size: var(--text-body-sm);
  line-height: 1.6;
  color: var(--color-fg);
}
</style>

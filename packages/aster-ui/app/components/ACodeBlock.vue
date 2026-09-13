<script setup lang="ts">
/** Mirrors CodeBlock.kt: 8px radius, 1px border, surface-2, mono, copy affordance. */
const props = withDefaults(
  defineProps<{ code: string; label?: string | null; copyable?: boolean }>(),
  { label: null, copyable: true },
);

const copied = ref(false);
let resetTimer: ReturnType<typeof setTimeout> | undefined;

/** The <pre> is a horizontal scroll container, so it needs a keyboard handle
    (WCAG 2.1.1) and therefore an accessible name. aria-label is ignored on a
    bare <pre> (role=generic forbids naming), hence role="group" below — group
    takes a name without minting a landmark, which region would do once per
    code block. */
const scrollLabel = computed(() =>
  props.label ? `Code sample: ${props.label}` : "Code sample",
);

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
    <pre class="code__pre" tabindex="0" role="group" :aria-label="scrollLabel"><code>{{ code }}</code></pre>
  </div>
</template>

<style scoped>
.code {
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  overflow: hidden;
  /* A flex or grid item defaults to min-width:auto, so the <pre>'s min-content
     width (the longest unbroken line) becomes this block's floor and the item
     refuses to shrink — the block then overflows and an ancestor's
     overflow:hidden clips it with no scrollbar to recover. Measured live: a
     919px .code inside a 375px viewport. Own it here so no call site has to. */
  min-width: 0;
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
  /* 11px label at line-height 1.5 plus 2px of padding each side is 20.5px tall,
     under the 24x24 minimum target size (WCAG 2.5.8). The bar's own padding
     absorbs the extra 3.5px, so nothing else moves. */
  min-height: 1.5rem;
}

.code__copy:hover {
  color: var(--color-primary);
}

.code__copy:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
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

/* Inset, because .code clips at its border box: the <pre> fills that box, so a
   positive offset would be drawn entirely under the parent's overflow:hidden. */
.code__pre:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}
</style>

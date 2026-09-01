<script setup lang="ts">
import type { ToolResult } from '~/composables/useApi';
import { toolImageSrc } from '~/composables/useApi';

defineProps<{ result: ToolResult | null }>();

/** Pretty-print JSON payloads; leave prose alone. */
function present(text: string): string {
  const trimmed = text.trim();
  if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) return text;
  try {
    return JSON.stringify(JSON.parse(trimmed), null, 2);
  } catch {
    return text;
  }
}
</script>

<template>
  <div v-if="result" class="res">
    <div class="res__bar">
      <ABadge :tone="result.isError ? 'error' : 'success'">
        {{ result.isError ? 'Error' : 'Success' }}
      </ABadge>
    </div>

    <div v-for="(item, i) in result.content" :key="i" class="res__item">
      <!-- Image blocks arrive as bare base64 with the media type in a separate
           field; toolImageSrc builds the data: URI the old explorer omitted. -->
      <img
        v-if="item.type === 'image'"
        :src="toolImageSrc(item)"
        :alt="`Tool result image ${i + 1}`"
        class="res__img"
      />
      <pre v-else class="res__pre">{{ present(item.text ?? '') }}</pre>
    </div>
  </div>
</template>

<style scoped>
.res {
  border-radius: var(--radius-card);
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
  overflow: hidden;
}

.res__bar {
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.res__item + .res__item {
  border-top: 1px solid var(--color-border);
}

.res__img {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 auto;
  background: var(--color-surface-2);
}

.res__pre {
  margin: 0;
  padding: 0.75rem;
  overflow-x: auto;
  font-family: var(--font-mono);
  font-size: var(--text-body-sm);
  line-height: 1.6;
  color: var(--color-fg);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>

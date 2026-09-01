<script setup lang="ts">
import type { ToolDefinition, ToolResult } from '~/composables/useApi';
import { TOOL_CATEGORIES, categoryFor } from '~/utils/toolCategories';

const route = useRoute();
const api = useApi();
const toast = useToast();
const deviceId = route.params.id as string;

useHead({ title: 'Tool explorer' });

const tools = ref<ToolDefinition[]>([]);
const selected = ref<ToolDefinition | null>(null);
const args = ref<Record<string, unknown>>({});
const result = ref<ToolResult | null>(null);
const running = ref(false);
const search = ref('');
const expanded = ref<Record<string, boolean>>({});

onMounted(async () => {
  tools.value = await api.getTools();
  // Open the first non-empty category by default, as the phone does.
  const first = grouped.value.find((g) => g.tools.length);
  if (first) expanded.value[first.key] = true;
});

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return tools.value;
  return tools.value.filter(
    (t) => t.name.toLowerCase().includes(q) || t.description?.toLowerCase().includes(q),
  );
});

const grouped = computed(() =>
  TOOL_CATEGORIES.map((cat) => ({
    ...cat,
    tools: filtered.value.filter((t) => categoryFor(t.name) === cat.key),
  })).filter((g) => g.tools.length > 0),
);

/** Visible params, minus deviceId which the server injects server-side. */
const params = computed(() => {
  const schema = selected.value?.inputSchema;
  if (!schema?.properties) return [];
  return Object.entries(schema.properties)
    .filter(([name]) => name !== 'deviceId')
    .map(([name, spec]) => ({
      name,
      spec: spec as Record<string, any>,
      required: schema.required?.includes(name) ?? false,
    }));
});

function selectTool(tool: ToolDefinition) {
  selected.value = tool;
  result.value = null;
  args.value = {};
  // Seed defaults so the form reflects what the server will actually receive.
  for (const p of params.value) {
    if (p.spec.default !== undefined) args.value[p.name] = p.spec.default;
    else if (p.spec.type === 'boolean') args.value[p.name] = false;
    else if (p.spec.type === 'array') args.value[p.name] = '';
  }
}

function coerce(name: string, spec: Record<string, any>, raw: unknown): unknown {
  if (raw === '' || raw === undefined || raw === null) return undefined;
  if (spec.type === 'number' || spec.type === 'integer') return Number(raw);
  if (spec.type === 'boolean') return Boolean(raw);
  if (spec.type === 'array') {
    const parts = String(raw)
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    return spec.items?.type === 'number' ? parts.map(Number) : parts;
  }
  if (spec.type === 'object') {
    try {
      return JSON.parse(String(raw));
    } catch {
      throw new Error(`${name} must be valid JSON`);
    }
  }
  return raw;
}

async function execute() {
  if (!selected.value) return;
  running.value = true;
  result.value = null;
  try {
    const payload: Record<string, unknown> = {};
    for (const p of params.value) {
      const v = coerce(p.name, p.spec, args.value[p.name]);
      if (v !== undefined) payload[p.name] = v;
    }
    result.value = await api.executeTool(deviceId, selected.value.name, payload);
  } catch (e) {
    toast.error('Execution failed', e instanceof Error ? e.message : String(e));
  } finally {
    running.value = false;
  }
}

const VIBRATE_PRESETS = [
  { label: 'Short', value: '0,200' },
  { label: 'Double', value: '0,100,100,100' },
  { label: 'Long', value: '0,800' },
  { label: 'SOS', value: '0,150,100,150,100,150,300,400,100,400,100,400,300,150,100,150,100,150' },
];
</script>

<template>
  <div>
    <PageHeader
      title="Tool explorer"
      :description="`Run any of the ${tools.length} MCP tools directly against this device.`"
      :back-to="`/devices/${deviceId}`"
      back-label="Device"
    />

    <div class="layout">
      <ACard variant="flush" class="sidebar">
        <label class="search">
          <Icon name="ph:magnifying-glass" />
          <input v-model="search" type="search" placeholder="Search tools" />
        </label>

        <div class="cats">
          <div v-for="cat in grouped" :key="cat.key" class="cat">
            <button
              type="button"
              class="cat__head"
              :aria-expanded="!!expanded[cat.key]"
              @click="expanded[cat.key] = !expanded[cat.key]"
            >
              <AIconTile :icon="cat.icon" :accent="cat.accent" :size="28" />
              <span class="cat__name">{{ cat.label }}</span>
              <span class="cat__count">{{ cat.tools.length }}</span>
              <Icon :name="expanded[cat.key] ? 'ph:caret-up' : 'ph:caret-down'" />
            </button>

            <div v-if="expanded[cat.key] || search" class="cat__tools">
              <button
                v-for="tool in cat.tools"
                :key="tool.name"
                type="button"
                class="tool"
                :class="{ 'tool--active': selected?.name === tool.name }"
                @click="selectTool(tool)"
              >
                <span class="tool__dot" :style="{ background: cat.accent }" />
                <span class="tool__name mono">{{ tool.name.replace('aster_', '') }}</span>
              </button>
            </div>
          </div>
        </div>
      </ACard>

      <div class="main">
        <AEmptyState
          v-if="!selected"
          icon="ph:cursor-click"
          title="Pick a tool"
          description="Choose a tool from the catalogue to build and run a call."
        />

        <template v-else>
          <ACard>
            <h2 class="tool__title mono">{{ selected.name }}</h2>
            <p class="tool__desc">{{ selected.description }}</p>

            <div v-if="params.length" class="params">
              <div v-for="p in params" :key="p.name" class="param">
                <label class="param__label" :for="`p-${p.name}`">
                  {{ p.name }}
                  <span v-if="p.required" class="param__req">required</span>
                  <span class="param__type">{{ p.spec.type }}</span>
                </label>
                <p v-if="p.spec.description" class="param__desc">{{ p.spec.description }}</p>

                <select
                  v-if="p.spec.enum"
                  :id="`p-${p.name}`"
                  v-model="args[p.name]"
                  class="control"
                >
                  <option value="">—</option>
                  <option v-for="opt in p.spec.enum" :key="opt" :value="opt">{{ opt }}</option>
                </select>

                <AToggle
                  v-else-if="p.spec.type === 'boolean'"
                  :id="`p-${p.name}`"
                  v-model="args[p.name] as boolean"
                />

                <input
                  v-else-if="p.spec.type === 'number' || p.spec.type === 'integer'"
                  :id="`p-${p.name}`"
                  v-model="args[p.name]"
                  type="number"
                  class="control"
                  :min="p.spec.minimum"
                  :max="p.spec.maximum"
                />

                <textarea
                  v-else-if="p.spec.type === 'object' || p.name === 'content' || p.name === 'html'"
                  :id="`p-${p.name}`"
                  v-model="args[p.name]"
                  class="control control--area"
                  rows="4"
                />

                <template v-else>
                  <input
                    :id="`p-${p.name}`"
                    v-model="args[p.name]"
                    type="text"
                    class="control"
                    :placeholder="p.spec.type === 'array' ? 'Comma-separated' : ''"
                  />
                  <div v-if="selected.name === 'aster_vibrate' && p.name === 'pattern'" class="presets">
                    <button
                      v-for="preset in VIBRATE_PRESETS"
                      :key="preset.label"
                      type="button"
                      class="preset"
                      @click="args[p.name] = preset.value"
                    >
                      {{ preset.label }}
                    </button>
                  </div>
                </template>
              </div>
            </div>
            <p v-else class="params__none">This tool takes no parameters.</p>

            <AButton
              variant="primary"
              icon="ph:play"
              :loading="running"
              class="run"
              @click="execute"
            >
              Execute
            </AButton>
          </ACard>

          <ToolResultView v-if="result" :result="result" class="result" />
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 1.25rem;
  align-items: start;
}

@media (max-width: 1000px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.sidebar {
  position: sticky;
  top: 5rem;
  max-height: calc(100vh - 7rem);
  display: flex;
  flex-direction: column;
}

.search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-fg-muted);
}

.search input {
  flex: 1;
  min-width: 0;
  border: none;
  background: none;
  color: var(--color-fg);
  font: inherit;
  font-size: var(--text-body-md);
  outline: none;
}

.cats {
  overflow-y: auto;
  padding: 0.375rem;
}

.cat__head {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.375rem 0.5rem;
  border: none;
  background: none;
  color: var(--color-fg);
  font: inherit;
  cursor: pointer;
  border-radius: var(--radius-sm);
}

.cat__head:hover {
  background: var(--color-surface-2);
}

.cat__name {
  flex: 1;
  text-align: left;
  font-size: var(--text-title-sm);
  font-weight: 600;
}

.cat__count {
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

.cat__tools {
  display: grid;
  gap: 1px;
  padding: 0.125rem 0 0.375rem 0.5rem;
}

.tool {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.3125rem 0.5rem;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  color: var(--color-fg-subtle);
  font: inherit;
  font-size: var(--text-body-sm);
  cursor: pointer;
  text-align: left;
}

.tool:hover {
  background: var(--color-surface-2);
  color: var(--color-fg);
}

.tool--active {
  background: color-mix(in oklab, var(--color-primary) 14%, transparent);
  color: var(--color-primary);
}

.tool__dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex: none;
  opacity: 0.6;
}

.tool__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main {
  display: grid;
  gap: 1rem;
  min-width: 0;
}

.tool__title {
  margin: 0;
  font-size: var(--text-title-lg);
  font-weight: 600;
  color: var(--color-fg);
}

.tool__desc {
  margin: 0.25rem 0 1.25rem;
  font-size: var(--text-body-md);
  color: var(--color-fg-subtle);
}

.params {
  display: grid;
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.param__label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-family: var(--font-mono);
  font-size: var(--text-body-sm);
  color: var(--color-fg);
  margin-bottom: 0.25rem;
}

.param__req {
  font-family: var(--font-display);
  font-size: var(--text-label-sm);
  color: var(--color-error);
}

.param__type {
  font-family: var(--font-display);
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

.param__desc {
  margin: 0 0 0.375rem;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
}

.control {
  width: 100%;
  min-height: 38px;
  padding: 0.4375rem 0.625rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  color: var(--color-fg);
  font: inherit;
  font-size: var(--text-body-md);
}

.control:focus {
  outline: none;
  border-color: var(--color-primary);
}

.control--area {
  font-family: var(--font-mono);
  resize: vertical;
}

.params__none {
  margin: 0 0 1.25rem;
  font-size: var(--text-label-md);
  color: var(--color-fg-muted);
}

.presets {
  display: flex;
  gap: 0.375rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
}

.preset {
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  color: var(--color-fg-subtle);
  border-radius: var(--radius-sm);
  padding: 0.1875rem 0.5rem;
  font: inherit;
  font-size: var(--text-label-sm);
  cursor: pointer;
}

.preset:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.run {
  width: 100%;
}
</style>

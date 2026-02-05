<script setup lang="ts">
import type { Device, ToolDefinition, ToolResult } from '~/composables/useApi';

const route = useRoute();
const api = useApi();

const device = ref<Device | null>(null);
const tools = ref<ToolDefinition[]>([]);
const selectedToolName = ref<string>('');
const toolArgs = ref<Record<string, any>>({});
const executing = ref(false);
const executionResult = ref<ToolResult | null>(null);
const commandHistory = ref<Array<{ tool: string; timestamp: Date; success: boolean }>>([]);
const toolSearch = ref('');
const loading = ref(true);

const deviceId = computed(() => route.params.id as string);

// Helper function to get category icon
const getCategoryIcon = (category: string): string => {
  const icons: Record<string, string> = {
    'Device': 'ph:device-mobile',
    'Files': 'ph:folder',
    'Display': 'ph:monitor',
    'Input': 'ph:cursor',
    'Media': 'ph:speaker-high',
    'System': 'ph:gear',
    'Other': 'ph:cube'
  };
  return icons[category] || 'ph:cube';
};

// Filtered tools based on search
const filteredTools = computed(() => {
  if (!toolSearch.value) return tools.value;
  const query = toolSearch.value.toLowerCase();
  return tools.value.filter(t =>
    t.name.toLowerCase().includes(query) ||
    t.description.toLowerCase().includes(query)
  );
});

// Tool categories
const toolCategories = computed(() => {
  const categories: Record<string, ToolDefinition[]> = {
    'Device': [],
    'Files': [],
    'Display': [],
    'Input': [],
    'Media': [],
    'System': [],
    'Other': []
  };

  for (const tool of filteredTools.value) {
    const name = tool.name.replace('aster_', '');
    if (name.includes('device') || name.includes('battery') || name.includes('location')) {
      categories['Device']?.push(tool);
    } else if (name.includes('file') || name.includes('package')) {
      categories['Files']?.push(tool);
    } else if (name.includes('screen') || name.includes('screenshot') || name.includes('overlay')) {
      categories['Display']?.push(tool);
    } else if (name.includes('input') || name.includes('gesture') || name.includes('global') || name.includes('text')) {
      categories['Input']?.push(tool);
    } else if (name.includes('speak') || name.includes('audio') || name.includes('vibrate')) {
      categories['Media']?.push(tool);
    } else if (name.includes('notification') || name.includes('sms') || name.includes('call') || name.includes('clipboard') || name.includes('toast') || name.includes('intent') || name.includes('shell')) {
      categories['System']?.push(tool);
    } else {
      categories['Other']?.push(tool);
    }
  }

  return Object.fromEntries(Object.entries(categories).filter(([, t]) => t.length > 0));
});

const selectedTool = computed(() => tools.value.find(t => t.name === selectedToolName.value));

const toolProperties = computed(() => {
  if (!selectedTool.value) return {};
  const props = { ...selectedTool.value.inputSchema.properties };
  delete props.deviceId;
  return props;
});

// Vibration preset patterns
const vibratePresets = [
  { name: 'Tap', pattern: [0, 50] },
  { name: 'Double Tap', pattern: [0, 50, 100, 50] },
  { name: 'Notification', pattern: [0, 200, 100, 200] },
  { name: 'Alert', pattern: [0, 100, 50, 100, 50, 100] },
  { name: 'Long Pulse', pattern: [0, 500] },
];

const isVibrateTool = computed(() => selectedTool.value?.name === 'aster_vibrate');

// Watch for tool changes to initialize args
watch(selectedToolName, () => {
  toolArgs.value = {};
  executionResult.value = null;
  if (selectedTool.value) {
    const props = toolProperties.value;
    for (const [key, schema] of Object.entries(props)) {
      if (schema.default !== undefined) {
        toolArgs.value[key] = schema.default;
      } else if (schema.type === 'boolean') {
        toolArgs.value[key] = false;
      } else if (schema.type === 'array') {
        toolArgs.value[key] = [];
      }
    }
  }
});

// Select vibrate preset
const selectVibratePreset = (pattern: number[]) => {
  toolArgs.value.pattern = pattern;
};

// Update custom vibrate pattern
const updateCustomVibratePattern = (value: string) => {
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed) && parsed.every(n => typeof n === 'number')) {
      toolArgs.value.pattern = parsed;
    }
  } catch (e) {
    // Invalid JSON, ignore
  }
};

// Execute command
const executeCommand = async () => {
  if (!selectedTool.value || !device.value) return;

  executing.value = true;
  executionResult.value = null;

  try {
    // Convert args to proper types based on schema
    const convertedArgs: Record<string, any> = { deviceId: device.value.id };
    const props = toolProperties.value;

    for (const [key, value] of Object.entries(toolArgs.value)) {
      const schema = props[key];
      if (!schema) continue;

      if (schema.type === 'number') {
        convertedArgs[key] = Number(value);
      } else if (schema.type === 'integer') {
        convertedArgs[key] = parseInt(String(value), 10);
      } else if (schema.type === 'boolean') {
        convertedArgs[key] = Boolean(value);
      } else if (schema.type === 'array') {
        convertedArgs[key] = Array.isArray(value) ? value : [];
      } else if (schema.type === 'object') {
        convertedArgs[key] = typeof value === 'object' ? value : {};
      } else {
        convertedArgs[key] = value;
      }
    }

    const result = await api.executeTool(device.value.id, selectedTool.value.name, convertedArgs);
    executionResult.value = result;

    // Add to command history
    commandHistory.value.unshift({
      tool: selectedTool.value.name,
      timestamp: new Date(),
      success: !result.isError,
    });

    // Keep only last 10 commands
    if (commandHistory.value.length > 10) {
      commandHistory.value = commandHistory.value.slice(0, 10);
    }
  } catch (e) {
    executionResult.value = {
      content: [{ type: 'text', text: `Error: ${e instanceof Error ? e.message : String(e)}` }],
      isError: true,
    };
  } finally {
    executing.value = false;
  }
};

onMounted(async () => {
  try {
    const [deviceData, toolsData] = await Promise.all([
      api.getDevice(deviceId.value),
      api.getTools(),
    ]);
    device.value = deviceData;
    tools.value = toolsData;
  } catch (e) {
    console.error('Failed to load control page data:', e);
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="control-page">
    <div class="grid-bg"></div>

    <div class="relative z-10 h-screen flex flex-col">
      <!-- Header -->
      <header class="control-header">
        <div class="max-w-[1800px] mx-auto px-4 py-3 flex items-center justify-between">
          <div class="flex items-center gap-4">
            <NuxtLink :to="`/devices/${deviceId}`" class="back-link">
              <Icon name="ph:arrow-left" size="18" />
              <span>Back to Device</span>
            </NuxtLink>
            <div class="header-divider"></div>
            <h1 class="control-title">Device Control</h1>
            <span v-if="device" class="device-id-badge">{{ device.id.slice(0, 8) }}</span>
          </div>
          <div v-if="device" class="connection-status" :class="device.online ? 'is-online' : 'is-offline'">
            <div class="status-dot"></div>
            <span>{{ device.online ? 'ONLINE' : 'OFFLINE' }}</span>
          </div>
        </div>
      </header>

      <!-- Loading State -->
      <div v-if="loading" class="flex-1 flex items-center justify-center">
        <div class="loading-spinner"></div>
      </div>

      <!-- Main Content -->
      <div v-else class="flex-1 flex overflow-hidden">
        <!-- Sidebar -->
        <aside class="control-sidebar">
          <div class="sidebar-search">
            <Icon name="ph:magnifying-glass" size="18" />
            <input
              v-model="toolSearch"
              type="text"
              placeholder="Search tools..."
              class="search-input"
            />
          </div>

          <div class="sidebar-content">
            <div v-for="(categoryTools, category) in toolCategories" :key="category" class="tool-category">
              <div class="category-header">
                <Icon :name="getCategoryIcon(category)" size="16" />
                <span>{{ category }}</span>
                <span class="category-count">{{ categoryTools.length }}</span>
              </div>
              <div class="category-tools">
                <button
                  v-for="tool in categoryTools"
                  :key="tool.name"
                  @click="selectedToolName = tool.name"
                  :class="['tool-item', { 'tool-active': selectedToolName === tool.name }]"
                >
                  {{ tool.name.replace('aster_', '') }}
                </button>
              </div>
            </div>

            <div v-if="commandHistory.length > 0" class="recent-commands">
              <div class="recent-header">Recent</div>
              <button
                v-for="(cmd, idx) in commandHistory.slice(0, 5)"
                :key="idx"
                @click="selectedToolName = cmd.tool"
                class="recent-item"
              >
                <Icon :name="cmd.success ? 'ph:check-circle' : 'ph:x-circle'" size="14" />
                <span>{{ cmd.tool.replace('aster_', '') }}</span>
              </button>
            </div>
          </div>
        </aside>

        <!-- Main Panel -->
        <main class="control-main">
          <div v-if="!selectedTool" class="empty-state">
            <Icon name="ph:hand-pointing" size="48" />
            <h3>Select a tool from the sidebar</h3>
            <p>{{ tools.length }} tools available</p>
          </div>

          <div v-else class="tool-panel">
            <div class="tool-header">
              <h2>{{ selectedTool.name.replace('aster_', '') }}</h2>
              <p>{{ selectedTool.description }}</p>
            </div>

            <div class="tool-form">
              <!-- Special case: Vibration presets -->
              <div v-if="isVibrateTool" class="form-group">
                <label class="form-label">Presets</label>
                <div class="preset-grid">
                  <button
                    v-for="preset in vibratePresets"
                    :key="preset.name"
                    @click="selectVibratePreset(preset.pattern)"
                    :class="['preset-btn', { 'preset-active': JSON.stringify(toolArgs.pattern) === JSON.stringify(preset.pattern) }]"
                  >
                    {{ preset.name }}
                  </button>
                </div>
              </div>

              <!-- Dynamic form fields -->
              <div v-for="(schema, key) in toolProperties" :key="key" class="form-group">
                <label class="form-label">
                  {{ key }}
                  <span v-if="selectedTool.inputSchema.required?.includes(key)" class="text-red-400">*</span>
                </label>
                <p v-if="schema.description" class="form-description">{{ schema.description }}</p>

                <!-- String input -->
                <input
                  v-if="schema.type === 'string' && !schema.enum"
                  v-model="toolArgs[key]"
                  type="text"
                  class="form-input"
                  :placeholder="schema.default"
                />

                <!-- Enum select -->
                <select
                  v-else-if="schema.enum"
                  v-model="toolArgs[key]"
                  class="form-input"
                >
                  <option v-for="option in schema.enum" :key="option" :value="option">
                    {{ option }}
                  </option>
                </select>

                <!-- Number input -->
                <input
                  v-else-if="schema.type === 'number' || schema.type === 'integer'"
                  v-model.number="toolArgs[key]"
                  type="number"
                  class="form-input"
                  :placeholder="schema.default?.toString()"
                  :min="schema.minimum"
                  :max="schema.maximum"
                />

                <!-- Boolean toggle -->
                <label v-else-if="schema.type === 'boolean'" class="toggle-label">
                  <input
                    v-model="toolArgs[key]"
                    type="checkbox"
                    class="toggle-input"
                  />
                  <span class="toggle-switch"></span>
                  <span class="toggle-text">{{ toolArgs[key] ? 'Enabled' : 'Disabled' }}</span>
                </label>

                <!-- Array input (JSON) -->
                <textarea
                  v-else-if="schema.type === 'array'"
                  v-model="toolArgs[key]"
                  @input="(e: Event) => { if (isVibrateTool && key === 'pattern') updateCustomVibratePattern((e.target as HTMLTextAreaElement).value) }"
                  class="form-input"
                  rows="3"
                  :placeholder="schema.default ? JSON.stringify(schema.default, null, 2) : '[...]'"
                ></textarea>

                <!-- Object input (JSON) -->
                <textarea
                  v-else-if="schema.type === 'object'"
                  v-model="toolArgs[key]"
                  class="form-input"
                  rows="4"
                  :placeholder="schema.default ? JSON.stringify(schema.default, null, 2) : '{...}'"
                ></textarea>
              </div>

              <!-- Execute button -->
              <button
                @click="executeCommand"
                :disabled="!device?.online || executing"
                class="execute-btn"
              >
                <Icon v-if="executing" name="svg-spinners:90-ring-with-bg" size="18" />
                <Icon v-else name="ph:play-fill" size="18" />
                <span>{{ executing ? 'Executing...' : 'Execute' }}</span>
              </button>
            </div>

            <div v-if="executionResult" class="execution-results">
              <div class="results-header">
                <h3>Results</h3>
                <span :class="['result-badge', executionResult.isError ? 'badge-error' : 'badge-success']">
                  {{ executionResult.isError ? 'ERROR' : 'SUCCESS' }}
                </span>
              </div>

              <div class="results-content">
                <div v-for="(item, idx) in executionResult.content" :key="idx">
                  <!-- Text content -->
                  <pre v-if="item.type === 'text'" class="result-text">{{ item.text }}</pre>

                  <!-- Image content -->
                  <img v-else-if="item.type === 'image'" :src="item.data" class="result-image" alt="Result" />
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<style>
@reference "tailwindcss";

.control-page {
  @apply min-h-screen bg-[#0a0b0d] relative overflow-hidden;
}

.grid-bg {
  @apply absolute inset-0 opacity-20;
  background-image:
    linear-gradient(rgba(139, 92, 246, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(139, 92, 246, 0.1) 1px, transparent 1px);
  background-size: 30px 30px;
}

.control-header {
  @apply border-b border-gray-800 bg-black/40 backdrop-blur-sm;
}

.back-link {
  @apply flex items-center gap-2 px-3 py-1.5 rounded-md
    bg-gray-800/50 hover:bg-gray-700/50 border border-gray-700
    text-gray-300 text-sm transition-colors;
}

.header-divider {
  @apply w-px h-5 bg-gray-700;
}

.control-title {
  @apply text-lg font-semibold text-white;
}

.device-id-badge {
  @apply px-2 py-1 rounded text-xs font-mono bg-violet-500/20 text-violet-300 border border-violet-500/30;
}

.connection-status {
  @apply flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium;
}

.connection-status.is-online {
  @apply bg-emerald-500/20 text-emerald-300 border border-emerald-500/30;
}

.connection-status.is-offline {
  @apply bg-gray-700/50 text-gray-400 border border-gray-600;
}

.status-dot {
  @apply w-2 h-2 rounded-full;
}

.is-online .status-dot {
  @apply bg-emerald-400 animate-pulse;
}

.is-offline .status-dot {
  @apply bg-gray-500;
}

.control-sidebar {
  @apply w-80 border-r border-gray-800 bg-black/20 backdrop-blur-sm flex flex-col;
}

.sidebar-search {
  @apply flex items-center gap-2 px-4 py-3 border-b border-gray-800;
}

.search-input {
  @apply flex-1 bg-transparent text-white placeholder-gray-500 outline-none text-sm;
}

.sidebar-content {
  @apply flex-1 overflow-y-auto p-3 space-y-4;
}

.tool-category {
  @apply space-y-1;
}

.category-header {
  @apply flex items-center gap-2 px-2 py-1.5 text-xs font-semibold text-gray-400 uppercase;
}

.category-count {
  @apply ml-auto px-1.5 py-0.5 rounded text-[10px] bg-gray-800 text-gray-500;
}

.category-tools {
  @apply space-y-0.5;
}

.tool-item {
  @apply w-full text-left px-3 py-2 rounded-md text-sm text-gray-300
    hover:bg-gray-800/50 transition-colors;
}

.tool-active {
  @apply bg-violet-500/20 text-violet-300 border border-violet-500/30;
}

.recent-commands {
  @apply pt-3 border-t border-gray-800 space-y-1;
}

.recent-header {
  @apply px-2 py-1.5 text-xs font-semibold text-gray-400 uppercase;
}

.recent-item {
  @apply w-full flex items-center gap-2 px-3 py-2 rounded-md text-sm text-gray-400
    hover:bg-gray-800/50 transition-colors;
}

.control-main {
  @apply flex-1 overflow-y-auto p-6;
}

.empty-state {
  @apply flex flex-col items-center justify-center h-full text-center space-y-4;
}

.empty-state h3 {
  @apply text-xl font-semibold text-white;
}

.empty-state p {
  @apply text-gray-400;
}

.tool-panel {
  @apply max-w-4xl mx-auto space-y-6;
}

.tool-header {
  @apply space-y-2;
}

.tool-header h2 {
  @apply text-2xl font-bold text-white;
}

.tool-header p {
  @apply text-gray-400;
}

.tool-form {
  @apply p-6 rounded-lg border border-gray-800 bg-gray-900/50;
}

.execution-results {
  @apply p-6 rounded-lg border border-gray-800 bg-gray-900/50 space-y-4;
}

.execution-results h3 {
  @apply text-lg font-semibold text-white;
}

.loading-spinner {
  @apply w-8 h-8 border-2 border-violet-500 border-t-transparent rounded-full animate-spin;
}

/* Form Styles */
.form-group {
  @apply space-y-2;
}

.form-label {
  @apply block text-sm font-medium text-gray-300;
}

.form-description {
  @apply text-xs text-gray-500;
}

.form-input {
  @apply w-full px-3 py-2 rounded-md bg-gray-800/50 border border-gray-700
    text-white placeholder-gray-500
    focus:outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500
    transition-colors;
}

.form-input:disabled {
  @apply opacity-50 cursor-not-allowed;
}

/* Toggle Switch */
.toggle-label {
  @apply flex items-center gap-3 cursor-pointer;
}

.toggle-input {
  @apply sr-only;
}

.toggle-switch {
  @apply relative w-11 h-6 bg-gray-700 rounded-full transition-colors;
}

.toggle-switch::after {
  @apply content-[''] absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform;
}

.toggle-input:checked + .toggle-switch {
  @apply bg-violet-500;
}

.toggle-input:checked + .toggle-switch::after {
  @apply translate-x-5;
}

.toggle-text {
  @apply text-sm text-gray-400;
}

/* Preset Grid */
.preset-grid {
  @apply grid grid-cols-3 gap-2;
}

.preset-btn {
  @apply px-4 py-2 rounded-md text-sm font-medium
    bg-gray-800/50 border border-gray-700 text-gray-300
    hover:bg-gray-700/50 hover:border-violet-500/50
    transition-colors;
}

.preset-active {
  @apply bg-violet-500/20 border-violet-500 text-violet-300;
}

/* Execute Button */
.execute-btn {
  @apply w-full mt-4 px-4 py-3 rounded-md font-medium
    bg-violet-500 text-white
    hover:bg-violet-600
    disabled:opacity-50 disabled:cursor-not-allowed
    flex items-center justify-center gap-2
    transition-colors;
}

/* Results Display */
.results-header {
  @apply flex items-center justify-between;
}

.results-header h3 {
  @apply text-lg font-semibold text-white;
}

.result-badge {
  @apply px-2 py-1 rounded text-xs font-semibold uppercase;
}

.badge-success {
  @apply bg-emerald-500/20 text-emerald-300 border border-emerald-500/30;
}

.badge-error {
  @apply bg-red-500/20 text-red-300 border border-red-500/30;
}

.results-content {
  @apply space-y-3;
}

.result-text {
  @apply p-4 rounded-md bg-gray-800/50 border border-gray-700
    text-sm text-gray-300 font-mono overflow-x-auto;
}

.result-image {
  @apply rounded-md border border-gray-700 max-w-full h-auto;
}
</style>

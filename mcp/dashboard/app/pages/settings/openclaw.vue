<script setup lang="ts">
const api = useApi();

const loading = ref(true);
const saving = ref(false);
const testing = ref(false);
const testResult = ref<{ success: boolean; status?: number; error?: string } | null>(null);
const saveSuccess = ref(false);
const isEditing = ref(false);
const hasExistingConfig = ref(false);

// Form fields
const form = reactive({
  enabled: true,
  endpoint: 'http://localhost:18789',
  webhookPath: '/hooks/agent',
  token: '',
  channel: 'whatsapp',
  deliverTo: '',
  events: {
    notifications: true,
    sms: true,
    deviceConnected: true,
    deviceDisconnected: true,
    pairingRequired: true,
  },
});

const configuredAt = ref<string | null>(null);
const hasSourceToken = ref(false);
const sourceTokenPreview = ref<string | null>(null);

onMounted(async () => {
  await loadConfig();
});

async function loadConfig() {
  loading.value = true;
  try {
    const data = await api.getOpenClawConfig();
    hasSourceToken.value = data.hasSourceToken;
    sourceTokenPreview.value = data.sourceTokenPreview;

    if (data.config) {
      hasExistingConfig.value = true;
      form.enabled = data.config.enabled;
      form.endpoint = data.config.endpoint;
      form.webhookPath = data.config.webhookPath;
      form.channel = data.config.channel || 'whatsapp';
      form.deliverTo = data.config.deliverTo || '';
      form.events = data.config.events;
      configuredAt.value = data.config.configuredAt;
      // Token is masked from server — keep field empty unless editing
      form.token = '';
    } else {
      isEditing.value = true;
      // Pre-fill token from source if available
      if (data.hasSourceToken) {
        await prefillToken();
      }
    }
  } catch {
    // No config yet, start in edit mode
    isEditing.value = true;
  } finally {
    loading.value = false;
  }
}

async function prefillToken() {
  try {
    const data = await api.prefillOpenClawToken();
    if (data.token) {
      form.token = data.token;
    }
  } catch {
    // Ignore — source config may not exist
  }
}

async function handleTest() {
  testing.value = true;
  testResult.value = null;
  try {
    // Server falls back to saved token when empty
    testResult.value = await api.testOpenClawConnection(form.endpoint, form.webhookPath, form.token || undefined);
  } catch (err: any) {
    testResult.value = { success: false, error: err.message };
  } finally {
    testing.value = false;
  }
}

async function handleSave() {
  saving.value = true;
  saveSuccess.value = false;
  try {
    // Server preserves existing token when empty string is sent
    await api.saveOpenClawConfig({
      enabled: form.enabled,
      endpoint: form.endpoint,
      webhookPath: form.webhookPath,
      token: form.token,
      channel: form.channel,
      deliverTo: form.deliverTo,
      events: form.events,
    });
    saveSuccess.value = true;
    hasExistingConfig.value = true;
    isEditing.value = false;
    configuredAt.value = new Date().toISOString();

    // Clear token field after save
    form.token = '';

    setTimeout(() => { saveSuccess.value = false; }, 3000);
  } catch (err: any) {
    testResult.value = { success: false, error: `Save failed: ${err.message}` };
  } finally {
    saving.value = false;
  }
}

function startEditing() {
  isEditing.value = true;
  testResult.value = null;
  saveSuccess.value = false;
}

function cancelEditing() {
  isEditing.value = false;
  testResult.value = null;
  loadConfig();
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}
</script>

<template>
  <div class="min-h-screen p-6">
    <div class="max-w-[1400px] mx-auto space-y-6">
      <!-- Header -->
      <TerminalPageHeader
        description="OpenClaw // Event Forwarding Configuration"
        :loading="loading"
        :show-refresh="false"
      >
        <template #left>
          <NuxtLink to="/" class="btn-terminal text-[11px] px-3 py-2 inline-flex items-center gap-2">
            <Icon name="ph:arrow-left" size="14" />
            DASHBOARD
          </NuxtLink>
        </template>
        <template #actions>
          <div class="flex items-center gap-3">
            <span class="status-dot" :class="hasExistingConfig ? (form.enabled ? 'status-online' : 'status-offline') : 'status-error'"></span>
            <span class="text-[11px] uppercase tracking-wider" :class="hasExistingConfig ? (form.enabled ? 'text-emerald' : 'text-terminal-muted') : 'text-rose'">
              {{ hasExistingConfig ? (form.enabled ? 'Active' : 'Disabled') : 'Not Configured' }}
            </span>
          </div>
        </template>
      </TerminalPageHeader>

      <!-- Loading -->
      <div v-if="loading" class="py-12 text-center">
        <div class="text-primary animate-pulse text-[12px]">Loading configuration...</div>
      </div>

      <template v-else>
        <!-- Info Banner -->
        <div class="terminal-panel p-4">
          <div class="flex items-start gap-3">
            <span class="text-primary text-lg mt-0.5">&#9432;</span>
            <div class="text-[12px] text-terminal-muted leading-relaxed">
              Forward device events (notifications, SMS) to an OpenClaw webhook in real-time.
              When your phone receives a notification or SMS, Aster will POST it to OpenClaw
              so Claude can react proactively.
            </div>
          </div>
        </div>

        <!-- View Mode (existing config, not editing) -->
        <TerminalWindow v-if="hasExistingConfig && !isEditing" title="current configuration" class="animate-fade-in">
          <div class="space-y-5">
            <!-- Status Row -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <span class="status-dot" :class="form.enabled ? 'status-online' : 'status-offline'"></span>
                <span class="text-[12px] uppercase tracking-wider" :class="form.enabled ? 'text-emerald' : 'text-terminal-muted'">
                  {{ form.enabled ? 'Enabled' : 'Disabled' }}
                </span>
              </div>
              <button class="btn-terminal btn-terminal-primary text-[11px] px-4 py-2" @click="startEditing">
                EDIT CONFIG
              </button>
            </div>

            <!-- Config Details -->
            <div class="grid grid-cols-2 gap-4">
              <div class="config-field">
                <div class="config-label">Endpoint</div>
                <div class="config-value">{{ form.endpoint }}</div>
              </div>
              <div class="config-field">
                <div class="config-label">Webhook Path</div>
                <div class="config-value">{{ form.webhookPath }}</div>
              </div>
              <div class="config-field">
                <div class="config-label">Token</div>
                <div class="config-value text-terminal-dim">Configured</div>
              </div>
              <div class="config-field">
                <div class="config-label">Configured At</div>
                <div class="config-value">{{ configuredAt ? formatDate(configuredAt) : '-' }}</div>
              </div>
              <div class="config-field">
                <div class="config-label">Delivery Channel</div>
                <div class="config-value">{{ form.channel || '-' }}</div>
              </div>
              <div class="config-field">
                <div class="config-label">Deliver To</div>
                <div class="config-value">{{ form.deliverTo || 'Not set' }}</div>
              </div>
            </div>

            <!-- Events -->
            <div>
              <div class="config-label mb-3">Event Types</div>
              <div class="flex flex-wrap gap-3">
                <span class="badge" :class="form.events.notifications ? 'badge-emerald' : 'badge-muted'">
                  Notifications {{ form.events.notifications ? 'ON' : 'OFF' }}
                </span>
                <span class="badge" :class="form.events.sms ? 'badge-emerald' : 'badge-muted'">
                  SMS {{ form.events.sms ? 'ON' : 'OFF' }}
                </span>
                <span class="badge" :class="form.events.deviceConnected ? 'badge-emerald' : 'badge-muted'">
                  Device Connected {{ form.events.deviceConnected ? 'ON' : 'OFF' }}
                </span>
                <span class="badge" :class="form.events.deviceDisconnected ? 'badge-emerald' : 'badge-muted'">
                  Device Disconnected {{ form.events.deviceDisconnected ? 'ON' : 'OFF' }}
                </span>
                <span class="badge" :class="form.events.pairingRequired ? 'badge-emerald' : 'badge-muted'">
                  Pairing Required {{ form.events.pairingRequired ? 'ON' : 'OFF' }}
                </span>
              </div>
            </div>
          </div>
        </TerminalWindow>

        <!-- Edit/Create Mode -->
        <TerminalWindow v-if="isEditing" title="configure openclaw" class="animate-fade-in">
          <div class="space-y-5">
            <!-- Token Pre-fill Notice -->
            <div v-if="hasSourceToken && !form.token && !hasExistingConfig" class="flex items-center gap-3 p-3 rounded-sm" style="background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.2);">
              <span class="text-emerald text-sm">&#10003;</span>
              <span class="text-[11px] text-emerald">OpenClaw token detected from ~/.openclaw/openclaw.json</span>
              <button class="btn-terminal btn-terminal-success btn-terminal-sm ml-auto" @click="prefillToken">
                USE TOKEN
              </button>
            </div>

            <!-- Enabled Toggle -->
            <div class="flex items-center justify-between">
              <div>
                <div class="text-[12px] text-terminal-text font-semibold">Enable Event Forwarding</div>
                <div class="text-[11px] text-terminal-dim mt-1">Forward notifications and SMS to OpenClaw</div>
              </div>
              <button
                class="toggle-btn"
                :class="{ 'toggle-on': form.enabled }"
                @click="form.enabled = !form.enabled"
              >
                <span class="toggle-knob"></span>
              </button>
            </div>

            <div class="border-t border-terminal-border"></div>

            <!-- Endpoint -->
            <div class="form-group">
              <label class="form-label">Endpoint URL</label>
              <input
                v-model="form.endpoint"
                type="text"
                class="input-terminal"
                placeholder="http://localhost:18789"
              />
            </div>

            <!-- Webhook Path -->
            <div class="form-group">
              <label class="form-label">Webhook Path</label>
              <input
                v-model="form.webhookPath"
                type="text"
                class="input-terminal"
                placeholder="/hooks/agent"
              />
            </div>

            <!-- Token -->
            <div class="form-group">
              <label class="form-label">
                Auth Token
                <span v-if="hasExistingConfig" class="text-terminal-dim font-normal ml-2">(leave empty to keep current)</span>
              </label>
              <input
                v-model="form.token"
                type="password"
                class="input-terminal"
                :placeholder="hasExistingConfig ? 'Leave empty to keep existing token' : 'Paste your OpenClaw token'"
              />
              <div v-if="hasSourceToken && !form.token" class="mt-2">
                <button class="text-[11px] text-primary hover:text-primary-bright transition-colors" @click="prefillToken">
                  Auto-fill from ~/.openclaw/openclaw.json
                </button>
              </div>
            </div>

            <div class="border-t border-terminal-border"></div>

            <!-- Delivery Settings -->
            <div class="form-group">
              <label class="form-label">Delivery Channel</label>
              <select v-model="form.channel" class="input-terminal">
                <option value="whatsapp">WhatsApp</option>
                <option value="telegram">Telegram</option>
              </select>
              <div class="text-[10px] text-terminal-dim mt-1">Channel used by OpenClaw to deliver event alerts to you</div>
            </div>

            <div class="form-group">
              <label class="form-label">Deliver To</label>
              <input
                v-model="form.deliverTo"
                type="text"
                class="input-terminal"
                placeholder="+919876543210"
              />
              <div class="text-[10px] text-terminal-dim mt-1">Your phone number with country code (e.g. +91 for India, +1 for US) or email/username for the selected channel</div>
            </div>

            <div class="border-t border-terminal-border"></div>

            <!-- Event Types -->
            <div>
              <div class="form-label mb-3">Event Types</div>
              <div class="space-y-3">
                <label class="flex items-center gap-3 cursor-pointer group">
                  <input
                    v-model="form.events.notifications"
                    type="checkbox"
                    class="checkbox-terminal"
                  />
                  <div>
                    <div class="text-[12px] text-terminal-text group-hover:text-primary-bright transition-colors">Notifications</div>
                    <div class="text-[10px] text-terminal-dim">Forward app notifications to OpenClaw</div>
                  </div>
                </label>
                <label class="flex items-center gap-3 cursor-pointer group">
                  <input
                    v-model="form.events.sms"
                    type="checkbox"
                    class="checkbox-terminal"
                  />
                  <div>
                    <div class="text-[12px] text-terminal-text group-hover:text-primary-bright transition-colors">SMS</div>
                    <div class="text-[10px] text-terminal-dim">Forward incoming SMS messages to OpenClaw</div>
                  </div>
                </label>
                <label class="flex items-center gap-3 cursor-pointer group">
                  <input
                    v-model="form.events.deviceConnected"
                    type="checkbox"
                    class="checkbox-terminal"
                  />
                  <div>
                    <div class="text-[12px] text-terminal-text group-hover:text-primary-bright transition-colors">Device Connected</div>
                    <div class="text-[10px] text-terminal-dim">Notify when an approved device comes online</div>
                  </div>
                </label>
                <label class="flex items-center gap-3 cursor-pointer group">
                  <input
                    v-model="form.events.deviceDisconnected"
                    type="checkbox"
                    class="checkbox-terminal"
                  />
                  <div>
                    <div class="text-[12px] text-terminal-text group-hover:text-primary-bright transition-colors">Device Disconnected</div>
                    <div class="text-[10px] text-terminal-dim">Notify when a device goes offline</div>
                  </div>
                </label>
                <label class="flex items-center gap-3 cursor-pointer group">
                  <input
                    v-model="form.events.pairingRequired"
                    type="checkbox"
                    class="checkbox-terminal"
                  />
                  <div>
                    <div class="text-[12px] text-terminal-text group-hover:text-primary-bright transition-colors">Pairing Approval Required</div>
                    <div class="text-[10px] text-terminal-dim">Notify when a new device needs approval</div>
                  </div>
                </label>
              </div>
            </div>

            <div class="border-t border-terminal-border"></div>

            <!-- Test Result -->
            <div v-if="testResult" class="p-3 rounded-sm" :class="testResult.success ? 'test-success' : 'test-error'">
              <div class="flex items-center gap-2">
                <span class="text-sm">{{ testResult.success ? '&#10003;' : '&#10007;' }}</span>
                <span class="text-[12px] font-semibold">
                  {{ testResult.success ? `Connected (HTTP ${testResult.status})` : 'Connection Failed' }}
                </span>
                <span v-if="!testResult.success && testResult.status" class="text-[10px] opacity-60 ml-1">
                  HTTP {{ testResult.status }}
                </span>
              </div>
              <div v-if="testResult.error" class="text-[11px] mt-2 opacity-80 leading-relaxed">{{ testResult.error }}</div>
            </div>

            <!-- Save Success -->
            <div v-if="saveSuccess" class="p-3 rounded-sm test-success">
              <div class="flex items-center gap-2">
                <span class="text-sm">&#10003;</span>
                <span class="text-[12px] font-semibold">Configuration saved and applied</span>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center gap-3">
              <button
                class="btn-terminal btn-terminal-primary px-5 py-2.5 text-[11px]"
                :disabled="saving || !form.endpoint"
                @click="handleSave"
              >
                {{ saving ? 'SAVING...' : (hasExistingConfig ? 'UPDATE CONFIG' : 'SAVE CONFIG') }}
              </button>
              <button
                class="btn-terminal px-5 py-2.5 text-[11px]"
                :disabled="testing || !form.endpoint"
                @click="handleTest"
              >
                {{ testing ? 'TESTING...' : 'TEST CONNECTION' }}
              </button>
              <button
                v-if="hasExistingConfig"
                class="btn-terminal btn-terminal-ghost px-4 py-2.5 text-[11px]"
                @click="cancelEditing"
              >
                CANCEL
              </button>
            </div>
          </div>
        </TerminalWindow>

        <!-- How it works -->
        <TerminalWindow title="how it works" class="animate-slide-up stagger-2">
          <div class="space-y-4 text-[12px] text-terminal-muted leading-relaxed">
            <div class="flex items-start gap-3">
              <span class="text-primary font-bold shrink-0">1.</span>
              <span>Your phone receives a notification or SMS</span>
            </div>
            <div class="flex items-start gap-3">
              <span class="text-primary font-bold shrink-0">2.</span>
              <span>The Aster Android service forwards the event via WebSocket</span>
            </div>
            <div class="flex items-start gap-3">
              <span class="text-primary font-bold shrink-0">3.</span>
              <span>The MCP server receives the event and HTTP POSTs it to OpenClaw</span>
            </div>
            <div class="flex items-start gap-3">
              <span class="text-primary font-bold shrink-0">4.</span>
              <span>OpenClaw wakes Claude, which can react to the event in real-time</span>
            </div>
          </div>
        </TerminalWindow>
      </template>
    </div>
  </div>
</template>

<style scoped>
.config-field {
  padding: 12px 14px;
  background: rgba(148, 163, 184, 0.03);
  border: 1px solid var(--color-terminal-border);
  border-radius: 2px;
}

.config-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-terminal-dim);
}

.config-label::before {
  content: '// ';
  opacity: 0.5;
}

.config-value {
  font-size: 13px;
  color: var(--color-terminal-text);
  margin-top: 4px;
  word-break: break-all;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-terminal-muted);
}

/* Toggle */
.toggle-btn {
  position: relative;
  width: 44px;
  height: 24px;
  background: var(--color-terminal-surface-elevated);
  border: 1px solid var(--color-terminal-border-bright);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0;
}

.toggle-btn.toggle-on {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.4);
}

.toggle-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  background: var(--color-terminal-dim);
  border-radius: 50%;
  transition: all 0.2s ease;
}

.toggle-on .toggle-knob {
  left: 23px;
  background: var(--color-emerald);
  box-shadow: 0 0 6px var(--color-emerald);
}

/* Checkbox */
.checkbox-terminal {
  appearance: none;
  width: 16px;
  height: 16px;
  border: 1px solid var(--color-terminal-border-bright);
  border-radius: 2px;
  background: var(--color-terminal-surface);
  cursor: pointer;
  position: relative;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.checkbox-terminal:checked {
  background: rgba(34, 211, 238, 0.15);
  border-color: rgba(34, 211, 238, 0.4);
}

.checkbox-terminal:checked::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 5px;
  width: 4px;
  height: 8px;
  border: solid var(--color-primary);
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

/* Test result */
.test-success {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: var(--color-emerald);
}

.test-error {
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: var(--color-error);
}
</style>

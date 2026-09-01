<script setup lang="ts">
import type { AgentEventForwardingConfig } from '~/composables/useApi';

useHead({ title: 'Event forwarding' });

const api = useApi();
const toast = useToast();

const loaded = ref(false);
const saving = ref(false);
const testing = ref(false);
const editing = ref(false);
const hasSourceToken = ref(false);
const savedConfig = ref<AgentEventForwardingConfig | null>(null);
const testResult = ref<{ success: boolean; status?: number; error?: string } | null>(null);

const form = reactive({
  enabled: false,
  channelType: 'openclaw' as 'openclaw' | 'mattermost',
  endpoint: '',
  webhookPath: '',
  token: '',
  channel: '',
  deliverTo: '',
  events: {
    notifications: true,
    sms: true,
    deviceConnected: true,
    deviceDisconnected: true,
    pairingRequired: true,
    incomingCalls: true,
  },
});

const EVENTS = [
  { key: 'notifications', label: 'Notifications', desc: 'App notifications from the shade', icon: 'ph:bell' },
  { key: 'sms', label: 'SMS', desc: 'Incoming text messages', icon: 'ph:chat-circle' },
  { key: 'incomingCalls', label: 'Incoming calls', desc: 'Caller number and contact name', icon: 'ph:phone-incoming' },
  { key: 'deviceConnected', label: 'Device connected', desc: 'A device comes online', icon: 'ph:plugs-connected' },
  { key: 'deviceDisconnected', label: 'Device disconnected', desc: 'A device drops off', icon: 'ph:plugs' },
  { key: 'pairingRequired', label: 'Pairing required', desc: 'A new device needs approval', icon: 'ph:key' },
] as const;

async function load() {
  const res = await api.getAgentEventForwardingConfig();
  savedConfig.value = res.config;
  hasSourceToken.value = res.hasSourceToken;
  if (res.config) {
    Object.assign(form, {
      enabled: res.config.enabled,
      channelType: res.config.channelType ?? 'openclaw',
      endpoint: res.config.endpoint,
      webhookPath: res.config.webhookPath,
      token: '',
      channel: res.config.channel,
      deliverTo: res.config.deliverTo,
      events: { ...form.events, ...res.config.events },
    });
  } else {
    editing.value = true;
  }
  loaded.value = true;
}

onMounted(load);

async function prefill() {
  const { token } = await api.prefillAgentEventForwardingToken();
  if (token) {
    form.token = token;
    toast.success('Token imported from ~/.openclaw/openclaw.json');
  } else {
    toast.info('No local OpenClaw token found');
  }
}

async function test() {
  testing.value = true;
  testResult.value = null;
  try {
    testResult.value = await api.testAgentEventForwardingConnection(
      form.endpoint,
      form.webhookPath,
      form.token,
      form.channelType,
      form.channel,
    );
    if (testResult.value.success) toast.success('Connection succeeded');
    else toast.error('Connection failed', testResult.value.error);
  } finally {
    testing.value = false;
  }
}

async function save() {
  saving.value = true;
  try {
    await api.saveAgentEventForwardingConfig({ ...form });
    toast.success('Configuration saved');
    editing.value = false;
    await load();
  } catch (e) {
    toast.error('Could not save', e instanceof Error ? e.message : String(e));
  } finally {
    saving.value = false;
  }
}

const enabledEvents = computed(() =>
  EVENTS.filter((e) => savedConfig.value?.events?.[e.key as keyof typeof form.events]),
);
</script>

<template>
  <div>
    <PageHeader
      title="Event forwarding"
      description="Push device events to your agent as they happen."
    >
      <template #actions>
        <AStatusPill
          v-if="savedConfig"
          :status="savedConfig.enabled ? 'running' : 'offline'"
          :label="savedConfig.enabled ? 'Enabled' : 'Disabled'"
        />
        <AButton v-if="savedConfig && !editing" size="sm" icon="ph:pencil" @click="editing = true">
          Edit
        </AButton>
      </template>
    </PageHeader>

    <div v-if="!loaded" class="state"><ASpinner /> Loading…</div>

    <div v-else class="layout">
      <div class="main">
        <!-- Saved view -->
        <ACard v-if="savedConfig && !editing">
          <ASectionLabel label="Current configuration" />
          <InfoGrid
            :items="[
              { label: 'Channel', value: savedConfig.channelType ?? 'openclaw' },
              { label: 'Endpoint', value: savedConfig.endpoint, mono: true },
              { label: 'Webhook path', value: savedConfig.webhookPath, mono: true },
              { label: 'Token', value: savedConfig.hasToken ? 'Configured' : 'Not set' },
              { label: 'Channel name', value: savedConfig.channel },
              { label: 'Deliver to', value: savedConfig.deliverTo },
              { label: 'Configured', value: savedConfig.configuredAt },
            ]"
          />

          <ASectionLabel label="Forwarded events" :count="enabledEvents.length" class="mt" />
          <div class="chips">
            <ABadge v-for="e in enabledEvents" :key="e.key" tone="primary">{{ e.label }}</ABadge>
            <p v-if="enabledEvents.length === 0" class="muted">No event types enabled.</p>
          </div>
        </ACard>

        <!-- Edit form -->
        <ACard v-else>
          <ASectionLabel label="Delivery" />
          <div class="form">
            <div class="field">
              <label class="lbl" for="channelType">Channel type</label>
              <select id="channelType" v-model="form.channelType" class="control">
                <option value="openclaw">OpenClaw (Bearer auth)</option>
                <option value="mattermost">Mattermost incoming webhook</option>
              </select>
            </div>

            <div class="field">
              <label class="lbl" for="endpoint">Endpoint</label>
              <input id="endpoint" v-model="form.endpoint" class="control" placeholder="http://localhost:18789" />
            </div>

            <div class="field">
              <label class="lbl" for="webhookPath">Webhook path</label>
              <input id="webhookPath" v-model="form.webhookPath" class="control" placeholder="/hooks/agent" />
            </div>

            <div v-if="form.channelType === 'openclaw'" class="field">
              <label class="lbl" for="token">Token</label>
              <div class="row">
                <input
                  id="token"
                  v-model="form.token"
                  type="password"
                  class="control"
                  :placeholder="savedConfig?.hasToken ? 'Leave blank to keep current token' : 'Bearer token'"
                />
                <AButton v-if="hasSourceToken" size="sm" icon="ph:download-simple" @click="prefill">
                  Import local
                </AButton>
              </div>
            </div>

            <div class="field">
              <label class="lbl" for="channel">Channel</label>
              <input id="channel" v-model="form.channel" class="control" placeholder="whatsapp, telegram, …" />
            </div>

            <div class="field">
              <label class="lbl" for="deliverTo">Deliver to</label>
              <input id="deliverTo" v-model="form.deliverTo" class="control" placeholder="Optional recipient" />
            </div>
          </div>

          <ASectionLabel label="Events" class="mt" />
          <div class="events">
            <label v-for="e in EVENTS" :key="e.key" class="event">
              <AIconTile :icon="e.icon" :size="32" accent="var(--color-primary)" />
              <div class="event__body">
                <p class="event__label">{{ e.label }}</p>
                <p class="event__desc">{{ e.desc }}</p>
              </div>
              <AToggle v-model="form.events[e.key as keyof typeof form.events]" />
            </label>
          </div>

          <div class="foot">
            <AToggle v-model="form.enabled" label="Forwarding enabled" />
            <div class="foot__btns">
              <AButton
                v-if="savedConfig"
                size="sm"
                @click="editing = false"
              >
                Cancel
              </AButton>
              <AButton size="sm" icon="ph:plugs-connected" :loading="testing" @click="test">
                Test connection
              </AButton>
              <AButton variant="primary" size="sm" icon="ph:check" :loading="saving" @click="save">
                Save
              </AButton>
            </div>
          </div>

          <div v-if="testResult" class="result" :class="testResult.success ? 'result--ok' : 'result--bad'">
            <Icon :name="testResult.success ? 'ph:check-circle' : 'ph:x-circle'" />
            <span>
              {{ testResult.success ? `Delivered (HTTP ${testResult.status})` : testResult.error }}
            </span>
          </div>
        </ACard>
      </div>

      <ACard class="aside">
        <ASectionLabel label="How it works" />
        <ol class="steps">
          <li>The device reports an event over its WebSocket connection.</li>
          <li>The server matches it against the event types enabled here.</li>
          <li>It POSTs a formatted message to your endpoint plus webhook path.</li>
          <li>Your agent wakes and handles it.</li>
        </ol>
        <p class="muted">
          Events are also written to the log, so anything forwarded is visible under
          <NuxtLink to="/logs" class="link">Logs</NuxtLink>.
        </p>
      </ACard>
    </div>
  </div>
</template>

<style scoped>
.state { display: flex; align-items: center; gap: 0.5rem; justify-content: center; padding: 3rem; color: var(--color-fg-subtle); }
.layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(260px, 320px); gap: 1.25rem; align-items: start; }
@media (max-width: 1000px) { .layout { grid-template-columns: 1fr; } }
.form { display: grid; gap: 0.875rem; }
.field { display: grid; gap: 0.25rem; }
.lbl { font-size: var(--text-label-md); color: var(--color-fg-subtle); }
.row { display: flex; gap: 0.5rem; }
.control { width: 100%; min-height: 38px; padding: 0.4375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; font-size: var(--text-body-md); }
.control:focus { outline: none; border-color: var(--color-primary); }
.mt { margin-top: 1.5rem; }
.events { display: grid; gap: 0.375rem; }
.event { display: flex; align-items: center; gap: 0.625rem; padding: 0.5rem; border-radius: var(--radius-md); cursor: pointer; }
.event:hover { background: var(--color-surface-2); }
.event__body { flex: 1; min-width: 0; }
.event__label { margin: 0; font-size: var(--text-body-md); }
.event__desc { margin: 0; font-size: var(--text-label-sm); color: var(--color-fg-muted); }
.foot { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid var(--color-border); flex-wrap: wrap; }
.foot__btns { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.result { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.875rem; padding: 0.625rem 0.75rem; border-radius: var(--radius-md); font-size: var(--text-body-sm); }
.result--ok { background: color-mix(in oklab, var(--color-success) 12%, transparent); color: var(--color-success); }
.result--bad { background: color-mix(in oklab, var(--color-error) 12%, transparent); color: var(--color-error); }
.chips { display: flex; gap: 0.375rem; flex-wrap: wrap; }
.steps { margin: 0 0 0.875rem; padding-left: 1.125rem; display: grid; gap: 0.5rem; font-size: var(--text-body-sm); color: var(--color-fg-subtle); }
.muted { margin: 0; font-size: var(--text-label-md); color: var(--color-fg-muted); }
.link { color: var(--color-primary); }
</style>

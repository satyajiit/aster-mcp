<script setup lang="ts">
import type { ServerStatus } from '~/composables/useApi';

useHead({ title: 'Connect' });

const api = useApi();
const status = ref<ServerStatus | null>(null);
const loading = ref(true);

async function load() {
  status.value = await api.getStatus();
  loading.value = false;
}
usePolling(load, 15000);

/** Prefer the Tailscale URL when there is one — it is the reachable address. */
const mcpUrl = computed(
  () => status.value?.tailscale?.mcpUrl || status.value?.mcpUrl || '',
);

const mcpConfig = computed(() =>
  JSON.stringify(
    { mcpServers: { aster: { type: 'http', url: mcpUrl.value || 'http://<host>:5988/mcp' } } },
    null,
    2,
  ),
);

const claudeCli = computed(
  () => `claude mcp add --transport http aster ${mcpUrl.value || 'http://<host>:5988/mcp'}`,
);

const endpoints = computed(() => {
  const s = status.value;
  if (!s) return [];
  return [
    {
      label: 'MCP endpoint',
      value: s.mcpUrl,
      hint: 'Paste into Claude, Cursor or any MCP client',
      icon: 'ph:plug',
      accent: 'var(--color-mode-mcp)',
    },
    {
      label: 'Device WebSocket',
      value: s.wsUrl ?? `ws://<host>:${s.wsPort}`,
      hint: 'Enter this in the Aster companion app',
      icon: 'ph:broadcast',
      accent: 'var(--color-mode-remote)',
    },
    {
      label: 'Dashboard',
      value: s.dashboardUrl,
      hint: 'This page',
      icon: 'ph:monitor',
      accent: 'var(--color-primary)',
    },
  ].filter((e) => e.value);
});

const tailscaleEndpoints = computed(() => {
  const t = status.value?.tailscale;
  if (!t) return [];
  return [
    { label: 'MCP over Tailscale', value: t.mcpUrl },
    { label: 'WebSocket over Tailscale', value: t.wsUrl, hint: 'Encrypted — use this in the app' },
    { label: 'Dashboard over Tailscale', value: t.dashboardUrl },
  ].filter((e) => e.value);
});
</script>

<template>
  <div>
    <PageHeader
      title="Connect"
      description="Server endpoints and client configuration."
    >
      <template #actions>
        <AStatusPill
          v-if="status"
          status="running"
          :label="status.uptimeMs ? `Up ${formatDuration(status.uptimeMs)}` : 'Running'"
        />
      </template>
    </PageHeader>

    <div v-if="loading" class="loading"><ASpinner /> Reading server status…</div>

    <template v-else>
      <section class="section">
        <ASectionLabel label="Server endpoints" />
        <div class="endpoints">
          <ACard v-for="e in endpoints" :key="e.label">
            <div class="ep">
              <AIconTile :icon="e.icon" :accent="e.accent" />
              <div class="ep__body">
                <p class="ep__label">{{ e.label }}</p>
                <p class="ep__value mono">{{ e.value }}</p>
                <p class="ep__hint">{{ e.hint }}</p>
              </div>
            </div>
          </ACard>
        </div>
      </section>

      <section v-if="tailscaleEndpoints.length" class="section">
        <ASectionLabel label="Tailscale" />
        <ACard>
          <div class="ts">
            <AIconTile icon="ph:shield-check" accent="var(--color-success)" />
            <div>
              <p class="ep__label">{{ status?.tailscale?.dns }}</p>
              <p class="ep__hint mono">{{ status?.tailscale?.ip }}</p>
            </div>
          </div>
          <div class="ts__list">
            <div v-for="e in tailscaleEndpoints" :key="e.label" class="ts__row">
              <span class="ts__label">{{ e.label }}</span>
              <code class="ts__value">{{ e.value }}</code>
            </div>
          </div>
          <p class="note">
            Tailscale Serve terminates TLS, so the companion app can use
            <code>wss://</code> without a reverse proxy.
          </p>
        </ACard>
      </section>

      <section class="section">
        <ASectionLabel label="MCP client config" />
        <div class="configs">
          <ACard>
            <p class="cfg__title">Claude Desktop / Cursor</p>
            <p class="cfg__desc">Add to your <code>.mcp.json</code>:</p>
            <ACodeBlock :code="mcpConfig" label="mcp.json" />
          </ACard>
          <ACard>
            <p class="cfg__title">Claude Code</p>
            <p class="cfg__desc">Register from the terminal:</p>
            <ACodeBlock :code="claudeCli" label="shell" />
          </ACard>
        </div>
      </section>

      <section class="section">
        <ASectionLabel label="Runtime" />
        <InfoGrid
          :items="[
            { label: 'Process ID', value: status?.pid, mono: true, icon: 'ph:hash' },
            { label: 'Started', value: status?.startedAt ? new Date(status.startedAt).toLocaleString() : null, icon: 'ph:play' },
            { label: 'Uptime', value: status?.uptimeMs ? formatDuration(status.uptimeMs) : null, icon: 'ph:timer' },
            { label: 'WebSocket port', value: status?.wsPort, mono: true, icon: 'ph:broadcast' },
            { label: 'API port', value: status?.apiPort, mono: true, icon: 'ph:plug' },
            { label: 'Dashboard port', value: status?.dashboardPort, mono: true, icon: 'ph:monitor' },
            { label: 'Database', value: status?.dbPath, mono: true, icon: 'ph:database' },
          ]"
        />
      </section>
    </template>
  </div>
</template>

<style scoped>
.loading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 3rem;
  justify-content: center;
  color: var(--color-fg-subtle);
}

.section {
  margin-bottom: 1.75rem;
}

.endpoints {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 0.75rem;
}

.ep {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}

.ep__body {
  min-width: 0;
}

.ep__label {
  margin: 0;
  font-size: var(--text-label-md);
  letter-spacing: var(--text-label-md--letter-spacing);
  text-transform: uppercase;
  color: var(--color-fg-subtle);
}

.ep__value {
  margin: 0.25rem 0;
  font-size: var(--text-body-md);
  color: var(--color-fg);
  overflow-wrap: anywhere;
}

.ep__hint {
  margin: 0;
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

.ts {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.875rem;
}

.ts__list {
  display: grid;
  gap: 0.375rem;
}

.ts__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.ts__label {
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
}

.ts__value {
  font-family: var(--font-mono);
  font-size: var(--text-body-sm);
  color: var(--color-fg);
  overflow-wrap: anywhere;
}

.note {
  margin: 0.875rem 0 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-muted);
}

.note code,
.cfg__desc code {
  font-family: var(--font-mono);
  color: var(--color-primary);
}

.configs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 0.75rem;
}

.cfg__title {
  margin: 0;
  font-size: var(--text-title-md);
  font-weight: 600;
}

.cfg__desc {
  margin: 0.25rem 0 0.75rem;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
}
</style>

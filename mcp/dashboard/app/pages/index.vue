<script setup lang="ts">
import type { Device, LogEntry, ServerStatus, Stats } from '~/composables/useApi';

useHead({ title: 'Overview' });

const api = useApi();
const toast = useToast();

const stats = ref<Stats | null>(null);
const devices = ref<Device[]>([]);
const logs = ref<LogEntry[]>([]);
const status = ref<ServerStatus | null>(null);
const forwardingEnabled = ref(false);
const serverOnline = ref(true);
const loading = ref(true);

async function load() {
  try {
    const [s, d, l, st, fw] = await Promise.all([
      api.getStats(),
      api.getDevices(),
      api.getLogs({ limit: 12 }),
      api.getStatus().catch(() => null),
      api.getAgentEventForwardingConfig().catch(() => null),
    ]);
    stats.value = s;
    devices.value = d;
    logs.value = l.logs;
    status.value = st;
    forwardingEnabled.value = !!fw?.config?.enabled;
    serverOnline.value = true;
  } catch {
    serverOnline.value = false;
  } finally {
    loading.value = false;
  }
}

// One polling primitive with guaranteed teardown, replacing the interval this
// page used to start and never clear.
const { refresh } = usePolling(load, 5000);

const pending = computed(() => devices.value.filter((d) => d.status === 'pending'));

async function approve(id: string) {
  await api.approveDevice(id);
  toast.success('Device approved');
  await refresh();
}

async function reject(id: string) {
  await api.rejectDevice(id);
  toast.info('Device rejected');
  await refresh();
}
</script>

<template>
  <div>
    <PageHeader title="Overview" description="Server health, devices and recent activity.">
      <template #actions>
        <AStatusPill :status="serverOnline ? 'online' : 'error'" :label="serverOnline ? 'Server online' : 'Server unreachable'" />
        <AButton size="sm" icon="ph:arrows-clockwise" @click="refresh">Refresh</AButton>
      </template>
    </PageHeader>

    <div class="grid-stats">
      <AAnimatedEntrance :delay="0">
        <AStatCard label="Total devices" :value="stats?.totalDevices ?? '—'" icon="ph:devices" to="/devices" />
      </AAnimatedEntrance>
      <AAnimatedEntrance :delay="60">
        <AStatCard label="Online" :value="stats?.onlineDevices ?? '—'" icon="ph:wifi-high" accent="var(--color-success)" />
      </AAnimatedEntrance>
      <AAnimatedEntrance :delay="120">
        <AStatCard label="Pending approval" :value="stats?.pendingDevices ?? '—'" icon="ph:hourglass" accent="var(--color-warning)" />
      </AAnimatedEntrance>
      <AAnimatedEntrance :delay="180">
        <AStatCard label="Approved" :value="stats?.approvedDevices ?? '—'" icon="ph:shield-check" accent="var(--color-primary)" />
      </AAnimatedEntrance>
    </div>

    <!-- Pending devices are the one thing that needs action, so surface them
         above the fold rather than buried in the registry table. -->
    <section v-if="pending.length" class="section">
      <ASectionLabel label="Awaiting approval" :count="pending.length" />
      <ACard variant="flush">
        <DeviceRow
          v-for="d in pending"
          :key="d.id"
          :device="d"
          @approve="approve"
          @reject="reject"
        />
      </ACard>
    </section>

    <div class="split">
      <section>
        <ASectionLabel label="Devices">
          <template #actions>
            <NuxtLink to="/devices" class="more">View all</NuxtLink>
          </template>
        </ASectionLabel>
        <ACard variant="flush">
          <AEmptyState
            v-if="!loading && devices.length === 0"
            icon="ph:device-mobile-slash"
            title="No devices yet"
            description="Install the Aster companion on an Android device and point it at this server."
          >
            <NuxtLink to="/connect">
              <AButton variant="primary" size="sm">Connection details</AButton>
            </NuxtLink>
          </AEmptyState>
          <DeviceRow
            v-for="d in devices.slice(0, 5)"
            :key="d.id"
            :device="d"
            @approve="approve"
            @reject="reject"
          />
        </ACard>
      </section>

      <section>
        <ASectionLabel label="Recent activity">
          <template #actions>
            <NuxtLink to="/logs" class="more">Open logs</NuxtLink>
          </template>
        </ASectionLabel>
        <ACard variant="flush">
          <AEmptyState v-if="!loading && logs.length === 0" icon="ph:list-dashes" title="No activity yet" />
          <LogRow v-for="log in logs" :key="log.id" :log="log" show-device />
        </ACard>
      </section>
    </div>

    <StarRepoCard />

    <section class="section">
      <ASectionLabel label="Quick links" />
      <div class="grid-links">
        <NuxtLink to="/connect" class="link-card focus-ring">
          <AIconTile icon="ph:plugs-connected" accent="var(--color-mode-remote)" />
          <div>
            <p class="link-card__title">MCP connection</p>
            <p class="link-card__desc">
              {{ status?.mcpUrl ?? 'URLs, ports and client config' }}
            </p>
          </div>
        </NuxtLink>

        <NuxtLink to="/settings/event-forwarding" class="link-card focus-ring">
          <AIconTile icon="ph:broadcast" accent="var(--color-mode-mcp)" />
          <div>
            <p class="link-card__title">Event forwarding</p>
            <p class="link-card__desc">
              Push notifications and SMS to your agent
            </p>
          </div>
          <AStatusPill
            class="link-card__pill"
            :status="forwardingEnabled ? 'running' : 'offline'"
            :label="forwardingEnabled ? 'Enabled' : 'Off'"
          />
        </NuxtLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.grid-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1.75rem;
}

.section {
  margin-bottom: 1.75rem;
}

.split {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 1.25rem;
  margin-bottom: 1.75rem;
}

@media (max-width: 1000px) {
  .split {
    grid-template-columns: 1fr;
  }
}

.more {
  font-size: var(--text-label-md);
  color: var(--color-primary);
  text-decoration: none;
}

.grid-links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 0.75rem;
}

.link-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: var(--radius-card);
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
  text-decoration: none;
  color: inherit;
  transition: border-color var(--dur-base) var(--ease-out-soft);
}

.link-card:hover {
  border-color: var(--color-border-bright);
}

.link-card__title {
  margin: 0;
  font-size: var(--text-body-md);
  font-weight: 600;
  color: var(--color-fg);
}

.link-card__desc {
  margin: 0.125rem 0 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
  overflow-wrap: anywhere;
}

.link-card__pill {
  margin-left: auto;
}
</style>

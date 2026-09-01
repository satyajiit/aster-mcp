<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

interface Sms {
  id?: number; address?: string; body?: string; date?: number | string;
  type?: string; read?: boolean; threadId?: number;
}

/* `aster_read_sms` with no `type` reads content://sms — the whole store, not
 * just the inbox — so outbound rows arrive too and their `address` is the
 * recipient. Label the direction instead of calling every address a sender. */
const OUTGOING = new Set(['sent', 'outbox', 'queued', 'failed']);
const isOutgoing = (m: Sms) => OUTGOING.has((m.type ?? '').toLowerCase());

const { data, loading, error, run, toast } = useDeviceTool<{ messages?: Sms[] } | Sms[]>(props.deviceId);
const limit = ref(20);
const to = ref('');
const body = ref('');
const sending = ref(false);

const messages = computed<Sms[]>(() => {
  const d = data.value as any;
  if (!d) return [];
  return Array.isArray(d) ? d : (d.messages ?? []);
});

function load() {
  return run('aster_read_sms', { limit: limit.value }, 'Read SMS');
}

async function send() {
  if (!to.value || !body.value) return;
  sending.value = true;
  const ok = await run('aster_send_sms', { number: to.value, message: body.value }, 'Send SMS');
  sending.value = false;
  if (ok !== null) {
    toast.success('Message sent', `to ${to.value}`);
    body.value = '';
    await load();
  }
}

onMounted(load);
</script>

<template>
  <div class="grid">
    <PanelShell
      title="Messages"
      description="Recent SMS on the device."
      :loading="loading"
      :error="error"
      :empty="messages.length === 0"
      empty-title="No messages"
      empty-icon="ph:chat-circle"
    >
      <template #actions>
        <select v-model.number="limit" class="control control--sm" @change="load">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
        <AButton size="sm" icon="ph:arrows-clockwise" @click="load">Refresh</AButton>
      </template>

      <div class="msgs">
        <div v-for="(m, i) in messages" :key="m.id ?? i" class="msg">
          <AIconTile
            :icon="isOutgoing(m) ? 'ph:paper-plane-tilt' : 'ph:chat-circle-text'"
            :size="32"
            :accent="isOutgoing(m) ? 'var(--color-info)' : 'var(--color-accent)'"
          />
          <div class="msg__body">
            <p class="msg__from">
              <span class="msg__dir">{{ isOutgoing(m) ? 'To' : 'From' }}</span>
              {{ m.address ?? 'Unknown' }}
              <ABadge v-if="m.type && m.type !== 'inbox'" tone="neutral">{{ m.type }}</ABadge>
            </p>
            <p class="msg__text">{{ m.body }}</p>
          </div>
          <span class="msg__date">
            {{ typeof m.date === 'number' ? formatRelativeTime(m.date) : m.date }}
          </span>
        </div>
      </div>
    </PanelShell>

    <PanelShell title="Send a message" description="Sends from the device's own SIM.">
      <form class="form" @submit.prevent="send">
        <input v-model="to" class="control" placeholder="Recipient number" />
        <textarea v-model="body" class="control control--area" rows="4" placeholder="Message" />
        <AButton
          type="submit"
          variant="primary"
          icon="ph:paper-plane-tilt"
          :loading="sending"
          :disabled="!to || !body"
        >
          Send SMS
        </AButton>
      </form>
    </PanelShell>
  </div>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: minmax(0,1.4fr) minmax(280px,1fr); gap: 1rem; align-items: start; }
@media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
.msg__dir { font-size: var(--text-label-sm); color: var(--color-fg-muted); text-transform: uppercase; letter-spacing: 0.04em; margin-right: 0.25rem; }
.msgs { display: grid; gap: 1px; }
.msg { display: flex; align-items: flex-start; gap: 0.625rem; padding: 0.625rem 0; border-bottom: 1px solid var(--color-border); }
.msg:last-child { border-bottom: none; }
.msg__body { flex: 1; min-width: 0; }
.msg__from { margin: 0; font-size: var(--text-body-md); font-weight: 600; }
.msg__text { margin: 0.125rem 0 0; font-size: var(--text-body-sm); color: var(--color-fg-subtle); overflow-wrap: anywhere; }
.msg__date { font-size: var(--text-label-sm); color: var(--color-fg-muted); white-space: nowrap; }
.form { display: grid; gap: 0.625rem; }
.control { width: 100%; min-height: 38px; padding: 0.4375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; font-size: var(--text-body-md); }
.control:focus { outline: none; border-color: var(--color-primary); }
.control--area { resize: vertical; }
.control--sm { min-height: 32px; width: auto; }
</style>

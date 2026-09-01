<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

interface Notif {
  key?: string; packageName?: string; title?: string; text?: string; bigText?: string;
  subText?: string; category?: string; postTime?: number; isOngoing?: boolean;
}

const { data, loading, error, run, toast } = useDeviceTool<any>(props.deviceId);
const title = ref('');
const body = ref('');
const posting = ref(false);

const items = computed<Notif[]>(() => {
  const d = data.value;
  if (!d) return [];
  // The device returns the shade under `active` (with a sibling `count`),
  // not `notifications` — reading the wrong key left this permanently empty.
  return Array.isArray(d) ? d : (d.active ?? []);
});

const load = () => run('aster_read_notifications', { limit: 25 }, 'Read notifications');

async function post() {
  posting.value = true;
  const ok = await run('aster_post_notification', { title: title.value, body: body.value }, 'Post notification');
  posting.value = false;
  if (ok !== null) {
    toast.success('Notification posted');
    title.value = '';
    body.value = '';
  }
}

onMounted(load);
</script>

<template>
  <div class="grid">
    <PanelShell
      title="Notifications"
      description="What is currently on the device's shade."
      :loading="loading"
      :error="error"
      :empty="items.length === 0"
      empty-title="No notifications"
      empty-icon="ph:bell-slash"
    >
      <template #actions>
        <AButton size="sm" icon="ph:arrows-clockwise" @click="load">Refresh</AButton>
      </template>

      <div class="list">
        <div v-for="(n, i) in items" :key="n.key ?? i" class="item">
          <AIconTile icon="ph:bell" :size="32" accent="var(--color-warning)" />
          <div class="item__body">
            <p class="item__title">
              {{ n.title || '(no title)' }}
              <ABadge v-if="n.isOngoing" tone="neutral">ongoing</ABadge>
            </p>
            <p class="item__text">{{ n.text || n.bigText || n.subText }}</p>
            <code class="item__pkg">{{ n.packageName }}</code>
          </div>
          <span v-if="n.postTime" class="item__date">{{ formatRelativeTime(n.postTime) }}</span>
        </div>
      </div>
    </PanelShell>

    <PanelShell title="Post a notification" description="Shows on the device immediately.">
      <form class="form" @submit.prevent="post">
        <input v-model="title" class="control" placeholder="Title" />
        <textarea v-model="body" class="control control--area" rows="3" placeholder="Body" />
        <AButton type="submit" variant="primary" icon="ph:bell-ringing" :loading="posting" :disabled="!title">
          Post
        </AButton>
      </form>
    </PanelShell>
  </div>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: minmax(0,1.4fr) minmax(280px,1fr); gap: 1rem; align-items: start; }
@media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
.list { display: grid; }
.item { display: flex; align-items: flex-start; gap: 0.625rem; padding: 0.625rem 0; border-bottom: 1px solid var(--color-border); }
.item:last-child { border-bottom: none; }
.item__body { flex: 1; min-width: 0; }
.item__title { margin: 0; font-size: var(--text-body-md); font-weight: 600; }
.item__text { margin: 0.125rem 0; font-size: var(--text-body-sm); color: var(--color-fg-subtle); overflow-wrap: anywhere; }
.item__pkg { font-family: var(--font-mono); font-size: var(--text-label-sm); color: var(--color-fg-muted); }
.item__date { font-size: var(--text-label-sm); color: var(--color-fg-muted); white-space: nowrap; }
.form { display: grid; gap: 0.625rem; }
.control { width: 100%; min-height: 38px; padding: 0.4375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; font-size: var(--text-body-md); }
.control:focus { outline: none; border-color: var(--color-primary); }
.control--area { resize: vertical; }
</style>

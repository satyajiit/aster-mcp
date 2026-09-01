<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

interface Alarm {
  id?: string | number; hour?: number; minute?: number;
  label?: string; enabled?: boolean; days?: string[];
}

interface NextAlarm { triggerTimeMs?: number; triggerTimeFormatted?: string }

const DAY_NAMES = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

/**
 * `aster_get_alarms` hands back raw clock-provider columns, not a tidy model:
 * the primary key is `_id`, the minute column is `minutes`, `enabled` arrives
 * as the string "0"/"1" (and "0" is truthy in JS, so disabled alarms read as
 * On), and recurrence is the `daysofweek` bitmask. Normalise once here.
 */
function normalise(raw: any): Alarm {
  const bitmask = Number(raw?.daysofweek ?? raw?.daysOfWeek ?? 0);
  const days = Number.isFinite(bitmask) && bitmask > 0
    ? DAY_NAMES.filter((_, i) => bitmask & (1 << i))
    : (Array.isArray(raw?.days) ? raw.days : []);
  const enabled = raw?.enabled;
  return {
    id: raw?._id ?? raw?.id,
    hour: Number(raw?.hour ?? 0),
    minute: Number(raw?.minutes ?? raw?.minute ?? 0),
    label: raw?.label || raw?.message || 'Alarm',
    enabled: typeof enabled === 'string' ? enabled !== '0' : enabled,
    days,
  };
}

const { data, loading, error, run, toast } = useDeviceTool<any>(props.deviceId);
const hour = ref(7);
const minute = ref(30);
const message = ref('');

const alarms = computed<Alarm[]>(() => {
  const d = data.value;
  if (!d) return [];
  const rows = Array.isArray(d) ? d : (d.alarms ?? []);
  return Array.isArray(rows) ? rows.map(normalise) : [];
});

/*
 * Many devices (this one included) expose no queryable clock provider, and the
 * handler answers with only the next scheduled alarm plus a note. The panel used
 * to render "No alarms set" over the top of a real upcoming alarm.
 */
const nextAlarm = computed<NextAlarm | null>(() => data.value?.nextAlarm ?? null);
const note = computed<string | null>(() => data.value?.note ?? null);
const source = computed<string | null>(() => data.value?.source ?? null);

const load = () => run('aster_get_alarms', {}, 'Get alarms');

async function create() {
  const ok = await run(
    'aster_set_alarm',
    { hour: hour.value, minute: minute.value, message: message.value || undefined, skipUi: true },
    'Set alarm',
  );
  if (ok !== null) { toast.success('Alarm set'); message.value = ''; await load(); }
}

async function dismiss() {
  const ok = await run('aster_dismiss_alarm', {}, 'Dismiss alarm');
  if (ok !== null) { toast.success('Alarm dismissed'); await load(); }
}

async function remove(id: string | number) {
  const ok = await run('aster_delete_alarm', { alarmId: id }, 'Delete alarm');
  if (ok !== null) { toast.success('Alarm deleted'); await load(); }
}

function pad(n?: number) { return String(n ?? 0).padStart(2, '0'); }

onMounted(load);
</script>

<template>
  <div class="grid">
    <PanelShell
      title="Alarms"
      :loading="loading"
      :error="error"
      :empty="alarms.length === 0 && !nextAlarm"
      empty-title="No alarms set"
      empty-icon="ph:alarm"
    >
      <template #actions>
        <ABadge v-if="source" tone="neutral">{{ source }}</ABadge>
        <AButton size="sm" icon="ph:bell-slash" @click="dismiss">Dismiss ringing</AButton>
        <AButton size="sm" icon="ph:arrows-clockwise" @click="load">Refresh</AButton>
      </template>

      <div v-if="nextAlarm && alarms.length === 0" class="next">
        <AIconTile icon="ph:alarm" :size="34" accent="var(--color-error)" />
        <div class="row__body">
          <p class="row__time mono">{{ nextAlarm.triggerTimeFormatted ?? '—' }}</p>
          <p class="row__label">
            Next alarm{{ nextAlarm.triggerTimeMs ? ` · ${formatRelativeTime(nextAlarm.triggerTimeMs)}` : '' }}
          </p>
        </div>
      </div>
      <p v-if="note && alarms.length === 0" class="note">{{ note }}</p>

      <div class="list">
        <div v-for="(a, i) in alarms" :key="a.id ?? i" class="row">
          <AIconTile icon="ph:alarm" :size="34" accent="var(--color-error)" />
          <div class="row__body">
            <p class="row__time mono">{{ pad(a.hour) }}:{{ pad(a.minute) }}</p>
            <p class="row__label">{{ a.label }}</p>
            <p v-if="a.days?.length" class="row__days">{{ a.days.join(', ') }}</p>
          </div>
          <AStatusPill v-if="a.enabled != null" :status="a.enabled ? 'running' : 'offline'" :label="a.enabled ? 'On' : 'Off'" />
          <AButton v-if="a.id != null" size="sm" variant="ghost" icon="ph:trash" @click="remove(a.id)" />
        </div>
      </div>
    </PanelShell>

    <PanelShell title="New alarm">
      <form class="form" @submit.prevent="create">
        <div class="time">
          <input v-model.number="hour" type="number" min="0" max="23" class="control" aria-label="Hour" />
          <span>:</span>
          <input v-model.number="minute" type="number" min="0" max="59" class="control" aria-label="Minute" />
        </div>
        <input v-model="message" class="control" placeholder="Label (optional)" />
        <AButton type="submit" variant="primary" icon="ph:plus" :loading="loading">Set alarm</AButton>
      </form>
    </PanelShell>
  </div>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: minmax(0,1.4fr) minmax(260px,1fr); gap: 1rem; align-items: start; }
@media (max-width: 1000px) { .grid { grid-template-columns: 1fr; } }
.list { display: grid; }
.next { display: flex; align-items: center; gap: 0.625rem; padding: 0.5rem 0; }
.note { margin: 0.25rem 0 0; font-size: var(--text-label-md); color: var(--color-fg-muted); }
.row { display: flex; align-items: center; gap: 0.625rem; padding: 0.5rem 0; border-bottom: 1px solid var(--color-border); }
.row:last-child { border-bottom: none; }
.row__body { flex: 1; min-width: 0; }
.row__time { margin: 0; font-size: var(--text-headline-sm); font-weight: 700; }
.row__label { margin: 0; font-size: var(--text-body-sm); color: var(--color-fg-subtle); }
.row__days { margin: 0; font-size: var(--text-label-sm); color: var(--color-fg-muted); }
.form { display: grid; gap: 0.625rem; }
.time { display: flex; align-items: center; gap: 0.5rem; font-size: var(--text-headline-sm); }
.time .control { width: 5rem; text-align: center; font-family: var(--font-mono); }
.control { min-height: 38px; padding: 0.4375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; font-size: var(--text-body-md); }
.control:focus { outline: none; border-color: var(--color-primary); }
</style>

<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

/** `aster_get_volume` returns {ringerMode, streams:{<name>:{current,max,min}}}. */
interface VolumeState {
  ringerMode?: string;
  streams?: Record<string, { current: number; max: number; min: number }>;
}

// Two independent tool runners on purpose. Speech and playback used to share
// the volume runner, so firing either replaced `data` with their own result
// and the whole volume readout vanished until the next refresh.
const vol = useDeviceTool<VolumeState>(props.deviceId);
const act = useDeviceTool<unknown>(props.deviceId);
const toast = vol.toast;

const streams = ['media', 'ring', 'notification', 'alarm', 'call', 'system'] as const;
const stream = ref<(typeof streams)[number]>('media');
const level = ref(0);
const tts = ref('');
const audioSource = ref('');

/** Only the streams the device actually reported, in the declared order. */
const rows = computed(() => {
  const s = vol.data.value?.streams;
  if (!s) return [];
  return streams.filter((k) => s[k]).map((k) => ({ name: k, ...s[k]! }));
});

const current = computed(() => vol.data.value?.streams?.[stream.value] ?? null);
/* Android stream indexes are not percentages — media maxes at 30 here, call
 * at 12. A fixed 0-100 slider sent out-of-range values that the device
 * clamped, so most of the travel did nothing. */
const maxLevel = computed(() => current.value?.max ?? 15);
const minLevel = computed(() => current.value?.min ?? 0);
const percent = computed(() =>
  current.value ? Math.round((current.value.current / Math.max(current.value.max, 1)) * 100) : 0,
);

async function loadVolume() {
  await vol.run('aster_get_volume', {}, 'Get volume');
  syncLevel();
}

/** Park the slider on what the stream is actually set to. */
function syncLevel() {
  if (current.value) level.value = current.value.current;
}
watch(stream, syncLevel);

async function setVolume() {
  const ok = await vol.run(
    'aster_set_volume',
    { stream: stream.value, level: level.value },
    'Set volume',
  );
  if (ok !== null) { toast.success(`${stream.value} set to ${level.value}/${maxLevel.value}`); await loadVolume(); }
}

async function mute(muted: boolean) {
  const ok = await vol.run('aster_set_volume', { stream: stream.value, mute: muted }, 'Mute');
  if (ok !== null) { toast.success(muted ? 'Muted' : 'Unmuted'); await loadVolume(); }
}

async function speak() {
  const ok = await act.run('aster_speak_tts', { text: tts.value }, 'Speak');
  if (ok !== null) toast.success('Speaking on device');
}

async function play() {
  const ok = await act.run('aster_play_audio', { source: audioSource.value }, 'Play audio');
  if (ok !== null) toast.success('Playing');
}

async function stop() {
  const ok = await act.run('aster_stop_audio', {}, 'Stop audio');
  if (ok !== null) toast.info('Stopped');
}

onMounted(loadVolume);
</script>

<template>
  <div class="grid">
    <PanelShell title="Volume" :loading="vol.loading.value" :error="vol.error.value">
      <template #actions>
        <ABadge v-if="vol.data.value?.ringerMode" tone="neutral">
          {{ vol.data.value.ringerMode }}
        </ABadge>
        <AButton size="sm" icon="ph:arrows-clockwise" @click="loadVolume">Refresh</AButton>
      </template>

      <div v-if="rows.length" class="levels mb">
        <div v-for="r in rows" :key="r.name" class="lvl">
          <span class="lvl__name">{{ r.name }}</span>
          <span class="lvl__bar"><i :style="{ width: `${(r.current / Math.max(r.max, 1)) * 100}%` }" /></span>
          <span class="lvl__num mono">{{ r.current }}/{{ r.max }}</span>
        </div>
      </div>

      <div class="form">
        <select v-model="stream" class="control">
          <option v-for="s in streams" :key="s" :value="s">{{ s }}</option>
        </select>
        <div class="slider">
          <input v-model.number="level" type="range" :min="minLevel" :max="maxLevel" />
          <span class="slider__val mono">{{ level }}/{{ maxLevel }}</span>
        </div>
        <p class="hint">Currently {{ percent }}% on {{ stream }}.</p>
        <div class="btns">
          <AButton variant="primary" size="sm" icon="ph:speaker-high" @click="setVolume">Set</AButton>
          <AButton size="sm" icon="ph:speaker-slash" @click="mute(true)">Mute</AButton>
          <AButton size="sm" icon="ph:speaker-simple-high" @click="mute(false)">Unmute</AButton>
        </div>
      </div>
    </PanelShell>

    <PanelShell title="Speech & playback">
      <form class="form" @submit.prevent="speak">
        <label class="lbl">Text to speech</label>
        <textarea v-model="tts" class="control control--area" rows="3" placeholder="Say something on the device" />
        <AButton type="submit" variant="primary" size="sm" icon="ph:megaphone" :disabled="!tts">Speak</AButton>
      </form>

      <form class="form mt" @submit.prevent="play">
        <label class="lbl">Audio source</label>
        <input v-model="audioSource" class="control" placeholder="URL or device file path" />
        <div class="btns">
          <AButton type="submit" size="sm" icon="ph:play" :disabled="!audioSource">Play</AButton>
          <AButton size="sm" icon="ph:stop" @click="stop">Stop</AButton>
        </div>
      </form>
    </PanelShell>
  </div>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1rem; align-items: start; }
.form { display: grid; gap: 0.625rem; }
.mt { margin-top: 1.25rem; }
.mb { margin-bottom: 1rem; }
.lbl { font-size: var(--text-label-md); color: var(--color-fg-subtle); }
.slider { display: flex; align-items: center; gap: 0.625rem; }
.slider input { flex: 1; accent-color: var(--color-primary); }
.slider__val { font-size: var(--text-body-sm); color: var(--color-fg); width: 3.75rem; text-align: right; }
.hint { margin: 0; font-size: var(--text-label-md); color: var(--color-fg-muted); }
.levels { display: grid; gap: 0.375rem; }
.lvl { display: grid; grid-template-columns: 5.5rem 1fr auto; align-items: center; gap: 0.625rem; }
.lvl__name { font-size: var(--text-label-md); color: var(--color-fg-subtle); text-transform: capitalize; }
.lvl__bar { height: 6px; border-radius: 999px; background: var(--color-surface-3); overflow: hidden; }
.lvl__bar i { display: block; height: 100%; border-radius: 999px; background: var(--color-primary); }
.lvl__num { font-size: var(--text-label-sm); color: var(--color-fg-muted); }
.btns { display: flex; gap: 0.375rem; flex-wrap: wrap; }
.control { min-height: 38px; padding: 0.4375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; font-size: var(--text-body-md); }
.control:focus { outline: none; border-color: var(--color-primary); }
.control--area { resize: vertical; }
</style>

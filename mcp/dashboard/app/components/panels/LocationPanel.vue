<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

interface Loc { latitude?: number; longitude?: number; accuracy?: number; provider?: string; timestamp?: number; altitude?: number; speed?: number }

const { data, loading, error, run } = useDeviceTool<Loc>(props.deviceId);
const load = () => run('aster_get_location', {}, 'Get location');

const loc = computed(() => data.value);
const hasFix = computed(() => loc.value?.latitude != null && loc.value?.longitude != null);

// OpenStreetMap embed — no API key, and no third-party script on the page.
const mapUrl = computed(() => {
  if (!hasFix.value) return '';
  const { latitude: la, longitude: lo } = loc.value!;
  const d = 0.01;
  const bbox = [lo! - d, la! - d, lo! + d, la! + d].join('%2C');
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${la}%2C${lo}`;
});

const osmLink = computed(() =>
  hasFix.value
    ? `https://www.openstreetmap.org/?mlat=${loc.value!.latitude}&mlon=${loc.value!.longitude}#map=15/${loc.value!.latitude}/${loc.value!.longitude}`
    : '',
);

onMounted(load);
</script>

<template>
  <PanelShell
    title="Location"
    description="Last known position reported by the device."
    :loading="loading"
    :error="error"
    :empty="!loading && !error && !hasFix"
    empty-title="No location fix"
    empty-icon="ph:map-pin-line"
  >
    <template #actions>
      <AButton size="sm" icon="ph:crosshair" @click="load">Locate</AButton>
    </template>

    <div class="wrap">
      <iframe v-if="mapUrl" :src="mapUrl" class="map" title="Device location" loading="lazy" />
      <div class="side">
        <InfoGrid
          :items="[
            { label: 'Latitude', value: loc?.latitude, mono: true },
            { label: 'Longitude', value: loc?.longitude, mono: true },
            { label: 'Accuracy', value: loc?.accuracy != null ? `${Math.round(loc.accuracy)} m` : null },
            { label: 'Altitude', value: loc?.altitude != null ? `${Math.round(loc.altitude)} m` : null },
            { label: 'Speed', value: loc?.speed != null ? `${loc.speed} m/s` : null },
            { label: 'Provider', value: loc?.provider },
            { label: 'Fixed at', value: loc?.timestamp ? new Date(loc.timestamp).toLocaleString() : null },
          ]"
        />
        <a v-if="osmLink" :href="osmLink" target="_blank" rel="noopener" class="link">
          Open in OpenStreetMap <Icon name="ph:arrow-square-out" />
        </a>
      </div>
    </div>
  </PanelShell>
</template>

<style scoped>
.wrap { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(260px, 1fr); gap: 1rem; align-items: start; }
@media (max-width: 900px) { .wrap { grid-template-columns: 1fr; } }
.map { width: 100%; aspect-ratio: 4 / 3; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface-2); }
.side { display: grid; gap: 0.75rem; }
.link { display: inline-flex; align-items: center; gap: 0.375rem; font-size: var(--text-label-md); color: var(--color-primary); text-decoration: none; }
</style>

<script setup lang="ts">
import { toolImageSrc, toolJson, toolText } from '~/composables/useApi';

const route = useRoute();
const api = useApi();
const toast = useToast();
const deviceId = route.params.id as string;

useHead({ title: 'Screen control' });

interface HierarchyNode {
  text?: string;
  contentDescription?: string;
  className?: string;
  viewId?: string;
  bounds?: { left: number; top: number; right: number; bottom: number };
  clickable?: boolean;
  children?: HierarchyNode[];
}

const shot = ref<string>('');
const shotSize = ref<{ w: number; h: number } | null>(null);
const imgEl = ref<HTMLImageElement | null>(null);
const capturing = ref(false);
const acting = ref(false);
const autoRefresh = ref(false);
const lastAction = ref<string | null>(null);

const hierarchy = ref<HierarchyNode | null>(null);
const hierarchyMode = ref<'interactive' | 'summary' | 'full'>('interactive');
const loadingHierarchy = ref(false);
const showOverlay = ref(true);

const textToType = ref('');
const textToFind = ref('');

const GLOBAL_ACTIONS = [
  { action: 'BACK', icon: 'ph:arrow-arc-left', label: 'Back' },
  { action: 'HOME', icon: 'ph:house', label: 'Home' },
  { action: 'RECENTS', icon: 'ph:squares-four', label: 'Recents' },
  { action: 'NOTIFICATIONS', icon: 'ph:bell', label: 'Notifications' },
  { action: 'POWER_DIALOG', icon: 'ph:power', label: 'Power' },
  { action: 'LOCK_SCREEN', icon: 'ph:lock', label: 'Lock' },
] as const;

async function capture() {
  capturing.value = true;
  try {
    const result = await api.executeTool(deviceId, 'aster_take_screenshot', {});
    const image = result.content?.find((c) => c.type === 'image');
    if (image) {
      // toolImageSrc adds the data: URI prefix the server omits.
      shot.value = toolImageSrc(image);
    } else {
      toast.error('Screenshot failed', toolText(result).slice(0, 200));
    }
  } catch (e) {
    toast.error('Screenshot failed', e instanceof Error ? e.message : String(e));
  } finally {
    capturing.value = false;
  }
}

function onImageLoad() {
  if (imgEl.value) {
    shotSize.value = { w: imgEl.value.naturalWidth, h: imgEl.value.naturalHeight };
  }
}

/** Map a click on the rendered screenshot back to device pixel coordinates. */
function toDeviceCoords(e: MouseEvent) {
  const el = imgEl.value;
  if (!el || !shotSize.value) return null;
  const rect = el.getBoundingClientRect();
  const x = Math.round(((e.clientX - rect.left) / rect.width) * shotSize.value.w);
  const y = Math.round(((e.clientY - rect.top) / rect.height) * shotSize.value.h);
  return { x, y };
}

async function runTool(name: string, args: Record<string, unknown>, label: string) {
  acting.value = true;
  lastAction.value = label;
  try {
    const result = await api.executeTool(deviceId, name, args);
    if (result.isError) {
      toast.error(label + ' failed', toolText(result).slice(0, 200));
    } else {
      // Give the UI a beat to settle before re-capturing.
      await new Promise((r) => setTimeout(r, 350));
      await capture();
    }
  } catch (e) {
    toast.error(label + ' failed', e instanceof Error ? e.message : String(e));
  } finally {
    acting.value = false;
  }
}

let dragStart: { x: number; y: number } | null = null;

function onPointerDown(e: MouseEvent) {
  dragStart = toDeviceCoords(e);
}

async function onPointerUp(e: MouseEvent) {
  const end = toDeviceCoords(e);
  if (!dragStart || !end) return;
  const dx = end.x - dragStart.x;
  const dy = end.y - dragStart.y;
  const distance = Math.hypot(dx, dy);
  const start = dragStart;
  dragStart = null;

  // Under ~24 device px of travel this is a tap, not a swipe.
  if (distance < 24) {
    await runTool(
      'aster_input_gesture',
      { gestureType: 'TAP', points: [{ x: start.x, y: start.y }], duration: 100 },
      `Tap (${start.x}, ${start.y})`,
    );
  } else {
    await runTool(
      'aster_input_gesture',
      { gestureType: 'SWIPE', points: [start, end], duration: 300 },
      `Swipe (${start.x}, ${start.y}) → (${end.x}, ${end.y})`,
    );
  }
}

async function loadHierarchy() {
  loadingHierarchy.value = true;
  try {
    const result = await api.executeTool(deviceId, 'aster_get_screen_hierarchy', {
      mode: hierarchyMode.value,
      includeInvisible: false,
    });
    hierarchy.value = toolJson<HierarchyNode>(result);
  } catch (e) {
    toast.error('Could not read screen hierarchy', e instanceof Error ? e.message : String(e));
  } finally {
    loadingHierarchy.value = false;
  }
}

/** Flatten to the clickable nodes with usable bounds, for overlay + list. */
const clickables = computed(() => {
  const out: HierarchyNode[] = [];
  const walk = (n: HierarchyNode | null | undefined) => {
    if (!n) return;
    if (n.clickable && n.bounds) out.push(n);
    n.children?.forEach(walk);
  };
  walk(hierarchy.value);
  return out;
});

function nodeLabel(n: HierarchyNode) {
  return n.text || n.contentDescription || n.viewId || n.className || 'element';
}

function overlayStyle(n: HierarchyNode) {
  if (!n.bounds || !shotSize.value) return {};
  const { left, top, right, bottom } = n.bounds;
  return {
    left: `${(left / shotSize.value.w) * 100}%`,
    top: `${(top / shotSize.value.h) * 100}%`,
    width: `${((right - left) / shotSize.value.w) * 100}%`,
    height: `${((bottom - top) / shotSize.value.h) * 100}%`,
  };
}

async function tapNode(n: HierarchyNode) {
  if (!n.bounds) return;
  const x = Math.round((n.bounds.left + n.bounds.right) / 2);
  const y = Math.round((n.bounds.top + n.bounds.bottom) / 2);
  await runTool(
    'aster_input_gesture',
    { gestureType: 'TAP', points: [{ x, y }], duration: 100 },
    `Tap ${nodeLabel(n)}`,
  );
  await loadHierarchy();
}

async function sendText() {
  if (!textToType.value) return;
  await runTool('aster_input_text', { text: textToType.value }, 'Type text');
  textToType.value = '';
}

async function clickByText() {
  if (!textToFind.value) return;
  await runTool('aster_click_by_text', { text: textToFind.value }, `Click "${textToFind.value}"`);
}

onMounted(capture);

usePolling(
  async () => {
    if (autoRefresh.value && !acting.value && !capturing.value) await capture();
  },
  3000,
  { immediate: false },
);
</script>

<template>
  <div>
    <PageHeader
      title="Screen control"
      description="Tap, swipe and type on the device. Drag on the screenshot to swipe."
      :back-to="`/devices/${deviceId}`"
      back-label="Device"
    >
      <template #actions>
        <AToggle v-model="autoRefresh" label="Auto-refresh" />
        <AButton size="sm" icon="ph:camera" :loading="capturing" @click="capture">Capture</AButton>
      </template>
    </PageHeader>

    <div class="layout">
      <ACard class="screen">
        <div class="screen__frame">
          <div v-if="!shot && capturing" class="screen__placeholder"><ASpinner :size="28" /></div>
          <AEmptyState
            v-else-if="!shot"
            icon="ph:image-broken"
            title="No screenshot yet"
            description="Capture the device screen to start controlling it."
          >
            <AButton variant="primary" size="sm" @click="capture">Capture screen</AButton>
          </AEmptyState>

          <div v-else class="screen__stage">
            <img
              ref="imgEl"
              :src="shot"
              alt="Device screen"
              class="screen__img"
              draggable="false"
              @load="onImageLoad"
              @mousedown.prevent="onPointerDown"
              @mouseup.prevent="onPointerUp"
            />
            <div v-if="showOverlay" class="screen__overlay">
              <button
                v-for="(n, i) in clickables"
                :key="i"
                type="button"
                class="hit"
                :style="overlayStyle(n)"
                :title="nodeLabel(n)"
                @click.stop="tapNode(n)"
              />
            </div>
            <div v-if="acting" class="screen__busy"><ASpinner :size="24" /></div>
          </div>
        </div>

        <p v-if="lastAction" class="screen__last">
          <Icon name="ph:cursor-click" /> {{ lastAction }}
        </p>
      </ACard>

      <div class="panel">
        <ACard>
          <ASectionLabel label="Navigation" />
          <div class="actions">
            <AButton
              v-for="g in GLOBAL_ACTIONS"
              :key="g.action"
              size="sm"
              :icon="g.icon"
              :disabled="acting"
              @click="runTool('aster_global_action', { action: g.action }, g.label)"
            >
              {{ g.label }}
            </AButton>
          </div>
        </ACard>

        <ACard>
          <ASectionLabel label="Text input" />
          <form class="field" @submit.prevent="sendText">
            <input v-model="textToType" placeholder="Text to type on device" />
            <AButton type="submit" variant="primary" size="sm" :disabled="!textToType || acting">
              Send
            </AButton>
          </form>

          <ASectionLabel label="Click by text" class="mt" />
          <form class="field" @submit.prevent="clickByText">
            <input v-model="textToFind" placeholder="Visible label to tap" />
            <AButton type="submit" size="sm" :disabled="!textToFind || acting">Click</AButton>
          </form>
        </ACard>

        <ACard>
          <ASectionLabel :label="`Elements`" :count="clickables.length || null">
            <template #actions>
              <AToggle v-model="showOverlay" label="Overlay" />
            </template>
          </ASectionLabel>

          <div class="hier__bar">
            <select v-model="hierarchyMode" class="select" aria-label="Hierarchy detail">
              <option value="interactive">Interactive</option>
              <option value="summary">Summary</option>
              <option value="full">Full</option>
            </select>
            <AButton
              size="sm"
              icon="ph:tree-structure"
              :loading="loadingHierarchy"
              @click="loadHierarchy"
            >
              Read screen
            </AButton>
          </div>

          <div v-if="clickables.length" class="hier__list">
            <button
              v-for="(n, i) in clickables"
              :key="i"
              type="button"
              class="hier__item"
              :disabled="acting"
              @click="tapNode(n)"
            >
              <Icon name="ph:cursor-click" />
              <span class="hier__label">{{ nodeLabel(n) }}</span>
              <code v-if="n.viewId" class="hier__id">{{ n.viewId.split('/').pop() }}</code>
            </button>
          </div>
          <p v-else-if="!loadingHierarchy" class="hier__none">
            Read the screen to list tappable elements.
          </p>
        </ACard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 380px);
  gap: 1.25rem;
  align-items: start;
}

@media (max-width: 1100px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

.screen__frame {
  display: flex;
  justify-content: center;
  background: var(--color-surface-2);
  border-radius: var(--radius-md);
  min-height: 320px;
  overflow: hidden;
}

.screen__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4rem;
  color: var(--color-fg-subtle);
}

.screen__stage {
  position: relative;
  display: inline-block;
  max-height: 78vh;
}

.screen__img {
  display: block;
  max-width: 100%;
  max-height: 78vh;
  width: auto;
  cursor: crosshair;
  user-select: none;
}

.screen__overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hit {
  position: absolute;
  pointer-events: auto;
  border: 1px solid color-mix(in oklab, var(--color-primary) 45%, transparent);
  background: color-mix(in oklab, var(--color-primary) 8%, transparent);
  border-radius: 2px;
  cursor: pointer;
  padding: 0;
}

.hit:hover {
  background: color-mix(in oklab, var(--color-primary) 24%, transparent);
}

.screen__busy {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in oklab, var(--color-bg) 45%, transparent);
  color: var(--color-primary);
}

.screen__last {
  margin: 0.75rem 0 0;
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: var(--text-label-md);
  color: var(--color-fg-subtle);
}

.panel {
  display: grid;
  gap: 0.75rem;
}

.actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.375rem;
}

.field {
  display: flex;
  gap: 0.5rem;
}

.field input,
.select {
  flex: 1;
  min-width: 0;
  min-height: 36px;
  padding: 0 0.625rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface-2);
  color: var(--color-fg);
  font: inherit;
  font-size: var(--text-body-md);
}

.field input:focus,
.select:focus {
  outline: none;
  border-color: var(--color-primary);
}

.mt {
  margin-top: 1rem;
}

.hier__bar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.hier__list {
  display: grid;
  gap: 0.125rem;
  max-height: 320px;
  overflow-y: auto;
}

.hier__item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.4375rem 0.5rem;
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  color: var(--color-fg);
  font: inherit;
  font-size: var(--text-body-sm);
  text-align: left;
  cursor: pointer;
}

.hier__item:hover:not(:disabled) {
  background: var(--color-surface-2);
}

.hier__label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hier__id {
  font-family: var(--font-mono);
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

.hier__none {
  margin: 0;
  font-size: var(--text-label-md);
  color: var(--color-fg-muted);
}
</style>

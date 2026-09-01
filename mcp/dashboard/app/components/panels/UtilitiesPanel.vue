<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

const { data, loading, error, run, toast } = useDeviceTool<any>(props.deviceId);
const clipboardIn = ref('');
const clipboardOut = ref<string | null>(null);
const toastMsg = ref('');
const overlayHtml = ref('');
const callNumber = ref('');
const callScript = ref('');

async function getClipboard() {
  const d: any = await run('aster_get_clipboard', {}, 'Get clipboard');
  // {hasContent:false, content:null} on an empty clipboard — `?? JSON.stringify(d)`
  // used to print that whole blob, and a failed read printed the string "null".
  if (d === null) return;
  clipboardOut.value = typeof d === 'string' ? d : (d?.content ?? d?.text ?? '');
}

async function setClipboard() {
  const ok = await run('aster_set_clipboard', { text: clipboardIn.value }, 'Set clipboard');
  if (ok !== null) toast.success('Clipboard updated');
}

async function showToast() {
  const ok = await run('aster_show_toast', { message: toastMsg.value, duration: 'short' }, 'Show toast');
  if (ok !== null) toast.success('Toast shown on device');
}

async function showOverlay() {
  const ok = await run('aster_show_overlay', { html: overlayHtml.value, showCloseButton: true }, 'Show overlay');
  if (ok !== null) toast.success('Overlay shown on device');
}

async function call() {
  const ok = callScript.value
    ? await run('aster_make_call_with_voice', { number: callNumber.value, text: callScript.value }, 'Call with voice')
    : await run('aster_make_call', { number: callNumber.value }, 'Call');
  if (ok !== null) toast.success(`Calling ${callNumber.value}`);
}
</script>

<template>
  <div class="grid">
    <PanelShell title="Clipboard" :loading="loading" :error="error">
      <div class="form">
        <div class="btns">
          <AButton size="sm" icon="ph:clipboard-text" @click="getClipboard">Read device clipboard</AButton>
        </div>
        <pre v-if="clipboardOut !== null" class="out">{{ clipboardOut || '(empty)' }}</pre>
        <input v-model="clipboardIn" class="control" placeholder="Text to copy to device" />
        <AButton size="sm" variant="primary" icon="ph:clipboard" :disabled="!clipboardIn" @click="setClipboard">
          Set clipboard
        </AButton>
      </div>
    </PanelShell>

    <PanelShell title="On-screen messages">
      <div class="form">
        <label class="lbl">Toast</label>
        <input v-model="toastMsg" class="control" placeholder="Short message" />
        <AButton size="sm" icon="ph:chat-teardrop-text" :disabled="!toastMsg" @click="showToast">
          Show toast
        </AButton>

        <label class="lbl mt">Overlay HTML</label>
        <textarea v-model="overlayHtml" class="control control--area" rows="4" placeholder="<h1>Hello</h1>" />
        <AButton size="sm" icon="ph:stack" :disabled="!overlayHtml" @click="showOverlay">Show overlay</AButton>
      </div>
    </PanelShell>

    <PanelShell title="Place a call" description="Optionally speak a script once connected.">
      <form class="form" @submit.prevent="call">
        <input v-model="callNumber" class="control" placeholder="Number to dial" />
        <textarea
          v-model="callScript"
          class="control control--area"
          rows="3"
          placeholder="Optional script to speak after connecting"
        />
        <AButton type="submit" variant="primary" size="sm" icon="ph:phone-call" :disabled="!callNumber">
          {{ callScript ? 'Call and speak' : 'Call' }}
        </AButton>
      </form>
    </PanelShell>
  </div>
</template>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1rem; align-items: start; }
.form { display: grid; gap: 0.625rem; }
.lbl { font-size: var(--text-label-md); color: var(--color-fg-subtle); }
.mt { margin-top: 0.75rem; }
.btns { display: flex; gap: 0.375rem; flex-wrap: wrap; }
.out { margin: 0; padding: 0.625rem; border-radius: var(--radius-md); background: var(--color-surface-2); font-family: var(--font-mono); font-size: var(--text-body-sm); max-height: 160px; overflow: auto; white-space: pre-wrap; overflow-wrap: anywhere; }
.control { min-height: 38px; padding: 0.4375rem 0.625rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); color: var(--color-fg); font: inherit; font-size: var(--text-body-md); }
.control:focus { outline: none; border-color: var(--color-primary); }
.control--area { resize: vertical; font-family: var(--font-mono); }
</style>

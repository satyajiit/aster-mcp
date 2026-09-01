<script setup lang="ts">
const props = defineProps<{ deviceId: string }>();

interface ShellResult {
  command?: string; exitCode?: number; output?: string;
  success?: boolean; truncated?: boolean;
}

const { raw, loading, error, run } = useDeviceTool<ShellResult>(props.deviceId);
const command = ref('');
const history = ref<{ command: string; output: string; failed: boolean; exitCode?: number }[]>([]);

async function execute() {
  const cmd = command.value.trim();
  if (!cmd) return;
  const res = await run('aster_execute_shell', { command: cmd }, 'Shell');
  // The device wraps the run in {command, exitCode, output, success, truncated}.
  // Printing `raw` dumped that whole envelope into the terminal — newlines and
  // all — instead of the shell's own output.
  const failed = !!error.value || (res?.success === false);
  let output = res?.output ?? '';
  if (res?.truncated) output += '\n… output truncated by the device';
  if (!output && failed) output = error.value ?? raw.value;
  history.value.unshift({
    command: cmd,
    output,
    failed,
    exitCode: res?.exitCode,
  });
  history.value = history.value.slice(0, 25);
  command.value = '';
}
</script>

<template>
  <PanelShell
    title="Shell"
    description="Runs in the app's own sandbox — this is not root, and not adb."
  >
    <template #controls>
      <form class="prompt" @submit.prevent="execute">
        <span class="prompt__sym mono">$</span>
        <input
          v-model="command"
          class="prompt__input mono"
          placeholder="e.g. getprop ro.build.version.release"
          autocapitalize="off"
          autocorrect="off"
          spellcheck="false"
        />
        <AButton type="submit" size="sm" variant="primary" :loading="loading" :disabled="!command">
          Run
        </AButton>
      </form>
    </template>

    <AEmptyState
      v-if="history.length === 0"
      icon="ph:terminal-window"
      title="No commands run yet"
      description="Output appears here, newest first."
    />

    <div v-else class="hist">
      <div v-for="(h, i) in history" :key="i" class="entry">
        <p class="entry__cmd mono">
          <span class="entry__sym">$</span> {{ h.command }}
          <ABadge :tone="h.failed ? 'error' : 'success'">
            {{ h.exitCode != null ? `exit ${h.exitCode}` : (h.failed ? 'failed' : 'ok') }}
          </ABadge>
        </p>
        <pre class="entry__out">{{ h.output || '(no output)' }}</pre>
      </div>
    </div>
  </PanelShell>
</template>

<style scoped>
.prompt { display: flex; align-items: center; gap: 0.5rem; padding: 0.4375rem 0.625rem; margin-bottom: 1rem; border-radius: var(--radius-md); border: 1px solid var(--color-border); background: var(--color-surface-2); }
.prompt__sym { color: var(--color-primary); }
.prompt__input { flex: 1; min-width: 0; border: none; background: none; color: var(--color-fg); font-size: var(--text-body-sm); outline: none; }
.hist { display: grid; gap: 0.75rem; }
.entry { border: 1px solid var(--color-border); border-radius: var(--radius-md); overflow: hidden; }
.entry__cmd { display: flex; align-items: center; gap: 0.5rem; margin: 0; padding: 0.4375rem 0.625rem; background: var(--color-surface-2); font-size: var(--text-body-sm); }
.entry__sym { color: var(--color-primary); }
.entry__cmd > :last-child { margin-left: auto; }
.entry__out { margin: 0; padding: 0.625rem; font-family: var(--font-mono); font-size: var(--text-body-sm); line-height: 1.6; color: var(--color-fg-subtle); overflow-x: auto; max-height: 300px; white-space: pre-wrap; overflow-wrap: anywhere; }
</style>

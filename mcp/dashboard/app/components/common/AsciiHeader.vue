<script setup lang="ts">
import { ref, onMounted } from 'vue';

const visible = ref(false);

onMounted(() => {
  setTimeout(() => {
    visible.value = true;
  }, 100);
});

const ascii = `
 █████╗ ███████╗████████╗███████╗██████╗
██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔══██╗
███████║███████╗   ██║   █████╗  ██████╔╝
██╔══██║╚════██║   ██║   ██╔══╝  ██╔══██╗
██║  ██║███████║   ██║   ███████╗██║  ██║
╚═╝  ╚═╝╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝
`.trim();
</script>

<template>
  <div class="ascii-header" :class="{ 'opacity-100': visible, 'opacity-0': !visible }">
    <pre class="ascii-logo">{{ ascii }}</pre>
    <div class="mt-2 text-[10px] text-terminal-muted uppercase tracking-[0.3em]">
      MCP Server // Device Control Bridge
    </div>
  </div>
</template>

<style scoped>
.ascii-header {
  transition: opacity 0.5s ease-out;
}

.ascii-logo {
  font-size: 10px;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: #22d3ee;
  user-select: none;
  text-shadow:
    0 0 5px rgba(34, 211, 238, 0.4),
    0 0 20px rgba(34, 211, 238, 0.2),
    0 0 40px rgba(34, 211, 238, 0.1);
}
</style>

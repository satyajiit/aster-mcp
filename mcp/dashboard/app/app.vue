<script setup lang="ts">
useHead({
  htmlAttrs: {
    class: 'dark',
  },
});
</script>

<template>
  <div class="min-h-screen bg-terminal-bg text-terminal-text font-mono">
    <NuxtPage />
  </div>
</template>

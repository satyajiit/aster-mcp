<script setup lang="ts">
defineProps<{
  title?: string;
  showDots?: boolean;
}>();
</script>

<template>
  <div class="terminal-window">
    <div v-if="title || showDots" class="terminal-titlebar">
      <div v-if="showDots !== false" class="terminal-dots">
        <span class="terminal-dot terminal-dot-close"></span>
        <span class="terminal-dot terminal-dot-minimize"></span>
        <span class="terminal-dot terminal-dot-maximize"></span>
      </div>
      <span v-if="title" class="terminal-title">{{ title }}</span>
      <div v-if="showDots !== false" class="w-[62px]"></div>
    </div>
    <div class="terminal-body">
      <slot />
    </div>
  </div>
</template>

<template>
  <section id="tools" class="relative py-32 px-6">
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-aster/[0.015] to-transparent" />

    <div class="relative max-w-5xl mx-auto">
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">MCP Tools</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          40+ tools at your AI's fingertips
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Every tool is exposed via the Model Context Protocol &mdash; compatible with Claude, OpenClaw, and any MCP client.
        </p>
      </div>

      <!-- Tool categories -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div
          v-for="category in categories"
          :key="category.name"
          class="p-5 rounded-2xl bg-surface-raised border border-border-dim"
        >
          <div class="flex items-center gap-3 mb-4">
            <div class="w-8 h-8 rounded-lg flex items-center justify-center" :class="category.bg">
              <Icon :name="category.icon" class="text-sm" :class="category.color" />
            </div>
            <h3 class="text-sm font-semibold text-text-primary">{{ category.name }}</h3>
          </div>
          <div class="flex flex-wrap gap-1.5">
            <span
              v-for="tool in category.tools"
              :key="tool"
              class="px-2.5 py-1 rounded-md bg-surface border border-border-dim text-xs font-mono text-text-tertiary hover:text-aster hover:border-aster/20 transition-colors cursor-default"
            >
              {{ tool }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const categories = [
  {
    name: 'Screen & Input',
    icon: 'lucide:monitor',
    bg: 'bg-aster/10',
    color: 'text-aster',
    tools: [
      'take_screenshot',
      'get_screen_hierarchy',
      'find_element',
      'input_gesture',
      'input_text',
      'click_by_text',
      'click_by_id',
      'scroll',
    ],
  },
  {
    name: 'Navigation & System',
    icon: 'lucide:compass',
    bg: 'bg-violet-500/10',
    color: 'text-violet-400',
    tools: [
      'global_action',
      'launch_intent',
      'list_packages',
      'execute_shell',
      'get_device_info',
    ],
  },
  {
    name: 'Files & Storage',
    icon: 'lucide:hard-drive',
    bg: 'bg-amber-500/10',
    color: 'text-amber-400',
    tools: [
      'list_files',
      'read_file',
      'write_file',
      'delete_file',
      'analyze_storage',
      'find_large_files',
    ],
  },
  {
    name: 'Media & Search',
    icon: 'lucide:image',
    bg: 'bg-rose-500/10',
    color: 'text-rose-400',
    tools: [
      'search_media',
      'index_media_metadata',
    ],
  },
  {
    name: 'Communication',
    icon: 'lucide:message-circle',
    bg: 'bg-blue-500/10',
    color: 'text-blue-400',
    tools: [
      'read_notifications',
      'read_sms',
      'post_notification',
      'make_call',
      'show_toast',
    ],
  },
  {
    name: 'Device Sensors',
    icon: 'lucide:cpu',
    bg: 'bg-green-500/10',
    color: 'text-green-400',
    tools: [
      'get_battery',
      'get_location',
      'get_clipboard',
      'set_clipboard',
      'speak_tts',
      'play_audio',
      'vibrate',
      'show_overlay',
    ],
  },
]
</script>

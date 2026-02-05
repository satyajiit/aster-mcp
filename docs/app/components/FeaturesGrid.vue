<template>
  <section id="features" class="relative py-32 px-6">
    <div class="max-w-6xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Capabilities</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Everything you need to control Android
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          40+ tools spanning screen control, file management, media intelligence, and device automation.
        </p>
      </div>

      <!-- Feature cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <FeatureCard
          v-for="feature in features"
          :key="feature.title"
          v-bind="feature"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const features = [
  {
    icon: 'lucide:monitor-smartphone',
    title: 'Screen Control',
    description: 'Capture screenshots, read the UI tree, tap, swipe, type, and navigate — all through natural language.',
    accent: 'from-aster to-cyan-400',
  },
  {
    icon: 'lucide:folder-search',
    title: 'File Management',
    description: 'List, read, write, delete files. Analyze storage, find large files, and manage your device filesystem.',
    accent: 'from-amber-400 to-orange-400',
  },
  {
    icon: 'lucide:images',
    title: 'Media Intelligence',
    description: 'Search photos with natural language — "trip to Mumbai last month" or "sunset photos near the beach".',
    accent: 'from-violet-400 to-purple-400',
  },
  {
    icon: 'lucide:bell-ring',
    title: 'Notifications & SMS',
    description: 'Read system notifications in real-time, access SMS conversations and threads.',
    accent: 'from-rose-400 to-pink-400',
  },
  {
    icon: 'lucide:terminal',
    title: 'Shell Execution',
    description: 'Run shell commands within the app sandbox. Launch intents, manage packages, and automate workflows.',
    accent: 'from-green-400 to-emerald-400',
  },
  {
    icon: 'lucide:volume-2',
    title: 'Audio & Haptics',
    description: 'Text-to-speech, audio playback, custom vibration patterns, toast messages, and overlay displays.',
    accent: 'from-blue-400 to-indigo-400',
  },
  {
    icon: 'lucide:map-pin',
    title: 'Location & Battery',
    description: 'GPS coordinates, geocoding, battery health and charge level — all accessible to your AI assistant.',
    accent: 'from-teal-400 to-cyan-400',
  },
  {
    icon: 'lucide:phone-call',
    title: 'Calls & Overlays',
    description: 'Initiate phone calls, display web overlays on device, and post custom notifications with action buttons.',
    accent: 'from-sky-400 to-blue-400',
  },
]
</script>

<template>
  <section class="relative min-h-[100dvh] flex items-center justify-center overflow-hidden pt-16">
    <!-- Background effects -->
    <div class="hero-glow -top-40 left-1/2 -translate-x-1/2 opacity-60" />
    <div class="absolute inset-0">
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_rgba(45,212,191,0.04)_0%,_transparent_60%)]" />
      <!-- Grid pattern -->
      <div
        class="absolute inset-0 opacity-[0.03]"
        style="background-image: linear-gradient(rgba(255,255,255,.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.1) 1px, transparent 1px); background-size: 60px 60px;"
      />
    </div>

    <div class="relative max-w-4xl mx-auto px-6 text-center">
      <!-- Badge -->
      <div class="animate-fade-up inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-border-subtle bg-surface-raised/60 backdrop-blur-sm mb-8">
        <span class="relative flex h-2 w-2">
          <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-aster opacity-75" />
          <span class="relative inline-flex rounded-full h-2 w-2 bg-aster" />
        </span>
        <span class="text-xs font-medium text-text-secondary tracking-wide uppercase">Open Source &middot; MIT Licensed</span>
      </div>

      <!-- Headline with sliding text -->
      <h1 class="animate-fade-up delay-100 text-5xl sm:text-6xl lg:text-7xl font-bold tracking-tight leading-[1.08] mb-6">
        <div class="headline-slider relative" :style="{ minHeight: headlineHeight }">
          <Transition name="headline" mode="out-in">
            <div v-if="headlineIndex === 0" key="copilot" ref="headlineRef">
              <span class="text-text-primary">AI CoPilot</span>
              <br />
              <span class="bg-gradient-to-r from-aster via-aster-light to-teal-300 bg-clip-text text-transparent">for your Mobile</span>
            </div>
            <div v-else key="give" ref="headlineRef">
              <span class="text-text-primary">Give your AI</span>
              <br />
              <span class="bg-gradient-to-r from-amber-400 via-orange-400 to-amber-300 bg-clip-text text-transparent">its own phone</span>
            </div>
          </Transition>
        </div>
      </h1>

      <!-- Subheading -->
      <p class="animate-fade-up delay-200 text-lg sm:text-xl text-text-secondary max-w-2xl mx-auto leading-relaxed mb-10">
        Aster connects Android to AI assistants like Claude, OpenClaw, or MoltBot via
        <span class="text-text-primary font-medium">Model Context Protocol</span>.
        The CoPilot for your mobile &mdash; or give your AI a dedicated device and let it call, text, and act on its own.
      </p>

      <!-- CTA Buttons -->
      <div class="animate-fade-up delay-300 flex flex-col sm:flex-row items-center justify-center gap-4">
        <a
          href="#setup"
          class="group relative px-7 py-3.5 rounded-xl bg-aster text-surface font-semibold text-sm tracking-wide hover:bg-aster-light transition-all shadow-[0_0_30px_rgba(45,212,191,0.3)] hover:shadow-[0_0_40px_rgba(45,212,191,0.45)]"
        >
          Get Started
          <Icon name="lucide:arrow-right" class="inline ml-1.5 transition-transform group-hover:translate-x-0.5" />
        </a>
        <a
          href="https://github.com/satyajiit/aster-mcp"
          target="_blank"
          rel="noopener"
          class="px-7 py-3.5 rounded-xl border border-border-subtle text-text-secondary font-medium text-sm hover:border-aster/30 hover:text-text-primary transition-all"
        >
          <Icon name="mdi:github" class="inline mr-1.5" />
          View on GitHub
        </a>
      </div>

      <!-- Live conversation preview -->
      <div class="animate-fade-up delay-500 mt-20 max-w-2xl mx-auto">
        <div class="gradient-border p-5 sm:p-7">
          <!-- Window chrome -->
          <div class="flex items-center gap-2 mb-5 pb-4 border-b border-border-dim">
            <div class="flex gap-1.5">
              <span class="w-2.5 h-2.5 rounded-full bg-rose-500/60" />
              <span class="w-2.5 h-2.5 rounded-full bg-amber-500/60" />
              <span class="w-2.5 h-2.5 rounded-full bg-green-500/60" />
            </div>
            <span class="ml-2 text-[10px] font-mono text-text-tertiary tracking-wider uppercase">Aster &middot; Live</span>
            <span class="ml-auto relative flex h-1.5 w-1.5">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
              <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-green-400" />
            </span>
          </div>

          <!-- Chat area -->
          <div class="text-left space-y-4 min-h-[140px]">
            <!-- User message -->
            <Transition name="chat" mode="out-in">
              <div :key="currentIndex" class="space-y-4">
                <div class="flex items-start gap-3">
                  <div class="flex-shrink-0 w-7 h-7 rounded-full bg-gradient-to-br from-blue-500/30 to-indigo-500/30 border border-blue-500/20 flex items-center justify-center">
                    <Icon name="lucide:user" class="text-xs text-blue-400" />
                  </div>
                  <div class="pt-1">
                    <span class="text-[10px] font-mono text-text-tertiary block mb-1">you</span>
                    <p class="text-sm text-text-primary font-medium leading-relaxed">{{ conversations[currentIndex].prompt }}</p>
                  </div>
                </div>

                <!-- Aster action -->
                <div class="flex items-center gap-2 ml-10 py-1.5 px-3 rounded-lg bg-aster/[0.06] border border-aster/10 w-fit">
                  <Icon :name="conversations[currentIndex].actionIcon" class="text-xs text-aster" />
                  <span class="text-[11px] font-mono text-aster/80">{{ conversations[currentIndex].action }}</span>
                </div>

                <!-- AI response -->
                <div class="flex items-start gap-3">
                  <div
                    class="flex-shrink-0 w-7 h-7 rounded-full bg-gradient-to-br border flex items-center justify-center"
                    :class="conversations[currentIndex].assistant.avatarClass"
                  >
                    <Icon name="lucide:bot" class="text-xs" :class="conversations[currentIndex].assistant.iconColor" />
                  </div>
                  <div class="pt-1">
                    <span class="text-[10px] font-mono block mb-1" :class="conversations[currentIndex].assistant.nameColor">{{ conversations[currentIndex].assistant.name }}</span>
                    <p class="text-[13px] text-text-secondary leading-relaxed">{{ conversations[currentIndex].response }}</p>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <!-- Dots indicator -->
          <div class="flex items-center justify-center gap-1.5 mt-5 pt-4 border-t border-border-dim">
            <button
              v-for="(_, i) in conversations"
              :key="i"
              class="w-1.5 h-1.5 rounded-full transition-all duration-300 cursor-pointer"
              :class="i === currentIndex ? 'bg-aster w-4' : 'bg-text-tertiary/40 hover:bg-text-tertiary'"
              @click="goTo(i)"
            />
          </div>
        </div>
      </div>
    </div>

  </section>
</template>

<script setup lang="ts">
const assistants = {
  claude: {
    name: 'Claude',
    avatarClass: 'from-amber-500/30 to-orange-500/30 border-amber-500/20',
    iconColor: 'text-amber-400',
    nameColor: 'text-amber-400/70',
  },
  clawdbot: {
    name: 'ClawdBot',
    avatarClass: 'from-violet-500/30 to-purple-500/30 border-violet-500/20',
    iconColor: 'text-violet-400',
    nameColor: 'text-violet-400/70',
  },
  moltbot: {
    name: 'MoltBot',
    avatarClass: 'from-rose-500/30 to-pink-500/30 border-rose-500/20',
    iconColor: 'text-rose-400',
    nameColor: 'text-rose-400/70',
  },
  openclaw: {
    name: 'OpenClaw',
    avatarClass: 'from-sky-500/30 to-blue-500/30 border-sky-500/20',
    iconColor: 'text-sky-400',
    nameColor: 'text-sky-400/70',
  },
}

const conversations = [
  {
    prompt: 'Find duplicate photos on my phone and free up space',
    action: 'search_media → index_media_metadata → scanning 2,104 photos...',
    actionIcon: 'lucide:images',
    response: 'Found 47 duplicate sets taking up 1.2 GB. Want me to keep the best quality version of each and delete the rest?',
    assistant: assistants.claude,
  },
  {
    prompt: 'Vibrate my phone — it fell behind the couch again',
    action: 'vibrate → strong 3-pulse pattern',
    actionIcon: 'lucide:vibrate',
    response: 'Done! Vibrating with a strong repeating pattern. Let me know when you find it and I\'ll stop.',
    assistant: assistants.clawdbot,
  },
  {
    prompt: 'Show me all the photos I took in Goa last December',
    action: 'search_media → filtering by location & date...',
    actionIcon: 'lucide:map-pin',
    response: 'Found 83 photos from Goa, Dec 2024 — including 12 beach shots, 6 sunset panoramas, and a few food pics from that cafe by the shore.',
    assistant: assistants.moltbot,
  },
  {
    prompt: 'Read my notifications — anything urgent?',
    action: 'read_notifications → parsing 14 notifications...',
    actionIcon: 'lucide:bell-ring',
    response: '2 urgent Slack messages from your team, 1 missed call from Mom, and a delivery arriving between 2–4 PM. The rest are promotions.',
    assistant: assistants.openclaw,
  },
  {
    prompt: 'My phone storage is full. What\'s eating up all the space?',
    action: 'analyze_storage → find_large_files → scanning...',
    actionIcon: 'lucide:hard-drive',
    response: 'WhatsApp media: 8.2 GB, cached app data: 3.1 GB, old APKs: 1.4 GB. I can clean up 4.5 GB of safe-to-delete files right now.',
    assistant: assistants.claude,
  },
  {
    prompt: 'Open Maps and find the nearest coffee shop',
    action: 'launch_intent → input_text → navigating...',
    actionIcon: 'lucide:coffee',
    response: 'Google Maps is open with 3 results nearby. "Blue Tokai" is 400m away with 4.6 stars — want me to start navigation?',
    assistant: assistants.clawdbot,
  },
]

// Headline slider
const headlineIndex = ref(0)
const headlineRef = ref<HTMLElement | null>(null)
const headlineHeight = ref('auto')
let headlineInterval: ReturnType<typeof setInterval> | null = null

const currentIndex = ref(0)
let interval: ReturnType<typeof setInterval> | null = null

function goTo(i: number) {
  currentIndex.value = i
  resetInterval()
}

function resetInterval() {
  if (interval) clearInterval(interval)
  interval = setInterval(() => {
    currentIndex.value = (currentIndex.value + 1) % conversations.length
  }, 5000)
}

onMounted(() => {
  resetInterval()

  // Set initial headline height (double nextTick to ensure full layout)
  nextTick(() => {
    nextTick(() => {
      if (headlineRef.value) {
        headlineHeight.value = headlineRef.value.offsetHeight + 'px'
      }
    })
  })

  // Cycle headlines every 4 seconds
  headlineInterval = setInterval(() => {
    headlineIndex.value = (headlineIndex.value + 1) % 2
    nextTick(() => {
      nextTick(() => {
        if (headlineRef.value) {
          headlineHeight.value = headlineRef.value.offsetHeight + 'px'
        }
      })
    })
  }, 4000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
  if (headlineInterval) clearInterval(headlineInterval)
})
</script>

<style scoped>
.chat-enter-active {
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
.chat-leave-active {
  transition: all 0.25s cubic-bezier(0.55, 0, 1, 0.45);
}
.chat-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.chat-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Headline slide transition */
.headline-slider {
  transition: height 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.headline-enter-active {
  transition: all 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}
.headline-leave-active {
  transition: all 0.35s cubic-bezier(0.55, 0, 1, 0.45);
}
.headline-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.headline-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
</style>

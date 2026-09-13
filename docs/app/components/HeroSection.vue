<template>
  <section class="relative min-h-[100dvh] flex items-center justify-center overflow-hidden pt-24 pb-16">
    <!-- Decorative background. Nothing here carries meaning. -->
    <div class="hero-glow -top-40 left-1/2 -translate-x-1/2 opacity-60" aria-hidden="true" />
    <div class="absolute inset-0" aria-hidden="true">
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_rgba(45,212,191,0.04)_0%,_transparent_60%)]" />
      <div
        class="absolute inset-0 opacity-[0.03]"
        style="background-image: linear-gradient(rgba(255,255,255,.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.1) 1px, transparent 1px); background-size: 60px 60px;"
      />
    </div>

    <div class="relative max-w-4xl mx-auto px-6 text-center">
      <div class="animate-fade-up inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-border-subtle bg-surface-raised/60 backdrop-blur-sm mb-8">
        <span class="relative flex h-2 w-2" aria-hidden="true">
          <span class="ping-dot absolute inline-flex h-full w-full rounded-full bg-aster opacity-75" />
          <span class="relative inline-flex rounded-full h-2 w-2 bg-aster" />
        </span>
        <span class="text-xs font-medium text-text-secondary tracking-wide uppercase">Open source &middot; MIT licensed &middot; v{{ FACTS.serverVersion }}</span>
      </div>

      <!-- ONE stable headline. It used to swap between two sentences every four
           seconds, which meant the prerendered HTML — the only copy a crawler or
           an LLM fetcher ever reads — carried whichever half happened to be
           first, and the page's own <h1> changed under a screen reader mid-read.
           The alternation survives below as decoration, marked aria-hidden,
           and both of its phrasings are restated in the paragraph that follows. -->
      <h1 class="animate-fade-up delay-100 text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight leading-[1.1] mb-5">
        <span class="text-text-primary">Aster gives your AI assistant </span>
        <br />
        <span class="bg-gradient-to-r from-aster via-aster-light to-teal-300 bg-clip-text text-transparent">hands on an Android phone</span>
      </h1>

      <p class="animate-fade-up delay-200 h-7 mb-4" aria-hidden="true">
        <Transition name="kicker" mode="out-in">
          <span
            :key="kickerIndex"
            class="text-xs sm:text-sm font-mono uppercase tracking-[0.2em]"
            :class="kickerIndex === 0 ? 'text-aster' : 'text-amber-400'"
          >{{ KICKERS[kickerIndex] }}</span>
        </Transition>
      </p>

      <p class="animate-fade-up delay-200 text-lg sm:text-xl text-text-secondary max-w-2xl mx-auto leading-relaxed mb-4">
        Aster connects an Android phone to Claude, OpenClaw or MoltBot &mdash; AI agent clients that speak the
        <span class="text-text-primary font-medium">Model Context Protocol</span>.
        Run it as the copilot for the phone in your pocket, or plug a spare handset into a charger and
        give your AI its own device &mdash; one that calls, texts and acts on its own.
      </p>

      <!-- The name collides with much larger entities (Aster DM Healthcare, the
           ASTER satellite instrument). The JSON-LD carries a
           disambiguatingDescription, but that does not help a fetcher reading
           text, so the page says it in prose as well. -->
      <p class="animate-fade-up delay-200 text-sm text-text-tertiary max-w-2xl mx-auto leading-relaxed mb-10">
        Open source, self-hosted, and published to npm as
        <code class="text-text-secondary">{{ FACTS.npmPackage }}</code>.
        Unrelated to Aster DM Healthcare or the ASTER satellite instrument.
      </p>

      <div class="animate-fade-up delay-300 flex flex-col sm:flex-row items-center justify-center gap-4">
        <NuxtLink
          to="/setup/"
          class="group relative inline-flex items-center min-h-12 px-7 rounded-xl bg-aster text-surface font-semibold text-sm tracking-wide hover:bg-aster-light transition-colors shadow-[0_0_30px_rgba(45,212,191,0.3)] hover:shadow-[0_0_40px_rgba(45,212,191,0.45)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
        >
          Set up Aster
          <Icon name="lucide:arrow-right" class="inline ml-1.5 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
        </NuxtLink>
        <a
          :href="LINKS.repo"
          target="_blank"
          rel="noopener"
          class="inline-flex items-center min-h-12 px-7 rounded-xl border border-border-subtle text-text-secondary font-medium text-sm hover:border-aster/30 hover:text-text-primary transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
        >
          <Icon name="mdi:github" class="mr-1.5" aria-hidden="true" />
          View source on GitHub
        </a>
      </div>

      <!-- Example exchange carousel -->
      <div class="animate-fade-up delay-500 mt-16 max-w-2xl mx-auto">
        <div class="gradient-border p-5 sm:p-7">
          <div class="flex items-center gap-2 mb-5 pb-4 border-b border-border-dim">
            <div class="flex gap-1.5" aria-hidden="true">
              <span class="w-2.5 h-2.5 rounded-full bg-rose-500/60" />
              <span class="w-2.5 h-2.5 rounded-full bg-amber-500/60" />
              <span class="w-2.5 h-2.5 rounded-full bg-green-500/60" />
            </div>
            <h2 class="ml-2 text-[10px] font-mono text-text-tertiary tracking-wider uppercase">What people ask for</h2>
            <button
              type="button"
              class="ml-auto inline-flex items-center gap-1.5 min-h-8 px-2.5 rounded-lg border border-border-dim text-[11px] font-medium text-text-secondary hover:text-text-primary hover:border-border-subtle transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              @click="togglePlay"
            >
              <Icon :name="playing ? 'lucide:pause' : 'lucide:play'" class="text-xs" aria-hidden="true" />
              <span>{{ playing ? 'Pause' : 'Play' }}</span>
              <span class="sr-only">the rotating examples and the animated strapline</span>
            </button>
          </div>

          <div
            class="text-left space-y-4 min-h-[150px]"
            role="group"
            :aria-label="`Example ${currentIndex + 1} of ${CONVERSATIONS.length}`"
          >
            <Transition name="chat" mode="out-in">
              <div :key="currentIndex" class="space-y-4">
                <div class="flex items-start gap-3">
                  <div class="flex-shrink-0 w-7 h-7 rounded-full bg-gradient-to-br from-blue-500/30 to-indigo-500/30 border border-blue-500/20 flex items-center justify-center">
                    <Icon name="lucide:user" class="text-xs text-blue-400" aria-hidden="true" />
                  </div>
                  <div class="pt-1">
                    <span class="text-[10px] font-mono text-text-tertiary block mb-1">you</span>
                    <p class="text-sm text-text-primary font-medium leading-relaxed">{{ current.prompt }}</p>
                  </div>
                </div>

                <p class="flex items-center gap-2 ml-10 py-1.5 px-3 rounded-lg bg-aster/[0.08] border border-aster/20 w-fit">
                  <Icon :name="current.actionIcon" class="text-xs text-aster flex-shrink-0" aria-hidden="true" />
                  <code class="text-[11px] font-mono text-aster">{{ current.action }}</code>
                </p>

                <div class="flex items-start gap-3">
                  <div
                    class="flex-shrink-0 w-7 h-7 rounded-full bg-gradient-to-br border flex items-center justify-center"
                    :class="current.assistant.avatarClass"
                  >
                    <Icon name="lucide:bot" class="text-xs" :class="current.assistant.iconColor" aria-hidden="true" />
                  </div>
                  <div class="pt-1">
                    <span class="text-[10px] font-mono block mb-1" :class="current.assistant.nameColor">{{ current.assistant.name }}</span>
                    <p class="text-[13px] text-text-secondary leading-relaxed">{{ current.response }}</p>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <div class="flex items-center justify-center gap-0.5 mt-5 pt-2 border-t border-border-dim">
            <button
              v-for="(item, i) in CONVERSATIONS"
              :key="item.prompt"
              type="button"
              class="inline-flex items-center justify-center w-8 h-8 rounded-full transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              :aria-current="i === currentIndex ? 'true' : undefined"
              @click="goTo(i)"
            >
              <span
                class="block h-1.5 rounded-full transition-all duration-300"
                :class="i === currentIndex ? 'w-4 bg-aster' : 'w-1.5 bg-text-tertiary'"
                aria-hidden="true"
              />
              <span class="sr-only">Show example {{ i + 1 }} of {{ CONVERSATIONS.length }}: {{ item.prompt }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { FACTS, LINKS, TOOL_PREFIX } from '~/data/site'

/** Decorative only, and aria-hidden in the template. Both phrasings appear in
 *  the paragraph under the headline, so nothing is lost when it is paused. */
const KICKERS = ['Your AI copilot on mobile', 'Or give your AI its own phone'] as const

const ASSISTANTS = {
  claude: {
    name: 'Claude',
    avatarClass: 'from-amber-500/30 to-orange-500/30 border-amber-500/20',
    iconColor: 'text-amber-400',
    nameColor: 'text-amber-400',
  },
  clawdbot: {
    name: 'ClawdBot',
    avatarClass: 'from-violet-500/30 to-purple-500/30 border-violet-500/20',
    iconColor: 'text-violet-400',
    nameColor: 'text-violet-400',
  },
  moltbot: {
    name: 'MoltBot',
    avatarClass: 'from-rose-500/30 to-pink-500/30 border-rose-500/20',
    iconColor: 'text-rose-400',
    nameColor: 'text-rose-400',
  },
  openclaw: {
    name: 'OpenClaw',
    avatarClass: 'from-sky-500/30 to-blue-500/30 border-sky-500/20',
    iconColor: 'text-sky-400',
    nameColor: 'text-sky-400',
  },
} as const

const t = (name: string) => `${TOOL_PREFIX}${name}`

/**
 * Five examples, not eight. "Vibrate my phone, it fell behind the couch" and
 * "photos I took in Goa" were printed here, in EmbraceSection and again in
 * UseCasesSection; they now appear once, on /use-cases.
 *
 * Every `action` string is a REAL, callable tool name — the MCP server
 * registers all 49 with the aster_ prefix (mcp/src/mcp/tools.ts), so the
 * unprefixed names this block used to print could not be called by anything.
 */
const CONVERSATIONS = [
  {
    prompt: 'Find duplicate photos on my phone and free up space',
    action: `${t('index_media_metadata')} → ${t('search_media')}`,
    actionIcon: 'lucide:images',
    response: 'Found 47 duplicate sets taking up 1.2 GB. Want me to keep the best copy of each and delete the rest?',
    assistant: ASSISTANTS.claude,
  },
  {
    prompt: 'Read my notifications — anything urgent?',
    action: `${t('read_notifications')}`,
    actionIcon: 'lucide:bell-ring',
    response: 'Two Slack messages flagged urgent, one missed call, and a delivery arriving between 2 and 4 PM. The rest are promotions.',
    assistant: ASSISTANTS.openclaw,
  },
  {
    prompt: 'My storage is full. What is eating all the space?',
    action: `${t('analyze_storage')} → ${t('find_large_files')}`,
    actionIcon: 'lucide:hard-drive',
    response: 'Messaging media 8.2 GB, cached app data 3.1 GB, old installers 1.4 GB. I can clear 4.5 GB that is safe to delete.',
    assistant: ASSISTANTS.clawdbot,
  },
  {
    prompt: 'Open Maps and find the nearest coffee shop',
    action: `${t('launch_intent')} → ${t('input_text')} → ${t('click_by_text')}`,
    actionIcon: 'lucide:coffee',
    response: 'Maps is open with three results nearby. The closest is 400 m away and rated 4.6. Want me to start navigation?',
    assistant: ASSISTANTS.moltbot,
  },
  {
    prompt: 'If my flight is delayed, call me and tell me the new time',
    action: `event forward → ${t('make_call_with_voice')}`,
    actionIcon: 'lucide:plane',
    response: 'Your flight is delayed 45 minutes — new boarding time 3:15 PM, gate B12. Calling you now to say so.',
    assistant: ASSISTANTS.claude,
  },
] as const

const currentIndex = ref(0)
const kickerIndex = ref(0)
const current = computed(() => CONVERSATIONS[currentIndex.value]!)

/**
 * WCAG 2.2.2 (Level A): both of these advance on their own, so there has to be
 * a pause control, and neither may start under prefers-reduced-motion. They
 * start PAUSED and are switched on in onMounted, which also keeps the
 * prerendered markup identical to the first painted frame.
 */
const playing = ref(false)
let chatTimer: ReturnType<typeof setInterval> | null = null
let kickerTimer: ReturnType<typeof setInterval> | null = null

function stopTimers() {
  if (chatTimer) clearInterval(chatTimer)
  if (kickerTimer) clearInterval(kickerTimer)
  chatTimer = null
  kickerTimer = null
}

function startTimers() {
  stopTimers()
  chatTimer = setInterval(() => {
    currentIndex.value = (currentIndex.value + 1) % CONVERSATIONS.length
  }, 5000)
  kickerTimer = setInterval(() => {
    kickerIndex.value = (kickerIndex.value + 1) % KICKERS.length
  }, 4000)
}

function togglePlay() {
  playing.value = !playing.value
  if (playing.value) startTimers()
  else stopTimers()
}

function goTo(i: number) {
  currentIndex.value = i
  // A deliberate pick should not be yanked away a moment later.
  if (playing.value) startTimers()
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  playing.value = true
  startTimers()
})

onUnmounted(stopTimers)
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

.kicker-enter-active {
  transition: all 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}
.kicker-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 1, 0.45);
}
.kicker-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.kicker-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.ping-dot {
  animation: hero-ping 1.6s cubic-bezier(0, 0, 0.2, 1) infinite;
}

@keyframes hero-ping {
  75%, 100% {
    transform: scale(2);
    opacity: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .chat-enter-active,
  .chat-leave-active,
  .kicker-enter-active,
  .kicker-leave-active {
    transition: none;
  }
  .ping-dot {
    animation: none;
  }
}
</style>

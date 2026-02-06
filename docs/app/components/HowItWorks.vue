<template>
  <section id="how-it-works" class="relative py-32 px-6 overflow-hidden">
    <!-- Background -->
    <div class="absolute inset-0">
      <div
        class="absolute inset-0 opacity-[0.018]"
        style="background-image: linear-gradient(rgba(45,212,191,.25) 1px, transparent 1px), linear-gradient(90deg, rgba(45,212,191,.25) 1px, transparent 1px); background-size: 48px 48px;"
      />
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_50%_40%,_rgba(45,212,191,0.04)_0%,_transparent_55%)]" />
    </div>

    <div class="relative max-w-5xl mx-auto">
      <!-- Header -->
      <div class="text-center mb-14">
        <span class="text-[10px] font-mono uppercase tracking-[0.3em] text-aster/70 mb-4 block">Command Trace</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Watch a command propagate
        </h2>
        <p class="mt-4 text-sm text-text-secondary max-w-lg mx-auto leading-relaxed">
          From natural language to hardware execution &mdash; trace a command across AI, server, and device. Works the same whether the AI controls your phone or its own.
        </p>
      </div>

      <!-- Scenario selector pills -->
      <div class="flex flex-wrap justify-center gap-1.5 mb-10">
        <button
          v-for="(s, i) in scenarios"
          :key="i"
          class="px-3.5 py-1.5 rounded-lg text-[11px] font-mono transition-all duration-300 cursor-pointer"
          :class="current === i
            ? 'bg-aster/10 text-aster border border-aster/20'
            : 'text-text-tertiary border border-transparent hover:text-text-secondary hover:bg-white/[0.02]'"
          @click="play(i)"
        >
          <Icon :name="s.icon" class="inline mr-1 text-[10px]" />
          {{ s.label }}
        </button>
      </div>

      <!-- Main tracer panel -->
      <div class="rounded-2xl bg-surface-raised/70 border border-border-dim backdrop-blur-sm overflow-hidden">
        <!-- Panel header bar -->
        <div class="flex items-center gap-2 px-5 py-3 border-b border-border-dim">
          <div class="flex gap-1.5">
            <span class="w-2 h-2 rounded-full bg-rose-500/50" />
            <span class="w-2 h-2 rounded-full bg-amber-500/50" />
            <span class="w-2 h-2 rounded-full bg-green-500/50" />
          </div>
          <span class="text-[9px] font-mono text-text-tertiary/50 tracking-wider ml-1 uppercase">aster &middot; trace</span>
          <div class="ml-auto flex items-center gap-1.5">
            <span class="relative flex h-1.5 w-1.5">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
              <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-green-400" />
            </span>
            <span class="text-[9px] font-mono text-green-400/60">live</span>
          </div>
        </div>

        <!-- Prompt row -->
        <div class="px-5 py-4 border-b border-border-dim bg-white/[0.01]">
          <div class="flex items-start gap-2.5">
            <span class="text-aster/50 font-mono text-sm select-none mt-0.5">&gt;</span>
            <Transition name="cmd" mode="out-in">
              <p :key="current" class="text-sm text-text-primary font-mono leading-relaxed">
                {{ activeScenario.prompt }}
              </p>
            </Transition>
          </div>
        </div>

        <!-- 3-column node flow (desktop) / stacked (mobile) -->
        <div class="p-5 sm:p-6">
          <!-- Horizontal connection lines (desktop only) -->
          <div class="hidden md:block relative mb-6">
            <div class="grid grid-cols-3 gap-4">
              <!-- 3 node headers -->
              <div class="flex flex-col items-center">
                <div
                  class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-500"
                  :class="phase >= 0
                    ? 'bg-violet-500/15 ring-2 ring-violet-500/30 shadow-[0_0_24px_rgba(167,139,250,0.15)]'
                    : 'bg-violet-500/[0.05] ring-1 ring-violet-500/10'"
                >
                  <Icon name="lucide:brain" class="text-xl text-violet-400" />
                </div>
                <h3 class="text-xs font-semibold text-text-primary mt-3">AI Assistant</h3>
                <span class="text-[9px] font-mono text-text-tertiary">MCP Client</span>
              </div>

              <div class="flex flex-col items-center">
                <div
                  class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-500"
                  :class="phase >= 1
                    ? 'bg-aster/15 ring-2 ring-aster/30 shadow-[0_0_24px_rgba(45,212,191,0.15)]'
                    : 'bg-aster/[0.05] ring-1 ring-aster/10'"
                >
                  <Icon name="lucide:server" class="text-xl text-aster" />
                </div>
                <h3 class="text-xs font-semibold text-text-primary mt-3">Aster Server</h3>
                <span class="text-[9px] font-mono text-text-tertiary">Node.js &middot; WS Bridge</span>
              </div>

              <div class="flex flex-col items-center">
                <div
                  class="w-14 h-14 rounded-2xl flex items-center justify-center transition-all duration-500"
                  :class="phase >= 2
                    ? 'bg-green-500/15 ring-2 ring-green-500/30 shadow-[0_0_24px_rgba(74,222,128,0.15)]'
                    : 'bg-green-500/[0.05] ring-1 ring-green-500/10'"
                >
                  <Icon name="lucide:smartphone" class="text-xl text-green-400" />
                </div>
                <h3 class="text-xs font-semibold text-text-primary mt-3">Android Device</h3>
                <span class="text-[9px] font-mono text-text-tertiary">Kotlin &middot; Accessibility</span>
              </div>
            </div>

            <!-- Connection beams overlaid between columns -->
            <div class="absolute top-7 left-0 right-0 pointer-events-none">
              <div class="relative mx-auto" style="width: 66.66%; left: 0%;">
                <!-- Left beam: AI → Server -->
                <div class="absolute left-[8%] right-[54%] top-0 h-[2px] bg-border-dim/60 overflow-hidden rounded-full">
                  <div
                    class="absolute inset-y-0 left-0 w-full transition-all duration-600"
                    :class="phase >= 1 ? 'bg-gradient-to-r from-violet-500/50 to-aster/50' : ''"
                  />
                  <div
                    v-if="phase === 0"
                    class="beam-packet"
                    style="background: linear-gradient(90deg, transparent, #a78bfa, transparent);"
                  />
                </div>
                <!-- Right beam: Server → Phone -->
                <div class="absolute left-[54%] right-[8%] top-0 h-[2px] bg-border-dim/60 overflow-hidden rounded-full">
                  <div
                    class="absolute inset-y-0 left-0 w-full transition-all duration-600"
                    :class="phase >= 2 ? 'bg-gradient-to-r from-aster/50 to-green-500/50' : ''"
                  />
                  <div
                    v-if="phase === 1"
                    class="beam-packet"
                    style="background: linear-gradient(90deg, transparent, #2dd4bf, transparent);"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- Mobile: stacked nodes with arrows -->
          <div class="md:hidden space-y-3 mb-5">
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 transition-all duration-500"
                :class="phase >= 0 ? 'bg-violet-500/15 ring-1 ring-violet-500/30' : 'bg-violet-500/[0.05] ring-1 ring-violet-500/10'"
              >
                <Icon name="lucide:brain" class="text-sm text-violet-400" />
              </div>
              <div>
                <h3 class="text-xs font-semibold text-text-primary">AI Assistant</h3>
                <span class="text-[9px] font-mono text-text-tertiary">MCP Client</span>
              </div>
              <Icon name="lucide:chevron-right" class="ml-auto text-xs" :class="phase >= 1 ? 'text-violet-400' : 'text-text-tertiary/30'" />
            </div>
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 transition-all duration-500"
                :class="phase >= 1 ? 'bg-aster/15 ring-1 ring-aster/30' : 'bg-aster/[0.05] ring-1 ring-aster/10'"
              >
                <Icon name="lucide:server" class="text-sm text-aster" />
              </div>
              <div>
                <h3 class="text-xs font-semibold text-text-primary">Aster Server</h3>
                <span class="text-[9px] font-mono text-text-tertiary">Node.js &middot; WS Bridge</span>
              </div>
              <Icon name="lucide:chevron-right" class="ml-auto text-xs" :class="phase >= 2 ? 'text-aster' : 'text-text-tertiary/30'" />
            </div>
            <div class="flex items-center gap-3">
              <div
                class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 transition-all duration-500"
                :class="phase >= 2 ? 'bg-green-500/15 ring-1 ring-green-500/30' : 'bg-green-500/[0.05] ring-1 ring-green-500/10'"
              >
                <Icon name="lucide:smartphone" class="text-sm text-green-400" />
              </div>
              <div>
                <h3 class="text-xs font-semibold text-text-primary">Android Device</h3>
                <span class="text-[9px] font-mono text-text-tertiary">Kotlin &middot; Accessibility</span>
              </div>
            </div>
          </div>

          <!-- Live log / trace output -->
          <div class="rounded-xl bg-surface/60 border border-border-dim p-4 font-mono text-[11px] leading-loose min-h-[140px]">
            <Transition name="log" mode="out-in">
              <div :key="current" class="space-y-1.5">
                <!-- Step 1 -->
                <div
                  class="flex items-start gap-2 transition-all duration-500"
                  :class="phase >= 0 ? 'opacity-100' : 'opacity-0 translate-y-1'"
                >
                  <span class="text-violet-400/70 flex-shrink-0 w-14 text-right">01 ai</span>
                  <span class="text-text-tertiary/40 flex-shrink-0">&rarr;</span>
                  <span class="text-text-secondary">
                    <span class="text-violet-400/50">tool_call</span>
                    {{ activeScenario.aiCall }}
                  </span>
                  <span v-if="phase === 0" class="ml-auto text-violet-400/60 animate-pulse flex-shrink-0">...</span>
                  <Icon v-else-if="phase > 0" name="lucide:check" class="ml-auto text-[9px] text-violet-400/40 flex-shrink-0 mt-1" />
                </div>

                <!-- Step 2 -->
                <div
                  class="flex items-start gap-2 transition-all duration-500"
                  :class="phase >= 1 ? 'opacity-100' : 'opacity-0 translate-y-1'"
                >
                  <span class="text-aster/70 flex-shrink-0 w-14 text-right">02 srv</span>
                  <span class="text-text-tertiary/40 flex-shrink-0">&rarr;</span>
                  <span class="text-text-secondary">
                    <span class="text-aster/50">ws://</span>
                    {{ activeScenario.serverRoute }}
                  </span>
                  <span v-if="phase === 1" class="ml-auto text-aster/60 animate-pulse flex-shrink-0">...</span>
                  <Icon v-else-if="phase > 1" name="lucide:check" class="ml-auto text-[9px] text-aster/40 flex-shrink-0 mt-1" />
                </div>

                <!-- Step 3 -->
                <div
                  class="flex items-start gap-2 transition-all duration-500"
                  :class="phase >= 2 ? 'opacity-100' : 'opacity-0 translate-y-1'"
                >
                  <span class="text-green-400/70 flex-shrink-0 w-14 text-right">03 dev</span>
                  <span class="text-text-tertiary/40 flex-shrink-0">&rarr;</span>
                  <span class="text-text-secondary">
                    <span class="text-green-400/50">exec</span>
                    {{ activeScenario.deviceExec }}
                  </span>
                  <span v-if="phase === 2" class="ml-auto text-green-400/60 animate-pulse flex-shrink-0">...</span>
                  <Icon v-else-if="phase > 2" name="lucide:check" class="ml-auto text-[9px] text-green-400/40 flex-shrink-0 mt-1" />
                </div>

                <!-- Result -->
                <div
                  class="flex items-start gap-2 pt-2 mt-2 border-t border-border-dim transition-all duration-500"
                  :class="phase >= 3 ? 'opacity-100' : 'opacity-0'"
                >
                  <span class="text-aster flex-shrink-0 w-14 text-right">
                    <Icon name="lucide:check-circle" class="inline text-[10px]" />
                  </span>
                  <span class="text-text-tertiary/40 flex-shrink-0">&rarr;</span>
                  <span class="text-text-secondary">{{ activeScenario.result }}</span>
                </div>

                <!-- Latency badge -->
                <div
                  class="flex justify-end transition-all duration-500"
                  :class="phase >= 3 ? 'opacity-100' : 'opacity-0'"
                >
                  <span class="text-[9px] text-aster/40 font-mono">{{ activeScenario.latency }}</span>
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </div>

      <!-- Protocol strip -->
      <div class="flex flex-wrap justify-center gap-2.5 mt-10">
        <span
          v-for="proto in protocols"
          :key="proto"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/[0.02] border border-border-dim text-[10px] font-mono text-text-tertiary"
        >
          <span class="w-1 h-1 rounded-full bg-aster/40" />
          {{ proto }}
        </span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
interface Scenario {
  label: string
  icon: string
  prompt: string
  aiCall: string
  serverRoute: string
  deviceExec: string
  result: string
  latency: string
}

const scenarios: Scenario[] = [
  {
    label: 'screenshot',
    icon: 'lucide:camera',
    prompt: '"Take a screenshot of my phone"',
    aiCall: 'take_screenshot()',
    serverRoute: 'CAPTURE_SCREEN → encode PNG',
    deviceExec: 'AccessibilityService.takeScreenshot() → base64',
    result: 'Screenshot captured (1080x2400, 842KB) — returned as base64 for visual analysis.',
    latency: '~320ms',
  },
  {
    label: 'search photos',
    icon: 'lucide:image',
    prompt: '"Find all my beach photos from last December"',
    aiCall: 'search_media({ query: "beach", month: 12 })',
    serverRoute: 'SEARCH_MEDIA → filter EXIF + location',
    deviceExec: 'MediaStore.query() → geo-filter → 23 matches',
    result: '23 photos across 3 locations — Goa, Pondicherry, Kovalam. EXIF timestamps intact.',
    latency: '~480ms',
  },
  {
    label: 'vibrate',
    icon: 'lucide:vibrate',
    prompt: '"Vibrate my phone — I dropped it behind the couch"',
    aiCall: 'vibrate({ pattern: [0, 500, 200, 500] })',
    serverRoute: 'VIBRATE → custom haptic pattern',
    deviceExec: 'Vibrator.vibrate(VibrationEffect.createWaveform())',
    result: 'Device vibrating with strong repeating pulse (500ms on, 200ms pause, 500ms on).',
    latency: '~90ms',
  },
  {
    label: 'voice call',
    icon: 'lucide:phone',
    prompt: '"Call Mom and tell her I\'ll be late"',
    aiCall: 'make_call_with_voice({ number: "+919XXXXXXXXX", text: "Hi Mom, I\'ll be about 20 minutes late.", waitSeconds: 8 })',
    serverRoute: 'MAKE_CALL_WITH_VOICE → dial + speaker + TTS queue',
    deviceExec: 'Intent(ACTION_CALL) → speakerphone on → TTS.speak()',
    result: 'Called Mom, spoke message via TTS on speakerphone after 8s wait.',
    latency: '~10s',
  },
  {
    label: 'notifications',
    icon: 'lucide:bell',
    prompt: '"Any urgent notifications?"',
    aiCall: 'read_notifications({ filter: "priority" })',
    serverRoute: 'READ_NOTIFICATIONS → parse 14 active',
    deviceExec: 'NotificationListenerService → extract + rank',
    result: '2 urgent Slack messages, 1 missed call from Mom, delivery arriving 2-4 PM.',
    latency: '~150ms',
  },
  {
    label: 'AI calls you',
    icon: 'lucide:phone-outgoing',
    prompt: 'AI-initiated: flight delay detected on AI\'s phone',
    aiCall: 'make_call_with_voice({ number: "+919XXX", text: "Your DEL-BOM flight is delayed 45 min. New gate B12.", waitSeconds: 8 })',
    serverRoute: 'MAKE_CALL_WITH_VOICE → dial + enable speaker + TTS',
    deviceExec: 'Intent(ACTION_CALL) → speakerphone → TTS.speak()',
    result: 'Called owner, spoke: "Your DEL-BOM flight is delayed 45 min. New gate B12." via TTS on speakerphone.',
    latency: '~10s (8s wait + TTS)',
  },
]

const protocols = ['MCP over HTTP', 'WebSocket', 'Accessibility API', 'MediaStore', 'SQLite']

const current = ref(0)
const activeScenario = computed(() => scenarios[current.value]!)
const phase = ref(-1)
let timers: ReturnType<typeof setTimeout>[] = []

function play(index: number) {
  timers.forEach(clearTimeout)
  timers = []
  phase.value = -1
  current.value = index

  timers.push(setTimeout(() => { phase.value = 0 }, 250))
  timers.push(setTimeout(() => { phase.value = 1 }, 1400))
  timers.push(setTimeout(() => { phase.value = 2 }, 2600))
  timers.push(setTimeout(() => { phase.value = 3 }, 3800))
}

onMounted(() => {
  play(0)
})

onUnmounted(() => {
  timers.forEach(clearTimeout)
})
</script>

<style scoped>
/* Beam packet animation (horizontal) */
.beam-packet {
  position: absolute;
  top: -2px;
  width: 32px;
  height: 6px;
  border-radius: 3px;
  filter: blur(2px);
  animation: beam-move 0.9s ease-in-out forwards;
}

@keyframes beam-move {
  0% { left: -32px; opacity: 0; }
  15% { opacity: 1; }
  85% { opacity: 1; }
  100% { left: 100%; opacity: 0; }
}

/* Command text transition */
.cmd-enter-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.cmd-leave-active {
  transition: all 0.15s ease-in;
}
.cmd-enter-from {
  opacity: 0;
  transform: translateY(4px);
  filter: blur(2px);
}
.cmd-leave-to {
  opacity: 0;
  transform: translateY(-3px);
  filter: blur(2px);
}

/* Log block transition */
.log-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.log-leave-active {
  transition: all 0.15s ease-in;
}
.log-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.log-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

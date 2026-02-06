<template>
  <section class="relative py-32 px-6 overflow-hidden">
    <!-- Ambient glow -->
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full bg-aster/[0.03] blur-[100px] pointer-events-none" />

    <div class="relative max-w-6xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">See It In Action</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Like texting a super-powered assistant
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Watch a real conversation unfold. Every message triggers actual tools on the Android device &mdash; yours or the AI's own.
        </p>
      </div>

      <div class="flex flex-col lg:flex-row items-center gap-12 lg:gap-16">
        <!-- Phone frame -->
        <div class="relative flex-shrink-0 phone-wrapper">
          <div class="relative w-[270px] sm:w-[285px]">
            <!-- Ambient phone glow -->
            <div class="absolute -inset-8 rounded-[60px] bg-aster/[0.04] blur-3xl pointer-events-none" />

            <!-- Side buttons -->
            <div class="absolute -left-[2px] top-[120px] w-[3px] h-8 rounded-l-sm bg-zinc-600" />
            <div class="absolute -left-[2px] top-[170px] w-[3px] h-14 rounded-l-sm bg-zinc-600" />
            <div class="absolute -left-[2px] top-[200px] w-[3px] h-14 rounded-l-sm bg-zinc-600" />
            <div class="absolute -right-[2px] top-[155px] w-[3px] h-16 rounded-r-sm bg-zinc-600" />

            <!-- Phone body -->
            <div class="phone-bezel rounded-[36px] p-[1.5px]">
              <div class="rounded-[35px] bg-[#0c0c12] overflow-hidden ring-1 ring-white/[0.04]">
                <!-- Status bar with dynamic island -->
                <div class="relative flex items-center justify-between px-6 pt-3 pb-1">
                  <span class="text-[10px] font-semibold text-text-secondary/80 tabular-nums">9:41</span>
                  <!-- Dynamic island -->
                  <div class="absolute left-1/2 -translate-x-1/2 top-2 w-[72px] h-[20px] bg-black rounded-full" />
                  <div class="flex items-center gap-[4px]">
                    <Icon name="lucide:signal" class="text-[9px] text-text-secondary/70" />
                    <Icon name="lucide:wifi" class="text-[9px] text-text-secondary/70" />
                    <Icon name="lucide:battery-full" class="text-[9px] text-text-secondary/70" />
                  </div>
                </div>

                <!-- Chat header -->
                <div class="flex items-center gap-2.5 px-3 py-2 mx-2.5 mt-1 rounded-xl bg-surface-raised/50">
                  <div class="w-7 h-7 rounded-full bg-gradient-to-br from-aster/30 to-teal-500/20 flex items-center justify-center">
                    <Icon name="lucide:bot" class="text-[11px] text-aster" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="text-[12px] font-semibold text-text-primary tracking-tight">ClawdBot</div>
                    <div class="flex items-center gap-1.5">
                      <span class="w-1.5 h-1.5 rounded-full bg-green-400" />
                      <span class="text-[9px] text-text-tertiary">via WhatsApp</span>
                    </div>
                  </div>
                  <div class="flex items-center gap-2.5">
                    <Icon name="lucide:video" class="text-[12px] text-text-tertiary/50" />
                    <Icon name="lucide:phone" class="text-[12px] text-text-tertiary/50" />
                  </div>
                </div>

                <!-- Chat body -->
                <div ref="chatBody" class="h-[400px] overflow-y-auto px-2.5 py-3 space-y-2 scroll-smooth chat-scrollbar">
                  <TransitionGroup name="msg">
                    <template v-for="(msg, i) in visibleMessages" :key="msg.id">
                      <!-- Timestamp divider -->
                      <div v-if="msg.type === 'time'" class="flex justify-center py-1">
                        <span class="text-[9px] text-text-tertiary/60 px-3 py-0.5 rounded-full">{{ msg.text }}</span>
                      </div>

                      <!-- User bubble -->
                      <div v-else-if="msg.type === 'user'" class="flex justify-end">
                        <div class="max-w-[82%] px-2.5 py-1.5 rounded-[16px] rounded-br-[4px] bg-aster/[0.15] backdrop-blur-sm">
                          <p class="text-[11px] text-text-primary/90 leading-[1.5]">{{ msg.text }}</p>
                          <span class="text-[7px] text-text-tertiary/50 block text-right mt-0.5 tabular-nums">{{ msg.time }}</span>
                        </div>
                      </div>

                      <!-- Aster system action -->
                      <div v-else-if="msg.type === 'action'" class="flex justify-center py-0.5">
                        <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-aster/[0.05]">
                          <Icon :name="msg.icon!" class="text-[8px] text-aster/60" />
                          <span class="text-[8px] font-mono text-aster/50">{{ msg.text }}</span>
                        </div>
                      </div>

                      <!-- AI bubble -->
                      <div v-else-if="msg.type === 'ai'" class="flex justify-start">
                        <div class="max-w-[85%] px-2.5 py-1.5 rounded-[16px] rounded-bl-[4px] bg-white/[0.04]">
                          <p class="text-[11px] text-text-secondary/90 leading-[1.5] whitespace-pre-line">{{ msg.text }}</p>
                          <span class="text-[7px] text-text-tertiary/50 block text-right mt-0.5 tabular-nums">{{ msg.time }}</span>
                        </div>
                      </div>
                    </template>
                  </TransitionGroup>

                  <!-- Typing indicator -->
                  <Transition name="typing">
                    <div v-if="isTyping" class="flex justify-start">
                      <div class="px-3 py-2 rounded-[16px] rounded-bl-[4px] bg-white/[0.04]">
                        <div class="flex items-center gap-[3px]">
                          <span class="typing-dot w-[4px] h-[4px] rounded-full bg-text-tertiary/60" />
                          <span class="typing-dot w-[4px] h-[4px] rounded-full bg-text-tertiary/60" style="animation-delay: 0.15s" />
                          <span class="typing-dot w-[4px] h-[4px] rounded-full bg-text-tertiary/60" style="animation-delay: 0.3s" />
                        </div>
                      </div>
                    </div>
                  </Transition>
                </div>

                <!-- Input bar -->
                <div class="px-2.5 py-2 flex items-center gap-1.5">
                  <div class="w-6 h-6 rounded-full flex items-center justify-center bg-white/[0.04]">
                    <Icon name="lucide:plus" class="text-[10px] text-text-tertiary/60" />
                  </div>
                  <div class="flex-1 flex items-center px-3 py-[6px] rounded-full bg-white/[0.04] ring-1 ring-white/[0.04]">
                    <span class="text-[10px] text-text-tertiary/40">Message</span>
                  </div>
                  <div class="w-6 h-6 rounded-full flex items-center justify-center bg-aster/20">
                    <Icon name="lucide:mic" class="text-[10px] text-aster/80" />
                  </div>
                </div>

                <!-- Home indicator -->
                <div class="flex justify-center pb-1.5 pt-0.5">
                  <div class="w-[80px] h-[3px] rounded-full bg-white/10" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right side: conversation context -->
        <div class="flex-1 max-w-lg">
          <div class="space-y-8">
            <!-- Step indicators -->
            <TransitionGroup name="step">
              <div
                v-for="(step, i) in visibleSteps"
                :key="step.id"
                class="flex items-start gap-4"
              >
                <div class="flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center border" :class="step.active ? step.activeBg : 'bg-surface-raised border-border-dim'">
                  <Icon :name="step.icon" class="text-lg" :class="step.active ? step.activeColor : 'text-text-tertiary'" />
                </div>
                <div class="pt-0.5">
                  <h4 class="text-sm font-semibold" :class="step.active ? 'text-text-primary' : 'text-text-tertiary'">{{ step.title }}</h4>
                  <p class="text-xs mt-1 leading-relaxed" :class="step.active ? 'text-text-secondary' : 'text-text-tertiary'">{{ step.description }}</p>
                  <div v-if="step.active && step.tools" class="flex flex-wrap gap-1.5 mt-2">
                    <span
                      v-for="tool in step.tools"
                      :key="tool"
                      class="px-2 py-0.5 rounded-md text-[10px] font-mono border border-aster/10 text-aster/60 bg-aster/[0.04]"
                    >
                      {{ tool }}
                    </span>
                  </div>
                </div>
              </div>
            </TransitionGroup>
          </div>

          <!-- Replay button -->
          <button
            v-if="finished"
            class="mt-10 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl border border-border-subtle text-sm text-text-secondary hover:text-aster hover:border-aster/30 transition-all cursor-pointer"
            @click="replay"
          >
            <Icon name="lucide:rotate-ccw" class="text-sm" />
            Replay conversation
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
interface Message {
  id: number
  type: 'user' | 'ai' | 'action' | 'time'
  text: string
  time?: string
  icon?: string
}

interface Step {
  id: number
  icon: string
  title: string
  description: string
  activeBg: string
  activeColor: string
  tools?: string[]
  active: boolean
}

const chatBody = ref<HTMLElement | null>(null)
const visibleMessages = ref<Message[]>([])
const visibleSteps = ref<Step[]>([])
const isTyping = ref(false)
const finished = ref(false)

const script: { msg: Omit<Message, 'id'>; stepIndex?: number; delay: number }[] = [
  { msg: { type: 'time', text: 'Today' }, delay: 400 },
  { msg: { type: 'user', text: 'Hey, I\'m running out of storage. Can you check what\'s taking up space?', time: '9:41 AM' }, delay: 800 },
  { msg: { type: 'action', text: 'analyze_storage → scanning...', icon: 'lucide:hard-drive' }, stepIndex: 0, delay: 1200 },
  { msg: { type: 'ai', text: 'Your phone has 4.2 GB free out of 64 GB. Here\'s the breakdown:\n\nWhatsApp media — 12.3 GB\nPhotos & videos — 18.7 GB\nCached data — 5.1 GB\nDownloads — 3.8 GB', time: '9:41 AM' }, delay: 1500 },
  { msg: { type: 'user', text: 'That\'s a lot of WhatsApp media. Any duplicate photos in there?', time: '9:42 AM' }, delay: 1200 },
  { msg: { type: 'action', text: 'search_media → index_media_metadata → comparing...', icon: 'lucide:images' }, stepIndex: 1, delay: 1800 },
  { msg: { type: 'ai', text: 'Found 94 duplicate images (2.1 GB) and 23 duplicate videos (3.4 GB). Most are WhatsApp forwards saved multiple times. Want me to clean them up?', time: '9:42 AM' }, delay: 1400 },
  { msg: { type: 'user', text: 'Yes! Delete the duplicates. Keep the best quality ones.', time: '9:43 AM' }, delay: 1000 },
  { msg: { type: 'action', text: 'delete_file → removing 117 duplicates...', icon: 'lucide:trash-2' }, stepIndex: 2, delay: 1500 },
  { msg: { type: 'ai', text: 'Done! Freed up 5.5 GB. You now have 9.7 GB free. Your photos are all intact — only exact duplicates were removed.', time: '9:43 AM' }, delay: 1200 },
  { msg: { type: 'user', text: 'Perfect. Call Mom and tell her I\'ll be 20 min late for lunch.', time: '9:44 AM' }, delay: 1000 },
  { msg: { type: 'action', text: 'make_call_with_voice → dialing Mom → TTS queued...', icon: 'lucide:phone-call' }, stepIndex: 3, delay: 800 },
  { msg: { type: 'ai', text: 'Calling Mom now. Speakerphone on — I\'ll speak your message once she picks up.', time: '9:44 AM' }, delay: 800 },
]

const steps: Omit<Step, 'id' | 'active'>[] = [
  {
    icon: 'lucide:hard-drive',
    title: 'Storage analyzed',
    description: 'Scanned entire filesystem, categorized usage by app and file type.',
    activeBg: 'bg-amber-500/10 border-amber-500/20',
    activeColor: 'text-amber-400',
    tools: ['analyze_storage'],
  },
  {
    icon: 'lucide:search',
    title: 'Duplicates found',
    description: 'Indexed all media metadata, compared hashes to identify 117 duplicate files.',
    activeBg: 'bg-violet-500/10 border-violet-500/20',
    activeColor: 'text-violet-400',
    tools: ['search_media', 'index_media_metadata'],
  },
  {
    icon: 'lucide:trash-2',
    title: '5.5 GB freed',
    description: 'Safely removed exact duplicates while keeping highest quality originals.',
    activeBg: 'bg-rose-500/10 border-rose-500/20',
    activeColor: 'text-rose-400',
    tools: ['delete_file'],
  },
  {
    icon: 'lucide:phone-call',
    title: 'Voice call placed',
    description: 'Called Mom with speakerphone and spoke a message via TTS — fully hands-free.',
    activeBg: 'bg-green-500/10 border-green-500/20',
    activeColor: 'text-green-400',
    tools: ['make_call_with_voice'],
  },
]

let msgId = 0
let timeoutIds: ReturnType<typeof setTimeout>[] = []

function scrollToBottom() {
  nextTick(() => {
    if (chatBody.value) {
      chatBody.value.scrollTop = chatBody.value.scrollHeight
    }
  })
}

function runScript() {
  finished.value = false
  visibleMessages.value = []
  visibleSteps.value = []
  isTyping.value = false
  msgId = 0

  let cumulativeDelay = 500

  script.forEach((entry, i) => {
    const isAiOrAction = entry.msg.type === 'ai' || entry.msg.type === 'action'

    // Show typing before AI/action messages
    if (isAiOrAction) {
      const typingStart = cumulativeDelay
      timeoutIds.push(setTimeout(() => {
        isTyping.value = true
        scrollToBottom()
      }, typingStart))
      cumulativeDelay += entry.delay
    } else {
      cumulativeDelay += entry.delay
    }

    const showAt = cumulativeDelay

    timeoutIds.push(setTimeout(() => {
      isTyping.value = false
      visibleMessages.value.push({ ...entry.msg, id: msgId++ })
      scrollToBottom()

      if (entry.stepIndex !== undefined) {
        const step = steps[entry.stepIndex]
        // Check if already added
        if (!visibleSteps.value.find(s => s.id === entry.stepIndex)) {
          // Deactivate previous steps
          visibleSteps.value.forEach(s => s.active = false)
          visibleSteps.value.push({ ...step, id: entry.stepIndex!, active: true })
        }
      }
    }, showAt))
  })

  // Mark finished
  timeoutIds.push(setTimeout(() => {
    finished.value = true
    // Activate all steps at the end
    visibleSteps.value.forEach(s => s.active = true)
  }, cumulativeDelay + 600))
}

function replay() {
  timeoutIds.forEach(clearTimeout)
  timeoutIds = []
  runScript()
}

onMounted(() => {
  runScript()
})

onUnmounted(() => {
  timeoutIds.forEach(clearTimeout)
})
</script>

<style scoped>
/* Phone bezel — subtle titanium edge */
.phone-bezel {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.04) 30%,
    rgba(255, 255, 255, 0.02) 50%,
    rgba(255, 255, 255, 0.04) 70%,
    rgba(255, 255, 255, 0.10) 100%
  );
}

.phone-wrapper {
  filter: drop-shadow(0 20px 60px rgba(0, 0, 0, 0.5)) drop-shadow(0 4px 12px rgba(0, 0, 0, 0.3));
}

.chat-scrollbar::-webkit-scrollbar {
  width: 0px;
}
.chat-scrollbar::-webkit-scrollbar-thumb {
  background: transparent;
}

/* Typing dots */
@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}
.typing-dot {
  animation: typing-bounce 1s ease-in-out infinite;
}

/* Message enter */
.msg-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.msg-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.96);
}

/* Typing indicator */
.typing-enter-active {
  transition: all 0.2s ease-out;
}
.typing-leave-active {
  transition: all 0.15s ease-in;
}
.typing-enter-from,
.typing-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* Step enter */
.step-enter-active {
  transition: all 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
.step-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}
</style>

<template>
  <section id="live-chat" class="relative py-28 px-6 overflow-hidden">
    <!-- Ambient glow -->
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] rounded-full bg-aster/[0.03] blur-[100px] pointer-events-none" aria-hidden="true" />

    <div class="relative max-w-6xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-14">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">See it in action</span>
        <component :is="headingTag" class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Like texting a super-powered assistant
        </component>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          One conversation, start to finish. Every step in it maps to real
          <code class="font-mono text-text-primary">{{ TOOL_PREFIX }}*</code> tools running on an Android phone &mdash;
          yours, or the AI&rsquo;s own.
        </p>
        <!-- The hedge is USE_CASES_DISCLAIMER, not prose written here: the
             markdown twin emits the same string, so the warning travels with
             the invented figures instead of staying on the rendered page. -->
        <p class="mt-3 text-sm text-text-tertiary max-w-xl mx-auto">
          {{ USE_CASES_DISCLAIMER }}
        </p>
      </div>

      <div class="flex flex-col lg:flex-row items-center gap-12 lg:gap-16">
        <!-- Phone frame -->
        <div class="relative flex-shrink-0 phone-wrapper">
          <div class="relative w-[270px] sm:w-[285px]">
            <!-- Ambient phone glow -->
            <div class="absolute -inset-8 rounded-[60px] bg-aster/[0.04] blur-3xl pointer-events-none" aria-hidden="true" />

            <!-- Side buttons -->
            <div class="absolute -left-[2px] top-[120px] w-[3px] h-8 rounded-l-sm bg-zinc-600" aria-hidden="true" />
            <div class="absolute -left-[2px] top-[170px] w-[3px] h-14 rounded-l-sm bg-zinc-600" aria-hidden="true" />
            <div class="absolute -left-[2px] top-[200px] w-[3px] h-14 rounded-l-sm bg-zinc-600" aria-hidden="true" />
            <div class="absolute -right-[2px] top-[155px] w-[3px] h-16 rounded-r-sm bg-zinc-600" aria-hidden="true" />

            <!-- Phone body -->
            <div class="phone-bezel rounded-[36px] p-[1.5px]">
              <div class="rounded-[35px] bg-surface overflow-hidden ring-1 ring-white/[0.04]">
                <!-- Status bar with dynamic island -->
                <div class="relative flex items-center justify-between px-6 pt-3 pb-1" aria-hidden="true">
                  <span class="text-[10px] font-semibold text-text-secondary tabular-nums">9:41</span>
                  <div class="absolute left-1/2 -translate-x-1/2 top-2 w-[72px] h-[20px] bg-black rounded-full" />
                  <div class="flex items-center gap-[4px]">
                    <Icon name="lucide:signal" class="text-[9px] text-text-secondary" />
                    <Icon name="lucide:wifi" class="text-[9px] text-text-secondary" />
                    <Icon name="lucide:battery-full" class="text-[9px] text-text-secondary" />
                  </div>
                </div>

                <!-- Chat header -->
                <div class="flex items-center gap-2.5 px-3 py-2 mx-2.5 mt-1 rounded-xl bg-surface-raised/50">
                  <span class="w-7 h-7 rounded-full bg-aster/20 flex items-center justify-center" aria-hidden="true">
                    <Icon name="lucide:bot" class="text-[11px] text-aster" />
                  </span>
                  <div class="flex-1 min-w-0">
                    <div class="text-[12px] font-semibold text-text-primary tracking-tight">Assistant</div>
                    <div class="flex items-center gap-1.5">
                      <span class="w-1.5 h-1.5 rounded-full bg-green-400" aria-hidden="true" />
                      <span class="text-[9px] text-text-tertiary">connected over MCP</span>
                    </div>
                  </div>
                  <div class="flex items-center gap-2.5" aria-hidden="true">
                    <Icon name="lucide:video" class="text-[12px] text-text-tertiary" />
                    <Icon name="lucide:phone" class="text-[12px] text-text-tertiary" />
                  </div>
                </div>

                <!-- Chat body. Seeded with the full transcript so it is present in
                     the prerendered HTML; the replay clears and refills it. -->
                <div
                  ref="chatBody"
                  class="h-[400px] overflow-y-auto px-2.5 py-3 space-y-2 scroll-smooth chat-scrollbar"
                  role="group"
                  tabindex="0"
                  aria-label="Example conversation between a person and their AI assistant"
                >
                  <TransitionGroup name="msg">
                    <template v-for="msg in visibleMessages" :key="msg.id">
                      <!-- Timestamp divider -->
                      <div v-if="msg.type === 'time'" class="flex justify-center py-1">
                        <span class="text-[10px] text-text-tertiary px-3 py-0.5 rounded-full">{{ msg.text }}</span>
                      </div>

                      <!-- Person -->
                      <div v-else-if="msg.type === 'user'" class="flex justify-end">
                        <div class="max-w-[82%] px-2.5 py-1.5 rounded-[16px] rounded-br-[4px] bg-aster/[0.15] backdrop-blur-sm">
                          <p class="text-[11.5px] text-text-primary leading-[1.5]">
                            <span class="sr-only">You: </span>{{ msg.text }}
                          </p>
                          <span class="text-[9px] text-text-tertiary block text-right mt-0.5 tabular-nums">{{ msg.time }}</span>
                        </div>
                      </div>

                      <!-- Tool call -->
                      <div v-else-if="msg.type === 'action'" class="flex justify-center py-0.5">
                        <div class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-aster/10">
                          <Icon :name="msg.icon!" class="text-[9px] text-aster" aria-hidden="true" />
                          <span class="text-[10px] font-mono text-aster">
                            <span class="sr-only">Tools called: </span>{{ actionLabel(msg) }}
                          </span>
                        </div>
                      </div>

                      <!-- Assistant -->
                      <div v-else class="flex justify-start">
                        <div class="max-w-[85%] px-2.5 py-1.5 rounded-[16px] rounded-bl-[4px] bg-white/[0.06]">
                          <p class="text-[11.5px] text-text-secondary leading-[1.5] whitespace-pre-line">
                            <span class="sr-only">Assistant: </span>{{ msg.text }}
                          </p>
                          <span class="text-[9px] text-text-tertiary block text-right mt-0.5 tabular-nums">{{ msg.time }}</span>
                        </div>
                      </div>
                    </template>
                  </TransitionGroup>

                  <!-- Typing indicator -->
                  <Transition name="typing">
                    <div v-if="isTyping" class="flex justify-start" aria-hidden="true">
                      <div class="px-3 py-2 rounded-[16px] rounded-bl-[4px] bg-white/[0.06]">
                        <div class="flex items-center gap-[3px]">
                          <span class="typing-dot w-[4px] h-[4px] rounded-full bg-text-tertiary" />
                          <span class="typing-dot w-[4px] h-[4px] rounded-full bg-text-tertiary" style="animation-delay: 0.15s" />
                          <span class="typing-dot w-[4px] h-[4px] rounded-full bg-text-tertiary" style="animation-delay: 0.3s" />
                        </div>
                      </div>
                    </div>
                  </Transition>
                </div>

                <!-- Input bar (decorative) -->
                <div class="px-2.5 py-2 flex items-center gap-1.5" aria-hidden="true">
                  <span class="w-6 h-6 rounded-full flex items-center justify-center bg-white/[0.06]">
                    <Icon name="lucide:plus" class="text-[10px] text-text-tertiary" />
                  </span>
                  <span class="flex-1 flex items-center px-3 py-[6px] rounded-full bg-white/[0.06] ring-1 ring-white/[0.04]">
                    <span class="text-[10px] text-text-tertiary">Message</span>
                  </span>
                  <span class="w-6 h-6 rounded-full flex items-center justify-center bg-aster/20">
                    <Icon name="lucide:mic" class="text-[10px] text-aster" />
                  </span>
                </div>

                <!-- Home indicator -->
                <div class="flex justify-center pb-1.5 pt-0.5" aria-hidden="true">
                  <div class="w-[80px] h-[3px] rounded-full bg-white/10" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- What actually ran -->
        <div class="flex-1 max-w-lg w-full">
          <component :is="subHeadingTag" class="text-sm font-semibold uppercase tracking-[0.15em] text-text-tertiary mb-6">
            What ran on the device
          </component>

          <ol class="space-y-8 list-none p-0 m-0">
            <TransitionGroup name="step">
              <li v-for="step in visibleSteps" :key="step.id" class="flex items-start gap-4">
                <span
                  class="flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center border"
                  :class="step.active ? step.activeBg : 'bg-surface-raised border-border-dim'"
                  aria-hidden="true"
                >
                  <Icon :name="step.icon" class="text-lg" :class="step.active ? step.activeColor : 'text-text-tertiary'" />
                </span>
                <div class="pt-0.5">
                  <component :is="stepHeadingTag" class="text-sm font-semibold" :class="step.active ? 'text-text-primary' : 'text-text-tertiary'">
                    {{ step.title }}
                  </component>
                  <p class="text-xs mt-1 leading-relaxed" :class="step.active ? 'text-text-secondary' : 'text-text-tertiary'">
                    {{ step.description }}
                  </p>
                  <ul v-if="step.active" class="flex flex-wrap gap-1.5 mt-2 list-none p-0 m-0">
                    <li
                      v-for="tool in step.tools"
                      :key="tool"
                      class="px-2 py-0.5 rounded-md text-[10px] font-mono border border-aster/25 bg-aster/10 text-aster"
                    >
                      {{ TOOL_PREFIX }}{{ tool }}
                    </li>
                  </ul>
                </div>
              </li>
            </TransitionGroup>
          </ol>

          <!-- Playback controls. WCAG 2.2.2: anything that moves on a timer needs
               a way to stop it, and the timer must not start under reduced motion. -->
          <div class="mt-10 flex flex-wrap gap-3">
            <button
              type="button"
              class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl border border-border-subtle text-sm text-text-secondary hover:text-aster hover:border-aster/40 transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              @click="toggle"
            >
              <Icon :name="isPlaying ? 'lucide:pause' : 'lucide:play'" class="text-sm" aria-hidden="true" />
              {{ isPlaying ? 'Pause replay' : atEnd ? 'Replay conversation' : 'Resume replay' }}
            </button>
            <button
              type="button"
              class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl border border-border-dim text-sm text-text-tertiary hover:text-text-primary hover:border-border-subtle transition-colors cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              @click="showAll"
            >
              <Icon name="lucide:list" class="text-sm" aria-hidden="true" />
              Show full transcript
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { TOOL_PREFIX } from '~/data/site'
import { USE_CASES_DISCLAIMER } from '~/data/use-cases'

/**
 * PRERENDER CONTRACT
 *
 * The refs below are seeded with the COMPLETE transcript and all four outcome
 * steps at setup time, so the static HTML this site ships already contains
 * every word. The previous version seeded them empty and filled them from
 * setTimeout in onMounted, which meant the entire conversation — the strongest
 * content on the page — was absent from the prerendered markup and invisible to
 * every fetcher that does not execute JavaScript.
 *
 * The animation is now a progressive ENHANCEMENT: onMounted clears the seeded
 * state and replays it, and only when the visitor has not asked for reduced
 * motion. Under `prefers-reduced-motion: reduce` nothing is cleared, no timer
 * starts, and the seeded transcript simply stays on screen.
 *
 * Every tool name is printed with TOOL_PREFIX, because `analyze_storage` is not
 * callable — `aster_analyze_storage` is (mcp/src/mcp/tools.ts).
 */

const props = withDefaults(
  defineProps<{
    /** 1 when this section opens a page and owns its <h1>. */
    headingLevel?: 1 | 2
  }>(),
  { headingLevel: 2 },
)

const headingTag = computed<string>(() => `h${props.headingLevel}`)
const subHeadingTag = computed<string>(() => `h${props.headingLevel + 1}`)
const stepHeadingTag = computed<string>(() => `h${props.headingLevel + 2}`)

interface Message {
  id: number
  type: 'user' | 'ai' | 'action' | 'time'
  text: string
  time?: string
  icon?: string
  /** Unprefixed tool names; rendered with TOOL_PREFIX. */
  tools?: string[]
}

interface Step {
  id: number
  icon: string
  title: string
  description: string
  activeBg: string
  activeColor: string
  tools: string[]
  active: boolean
}

const SCRIPT: { msg: Omit<Message, 'id'>; stepIndex?: number; delay: number }[] = [
  { msg: { type: 'time', text: 'Today' }, delay: 400 },
  { msg: { type: 'user', text: "I'm running out of storage. Can you check what is taking up space?", time: '9:41 AM' }, delay: 900 },
  { msg: { type: 'action', tools: ['analyze_storage'], text: 'scanning', icon: 'lucide:hard-drive' }, stepIndex: 0, delay: 1200 },
  { msg: { type: 'ai', text: 'You have 4.2 GB free of 64 GB. The breakdown:\n\nWhatsApp media — 12.3 GB\nPhotos and videos — 18.7 GB\nCached data — 5.1 GB\nDownloads — 3.8 GB', time: '9:41 AM' }, delay: 1500 },
  { msg: { type: 'user', text: 'That is a lot of WhatsApp media. Any duplicates in there?', time: '9:42 AM' }, delay: 1200 },
  { msg: { type: 'action', tools: ['search_media', 'index_media_metadata'], text: 'comparing', icon: 'lucide:images' }, stepIndex: 1, delay: 1800 },
  { msg: { type: 'ai', text: '94 duplicate images (2.1 GB) and 23 duplicate videos (3.4 GB). Most are forwards saved more than once. Want them cleaned up?', time: '9:42 AM' }, delay: 1400 },
  { msg: { type: 'user', text: 'Yes. Delete the duplicates and keep the best copy of each.', time: '9:43 AM' }, delay: 1000 },
  { msg: { type: 'action', tools: ['delete_file'], text: 'removing 117 files', icon: 'lucide:trash-2' }, stepIndex: 2, delay: 1500 },
  { msg: { type: 'ai', text: 'Done. 5.5 GB freed, 9.7 GB now available. Only exact duplicates went; every original is still there.', time: '9:43 AM' }, delay: 1200 },
  { msg: { type: 'user', text: 'Perfect. Call Mum and tell her I will be twenty minutes late for lunch.', time: '9:44 AM' }, delay: 1000 },
  { msg: { type: 'action', tools: ['search_contacts', 'make_call_with_voice'], text: 'dialling', icon: 'lucide:phone-call' }, stepIndex: 3, delay: 900 },
  { msg: { type: 'ai', text: 'Calling now with the speaker on. I will say your message as soon as she picks up.', time: '9:44 AM' }, delay: 900 },
]

const STEPS: Omit<Step, 'id' | 'active'>[] = [
  {
    icon: 'lucide:hard-drive',
    title: 'Storage measured',
    description: 'Walked the filesystem and grouped usage by app and file type.',
    activeBg: 'bg-amber-500/10 border-amber-500/25',
    activeColor: 'text-amber-400',
    tools: ['analyze_storage'],
  },
  {
    icon: 'lucide:search',
    title: 'Duplicates identified',
    description: 'Indexed media metadata and compared it to pick out repeated files.',
    activeBg: 'bg-violet-500/10 border-violet-500/25',
    activeColor: 'text-violet-400',
    tools: ['search_media', 'index_media_metadata'],
  },
  {
    icon: 'lucide:trash-2',
    title: 'Copies removed on confirmation',
    description: 'Deleted only the extra copies, after the person said yes. Originals untouched.',
    activeBg: 'bg-rose-500/10 border-rose-500/25',
    activeColor: 'text-rose-400',
    tools: ['delete_file'],
  },
  {
    icon: 'lucide:phone-call',
    title: 'Call placed and spoken',
    description: 'Dialled the contact, turned on the speaker and read the message aloud through the phone.',
    activeBg: 'bg-green-500/10 border-green-500/25',
    activeColor: 'text-green-400',
    tools: ['search_contacts', 'make_call_with_voice'],
  },
]

/** The full transcript, ready to render on the server. */
const seededMessages = (): Message[] => SCRIPT.map((entry, i) => ({ ...entry.msg, id: i }))
const seededSteps = (): Step[] => STEPS.map((step, i) => ({ ...step, id: i, active: true }))

const chatBody = ref<HTMLElement | null>(null)
const visibleMessages = ref<Message[]>(seededMessages())
const visibleSteps = ref<Step[]>(seededSteps())
const isTyping = ref(false)
const isPlaying = ref(false)
const cursor = ref(SCRIPT.length)

const atEnd = computed(() => cursor.value >= SCRIPT.length)

function actionLabel(msg: Message): string {
  const names = (msg.tools ?? []).map((t) => `${TOOL_PREFIX}${t}`).join(' → ')
  return msg.text ? `${names} → ${msg.text}` : names
}

let timer: ReturnType<typeof setTimeout> | null = null

function clearTimer() {
  if (timer !== null) {
    clearTimeout(timer)
    timer = null
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight
  })
}

function step() {
  if (!isPlaying.value) return

  const entry = SCRIPT[cursor.value]
  if (!entry) {
    isPlaying.value = false
    isTyping.value = false
    visibleSteps.value.forEach((s) => { s.active = true })
    return
  }

  if (entry.msg.type === 'ai' || entry.msg.type === 'action') {
    isTyping.value = true
    scrollToBottom()
  }

  const index = cursor.value
  timer = setTimeout(() => {
    isTyping.value = false
    visibleMessages.value.push({ ...entry.msg, id: index })

    if (entry.stepIndex !== undefined && !visibleSteps.value.some((s) => s.id === entry.stepIndex)) {
      const source = STEPS[entry.stepIndex]
      if (source) {
        visibleSteps.value.forEach((s) => { s.active = false })
        visibleSteps.value.push({ ...source, id: entry.stepIndex, active: true })
      }
    }

    scrollToBottom()
    cursor.value = index + 1
    step()
  }, entry.delay)
}

/** Jump straight to the finished state. Also the non-JS and reduced-motion state. */
function showAll() {
  clearTimer()
  isPlaying.value = false
  isTyping.value = false
  visibleMessages.value = seededMessages()
  visibleSteps.value = seededSteps()
  cursor.value = SCRIPT.length
  scrollToBottom()
}

function replay() {
  clearTimer()
  visibleMessages.value = []
  visibleSteps.value = []
  isTyping.value = false
  cursor.value = 0
  isPlaying.value = true
  step()
}

function toggle() {
  if (isPlaying.value) {
    clearTimer()
    isPlaying.value = false
    isTyping.value = false
    return
  }
  if (atEnd.value) {
    replay()
    return
  }
  isPlaying.value = true
  step()
}

function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined'
    && typeof window.matchMedia === 'function'
    && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

onMounted(() => {
  // Reduced motion: leave the seeded transcript exactly as the server rendered
  // it and start no timer at all. The controls still work if the visitor wants
  // to watch the replay on purpose.
  if (prefersReducedMotion()) return
  replay()
})

onUnmounted(clearTimer)
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
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
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

@media (prefers-reduced-motion: reduce) {
  .typing-dot {
    animation: none;
  }
  .msg-enter-active,
  .typing-enter-active,
  .typing-leave-active,
  .step-enter-active {
    transition: none;
  }
  .msg-enter-from,
  .typing-enter-from,
  .typing-leave-to,
  .step-enter-from {
    opacity: 1;
    transform: none;
  }
}
</style>

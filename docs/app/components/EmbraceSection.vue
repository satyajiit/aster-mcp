<template>
  <section id="embrace" class="relative py-32 px-6 overflow-hidden">
    <!-- Background: warm radial glow -->
    <div class="absolute inset-0">
      <div class="absolute inset-0 bg-gradient-to-b from-surface via-[#0d0a12] to-surface" />
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] rounded-full bg-[radial-gradient(circle,_rgba(168,85,247,0.06)_0%,_rgba(45,212,191,0.03)_40%,_transparent_70%)] blur-[40px]" />
    </div>

    <div class="relative max-w-5xl mx-auto">
      <!-- Section intro -->
      <div class="text-center mb-20 observe-fade" data-delay="0">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-violet-400 mb-4 block font-mono">The Missing Piece</span>
        <h2 class="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight leading-[1.15]">
          <span class="text-text-primary">You gave your AI a voice.</span>
          <br />
          <span class="bg-gradient-to-r from-violet-400 via-aster-light to-aster bg-clip-text text-transparent">Now give it hands.</span>
        </h2>
        <p class="mt-6 text-text-secondary max-w-2xl mx-auto text-lg leading-relaxed">
          Your AI assistant already reads, writes, and thinks for you. But it's been stuck behind a screen
          &mdash; unable to touch the one device that runs your life.
        </p>
      </div>

      <!-- The Journey: Before → After -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-20">

        <!-- BEFORE card -->
        <div class="observe-fade group relative rounded-2xl border border-border-dim bg-surface-raised/50 backdrop-blur-sm p-8 transition-all duration-500 hover:border-rose-500/20" data-delay="100">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/15 flex items-center justify-center">
              <Icon name="lucide:message-circle" class="text-rose-400 text-lg" />
            </div>
            <div>
              <span class="text-[10px] font-mono uppercase tracking-[0.15em] text-rose-400/70">Today</span>
              <h3 class="text-text-primary font-semibold text-sm">Your AI can talk</h3>
            </div>
          </div>

          <div class="space-y-3.5">
            <div v-for="(item, i) in beforeItems" :key="i" class="flex items-start gap-3">
              <div class="flex-shrink-0 w-5 h-5 rounded-md bg-rose-500/8 border border-rose-500/10 flex items-center justify-center mt-0.5">
                <Icon :name="item.icon" class="text-[11px] text-rose-400/60" />
              </div>
              <div>
                <span class="text-[13px] text-text-primary font-medium block leading-snug">{{ item.label }}</span>
                <span class="text-[11px] text-text-tertiary leading-snug">{{ item.sub }}</span>
              </div>
            </div>
          </div>

          <div class="mt-6 pt-5 border-t border-border-dim">
            <p class="text-[13px] text-text-tertiary italic leading-relaxed">
              "Hey AI, set an alarm for 6 AM" &mdash; <span class="text-rose-400/60">and then you pick up the phone yourself.</span>
            </p>
          </div>
        </div>

        <!-- AFTER card -->
        <div class="observe-fade group relative rounded-2xl border border-aster/15 bg-surface-raised/50 backdrop-blur-sm p-8 transition-all duration-500 hover:border-aster/30 hover:shadow-[0_0_60px_rgba(45,212,191,0.06)]" data-delay="200">
          <!-- Subtle glow behind this card -->
          <div class="absolute -inset-px rounded-2xl bg-gradient-to-br from-aster/[0.04] to-violet-500/[0.02] pointer-events-none" />

          <div class="relative">
            <div class="flex items-center gap-3 mb-6">
              <div class="w-10 h-10 rounded-xl bg-aster/10 border border-aster/15 flex items-center justify-center">
                <Icon name="lucide:hand" class="text-aster text-lg" />
              </div>
              <div>
                <span class="text-[10px] font-mono uppercase tracking-[0.15em] text-aster/70">With Aster</span>
                <h3 class="text-text-primary font-semibold text-sm">Your AI can act</h3>
              </div>
            </div>

            <div class="space-y-3.5">
              <div v-for="(item, i) in afterItems" :key="i" class="flex items-start gap-3">
                <div class="flex-shrink-0 w-5 h-5 rounded-md bg-aster/8 border border-aster/10 flex items-center justify-center mt-0.5">
                  <Icon :name="item.icon" class="text-[11px] text-aster/80" />
                </div>
                <div>
                  <span class="text-[13px] text-text-primary font-medium block leading-snug">{{ item.label }}</span>
                  <span class="text-[11px] text-text-tertiary leading-snug">{{ item.sub }}</span>
                </div>
              </div>
            </div>

            <div class="mt-6 pt-5 border-t border-aster/10">
              <p class="text-[13px] text-text-secondary italic leading-relaxed">
                "Hey AI, set an alarm for 6 AM" &mdash; <span class="text-aster">and it just does it.</span>
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- The Emotional Punch -->
      <div class="observe-fade text-center mb-20" data-delay="100">
        <div class="relative max-w-3xl mx-auto">
          <div class="absolute -inset-8 bg-gradient-to-r from-violet-500/[0.03] via-aster/[0.05] to-violet-500/[0.03] rounded-3xl blur-xl" />
          <blockquote class="relative text-xl sm:text-2xl font-medium text-text-primary leading-relaxed tracking-tight">
            "You already trust AI with your emails, your calendar, your code.
            <br class="hidden sm:block" />
            <span class="bg-gradient-to-r from-violet-400 to-aster bg-clip-text text-transparent">Aster lets it touch your phone &mdash; or better yet, its own.</span>"
          </blockquote>
        </div>
      </div>

      <!-- THE BIG IDEA: AI's Own Phone -->
      <div class="observe-fade mb-20" data-delay="0">
        <div class="relative rounded-2xl border border-amber-500/15 bg-surface-raised/50 backdrop-blur-sm overflow-hidden">
          <!-- Warm ambient glow -->
          <div class="absolute -top-20 -right-20 w-[300px] h-[300px] rounded-full bg-[radial-gradient(circle,_rgba(251,191,36,0.06)_0%,_transparent_70%)] pointer-events-none" />
          <div class="absolute -bottom-20 -left-20 w-[250px] h-[250px] rounded-full bg-[radial-gradient(circle,_rgba(45,212,191,0.05)_0%,_transparent_70%)] pointer-events-none" />

          <div class="relative grid grid-cols-1 lg:grid-cols-5 gap-0">
            <!-- Left: Phone mockup with incoming call -->
            <div class="lg:col-span-2 flex items-center justify-center p-8 lg:p-10">
              <div class="relative w-[220px]">
                <!-- Phone frame -->
                <div class="relative rounded-[28px] border border-border-subtle bg-[#0c0c14] p-3 shadow-[0_0_80px_rgba(251,191,36,0.08)]">
                  <!-- Notch -->
                  <div class="absolute top-0 left-1/2 -translate-x-1/2 w-20 h-5 bg-[#0c0c14] rounded-b-2xl z-10" />
                  <!-- Screen -->
                  <div class="rounded-[18px] bg-gradient-to-b from-[#0f0f1a] to-[#0a0a12] overflow-hidden">
                    <!-- Status bar -->
                    <div class="flex items-center justify-between px-5 pt-6 pb-2">
                      <span class="text-[9px] text-text-tertiary font-mono">9:41</span>
                      <div class="flex items-center gap-1">
                        <div class="w-3 h-1.5 rounded-sm border border-text-tertiary/40">
                          <div class="w-2 h-full rounded-sm bg-green-400/60" />
                        </div>
                      </div>
                    </div>

                    <!-- Incoming call UI -->
                    <div class="px-5 pt-6 pb-8 text-center">
                      <!-- Pulsing avatar -->
                      <div class="relative mx-auto w-16 h-16 mb-4">
                        <div class="absolute inset-0 rounded-full bg-amber-500/20 call-pulse" />
                        <div class="absolute inset-1 rounded-full bg-amber-500/15 call-pulse-delayed" />
                        <div class="relative w-16 h-16 rounded-full bg-gradient-to-br from-amber-500/30 to-orange-500/30 border border-amber-500/25 flex items-center justify-center">
                          <Icon name="lucide:bot" class="text-2xl text-amber-400" />
                        </div>
                      </div>

                      <span class="text-[10px] text-text-tertiary font-mono uppercase tracking-wider block mb-1">incoming call</span>
                      <span class="text-text-primary font-semibold text-base block mb-0.5">OpenClaw</span>
                      <span class="text-[11px] text-text-tertiary">+91 98XXX XXXXX</span>

                      <!-- Reminder text -->
                      <div class="mt-4 mx-auto max-w-[160px] rounded-lg bg-amber-500/[0.08] border border-amber-500/10 px-3 py-2">
                        <p class="text-[10px] text-amber-400/80 leading-relaxed">
                          "Your flight to Delhi boards in 90 min. Leave now to make it."
                        </p>
                      </div>

                      <!-- Call buttons -->
                      <div class="flex items-center justify-center gap-8 mt-6">
                        <div class="flex flex-col items-center gap-1.5">
                          <div class="w-11 h-11 rounded-full bg-rose-500/20 border border-rose-500/20 flex items-center justify-center">
                            <Icon name="lucide:phone-off" class="text-rose-400 text-sm" />
                          </div>
                          <span class="text-[8px] text-text-tertiary">Decline</span>
                        </div>
                        <div class="flex flex-col items-center gap-1.5">
                          <div class="w-11 h-11 rounded-full bg-green-500/20 border border-green-500/20 flex items-center justify-center call-btn-glow">
                            <Icon name="lucide:phone" class="text-green-400 text-sm" />
                          </div>
                          <span class="text-[8px] text-text-tertiary">Accept</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Right: The pitch -->
            <div class="lg:col-span-3 p-8 lg:p-10 lg:pl-4 flex flex-col justify-center">
              <span class="text-[10px] font-mono uppercase tracking-[0.15em] text-amber-400/70 mb-3 block">Next Level</span>
              <h3 class="text-2xl sm:text-3xl font-bold tracking-tight text-text-primary leading-snug mb-4">
                Give your AI<br />
                <span class="bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">its own phone.</span>
              </h3>
              <p class="text-[15px] text-text-secondary leading-relaxed mb-6 max-w-md">
                Plug an old Android into a charger. Install Aster. Connect it to your AI.
                Now your assistant has its own number, its own device &mdash; and it can <span class="text-text-primary font-medium">call you</span>.
              </p>

              <div class="space-y-3">
                <div v-for="(item, i) in ownPhoneItems" :key="i" class="flex items-start gap-3">
                  <div class="flex-shrink-0 w-5 h-5 rounded-md bg-amber-500/8 border border-amber-500/10 flex items-center justify-center mt-0.5">
                    <Icon :name="item.icon" class="text-[11px] text-amber-400/70" />
                  </div>
                  <div>
                    <span class="text-[13px] text-text-primary font-medium leading-snug">{{ item.label }}</span>
                    <span class="text-[11px] text-text-tertiary block leading-snug">{{ item.sub }}</span>
                  </div>
                </div>
              </div>

              <div class="mt-6 pt-5 border-t border-amber-500/10">
                <p class="text-[12px] text-text-tertiary leading-relaxed">
                  You gave it WhatsApp. You gave it Telegram. You gave it your email.
                  <br />
                  <span class="text-amber-400/80 font-medium">Now give it a phone. Let it call you back.</span>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Use case vignettes -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="(vignette, i) in vignettes"
          :key="i"
          class="observe-fade group relative rounded-2xl border border-border-dim bg-surface-raised/40 p-6 transition-all duration-500 hover:border-border-subtle hover:bg-surface-raised/70"
          :data-delay="100 + i * 80"
        >
          <div class="flex items-center gap-2.5 mb-4">
            <span class="text-xl" role="img" :aria-label="vignette.emojiLabel">{{ vignette.emoji }}</span>
            <span class="text-[10px] font-mono uppercase tracking-[0.15em]" :class="vignette.labelColor">{{ vignette.label }}</span>
          </div>
          <p class="text-[13px] text-text-primary font-medium leading-relaxed mb-2">{{ vignette.scenario }}</p>
          <p class="text-[11px] text-text-tertiary leading-relaxed">{{ vignette.detail }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const beforeItems = [
  { icon: 'lucide:message-square', label: 'Chat on WhatsApp & Telegram', sub: 'It reads and replies to your messages' },
  { icon: 'lucide:mail', label: 'Draft emails & documents', sub: 'It writes content on your behalf' },
  { icon: 'lucide:calendar', label: 'Schedule & organize', sub: 'It manages your time and plans' },
  { icon: 'lucide:code', label: 'Write & review code', sub: 'It builds software alongside you' },
]

const afterItems = [
  { icon: 'lucide:smartphone', label: 'Tap, swipe, navigate your phone', sub: 'Full screen control via UI automation' },
  { icon: 'lucide:folder-open', label: 'Manage files & clear storage', sub: 'Read, write, delete — no hand-holding' },
  { icon: 'lucide:image', label: 'Search & organize your photos', sub: '"Sunset pics from last vacation" — found.' },
  { icon: 'lucide:bell', label: 'Read notifications, send SMS', sub: 'Never miss what matters again' },
]

const ownPhoneItems = [
  { icon: 'lucide:phone-outgoing', label: 'It calls and speaks to you', sub: 'make_call_with_voice — dials you, enables speaker, and delivers a TTS message.' },
  { icon: 'lucide:message-square-plus', label: 'It texts you updates', sub: 'send_sms — critical alerts straight to your number, even when you\'re offline.' },
  { icon: 'lucide:eye', label: 'It monitors things for you', sub: 'Price drops, delivery updates, app notifications — handled.' },
  { icon: 'lucide:bot', label: 'It runs tasks in the background', sub: 'Backup photos, clean storage, organize files — on its own time.' },
]

const vignettes = [
  {
    emoji: '🛏️',
    emojiLabel: 'bed',
    label: 'Lazy morning',
    labelColor: 'text-amber-400/70',
    scenario: '"Turn off my alarm and read me any urgent messages."',
    detail: 'Your AI silences the alarm, scans notifications, and gives you a voice briefing — all before you open your eyes.',
  },
  {
    emoji: '🔍',
    emojiLabel: 'search',
    label: 'Lost phone',
    labelColor: 'text-rose-400/70',
    scenario: '"My phone is somewhere in this room — make it vibrate."',
    detail: 'Triggers a strong vibration pattern instantly. No need to find another phone to call yourself.',
  },
  {
    emoji: '✈️',
    emojiLabel: 'airplane',
    label: 'Travel prep',
    labelColor: 'text-sky-400/70',
    scenario: '"Screenshot my boarding pass and save it to Downloads."',
    detail: 'Opens your email, finds the pass, takes a screenshot, and files it — ready for offline access.',
  },
  {
    emoji: '👨‍👩‍👧',
    emojiLabel: 'family',
    label: 'Parental peace',
    labelColor: 'text-violet-400/70',
    scenario: '"What apps did my kid use today and for how long?"',
    detail: 'Reads app usage stats and gives you a clear breakdown. No snooping apps needed.',
  },
  {
    emoji: '📸',
    emojiLabel: 'camera',
    label: 'Memory lane',
    labelColor: 'text-aster/70',
    scenario: '"Find all photos with Mom from this year."',
    detail: 'Searches by date, location, and content. Returns a curated gallery from your camera roll.',
  },
  {
    emoji: '🧹',
    emojiLabel: 'broom',
    label: 'Digital cleanup',
    labelColor: 'text-green-400/70',
    scenario: '"My storage is full. Clean up whatever is safe to delete."',
    detail: 'Analyzes storage, identifies junk — cached data, duplicate photos, old APKs — and clears it with your approval.',
  },
]

// Scroll-triggered fade-in using Intersection Observer
onMounted(() => {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const el = entry.target as HTMLElement
          const delay = parseInt(el.dataset.delay || '0', 10)
          setTimeout(() => {
            el.classList.add('is-visible')
          }, delay)
          observer.unobserve(el)
        }
      })
    },
    { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
  )

  document.querySelectorAll('.observe-fade').forEach((el) => {
    observer.observe(el)
  })
})
</script>

<style scoped>
.observe-fade {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
              transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

.observe-fade.is-visible {
  opacity: 1;
  transform: translateY(0);
}

/* Incoming call pulse rings */
@keyframes call-pulse {
  0% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(1.35); opacity: 0; }
  100% { transform: scale(1); opacity: 0; }
}

.call-pulse {
  animation: call-pulse 2s ease-out infinite;
}

.call-pulse-delayed {
  animation: call-pulse 2s ease-out infinite;
  animation-delay: 0.4s;
}

/* Green button subtle glow */
.call-btn-glow {
  box-shadow: 0 0 20px rgba(34, 197, 94, 0.15);
}
</style>

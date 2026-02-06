<template>
  <section id="proactive" class="relative py-32 px-6 overflow-hidden">
    <!-- Background atmosphere -->
    <div class="absolute inset-0">
      <div class="absolute inset-0 bg-gradient-to-b from-surface via-[#0c0811] to-surface" />
      <div class="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[900px] h-[700px] rounded-full bg-[radial-gradient(circle,_rgba(232,121,249,0.05)_0%,_rgba(45,212,191,0.03)_40%,_transparent_70%)] blur-[60px]" />
    </div>

    <div class="relative max-w-6xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-16 observe-fade" data-delay="0">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-fuchsia-400 mb-4 block font-mono">Proactive Intelligence</span>
        <h2 class="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight leading-[1.15]">
          <span class="text-text-primary">Your phone talks to your AI</span>
          <br />
          <span class="bg-gradient-to-r from-fuchsia-400 via-pink-400 to-aster bg-clip-text text-transparent">before you even ask.</span>
        </h2>
        <p class="mt-6 text-text-secondary max-w-2xl mx-auto text-lg leading-relaxed">
          Aster pushes real-time events &mdash; SMS, notifications, device status &mdash; straight to your AI via webhooks.
          No polling, no delays. Your AI reacts the moment something happens.
        </p>
      </div>

      <!-- Event flow visualizer -->
      <div class="observe-fade mb-20" data-delay="100">
        <div class="relative rounded-2xl border border-fuchsia-500/15 bg-surface-raised/50 backdrop-blur-sm overflow-hidden">
          <!-- Ambient glows -->
          <div class="absolute -top-24 -left-24 w-[300px] h-[300px] rounded-full bg-[radial-gradient(circle,_rgba(232,121,249,0.06)_0%,_transparent_70%)] pointer-events-none" />
          <div class="absolute -bottom-24 -right-24 w-[300px] h-[300px] rounded-full bg-[radial-gradient(circle,_rgba(45,212,191,0.05)_0%,_transparent_70%)] pointer-events-none" />

          <div class="relative p-8 lg:p-10">
            <!-- Flow diagram -->
            <div class="flex flex-col lg:flex-row items-center justify-between gap-6 lg:gap-4 mb-10">
              <!-- Phone node -->
              <div class="flex-1 max-w-[220px]">
                <div class="relative rounded-xl border border-fuchsia-500/20 bg-surface/80 p-5 text-center">
                  <div class="w-12 h-12 rounded-xl bg-fuchsia-500/10 border border-fuchsia-500/15 flex items-center justify-center mx-auto mb-3">
                    <Icon name="lucide:smartphone" class="text-xl text-fuchsia-400" />
                  </div>
                  <span class="text-[10px] font-mono uppercase tracking-[0.15em] text-fuchsia-400/70 block mb-1">Phone</span>
                  <span class="text-xs text-text-secondary">SMS, notification, or device event fires</span>
                  <!-- Pulse dot -->
                  <div class="absolute -top-1.5 -right-1.5">
                    <span class="relative flex h-3 w-3">
                      <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-fuchsia-400 opacity-75" />
                      <span class="relative inline-flex rounded-full h-3 w-3 bg-fuchsia-400" />
                    </span>
                  </div>
                </div>
              </div>

              <!-- Arrow -->
              <div class="hidden lg:flex items-center">
                <div class="w-16 h-px bg-gradient-to-r from-fuchsia-500/40 to-aster/40" />
                <Icon name="lucide:chevron-right" class="text-aster/60 -ml-1" />
              </div>
              <div class="lg:hidden flex items-center">
                <Icon name="lucide:chevron-down" class="text-aster/60" />
              </div>

              <!-- Aster server node -->
              <div class="flex-1 max-w-[220px]">
                <div class="relative rounded-xl border border-aster/20 bg-surface/80 p-5 text-center">
                  <div class="w-12 h-12 rounded-xl bg-aster/10 border border-aster/15 flex items-center justify-center mx-auto mb-3">
                    <Icon name="lucide:server" class="text-xl text-aster" />
                  </div>
                  <span class="text-[10px] font-mono uppercase tracking-[0.15em] text-aster/70 block mb-1">Aster Server</span>
                  <span class="text-xs text-text-secondary">Receives via WebSocket, forwards instantly</span>
                </div>
              </div>

              <!-- Arrow -->
              <div class="hidden lg:flex items-center">
                <div class="w-16 h-px bg-gradient-to-r from-aster/40 to-amber-400/40" />
                <Icon name="lucide:chevron-right" class="text-amber-400/60 -ml-1" />
              </div>
              <div class="lg:hidden flex items-center">
                <Icon name="lucide:chevron-down" class="text-amber-400/60" />
              </div>

              <!-- AI agent node -->
              <div class="flex-1 max-w-[220px]">
                <div class="relative rounded-xl border border-amber-500/20 bg-surface/80 p-5 text-center">
                  <div class="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/15 flex items-center justify-center mx-auto mb-3">
                    <Icon name="lucide:bot" class="text-xl text-amber-400" />
                  </div>
                  <span class="text-[10px] font-mono uppercase tracking-[0.15em] text-amber-400/70 block mb-1">Your AI</span>
                  <span class="text-xs text-text-secondary">Reacts, decides, and acts &mdash; instantly</span>
                </div>
              </div>
            </div>

            <!-- Live event feed -->
            <div class="rounded-xl border border-border-dim bg-surface/60 overflow-hidden">
              <div class="flex items-center gap-2 px-4 py-2.5 border-b border-border-dim bg-surface-raised/40">
                <span class="relative flex h-1.5 w-1.5">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
                  <span class="relative inline-flex rounded-full h-1.5 w-1.5 bg-green-400" />
                </span>
                <span class="text-[10px] font-mono text-text-tertiary tracking-wider uppercase">Live Event Stream</span>
              </div>

              <div class="divide-y divide-border-dim">
                <div
                  v-for="(event, i) in liveEvents"
                  :key="i"
                  class="flex items-center gap-4 px-4 py-3 transition-colors hover:bg-surface-raised/30"
                >
                  <div class="flex-shrink-0 w-7 h-7 rounded-lg flex items-center justify-center" :class="event.iconBg">
                    <Icon :name="event.icon" class="text-xs" :class="event.iconColor" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2">
                      <span class="text-[10px] font-mono px-1.5 py-0.5 rounded border" :class="event.badgeClass">{{ event.type }}</span>
                      <span class="text-[11px] text-text-tertiary font-mono">{{ event.time }}</span>
                    </div>
                    <p class="text-xs text-text-secondary mt-0.5 truncate">{{ event.message }}</p>
                  </div>
                  <div class="hidden sm:flex items-center gap-1.5">
                    <Icon name="lucide:arrow-right" class="text-[10px] text-text-tertiary" />
                    <span class="text-[10px] font-mono text-aster/60">{{ event.action }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Integration callout -->
      <div class="observe-fade mb-8" data-delay="150">
        <div class="relative rounded-2xl border border-aster/15 bg-surface-raised/50 backdrop-blur-sm overflow-hidden">
          <div class="absolute -inset-px rounded-2xl bg-gradient-to-br from-aster/[0.03] to-fuchsia-500/[0.02] pointer-events-none" />
          <div class="relative p-6 sm:p-8">
            <div class="flex flex-col lg:flex-row lg:items-center gap-6">
              <!-- Left: icons + text -->
              <div class="flex-1">
                <div class="flex items-center gap-3 mb-3">
                  <div class="flex -space-x-2">
                    <div class="w-8 h-8 rounded-lg bg-sky-500/10 border border-sky-500/15 flex items-center justify-center">
                      <Icon name="lucide:bot" class="text-sm text-sky-400" />
                    </div>
                    <div class="w-8 h-8 rounded-lg bg-violet-500/10 border border-violet-500/15 flex items-center justify-center">
                      <Icon name="lucide:bot" class="text-sm text-violet-400" />
                    </div>
                    <div class="w-8 h-8 rounded-lg bg-rose-500/10 border border-rose-500/15 flex items-center justify-center">
                      <Icon name="lucide:bot" class="text-sm text-rose-400" />
                    </div>
                  </div>
                  <span class="text-[10px] font-mono uppercase tracking-[0.15em] text-aster/70">Drop-in Support</span>
                </div>
                <h3 class="text-lg font-semibold text-text-primary mb-2">
                  Works out of the box with <span class="text-sky-400">OpenClaw</span>, <span class="text-violet-400">ClawdBot</span> &amp; <span class="text-rose-400">MoltBot</span>
                </h3>
                <p class="text-sm text-text-secondary leading-relaxed max-w-xl">
                  Enable proactive event forwarding in one command. Your AI agent starts receiving real-time phone events via webhook &mdash; no extra setup, no plugins.
                </p>
              </div>

              <!-- Right: CLI command -->
              <div class="flex-shrink-0 lg:min-w-[320px]">
                <div class="rounded-xl border border-border-dim bg-surface/80 overflow-hidden">
                  <div class="flex items-center gap-2 px-4 py-2 border-b border-border-dim bg-surface-raised/40">
                    <div class="flex gap-1">
                      <span class="w-2 h-2 rounded-full bg-rose-500/50" />
                      <span class="w-2 h-2 rounded-full bg-amber-500/50" />
                      <span class="w-2 h-2 rounded-full bg-green-500/50" />
                    </div>
                    <span class="text-[9px] font-mono text-text-tertiary tracking-wider uppercase">Terminal</span>
                  </div>
                  <div class="px-4 py-3 font-mono text-[13px]">
                    <div class="flex items-center gap-2">
                      <span class="text-aster select-none">$</span>
                      <span class="text-text-primary">aster set-openclaw-callbacks</span>
                    </div>
                    <div class="mt-1.5 text-text-tertiary text-[11px] leading-relaxed">
                      <span class="text-green-400/70">&#10003;</span> Token loaded from ~/.openclaw/openclaw.json<br />
                      <span class="text-green-400/70">&#10003;</span> Webhook connected &mdash; events will forward automatically
                    </div>
                  </div>
                </div>
                <p class="mt-2 text-[11px] text-text-tertiary text-center">
                  Or configure from the dashboard at <span class="text-aster/70 font-mono">/settings/openclaw</span>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Scenario showcase cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="(scenario, i) in scenarios"
          :key="i"
          class="observe-fade group relative rounded-2xl border border-border-dim bg-surface-raised/40 overflow-hidden transition-all duration-500 hover:border-border-subtle hover:bg-surface-raised/70"
          :data-delay="100 + i * 80"
        >
          <!-- Top accent gradient -->
          <div class="absolute top-0 inset-x-0 h-px bg-gradient-to-r opacity-0 group-hover:opacity-100 transition-opacity duration-500" :class="scenario.accent" />

          <div class="p-6">
            <!-- Category pill -->
            <div class="flex items-center gap-2 mb-4">
              <div class="w-6 h-6 rounded-md flex items-center justify-center" :class="scenario.iconBg">
                <Icon :name="scenario.icon" class="text-[11px]" :class="scenario.iconColor" />
              </div>
              <span class="text-[10px] font-mono uppercase tracking-[0.12em]" :class="scenario.labelColor">{{ scenario.label }}</span>
            </div>

            <!-- Scenario title -->
            <h3 class="text-[15px] font-semibold text-text-primary mb-2 leading-snug">{{ scenario.title }}</h3>

            <!-- Description -->
            <p class="text-xs text-text-secondary leading-relaxed mb-4">{{ scenario.description }}</p>

            <!-- The flow -->
            <div class="space-y-2">
              <div
                v-for="(step, si) in scenario.steps"
                :key="si"
                class="flex items-start gap-2"
              >
                <div class="flex-shrink-0 mt-0.5 w-4 h-4 rounded-full border flex items-center justify-center" :class="step.dotClass">
                  <span class="text-[8px] font-mono font-bold" :class="step.numColor">{{ si + 1 }}</span>
                </div>
                <span class="text-[11px] text-text-tertiary leading-snug">{{ step.text }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const liveEvents = [
  {
    type: 'sms',
    icon: 'lucide:message-square',
    iconBg: 'bg-blue-500/10',
    iconColor: 'text-blue-400',
    badgeClass: 'border-blue-500/15 text-blue-400/70 bg-blue-500/[0.06]',
    time: '2s ago',
    message: 'Mom: "Running late, can you pick up groceries on the way?"',
    action: 'send_sms → reply sent',
  },
  {
    type: 'notification',
    icon: 'lucide:bell',
    iconBg: 'bg-violet-500/10',
    iconColor: 'text-violet-400',
    badgeClass: 'border-violet-500/15 text-violet-400/70 bg-violet-500/[0.06]',
    time: '14s ago',
    message: 'Airline app: Flight AI-302 delayed 45 min — new gate B12',
    action: 'make_call_with_voice',
  },
  {
    type: 'notification',
    icon: 'lucide:bell',
    iconBg: 'bg-amber-500/10',
    iconColor: 'text-amber-400',
    badgeClass: 'border-amber-500/15 text-amber-400/70 bg-amber-500/[0.06]',
    time: '1m ago',
    message: 'Delivery app: Your package has been delivered to your doorstep',
    action: 'take_photo → send_sms',
  },
  {
    type: 'device_online',
    icon: 'lucide:wifi',
    iconBg: 'bg-green-500/10',
    iconColor: 'text-green-400',
    badgeClass: 'border-green-500/15 text-green-400/70 bg-green-500/[0.06]',
    time: '3m ago',
    message: 'Samsung Galaxy S25 connected — Android 16, battery 87%',
    action: 'read_notifications',
  },
]

const scenarios = [
  {
    label: 'Pet Cam',
    icon: 'lucide:camera',
    iconBg: 'bg-orange-500/10',
    iconColor: 'text-orange-400',
    labelColor: 'text-orange-400/70',
    accent: 'from-transparent via-orange-400/60 to-transparent',
    title: 'Check on your dog while you\'re at work',
    description: 'Set up a spare phone near your pet. Your AI takes photos on schedule and sends them to your WhatsApp. Your pup is safe, and you have the pics to prove it.',
    steps: [
      { text: 'Schedule triggers every 30 minutes', dotClass: 'border-orange-500/20', numColor: 'text-orange-400/70' },
      { text: 'AI snaps a photo via the phone camera', dotClass: 'border-orange-500/20', numColor: 'text-orange-400/70' },
      { text: 'Photo forwarded to your WhatsApp instantly', dotClass: 'border-orange-500/20', numColor: 'text-orange-400/70' },
    ],
  },
  {
    label: 'Smart Doorbell',
    icon: 'lucide:video',
    iconBg: 'bg-rose-500/10',
    iconColor: 'text-rose-400',
    labelColor: 'text-rose-400/70',
    accent: 'from-transparent via-rose-400/60 to-transparent',
    title: 'Record a clip when someone\'s at the door',
    description: 'Keep a spare phone near your entrance. When a doorbell notification fires, your AI records a short video and texts it to you.',
    steps: [
      { text: 'Doorbell notification event fires', dotClass: 'border-rose-500/20', numColor: 'text-rose-400/70' },
      { text: 'AI records a 5-second video clip', dotClass: 'border-rose-500/20', numColor: 'text-rose-400/70' },
      { text: 'Clip sent to your phone with context', dotClass: 'border-rose-500/20', numColor: 'text-rose-400/70' },
    ],
  },
  {
    label: 'Ride Alerts',
    icon: 'lucide:car',
    iconBg: 'bg-blue-500/10',
    iconColor: 'text-blue-400',
    labelColor: 'text-blue-400/70',
    accent: 'from-transparent via-blue-400/60 to-transparent',
    title: 'Know the second your ride arrives',
    description: 'Your AI watches for ride-hailing notifications. "Driver is 2 mins away" — it speaks it aloud via TTS and texts you so you\'re ready.',
    steps: [
      { text: 'Uber/Lyft notification event received', dotClass: 'border-blue-500/20', numColor: 'text-blue-400/70' },
      { text: 'AI announces via TTS on speakerphone', dotClass: 'border-blue-500/20', numColor: 'text-blue-400/70' },
      { text: 'Also texts you the driver details', dotClass: 'border-blue-500/20', numColor: 'text-blue-400/70' },
    ],
  },
  {
    label: 'Flight Tracker',
    icon: 'lucide:plane',
    iconBg: 'bg-sky-500/10',
    iconColor: 'text-sky-400',
    labelColor: 'text-sky-400/70',
    accent: 'from-transparent via-sky-400/60 to-transparent',
    title: 'Get called when your flight status changes',
    description: 'Gate change? Delay? Cancellation? Your AI monitors airline notifications and actually calls you to speak the update — no more missed gate announcements.',
    steps: [
      { text: 'Airline notification event fires', dotClass: 'border-sky-500/20', numColor: 'text-sky-400/70' },
      { text: 'AI detects delay or gate change', dotClass: 'border-sky-500/20', numColor: 'text-sky-400/70' },
      { text: 'Calls you and speaks the update via TTS', dotClass: 'border-sky-500/20', numColor: 'text-sky-400/70' },
    ],
  },
  {
    label: 'Baby Monitor',
    icon: 'lucide:heart-pulse',
    iconBg: 'bg-pink-500/10',
    iconColor: 'text-pink-400',
    labelColor: 'text-pink-400/70',
    accent: 'from-transparent via-pink-400/60 to-transparent',
    title: 'Keep an eye on the nursery',
    description: 'Keep a spare phone in the baby\'s room. Your AI takes periodic photos and forwards them to your WhatsApp. Peace of mind, one message away.',
    steps: [
      { text: 'Periodic check every 15 minutes', dotClass: 'border-pink-500/20', numColor: 'text-pink-400/70' },
      { text: 'AI snaps a photo of the nursery', dotClass: 'border-pink-500/20', numColor: 'text-pink-400/70' },
      { text: 'Sends "All good, sleeping peacefully" with photo', dotClass: 'border-pink-500/20', numColor: 'text-pink-400/70' },
    ],
  },
  {
    label: 'Smart Replies',
    icon: 'lucide:message-circle',
    iconBg: 'bg-violet-500/10',
    iconColor: 'text-violet-400',
    labelColor: 'text-violet-400/70',
    accent: 'from-transparent via-violet-400/60 to-transparent',
    title: 'Auto-reply when you\'re busy or driving',
    description: 'In a meeting? Driving? Your AI reads incoming messages and sends context-aware replies — "In a meeting, will call you back in 30 min."',
    steps: [
      { text: 'SMS or WhatsApp notification arrives', dotClass: 'border-violet-500/20', numColor: 'text-violet-400/70' },
      { text: 'AI reads the message and decides on a reply', dotClass: 'border-violet-500/20', numColor: 'text-violet-400/70' },
      { text: 'Sends a thoughtful auto-reply via SMS', dotClass: 'border-violet-500/20', numColor: 'text-violet-400/70' },
    ],
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

  document.querySelectorAll('#proactive .observe-fade').forEach((el) => {
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
</style>

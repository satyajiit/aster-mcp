<template>
  <section id="use-cases" class="relative py-32 px-6 overflow-hidden">
    <!-- Atmospheric background -->
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-surface-raised/40 to-transparent" />
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] rounded-full bg-aster/[0.02] blur-[120px] pointer-events-none" />

    <div class="relative max-w-6xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-20">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Real-World Use Cases</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Things you'll actually say &mdash; and things your AI will
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Forget technical jargon. Talk to your AI like a human, or let it act on its own device &mdash; Aster handles the rest.
        </p>
      </div>

      <!-- Use case category groups -->
      <div class="space-y-6">
        <div v-for="(group, gi) in groups" :key="group.label" class="space-y-4">
          <!-- Group label -->
          <div class="flex items-center gap-3 pl-1">
            <div class="w-7 h-7 rounded-lg flex items-center justify-center" :class="group.bg">
              <Icon :name="group.icon" class="text-sm" :class="group.color" />
            </div>
            <span class="text-xs font-semibold uppercase tracking-[0.15em]" :class="group.color">{{ group.label }}</span>
          </div>

          <!-- Cards row -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            <div
              v-for="(card, ci) in group.cards"
              :key="ci"
              class="use-card group relative rounded-2xl border border-border-dim bg-surface-raised/80 backdrop-blur-sm hover:border-border-subtle transition-all duration-400 overflow-hidden"
            >
              <!-- Top accent line -->
              <div class="absolute top-0 inset-x-0 h-px bg-gradient-to-r opacity-0 group-hover:opacity-100 transition-opacity duration-500" :class="group.accentGradient" />

              <div class="p-5">
                <!-- The human prompt -->
                <div class="flex items-start gap-3 mb-4">
                  <div class="flex-shrink-0 mt-0.5 w-6 h-6 rounded-full bg-gradient-to-br from-aster/20 to-teal-400/10 border border-aster/20 flex items-center justify-center">
                    <Icon name="lucide:user" class="text-[10px] text-aster" />
                  </div>
                  <p class="text-[13.5px] leading-relaxed text-text-primary font-medium">
                    "{{ card.prompt }}"
                  </p>
                </div>

                <!-- The AI response hint -->
                <div class="flex items-start gap-3 ml-0.5">
                  <div class="flex-shrink-0 mt-0.5 w-5 h-5 rounded-full flex items-center justify-center" :class="group.responseBg">
                    <Icon :name="card.responseIcon" class="text-[10px]" :class="group.color" />
                  </div>
                  <p class="text-xs leading-relaxed text-text-tertiary">
                    {{ card.response }}
                  </p>
                </div>
              </div>

              <!-- Bottom tool tags -->
              <div class="px-5 pb-4 flex flex-wrap gap-1.5">
                <span
                  v-for="tool in card.tools"
                  :key="tool"
                  class="px-2 py-0.5 rounded-md text-[10px] font-mono border transition-colors"
                  :class="[group.tagClass]"
                >
                  {{ tool }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
interface UseCase {
  prompt: string
  response: string
  responseIcon: string
  tools: string[]
}

interface Group {
  label: string
  icon: string
  bg: string
  color: string
  accentGradient: string
  responseBg: string
  tagClass: string
  cards: UseCase[]
}

const groups: Group[] = [
  {
    label: 'Photos & Media',
    icon: 'lucide:image',
    bg: 'bg-violet-500/10',
    color: 'text-violet-400',
    accentGradient: 'from-transparent via-violet-400/60 to-transparent',
    responseBg: 'bg-violet-500/10',
    tagClass: 'border-violet-500/10 text-violet-400/50 bg-violet-500/[0.04]',
    cards: [
      {
        prompt: 'Find all duplicate photos on my phone and show me which ones to delete',
        response: 'Scanning media library... Found 47 duplicate sets across 2,104 photos. I can show you side-by-side comparisons.',
        responseIcon: 'lucide:copy',
        tools: ['search_media', 'index_media_metadata'],
      },
      {
        prompt: 'Show me photos I took in Goa last December',
        response: 'Found 83 photos from Dec 2024 geotagged near Goa. Includes 12 beach shots and 6 sunset panoramas.',
        responseIcon: 'lucide:map-pin',
        tools: ['search_media'],
      },
      {
        prompt: 'Find screenshots I took this week and delete the blurry ones',
        response: 'Found 23 screenshots from this week. 5 appear blurry or unreadable — ready to delete on your say.',
        responseIcon: 'lucide:trash-2',
        tools: ['search_media', 'delete_file'],
      },
    ],
  },
  {
    label: 'Reminders & Alerts',
    icon: 'lucide:bell',
    bg: 'bg-amber-500/10',
    color: 'text-amber-400',
    accentGradient: 'from-transparent via-amber-400/60 to-transparent',
    responseBg: 'bg-amber-500/10',
    tagClass: 'border-amber-500/10 text-amber-400/50 bg-amber-500/[0.04]',
    cards: [
      {
        prompt: 'Ring my phone at 10 AM tomorrow for the standup meeting',
        response: 'Done! I\'ll play a ringtone and show a notification at 10:00 AM — "Standup Meeting".',
        responseIcon: 'lucide:alarm-clock',
        tools: ['play_audio', 'post_notification'],
      },
      {
        prompt: 'Vibrate my phone three times — I can\'t find it under the couch',
        response: 'Vibrating now with a strong 3-pulse pattern. Keep listening!',
        responseIcon: 'lucide:vibrate',
        tools: ['vibrate'],
      },
      {
        prompt: 'Read my unread notifications and tell me if anything is urgent',
        response: '14 notifications: 2 from Slack (marked urgent), 1 missed call from Mom, rest are promotions.',
        responseIcon: 'lucide:bell-ring',
        tools: ['read_notifications'],
      },
    ],
  },
  {
    label: 'Files & Cleanup',
    icon: 'lucide:folder',
    bg: 'bg-teal-500/10',
    color: 'text-teal-400',
    accentGradient: 'from-transparent via-teal-400/60 to-transparent',
    responseBg: 'bg-teal-500/10',
    tagClass: 'border-teal-500/10 text-teal-400/50 bg-teal-500/[0.04]',
    cards: [
      {
        prompt: 'Copy the meeting-notes.pdf from my phone to my Mac',
        response: 'Found meeting-notes.pdf in Downloads (2.3 MB). Reading file and ready to transfer.',
        responseIcon: 'lucide:send',
        tools: ['list_files', 'read_file'],
      },
      {
        prompt: 'My phone is running low on storage — find what\'s eating up space',
        response: 'Storage breakdown: WhatsApp media 8.2 GB, cached data 3.1 GB, old APKs 1.4 GB. Want me to clean up?',
        responseIcon: 'lucide:hard-drive',
        tools: ['analyze_storage', 'find_large_files'],
      },
      {
        prompt: 'Delete all downloaded PDFs older than 6 months',
        response: 'Found 34 PDFs older than 6 months in Downloads (890 MB total). Shall I delete them all?',
        responseIcon: 'lucide:trash-2',
        tools: ['list_files', 'delete_file'],
      },
    ],
  },
  {
    label: 'Communication',
    icon: 'lucide:message-circle',
    bg: 'bg-blue-500/10',
    color: 'text-blue-400',
    accentGradient: 'from-transparent via-blue-400/60 to-transparent',
    responseBg: 'bg-blue-500/10',
    tagClass: 'border-blue-500/10 text-blue-400/50 bg-blue-500/[0.04]',
    cards: [
      {
        prompt: 'Call Mom and tell her I\'ll be 20 minutes late',
        response: 'Calling Mom now. Speakerphone on — I\'ll speak your message once she picks up.',
        responseIcon: 'lucide:phone-call',
        tools: ['make_call_with_voice'],
      },
      {
        prompt: 'Read my last SMS from the delivery guy and reply "Thanks, I\'ll be home"',
        response: 'Last SMS: "Your package will arrive between 2-4 PM." Sending reply now...',
        responseIcon: 'lucide:message-square',
        tools: ['read_sms', 'send_sms'],
      },
      {
        prompt: 'Show "Do Not Disturb — In a Meeting" on my phone screen',
        response: 'Displaying overlay on your device. It\'ll stay visible until you dismiss it.',
        responseIcon: 'lucide:monitor',
        tools: ['show_overlay'],
      },
    ],
  },
  {
    label: 'Daily Automations',
    icon: 'lucide:zap',
    bg: 'bg-rose-500/10',
    color: 'text-rose-400',
    accentGradient: 'from-transparent via-rose-400/60 to-transparent',
    responseBg: 'bg-rose-500/10',
    tagClass: 'border-rose-500/10 text-rose-400/50 bg-rose-500/[0.04]',
    cards: [
      {
        prompt: 'Take a screenshot of my screen and tell me what app is open',
        response: 'You have YouTube open, playing "Lo-fi Beats to Study To". Battery at 62%, Wi-Fi connected.',
        responseIcon: 'lucide:scan',
        tools: ['take_screenshot', 'get_screen_hierarchy'],
      },
      {
        prompt: 'What\'s my battery at? Should I charge before heading out?',
        response: '34% with about 2 hours left. You should charge before leaving — it won\'t last a full evening.',
        responseIcon: 'lucide:battery-low',
        tools: ['get_battery'],
      },
      {
        prompt: 'Open Google Maps and search for the nearest coffee shop',
        response: 'Launching Google Maps... Searching "coffee shop near me".',
        responseIcon: 'lucide:coffee',
        tools: ['launch_intent', 'input_text', 'click_by_text'],
      },
    ],
  },
  {
    label: 'Proactive & Monitoring',
    icon: 'lucide:radio',
    bg: 'bg-fuchsia-500/10',
    color: 'text-fuchsia-400',
    accentGradient: 'from-transparent via-fuchsia-400/60 to-transparent',
    responseBg: 'bg-fuchsia-500/10',
    tagClass: 'border-fuchsia-500/10 text-fuchsia-400/50 bg-fuchsia-500/[0.04]',
    cards: [
      {
        prompt: 'Left my dog home alone — take a photo every 30 mins and send it to me on WhatsApp',
        response: 'Pet cam mode active. I\'ll snap a photo on schedule and forward it to your WhatsApp. Your pup is safe with me.',
        responseIcon: 'lucide:camera',
        tools: ['take_photo', 'send_sms', 'event_forwarding'],
      },
      {
        prompt: 'When I get a message while driving, auto-reply that I\'m on the road and will call back',
        response: 'Got SMS from Alex: "Are you free?" Replied: "Driving right now, will call you back in 20 min."',
        responseIcon: 'lucide:message-circle',
        tools: ['sms_event', 'send_sms'],
      },
      {
        prompt: 'If my food delivery app says the rider is nearby, announce it on speakerphone',
        response: 'Delivery notification detected — "Your rider is 1 min away." Announcing via TTS now. Go grab it!',
        responseIcon: 'lucide:volume-2',
        tools: ['notification_event', 'speak_tts'],
      },
    ],
  },
  {
    label: 'AI\'s Own Phone',
    icon: 'lucide:smartphone-charging',
    bg: 'bg-orange-500/10',
    color: 'text-orange-400',
    accentGradient: 'from-transparent via-orange-400/60 to-transparent',
    responseBg: 'bg-orange-500/10',
    tagClass: 'border-orange-500/10 text-orange-400/50 bg-orange-500/[0.04]',
    cards: [
      {
        prompt: 'Call me if my flight gets delayed — and tell me the new time',
        response: 'Monitoring airline notifications. Flight AI-302 delayed 45 mins — calling you now to let you know.',
        responseIcon: 'lucide:phone-outgoing',
        tools: ['notification_event', 'make_call_with_voice'],
      },
      {
        prompt: 'If someone rings the doorbell, record a 5-second clip and send it to me',
        response: 'Doorbell notification detected. Recording 5s video... Done. Sending clip to your WhatsApp now.',
        responseIcon: 'lucide:video',
        tools: ['notification_event', 'record_video', 'send_sms'],
      },
      {
        prompt: 'Keep the phone in the baby\'s room — if it picks up crying sounds, text me immediately',
        response: 'Audio monitoring active. Detected unusual noise — taking a photo and texting you with status. Everything looks calm.',
        responseIcon: 'lucide:baby',
        tools: ['take_photo', 'send_sms', 'notification_event'],
      },
    ],
  },
]
</script>

<style scoped>
.use-card {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
              border-color 0.4s ease,
              box-shadow 0.4s ease;
}
.use-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.3);
}
</style>

<template>
  <section id="embrace" class="relative py-20 sm:py-28 px-6 overflow-hidden">
    <!-- Decorative background -->
    <div class="absolute inset-0" aria-hidden="true">
      <div class="absolute inset-0 bg-gradient-to-b from-surface via-[#0d0a12] to-surface" />
      <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] max-w-full rounded-full bg-[radial-gradient(circle,_rgba(168,85,247,0.06)_0%,_rgba(45,212,191,0.03)_40%,_transparent_70%)] blur-[40px]" />
    </div>

    <div class="relative max-w-5xl mx-auto">
      <div class="text-center mb-12 observe-fade">
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-violet-400 mb-4 font-mono">The missing piece</p>
        <h2 class="text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight leading-[1.15]">
          <span class="text-text-primary">You gave your AI a voice.</span>
          <br />
          <span class="bg-gradient-to-r from-violet-400 via-aster-light to-aster bg-clip-text text-transparent">Now give it hands.</span>
        </h2>
        <p class="mt-6 text-text-secondary max-w-2xl mx-auto text-lg leading-relaxed">
          Your assistant already reads, writes and plans for you. It has been stuck behind a
          text box, unable to touch the one device that runs your day.
        </p>
      </div>

      <!-- Before / after. Two definition lists, because each row really is a
           term and its explanation — and because a screen reader should hear
           "4 items" rather than a run of unrelated paragraphs. -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-14">
        <div class="observe-fade rounded-2xl border border-border-dim bg-surface-raised/50 backdrop-blur-sm p-6 sm:p-8" data-delay="80">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center flex-shrink-0">
              <Icon name="lucide:message-circle" class="text-rose-400 text-lg" aria-hidden="true" />
            </div>
            <div>
              <p class="text-[10px] font-mono uppercase tracking-[0.15em] text-rose-400">Today</p>
              <h3 class="text-text-primary font-semibold text-sm">Your AI can talk</h3>
            </div>
          </div>

          <dl class="space-y-3.5">
            <div v-for="item in BEFORE_ITEMS" :key="item.label">
              <dt class="flex items-start gap-3 text-[13px] text-text-primary font-medium leading-snug">
                <span class="flex-shrink-0 w-5 h-5 rounded-md bg-rose-500/10 border border-rose-500/20 flex items-center justify-center mt-px">
                  <Icon :name="item.icon" class="text-[11px] text-rose-400" aria-hidden="true" />
                </span>
                <span>{{ item.label }}</span>
              </dt>
              <dd class="ml-8 text-xs text-text-tertiary leading-snug">{{ item.detail }}</dd>
            </div>
          </dl>

          <p class="mt-6 pt-5 border-t border-border-dim text-[13px] text-text-tertiary italic leading-relaxed">
            &ldquo;Set an alarm for 6 AM&rdquo; &mdash; and then you pick up the phone and do it yourself.
          </p>
        </div>

        <div class="observe-fade relative rounded-2xl border border-aster/20 bg-surface-raised/50 backdrop-blur-sm p-6 sm:p-8" data-delay="160">
          <div class="absolute -inset-px rounded-2xl bg-gradient-to-br from-aster/[0.04] to-violet-500/[0.02] pointer-events-none" aria-hidden="true" />

          <div class="relative">
            <div class="flex items-center gap-3 mb-6">
              <div class="w-10 h-10 rounded-xl bg-aster/10 border border-aster/20 flex items-center justify-center flex-shrink-0">
                <Icon name="lucide:hand" class="text-aster text-lg" aria-hidden="true" />
              </div>
              <div>
                <p class="text-[10px] font-mono uppercase tracking-[0.15em] text-aster">With Aster</p>
                <h3 class="text-text-primary font-semibold text-sm">Your AI can act</h3>
              </div>
            </div>

            <dl class="space-y-3.5">
              <div v-for="item in AFTER_ITEMS" :key="item.label">
                <dt class="flex items-start gap-3 text-[13px] text-text-primary font-medium leading-snug">
                  <span class="flex-shrink-0 w-5 h-5 rounded-md bg-aster/10 border border-aster/20 flex items-center justify-center mt-px">
                    <Icon :name="item.icon" class="text-[11px] text-aster" aria-hidden="true" />
                  </span>
                  <span>{{ item.label }}</span>
                </dt>
                <dd class="ml-8 text-xs text-text-tertiary leading-snug">{{ item.detail }}</dd>
              </div>
            </dl>

            <p class="mt-6 pt-5 border-t border-aster/20 text-[13px] text-text-secondary italic leading-relaxed">
              &ldquo;Set an alarm for 6 AM&rdquo; &mdash; <span class="text-aster not-italic font-medium">and it just does it.</span>
            </p>
          </div>
        </div>
      </div>

      <!-- The pull quote -->
      <div class="observe-fade text-center">
        <div class="relative max-w-3xl mx-auto">
          <div class="absolute -inset-8 bg-gradient-to-r from-violet-500/[0.03] via-aster/[0.05] to-violet-500/[0.03] rounded-3xl blur-xl" aria-hidden="true" />
          <blockquote class="relative text-xl sm:text-2xl font-medium text-text-primary leading-relaxed tracking-tight">
            You already trust AI with your email, your calendar and your code.
            <br class="hidden sm:block" />
            <span class="bg-gradient-to-r from-violet-400 to-aster bg-clip-text text-transparent">Aster lets it touch your phone &mdash; or better yet, its own.</span>
          </blockquote>
        </div>

        <p class="relative mt-8">
          <NuxtLink
            to="/ai-phone/"
            class="inline-flex items-center gap-2 min-h-11 px-5 rounded-xl border border-amber-500/30 bg-amber-500/10 text-amber-400 text-sm font-semibold hover:bg-amber-500/15 hover:border-amber-500/50 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
          >
            <Icon name="lucide:smartphone" class="text-base" aria-hidden="true" />
            Give your AI its own phone
            <Icon name="lucide:arrow-right" class="text-sm" aria-hidden="true" />
          </NuxtLink>
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * Trimmed to one idea: the before/after contrast and the pull quote.
 *
 * What left this file and where it went:
 * - The "give your AI its own phone" half — phone mockup, incoming call,
 *   four bullets — is now the whole of /ai-phone. Only the link out remains.
 * - The six use-case vignettes are on /use-cases. Three of them ("make it
 *   vibrate, it is behind the couch", "photos with Mom", "pet cam") were
 *   printed on this page two or three separate times over.
 */
const BEFORE_ITEMS = [
  { icon: 'lucide:message-square', label: 'Chats on WhatsApp and Telegram', detail: 'It reads and answers messages for you' },
  { icon: 'lucide:mail', label: 'Drafts email and documents', detail: 'It writes on your behalf' },
  { icon: 'lucide:calendar', label: 'Schedules and organises', detail: 'It keeps track of your time' },
  { icon: 'lucide:code', label: 'Writes and reviews code', detail: 'It builds software alongside you' },
]

const AFTER_ITEMS = [
  { icon: 'lucide:smartphone', label: 'Taps, swipes and types on the phone', detail: 'Full screen control through the accessibility service' },
  { icon: 'lucide:camera', label: 'Takes photos and records video', detail: 'Either camera, on request or when an event fires' },
  { icon: 'lucide:radio', label: 'Reacts to events as they happen', detail: 'New SMS, notifications and device status, pushed to your agent' },
  { icon: 'lucide:phone-call', label: 'Reads notifications, sends SMS, places calls', detail: 'Including a call that speaks a message out loud' },
]

useObserveFade()
</script>

<style scoped>
/* Visible by default — the prerendered HTML must read as finished copy. The
   dimmed start state is armed by useObserveFade() at runtime, and only for
   elements still below the fold. */
.observe-fade.fade-armed {
  opacity: 0;
  transform: translateY(24px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

.observe-fade.fade-armed.fade-in {
  opacity: 1;
  transform: translateY(0);
}
</style>

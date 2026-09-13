<template>
  <!--
    Icon literals for the @nuxt/icon clientBundle scanner (it globs .vue, never
    .ts): lucide:smartphone lucide:plug lucide:server lucide:download
    lucide:phone-call lucide:bell-ring lucide:hand lucide:bot lucide:signal
    lucide:list-checks lucide:phone lucide:phone-off

    The first six are the icons on the NEEDS and CHANGES cards, which now live
    in ~/data/ai-phone so the markdown twin can see them. The scanner cannot
    read a .ts file, so moving the arrays out of this component moved their
    icon names out of its reach; this comment is what keeps them bundled.
    lucide:server was already missing from the old list.
  -->
  <section id="ai-phone" class="relative px-6 pt-28 pb-20 sm:pt-36 sm:pb-24 overflow-hidden">
    <div aria-hidden="true" class="absolute inset-0 pointer-events-none">
      <div class="absolute inset-0 bg-gradient-to-b from-surface to-surface-raised" />
      <div
        class="absolute -top-24 right-0 w-[620px] max-w-full h-[520px] rounded-full blur-[90px]"
        style="background: radial-gradient(circle, var(--color-aster-glow) 0%, transparent 70%)"
      />
    </div>

    <div class="relative max-w-5xl mx-auto">
      <div class="grid gap-12 lg:grid-cols-[minmax(0,1fr)_260px] lg:items-start">
        <div>
          <p class="font-mono text-xs uppercase tracking-[0.2em] text-aster">The AI&rsquo;s own phone</p>
          <h1 class="mt-4 text-4xl sm:text-5xl font-bold tracking-tight text-text-primary leading-[1.1]">
            Give your AI its own phone
          </h1>

          <p class="mt-6 text-lg leading-relaxed text-text-secondary">
            {{ AI_PHONE_LEDE.definition }}
          </p>
          <p class="mt-4 text-lg leading-relaxed text-text-secondary">
            {{ AI_PHONE_LEDE.contrast }}
          </p>

          <h2 class="mt-12 text-2xl font-bold tracking-tight text-text-primary">What it takes</h2>
          <p class="mt-3 text-base leading-relaxed text-text-secondary">
            {{ AI_PHONE_NEEDS_INTRO }}
          </p>
          <ul class="mt-6 space-y-4">
            <li v-for="need in AI_PHONE_NEEDS" :key="need.title" class="flex gap-4">
              <AIconTile :icon="need.icon" :accent="need.accent" :size="36" class="shrink-0" />
              <div>
                <h3 class="text-base font-semibold text-text-primary">{{ need.title }}</h3>
                <p class="mt-1 text-sm leading-relaxed text-text-secondary">{{ need.detail }}</p>
              </div>
            </li>
          </ul>

          <h2 class="mt-12 text-2xl font-bold tracking-tight text-text-primary">What changes</h2>
          <p class="mt-3 text-base leading-relaxed text-text-secondary">
            {{ AI_PHONE_CHANGES_INTRO }}
          </p>
          <ul class="mt-6 space-y-4">
            <li v-for="change in AI_PHONE_CHANGES" :key="change.title" class="flex gap-4">
              <AIconTile :icon="change.icon" :accent="change.accent" :size="36" class="shrink-0" />
              <div>
                <h3 class="text-base font-semibold text-text-primary">{{ change.title }}</h3>
                <p class="mt-1 text-sm leading-relaxed text-text-secondary">{{ change.detail }}</p>
              </div>
            </li>
          </ul>

          <p class="mt-10 rounded-2xl border border-border-dim bg-surface-raised p-5 text-sm leading-relaxed text-text-secondary">
            <strong class="font-semibold text-text-primary">{{ AI_PHONE_LIMIT.title }}</strong>
            {{ AI_PHONE_LIMIT.body }}
          </p>

          <div class="mt-8 flex flex-wrap gap-3">
            <NuxtLink
              to="/setup/"
              class="inline-flex min-h-11 items-center gap-2 rounded-xl border border-aster/40 bg-aster/10 px-5 py-3 text-sm font-semibold text-text-primary transition-colors hover:bg-aster/20"
            >
              <Icon name="lucide:download" aria-hidden="true" />
              Set up the spare phone
            </NuxtLink>
            <NuxtLink
              to="/tools/"
              class="inline-flex min-h-11 items-center gap-2 rounded-xl border border-border-subtle bg-surface-raised px-5 py-3 text-sm font-semibold text-text-primary transition-colors hover:border-aster/40"
            >
              <Icon name="lucide:list-checks" aria-hidden="true" />
              All {{ TOOL_COUNTS.mcpServer }} tools
            </NuxtLink>
          </div>
        </div>

        <!-- Illustration: the call you get from the phone your AI holds. -->
        <figure class="mx-auto w-[220px] lg:sticky lg:top-24">
          <div
            aria-hidden="true"
            class="rounded-[28px] border border-border-subtle bg-surface-raised p-3"
          >
            <div class="rounded-[18px] bg-surface overflow-hidden">
              <div class="flex items-center justify-between px-5 pt-5 pb-2">
                <span class="font-mono text-[10px] text-text-tertiary">9:41</span>
                <Icon name="lucide:signal" class="text-xs text-text-tertiary" />
              </div>

              <div class="px-5 pt-4 pb-7 text-center">
                <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full border border-aster/30 bg-aster/10">
                  <Icon name="lucide:bot" class="text-2xl text-aster" />
                </div>

                <p class="mt-4 font-mono text-[10px] uppercase tracking-[0.15em] text-text-tertiary">
                  Incoming call
                </p>
                <p class="mt-1 text-base font-semibold text-text-primary">Your assistant</p>
                <p class="mt-0.5 text-xs text-text-tertiary">Aster line</p>

                <p class="mx-auto mt-4 max-w-[168px] rounded-lg border border-border-dim bg-surface-raised px-3 py-2 text-left text-[11px] leading-relaxed text-text-secondary">
                  &ldquo;Your gate moved to B12 and boarding starts in forty minutes.&rdquo;
                </p>

                <div class="mt-6 flex items-center justify-center gap-8">
                  <span class="flex h-11 w-11 items-center justify-center rounded-full border border-border-subtle bg-surface-raised">
                    <Icon name="lucide:phone-off" class="text-sm text-text-secondary" />
                  </span>
                  <span class="flex h-11 w-11 items-center justify-center rounded-full border border-aster/40 bg-aster/15">
                    <Icon name="lucide:phone" class="text-sm text-aster" />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <figcaption class="mt-4 text-xs leading-relaxed text-text-tertiary">
            What it looks like from your side: a call from the phone your assistant holds, reading out
            the one thing you needed to know.
          </figcaption>
        </figure>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * The hero's prose lives in ~/data/ai-phone, not here.
 *
 * NEEDS and CHANGES were component-local consts, which is precisely the shape
 * scripts/generate-machine-readable.ts cannot see: /ai-phone.md shipped without
 * "What it takes", "What changes" or the acoustic-coupling limit, at 48% of the
 * rendered page. Nothing in this file is copy any more — it is layout over
 * exported data the twin reads from the same module.
 */
import { AI_PHONE_CHANGES, AI_PHONE_CHANGES_INTRO, AI_PHONE_LEDE, AI_PHONE_LIMIT, AI_PHONE_NEEDS, AI_PHONE_NEEDS_INTRO } from '~/data/ai-phone'
import { TOOL_COUNTS } from '~/data/site'

</script>

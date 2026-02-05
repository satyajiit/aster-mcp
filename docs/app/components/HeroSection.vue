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

      <!-- Headline -->
      <h1 class="animate-fade-up delay-100 text-5xl sm:text-6xl lg:text-7xl font-bold tracking-tight leading-[1.08] mb-6">
        <span class="text-text-primary">Control Android</span>
        <br />
        <span class="bg-gradient-to-r from-aster via-aster-light to-teal-300 bg-clip-text text-transparent">with your AI</span>
      </h1>

      <!-- Subheading -->
      <p class="animate-fade-up delay-200 text-lg sm:text-xl text-text-secondary max-w-2xl mx-auto leading-relaxed mb-10">
        Aster bridges your Android device to AI assistants like Claude, OpenClaw, or MoltBot through the
        <span class="text-text-primary font-medium">Model Context Protocol</span>.
        Automate UI, manage files, search photos, and more &mdash; all with natural language.
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
          href="https://github.com/satyajiit/Aster"
          target="_blank"
          rel="noopener"
          class="px-7 py-3.5 rounded-xl border border-border-subtle text-text-secondary font-medium text-sm hover:border-aster/30 hover:text-text-primary transition-all"
        >
          <Icon name="mdi:github" class="inline mr-1.5" />
          View on GitHub
        </a>
      </div>

      <!-- Architecture preview -->
      <div class="animate-fade-up delay-500 mt-20 max-w-2xl mx-auto">
        <div class="gradient-border p-6 sm:p-8">
          <div class="terminal text-left">
            <div class="comment">// Ask Claude anything about your device</div>
            <div class="mt-2">
              <span class="prompt">claude</span>
              <span class="text-text-secondary"> &gt; </span>
              <span class="string">"Take a screenshot and tell me what's on screen"</span>
            </div>
            <div class="mt-3 text-text-tertiary text-xs">
              <span class="text-aster">Aster</span> &rarr; Screenshot captured &rarr; Accessibility tree parsed
            </div>
            <div class="mt-1 text-text-secondary text-xs">
              I can see the home screen with 12 apps. The time shows 3:42 PM, <br class="hidden sm:inline" />
              battery is at 78%, and you have 3 unread notifications...
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Scroll indicator -->
    <div class="absolute bottom-8 left-1/2 -translate-x-1/2 animate-bounce">
      <Icon name="lucide:chevron-down" class="text-text-tertiary text-xl" />
    </div>
  </section>
</template>

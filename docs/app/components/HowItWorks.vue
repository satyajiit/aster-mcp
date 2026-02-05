<template>
  <section id="how-it-works" class="relative py-32 px-6">
    <!-- Background accent -->
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-aster/[0.02] to-transparent" />

    <div class="relative max-w-5xl mx-auto">
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Architecture</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          How Aster works
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Three components working together to give AI full control over your Android device.
        </p>
      </div>

      <!-- Architecture diagram -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- AI Client -->
        <div class="relative p-6 rounded-2xl bg-surface-raised border border-border-dim text-center">
          <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-violet-500/20 to-purple-500/20 border border-violet-500/20 flex items-center justify-center mx-auto mb-4">
            <Icon name="lucide:brain" class="text-2xl text-violet-400" />
          </div>
          <h3 class="text-base font-semibold text-text-primary mb-2">AI Assistant</h3>
          <p class="text-sm text-text-secondary leading-relaxed">
            Claude, OpenClaw, or any MCP-compatible client sends tool calls to the Aster server.
          </p>
          <div class="hidden md:block absolute -right-3 top-1/2 -translate-y-1/2 z-10">
            <Icon name="lucide:chevron-right" class="text-aster text-lg" />
          </div>
          <div class="md:hidden flex justify-center mt-4">
            <Icon name="lucide:chevron-down" class="text-aster text-lg" />
          </div>
        </div>

        <!-- MCP Server -->
        <div class="relative p-6 rounded-2xl bg-surface-raised border border-aster/20 text-center ring-1 ring-aster/10">
          <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-aster/20 to-teal-500/20 border border-aster/20 flex items-center justify-center mx-auto mb-4">
            <Icon name="lucide:server" class="text-2xl text-aster" />
          </div>
          <h3 class="text-base font-semibold text-text-primary mb-2">Aster MCP Server</h3>
          <p class="text-sm text-text-secondary leading-relaxed">
            Node.js server exposes 40+ tools via MCP HTTP. Routes commands over WebSocket to your device.
          </p>
          <div class="hidden md:block absolute -right-3 top-1/2 -translate-y-1/2 z-10">
            <Icon name="lucide:chevron-right" class="text-aster text-lg" />
          </div>
          <div class="md:hidden flex justify-center mt-4">
            <Icon name="lucide:chevron-down" class="text-aster text-lg" />
          </div>
        </div>

        <!-- Android App -->
        <div class="p-6 rounded-2xl bg-surface-raised border border-border-dim text-center">
          <div class="w-14 h-14 rounded-2xl bg-gradient-to-br from-green-500/20 to-emerald-500/20 border border-green-500/20 flex items-center justify-center mx-auto mb-4">
            <Icon name="lucide:smartphone" class="text-2xl text-green-400" />
          </div>
          <h3 class="text-base font-semibold text-text-primary mb-2">Android App</h3>
          <p class="text-sm text-text-secondary leading-relaxed">
            Kotlin companion app connects via WebSocket. Uses Accessibility APIs to execute commands on device.
          </p>
        </div>
      </div>

      <!-- Protocol badges -->
      <div class="flex flex-wrap justify-center gap-3 mt-10">
        <span class="px-3 py-1.5 rounded-lg bg-surface-raised border border-border-dim text-xs text-text-tertiary">
          <Icon name="lucide:plug" class="inline mr-1 text-aster" /> MCP over HTTP
        </span>
        <span class="px-3 py-1.5 rounded-lg bg-surface-raised border border-border-dim text-xs text-text-tertiary">
          <Icon name="lucide:radio" class="inline mr-1 text-aster" /> WebSocket
        </span>
        <span class="px-3 py-1.5 rounded-lg bg-surface-raised border border-border-dim text-xs text-text-tertiary">
          <Icon name="lucide:accessibility" class="inline mr-1 text-aster" /> Accessibility API
        </span>
        <span class="px-3 py-1.5 rounded-lg bg-surface-raised border border-border-dim text-xs text-text-tertiary">
          <Icon name="lucide:database" class="inline mr-1 text-aster" /> SQLite
        </span>
      </div>
    </div>
  </section>
</template>

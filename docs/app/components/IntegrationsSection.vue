<template>
  <section id="integrations" class="relative py-32 px-6">
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-violet-500/[0.015] to-transparent" />

    <div class="relative max-w-5xl mx-auto">
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Integrations</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Works with your favorite AI clients
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Aster is compatible with any MCP client. Give Claude, OpenClaw, MoltBot, or ClawBot a CoPilot for your phone &mdash; or let them own one.
        </p>
      </div>

      <!-- Client cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-12">
        <div
          v-for="client in clients"
          :key="client.name"
          class="group p-5 rounded-2xl bg-surface-raised border border-border-dim hover:border-border-subtle transition-all text-center"
        >
          <div class="w-12 h-12 rounded-xl flex items-center justify-center mx-auto mb-3" :class="client.bg">
            <Icon :name="client.icon" class="text-xl" :class="client.color" />
          </div>
          <h3 class="text-sm font-semibold text-text-primary mb-1">{{ client.name }}</h3>
          <p class="text-xs text-text-tertiary leading-relaxed">{{ client.description }}</p>
        </div>
      </div>

      <!-- Install methods -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- ClawHub install -->
        <div class="p-6 rounded-2xl bg-surface-raised border border-border-dim">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-8 h-8 rounded-lg bg-purple-500/10 flex items-center justify-center">
              <Icon name="lucide:package" class="text-sm text-purple-400" />
            </div>
            <h3 class="text-sm font-semibold text-text-primary">Install via ClawHub</h3>
          </div>
          <p class="text-xs text-text-secondary mb-3">
            Available as a skill on <a href="https://clawhub.ai/satyajit/aster" target="_blank" rel="noopener" class="text-aster hover:underline">ClawHub</a> for OpenClaw, MoltBot, and ClawBot.
          </p>
          <div class="rounded-lg bg-surface border border-border-dim overflow-hidden">
            <div class="flex items-center gap-2 px-3 py-2 border-b border-border-dim">
              <span class="w-2 h-2 rounded-full bg-red-500/60" />
              <span class="w-2 h-2 rounded-full bg-yellow-500/60" />
              <span class="w-2 h-2 rounded-full bg-green-500/60" />
            </div>
            <pre class="terminal p-3 text-xs"><code><span class="prompt">$</span> <span class="text-text-primary">clawhub install aster</span></code></pre>
          </div>
        </div>

        <!-- Direct skill install -->
        <div class="p-6 rounded-2xl bg-surface-raised border border-border-dim">
          <div class="flex items-center gap-2 mb-4">
            <div class="w-8 h-8 rounded-lg bg-orange-500/10 flex items-center justify-center">
              <Icon name="lucide:link" class="text-sm text-orange-400" />
            </div>
            <h3 class="text-sm font-semibold text-text-primary">Install via direct link</h3>
          </div>
          <p class="text-xs text-text-secondary mb-3">
            Install the skill directly from the GitHub repository raw link for any compatible client.
          </p>
          <div class="rounded-lg bg-surface border border-border-dim overflow-hidden">
            <div class="flex items-center gap-2 px-3 py-2 border-b border-border-dim">
              <span class="w-2 h-2 rounded-full bg-red-500/60" />
              <span class="w-2 h-2 rounded-full bg-yellow-500/60" />
              <span class="w-2 h-2 rounded-full bg-green-500/60" />
            </div>
            <pre class="terminal p-3 text-xs overflow-x-auto"><code><span class="comment"># Raw skill link:</span>
<span class="text-text-secondary break-all">https://raw.githubusercontent.com/satyajiit/Aster/main/skill/SKILL.md</span></code></pre>
          </div>
        </div>
      </div>

      <!-- npm badge -->
      <div class="mt-8 text-center">
        <div class="inline-flex items-center gap-3 px-5 py-3 rounded-xl bg-surface-raised border border-border-dim">
          <Icon name="lucide:box" class="text-aster" />
          <span class="text-sm text-text-secondary">Also available on npm:</span>
          <code class="text-sm font-mono text-aster">npm install -g aster-mcp</code>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const clients = [
  {
    name: 'Claude',
    icon: 'lucide:brain',
    bg: 'bg-amber-500/10',
    color: 'text-amber-400',
    description: 'Claude Desktop & Claude Code via MCP',
  },
  {
    name: 'OpenClaw',
    icon: 'lucide:terminal-square',
    bg: 'bg-orange-500/10',
    color: 'text-orange-400',
    description: 'Install as a ClawHub skill',
  },
  {
    name: 'MoltBot',
    icon: 'lucide:bot',
    bg: 'bg-blue-500/10',
    color: 'text-blue-400',
    description: 'Add via ClawHub or direct skill link',
  },
  {
    name: 'ClawBot',
    icon: 'lucide:cog',
    bg: 'bg-purple-500/10',
    color: 'text-purple-400',
    description: 'Compatible via MCP or ClawHub skill',
  },
]
</script>

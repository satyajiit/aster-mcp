<template>
  <section class="relative py-32 px-6 overflow-hidden">
    <!-- Background texture -->
    <div class="absolute inset-0 bg-gradient-to-b from-transparent via-surface-raised/30 to-transparent" />
    <div
      class="absolute inset-0 opacity-[0.015]"
      style="background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,0.3) 1px, transparent 0); background-size: 32px 32px;"
    />

    <div class="relative max-w-5xl mx-auto">
      <!-- Section header -->
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Security & Privacy</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Your data never leaves your network
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Aster is fully self-hosted. No cloud, no telemetry, no third-party relay. Everything runs on your machine.
        </p>
      </div>

      <!-- Top row: 3 key pillars -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-3">
        <div
          v-for="pillar in pillars"
          :key="pillar.title"
          class="group relative p-5 rounded-2xl bg-surface-raised/60 border border-border-dim hover:border-border-subtle transition-all duration-300 text-center"
        >
          <div class="absolute inset-0 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" :class="pillar.hoverBg" />
          <div class="relative">
            <div class="w-10 h-10 rounded-xl mx-auto mb-3 flex items-center justify-center" :class="pillar.iconBg">
              <Icon :name="pillar.icon" class="text-lg" :class="pillar.iconColor" />
            </div>
            <h3 class="text-sm font-semibold text-text-primary mb-1">{{ pillar.title }}</h3>
            <p class="text-[11px] text-text-tertiary leading-relaxed">{{ pillar.description }}</p>
          </div>
        </div>
      </div>

      <!-- Bottom row: two detailed cards -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
        <!-- Self-hosted + Tailscale -->
        <div class="relative rounded-2xl overflow-hidden">
          <!-- Animated border -->
          <div class="absolute inset-0 rounded-2xl p-px security-border-blue">
            <div class="w-full h-full rounded-2xl bg-[#0c0c14]" />
          </div>

          <div class="relative p-6">
            <div class="flex items-center gap-2 mb-4">
              <Icon name="lucide:shield" class="text-sky-400" />
              <h3 class="text-sm font-semibold text-text-primary">Private & Tailscale Ready</h3>
            </div>

            <p class="text-xs text-text-secondary leading-relaxed mb-5">
              The MCP server runs on <span class="text-text-primary font-medium">your machine</span>. For remote access,
              Aster auto-detects <span class="text-sky-400 font-medium">Tailscale</span> and routes traffic over an encrypted WireGuard mesh &mdash; zero config, no port forwarding.
            </p>

            <!-- Visual: network diagram -->
            <div class="flex items-center justify-between gap-2 px-2 py-3 rounded-xl bg-white/[0.02]">
              <div class="flex flex-col items-center gap-1.5 flex-1">
                <div class="w-8 h-8 rounded-lg bg-sky-500/10 flex items-center justify-center">
                  <Icon name="lucide:laptop" class="text-sm text-sky-400" />
                </div>
                <span class="text-[9px] text-text-tertiary">Your PC</span>
              </div>
              <div class="flex flex-col items-center gap-1">
                <div class="flex items-center gap-1">
                  <div class="w-6 h-px bg-gradient-to-r from-sky-500/40 to-sky-500/10" />
                  <div class="w-5 h-5 rounded-md bg-sky-500/10 flex items-center justify-center">
                    <Icon name="lucide:lock" class="text-[9px] text-sky-400/80" />
                  </div>
                  <div class="w-6 h-px bg-gradient-to-r from-sky-500/10 to-sky-500/40" />
                </div>
                <span class="text-[8px] text-sky-400/50 font-mono">WireGuard</span>
              </div>
              <div class="flex flex-col items-center gap-1.5 flex-1">
                <div class="w-8 h-8 rounded-lg bg-sky-500/10 flex items-center justify-center">
                  <Icon name="lucide:smartphone" class="text-sm text-sky-400" />
                </div>
                <span class="text-[9px] text-text-tertiary">Android</span>
              </div>
            </div>

            <div class="mt-4 space-y-2">
              <div v-for="item in tailscalePoints" :key="item.label" class="flex items-center gap-2">
                <Icon :name="item.icon" class="text-[11px] text-sky-400/70 flex-shrink-0" />
                <span class="text-[11px] text-text-secondary"><span class="text-text-primary/80 font-medium">{{ item.label }}</span> {{ item.desc }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- No Root / Safe Automation -->
        <div class="relative rounded-2xl overflow-hidden">
          <!-- Animated border -->
          <div class="absolute inset-0 rounded-2xl p-px security-border-green">
            <div class="w-full h-full rounded-2xl bg-[#0c0c14]" />
          </div>

          <div class="relative p-6">
            <div class="flex items-center gap-2 mb-4">
              <Icon name="lucide:shield-check" class="text-green-400" />
              <h3 class="text-sm font-semibold text-text-primary">No Root Required</h3>
            </div>

            <p class="text-xs text-text-secondary leading-relaxed mb-5">
              Uses the official <span class="text-text-primary font-medium">Android Accessibility Service</span> API &mdash; the same system powering screen readers.
              No rooting, no ADB hacks, no exploits. Every action is permission-gated and sandboxed.
            </p>

            <!-- Visual: permission layers -->
            <div class="space-y-1.5 mb-4">
              <div v-for="(layer, i) in permissionLayers" :key="i" class="flex items-center gap-2.5 px-3 py-2 rounded-lg" :class="layer.bg">
                <Icon :name="layer.icon" class="text-xs" :class="layer.color" />
                <span class="text-[10px] font-medium flex-1" :class="layer.textColor">{{ layer.label }}</span>
                <span class="text-[9px] font-mono" :class="layer.badgeColor">{{ layer.badge }}</span>
              </div>
            </div>

            <div class="mt-4 space-y-2">
              <div v-for="item in safetyPoints" :key="item.label" class="flex items-center gap-2">
                <Icon name="lucide:check" class="text-[11px] text-green-400/70 flex-shrink-0" />
                <span class="text-[11px] text-text-secondary"><span class="text-text-primary/80 font-medium">{{ item.label }}</span> {{ item.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Open source callout -->
      <div class="mt-3 p-4 rounded-2xl bg-surface-raised/40 border border-border-dim flex flex-col sm:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-aster/10 flex items-center justify-center">
            <Icon name="lucide:code" class="text-base text-aster" />
          </div>
          <div>
            <span class="text-sm font-semibold text-text-primary">100% Open Source</span>
            <p class="text-[11px] text-text-tertiary">MIT licensed. Read every line, audit every tool, fork and modify freely.</p>
          </div>
        </div>
        <a
          href="https://github.com/satyajiit/Aster"
          target="_blank"
          rel="noopener"
          class="flex items-center gap-2 px-4 py-2 rounded-lg border border-border-subtle text-xs text-text-secondary hover:text-aster hover:border-aster/30 transition-all whitespace-nowrap"
        >
          <Icon name="mdi:github" class="text-sm" />
          View Source
        </a>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const pillars = [
  {
    icon: 'lucide:server',
    title: 'Self-Hosted',
    description: 'Runs entirely on your machine. No cloud dependency, no data leaves your network.',
    iconBg: 'bg-aster/10',
    iconColor: 'text-aster',
    hoverBg: 'bg-gradient-to-br from-aster/[0.03] to-transparent',
  },
  {
    icon: 'lucide:eye-off',
    title: 'Zero Telemetry',
    description: 'No analytics, no tracking, no usage data collection. What you do stays with you.',
    iconBg: 'bg-violet-500/10',
    iconColor: 'text-violet-400',
    hoverBg: 'bg-gradient-to-br from-violet-500/[0.03] to-transparent',
  },
  {
    icon: 'lucide:fingerprint',
    title: 'Device Approval',
    description: 'Every new device must be manually approved from your dashboard before connecting.',
    iconBg: 'bg-amber-500/10',
    iconColor: 'text-amber-400',
    hoverBg: 'bg-gradient-to-br from-amber-500/[0.03] to-transparent',
  },
]

const tailscalePoints = [
  { icon: 'lucide:lock', label: 'Self-hosted —', desc: 'MCP server runs locally, nothing sent externally' },
  { icon: 'lucide:globe-lock', label: 'Auto-detect —', desc: 'Tailscale encryption with zero configuration' },
  { icon: 'lucide:wifi-off', label: 'No port forwarding —', desc: 'VPN mesh, no public exposure' },
]

const permissionLayers = [
  {
    icon: 'lucide:smartphone',
    label: 'Android Accessibility API',
    badge: 'official',
    bg: 'bg-green-500/[0.06]',
    color: 'text-green-400/80',
    textColor: 'text-green-300/70',
    badgeColor: 'text-green-400/40',
  },
  {
    icon: 'lucide:shield-check',
    label: 'Permission-gated access',
    badge: 'required',
    bg: 'bg-green-500/[0.04]',
    color: 'text-green-400/60',
    textColor: 'text-green-300/50',
    badgeColor: 'text-green-400/30',
  },
  {
    icon: 'lucide:box',
    label: 'Sandboxed execution',
    badge: 'isolated',
    bg: 'bg-green-500/[0.02]',
    color: 'text-green-400/40',
    textColor: 'text-green-300/35',
    badgeColor: 'text-green-400/20',
  },
]

const safetyPoints = [
  { label: 'Foreground service —', desc: 'always-visible notification for transparency' },
  { label: 'Explicit consent —', desc: 'every capability requires user permission' },
  { label: 'Open source —', desc: 'fully auditable MIT-licensed codebase' },
]
</script>

<style scoped>
.security-border-blue {
  background: linear-gradient(
    135deg,
    rgba(56, 189, 248, 0.15) 0%,
    rgba(56, 189, 248, 0.03) 40%,
    rgba(56, 189, 248, 0.01) 60%,
    rgba(56, 189, 248, 0.08) 100%
  );
}

.security-border-green {
  background: linear-gradient(
    135deg,
    rgba(74, 222, 128, 0.15) 0%,
    rgba(74, 222, 128, 0.03) 40%,
    rgba(74, 222, 128, 0.01) 60%,
    rgba(74, 222, 128, 0.08) 100%
  );
}
</style>

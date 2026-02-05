<template>
  <section class="relative py-28 px-6">
    <div class="max-w-5xl mx-auto">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <!-- Tailscale / Private Server -->
        <div class="relative p-7 rounded-2xl bg-surface-raised border border-sky-500/10 overflow-hidden group">
          <!-- Subtle glow -->
          <div class="absolute -top-20 -right-20 w-60 h-60 rounded-full bg-sky-500/[0.06] blur-3xl pointer-events-none" />

          <div class="relative">
            <div class="flex items-center gap-3 mb-4">
              <div class="w-11 h-11 rounded-xl bg-gradient-to-br from-sky-500/20 to-blue-500/20 border border-sky-500/15 flex items-center justify-center">
                <Icon name="lucide:shield" class="text-xl text-sky-400" />
              </div>
              <div>
                <h3 class="text-base font-semibold text-text-primary">Private & Secure by Default</h3>
                <span class="text-xs text-sky-400/80 font-medium">Tailscale Ready</span>
              </div>
            </div>

            <p class="text-sm text-text-secondary leading-relaxed mb-5">
              Aster runs on <span class="text-text-primary font-medium">your own server</span> &mdash; no cloud, no third-party relay.
              Your device data never leaves your network. With
              <span class="text-sky-400 font-medium">Tailscale</span> integration, access your device remotely over an encrypted WireGuard mesh &mdash; no port forwarding, no public exposure.
            </p>

            <div class="space-y-2.5">
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:lock" class="text-sky-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">Self-hosted</span> &mdash; the MCP server runs locally on your machine, nothing is sent externally</span>
              </div>
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:globe-lock" class="text-sky-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">Tailscale auto-detect</span> &mdash; encrypted remote access with zero configuration</span>
              </div>
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:wifi-off" class="text-sky-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">No port forwarding</span> &mdash; works over Tailscale VPN without exposing any ports</span>
              </div>
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:user-check" class="text-sky-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">Device approval</span> &mdash; new devices require manual approval from the dashboard</span>
              </div>
            </div>
          </div>
        </div>

        <!-- No Root / Safe Automation -->
        <div class="relative p-7 rounded-2xl bg-surface-raised border border-green-500/10 overflow-hidden group">
          <!-- Subtle glow -->
          <div class="absolute -top-20 -left-20 w-60 h-60 rounded-full bg-green-500/[0.06] blur-3xl pointer-events-none" />

          <div class="relative">
            <div class="flex items-center gap-3 mb-4">
              <div class="w-11 h-11 rounded-xl bg-gradient-to-br from-green-500/20 to-emerald-500/20 border border-green-500/15 flex items-center justify-center">
                <Icon name="lucide:shield-check" class="text-xl text-green-400" />
              </div>
              <div>
                <h3 class="text-base font-semibold text-text-primary">No Root Required</h3>
                <span class="text-xs text-green-400/80 font-medium">Standard Android APIs</span>
              </div>
            </div>

            <p class="text-sm text-text-secondary leading-relaxed mb-5">
              Aster uses the official <span class="text-text-primary font-medium">Android Accessibility Service</span> API &mdash; the same system used by screen readers and assistive tools. No rooting, no ADB hacks, no sideloaded exploits. Clean, safe, and permission-gated automation.
            </p>

            <div class="space-y-2.5">
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:check-circle" class="text-green-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">Permission-gated</span> &mdash; every capability requires explicit user consent</span>
              </div>
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:check-circle" class="text-green-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">Foreground service</span> &mdash; visible notification ensures transparency</span>
              </div>
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:check-circle" class="text-green-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">Sandboxed execution</span> &mdash; shell commands run within the app sandbox only</span>
              </div>
              <div class="flex items-start gap-2.5">
                <Icon name="lucide:check-circle" class="text-green-400 mt-0.5 flex-shrink-0" />
                <span class="text-xs text-text-secondary"><span class="text-text-primary font-medium">Open source</span> &mdash; fully auditable MIT-licensed codebase</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

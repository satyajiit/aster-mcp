<template>
  <nav
    class="fixed top-0 left-0 right-0 z-50 transition-all duration-300"
    :class="scrolled ? 'bg-surface/80 backdrop-blur-xl border-b border-border-dim' : ''"
  >
    <div class="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
      <a href="#" class="flex items-center gap-3 group">
        <img src="/logo.png" alt="Aster" class="w-8 h-8 rounded-lg transition-transform group-hover:scale-110" />
        <span class="text-lg font-semibold tracking-tight text-text-primary">Aster</span>
      </a>

      <div class="hidden md:flex items-center gap-8 text-sm text-text-secondary">
        <a href="#features" class="hover:text-aster transition-colors">Features</a>
        <a href="#use-cases" class="hover:text-aster transition-colors">Use Cases</a>
        <a href="#how-it-works" class="hover:text-aster transition-colors">How It Works</a>
        <a href="#setup" class="hover:text-aster transition-colors">Setup</a>
        <a href="#integrations" class="hover:text-aster transition-colors">Integrations</a>
        <a href="#tools" class="hover:text-aster transition-colors">Tools</a>
      </div>

      <a
        href="https://github.com/satyajiit/aster-mcp"
        target="_blank"
        rel="noopener"
        class="flex items-center gap-2 px-4 py-2 rounded-lg bg-surface-raised border border-border-subtle text-sm text-text-secondary hover:text-text-primary hover:border-aster/40 transition-all"
      >
        <Icon name="mdi:github" class="text-lg" />
        <span class="hidden sm:inline">GitHub</span>
      </a>
    </div>
  </nav>
</template>

<script setup lang="ts">
const scrolled = ref(false)

onMounted(() => {
  const onScroll = () => {
    scrolled.value = window.scrollY > 20
  }
  window.addEventListener('scroll', onScroll, { passive: true })
  onUnmounted(() => window.removeEventListener('scroll', onScroll))
})
</script>

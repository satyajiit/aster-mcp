<template>
  <footer class="relative border-t border-border-dim py-16 px-6">
    <div class="max-w-6xl mx-auto">
      <div class="flex flex-col md:flex-row items-center justify-between gap-8">
        <!-- Left -->
        <div class="flex items-center gap-3">
          <img src="/logo.png" alt="Aster" class="w-8 h-8 rounded-lg" />
          <div>
            <span class="text-sm font-semibold text-text-primary">Aster</span>
            <p class="text-xs text-text-tertiary">AI-powered Android control via MCP</p>
          </div>
        </div>

        <!-- Links -->
        <div class="flex items-center gap-6 text-sm text-text-secondary">
          <a
            href="https://github.com/satyajiit/Aster"
            target="_blank"
            rel="noopener"
            class="hover:text-aster transition-colors flex items-center gap-1.5"
          >
            <Icon name="mdi:github" />
            GitHub
          </a>
          <a
            href="https://github.com/satyajiit/Aster/releases"
            target="_blank"
            rel="noopener"
            class="hover:text-aster transition-colors flex items-center gap-1.5"
          >
            <Icon name="lucide:download" />
            Releases
          </a>
          <a
            href="https://github.com/satyajiit/Aster/issues"
            target="_blank"
            rel="noopener"
            class="hover:text-aster transition-colors flex items-center gap-1.5"
          >
            <Icon name="lucide:circle-dot" />
            Issues
          </a>
          <a
            href="https://youtube.com/@GamesPatch"
            target="_blank"
            rel="noopener"
            class="hover:text-red-400 transition-colors flex items-center gap-1.5"
          >
            <Icon name="mdi:youtube" />
            YouTube
          </a>
        </div>
      </div>

      <!-- Bottom -->
      <div class="mt-10 pt-6 border-t border-border-dim flex flex-col sm:flex-row items-center justify-between gap-4">
        <p class="text-xs text-text-tertiary">
          MIT License &middot; Built with Nuxt, Vue & Tailwind CSS
        </p>
        <div class="flex items-center gap-4">
          <a
            href="https://www.npmjs.com/package/aster-mcp"
            target="_blank"
            rel="noopener"
            class="text-xs text-text-tertiary hover:text-aster transition-colors"
          >
            npm: aster-mcp
          </a>
          <a
            href="https://clawhub.ai/satyajiit/aster"
            target="_blank"
            rel="noopener"
            class="text-xs text-text-tertiary hover:text-aster transition-colors"
          >
            ClawHub
          </a>
          <a
            href="https://modelcontextprotocol.io"
            target="_blank"
            rel="noopener"
            class="text-xs text-text-tertiary hover:text-aster transition-colors"
          >
            Model Context Protocol
          </a>
        </div>
      </div>
    </div>
  </footer>
</template>

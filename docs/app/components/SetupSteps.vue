<template>
  <section id="setup" class="relative py-32 px-6">
    <div class="max-w-3xl mx-auto">
      <div class="text-center mb-16">
        <span class="text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block">Getting Started</span>
        <h2 class="text-3xl sm:text-4xl font-bold tracking-tight text-text-primary">
          Up and running in minutes
        </h2>
        <p class="mt-4 text-text-secondary max-w-xl mx-auto">
          Four steps to connect your Android device to your AI assistant.
        </p>
      </div>

      <!-- Steps -->
      <div class="space-y-12">
        <SetupStep
          v-for="(step, i) in steps"
          :key="i"
          :number="i + 1"
          :title="step.title"
          :description="step.description"
          :is-last="i === steps.length - 1"
        >
          <!-- Custom description for step 2: companion app with download link -->
          <p v-if="step.hasCustomDescription" class="text-sm text-text-secondary leading-relaxed -mt-1 mb-1">
            Download the latest APK from
            <a href="https://github.com/satyajiit/aster-mcp/releases" target="_blank" rel="noopener" class="text-aster hover:underline font-medium">GitHub Releases</a>
            or build from source. Grant Accessibility Service permission when prompted.
          </p>

          <div v-if="step.code" class="mt-4 rounded-xl bg-surface border border-border-dim overflow-hidden">
            <div class="flex items-center gap-2 px-4 py-2.5 border-b border-border-dim">
              <span class="w-2.5 h-2.5 rounded-full bg-red-500/60" />
              <span class="w-2.5 h-2.5 rounded-full bg-yellow-500/60" />
              <span class="w-2.5 h-2.5 rounded-full bg-green-500/60" />
              <span class="ml-2 text-xs text-text-tertiary font-mono">{{ step.filename }}</span>
            </div>
            <pre class="terminal p-4 overflow-x-auto"><code v-html="step.code" /></pre>
          </div>
        </SetupStep>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const steps = [
  {
    title: 'Install & start the MCP server',
    description: 'Install the aster-mcp package from npm globally and start the server. It launches a WebSocket on port 5987 and an MCP HTTP endpoint on port 5988.',
    filename: 'terminal',
    code: `<span class="prompt">$</span> <span class="text-text-primary">npm install -g aster-mcp</span>
<span class="prompt">$</span> <span class="text-text-primary">aster start</span>
<span class="comment">
# Server running:
#   WebSocket  → ws://192.168.1.x:5987
#   MCP HTTP   → http://localhost:5988/mcp
#   Dashboard  → http://localhost:5989</span>`,
  },
  {
    title: 'Install the Android companion app',
    description: '',
    hasCustomDescription: true,
    filename: 'permissions',
    code: `<span class="comment"># Required permissions:</span>
<span class="text-aster">✓</span> <span class="text-text-secondary">Accessibility Service</span>  <span class="comment">— UI automation</span>
<span class="text-aster">✓</span> <span class="text-text-secondary">Notification Access</span>   <span class="comment">— read notifications</span>
<span class="text-aster">✓</span> <span class="text-text-secondary">Location</span>              <span class="comment">— GPS & geocoding</span>
<span class="text-aster">✓</span> <span class="text-text-secondary">Storage</span>               <span class="comment">— file management</span>
<span class="text-aster">✓</span> <span class="text-text-secondary">SMS & Phone</span>           <span class="comment">— messages & calls</span>`,
  },
  {
    title: 'Connect device to server',
    description: 'Open the Aster app, enter your server\'s WebSocket URL, and tap Connect. Approve the device from the web dashboard.',
  },
  {
    title: 'Configure your AI client',
    description: 'Add the Aster MCP endpoint to Claude Desktop, Claude Code, or any MCP-compatible client.',
    filename: '.mcp.json',
    code: `<span class="text-text-tertiary">{</span>
  <span class="text-aster">"mcpServers"</span><span class="text-text-tertiary">:</span> <span class="text-text-tertiary">{</span>
    <span class="text-aster">"aster"</span><span class="text-text-tertiary">:</span> <span class="text-text-tertiary">{</span>
      <span class="text-violet-400">"type"</span><span class="text-text-tertiary">:</span> <span class="string">"http"</span><span class="text-text-tertiary">,</span>
      <span class="text-violet-400">"url"</span><span class="text-text-tertiary">:</span> <span class="string">"http://localhost:5988/mcp"</span>
    <span class="text-text-tertiary">}</span>
  <span class="text-text-tertiary">}</span>
<span class="text-text-tertiary">}</span>`,
  },
]
</script>

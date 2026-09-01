import tailwindcss from '@tailwindcss/vite'

const isGitHubPages = process.env.GITHUB_PAGES === 'true';

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  // Shared Aster design system — same layer the dashboard consumes, so the two
  // surfaces cannot drift. Relative path: docs/ and mcp/dashboard/ each carry
  // their own lockfile and there is no root workspace to link through.
  extends: ['../packages/aster-ui'],

  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  modules: ['@nuxt/fonts', '@nuxt/icon'],

  css: ['~/assets/css/main.css'],

  vite: {
    plugins: [tailwindcss()],
  },

  app: {
    head: {
      title: 'Aster — Your AI CoPilot on Mobile or Give Your AI Its Own Phone',
      htmlAttrs: { lang: 'en', 'data-theme': 'dark' },
      meta: [
        // Primary meta
        { name: 'description', content: 'Aster connects any Android device to AI assistants via MCP. Your AI CoPilot on mobile — or give your AI a dedicated device and let it call, text, and act on its own. 49 tools, open source, self-hosted.' },
        { name: 'keywords', content: 'aster, android ai copilot, mcp server, model context protocol, ai phone, ai assistant android, claude android, openclaw, clawdbot, moltbot, clawbot, clawhub, ai automation, ai copilot mobile, give ai a phone, ai own phone, natural language android, ai calls you, ai own device' },
        { name: 'author', content: 'Satyajit Pradhan' },
        { name: 'robots', content: 'index, follow' },
        { name: 'theme-color', content: '#06060c' },

        // Open Graph
        { property: 'og:type', content: 'website' },
        { property: 'og:site_name', content: 'Aster' },
        { property: 'og:title', content: 'Aster — Your AI CoPilot on Mobile or Give Your AI Its Own Phone' },
        { property: 'og:description', content: 'Connect any Android to AI assistants like Claude, OpenClaw, or MoltBot. Your AI CoPilot on mobile — or give your AI a dedicated device that calls, texts, and acts for you. Open source, self-hosted, 49 MCP tools.' },
        { property: 'og:image', content: 'https://aster.matterwardlabs.com/og-card.png' },
        { property: 'og:url', content: 'https://aster.matterwardlabs.com' },
        { property: 'og:locale', content: 'en_US' },

        // Twitter Card
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:title', content: 'Aster — Your AI CoPilot on Mobile or Give Your AI Its Own Phone' },
        { name: 'twitter:description', content: 'Connect any Android to AI assistants. Your AI CoPilot on mobile — or give your AI a dedicated device. 49 tools, open source, self-hosted.' },
        { name: 'twitter:image', content: 'https://aster.matterwardlabs.com/og-card.png' },
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'icon', type: 'image/png', sizes: '32x32', href: '/favicon-32x32.png' },
        { rel: 'icon', type: 'image/png', sizes: '16x16', href: '/favicon-16x16.png' },
        { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
        { rel: 'canonical', href: 'https://aster.matterwardlabs.com' },
      ],
      script: [
        {
          type: 'application/ld+json',
          innerHTML: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'SoftwareApplication',
            name: 'Aster',
            description: 'Connect any Android device to AI assistants via the Model Context Protocol. Your AI CoPilot on mobile or give your AI its own dedicated device.',
            url: 'https://aster.matterwardlabs.com',
            applicationCategory: 'DeveloperApplication',
            operatingSystem: 'Android',
            offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' },
            author: { '@type': 'Person', name: 'Satyajit Pradhan', url: 'https://github.com/satyajiit' },
            license: 'https://opensource.org/licenses/MIT',
            downloadUrl: 'https://www.npmjs.com/package/aster-mcp',
            screenshot: 'https://aster.matterwardlabs.com/screenshots/app/device-dashboard.jpg',
          }),
        },
      ],
    },
  },

  nitro: {
    preset: isGitHubPages ? 'github-pages' : undefined,
    prerender: {
      crawlLinks: true,
      routes: ['/'],
    },
  },
})

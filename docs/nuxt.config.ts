import tailwindcss from '@tailwindcss/vite'

const isGitHubPages = process.env.GITHUB_PAGES === 'true';

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  modules: ['@nuxt/fonts', '@nuxt/icon'],

  css: ['~/assets/css/main.css'],

  vite: {
    plugins: [tailwindcss()],
  },

  app: {
    head: {
      title: 'Aster — Control Android with AI or Give Your AI Its Own Phone',
      htmlAttrs: { lang: 'en' },
      meta: [
        // Primary meta
        { name: 'description', content: 'Aster connects any Android device to AI assistants via MCP. Control your phone remotely with natural language — or give your AI a dedicated device and let it call, text, and act on its own. 40+ tools, open source, self-hosted.' },
        { name: 'keywords', content: 'aster, android ai control, mcp server, model context protocol, ai phone, ai assistant android, claude android, openclaw, clawdbot, moltbot, clawbot, clawhub, ai automation, android remote control, give ai a phone, ai own phone, natural language android, ai calls you, ai own device' },
        { name: 'author', content: 'Satyajit Pradhan' },
        { name: 'robots', content: 'index, follow' },
        { name: 'theme-color', content: '#2dd4bf' },

        // Open Graph
        { property: 'og:type', content: 'website' },
        { property: 'og:site_name', content: 'Aster' },
        { property: 'og:title', content: 'Aster — Control Android with AI or Give Your AI Its Own Phone' },
        { property: 'og:description', content: 'Connect any Android to AI assistants like Claude, OpenClaw, or MoltBot. Control your phone — or give your AI a dedicated device that calls, texts, and acts for you. Open source, self-hosted, 40+ MCP tools.' },
        { property: 'og:image', content: 'https://aster.theappstack.in/logo.png' },
        { property: 'og:url', content: 'https://aster.theappstack.in' },
        { property: 'og:locale', content: 'en_US' },

        // Twitter Card
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:title', content: 'Aster — Control Android with AI or Give Your AI Its Own Phone' },
        { name: 'twitter:description', content: 'Connect any Android to AI assistants. Control your phone remotely — or give your AI a dedicated device. 40+ tools, open source, self-hosted.' },
        { name: 'twitter:image', content: 'https://aster.theappstack.in/logo.png' },
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'icon', type: 'image/png', sizes: '32x32', href: '/favicon-32x32.png' },
        { rel: 'icon', type: 'image/png', sizes: '16x16', href: '/favicon-16x16.png' },
        { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
        { rel: 'canonical', href: 'https://aster.theappstack.in' },
      ],
      script: [
        {
          type: 'application/ld+json',
          innerHTML: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'SoftwareApplication',
            name: 'Aster',
            description: 'Connect any Android device to AI assistants via the Model Context Protocol. Control your phone remotely or give your AI its own dedicated device.',
            url: 'https://aster.theappstack.in',
            applicationCategory: 'DeveloperApplication',
            operatingSystem: 'Android',
            offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' },
            author: { '@type': 'Person', name: 'Satyajit Pradhan', url: 'https://github.com/satyajiit' },
            license: 'https://opensource.org/licenses/MIT',
            softwareVersion: '0.1.10',
            downloadUrl: 'https://www.npmjs.com/package/aster-mcp',
            screenshot: 'https://aster.theappstack.in/logo.png',
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

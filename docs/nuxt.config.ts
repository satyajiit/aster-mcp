import tailwindcss from '@tailwindcss/vite'

const isGitHubPages = process.env.GITHUB_PAGES === 'true';
const baseURL = isGitHubPages ? '/aster-mcp/' : '/';

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
    baseURL,
    head: {
      title: 'Aster - AI-Powered Android Control',
      meta: [
        { name: 'description', content: 'Control your Android device with natural language through AI assistants like Claude. Full UI automation, media search, file management & more.' },
        { property: 'og:title', content: 'Aster - AI-Powered Android Control' },
        { property: 'og:description', content: 'Bridge your Android device to AI assistants via the Model Context Protocol.' },
        { property: 'og:image', content: `${baseURL}logo.png` },
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

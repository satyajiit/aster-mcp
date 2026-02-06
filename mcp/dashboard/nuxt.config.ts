export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },

  devServer: {
    port: 5989,
  },

  modules: ['@nuxt/fonts', '@nuxt/icon'],

  icon: {
    provider: 'server',
    serverBundle: {
      collections: ['ph', 'lucide', 'mdi'],
    },
  },

  components: [
    { path: '~/components/common', pathPrefix: false },
    { path: '~/components/device', pathPrefix: false },
    { path: '~/components/ui', pathPrefix: false },
    { path: '~/components/files', pathPrefix: false },
  ],

  fonts: {
    families: [
      { name: 'JetBrains Mono', provider: 'google' },
      { name: 'Inter', provider: 'google' },
    ],
  },

  css: ['~/assets/css/main.css'],

  vite: {
    plugins: [
      // @ts-expect-error tailwindcss vite plugin
      (await import('@tailwindcss/vite')).default(),
    ],
  },

  app: {
    head: {
      title: 'Aster Dashboard',
      meta: [
        { name: 'description', content: 'Aster MCP Server Dashboard' },
        { name: 'theme-color', content: '#0a0a0a' },
      ],
    },
  },

  routeRules: {
    '/api/**': { proxy: `http://localhost:${process.env.API_PORT || '5988'}/api/**` },
    '/mcp': { proxy: `http://localhost:${process.env.API_PORT || '5988'}/mcp` },
  },

  runtimeConfig: {
    public: {
      apiUrl: process.env.API_URL || '',
    },
  },
});

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));

// The footer used to hardcode "ASTER v0.1.0" while the package was at 0.1.14.
// Read the real version at build time so it cannot drift again.
const serverPkg = JSON.parse(readFileSync(join(here, '..', 'package.json'), 'utf-8')) as {
  version: string;
};

export default defineNuxtConfig({
  // Shared Aster design system: tokens, primitives and brand lockup.
  // Relative path, not a workspace dep — dashboard/, docs/ and mcp/ each carry
  // their own lockfile and there is no root workspace to link through.
  extends: ['../../packages/aster-ui'],

  compatibilityDate: '2025-11-01',
  devtools: { enabled: true },

  devServer: {
    port: 5989,
  },

  modules: ['@nuxt/fonts', '@nuxt/icon'],

  icon: {
    // Inline exactly the icons this app references into the client bundle.
    //
    // The server provider is not usable here: it serves from /api/_nuxt_icon,
    // and routeRules proxies /api/** to the Aster server, so every icon 502'd.
    // Moving the endpoint elsewhere still leaves icons behind an HTTP round
    // trip on a dashboard that is meant to run on a LAN with no internet. A
    // client bundle resolves offline and covers dynamic :name bindings, which
    // scanning cannot see.
    provider: 'none',
    clientBundle: {
      icons: [
        'ph:address-book',
        'ph:alarm',
        'ph:android-logo',
        'ph:apple-logo',
        'ph:arrow-arc-left',
        'ph:arrow-left',
        'ph:arrow-square-out',
        'ph:arrow-up',
        'ph:arrows-clockwise',
        'ph:bell',
        'ph:bell-ringing',
        'ph:bell-slash',
        'ph:brackets-curly',
        'ph:broadcast',
        'ph:bug',
        'ph:calendar-plus',
        'ph:camera',
        'ph:caret-down',
        'ph:caret-up',
        'ph:cell-signal-full',
        'ph:chat-circle',
        'ph:chat-circle-text',
        'ph:chat-teardrop-text',
        'ph:check',
        'ph:check-circle',
        'ph:circle-half',
        'ph:clipboard',
        'ph:clipboard-text',
        'ph:clock',
        'ph:code',
        'ph:copy',
        'ph:cpu',
        'ph:crosshair',
        'ph:cursor-click',
        'ph:database',
        'ph:device-mobile',
        'ph:device-mobile-slash',
        'ph:devices',
        'ph:download-simple',
        'ph:factory',
        'ph:file',
        'ph:file-pdf',
        'ph:file-text',
        'ph:file-zip',
        'ph:files',
        'ph:film-strip',
        'ph:fingerprint',
        'ph:folder',
        'ph:folder-open',
        'ph:folder-plus',
        'ph:gear-six',
        'ph:globe',
        'ph:hard-drive',
        'ph:hash',
        'ph:hourglass',
        'ph:house',
        'ph:image',
        'ph:image-broken',
        'ph:image-square',
        'ph:info',
        'ph:key',
        'ph:list',
        'ph:list-dashes',
        'ph:list-magnifying-glass',
        'ph:lock',
        'ph:magnifying-glass',
        'ph:map-pin',
        'ph:map-pin-line',
        'ph:megaphone',
        'ph:monitor',
        'ph:moon',
        'ph:music-note',
        'ph:package',
        'ph:paper-plane-tilt',
        'ph:pencil',
        'ph:phone-call',
        'ph:phone-incoming',
        'ph:play',
        'ph:plug',
        'ph:plugs',
        'ph:plugs-connected',
        'ph:plus',
        'ph:power',
        'ph:shield-check',
        'ph:speaker-high',
        'ph:speaker-simple-high',
        'ph:speaker-slash',
        'ph:squares-four',
        'ph:stack',
        'ph:star',
        'ph:stop',
        'ph:sun',
        'ph:tag',
        'ph:terminal',
        'ph:terminal-window',
        'ph:timer',
        'ph:translate',
        'ph:trash',
        'ph:tray',
        'ph:tree-structure',
        'ph:user',
        'ph:warning',
        'ph:warning-circle',
        'ph:wifi-high',
        'ph:wifi-slash',
        'ph:wrench',
        'ph:x',
        'ph:x-circle',
      ],
    },
  },

  components: [{ path: '~/components', pathPrefix: false }],

  fonts: {
    families: [
      // Instrument Sans is the Aster brand face, bundled in the Android app as
      // res/font/instrument_sans_*.ttf. JetBrains Mono is for code and ids only.
      { name: 'Instrument Sans', provider: 'google' },
      { name: 'JetBrains Mono', provider: 'google' },
    ],
  },

  css: ['~/assets/css/main.css'],

  vite: {
    plugins: [
      // @ts-expect-error tailwindcss vite plugin
      (await import('@tailwindcss/vite')).default(),
    ],
  },

  runtimeConfig: {
    public: {
      apiUrl: process.env.API_URL || '',
      version: serverPkg.version,
    },
  },

  app: {
    head: {
      // Must be the string form. Nuxt serialises app.head into the build
      // payload, so a function here is silently dropped and every page
      // renders a bare title with no product name.
      titleTemplate: '%s · Aster',
      htmlAttrs: { lang: 'en' },
      meta: [
        { name: 'description', content: 'Control and observe your Android devices over MCP.' },
        { name: 'theme-color', content: '#06060c' },
        { name: 'color-scheme', content: 'dark light' },
        { property: 'og:title', content: 'Aster Dashboard' },
        { property: 'og:description', content: 'Control and observe your Android devices over MCP.' },
        { property: 'og:type', content: 'website' },
      ],
      link: [
        { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' },
        { rel: 'apple-touch-icon', href: '/icon-192.png' },
      ],
    },
  },

  routeRules: {
    '/api/**': { proxy: `http://localhost:${process.env.API_PORT || '5988'}/api/**` },
    '/mcp': { proxy: `http://localhost:${process.env.API_PORT || '5988'}/mcp` },
  },
});

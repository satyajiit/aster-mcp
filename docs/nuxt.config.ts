import tailwindcss from '@tailwindcss/vite'
import { ROUTES } from './app/data/routes'

const ICONS = [
  "lucide:alarm-clock",
  "lucide:arrow-right",
  "lucide:arrow-up-right",
  "lucide:baby",
  "lucide:battery-full",
  "lucide:battery-low",
  "lucide:bell",
  "lucide:bell-ring",
  "lucide:book-open",
  "lucide:bot",
  "lucide:box",
  "lucide:brain",
  "lucide:calendar",
  "lucide:camera",
  "lucide:car",
  "lucide:check-circle",
  "lucide:chevron-down",
  "lucide:chevron-right",
  "lucide:circle-dot",
  "lucide:clapperboard",
  "lucide:code",
  "lucide:coffee",
  "lucide:compass",
  "lucide:copy",
  "lucide:corner-up-left",
  "lucide:cpu",
  "lucide:download",
  "lucide:external-link",
  "lucide:file-text",
  "lucide:fingerprint",
  "lucide:folder",
  "lucide:folder-open",
  "lucide:folder-search",
  "lucide:hand",
  "lucide:hard-drive",
  "lucide:image",
  "lucide:images",
  "lucide:info",
  "lucide:list",
  "lucide:list-checks",
  "lucide:mail",
  "lucide:map",
  "lucide:map-pin",
  "lucide:menu",
  "lucide:message-circle",
  "lucide:message-square",
  "lucide:message-square-quote",
  "lucide:mic",
  "lucide:monitor",
  "lucide:monitor-smartphone",
  "lucide:moon",
  "lucide:octagon-x",
  "lucide:package",
  "lucide:pause",
  "lucide:phone",
  "lucide:phone-call",
  "lucide:phone-incoming",
  "lucide:phone-off",
  "lucide:phone-outgoing",
  "lucide:plane",
  "lucide:play",
  "lucide:plug",
  "lucide:plus",
  "lucide:radio",
  "lucide:radio-tower",
  "lucide:scan",
  "lucide:search",
  "lucide:send",
  "lucide:server",
  "lucide:shield-ban",
  "lucide:shield-check",
  "lucide:signal",
  "lucide:smartphone",
  "lucide:smartphone-charging",
  "lucide:smile",
  "lucide:sparkles",
  "lucide:sun",
  "lucide:terminal",
  "lucide:terminal-square",
  "lucide:trash-2",
  "lucide:unlock",
  "lucide:user",
  "lucide:user-round-plus",
  "lucide:users",
  "lucide:vibrate",
  "lucide:video",
  "lucide:volume-2",
  "lucide:webhook",
  "lucide:wifi",
  "lucide:x",
  "lucide:zap",
  "mdi:github",
  "mdi:youtube",
  "ph:check",
  "ph:check-circle",
  "ph:copy",
  "ph:info",
  "ph:tray",
  "ph:warning-circle",
  "ph:x"
]

const isGitHubPages = process.env.GITHUB_PAGES === 'true';

/**
 * The build date, resolved once and shared by every surface that states one.
 *
 * Three places used to have to agree and had no way to: the sitemap's <lastmod>,
 * the "Last generated" line in the markdown twins, and the structured data —
 * which carried no date at all, so the HTML a crawler reads had no recency
 * signal while the twins and the sitemap did. This is the single source; the
 * generator reads the same env var, and useRouteSeo() reads it from
 * runtimeConfig.public.
 *
 * SOURCE_DATE_EPOCH is honoured so a reproducible build produces a reproducible
 * date rather than whenever CI happened to run.
 */
const BUILD_DATE = (process.env.SOURCE_DATE_EPOCH
  ? new Date(Number(process.env.SOURCE_DATE_EPOCH) * 1000)
  : new Date()
).toISOString().slice(0, 10);

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  // Shared Aster design system — same layer the dashboard consumes, so the two
  // surfaces cannot drift. Relative path: docs/ and mcp/dashboard/ each carry
  // their own lockfile and there is no root workspace to link through.
  extends: ['../packages/aster-ui'],

  compatibilityDate: '2025-07-15',

  runtimeConfig: {
    public: { buildDate: BUILD_DATE },
  },

  devtools: { enabled: false },
  modules: ['@nuxt/fonts', '@nuxt/icon'],

  css: ['~/assets/css/main.css'],

  vite: {
    plugins: [tailwindcss()],
  },

  // The tokens declare weights 500/600/700 and the components request them, but
  // the module's default is 400 only — every bold on the site was synthesised by
  // the browser. Ask for the weights we actually use. docs-only key; cannot
  // affect mcp/dashboard.
  fonts: {
    defaults: {
      weights: [400, 500, 600, 700],
      styles: ['normal'],
      subsets: ['latin'],
    },
  },

  // mode 'svg' makes the icon an inline <svg> in the SERVER-RENDERED HTML, so
  // icons survive with JavaScript off and are present for fetchers that read
  // raw HTML. The default 'css' mode emits a bare <span class="i-lucide:...">
  // that is blank until the client hydrates.
  //
  // Previously this site shipped no icon data at all: @nuxt/icon had no server
  // route on GitHub Pages and fell back to fetching every icon from
  // api.iconify.design at runtime — a third-party request on a page that
  // advertises "no vendor relay", and a hard dependency on someone else's CDN.
  //
  // ICONS is the exhaustive list of every icon referenced anywhere in docs/app
  // and in the shared packages/aster-ui layer, including the ones bound
  // dynamically from app/data/*.ts that a static scan cannot see. Enumerating
  // them is what makes the build hermetic: provider 'none' means a name missing
  // from this list fails loudly at build time instead of silently reaching for
  // the network. If you add an icon, add it here (or run the census in
  // docs/audit/LANDING-AUDIT.md).
  icon: {
    mode: 'svg',
    provider: 'none',
    serverBundle: { collections: ['lucide', 'mdi', 'ph'] },
    clientBundle: { icons: ICONS, includeCustomCollections: true, sizeLimitKb: 1024 },
  },

  app: {
    // Per-route titles come from useRouteSeo(); this appends the site name once.
    head: {
      titleTemplate: (chunk?: string) =>
        chunk && chunk !== 'Aster' ? `${chunk} · Aster` : 'Aster — AI copilot for Android',
      htmlAttrs: { lang: 'en', 'data-theme': 'dark' },
      meta: [
        // SITE-WIDE ONLY. Anything route-specific — title, description,
        // canonical, og:url, og:title, og:description — is set per page by
        // useRouteSeo() in app/composables/useRouteSeo.ts.
        //
        // Do NOT put a canonical or an og:url back here. A global canonical
        // pointing at the origin tells search engines that every other route is
        // a duplicate of the homepage and should be dropped from the index.
        { name: 'author', content: 'Satyajit Pradhan' },
        { name: 'robots', content: 'index, follow, max-image-preview:large, max-snippet:-1' },
        { name: 'theme-color', content: '#06060c' },
        { property: 'og:site_name', content: 'Aster' },
      ],
      link: [
        { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
        { rel: 'icon', type: 'image/png', sizes: '32x32', href: '/favicon-32x32.png' },
        { rel: 'icon', type: 'image/png', sizes: '16x16', href: '/favicon-16x16.png' },
        { rel: 'apple-touch-icon', sizes: '180x180', href: '/apple-touch-icon.png' },
      ],
    },
  },

  nitro: {
    preset: isGitHubPages ? 'github-pages' : undefined,
    prerender: {
      // Enumerated explicitly, NOT left to crawlLinks. A route that is neither
      // listed here nor reachable by a crawled <a href> is served as the 3.8 KB
      // soft-404 shell — which, being rendered from the same app, would carry an
      // indexable robots tag. crawlLinks stays on as a belt-and-braces net.
      crawlLinks: true,
      // '/404' is app/pages/404.vue. Prerendering it to 404/index.html is step
      // one of getting a real 404: the hook below then copies it over the SPA
      // shell the github-pages preset writes into 404.html (1.2 KB, no title,
      // no canonical, no visible text, and carrying the site-wide
      // `index, follow` robots tag). Prerendering straight to '/404.html' does
      // NOT work — the preset's fallback write lands last and overwrites it.
      routes: [...ROUTES.map((r) => r.path), '/404'],
      failOnError: true,
    },
  },

  hooks: {
    // Generators are wired to a nitro hook, not to an npm script: CI runs
    // `pnpm nuxt generate` (.github/workflows/deploy-docs.yml), which never
    // executes a package.json pre/post hook. A generator chained there would
    // run locally and silently never run in production.
    async 'nitro:build:public-assets'(nitro) {
      const publicDir = nitro.options.output.publicDir
      const { writeMachineReadable } = await import('./scripts/generate-machine-readable')
      await writeMachineReadable(publicDir)

      // Runs AFTER the generator so it can also scan what was written for
      // "undefined" and friends. Throws on drift, which fails the build.
      const { verifyFacts } = await import('./scripts/verify-facts')
      await verifyFacts(publicDir)

      // The github-pages preset emits a 200.html SPA fallback — a Netlify and
      // Surge convention that GitHub Pages never reads. Left in place it is an
      // indexable, titleless, zero-content URL that returns HTTP 200.
      const { rm } = await import('node:fs/promises')
      const { join } = await import('node:path')
      await rm(join(publicDir, '200.html'), { force: true })

      // Put the prerendered 404 where GitHub Pages looks for it, then remove the
      // directory so /404/ never becomes a second live URL. This runs after the
      // preset's fallback write, which is the whole reason it works.
      const { readFile, writeFile } = await import('node:fs/promises')
      let notFound = await readFile(join(publicDir, '404', 'index.html'), 'utf8')

      // Strip the Nuxt runtime from the 404 and only from the 404.
      //
      // Two reasons, both measured. The page was built at route /404, so its
      // hydration bundle fetches /404/_payload.json — a file this hook deletes
      // two lines down, giving three failed requests and two console errors on
      // every 404 view, each answered with the 22 KB 404.html itself. And once
      // the router booted it replaced the address bar with /404, erasing the URL
      // the visitor actually mistyped so they could neither see nor correct it.
      //
      // Nothing on this page needs JavaScript: it is static prose and eight
      // NuxtLinks, which SSR to ordinary <a href>. Removing the scripts makes it
      // a plain document that renders identically, navigates correctly, keeps the
      // requested URL in the bar and fetches nothing. The stylesheet <link> tags
      // are left alone, so it still looks like the rest of the site.
      const before = notFound.length
      notFound = notFound
        .replace(/<script\b[^>]*>[\s\S]*?<\/script>/g, '')
        .replace(/<link\b[^>]*\brel="(?:modulepreload|preload|prefetch)"[^>]*>/g, '')
      await writeFile(join(publicDir, '404.html'), notFound, 'utf8')
      console.log(`[404] prerendered branded 404.html, runtime stripped (${before} -> ${notFound.length} bytes)`)

      await rm(join(publicDir, '404'), { recursive: true, force: true })
    },
  },
})

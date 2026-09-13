<template>
  <footer class="relative border-t border-border-dim py-14 px-6">
    <div class="max-w-6xl mx-auto">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-10">
        <!-- Identity -->
        <div class="lg:col-span-1">
          <NuxtLink to="/" class="inline-flex items-center gap-3 rounded-lg py-1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster">
            <img src="/logo.png" alt="" width="32" height="32" class="w-8 h-8 rounded-lg" />
            <span class="text-sm font-semibold text-text-primary">{{ SITE.name }}</span>
            <span class="sr-only">— home</span>
          </NuxtLink>
          <p class="mt-3 text-xs text-text-tertiary leading-relaxed max-w-[24ch]">
            {{ SITE.tagline }}.
          </p>
        </div>

        <!-- Every route, from every page. Before the split the footer carried
             no internal links at all, so a crawler that landed deep had no path
             back to the rest of the site. -->
        <nav aria-labelledby="footer-pages">
          <h2 id="footer-pages" class="text-[11px] font-semibold uppercase tracking-[0.15em] text-text-tertiary mb-3">Pages</h2>
          <ul class="space-y-0.5 text-sm">
            <li>
              <NuxtLink
                to="/"
                class="inline-flex items-center min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
                :aria-current="isActive('/') ? 'page' : undefined"
              >Home</NuxtLink>
            </li>
            <li v-for="route in NAV_ROUTES" :key="route.path">
              <NuxtLink
                :to="href(route.path)"
                class="inline-flex items-center min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
                :aria-current="isActive(route.path) ? 'page' : undefined"
              >{{ route.nav }}</NuxtLink>
            </li>
          </ul>
        </nav>

        <!-- Project -->
        <nav aria-labelledby="footer-project">
          <h2 id="footer-project" class="text-[11px] font-semibold uppercase tracking-[0.15em] text-text-tertiary mb-3">Project</h2>
          <ul class="space-y-0.5 text-sm">
            <li v-for="item in PROJECT_LINKS" :key="item.href">
              <a
                :href="item.href"
                target="_blank"
                rel="noopener"
                class="inline-flex items-center gap-1.5 min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              >
                <Icon :name="item.icon" aria-hidden="true" />
                {{ item.label }}
              </a>
            </li>
          </ul>
        </nav>

        <!-- Machine-readable + related -->
        <nav aria-labelledby="footer-machine">
          <h2 id="footer-machine" class="text-[11px] font-semibold uppercase tracking-[0.15em] text-text-tertiary mb-3">For machines</h2>
          <ul class="space-y-0.5 text-sm">
            <li>
              <a
                href="/llms.txt"
                class="inline-flex items-center gap-1.5 min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              >
                <Icon name="lucide:file-text" aria-hidden="true" />
                llms.txt
              </a>
            </li>
            <li>
              <a
                href="/llms-full.txt"
                class="inline-flex items-center gap-1.5 min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              >
                <Icon name="lucide:file-text" aria-hidden="true" />
                llms-full.txt
              </a>
            </li>
            <li>
              <a
                href="/sitemap.xml"
                class="inline-flex items-center gap-1.5 min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              >
                <Icon name="lucide:map" aria-hidden="true" />
                sitemap.xml
              </a>
            </li>
            <li>
              <a
                :href="LINKS.mcpSpec"
                target="_blank"
                rel="noopener"
                class="inline-flex items-center gap-1.5 min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              >
                <Icon name="lucide:book-open" aria-hidden="true" />
                Model Context Protocol
              </a>
            </li>
            <li>
              <a
                :href="LINKS.openally"
                target="_blank"
                rel="noopener"
                class="inline-flex items-center gap-1.5 min-h-8 text-text-secondary hover:text-aster transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
              >
                <img src="/openally-mark.svg" alt="" width="16" height="16" class="w-4 h-4" />
                OpenAlly
              </a>
            </li>
          </ul>
        </nav>
      </div>

      <div class="mt-10 pt-6 border-t border-border-dim flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <p class="text-xs text-text-tertiary">
          {{ FACTS.license }} licence &middot; server v{{ FACTS.serverVersion }} &middot; app v{{ FACTS.appVersion }} &middot;
          updated <time :datetime="buildDate">{{ buildDate }}</time> &middot;
          built by
          <a :href="SITE.authorUrl" target="_blank" rel="noopener" class="text-text-secondary hover:text-aster transition-colors">{{ SITE.author }}</a>
        </p>
        <p class="text-xs text-text-tertiary">
          A <a :href="SITE.publisherUrl" target="_blank" rel="noopener" class="text-text-secondary hover:text-aster transition-colors">{{ SITE.publisher }}</a> project
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { NAV_ROUTES, href } from '~/data/routes'
import { FACTS, LINKS, SITE } from '~/data/site'

const route = useRoute()

/** Same build date the sitemap, the twins and the JSON-LD use. */
const buildDate = useRuntimeConfig().public.buildDate as string

function normalise(path: string): string {
  if (path.length > 1 && path.endsWith('/')) return path.slice(0, -1)
  return path
}

function isActive(path: string): boolean {
  return normalise(route.path) === normalise(path)
}

const PROJECT_LINKS = [
  { href: LINKS.repo, label: 'GitHub', icon: 'mdi:github' },
  { href: LINKS.releases, label: 'Releases', icon: 'lucide:download' },
  { href: LINKS.issues, label: 'Issues', icon: 'lucide:circle-dot' },
  { href: LINKS.npm, label: `npm: ${FACTS.npmPackage}`, icon: 'lucide:package' },
  { href: LINKS.clawhub, label: 'ClawHub', icon: 'lucide:box' },
  { href: LINKS.youtube, label: 'YouTube', icon: 'mdi:youtube' },
]
</script>

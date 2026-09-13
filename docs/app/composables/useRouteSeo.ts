import { SITE } from '~/data/site'
import { href } from '~/data/routes'

export interface RouteSeoInput {
  /** Page title WITHOUT the site suffix — titleTemplate in nuxt.config adds it. */
  title: string
  /** Meta description. Keep at or under 160 characters. */
  description: string
  /** Route path with a leading slash and no trailing slash, e.g. '/tools'. '/' for home. */
  path: string
  /** Plain-text twin an LLM can fetch, e.g. '/tools.md'. */
  textTwin?: string
  /** Per-route social card. Falls back to the shared one. */
  image?: string
  /** JSON-LD objects for this route. BreadcrumbList is added automatically. */
  schema?: Record<string, unknown>[]
  /** Breadcrumb label. Omit on the home route. */
  breadcrumb?: string
}

/**
 * Node types that carry a date. A crawler or answer engine deciding whether a
 * version-bearing claim ("server v0.1.16") is current had nothing to date it
 * against: the twins printed a generation date and the sitemap carried lastmod,
 * but the HTML itself carried none. Stamped from the same build date as both, so
 * the three cannot disagree.
 */
const DATED_TYPES = new Set([
  'TechArticle',
  'Article',
  'HowTo',
  'FAQPage',
  'CollectionPage',
  'WebPage',
  // Both are CreativeWork subtypes, and these two are where the version numbers
  // live — the nodes most in need of a date to be judged against.
  'SoftwareApplication',
  'MobileApplication',
])

/**
 * Absolute, non-redirecting URL for a site-relative path.
 *
 * Routes get the trailing slash GitHub Pages 301s to (see href() in
 * app/data/routes.ts); files such as `/tools.md` are passed through untouched.
 * Canonicals, og:url and the sitemap all come through here, so they cannot
 * disagree about the form.
 */
export function abs(path: string): string {
  const p = path.startsWith('/') ? path : '/' + path
  return SITE.origin + href(p)
}

/**
 * The single place a route declares its search + answer-engine surface.
 *
 * Sets title, description, canonical, og:*, twitter:*, the plain-text-twin
 * alternate link, and JSON-LD. Every route MUST call this; a route that does
 * not inherits no canonical at all, which is worse than a wrong one.
 */
export function useRouteSeo(input: RouteSeoInput) {
  const url = abs(input.path)
  const image = input.image ?? SITE.ogImage

  useSeoMeta({
    title: input.title,
    description: input.description,
    ogType: 'website',
    ogTitle: input.title,
    ogDescription: input.description,
    ogUrl: url,
    ogImage: image,
    ogImageWidth: SITE.ogImageWidth,
    ogImageHeight: SITE.ogImageHeight,
    ogImageAlt: input.title,
    ogLocale: 'en_US',
    twitterCard: 'summary_large_image',
    twitterTitle: input.title,
    twitterDescription: input.description,
    twitterImage: image,
    twitterImageAlt: input.title,
  })

  const link: Record<string, string>[] = [{ rel: 'canonical', href: url }]
  if (input.textTwin) {
    // Signals the machine-readable twin to fetchers that look for one. Not a
    // Google ranking input; it is developer and answer-engine ergonomics.
    // No hreflang: that declares an alternate LANGUAGE/REGION version and is
    // expected to be reciprocal. The twin is the same page in another format.
    link.push({
      rel: 'alternate',
      type: 'text/markdown',
      href: abs(input.textTwin),
      title: `${input.title} (plain text)`,
    })
  }

  const buildDate = useRuntimeConfig().public.buildDate as string

  const graph: Record<string, unknown>[] = (input.schema ?? []).map((node) => {
    const type = node['@type']
    if (typeof type !== 'string' || !DATED_TYPES.has(type)) return node
    // Only fill what the page has not already said for itself.
    return { dateModified: buildDate, ...node }
  })

  if (input.breadcrumb && input.path !== '/') {
    graph.push({
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: [
        { '@type': 'ListItem', position: 1, name: 'Aster', item: abs('/') },
        { '@type': 'ListItem', position: 2, name: input.breadcrumb, item: url },
      ],
    })
  }

  useHead({
    link,
    script: graph.map((node) => ({
      type: 'application/ld+json',
      innerHTML: JSON.stringify(node),
    })),
  })
}

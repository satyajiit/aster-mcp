<template>
  <SecuritySection :faqs="SECURITY_FAQS" />
</template>

<script setup lang="ts">
/**
 * /security — the honest answer to "is this safe to install".
 *
 * The FAQ answers below are the same claims SecuritySection.vue renders, in the
 * shorter form an answer engine will quote. If a claim changes in
 * ~/data/security, change it here too: a FAQPage that contradicts the visible
 * page is worse than no FAQPage at all.
 */
import { routeByPath } from '~/data/routes'
import { SITE, LINKS, FACTS, TOOL_PREFIX } from '~/data/site'
import { PERMISSIONS, SECURITY_FAQS } from '~/data/security'

const route = routeByPath('/security')

const NOTABLE_PERMISSIONS = new Set([
  'BIND_ACCESSIBILITY_SERVICE',
  'BIND_NOTIFICATION_LISTENER_SERVICE',
  'READ_SMS',
  'SEND_SMS',
  'CALL_PHONE',
  'READ_CALL_LOG',
  'ACCESS_FINE_LOCATION',
  'MANAGE_EXTERNAL_STORAGE',
  'QUERY_ALL_PACKAGES',
  'READ_CONTACTS',
  'CAMERA',
  'SYSTEM_ALERT_WINDOW',
])


useRouteSeo({
  title: route.title,
  description: route.description,
  path: route.path,
  textTwin: route.twin,
  breadcrumb: route.nav ?? undefined,
  schema: [
    {
      '@context': 'https://schema.org',
      '@type': 'TechArticle',
      headline: route.title,
      description: route.description,
      about: [
        { '@type': 'Thing', name: 'Android accessibility automation safety' },
        { '@type': 'Thing', name: 'Self-hosted MCP server security' },
        { '@type': 'Thing', name: 'Android runtime permissions' },
      ],
      inLanguage: 'en',
      isAccessibleForFree: true,
      license: `https://spdx.org/licenses/${FACTS.license}.html`,
      url: abs(route.path),
      mainEntityOfPage: { '@type': 'WebPage', '@id': abs(route.path) },
      author: { '@type': 'Person', name: SITE.author, url: SITE.authorUrl },
      publisher: { '@type': 'Organization', name: SITE.publisher, url: SITE.publisherUrl },
      image: SITE.ogImage,
      // Subject terms, not search phrases. The previous list was five queries
      // ("android mcp server without root", "aster mcp security", …), which is
      // keyword stuffing wearing a schema.org property.
      keywords: route.topics.join(', '),
      articleSection: 'Security',
      citation: {
        '@type': 'CreativeWork',
        name: `${SITE.name} source code`,
        url: LINKS.repo,
      },
      // The grants people actually challenge, not the first eight in manifest
      // order. "Why does this want to read my SMS" is the question this route
      // exists to answer, so it is the one the structured data carries.
      mentions: PERMISSIONS.filter((p) => NOTABLE_PERMISSIONS.has(p.name)).map((p) => ({
        '@type': 'Thing',
        name: `Android permission ${p.name}`,
        description: p.why,
      })),
    },
    {
      '@context': 'https://schema.org',
      '@type': 'FAQPage',
      mainEntity: SECURITY_FAQS.map((f) => ({
        '@type': 'Question',
        name: f.q,
        acceptedAnswer: { '@type': 'Answer', text: f.a },
      })),
    },
  ],
})
</script>

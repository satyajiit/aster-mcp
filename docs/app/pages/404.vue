<template>
  <NotFoundBody :status-code="404" />
</template>

<script setup lang="ts">
/**
 * The STATIC 404, prerendered at /404 and copied to 404.html by the
 * nitro:build:public-assets hook in nuxt.config.ts.
 *
 * Why a page and not just app/error.vue: the github-pages preset writes its own
 * SPA fallback into 404.html at the end of the build, overwriting anything
 * prerendered at that path — so rendering the error page there is silently
 * undone. A normal route renders to 404/index.html, which the preset does not
 * touch, and the hook then puts it where the host looks. The hook also removes
 * the 404/ directory afterwards, so this never becomes a second live URL.
 *
 * It is deliberately absent from app/data/routes.ts: it is not a page of the
 * site, it must not appear in the sitemap, llms.txt or the nav, and it has no
 * markdown twin.
 */
/**
 * noindex, and deliberately NO canonical.
 *
 * A canonical pointing at the homepage would assert that the 404 is a duplicate
 * of `/` while noindex asserts it must not be indexed — conflicting signals, and
 * a noindex on a page canonicalised elsewhere can propagate to the target. The
 * canonical bought nothing here: GitHub Pages already serves this body with an
 * HTTP 404, which is the signal that matters.
 */
useHead({
  title: 'Page not found',
  meta: [{ name: 'robots', content: 'noindex, follow' }],
})
</script>

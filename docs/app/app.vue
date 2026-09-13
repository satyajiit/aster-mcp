<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
// Head/SEO is PER ROUTE. Each page in app/pages/ calls useRouteSeo() (see
// app/composables/useRouteSeo.ts), which owns that route's title, description,
// canonical, og:url and JSON-LD.
//
// nuxt.config.ts app.head now carries ONLY the things that are genuinely
// site-wide: htmlAttrs, favicons, theme-color, og:site_name and the title
// template. Do not put a canonical or an og:url back there — a global canonical
// pointing at the origin tells search engines to drop every other route.
useRouteAnchorShim()
</script>

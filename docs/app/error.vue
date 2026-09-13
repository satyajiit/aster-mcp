<template>
  <!-- error.vue renders OUTSIDE the layout system, so the layout is wrapped by
       hand here. Without it a failed route lost the nav and the footer, which is
       exactly the visitor who needs them.

       Note this is the CLIENT-SIDE path only. The file GitHub Pages serves for
       an unknown URL is 404.html, which comes from app/pages/404.vue — see the
       comment there for why it cannot come from this file. -->
  <NuxtLayout>
    <NotFoundBody :status-code="error?.statusCode ?? 404" />
  </NuxtLayout>
</template>

<script setup lang="ts">
const props = defineProps<{ error?: { statusCode?: number; message?: string } }>()

useHead({
  title: (props.error?.statusCode ?? 404) === 404 ? 'Page not found' : 'Error',
  meta: [{ name: 'robots', content: 'noindex, follow' }],
})
</script>

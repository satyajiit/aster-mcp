<script setup lang="ts">
// Compatibility route for bookmarks from releases before the event-forwarding
// terminology became canonical. This is a dashboard GET navigation only; API
// mutation compatibility is handled server-side without redirects.
await navigateTo('/settings/event-forwarding', { redirectCode: 308 });
</script>

<template>
  <div />
</template>

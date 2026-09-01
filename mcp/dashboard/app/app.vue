<script setup lang="ts">
const { init } = useColorScheme();

// Apply the stored scheme before paint so there is no flash of the wrong theme.
onMounted(init);
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

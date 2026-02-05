<template>
  <section class="relative py-24 px-6">
    <div class="max-w-3xl mx-auto text-center">
      <div class="inline-flex flex-col items-center p-8 rounded-2xl bg-surface-raised border border-border-dim">
        <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-red-500/20 to-red-600/20 border border-red-500/20 flex items-center justify-center mb-5">
          <Icon name="mdi:youtube" class="text-3xl text-red-400" />
        </div>

        <h3 class="text-xl font-bold text-text-primary mb-2">
          Follow the Author
        </h3>
        <p class="text-sm text-text-secondary max-w-md mb-6 leading-relaxed">
          Aster is built by <span class="text-text-primary font-medium">Satyajit Pradhan</span>. Follow
          <span class="text-text-primary font-medium">@GamesPatch</span> on YouTube for tutorials, demos, and updates.
        </p>

        <a
          href="https://youtube.com/@GamesPatch"
          target="_blank"
          rel="noopener"
          class="group inline-flex items-center gap-2.5 px-6 py-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 font-semibold text-sm hover:bg-red-500/20 hover:border-red-500/30 transition-all"
        >
          <Icon name="mdi:youtube" class="text-lg" />
          Subscribe to @GamesPatch
          <Icon name="lucide:external-link" class="text-xs opacity-60 transition-transform group-hover:translate-x-0.5" />
        </a>
      </div>
    </div>
  </section>
</template>

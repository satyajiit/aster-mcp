<template>
  <div class="noise">
    <a href="#main" class="skip-link">Skip to content</a>
    <!-- The <header> is what makes the site chrome a `banner` landmark. Without
         it the page offered a bare <nav> and no way for a screen-reader user to
         jump to the header region at all — every route reported zero
         [role=banner] elements. NavBar keeps its own aria-label="Primary" on the
         inner <nav>, so the two landmarks nest correctly. -->
    <header>
      <NavBar />
    </header>
    <main id="main" tabindex="-1">
      <slot />
    </main>
    <FooterSection />
  </div>
</template>

<style scoped>
/* WCAG 2.4.1 Bypass Blocks. Visible only on keyboard focus; clears the fixed
   4rem NavBar so 2.4.11 (focus not obscured) holds. */
.skip-link {
  position: fixed;
  top: 0.5rem;
  left: 0.5rem;
  z-index: 100;
  padding: 0.625rem 1rem;
  border-radius: var(--radius-card);
  background: var(--color-primary);
  color: var(--color-bg);
  font-size: var(--text-label-lg);
  font-weight: 600;
  transform: translateY(-160%);
  transition: transform var(--dur-fast) var(--ease-out-soft);
}

.skip-link:focus-visible {
  transform: translateY(0);
  outline: 2px solid var(--color-fg);
  outline-offset: 2px;
}

/* The landing sections own their own vertical rhythm; main is a pass-through. */
main:focus {
  outline: none;
}
</style>

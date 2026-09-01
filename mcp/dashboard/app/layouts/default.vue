<script setup lang="ts">
const route = useRoute();
const config = useRuntimeConfig();

const NAV = [
  { to: '/', label: 'Overview', icon: 'ph:squares-four' },
  { to: '/devices', label: 'Devices', icon: 'ph:devices' },
  { to: '/logs', label: 'Logs', icon: 'ph:list-magnifying-glass' },
  { to: '/connect', label: 'Connect', icon: 'ph:plugs-connected' },
  { to: '/settings/event-forwarding', label: 'Settings', icon: 'ph:gear-six' },
];

// A nav item is active for its own route and anything beneath it, except "/",
// which would otherwise match everything.
function isActive(to: string) {
  return to === '/' ? route.path === '/' : route.path.startsWith(to);
}

const mobileNavOpen = ref(false);
watch(() => route.path, () => (mobileNavOpen.value = false));
</script>

<template>
  <div class="shell">
    <a href="#main" class="skip">Skip to content</a>

    <header class="topbar">
      <div class="topbar__inner">
        <NuxtLink to="/" class="topbar__brand">
          <BrandLockup variant="compact" />
        </NuxtLink>

        <nav class="topbar__nav" :class="{ 'topbar__nav--open': mobileNavOpen }">
          <NuxtLink
            v-for="item in NAV"
            :key="item.to"
            :to="item.to"
            class="navlink"
            :class="{ 'navlink--active': isActive(item.to) }"
          >
            <Icon :name="item.icon" class="navlink__icon" />
            {{ item.label }}
          </NuxtLink>
        </nav>

        <div class="topbar__actions">
          <ThemeToggle />
          <button
            type="button"
            class="topbar__burger"
            :aria-expanded="mobileNavOpen"
            aria-label="Toggle navigation"
            @click="mobileNavOpen = !mobileNavOpen"
          >
            <Icon :name="mobileNavOpen ? 'ph:x' : 'ph:list'" />
          </button>
        </div>
      </div>
    </header>

    <main id="main" class="main">
      <slot />
    </main>

    <footer class="footer">
      <div class="footer__inner">
        <div class="footer__brand">
          <BrandLockup variant="compact" />
          <p class="footer__version">v{{ config.public.version }} · MCP Protocol</p>
        </div>

        <nav class="footer__links">
          <a href="https://aster.matterwardlabs.com" target="_blank" rel="noopener">Documentation</a>
          <a href="https://github.com/satyajiit/aster-mcp" target="_blank" rel="noopener">GitHub</a>
          <a
            href="https://github.com/satyajiit/aster-mcp"
            target="_blank"
            rel="noopener"
            class="footer__star"
          >
            <Icon name="ph:star" /> Star the repo
          </a>
          <a href="https://openally.ai" target="_blank" rel="noopener">OpenAlly</a>
        </nav>
      </div>
      <p class="footer__legal">
        OpenAlly is a trademark of Matterward Labs Private Limited.
      </p>
    </footer>

    <AToastHost />
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.skip {
  position: absolute;
  left: -9999px;
}

.skip:focus {
  left: 1rem;
  top: 1rem;
  z-index: 300;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-bg);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: color-mix(in oklab, var(--color-bg) 85%, transparent);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.topbar__inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0.625rem 1.25rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.topbar__brand {
  text-decoration: none;
  flex: none;
}

.topbar__nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.topbar__actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.topbar__burger {
  display: none;
  border: 1px solid var(--color-border);
  background: var(--color-surface-1);
  color: var(--color-fg-subtle);
  border-radius: var(--radius-md);
  padding: 0.375rem;
  font-size: 1.125rem;
  cursor: pointer;
}

.navlink {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.4375rem 0.75rem;
  border-radius: var(--radius-md);
  color: var(--color-fg-subtle);
  text-decoration: none;
  font-size: var(--text-label-lg);
  font-weight: 500;
  transition:
    color var(--dur-fast) var(--ease-out-soft),
    background-color var(--dur-fast) var(--ease-out-soft);
}

.navlink:hover {
  color: var(--color-fg);
  background: var(--color-surface-1);
}

.navlink--active {
  color: var(--color-primary);
  background: color-mix(in oklab, var(--color-primary) 12%, transparent);
}

.navlink__icon {
  font-size: 1.125em;
}

.main {
  flex: 1;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 1.5rem 1.25rem 3rem;
}

.footer {
  border-top: 1px solid var(--color-border);
  padding: 1.5rem 1.25rem;
}

.footer__inner {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.footer__version {
  margin: 0.25rem 0 0;
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

.footer__links {
  display: flex;
  gap: 1.25rem;
  flex-wrap: wrap;
}

.footer__links a {
  color: var(--color-fg-subtle);
  text-decoration: none;
  font-size: var(--text-label-lg);
}

.footer__links a:hover {
  color: var(--color-primary);
}

.footer__star {
  display: inline-flex;
  align-items: center;
  gap: 0.3125rem;
  color: var(--color-warning) !important;
}

.footer__star:hover {
  color: var(--color-warning-bright) !important;
}

.footer__legal {
  max-width: 1400px;
  margin: 1rem auto 0;
  font-size: var(--text-label-sm);
  color: var(--color-fg-muted);
}

@media (max-width: 900px) {
  .topbar__burger {
    display: inline-flex;
  }

  .topbar__nav {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    gap: 0.125rem;
    padding: 0.5rem;
    background: var(--color-surface-1);
    border-bottom: 1px solid var(--color-border);
    display: none;
  }

  .topbar__nav--open {
    display: flex;
  }
}
</style>

<template>
  <nav
    aria-label="Primary"
    class="fixed top-0 left-0 right-0 z-50 border-b transition-colors duration-300"
    :class="scrolled || open ? 'bg-surface/90 backdrop-blur-xl border-border-dim' : 'border-transparent'"
  >
    <div class="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between gap-3">
      <NuxtLink
        to="/"
        class="shrink-0 group flex items-center gap-3 rounded-lg px-1 py-2 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
        :aria-current="isActive('/') ? 'page' : undefined"
      >
        <img
          src="/logo.png"
          alt=""
          width="32"
          height="32"
          class="w-8 h-8 rounded-lg transition-transform group-hover:scale-110"
        >
        <span class="text-lg font-semibold tracking-tight text-text-primary">Aster</span>
        <span class="sr-only">— home</span>
      </NuxtLink>

      <!-- Desktop route list. Present in the prerendered HTML at every width,
           so a crawler sees every route even though CSS hides it on phones. -->
      <ul class="hidden md:flex items-center gap-0.5 text-sm">
        <li v-for="route in NAV_ROUTES" :key="route.path">
          <NuxtLink
            :to="href(route.path)"
            class="block px-3 py-2 rounded-lg whitespace-nowrap transition-colors hover:bg-surface-raised focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
            :class="isActive(route.path) ? 'text-aster' : 'text-text-secondary hover:text-text-primary'"
            :aria-current="isActive(route.path) ? 'page' : undefined"
          >{{ route.nav }}</NuxtLink>
        </li>
      </ul>

      <div class="flex items-center gap-2">
        <a
          :href="LINKS.repo"
          target="_blank"
          rel="noopener"
          class="flex items-center gap-2 min-h-11 px-3 sm:px-4 rounded-lg bg-surface-raised border border-border-subtle text-sm text-text-secondary hover:text-text-primary hover:border-aster/40 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
        >
          <Icon name="mdi:github" class="text-lg" aria-hidden="true" />
          <span class="hidden sm:inline">GitHub</span>
          <span class="sr-only sm:hidden">Aster on GitHub</span>
        </a>

        <button
          ref="toggleRef"
          type="button"
          class="md:hidden inline-flex items-center justify-center w-11 h-11 rounded-lg border border-border-subtle bg-surface-raised text-text-secondary hover:text-text-primary transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
          :aria-expanded="open"
          aria-controls="primary-drawer"
          @click="toggle"
        >
          <Icon :name="open ? 'lucide:x' : 'lucide:menu'" class="text-xl" aria-hidden="true" />
          <span class="sr-only">{{ open ? 'Close menu' : 'Open menu' }}</span>
        </button>
      </div>
    </div>

    <!-- Mobile drawer. Before the route split every nav target was a hash on a
         single page, so `hidden md:flex` alone was survivable; with eight real
         routes it left a phone visitor unable to reach any page but this one. -->
    <!-- v-if, deliberately, and the no-JS path is the footer.
         With JavaScript off at phone width this drawer cannot open — but neither
         can a v-show version, which server-renders as style="display:none" and
         is equally unreachable, so swapping one for the other buys nothing but
         bytes. The links are not lost: FooterSection renders every route on
         every page as plain anchors, so a no-JS visitor scrolls to the footer
         and navigates from there. Crawlers are covered separately by the
         desktop <ul> above, which is in the HTML at every width.
         If this ever needs to work without JS, the fix is a CSS-only
         disclosure (<details>/<summary>), not v-show. -->
    <div
      v-if="open"
      id="primary-drawer"
      ref="drawerRef"
      class="md:hidden max-h-[calc(100dvh-4rem)] overflow-y-auto overscroll-contain border-t border-border-dim bg-surface/95 backdrop-blur-xl"
      @keydown="onDrawerKeydown"
    >
      <ul class="max-w-6xl mx-auto px-6 py-3">
        <li v-for="route in NAV_ROUTES" :key="route.path">
          <NuxtLink
            :to="href(route.path)"
            class="flex items-center justify-between gap-3 min-h-12 px-2 py-3 rounded-lg border-b border-border-dim last:border-b-0 transition-colors hover:bg-surface-raised focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-aster"
            :class="isActive(route.path) ? 'text-aster' : 'text-text-secondary'"
            :aria-current="isActive(route.path) ? 'page' : undefined"
            @click="close()"
          >
            <span class="text-base font-medium">{{ route.nav }}</span>
            <Icon name="lucide:chevron-right" class="text-base text-text-tertiary" aria-hidden="true" />
          </NuxtLink>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { NAV_ROUTES, href } from '~/data/routes'
import { LINKS } from '~/data/site'

const route = useRoute()
const scrolled = ref(false)
const open = ref(false)
const drawerRef = ref<HTMLElement | null>(null)
const toggleRef = ref<HTMLElement | null>(null)

/** Canonical paths carry no trailing slash, but a browser may arrive with one. */
function normalise(path: string): string {
  if (path.length > 1 && path.endsWith('/')) return path.slice(0, -1)
  return path
}

function isActive(path: string): boolean {
  return normalise(route.path) === normalise(path)
}

function toggle() {
  if (open.value) close()
  else openDrawer()
}

function openDrawer() {
  open.value = true
  nextTick(() => {
    drawerRef.value?.querySelector<HTMLElement>('a, button')?.focus()
  })
}

function close(restoreFocus = false) {
  if (!open.value) return
  open.value = false
  if (restoreFocus) nextTick(() => toggleRef.value?.focus())
}

/** Focus stays inside the drawer while it is open (WCAG 2.4.3 / 2.1.2). */
function onDrawerKeydown(event: KeyboardEvent) {
  if (event.key !== 'Tab') return
  const focusable = Array.from(
    drawerRef.value?.querySelectorAll<HTMLElement>('a[href], button:not([disabled])') ?? [],
  )
  // The toggle is the drawer's visual handle, so it belongs inside the cycle —
  // otherwise Shift+Tab off the first link escapes to the page behind.
  if (toggleRef.value) focusable.unshift(toggleRef.value)
  if (focusable.length === 0) return

  const first = focusable[0]!
  const last = focusable[focusable.length - 1]!
  const active = document.activeElement as HTMLElement | null

  if (event.shiftKey && active === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && active === last) {
    event.preventDefault()
    first.focus()
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') close(true)
}

// A drawer left open across a navigation would cover the page it opened.
watch(() => route.fullPath, () => close())

// The page behind must not scroll under an open drawer.
//
// The drawer's VISIBILITY is CSS (`md:hidden`), but the lock is JS keyed to
// `open` — so crossing the breakpoint with the menu open used to hide the drawer
// AND its close button while leaving `overflow: hidden` on <html> forever. The
// page was then frozen with no visible way out: rotating an iPad mini, or just
// widening a desktop window, stranded the visitor with thousands of pixels
// unreachable and only Escape (an invisible affordance) to recover. Watch the
// same breakpoint the CSS uses and close the drawer when it is crossed.
const DESKTOP = '(min-width: 768px)'
let desktopQuery: MediaQueryList | null = null

function onBreakpointChange(event: MediaQueryListEvent | MediaQueryList) {
  if (event.matches) close()
}

watch(open, (isOpen) => {
  document.documentElement.style.overflow = isOpen ? 'hidden' : ''
})

function onScroll() {
  scrolled.value = window.scrollY > 20
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('keydown', onKeydown)
  desktopQuery = window.matchMedia(DESKTOP)
  desktopQuery.addEventListener('change', onBreakpointChange)
  // Also check once on mount: a reload at desktop width must never start locked.
  onBreakpointChange(desktopQuery)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('keydown', onKeydown)
  desktopQuery?.removeEventListener('change', onBreakpointChange)
  document.documentElement.style.overflow = ''
})
</script>

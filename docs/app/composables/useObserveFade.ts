/**
 * Scroll-reveal that cannot hide content from a fetcher.
 *
 * The pattern this replaces shipped `.observe-fade { opacity: 0 }` in the
 * stylesheet and let an IntersectionObserver add `.is-visible`. On a
 * prerendered site read by a crawler, an LLM fetcher or any browser whose JS
 * failed, every one of those sections stayed at opacity 0 forever — the copy
 * was in the HTML but painted invisible, which is exactly what a cloaking
 * check flags and what a reader sees as a blank page.
 *
 * Here the prerendered HTML is fully visible. The dimmed starting state is
 * ARMED BY SCRIPT (`.fade-armed`) and only ever on elements that are still
 * below the fold at hydration, so nothing that is already on screen flashes
 * out and back in. Under prefers-reduced-motion, or with no
 * IntersectionObserver, nothing is armed at all and the page simply stays as
 * it was painted (WCAG 2.3.3).
 *
 * Usage: call `useObserveFade()` in the section's <script setup>, put
 * `class="observe-fade"` (and an optional `data-delay="120"` in ms) on the
 * elements, and pair it with the `.observe-fade.fade-armed` rules in that
 * component's scoped <style>.
 */

export interface ObserveFadeOptions {
  /** Elements to reveal. Defaults to every `.observe-fade` in the document. */
  selector?: string
  /**
   * Fraction of the element that must be on screen before it reveals. Leave it
   * at the default unless the element is known to be short: the ratio is
   * measured against the TARGET's area, not the viewport's, so anything taller
   * than `innerHeight / threshold` can never satisfy a non-zero threshold and
   * would sit at `.fade-armed` opacity 0 forever.
   */
  threshold?: number
}

export function useObserveFade(options: ObserveFadeOptions = {}) {
  const selector = options.selector ?? '.observe-fade'
  let observer: IntersectionObserver | null = null
  const timers: ReturnType<typeof setTimeout>[] = []

  onMounted(() => {
    const elements = Array.from(document.querySelectorAll<HTMLElement>(selector))
    if (elements.length === 0) return

    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduced || !('IntersectionObserver' in window)) return

    const armed: HTMLElement[] = []
    for (const el of elements) {
      // Already on screen at hydration: leave it exactly as the prerendered
      // HTML painted it. Arming it here would be a visible flicker.
      if (el.getBoundingClientRect().top < window.innerHeight) continue
      el.classList.add('fade-armed')
      armed.push(el)
    }
    if (armed.length === 0) return

    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          const el = entry.target as HTMLElement
          const delay = Number.parseInt(el.dataset.delay ?? '0', 10) || 0
          timers.push(setTimeout(() => el.classList.add('fade-in'), delay))
          observer?.unobserve(el)
        }
      },
      // Threshold 0 fires the moment any sliver crosses the root edge, which is
      // the only height-independent choice; the negative rootMargin below is
      // what actually buys the "slightly in view before it reveals" feel.
      { threshold: options.threshold ?? 0, rootMargin: '0px 0px -40px 0px' },
    )

    for (const el of armed) observer.observe(el)
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
    for (const id of timers.splice(0)) clearTimeout(id)
  })
}

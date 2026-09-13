/**
 * A horizontal scroll container that becomes a keyboard target only while it
 * actually scrolls.
 *
 * WCAG 2.1.1 needs a focusable handle on a region a mouse user can scroll but a
 * keyboard user otherwise cannot reach. Applying `tabindex="0"` unconditionally
 * solved that at phone widths and created the opposite problem at desktop ones:
 * measured at 1440x900, all four of the site's wide tables fit
 * (scrollWidth === clientWidth) yet each still took a tab stop and still
 * announced itself as a scrollable region — four pieces of furniture that do
 * nothing, in the middle of the reading order.
 *
 * So bind the affordance to the measurement. Returns a ref to attach to the
 * wrapper plus the attributes to spread onto it; before hydration (and with
 * JavaScript off) it reports scrollable, which is the safe default — a stray tab
 * stop is a much smaller failure than content no keyboard can reach.
 */
export function useScrollableRegion(label: string) {
  const el = ref<HTMLElement | null>(null)
  const scrollable = ref(true)

  let observer: ResizeObserver | undefined

  function measure() {
    const node = el.value
    if (!node) return
    // 1px of slack: sub-pixel layout rounding reports a 1px overflow on tables
    // that visually fit.
    scrollable.value = node.scrollWidth - node.clientWidth > 1
  }

  onMounted(() => {
    measure()
    if (typeof ResizeObserver === 'undefined') return
    observer = new ResizeObserver(measure)
    observer.observe(el.value!)
    // The table's own width can change without the wrapper resizing.
    const inner = el.value?.firstElementChild
    if (inner) observer.observe(inner)
  })

  onUnmounted(() => observer?.disconnect())

  const attrs = computed(() =>
    scrollable.value
      ? { role: 'region', 'aria-label': `${label} (scrollable table)`, tabindex: 0 }
      : {},
  )

  return { el, attrs, scrollable }
}

/**
 * Back-compat for the hash anchors the site used before it had routes.
 *
 * A fragment is never transmitted to the host, so no GitHub Pages rule could
 * ever handle these — it has to happen client-side, after hydration. That is a
 * real limitation: a crawler or a JS-disabled browser hitting `/#setup` lands
 * on `/` and scrolls nowhere. It is acceptable because no external property
 * links a fragment (every reference to aster.matterwardlabs.com in this repo
 * points at the bare origin), and because the three anchors whose sections
 * stayed on `/` keep working natively without any shim at all.
 *
 * Uses router.replace, not push, so Back does not bounce the visitor.
 */
const MOVED_ANCHORS: Record<string, string> = {
  '#use-cases': '/use-cases#use-cases',
  '#proactive': '/ai-phone#proactive',
  '#how-it-works': '/architecture#how-it-works',
  '#setup': '/setup#setup',
  '#integrations': '/setup#integrations',
  '#tools': '/tools#tools',
  '#security': '/security#security',
  '#live-chat': '/use-cases#live-chat',
  // #features, #embrace and #screenshots are deliberately absent: those three
  // sections still live on `/` (FeaturesGrid, EmbraceSection, ScreenshotsSection
  // each render their own id), so the browser resolves them natively. Every
  // other id rendered by a section component has moved and needs a row above.
}

export function useRouteAnchorShim() {
  const router = useRouter()

  onMounted(() => {
    if (window.location.pathname !== '/') return
    const target = MOVED_ANCHORS[window.location.hash]
    if (target) router.replace(target)
  })
}

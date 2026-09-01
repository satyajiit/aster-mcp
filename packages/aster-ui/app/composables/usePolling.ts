/**
 * One polling primitive with guaranteed teardown.
 *
 * The old dashboard started three `setInterval` polls (index.vue,
 * devices/index.vue, devices/[id]/index.vue) and cleared none of them, so every
 * navigation leaked a timer that kept fetching. This clears on scope dispose,
 * and additionally pauses while the tab is hidden so a backgrounded dashboard
 * stops hammering the API.
 */
export function usePolling(
  fn: () => unknown | Promise<unknown>,
  intervalMs: number,
  options: { immediate?: boolean; pauseWhenHidden?: boolean } = {},
) {
  const { immediate = true, pauseWhenHidden = true } = options;

  const active = ref(false);
  let timer: ReturnType<typeof setInterval> | undefined;
  let running = false;

  // Never overlap runs: a slow request must not stack up behind the interval.
  async function tick() {
    if (running) return;
    running = true;
    try {
      await fn();
    } finally {
      running = false;
    }
  }

  function start() {
    if (!import.meta.client || timer) return;
    active.value = true;
    timer = setInterval(tick, intervalMs);
  }

  function stop() {
    active.value = false;
    if (timer) {
      clearInterval(timer);
      timer = undefined;
    }
  }

  function onVisibility() {
    if (document.visibilityState === 'hidden') stop();
    else {
      start();
      void tick();
    }
  }

  onMounted(() => {
    if (immediate) void tick();
    start();
    if (pauseWhenHidden) document.addEventListener('visibilitychange', onVisibility);
  });

  onScopeDispose(() => {
    stop();
    if (import.meta.client && pauseWhenHidden) {
      document.removeEventListener('visibilitychange', onVisibility);
    }
  });

  return { start, stop, refresh: tick, active };
}

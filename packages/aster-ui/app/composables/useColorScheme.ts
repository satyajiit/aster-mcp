export type ColorScheme = 'dark' | 'light' | 'system';

const STORAGE_KEY = 'aster-color-scheme';

/**
 * Theme control. `system` leaves no `data-theme` attribute on <html>, which is
 * what lets the `prefers-color-scheme` block in tokens.css take over.
 */
export function useColorScheme() {
  const scheme = useState<ColorScheme>('aster-color-scheme', () => 'dark');

  function apply(next: ColorScheme) {
    if (!import.meta.client) return;
    const root = document.documentElement;
    if (next === 'system') root.removeAttribute('data-theme');
    else root.setAttribute('data-theme', next);
  }

  function set(next: ColorScheme) {
    scheme.value = next;
    apply(next);
    if (import.meta.client) localStorage.setItem(STORAGE_KEY, next);
  }

  function init() {
    if (!import.meta.client) return;
    const stored = localStorage.getItem(STORAGE_KEY) as ColorScheme | null;
    const next: ColorScheme =
      stored === 'dark' || stored === 'light' || stored === 'system' ? stored : 'dark';
    scheme.value = next;
    apply(next);
  }

  return { scheme, set, init };
}

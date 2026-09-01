import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

// Layer-relative paths must be absolute. `~` resolution inside a layer config is
// unreliable across Nuxt versions, so anchor everything to this file's directory.
const layerDir = dirname(fileURLToPath(import.meta.url));

/**
 * The shared Aster design system.
 *
 * NOTE: this layer deliberately does NOT register `app/assets/css/tokens.css`
 * via `css:`. Tailwind v4 only generates utilities for a `@theme` block that
 * sits in the same compilation unit as `@import "tailwindcss"`. Registered as a
 * separate entry the variables are emitted but `bg-surface-1`, `rounded-card`
 * and friends are never produced. Each consuming app therefore @imports
 * tokens.css from its own main.css, directly after the Tailwind import.
 *
 * The layer assumes the host app provides @nuxt/icon (for <Icon>) and
 * @nuxt/fonts (which auto-provisions Instrument Sans / JetBrains Mono from the
 * font-family declarations in tokens.css). Both apps already have them.
 */
export default defineNuxtConfig({
  components: [
    {
      path: join(layerDir, 'app/components'),
      pathPrefix: false,
      global: false,
    },
  ],

  imports: {
    dirs: [join(layerDir, 'app/composables'), join(layerDir, 'app/utils')],
  },
});

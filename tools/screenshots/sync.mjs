/**
 * Copy captured dashboard shots into the docs site.
 *
 * The screenshots previously lived in three places, hand-copied, with no sync
 * step — so the site and the README could disagree with each other and with the
 * actual UI. This makes mcp/dashboard/screenshots the single source.
 */
import { cpSync, mkdirSync, rmSync, readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const SRC = join(here, '../../mcp/dashboard/screenshots');
const DEST = join(here, '../../docs/public/screenshots/dashboard');

rmSync(DEST, { recursive: true, force: true });
mkdirSync(DEST, { recursive: true });

// The docs gallery swaps themes by string-replacing the path, so dark shots sit
// at the root and light shots under light/ — keep that contract.
for (const f of readdirSync(join(SRC, 'dark'))) {
  cpSync(join(SRC, 'dark', f), join(DEST, f));
}
mkdirSync(join(DEST, 'light'), { recursive: true });
for (const f of readdirSync(join(SRC, 'light'))) {
  cpSync(join(SRC, 'light', f), join(DEST, 'light', f));
}

console.log(`synced ${readdirSync(join(SRC, 'dark')).length} dark + ${readdirSync(join(SRC, 'light')).length} light shots to docs/public/screenshots/dashboard`);

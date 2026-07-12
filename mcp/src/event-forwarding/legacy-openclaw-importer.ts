import { existsSync, readFileSync } from 'fs';
import { homedir } from 'os';
import { join } from 'path';

/**
 * Compatibility-only discovery for an external OpenClaw installation.
 *
 * This is intentionally isolated from Aster's own event-forwarding config so
 * the upstream product name cannot leak back into the internal configuration
 * model.
 */
export const LEGACY_OPENCLAW_SOURCE_CONFIG_PATH = join(
  homedir(),
  '.openclaw',
  'openclaw.json',
);

export function getLegacyOpenClawSourceToken(): string | null {
  try {
    if (!existsSync(LEGACY_OPENCLAW_SOURCE_CONFIG_PATH)) return null;
    const raw = readFileSync(LEGACY_OPENCLAW_SOURCE_CONFIG_PATH, 'utf-8');
    const sourceConfig = JSON.parse(raw);
    return sourceConfig?.gateway?.auth?.token || null;
  } catch {
    return null;
  }
}

/**
 * Capture every dashboard page in dark and light at 1440x900@2x.
 *
 * Matches the dimensions of the previous hand-taken shots (2880x1800), but is
 * reproducible: the fixture server pins every value, so two runs produce
 * byte-comparable images and the README can be regenerated on demand.
 */
import { chromium } from 'playwright';
import { mkdirSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { DEVICE, NOW } from './fixtures.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const OUT = join(here, '../../mcp/dashboard/screenshots');
const BASE = process.env.DASHBOARD_URL || 'http://localhost:5989';

const VIEWPORT = { width: 1440, height: 900 };
const SCALE = 2;

const SHOTS = [
  { name: 'dashboard-overview', path: '/', full: true },
  { name: 'device-registry', path: '/devices' },
  { name: 'device-telemetry', path: `/devices/${DEVICE.id}`, full: true },
  { name: 'device-screen-control', path: `/devices/${DEVICE.id}/screen`, settle: 2500 },
  { name: 'logs', path: '/logs' },
  { name: 'connect', path: '/connect', full: true },
  { name: 'file-browser', path: `/devices/${DEVICE.id}/files`, settle: 1500 },
  { name: 'mcp-tool-explorer', path: `/devices/${DEVICE.id}/control`, settle: 2000 },
  { name: 'panel-messages', path: `/devices/${DEVICE.id}/panels?panel=messages`, settle: 1200 },
  { name: 'panel-apps', path: `/devices/${DEVICE.id}/panels?panel=apps`, settle: 1200 },
  { name: 'panel-storage', path: `/devices/${DEVICE.id}/panels?panel=storage`, settle: 1500 },
  { name: 'event-forwarding', path: '/settings/event-forwarding', full: true },
];

async function capture(browser, theme) {
  const dir = join(OUT, theme);
  rmSync(dir, { recursive: true, force: true });
  mkdirSync(dir, { recursive: true });

  const context = await browser.newContext({
    viewport: VIEWPORT,
    deviceScaleFactor: SCALE,
    colorScheme: theme,
    reducedMotion: 'reduce', // freeze entrance animations so frames are stable
  });

  // Pin the theme the same way the UI would, before any page script runs.
  await context.addInitScript((t) => {
    localStorage.setItem('aster-color-scheme', t);
    // Keep the star card in frame for the overview shot.
    localStorage.removeItem('aster-star-dismissed');
  }, theme);

  const page = await context.newPage();

  // Freeze the clock at the fixtures' reference instant. Without this the page
  // computes "x ago" against the real wall clock and every relative time in the
  // shots renders as a bare date — and drifts on every run.
  await page.clock.setFixedTime(new Date(NOW));

  for (const shot of SHOTS) {
    await page.goto(`${BASE}${shot.path}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(shot.settle ?? 700);
    // Some panels fetch on mount; wait for spinners to clear.
    await page
      .waitForFunction(() => !document.querySelector('[role="status"]'), { timeout: 5000 })
      .catch(() => {});
    // Wait for webfonts. A late Instrument Sans swap re-lays out every label,
    // which made one shot differ between otherwise identical runs.
    await page.evaluate(() => document.fonts.ready).catch(() => {});
    await page.waitForTimeout(150);
    await page.screenshot({
      path: join(dir, `${shot.name}.png`),
      fullPage: !!shot.full,
    });
    console.log(`  ${theme}/${shot.name}.png`);
  }

  await context.close();
}

const browser = await chromium.launch();
console.log('capturing dark…');
await capture(browser, 'dark');
console.log('capturing light…');
await capture(browser, 'light');
await browser.close();
console.log(`\ndone — ${SHOTS.length * 2} images in ${OUT}`);

/**
 * Smoke-check every device panel against a running dashboard.
 *
 * Loads each panel tab and reports OK / empty / BLANK / ERROR plus any JS
 * errors. Point it at the fixture stack (`node run.mjs` leaves one up) or at
 * a real `aster start` with a connected phone:
 *
 *   node panelcheck.mjs <deviceId>
 *
 * Written after every panel silently rendered nothing: `<component :is>` was
 * given a component NAME, which Nuxt cannot resolve, so Vue emitted an empty
 * unknown element instead of the panel. Nothing threw, so only looking at the
 * DOM caught it.
 */
import { chromium } from 'playwright';
const DEV = process.argv[2];
const PANELS = ['messages','notifications','contacts','alarms','apps','storage','audio','location','shell','utilities'];
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
const errs = [];
page.on('pageerror', e => errs.push(`[pageerror] ${e.message}`));
page.on('console', m => { if (m.type()==='error') errs.push(`[console] ${m.text().slice(0,140)}`); });

for (const p of PANELS) {
  await page.goto(`http://localhost:5989/devices/${DEV}/panels?panel=${p}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(3500);
  const r = await page.evaluate(() => {
    const tabs = document.querySelector('nav.tabs');
    let el = tabs?.nextElementSibling, txt = '', tags = [];
    while (el) { txt += el.innerText + '\n'; tags.push(el.tagName.toLowerCase()); el = el.nextElementSibling; }
    return {
      tags: tags.join(','),
      chars: txt.trim().length,
      empty: /Nothing to show|No apps|No contacts|No messages|No notifications|No alarms|No media|Request failed|No large files/i.test(txt),
      failed: /Request failed/i.test(txt),
      head: txt.trim().split('\n').filter(Boolean).slice(0, 7).join(' | ').slice(0, 210),
    };
  });
  const flag = r.failed ? 'ERROR ' : r.chars < 60 ? 'BLANK ' : r.empty ? 'empty ' : 'OK    ';
  console.log(`${flag} ${p.padEnd(14)} <${r.tags}> ${r.chars}c  ${r.head}`);
}
console.log('\n--- js errors ---');
console.log([...new Set(errs)].slice(0,10).join('\n') || '(none)');
await browser.close();

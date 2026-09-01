/**
 * A stand-in for the Aster API that answers with fixed data.
 *
 * The dashboard proxies /api/** to this, so the capture script needs neither a
 * paired phone nor a running MCP server, and every run renders identically.
 */
import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import * as F from './fixtures.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.FIXTURE_PORT || 5998);

// The real tool catalogue, so the explorer renders the true 49 tools.
const TOOL_DEFINITIONS = (() => {
  const src = readFileSync(join(here, '../../mcp/src/mcp/tools.ts'), 'utf-8');
  const start = src.indexOf('export const TOOL_DEFINITIONS');
  const body = src.slice(src.indexOf('[', start));
  // The definitions are a plain literal; evaluate just that array.
  const end = body.lastIndexOf('];');
  return eval(body.slice(0, end + 1));
})();

const SCREENSHOT_PNG = (() => {
  try {
    return readFileSync(join(here, 'assets/device-screen.png')).toString('base64');
  } catch {
    return null;
  }
})();

function toolResult(payload) {
  return { content: [{ type: 'text', text: JSON.stringify(payload) }] };
}

/*
 * Every payload below mirrors what a REAL device returns — captured from a
 * connected phone over the WebSocket link, not invented. The device speaks
 * snake_case in places (`package`, `next_cursor`, `display_name`), reports
 * storage sizes in MEGABYTES, and nests volume under `streams`. Fixtures that
 * quietly "cleaned that up" were the reason several panels looked fine here
 * and rendered blank against hardware.
 */
const TOOL_RESPONSES = {
  aster_get_battery: () => toolResult({
    level: 76, status: 'charging', plugged: 'ac', health: 'good',
    temperature: 31, voltage: 4302, technology: 'Li-ion', isCharging: true,
  }),
  aster_list_files: ({ path = '/sdcard' }) => toolResult({ path, files: F.FILES[path] ?? [] }),
  aster_read_file: ({ path }) => toolResult(F.FILE_CONTENT[path] ?? { content: '', encoding: 'text', size: 0 }),
  aster_read_sms: () => toolResult({
    messages: [
      { id: 41, address: '+91 98765 43210', body: 'Your OTP is 448192. Valid for 10 minutes.', date: F.NOW - 12_000, type: 'inbox', read: false, threadId: 3 },
      { id: 40, address: 'Clinic reception', body: 'Appointment confirmed for Tuesday 11:00.', date: F.NOW - 3_600_000, type: 'inbox', read: true, threadId: 5 },
      { id: 39, address: '+91 90000 11111', body: 'On my way, 10 minutes out.', date: F.NOW - 5_400_000, type: 'sent', read: true, threadId: 7 },
      { id: 38, address: '+91 90000 11111', body: 'Delivery arriving between 2 and 4 pm.', date: F.NOW - 7_200_000, type: 'inbox', read: true, threadId: 7 },
    ],
    count: 4, offset: 0, hasMore: false,
  }),
  aster_read_notifications: () => toolResult({
    active: [
      { key: '0|com.google.android.gm|1|null|10123', packageName: 'com.google.android.gm', title: 'Design review at 11:00', text: 'Starts in 20 minutes', postTime: F.NOW - 68_000, isOngoing: false, isClearable: true, category: 'event' },
      { key: '0|com.whatsapp|7|null|10188', packageName: 'com.whatsapp', title: 'Priya', text: 'Sent the updated deck', postTime: F.NOW - 400_000, isOngoing: false, isClearable: true, category: 'msg' },
      { key: '0|com.aster|9|null|10240', packageName: 'com.aster', title: 'Aster', text: 'Active: Remote Server', postTime: F.NOW - 900_000, isOngoing: true, isClearable: false, category: 'service' },
      { key: '0|com.android.vending|3|null|10099', packageName: 'com.android.vending', title: 'Update available', text: '3 apps ready to update', postTime: F.NOW - 1_800_000, isOngoing: false, isClearable: true },
    ],
    count: 4,
  }),
  aster_search_contacts: ({ name }) =>
    toolResult({
      contacts: name
        ? CONTACTS.filter((c) => c.display_name.toLowerCase().includes(String(name).toLowerCase()))
        : CONTACTS,
    }),
  aster_list_contacts_full: () => toolResult({ contacts: CONTACTS, next_cursor: null, has_more: false }),
  aster_get_alarms: () => toolResult({
    source: 'clock_provider',
    alarms: [
      { _id: 1, hour: 6, minutes: 45, label: 'Morning run', enabled: '1', daysofweek: 21 },
      { _id: 2, hour: 9, minutes: 0, label: 'Standup', enabled: '1', daysofweek: 31 },
      { _id: 3, hour: 22, minutes: 30, label: 'Wind down', enabled: '0', daysofweek: 0 },
    ],
  }),
  aster_list_installed_apps: () => toolResult({ apps: APPS, next_cursor: null, has_more: false, count: APPS.length }),
  // Sizes are MB — this is the unit the device reports.
  aster_analyze_storage: () => toolResult({
    path: '/sdcard',
    totalSize: 134_217.7,
    fileCount: 48_213,
    breakdown: {
      byDirectory: [
        { path: '/sdcard/DCIM', size: 60_416.0, fileCount: 3_812, percentage: 45.01 },
        { path: '/sdcard/Android', size: 39_936.0, fileCount: 38_004, percentage: 29.75 },
        { path: '/sdcard/Download', size: 17_408.0, fileCount: 214, percentage: 12.97 },
        { path: '/sdcard/Music', size: 9_216.0, fileCount: 1_902, percentage: 6.87 },
        { path: '/sdcard/Documents', size: 2_048.0, fileCount: 148, percentage: 1.53 },
        { path: '/sdcard/Pictures/Screenshots', size: 1_126.4, fileCount: 486, percentage: 0.84 },
      ],
    },
  }),
  aster_find_large_files: () => toolResult({
    path: '/sdcard', minSizeMB: 50, count: 3, showing: 3,
    files: [
      { path: '/sdcard/DCIM/Camera/VID_20260311_180422.mp4', size: 2_560.0 },
      { path: '/sdcard/Download/dataset-archive.tar.gz', size: 1_331.2 },
      { path: '/sdcard/backup-2026-03-01.zip', size: 464.0 },
    ],
  }),
  aster_search_media: () => toolResult({
    path: '/sdcard/DCIM', matched: 2,
    files: [
      { path: '/sdcard/DCIM/Camera/IMG_20260301_142233.jpg', size: 4.5, location: { city: 'Panaji', country: 'India' }, camera: { make: 'Google', model: 'Pixel 9' } },
      { path: '/sdcard/DCIM/Camera/IMG_20260301_151044.jpg', size: 5.0, location: { city: 'Panaji', country: 'India' }, camera: { make: 'Google', model: 'Pixel 9' } },
    ],
  }),
  // Android stream indexes with their real per-stream maxima, not percentages.
  aster_get_volume: () => toolResult({
    ringerMode: 'normal',
    streams: {
      media: { current: 22, max: 30, min: 0 },
      ring: { current: 8, max: 15, min: 0 },
      notification: { current: 8, max: 15, min: 0 },
      alarm: { current: 12, max: 15, min: 1 },
      call: { current: 4, max: 12, min: 1 },
      system: { current: 6, max: 15, min: 0 },
    },
  }),
  aster_get_location: () => toolResult({
    latitude: 15.4909, longitude: 73.8278, accuracy: 12.4, altitude: 8,
    bearing: 0, speed: 0, provider: 'fused', timestamp: F.NOW - 90_000,
  }),
  aster_get_clipboard: () => toolResult({ hasContent: true, content: 'http://192.168.1.42:5988/mcp' }),
  aster_execute_shell: ({ command }) => toolResult({
    command, exitCode: 0, success: true, truncated: false,
    output: '16\n6.6.89-android15-8-g8e4be6b47e40-ab14134548-4k',
  }),
  aster_get_screen_hierarchy: () => toolResult(HIERARCHY),
};

const CONTACTS = [
  { contact_id: '1', display_name: 'Priya Nair', phones: [{ number: '+91 98200 11223', type: 'mobile' }] },
  { contact_id: '2', display_name: 'Clinic reception', phones: [{ number: '+91 22284 01199', type: 'work' }] },
  { contact_id: '3', display_name: 'Arun Mehta', phones: [{ number: '+91 99870 44512', type: 'mobile' }] },
  { contact_id: '4', display_name: 'Building security', phones: [{ number: '+91 22661 20034', type: 'work' }] },
];

const APPS = [
  { package: 'com.whatsapp', label: 'WhatsApp', system: false, version: '2.26.4.11', size_app: 320_864_256, size_data: 88_012_544, size_cache: 4_000_000, last_used: F.NOW - 600_000, install_time: F.NOW - 400 * 86_400_000, update_time: F.NOW - 9 * 86_400_000, permissions: ['android.permission.CAMERA', 'android.permission.RECORD_AUDIO', 'android.permission.READ_CONTACTS'] },
  { package: 'com.google.android.gm', label: 'Gmail', system: false, version: '2026.02.18', size_app: 240_844_160, size_data: 54_000_000, size_cache: 4_000_000, last_used: F.NOW - 68_000, install_time: F.NOW - 900 * 86_400_000, update_time: F.NOW - 21 * 86_400_000, permissions: ['android.permission.READ_CONTACTS', 'android.permission.CAMERA'] },
  { package: 'com.spotify.music', label: 'Spotify', system: false, version: '9.0.14.1', size_app: 402_012_544, size_data: 190_000_000, size_cache: 12_000_000, last_used: F.NOW - 7_200_000, install_time: F.NOW - 300 * 86_400_000, update_time: F.NOW - 3 * 86_400_000, permissions: ['android.permission.RECORD_AUDIO'] },
  { package: 'openally.ai', label: 'OpenAlly', system: false, version: '1.9.2', size_app: 150_695_104, size_data: 35_000_000, size_cache: 2_000_000, last_used: F.NOW - 30_000, install_time: F.NOW - 40 * 86_400_000, update_time: F.NOW - 2 * 86_400_000, permissions: ['android.permission.BIND_ACCESSIBILITY_SERVICE'] },
  { package: 'com.aster', label: 'Aster by OpenAlly', system: false, version: '1.7.0', size_app: 34_991_616, size_data: 7_000_000, size_cache: 1_000_000, last_used: F.NOW - 4_000, install_time: F.NOW - 30 * 86_400_000, update_time: F.NOW - 86_400_000, permissions: ['android.permission.BIND_ACCESSIBILITY_SERVICE', 'android.permission.READ_SMS', 'android.permission.CAMERA'] },
];

const HIERARCHY = {
  className: 'android.widget.FrameLayout',
  children: [
    { className: 'android.widget.Button', text: 'Continue', viewId: 'com.example:id/continue_btn', clickable: true, bounds: { left: 84, top: 1980, right: 996, bottom: 2124 } },
    { className: 'android.widget.TextView', text: 'Skip for now', viewId: 'com.example:id/skip', clickable: true, bounds: { left: 396, top: 2180, right: 684, bottom: 2260 } },
    { className: 'android.widget.EditText', contentDescription: 'Email address', viewId: 'com.example:id/email', clickable: true, bounds: { left: 84, top: 1420, right: 996, bottom: 1556 } },
    { className: 'android.widget.ImageButton', contentDescription: 'Back', clickable: true, bounds: { left: 36, top: 180, right: 156, bottom: 300 } },
  ],
};

function json(res, body, status = 200) {
  const payload = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Content-Length': Buffer.byteLength(payload),
  });
  res.end(payload);
}

function pageLogs(url) {
  const levels = (url.searchParams.get('level') || '').split(',').filter(Boolean);
  const search = (url.searchParams.get('search') || '').toLowerCase();
  const limit = Number(url.searchParams.get('limit') || 100);
  const offset = Number(url.searchParams.get('offset') || 0);

  let logs = F.LOGS;
  if (levels.length) logs = logs.filter((l) => levels.includes(l.level));
  if (search) {
    logs = logs.filter(
      (l) => l.message.toLowerCase().includes(search) || (l.data || '').toLowerCase().includes(search),
    );
  }
  return { logs: logs.slice(offset, offset + limit), total: logs.length, limit, offset };
}

createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const p = url.pathname;

  if (req.method === 'OPTIONS') {
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Content-Type', 'Access-Control-Allow-Methods': 'GET,POST,DELETE,OPTIONS' });
    return res.end();
  }

  if (p === '/api/health') return json(res, { status: 'ok', timestamp: F.NOW });
  if (p === '/api/status') return json(res, F.STATUS);
  if (p === '/api/stats') return json(res, F.STATS);
  if (p === '/api/devices') return json(res, F.DEVICES);
  if (p === '/api/logs') return json(res, pageLogs(url));
  if (p === '/api/logs/devices') return json(res, F.DEVICES.map((d) => d.id));
  if (p === '/api/tools') return json(res, TOOL_DEFINITIONS);
  if (p === '/api/event-forwarding/config' || p === '/api/openclaw/config') {
    if (req.method === 'POST') return json(res, { success: true });
    return json(res, F.EVENT_FORWARDING);
  }

  const deviceMatch = p.match(/^\/api\/devices\/([^/]+)(\/.*)?$/);
  if (deviceMatch) {
    const [, id, rest] = deviceMatch;
    const device = F.DEVICES.find((d) => d.id === id);
    if (!device) return json(res, { error: 'Device not found' }, 404);

    if (!rest) return json(res, device);
    if (rest === '/info') return json(res, { ...device, liveInfo: device.extendedInfo ?? null });
    if (rest === '/logs') {
      const page = pageLogs(url);
      const logs = page.logs.filter((l) => l.deviceId === id);
      return json(res, { ...page, logs, total: logs.length });
    }
    if (rest === '/execute') {
      const body = await new Promise((resolve) => {
        let raw = '';
        req.on('data', (c) => (raw += c));
        req.on('end', () => resolve(JSON.parse(raw || '{}')));
      });

      if (body.name === 'aster_take_screenshot') {
        if (!SCREENSHOT_PNG) {
          return json(res, { isError: true, content: [{ type: 'text', text: 'No fixture screenshot at assets/device-screen.png' }] });
        }
        return json(res, { content: [{ type: 'image', data: SCREENSHOT_PNG, mimeType: 'image/png' }] });
      }

      const handler = TOOL_RESPONSES[body.name];
      if (handler) return json(res, handler(body.args || {}));
      return json(res, toolResult({ ok: true, tool: body.name, args: body.args ?? {} }));
    }
    return json(res, { success: true });
  }

  json(res, { error: 'Not found' }, 404);
}).listen(PORT, () => {
  console.log(`fixture API listening on http://localhost:${PORT}`);
});

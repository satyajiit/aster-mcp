/**
 * Deterministic fixture data for screenshot capture.
 *
 * Captures used to be taken by hand against a real phone on the WebSocket link,
 * which meant they could never be reproduced and drifted from the UI. Every
 * value here is fixed (no Date.now(), no randomness) so two runs of the capture
 * script produce byte-comparable images.
 */

// A fixed instant so relative times ("2m ago") never move between runs.
export const NOW = Date.parse('2026-03-14T09:41:00Z');

export const DEVICE = {
  id: 'b41d8f2c9a734e06b7c5d1a2e8f93c40',
  name: 'Pixel 9',
  model: 'Pixel 9',
  manufacturer: 'Google',
  platform: 'android',
  osVersion: '16',
  status: 'approved',
  lastSeen: NOW - 4_000,
  createdAt: NOW - 3 * 86_400_000,
  online: true,
  extendedInfo: {
    cpuAbi: 'arm64-v8a',
    supportedAbis: ['arm64-v8a', 'armeabi-v7a', 'armeabi'],
    securityPatch: '2026-02-05',
    buildType: 'user',
    buildTags: 'release-keys',
    radioVersion: 'g5300i-250720-250815-B-12683976',
    totalRam: 12005,
    availableRam: 5231,
    totalStorage: 256,
    availableStorage: 118,
    screenRefreshRate: 120.000005,
    screen: { widthPixels: 1080, heightPixels: 2424, density: 2.625, densityDpi: 420 },
    batteryCapacity: 4700,
    uptimeMillis: 283_567_627,
    timezone: 'Asia/Kolkata',
    locale: 'en_IN',
  },
};

export const SECOND_DEVICE = {
  id: '7e2a55f10c884b93aa61d4e77b9f0c12',
  name: 'Galaxy Tab S9',
  model: 'SM-X710',
  manufacturer: 'Samsung',
  platform: 'android',
  osVersion: '15',
  status: 'pending',
  lastSeen: NOW - 42_000,
  createdAt: NOW - 600_000,
  online: true,
};

export const THIRD_DEVICE = {
  id: 'c93b1e77a0d5426fb8e2f0a1c6d34e58',
  name: 'Pixel 6a',
  model: 'Pixel 6a',
  manufacturer: 'Google',
  platform: 'android',
  osVersion: '14',
  status: 'approved',
  lastSeen: NOW - 5 * 3_600_000,
  createdAt: NOW - 20 * 86_400_000,
  online: false,
};

export const DEVICES = [DEVICE, SECOND_DEVICE, THIRD_DEVICE];

export const STATS = {
  totalDevices: 3,
  onlineDevices: 2,
  pendingDevices: 1,
  approvedDevices: 2,
};

export const STATUS = {
  pid: 48213,
  startedAt: NOW - 283_567_627,
  wsPort: 5987,
  wsUrl: 'ws://192.168.1.42:5987',
  apiPort: 5988,
  apiUrl: 'http://192.168.1.42:5988',
  mcpUrl: 'http://192.168.1.42:5988/mcp',
  dashboardPort: 5989,
  dashboardUrl: 'http://192.168.1.42:5989',
  dbPath: '~/.aster/aster.db',
  tailscale: {
    ip: '100.84.12.9',
    dns: 'pixel-host.tail2af8.ts.net',
    wsUrl: 'wss://pixel-host.tail2af8.ts.net',
    dashboardUrl: 'https://pixel-host.tail2af8.ts.net:8443',
    mcpUrl: 'http://100.84.12.9:5988/mcp',
  },
  serverTime: NOW,
  uptimeMs: 283_567_627,
};

const L = (id, deviceId, level, message, data, ago) => ({
  id,
  deviceId,
  level,
  message,
  data: data ? JSON.stringify(data) : undefined,
  timestamp: NOW - ago,
});

export const LOGS = [
  L(412, DEVICE.id, 'info', 'SMS received from +91 98765 43210', { sender: '+919876543210', body: 'Your OTP is 448192. Valid for 10 minutes.', receivedAt: NOW - 12_000 }, 12_000),
  L(411, DEVICE.id, 'debug', 'Sent command: take_screenshot', { id: '163b4a18-4f81-4284-9ea7', action: 'take_screenshot' }, 26_000),
  L(410, DEVICE.id, 'debug', 'Command response: take_screenshot', { durationMs: 412, bytes: 284_119 }, 25_600),
  L(409, SECOND_DEVICE.id, 'warn', 'Pairing required — device awaiting approval', { deviceName: 'Galaxy Tab S9' }, 42_000),
  L(408, DEVICE.id, 'info', 'Notification posted', { packageName: 'com.google.android.gm', title: 'Design review at 11:00', text: 'Starts in 20 minutes' }, 68_000),
  L(407, DEVICE.id, 'error', 'Tool call failed: aster_take_photo', { tool: 'aster_take_photo', error: 'Camera in use by another application' }, 121_000),
  L(406, DEVICE.id, 'debug', 'Sent command: list_files', { path: '/sdcard/DCIM' }, 190_000),
  L(405, DEVICE.id, 'info', 'Incoming call', { number: '+912228401199', contactName: 'Clinic reception', timestamp: NOW - 240_000 }, 240_000),
  L(404, THIRD_DEVICE.id, 'info', 'Device disconnected', { reason: 'heartbeat timeout' }, 5 * 3_600_000),
  L(403, DEVICE.id, 'debug', 'Heartbeat', null, 5 * 3_600_000 + 30_000),
  L(402, DEVICE.id, 'info', 'Device connected', { appVersion: '1.7.0' }, 6 * 3_600_000),
  L(401, DEVICE.id, 'warn', 'Storage above 50% on /sdcard', { usedGb: 138, totalGb: 256 }, 7 * 3_600_000),
];

export const EVENT_FORWARDING = {
  config: {
    enabled: true,
    endpoint: 'http://localhost:18789',
    webhookPath: '/hooks/agent',
    token: 'sk-abc123…',
    hasToken: true,
    channel: 'whatsapp',
    deliverTo: '',
    configuredAt: '2026-03-02 18:24:11',
    channelType: 'openclaw',
    events: {
      notifications: true,
      sms: true,
      deviceConnected: true,
      deviceDisconnected: false,
      pairingRequired: true,
      incomingCalls: true,
    },
  },
  hasSourceToken: true,
  sourceTokenPreview: 'sk-abc12…',
};

export const FILES = {
  '/sdcard': [
    { name: 'DCIM', path: '/sdcard/DCIM', isDirectory: true, isFile: false, size: 0, lastModified: '2026-03-14 08:12:04', canRead: true, canWrite: true, isHidden: false },
    { name: 'Download', path: '/sdcard/Download', isDirectory: true, isFile: false, size: 0, lastModified: '2026-03-13 21:55:40', canRead: true, canWrite: true, isHidden: false },
    { name: 'Documents', path: '/sdcard/Documents', isDirectory: true, isFile: false, size: 0, lastModified: '2026-03-10 11:02:18', canRead: true, canWrite: true, isHidden: false },
    { name: 'Android', path: '/sdcard/Android', isDirectory: true, isFile: false, size: 0, lastModified: '2026-01-04 09:00:00', canRead: true, canWrite: false, isHidden: false },
    { name: '.thumbnails', path: '/sdcard/.thumbnails', isDirectory: true, isFile: false, size: 0, lastModified: '2026-03-14 08:12:10', canRead: true, canWrite: true, isHidden: true },
    { name: 'notes.txt', path: '/sdcard/notes.txt', isDirectory: false, isFile: true, size: 2_418, lastModified: '2026-03-14 07:41:33', canRead: true, canWrite: true, isHidden: false, extension: 'txt' },
    { name: 'backup-2026-03-01.zip', path: '/sdcard/backup-2026-03-01.zip', isDirectory: false, isFile: true, size: 486_539_264, lastModified: '2026-03-01 02:15:00', canRead: true, canWrite: true, isHidden: false, extension: 'zip' },
    { name: 'device-report.json', path: '/sdcard/device-report.json', isDirectory: false, isFile: true, size: 14_902, lastModified: '2026-03-12 16:20:51', canRead: true, canWrite: false, isHidden: false, extension: 'json' },
  ],
  '/sdcard/DCIM': [
    { name: 'Camera', path: '/sdcard/DCIM/Camera', isDirectory: true, isFile: false, size: 0, lastModified: '2026-03-14 08:12:04', canRead: true, canWrite: true, isHidden: false },
    { name: 'Screenshots', path: '/sdcard/DCIM/Screenshots', isDirectory: true, isFile: false, size: 0, lastModified: '2026-03-13 19:30:22', canRead: true, canWrite: true, isHidden: false },
  ],
};

export const FILE_CONTENT = {
  '/sdcard/notes.txt': {
    content:
      'Aster field notes\n=================\n\n- Pair the phone over Tailscale rather than the LAN when travelling.\n- The companion needs accessibility for screen control; everything else\n  works without it.\n- Event forwarding fires on notifications and SMS, not on every log line.\n\nTODO\n----\n[ ] Move the nightly backup off /sdcard\n[ ] Re-check battery drain after the 1.7.0 update\n',
    encoding: 'text',
    size: 2418,
  },
  '/sdcard/device-report.json': {
    content: JSON.stringify({ generated: '2026-03-12T16:20:51Z', device: 'Pixel 9', storage: { totalGb: 256, usedGb: 138 }, battery: { capacityMah: 4700, healthPct: 96 } }, null, 2),
    encoding: 'text',
    size: 14902,
  },
};

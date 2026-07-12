import {
  chmodSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  rmSync,
  statSync,
  utimesSync,
  writeFileSync,
} from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const originalHome = process.env.HOME;
let home: string;
let eventForwarding: typeof import('../index.js');

function fixtureConfig(): import('../index.js').AgentEventForwardingConfig {
  return {
    enabled: true,
    endpoint: 'http://localhost:18789',
    webhookPath: '/hooks/agent',
    token: '0123456789abcdef',
    channel: 'whatsapp',
    deliverTo: '+919876543210',
    configuredAt: '2026-07-12T00:00:00.000Z',
    events: {
      notifications: true,
      sms: true,
      deviceConnected: true,
      deviceDisconnected: true,
      pairingRequired: true,
    },
  };
}

beforeEach(async () => {
  home = mkdtempSync(join(tmpdir(), 'aster-event-forwarding-'));
  process.env.HOME = home;
  vi.resetModules();
  eventForwarding = await import('../index.js');
});

afterEach(() => {
  vi.restoreAllMocks();
  rmSync(home, { recursive: true, force: true });
  if (originalHome === undefined) delete process.env.HOME;
  else process.env.HOME = originalHome;
});

describe('event-forwarding configuration compatibility', () => {
  it('atomically migrates the legacy Aster config while retaining the rollback file', () => {
    const legacyPath = join(home, '.aster', 'openclaw.json');
    const canonicalPath = join(home, '.aster', 'event-forwarding.json');
    const raw = JSON.stringify(fixtureConfig(), null, 2);
    mkdirSync(join(home, '.aster'), { recursive: true });
    writeFileSync(legacyPath, raw);
    chmodSync(legacyPath, 0o644);

    eventForwarding.loadAgentEventForwardingConfig();

    expect(readFileSync(canonicalPath, 'utf-8')).toBe(raw);
    expect(readFileSync(legacyPath, 'utf-8')).toBe(raw);
    expect(statSync(canonicalPath).mode & 0o077).toBe(0);
    expect(statSync(legacyPath).mode & 0o777).toBe(0o644);
    expect(readdirSync(join(home, '.aster')).filter(name => name.endsWith('.tmp'))).toEqual([]);
    expect(eventForwarding.isAgentEventForwardingEnabled()).toBe(true);
  });

  it('prefers canonical config and masks saved tokens without changing their presence signal', () => {
    const canonical = fixtureConfig();
    eventForwarding.saveAgentEventForwardingConfig(canonical);

    expect(eventForwarding.getAgentEventForwardingConfig()).toEqual({
      ...canonical,
      token: '01234567...',
      hasToken: true,
    });
    expect(eventForwarding.getSavedAgentEventForwardingToken()).toBe(canonical.token);
    expect(readFileSync(join(home, '.aster', 'openclaw.json'), 'utf-8')).toBe(
      readFileSync(join(home, '.aster', 'event-forwarding.json'), 'utf-8'),
    );
    expect(statSync(join(home, '.aster', 'openclaw.json')).mode & 0o077).toBe(0);
  });

  it('accepts a newer legacy edit after downgrade and repairs the canonical copy', () => {
    const original = fixtureConfig();
    eventForwarding.saveAgentEventForwardingConfig(original);

    const legacyPath = join(home, '.aster', 'openclaw.json');
    const canonicalPath = join(home, '.aster', 'event-forwarding.json');
    const downgraded = { ...original, token: 'downgrade-token' };
    const raw = JSON.stringify(downgraded, null, 2);
    writeFileSync(legacyPath, raw);
    const future = new Date(Date.now() + 5_000);
    utimesSync(legacyPath, future, future);

    eventForwarding.loadAgentEventForwardingConfig();

    expect(readFileSync(canonicalPath, 'utf-8')).toBe(raw);
    expect(eventForwarding.getSavedAgentEventForwardingToken()).toBe('downgrade-token');
  });

  it('repairs a stale legacy copy after a canonical-first interrupted save', () => {
    const original = fixtureConfig();
    eventForwarding.saveAgentEventForwardingConfig(original);

    const legacyPath = join(home, '.aster', 'openclaw.json');
    const canonicalPath = join(home, '.aster', 'event-forwarding.json');
    const updated = { ...original, endpoint: 'https://new.example.test' };
    const raw = JSON.stringify(updated, null, 2);
    writeFileSync(canonicalPath, raw);
    const future = new Date(Date.now() + 5_000);
    utimesSync(canonicalPath, future, future);

    eventForwarding.loadAgentEventForwardingConfig();

    expect(readFileSync(legacyPath, 'utf-8')).toBe(raw);
    expect(eventForwarding.getAgentEventForwardingConfig()?.endpoint).toBe(
      'https://new.example.test',
    );
  });

  it('keeps legacy exports as strict aliases of canonical functions', async () => {
    const legacy = await import('../../openclaw/index.js');

    expect(legacy.loadOpenClawConfig).toBe(eventForwarding.loadAgentEventForwardingConfig);
    expect(legacy.forwardEventToOpenClaw).toBe(eventForwarding.forwardAgentEvent);
    expect(legacy.saveOpenClawConfig).toBe(eventForwarding.saveAgentEventForwardingConfig);
    expect(legacy.testOpenClawConnection).toBe(eventForwarding.testAgentEventForwardingConnection);
  });
});

describe('event-forwarding webhook contract', () => {
  it('preserves the notification payload, headers, and delivery defaults', async () => {
    eventForwarding.saveAgentEventForwardingConfig(fixtureConfig());
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 202 });
    vi.stubGlobal('fetch', fetchMock);

    await eventForwarding.forwardAgentEvent(
      {
        deviceId: 'pixel-1',
        manufacturer: 'Google',
        model: 'Pixel 9',
        osVersion: '16',
      },
      'notification',
      {
        packageName: 'com.example.mail',
        title: 'Hello',
        text: 'World',
      },
      1_720_742_400_000,
    );

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, request] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:18789/hooks/agent');
    expect(request).toMatchObject({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer 0123456789abcdef',
      },
    });
    expect(JSON.parse(request.body)).toEqual({
      message: [
        '[skill] aster',
        '[event] notification',
        '[device_id] pixel-1',
        '[model] Google Pixel 9, Android 16',
        '[data-app] mail',
        '[data-package] com.example.mail',
        '[data-title] Hello',
        '[data-text] World',
      ].join('\n'),
      wakeMode: 'now',
      deliver: true,
      channel: 'whatsapp',
      to: '+919876543210',
    });
  });
});

import { mkdtempSync, rmSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { FastifyInstance } from 'fastify';

const originalHome = process.env.HOME;
let home: string;
let app: FastifyInstance;

beforeEach(async () => {
  home = mkdtempSync(join(tmpdir(), 'aster-event-forwarding-routes-'));
  process.env.HOME = home;
  vi.resetModules();

  const { createApiServer } = await import('../index.js');
  app = createApiServer({
    wsPort: 5987,
    dashboardPort: 5988,
    dbPath: ':memory:',
    commandTimeout: 30000,
    heartbeatInterval: 30000,
    heartbeatTimeout: 90000,
  });
});

afterEach(async () => {
  await app.close();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  rmSync(home, { recursive: true, force: true });
  if (originalHome === undefined) delete process.env.HOME;
  else process.env.HOME = originalHome;
});

describe('event-forwarding REST compatibility', () => {
  it('serves canonical and legacy routes through equivalent non-redirecting handlers', async () => {
    const payload = {
      enabled: true,
      endpoint: 'http://localhost:18789',
      webhookPath: '/hooks/agent',
      token: 'route-parity-token',
      channel: 'whatsapp',
      deliverTo: '',
      events: {
        notifications: true,
        sms: true,
        deviceConnected: true,
        deviceDisconnected: true,
        pairingRequired: true,
      },
    };

    const canonicalPost = await app.inject({
      method: 'POST',
      url: '/api/event-forwarding/config',
      payload,
    });
    const legacyPost = await app.inject({
      method: 'POST',
      url: '/api/openclaw/config',
      payload: { ...payload, token: '' },
    });

    expect(canonicalPost.statusCode).toBe(200);
    expect(legacyPost.statusCode).toBe(200);
    expect(canonicalPost.headers.location).toBeUndefined();
    expect(legacyPost.headers.location).toBeUndefined();

    const canonicalGet = await app.inject({ method: 'GET', url: '/api/event-forwarding/config' });
    const legacyGet = await app.inject({ method: 'GET', url: '/api/openclaw/config' });

    expect(canonicalGet.statusCode).toBe(200);
    expect(legacyGet.statusCode).toBe(200);
    expect(legacyGet.json()).toEqual(canonicalGet.json());
    expect(canonicalGet.json().config).toMatchObject({
      token: 'route-pa...',
      hasToken: true,
    });
  });

  it('persists channelType and incomingCalls without defaulting Mattermost channel to whatsapp', async () => {
    const payload = {
      enabled: true,
      endpoint: 'https://mm.example.test/hooks/abc',
      webhookPath: '',
      token: '',
      channelType: 'mattermost',
      channel: '',
      deliverTo: '',
      events: {
        notifications: true,
        sms: true,
        deviceConnected: true,
        deviceDisconnected: true,
        pairingRequired: true,
        incomingCalls: false,
      },
    };

    const post = await app.inject({
      method: 'POST',
      url: '/api/event-forwarding/config',
      payload,
    });
    expect(post.statusCode).toBe(200);

    const get = await app.inject({ method: 'GET', url: '/api/event-forwarding/config' });
    expect(get.json().config).toMatchObject({
      channelType: 'mattermost',
      channel: '',
      endpoint: 'https://mm.example.test/hooks/abc',
      webhookPath: '',
      events: {
        incomingCalls: false,
      },
    });
    expect(get.json().config.channel).not.toBe('whatsapp');
  });

  it('defaults missing channelType to openclaw and missing incomingCalls to true on save', async () => {
    const post = await app.inject({
      method: 'POST',
      url: '/api/event-forwarding/config',
      payload: {
        enabled: true,
        endpoint: 'http://localhost:18789',
        webhookPath: '/hooks/agent',
        token: 'save-default-token',
        channel: 'telegram',
        deliverTo: '',
        events: {
          notifications: true,
          sms: true,
          deviceConnected: true,
          deviceDisconnected: true,
          pairingRequired: true,
        },
      },
    });
    expect(post.statusCode).toBe(200);

    const get = await app.inject({ method: 'GET', url: '/api/event-forwarding/config' });
    expect(get.json().config).toMatchObject({
      channelType: 'openclaw',
      channel: 'telegram',
      events: { incomingCalls: true },
    });
  });

  it('dispatches the connection probe by channel type', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => '',
    });
    vi.stubGlobal('fetch', fetchMock);

    const openclaw = await app.inject({
      method: 'POST',
      url: '/api/event-forwarding/test',
      payload: {
        endpoint: 'http://localhost:18789',
        webhookPath: '/hooks/agent',
        token: 'probe-token',
      },
    });
    expect(openclaw.statusCode).toBe(200);
    expect(openclaw.json()).toMatchObject({ success: true, status: 200 });
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:18789/hooks/agent');
    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer probe-token',
      },
    });
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
      message: '[skill] aster\n[event] test\n[data]\nstatus: connection test from dashboard',
      wakeMode: 'now',
      deliver: false,
    });

    fetchMock.mockClear();
    const mattermost = await app.inject({
      method: 'POST',
      url: '/api/event-forwarding/test',
      payload: {
        endpoint: 'https://mm.example.test/hooks/abc',
        webhookPath: '',
        token: 'should-not-be-sent',
        channelType: 'mattermost',
        channel: 'whatsapp',
      },
    });
    expect(mattermost.statusCode).toBe(200);
    expect(fetchMock.mock.calls[0][0]).toBe('https://mm.example.test/hooks/abc');
    expect(fetchMock.mock.calls[0][1].headers).toEqual({ 'Content-Type': 'application/json' });
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined();
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({ text: 'Aster connection test' });
  });
});

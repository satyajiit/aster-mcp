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
});

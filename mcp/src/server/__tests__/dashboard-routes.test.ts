import { mkdtempSync, rmSync, writeFileSync, mkdirSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { FastifyInstance } from 'fastify';

const originalHome = process.env.HOME;
let home: string;
let app: FastifyInstance;
let db: typeof import('../../db/index.js');

const DEVICE = {
  id: 'device-under-test',
  name: 'Probe',
  model: 'Pixel 9',
  manufacturer: 'Google',
  platform: 'android' as const,
  osVersion: '16',
  status: 'rejected' as const,
  lastSeen: Date.now(),
  createdAt: Date.now(),
};

beforeEach(async () => {
  home = mkdtempSync(join(tmpdir(), 'aster-dashboard-routes-'));
  process.env.HOME = home;
  vi.resetModules();

  db = await import('../../db/index.js');
  db.initDatabase(':memory:');

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
  vi.restoreAllMocks();
  rmSync(home, { recursive: true, force: true });
  if (originalHome === undefined) delete process.env.HOME;
  else process.env.HOME = originalHome;
});

describe('routes reachable from createApiServer', () => {
  // These two were previously registered only inside startApiServer, so any
  // consumer building the app via createApiServer silently lacked them.
  it('exposes /api/tools', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/tools' });
    expect(res.statusCode).toBe(200);
    const tools = res.json() as { name: string }[];
    expect(tools.length).toBeGreaterThan(40);
    expect(tools.some((t) => t.name === 'aster_take_screenshot')).toBe(true);
  });

  it('exposes /api/devices/:id/execute', async () => {
    const res = await app.inject({
      method: 'POST',
      url: '/api/devices/missing/execute',
      payload: { name: 'aster_get_battery', args: {} },
    });
    // 404 rather than Fastify's "route not found" proves the route is mounted.
    expect(res.statusCode).toBe(404);
    expect(res.json()).toEqual({ error: 'Device not found' });
  });
});

describe('log filtering', () => {
  beforeEach(() => {
    db.upsertDevice(DEVICE);
    db.addLog(DEVICE.id, 'error', 'disk full at 100% capacity', { code: 'ENOSPC' });
    db.addLog(DEVICE.id, 'info', 'connected ok', { via: 'ws' });
    db.addLog(DEVICE.id, 'debug', 'heartbeat', null);
  });

  it('returns a page envelope rather than a bare array', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/logs' });
    const body = res.json();
    expect(body).toMatchObject({ total: 3, limit: 100, offset: 0 });
    expect(body.logs).toHaveLength(3);
  });

  it('filters by level, including multiple levels', async () => {
    const one = await app.inject({ method: 'GET', url: '/api/logs?level=error' });
    expect(one.json().logs).toHaveLength(1);

    const two = await app.inject({ method: 'GET', url: '/api/logs?level=info,debug' });
    expect(two.json().logs).toHaveLength(2);
  });

  it('ignores unknown level values instead of returning nothing', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/logs?level=bogus' });
    expect(res.json().logs).toHaveLength(3);
  });

  it('treats % in a search as a literal, not a wildcard', async () => {
    const hit = await app.inject({ method: 'GET', url: '/api/logs?search=100%25' });
    expect(hit.json().logs).toHaveLength(1);

    // A bare wildcard must not match everything.
    const miss = await app.inject({ method: 'GET', url: '/api/logs?search=%25%25%25' });
    expect(miss.json().logs).toHaveLength(0);
  });

  it('searches the structured data payload, not just the message', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/logs?search=ENOSPC' });
    expect(res.json().logs).toHaveLength(1);
  });

  it('paginates with limit and offset while reporting the full total', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/logs?limit=1&offset=1' });
    const body = res.json();
    expect(body.logs).toHaveLength(1);
    expect(body.total).toBe(3);
  });

  it('scopes device logs to the device', async () => {
    db.upsertDevice({ ...DEVICE, id: 'other-device' });
    db.addLog('other-device', 'info', 'unrelated', null);

    const res = await app.inject({ method: 'GET', url: `/api/devices/${DEVICE.id}/logs` });
    expect(res.json().total).toBe(3);
  });

  it('lists device ids present in the log table', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/logs/devices' });
    expect(res.json()).toEqual([DEVICE.id]);
  });
});

describe('device lifecycle', () => {
  beforeEach(() => db.upsertDevice(DEVICE));

  it('returns a rejected device to pending', async () => {
    const res = await app.inject({ method: 'POST', url: `/api/devices/${DEVICE.id}/unreject` });
    expect(res.statusCode).toBe(200);
    expect(db.getDevice(DEVICE.id)?.status).toBe('pending');
  });

  it('404s unreject for an unknown device', async () => {
    const res = await app.inject({ method: 'POST', url: '/api/devices/nope/unreject' });
    expect(res.statusCode).toBe(404);
  });

  it('deletes a device, and 404s on a second delete', async () => {
    const first = await app.inject({ method: 'DELETE', url: `/api/devices/${DEVICE.id}` });
    expect(first.statusCode).toBe(200);
    expect(db.getDevice(DEVICE.id)).toBeNull();

    const second = await app.inject({ method: 'DELETE', url: `/api/devices/${DEVICE.id}` });
    expect(second.statusCode).toBe(404);
  });
});

describe('runtime status', () => {
  it('falls back to config when status.json is absent', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/status' });
    expect(res.statusCode).toBe(200);
    expect(res.json()).toMatchObject({ wsPort: 5987, apiPort: 5988, uptimeMs: null });
  });

  it('surfaces the MCP and Tailscale URLs the CLI writes', async () => {
    mkdirSync(join(home, '.aster'), { recursive: true });
    writeFileSync(
      join(home, '.aster', 'status.json'),
      JSON.stringify({
        startedAt: Date.now() - 60_000,
        mcpUrl: 'http://192.168.1.10:5988/mcp',
        tailscale: { dns: 'box.tail2af8.ts.net' },
      }),
    );

    vi.resetModules();
    const { createApiServer } = await import('../index.js');
    const fresh = createApiServer({
      wsPort: 5987,
      dashboardPort: 5988,
      dbPath: ':memory:',
      commandTimeout: 30000,
      heartbeatInterval: 30000,
      heartbeatTimeout: 90000,
    });

    const res = await fresh.inject({ method: 'GET', url: '/api/status' });
    const body = res.json();
    expect(body.mcpUrl).toBe('http://192.168.1.10:5988/mcp');
    expect(body.tailscale.dns).toBe('box.tail2af8.ts.net');
    expect(body.uptimeMs).toBeGreaterThan(0);
    await fresh.close();
  });

  // The daemon writes `startedAt: new Date().toISOString()`, so a
  // number-only reader reports null uptime against every real status file.
  it('computes uptime from the ISO startedAt the daemon actually writes', async () => {
    mkdirSync(join(home, '.aster'), { recursive: true });
    writeFileSync(
      join(home, '.aster', 'status.json'),
      JSON.stringify({ startedAt: new Date(Date.now() - 90_000).toISOString() }),
    );

    vi.resetModules();
    const { createApiServer } = await import('../index.js');
    const fresh = createApiServer({
      wsPort: 5987,
      dashboardPort: 5988,
      dbPath: ':memory:',
      commandTimeout: 30000,
      heartbeatInterval: 30000,
      heartbeatTimeout: 90000,
    });

    const body = (await fresh.inject({ method: 'GET', url: '/api/status' })).json();
    expect(body.uptimeMs).toBeGreaterThanOrEqual(90_000);
    expect(typeof body.startedAt).toBe('number');
    await fresh.close();
  });

  it('reports null uptime rather than NaN for an unparseable startedAt', async () => {
    mkdirSync(join(home, '.aster'), { recursive: true });
    writeFileSync(
      join(home, '.aster', 'status.json'),
      JSON.stringify({ startedAt: 'not a date' }),
    );

    vi.resetModules();
    const { createApiServer } = await import('../index.js');
    const fresh = createApiServer({
      wsPort: 5987,
      dashboardPort: 5988,
      dbPath: ':memory:',
      commandTimeout: 30000,
      heartbeatInterval: 30000,
      heartbeatTimeout: 90000,
    });

    const body = (await fresh.inject({ method: 'GET', url: '/api/status' })).json();
    expect(body.uptimeMs).toBeNull();
    expect(body.startedAt).toBeNull();
    await fresh.close();
  });
});

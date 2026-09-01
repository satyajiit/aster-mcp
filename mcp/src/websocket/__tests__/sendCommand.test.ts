import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { WebSocket } from 'ws';
import { closeDatabase, initDatabase, upsertDevice } from '../../db/index.js';
import type { Device } from '../../types/index.js';
import { getConnectedDevices, sendCommand } from '../index.js';

const originalHome = process.env.HOME;
const originalDbPath = process.env.DB_PATH;

let home: string;

function seedDevice(overrides: Partial<Device> = {}): Device {
  return upsertDevice({
    id: 'pixel-1',
    name: 'Pixel',
    model: 'Pixel 2 XL',
    manufacturer: 'Google',
    platform: 'android',
    osVersion: '11',
    status: 'approved',
    lastSeen: Date.parse('2026-08-01T12:00:00.000Z'),
    ...overrides,
  });
}

describe('sendCommand', () => {
  beforeEach(() => {
    home = mkdtempSync(join(tmpdir(), 'aster-sendcommand-'));
    process.env.HOME = home;
    process.env.DB_PATH = join(home, 'aster.db');
    closeDatabase();
    initDatabase(':memory:');
    getConnectedDevices().clear();
  });

  afterEach(() => {
    getConnectedDevices().clear();
    closeDatabase();
    rmSync(home, { recursive: true, force: true });
    if (originalHome === undefined) delete process.env.HOME;
    else process.env.HOME = originalHome;
    if (originalDbPath === undefined) delete process.env.DB_PATH;
    else process.env.DB_PATH = originalDbPath;
  });

  it('throws a short unknown-device error when the registry has no row', async () => {
    await expect(sendCommand('missing-id', 'get_battery')).rejects.toThrow(
      'Unknown device missing-id',
    );
  });

  it('explains a registered device with no socket and points at status.json /mcp', async () => {
    seedDevice();
    mkdirSync(join(home, '.aster'), { recursive: true });
    writeFileSync(
      join(home, '.aster', 'status.json'),
      JSON.stringify({
        pid: 4242,
        apiUrl: 'http://192.168.1.10:5988',
      }),
    );

    const err = await sendCommand('pixel-1', 'get_battery').then(
      () => {
        throw new Error('expected sendCommand to reject');
      },
      (e: unknown) => e as Error,
    );

    expect(err.message).toContain(
      'Device pixel-1 is registered (last seen 2026-08-01T12:00:00.000Z) but this process has no live WebSocket for it',
    );
    expect(err.message).toContain('http://192.168.1.10:5988/mcp');
    expect(err.message).toContain(`cwd=${process.cwd()}`);
    expect(err.message).toContain(`DB_PATH=${process.env.DB_PATH}`);
  });

  it('points at aster status when status.json is missing', async () => {
    seedDevice();

    const err = await sendCommand('pixel-1', 'get_battery').then(
      () => {
        throw new Error('expected sendCommand to reject');
      },
      (e: unknown) => e as Error,
    );

    expect(err.message).toContain('run `aster status`');
    expect(err.message).not.toContain('http://');
    expect(err.message).toContain(`cwd=${process.cwd()}`);
    expect(err.message).toContain(`DB_PATH=${process.env.DB_PATH}`);
  });

  it('rejects an unapproved live socket without consulting the no-socket path', async () => {
    const device = seedDevice({ status: 'pending' });
    getConnectedDevices().set(device.id, {
      device,
      ws: { send() { /* unused */ } } as unknown as WebSocket,
      lastHeartbeat: Date.now(),
      pendingCommands: new Map(),
    });

    await expect(sendCommand(device.id, 'get_battery')).rejects.toThrow(
      `Device ${device.id} is not approved`,
    );
  });

  it('sends on a live approved socket from the in-memory map', async () => {
    const device = seedDevice();
    const sent: Array<Record<string, unknown>> = [];
    const pendingCommands = new Map();
    getConnectedDevices().set(device.id, {
      device,
      ws: {
        send(raw: string) {
          sent.push(JSON.parse(raw) as Record<string, unknown>);
        },
      } as unknown as WebSocket,
      lastHeartbeat: Date.now(),
      pendingCommands,
    });

    const resultPromise = sendCommand(device.id, 'get_battery', {}, 1000);
    expect(sent).toEqual([
      expect.objectContaining({ type: 'command', action: 'get_battery' }),
    ]);

    const commandId = sent[0].id as string;
    const pending = pendingCommands.get(commandId);
    expect(pending).toBeDefined();
    clearTimeout(pending.timeout);
    pendingCommands.delete(commandId);
    pending.resolve({
      type: 'command_response',
      id: commandId,
      success: true,
      data: { level: 80 },
    });

    await expect(resultPromise).resolves.toMatchObject({
      success: true,
      data: { level: 80 },
    });
  });
});

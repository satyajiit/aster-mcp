import { describe, it, expect, vi, beforeEach } from 'vitest';

// `handler.ts` imports `sendCommand` from `../websocket/index.js` and the
// device accessors from `../db/index.js` (which pulls in better-sqlite3 at
// import time). Mock both so the dispatch can be exercised in isolation,
// without a live websocket or a native sqlite build.
const sendCommand = vi.fn();
vi.mock('../../websocket/index.js', () => ({
  sendCommand: (...args: unknown[]) => sendCommand(...args),
  getConnectedDevices: () => new Map([['dev1', { id: 'dev1' }]]),
  isDeviceOnline: () => true,
}));
vi.mock('../../db/index.js', () => ({
  getDevice: () => ({ id: 'dev1', name: 'd', status: 'online', lastSeen: 0 }),
  getAllDevices: () => [],
}));
vi.mock('../../util/queryParser.js', () => ({
  parseNaturalLanguageQuery: () => ({}),
}));

import { handleToolCall } from '../handler';

describe('aster_list_installed_apps', () => {
  beforeEach(() => {
    sendCommand.mockReset();
    sendCommand.mockResolvedValue({ data: { apps: [], next_cursor: null, has_more: false, count: 0 } });
  });

  it('maps camelCase args to snake_case companion params', async () => {
    await handleToolCall('aster_list_installed_apps', {
      deviceId: 'dev1',
      includeSystem: true,
      cursor: 200,
      limit: 50,
    });
    expect(sendCommand).toHaveBeenCalledWith('dev1', 'list_installed_apps', {
      include_system: true,
      cursor: 200,
      limit: 50,
    });
  });

  it('defaults include_system=false and limit=100 when omitted', async () => {
    await handleToolCall('aster_list_installed_apps', { deviceId: 'dev1' });
    expect(sendCommand).toHaveBeenCalledWith('dev1', 'list_installed_apps', {
      include_system: false,
      cursor: undefined,
      limit: 100,
    });
  });
});

import Fastify from 'fastify';
import { consola } from 'consola';
import type { ServerConfig } from '../types/index.js';
import { getAllDevices, getDevice, getDeviceLogs, getAllLogs } from '../db/index.js';
import {
  approveDevice,
  fetchAndCacheExtendedInfo,
  getConnectedDevices,
  rejectDevice,
} from '../websocket/index.js';
import { TOOL_DEFINITIONS } from '../mcp/tools.js';
import { handleToolCall } from '../mcp/handler.js';
import { registerMcpHttpRoutes } from '../mcp/http.js';

export function createApiServer(config: ServerConfig) {
  const app = Fastify({
    logger: false,
  });

  // CORS for dashboard
  app.addHook('onRequest', async (request, reply) => {
    reply.header('Access-Control-Allow-Origin', '*');
    reply.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    reply.header('Access-Control-Allow-Headers', 'Content-Type');

    if (request.method === 'OPTIONS') {
      reply.status(204).send();
    }
  });

  // Health check
  app.get('/api/health', async () => {
    return { status: 'ok', timestamp: Date.now() };
  });

  // List all devices
  app.get('/api/devices', async () => {
    const devices = getAllDevices();
    const connected = getConnectedDevices();

    return devices.map(device => ({
      ...device,
      online: connected.has(device.id),
    }));
  });

  // Get single device
  app.get<{ Params: { id: string } }>('/api/devices/:id', async (request, reply) => {
    const device = getDevice(request.params.id);
    if (!device) {
      reply.status(404);
      return { error: 'Device not found' };
    }

    const connected = getConnectedDevices();
    return {
      ...device,
      online: connected.has(device.id),
    };
  });

  // Approve device
  app.post<{ Params: { id: string } }>('/api/devices/:id/approve', async (request, reply) => {
    const success = approveDevice(request.params.id);
    if (!success) {
      reply.status(404);
      return { error: 'Device not found' };
    }
    return { success: true };
  });

  // Reject device
  app.post<{ Params: { id: string } }>('/api/devices/:id/reject', async (request, reply) => {
    const success = rejectDevice(request.params.id);
    if (!success) {
      reply.status(404);
      return { error: 'Device not found' };
    }
    return { success: true };
  });

  // Get device with live extended info
  app.get<{ Params: { id: string } }>('/api/devices/:id/info', async (request, reply) => {
    const device = getDevice(request.params.id);
    if (!device) {
      reply.status(404);
      return { error: 'Device not found' };
    }

    const connected = getConnectedDevices();
    const online = connected.has(device.id);

    // If device is offline, return cached info
    if (!online) {
      return {
        ...device,
        online: false,
        liveInfo: null,
        message: 'Device is offline. Showing cached information.',
      };
    }

    // If device is not approved, don't fetch live info
    if (device.status !== 'approved') {
      return {
        ...device,
        online: true,
        liveInfo: null,
        message: 'Device is not approved. Cannot fetch live information.',
      };
    }

    // Fetch live info from device
    try {
      const liveInfo = await fetchAndCacheExtendedInfo(device.id);
      return {
        ...device,
        online: true,
        liveInfo,
      };
    } catch {
      return {
        ...device,
        online: true,
        liveInfo: null,
        error: 'Failed to fetch live info from device',
      };
    }
  });

  // Get device logs
  app.get<{ Params: { id: string }; Querystring: { limit?: string } }>(
    '/api/devices/:id/logs',
    async (request) => {
      const limit = parseInt(request.query.limit || '100', 10);
      return getDeviceLogs(request.params.id, limit);
    }
  );

  // Get all logs
  app.get<{ Querystring: { limit?: string } }>('/api/logs', async (request) => {
    const limit = parseInt(request.query.limit || '100', 10);
    return getAllLogs(limit);
  });

  // Stats endpoint for dashboard
  app.get('/api/stats', async () => {
    const devices = getAllDevices();
    const connected = getConnectedDevices();

    return {
      totalDevices: devices.length,
      onlineDevices: connected.size,
      pendingDevices: devices.filter(d => d.status === 'pending').length,
      approvedDevices: devices.filter(d => d.status === 'approved').length,
    };
  });

  return app;
}

export async function startApiServer(config: ServerConfig): Promise<void> {
  const app = createApiServer(config);

  // Register MCP HTTP routes
  registerMcpHttpRoutes(app);

  // List available tools
  app.get('/api/tools', async () => {
    return TOOL_DEFINITIONS;
  });

  // Execute tool on device
  app.post<{ Params: { id: string }; Body: { name: string; args: Record<string, unknown> } }>(
    '/api/devices/:id/execute',
    async (request, reply) => {
      const { id } = request.params;
      const { name, args } = request.body;
      const device = getDevice(id);

      if (!device) {
        reply.status(404);
        return { error: 'Device not found' };
      }

      // Inject deviceId into args
      const toolArgs = {
        ...args,
        deviceId: id,
      };

      try {
        const result = await handleToolCall(name, toolArgs);
        return result;
      } catch (error) {
        reply.status(500);
        return {
          isError: true,
          content: [{ type: 'text', text: error instanceof Error ? error.message : String(error) }]
        };
      }
    }
  );

  await app.listen({ port: config.dashboardPort, host: '0.0.0.0' });
  consola.success(`API server listening on port ${config.dashboardPort}`);
}

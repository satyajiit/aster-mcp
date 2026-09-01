import Fastify from 'fastify';
import { consola } from 'consola';
import type { LogEntry, ServerConfig } from '../types/index.js';
import {
  getAllDevices,
  getDevice,
  deleteDevice,
  updateDeviceStatus,
  queryLogs,
  getLoggedDeviceIds,
  type LogQuery,
} from '../db/index.js';
import { readFileSync } from 'fs';
import { join } from 'path';
import { homedir } from 'os';
import {
  approveDevice,
  fetchAndCacheExtendedInfo,
  getConnectedDevices,
  rejectDevice,
} from '../websocket/index.js';
import { TOOL_DEFINITIONS } from '../mcp/tools.js';
import { handleToolCall } from '../mcp/handler.js';
import { registerMcpHttpRoutes } from '../mcp/http.js';
import {
  getAgentEventForwardingConfig,
  getLegacyOpenClawSourceToken,
  getSavedAgentEventForwardingToken,
  saveAgentEventForwardingConfig,
  testAgentEventForwardingConnection,
  type AgentEventForwardingConfig,
} from '../event-forwarding/index.js';

const STATUS_FILE = join(homedir(), '.aster', 'status.json');

const LOG_LEVELS: LogEntry['level'][] = ['debug', 'info', 'warn', 'error'];

interface LogQueryString {
  limit?: string;
  offset?: string;
  /** Comma-separated subset of debug,info,warn,error. */
  level?: string;
  search?: string;
  deviceId?: string;
}

function parseLogQuery(q: LogQueryString): LogQuery {
  const levels = (q.level ?? '')
    .split(',')
    .map((l) => l.trim().toLowerCase())
    .filter((l): l is LogEntry['level'] => (LOG_LEVELS as string[]).includes(l));

  return {
    limit: q.limit ? parseInt(q.limit, 10) : 100,
    offset: q.offset ? parseInt(q.offset, 10) : 0,
    levels: levels.length > 0 ? levels : undefined,
    search: q.search?.trim() || undefined,
    deviceId: q.deviceId || undefined,
  };
}

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

  // Get device logs. Supports level/search/offset filtering; returns a page
  // envelope so the UI can paginate instead of guessing at `limit`.
  app.get<{ Params: { id: string }; Querystring: LogQueryString }>(
    '/api/devices/:id/logs',
    async (request) => {
      return queryLogs({ ...parseLogQuery(request.query), deviceId: request.params.id });
    }
  );

  // Get all logs, filtered.
  app.get<{ Querystring: LogQueryString }>('/api/logs', async (request) => {
    return queryLogs(parseLogQuery(request.query));
  });

  // Device ids that appear in the log table, for the log filter dropdown.
  app.get('/api/logs/devices', async () => {
    return getLoggedDeviceIds();
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

  // Runtime status. `~/.aster/status.json` already carries the MCP URL, the
  // dashboard/WS URLs, ports and the Tailscale block, but nothing ever served
  // it — users had to leave the dashboard and read terminal output to find the
  // URL they paste into their MCP client.
  app.get('/api/status', async () => {
    let status: Record<string, unknown> = {};
    try {
      status = JSON.parse(readFileSync(STATUS_FILE, 'utf-8')) as Record<string, unknown>;
    } catch {
      // Not running under the daemon, or the file has not been written yet.
      // Fall back to what this process knows for certain.
    }

    // startDaemon writes startedAt as an ISO string; older files carry an epoch
    // number. Accept both, and never let a malformed value become NaN.
    const startedAtMs = (() => {
      if (typeof status.startedAt === 'number') return status.startedAt;
      if (typeof status.startedAt === 'string') {
        const parsed = Date.parse(status.startedAt);
        return Number.isNaN(parsed) ? null : parsed;
      }
      return null;
    })();

    return {
      ...status,
      startedAt: startedAtMs,
      wsPort: status.wsPort ?? config.wsPort,
      apiPort: status.apiPort ?? config.dashboardPort,
      dbPath: status.dbPath ?? config.dbPath,
      serverTime: Date.now(),
      uptimeMs: startedAtMs === null ? null : Date.now() - startedAtMs,
    };
  });

  // Remove a device. `deleteDevice` has always existed but was CLI-only, so a
  // stale device could not be cleared from the dashboard.
  app.delete<{ Params: { id: string } }>('/api/devices/:id', async (request, reply) => {
    const removed = deleteDevice(request.params.id);
    if (!removed) {
      reply.status(404);
      return { error: 'Device not found' };
    }
    return { success: true };
  });

  // Return a rejected device to pending. Rejecting was previously a dead end:
  // the UI could reject but never undo it.
  app.post<{ Params: { id: string } }>(
    '/api/devices/:id/unreject',
    async (request, reply) => {
      const device = getDevice(request.params.id);
      if (!device) {
        reply.status(404);
        return { error: 'Device not found' };
      }
      updateDeviceStatus(request.params.id, 'pending');
      return { success: true, status: 'pending' };
    }
  );

  // --- Agent Event Forwarding Configuration ---

  const getEventForwardingConfigHandler = async () => {
    const cfg = getAgentEventForwardingConfig();
    const sourceToken = getLegacyOpenClawSourceToken();
    return {
      config: cfg,
      hasSourceToken: !!sourceToken,
      sourceTokenPreview: sourceToken ? `${sourceToken.slice(0, 8)}...` : null,
    };
  };

  // Pre-fill token from ~/.openclaw/openclaw.json (local use only)
  const prefillEventForwardingTokenHandler = async () => {
    const token = getLegacyOpenClawSourceToken();
    return { token: token || null };
  };

  // Save config — empty token preserves existing saved token
  const saveEventForwardingConfigHandler = async (request: { body: Partial<AgentEventForwardingConfig> }) => {
    const body = request.body;

    // If token is empty, preserve existing saved token
    let token = body.token || '';
    if (!token) {
      token = getSavedAgentEventForwardingToken() || '';
    }

    const channelType = body.channelType === 'mattermost' ? 'mattermost' : 'openclaw';
    saveAgentEventForwardingConfig({
      enabled: body.enabled ?? true,
      endpoint: body.endpoint || (channelType === 'mattermost' ? '' : 'http://localhost:18789'),
      webhookPath: channelType === 'mattermost'
        ? (body.webhookPath ?? '')
        : (body.webhookPath || '/hooks/agent'),
      token,
      channelType,
      // OpenClaw delivery defaults to whatsapp. Mattermost empty must not become whatsapp.
      channel: channelType === 'openclaw' ? (body.channel || 'whatsapp') : (body.channel || ''),
      deliverTo: body.deliverTo || '',
      configuredAt: new Date().toISOString(),
      events: {
        notifications: true,
        sms: true,
        deviceConnected: true,
        deviceDisconnected: true,
        pairingRequired: true,
        incomingCalls: true,
        ...body.events,
      },
    });
    return { success: true };
  };

  // Test connection — empty token uses saved token as fallback
  const testEventForwardingConnectionHandler = async (request: {
    body: {
      endpoint: string;
      webhookPath: string;
      token?: string;
      channelType?: AgentEventForwardingConfig['channelType'];
      channel?: string;
    };
  }) => {
    const { endpoint, webhookPath, channelType, channel } = request.body;
    let token = request.body.token || '';
    if (!token) {
      token = getSavedAgentEventForwardingToken() || '';
    }
    return testAgentEventForwardingConnection(endpoint, webhookPath, token, channelType, channel);
  };

  // The legacy routes are registered against the exact same handlers. In
  // particular, mutating POST requests are never redirected.
  app.get('/api/event-forwarding/config', getEventForwardingConfigHandler);
  app.get('/api/openclaw/config', getEventForwardingConfigHandler);
  app.post<{ Body: Partial<AgentEventForwardingConfig> }>('/api/event-forwarding/config', saveEventForwardingConfigHandler);
  app.post<{ Body: Partial<AgentEventForwardingConfig> }>('/api/openclaw/config', saveEventForwardingConfigHandler);
  app.post('/api/event-forwarding/prefill-token', prefillEventForwardingTokenHandler);
  app.post('/api/openclaw/prefill-token', prefillEventForwardingTokenHandler);
  app.post<{ Body: { endpoint: string; webhookPath: string; token?: string; channelType?: AgentEventForwardingConfig['channelType']; channel?: string } }>('/api/event-forwarding/test', testEventForwardingConnectionHandler);
  app.post<{ Body: { endpoint: string; webhookPath: string; token?: string; channelType?: AgentEventForwardingConfig['channelType']; channel?: string } }>('/api/openclaw/test', testEventForwardingConnectionHandler);

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

  return app;
}

export async function startApiServer(config: ServerConfig): Promise<void> {
  const app = createApiServer(config);

  await app.listen({ port: config.dashboardPort, host: '0.0.0.0' });
  consola.success(`API server listening on port ${config.dashboardPort}`);
}

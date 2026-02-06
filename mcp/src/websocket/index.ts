import { WebSocketServer, WebSocket } from 'ws';
import { consola } from 'consola';
import { randomUUID } from 'crypto';
import {
  type AuthMessage,
  AuthMessageSchema,
  type Command,
  type CommandResponse,
  CommandResponseSchema,
  type ConnectedDevice,
  type Device,
  EventSchema,
  HeartbeatSchema,
  type ServerConfig,
} from '../types/index.js';
import {
  addLog,
  getDevice,
  updateDeviceExtendedInfo,
  updateDeviceLastSeen,
  updateDeviceStatus,
  upsertDevice,
} from '../db/index.js';
import { forwardEventToOpenClaw, type DeviceContext } from '../openclaw/index.js';
import type { ExtendedDeviceInfo } from '../types/index.js';

// Store for connected devices
const connectedDevices = new Map<string, ConnectedDevice>();

export function getConnectedDevices(): Map<string, ConnectedDevice> {
  return connectedDevices;
}

export function isDeviceOnline(deviceId: string): boolean {
  return connectedDevices.has(deviceId);
}

export async function sendCommand(
  deviceId: string,
  action: string,
  params?: Record<string, unknown>,
  timeout: number = 30000
): Promise<CommandResponse> {
  const connected = connectedDevices.get(deviceId);

  if (!connected) {
    throw new Error(`Device ${deviceId} is not connected`);
  }

  if (connected.device.status !== 'approved') {
    throw new Error(`Device ${deviceId} is not approved`);
  }

  const commandId = randomUUID();
  const command: Command = {
    type: 'command',
    id: commandId,
    action,
    params,
  };

  return new Promise((resolve, reject) => {
    const timeoutId = setTimeout(() => {
      connected.pendingCommands.delete(commandId);
      reject(new Error(`Command ${action} timed out after ${timeout}ms`));
    }, timeout);

    connected.pendingCommands.set(commandId, {
      resolve,
      reject,
      timeout: timeoutId,
    });

    try {
      connected.ws.send(JSON.stringify(command));
      addLog(deviceId, 'debug', `Sent command: ${action}`, params);
    } catch (err) {
      connected.pendingCommands.delete(commandId);
      clearTimeout(timeoutId);
      reject(err);
    }
  });
}

export function createWebSocketServer(config: ServerConfig): WebSocketServer {
  const wss = new WebSocketServer({ port: config.wsPort });

  consola.info(`WebSocket server listening on port ${config.wsPort}`);

  wss.on('connection', (ws: WebSocket) => {
    consola.debug('New WebSocket connection');
    let deviceId: string | null = null;
    let authTimeout: NodeJS.Timeout | null = null;
    let heartbeatInterval: NodeJS.Timeout | null = null;

    // Require auth within 10 seconds
    authTimeout = setTimeout(() => {
      if (!deviceId) {
        consola.debug('Client did not authenticate in time, closing connection');
        ws.close(4001, 'Authentication timeout');
      }
    }, 10000);

    ws.on('message', (data: Buffer) => {
      try {
        const message = JSON.parse(data.toString());
        handleMessage(ws, message, {
          setDeviceId: (id: string) => {
            deviceId = id;
            if (authTimeout) {
              clearTimeout(authTimeout);
              authTimeout = null;
            }
          },
          getDeviceId: () => deviceId,
          setHeartbeatInterval: (interval: NodeJS.Timeout) => {
            heartbeatInterval = interval;
          },
          config,
        });
      } catch (err) {
        consola.error('Failed to parse message:', err);
      }
    });

    ws.on('close', () => {
      if (authTimeout) clearTimeout(authTimeout);
      if (heartbeatInterval) clearInterval(heartbeatInterval);

      if (deviceId) {
        const connected = connectedDevices.get(deviceId);
        if (connected) {
          const dev = connected.device;
          const deviceCtx: DeviceContext = {
            deviceId, manufacturer: dev.manufacturer, model: dev.model, osVersion: dev.osVersion,
          };
          // Reject all pending commands
          for (const [, pending] of connected.pendingCommands) {
            clearTimeout(pending.timeout);
            pending.reject(new Error('Device disconnected'));
          }
          connectedDevices.delete(deviceId);

          forwardEventToOpenClaw(deviceCtx, 'device_disconnected', {}, Date.now()).catch(() => {});
        }
        addLog(deviceId, 'info', 'Device disconnected');
        consola.info(`Device ${deviceId} disconnected`);
      }
    });

    ws.on('error', (err) => {
      consola.error('WebSocket error:', err);
    });
  });

  return wss;
}

interface MessageContext {
  setDeviceId: (id: string) => void;
  getDeviceId: () => string | null;
  setHeartbeatInterval: (interval: NodeJS.Timeout) => void;
  config: ServerConfig;
}

// Fetch and cache extended device info
export async function fetchAndCacheExtendedInfo(deviceId: string): Promise<ExtendedDeviceInfo | null> {
  try {
    const response = await sendCommand(deviceId, 'get_device_info', {}, 15000);
    if (response.success && response.data) {
      const data = response.data as Record<string, unknown>;
      const extendedInfo: ExtendedDeviceInfo = {
        cpuAbi: (data.cpuAbi as string) || 'unknown',
        supportedAbis: (data.supportedAbis as string[]) || [],
        securityPatch: (data.securityPatch as string | null) ?? null,
        buildType: (data.buildType as string) || 'unknown',
        buildTags: (data.buildTags as string) || '',
        radioVersion: (data.radioVersion as string | null) ?? null,
        totalRam: (data.totalRam as number) || 0,
        availableRam: (data.availableRam as number) || 0,
        totalStorage: (data.totalStorage as number) || 0,
        availableStorage: (data.availableStorage as number) || 0,
        screenRefreshRate: (data.screenRefreshRate as number) || 60,
        screen: data.screen as ExtendedDeviceInfo['screen'],
        batteryCapacity: (data.batteryCapacity as number | null) ?? null,
        uptimeMillis: (data.uptimeMillis as number) || 0,
        timezone: (data.timezone as string) || 'UTC',
        locale: (data.locale as string) || 'en_US',
      };
      updateDeviceExtendedInfo(deviceId, extendedInfo);
      consola.debug(`Cached extended info for device ${deviceId}`);
      return extendedInfo;
    }
  } catch (err) {
    consola.debug(`Failed to fetch extended info for ${deviceId}:`, err);
  }
  return null;
}

function handleMessage(
  ws: WebSocket,
  message: unknown,
  ctx: MessageContext
): void {
  const { setDeviceId, getDeviceId, setHeartbeatInterval, config } = ctx;

  // Handle auth message
  const authResult = AuthMessageSchema.safeParse(message);
  if (authResult.success) {
    handleAuth(ws, authResult.data, setDeviceId, setHeartbeatInterval, config);
    return;
  }

  // Handle command response
  const responseResult = CommandResponseSchema.safeParse(message);
  if (responseResult.success) {
    handleCommandResponse(responseResult.data, getDeviceId);
    return;
  }

  // Handle event
  const eventResult = EventSchema.safeParse(message);
  if (eventResult.success) {
    const deviceId = getDeviceId();
    if (deviceId) {
      addLog(deviceId, 'debug', `Event: ${eventResult.data.eventType}`, eventResult.data.data);
      const dev = connectedDevices.get(deviceId)?.device;
      const deviceCtx: DeviceContext = {
        deviceId,
        manufacturer: dev?.manufacturer ?? '',
        model: dev?.model ?? '',
        osVersion: dev?.osVersion ?? '',
      };
      forwardEventToOpenClaw(
        deviceCtx,
        eventResult.data.eventType,
        eventResult.data.data as Record<string, unknown>,
        eventResult.data.timestamp
      ).catch((err) => {
        consola.debug('OpenClaw forward failed:', err);
      });
    }
    return;
  }

  // Handle heartbeat
  const heartbeatResult = HeartbeatSchema.safeParse(message);
  if (heartbeatResult.success) {
    const deviceId = getDeviceId();
    if (deviceId) {
      const connected = connectedDevices.get(deviceId);
      if (connected) {
        connected.lastHeartbeat = Date.now();
        updateDeviceLastSeen(deviceId, connected.lastHeartbeat);
      }
      ws.send(JSON.stringify({ type: 'heartbeat_ack', timestamp: Date.now() }));
    }
    return;
  }

  consola.warn('Unknown message type:', message);
}

function handleAuth(
  ws: WebSocket,
  authMsg: AuthMessage,
  setDeviceId: (id: string) => void,
  setHeartbeatInterval: (interval: NodeJS.Timeout) => void,
  config: ServerConfig
): void {
  const { deviceId, deviceName, model, manufacturer, platform, osVersion } = authMsg;

  // Check if device exists
  let device = getDevice(deviceId);

  if (!device) {
    // New device - create pending entry
    device = upsertDevice({
      id: deviceId,
      name: deviceName,
      model,
      manufacturer,
      platform,
      osVersion,
      status: 'pending',
      lastSeen: Date.now(),
    });
    addLog(deviceId, 'info', 'New device registered, pending approval');
    consola.info(`New device registered: ${deviceName} (${deviceId})`);
  } else {
    // Update device info
    device = upsertDevice({
      ...device,
      name: deviceName,
      model,
      manufacturer,
      osVersion,
      lastSeen: Date.now(),
    });
  }

  // Store connection
  setDeviceId(deviceId);
  connectedDevices.set(deviceId, {
    device,
    ws: ws as unknown as WebSocket,
    lastHeartbeat: Date.now(),
    pendingCommands: new Map(),
  });

  // Setup heartbeat check
  const heartbeatCheck = setInterval(() => {
    const connected = connectedDevices.get(deviceId);
    if (connected) {
      const elapsed = Date.now() - connected.lastHeartbeat;
      if (elapsed > config.heartbeatTimeout) {
        consola.warn(`Device ${deviceId} heartbeat timeout, closing connection`);
        ws.close(4002, 'Heartbeat timeout');
        clearInterval(heartbeatCheck);
      }
    }
  }, config.heartbeatInterval);
  setHeartbeatInterval(heartbeatCheck);

  // Send auth result
  ws.send(JSON.stringify({
    type: 'auth_result',
    success: true,
    status: device.status,
    message: device.status === 'approved'
      ? 'Connected and approved'
      : 'Connected, pending approval',
  }));

  addLog(deviceId, 'info', `Device connected with status: ${device.status}`);
  consola.success(`Device ${deviceName} connected (status: ${device.status})`);

  // Forward connection events to OpenClaw
  const deviceCtx: DeviceContext = { deviceId, manufacturer, model, osVersion };
  if (device.status === 'approved') {
    forwardEventToOpenClaw(deviceCtx, 'device_connected', {}, Date.now()).catch(() => {});
  } else if (device.status === 'pending') {
    forwardEventToOpenClaw(deviceCtx, 'pairing_required', {}, Date.now()).catch(() => {});
  }

  // Auto-fetch extended info for approved devices (with slight delay to ensure connection is stable)
  if (device.status === 'approved') {
    setTimeout(() => {
      fetchAndCacheExtendedInfo(deviceId).catch((err) => {
        consola.debug(`Failed to fetch extended info for ${deviceId}:`, err);
      });
    }, 1500);
  }
}

function handleCommandResponse(
  response: CommandResponse,
  getDeviceId: () => string | null
): void {
  const deviceId = getDeviceId();
  if (!deviceId) return;

  const connected = connectedDevices.get(deviceId);
  if (!connected) return;

  const pending = connected.pendingCommands.get(response.id);
  if (!pending) {
    consola.warn(`Received response for unknown command: ${response.id}`);
    return;
  }

  clearTimeout(pending.timeout);
  connected.pendingCommands.delete(response.id);

  if (response.success) {
    pending.resolve(response);
  } else {
    pending.reject(new Error(response.error || 'Command failed'));
  }

  addLog(deviceId, response.success ? 'debug' : 'warn', `Command response: ${response.id}`, {
    success: response.success,
    error: response.error,
  });
}

// Approve a pending device
export function approveDevice(deviceId: string): boolean {
  const success = updateDeviceStatus(deviceId, 'approved');
  if (success) {
    const connected = connectedDevices.get(deviceId);
    if (connected) {
      connected.device.status = 'approved';
      connected.ws.send(JSON.stringify({
        type: 'auth_result',
        success: true,
        status: 'approved',
        message: 'Device approved',
      }));
      // Fetch extended info now that device is approved
      setTimeout(() => {
        fetchAndCacheExtendedInfo(deviceId).catch((err) => {
          consola.debug(`Failed to fetch extended info for ${deviceId}:`, err);
        });
      }, 500);
    }
    addLog(deviceId, 'info', 'Device approved');
    consola.success(`Device ${deviceId} approved`);
  }
  return success;
}

// Reject a device
export function rejectDevice(deviceId: string): boolean {
  const success = updateDeviceStatus(deviceId, 'rejected');
  if (success) {
    const connected = connectedDevices.get(deviceId);
    if (connected) {
      connected.device.status = 'rejected';
      connected.ws.send(JSON.stringify({
        type: 'auth_result',
        success: false,
        status: 'rejected',
        message: 'Device rejected',
      }));
      connected.ws.close(4003, 'Device rejected');
    }
    addLog(deviceId, 'info', 'Device rejected');
    consola.warn(`Device ${deviceId} rejected`);
  }
  return success;
}

import { z } from 'zod';
import type { WebSocket } from 'ws';

// Device status enum
export const DeviceStatus = {
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
} as const;

export type DeviceStatus = (typeof DeviceStatus)[keyof typeof DeviceStatus];

// Device platform
export const Platform = {
  ANDROID: 'android',
  IOS: 'ios',
} as const;

export type Platform = (typeof Platform)[keyof typeof Platform];

// Extended device info (fetched from device on-demand)
export interface ExtendedDeviceInfo {
  // CPU/ABI
  cpuAbi: string;
  supportedAbis: string[];
  // Build info
  securityPatch: string | null;
  buildType: string;
  buildTags: string;
  radioVersion: string | null;
  // Memory (MB)
  totalRam: number;
  availableRam: number;
  // Storage (GB)
  totalStorage: number;
  availableStorage: number;
  // Display
  screenRefreshRate: number;
  screen?: {
    widthPixels: number;
    heightPixels: number;
    density: number;
    densityDpi: number;
  };
  // Battery
  batteryCapacity: number | null;
  // System
  uptimeMillis: number;
  timezone: string;
  locale: string;
}

// Device schema
export const DeviceSchema = z.object({
  id: z.string(),
  name: z.string(),
  model: z.string(),
  manufacturer: z.string(),
  platform: z.enum(['android', 'ios']),
  osVersion: z.string(),
  status: z.enum(['pending', 'approved', 'rejected']),
  lastSeen: z.number(),
  createdAt: z.number(),
  // Optional cached extended info
  extendedInfo: z.custom<ExtendedDeviceInfo>().optional(),
});

export type Device = z.infer<typeof DeviceSchema>;

// WebSocket message types
export const WsMessageType = {
  // Client -> Server
  AUTH: 'auth',
  COMMAND_RESPONSE: 'command_response',
  EVENT: 'event',
  HEARTBEAT: 'heartbeat',
  // Server -> Client
  AUTH_RESULT: 'auth_result',
  COMMAND: 'command',
  HEARTBEAT_ACK: 'heartbeat_ack',
} as const;

export type WsMessageType = (typeof WsMessageType)[keyof typeof WsMessageType];

// Auth message from device
export const AuthMessageSchema = z.object({
  type: z.literal('auth'),
  deviceId: z.string(),
  deviceName: z.string(),
  model: z.string(),
  manufacturer: z.string(),
  platform: z.enum(['android', 'ios']),
  osVersion: z.string(),
  appVersion: z.string(),
});

export type AuthMessage = z.infer<typeof AuthMessageSchema>;

// Auth result from server
export const AuthResultSchema = z.object({
  type: z.literal('auth_result'),
  success: z.boolean(),
  status: z.enum(['pending', 'approved', 'rejected']),
  message: z.string().optional(),
});

export type AuthResult = z.infer<typeof AuthResultSchema>;

// Command from server to device
export const CommandSchema = z.object({
  type: z.literal('command'),
  id: z.string(),
  action: z.string(),
  params: z.record(z.unknown()).optional(),
});

export type Command = z.infer<typeof CommandSchema>;

// Command response from device
export const CommandResponseSchema = z.object({
  type: z.literal('command_response'),
  id: z.string(),
  success: z.boolean(),
  data: z.unknown().nullish(),
  error: z.string().nullish(),
});

export type CommandResponse = z.infer<typeof CommandResponseSchema>;

// Event from device (notifications, SMS, etc.)
export const EventSchema = z.object({
  type: z.literal('event'),
  eventType: z.string(),
  data: z.record(z.unknown()),
  timestamp: z.number(),
});

export type Event = z.infer<typeof EventSchema>;

// Heartbeat
export const HeartbeatSchema = z.object({
  type: z.literal('heartbeat'),
  timestamp: z.number(),
});

export type Heartbeat = z.infer<typeof HeartbeatSchema>;

// Generic WebSocket message
export const WsMessageSchema = z.discriminatedUnion('type', [
  AuthMessageSchema,
  AuthResultSchema,
  CommandSchema,
  CommandResponseSchema,
  EventSchema,
  HeartbeatSchema,
  z.object({ type: z.literal('heartbeat_ack'), timestamp: z.number() }),
]);

export type WsMessage = z.infer<typeof WsMessageSchema>;

// Log entry
export const LogEntrySchema = z.object({
  id: z.number(),
  deviceId: z.string(),
  level: z.enum(['debug', 'info', 'warn', 'error']),
  message: z.string(),
  data: z.string().optional(),
  timestamp: z.number(),
});

export type LogEntry = z.infer<typeof LogEntrySchema>;

// MCP Tool definitions
export interface McpToolDefinition {
  name: string;
  description: string;
  inputSchema: z.ZodType;
}

// Connected device state (runtime)
export interface ConnectedDevice {
  device: Device;
  ws: WebSocket;
  lastHeartbeat: number;
  pendingCommands: Map<string, {
    resolve: (value: CommandResponse) => void;
    reject: (error: Error) => void;
    timeout: NodeJS.Timeout;
  }>;
}

// Server configuration
export const ServerConfigSchema = z.object({
  wsPort: z.number().default(3000),
  dashboardPort: z.number().default(3001),
  dbPath: z.string().default('./aster.db'),
  commandTimeout: z.number().default(30000),
  heartbeatInterval: z.number().default(30000),
  heartbeatTimeout: z.number().default(90000),
});

export type ServerConfig = z.infer<typeof ServerConfigSchema>;

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

export interface Device {
  id: string;
  name: string;
  model: string;
  manufacturer: string;
  platform: 'android' | 'ios';
  osVersion: string;
  status: 'pending' | 'approved' | 'rejected';
  lastSeen: number;
  createdAt: number;
  online: boolean;
  // Cached extended info
  extendedInfo?: ExtendedDeviceInfo;
}

export interface DeviceWithLiveInfo extends Device {
  liveInfo: ExtendedDeviceInfo | null;
  message?: string;
  error?: string;
}

export interface Stats {
  totalDevices: number;
  onlineDevices: number;
  pendingDevices: number;
  approvedDevices: number;
}

export interface LogEntry {
  id: number;
  deviceId: string;
  level: 'debug' | 'info' | 'warn' | 'error';
  message: string;
  data?: string;
  timestamp: number;
}

export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: {
    type: 'object';
    properties: Record<string, any>;
    required?: string[];
  };
}

export interface ToolResult {
  content: Array<{
    type: 'text' | 'image';
    text?: string;
    data?: string;
    mimeType?: string;
  }>;
  isError?: boolean;
}

export interface FileEntry {
  name: string;
  path: string;
  isDirectory: boolean;
  isFile?: boolean;
  size: number;
  lastModified: string; // Format: "2026-01-09 12:54:48"
  canRead?: boolean;
  canWrite?: boolean;
  isHidden?: boolean;
  extension?: string;
}

export interface FileListResult {
  files: FileEntry[];
  path: string;
}

export interface FileContentResult {
  content: string;
  encoding: 'text' | 'base64';
  size: number;
  truncated?: boolean;
  mimeType?: string;
}

export interface AgentEventForwardingConfig {
  enabled: boolean;
  endpoint: string;
  webhookPath: string;
  token: string;
  hasToken: boolean;
  channel: string;
  deliverTo: string;
  configuredAt: string;
  events: {
    notifications: boolean;
    sms: boolean;
    deviceConnected: boolean;
    deviceDisconnected: boolean;
    pairingRequired: boolean;
  };
}

export interface AgentEventForwardingConfigResponse {
  config: AgentEventForwardingConfig | null;
  hasSourceToken: boolean;
  sourceTokenPreview: string | null;
}

export interface AgentEventForwardingTestResult {
  success: boolean;
  status?: number;
  error?: string;
}

export function useApi() {
  const config = useRuntimeConfig();
  const baseUrl = config.public.apiUrl;

  async function fetchJson<T>(path: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });

    // Always try to parse the response body
    const data = await response.json().catch(() => null);

    if (!response.ok) {
      // If the response has the ToolResult error format, return it as-is
      // so the UI can display the detailed error message
      if (data && typeof data === 'object' && 'isError' in data) {
        return data as T;
      }
      // Otherwise throw with whatever message we have
      const errorMessage = data?.error || data?.message || `API error: ${response.status}`;
      throw new Error(errorMessage);
    }

    return data as T;
  }

  const getAgentEventForwardingConfig = () =>
    fetchJson<AgentEventForwardingConfigResponse>('/api/event-forwarding/config');
  const prefillAgentEventForwardingToken = () =>
    fetchJson<{ token: string | null }>('/api/event-forwarding/prefill-token', { method: 'POST' });
  const saveAgentEventForwardingConfig = (eventForwardingConfig: {
    enabled: boolean;
    endpoint: string;
    webhookPath: string;
    token: string;
    channel: string;
    deliverTo: string;
    events: {
      notifications: boolean;
      sms: boolean;
      deviceConnected: boolean;
      deviceDisconnected: boolean;
      pairingRequired: boolean;
    };
  }) => fetchJson<{ success: boolean }>('/api/event-forwarding/config', {
    method: 'POST',
    body: JSON.stringify(eventForwardingConfig),
  });
  const testAgentEventForwardingConnection = (endpoint: string, webhookPath: string, token?: string) =>
    fetchJson<AgentEventForwardingTestResult>('/api/event-forwarding/test', {
      method: 'POST',
      body: JSON.stringify({ endpoint, webhookPath, token: token || '' }),
    });

  return {
    // Stats
    getStats: () => fetchJson<Stats>('/api/stats'),

    // Devices
    getDevices: () => fetchJson<Device[]>('/api/devices'),
    getDevice: (id: string) => fetchJson<Device>(`/api/devices/${id}`),
    getDeviceInfo: (id: string) => fetchJson<DeviceWithLiveInfo>(`/api/devices/${id}/info`),
    approveDevice: (id: string) =>
      fetchJson<{ success: boolean }>(`/api/devices/${id}/approve`, { method: 'POST', body: '{}' }),
    rejectDevice: (id: string) =>
      fetchJson<{ success: boolean }>(`/api/devices/${id}/reject`, { method: 'POST', body: '{}' }),

    // Logs
    getLogs: (limit = 100) => fetchJson<LogEntry[]>(`/api/logs?limit=${limit}`),
    getDeviceLogs: (deviceId: string, limit = 100) =>
      fetchJson<LogEntry[]>(`/api/devices/${deviceId}/logs?limit=${limit}`),

    // Tools
    getTools: () => fetchJson<ToolDefinition[]>('/api/tools'),
    executeTool: (deviceId: string, name: string, args: Record<string, any>) =>
      fetchJson<ToolResult>(`/api/devices/${deviceId}/execute`, {
        method: 'POST',
        body: JSON.stringify({ name, args }),
      }),

    // Health
    getHealth: () => fetchJson<{ status: string; timestamp: number }>('/api/health'),

    // Agent event forwarding
    getAgentEventForwardingConfig,
    prefillAgentEventForwardingToken,
    saveAgentEventForwardingConfig,
    testAgentEventForwardingConnection,

    // Deprecated zero-logic aliases for dashboard extensions using the old API.
    getOpenClawConfig: getAgentEventForwardingConfig,
    prefillOpenClawToken: prefillAgentEventForwardingToken,
    saveOpenClawConfig: saveAgentEventForwardingConfig,
    testOpenClawConnection: testAgentEventForwardingConnection,
  };
}

/** @deprecated Use AgentEventForwardingConfig. */
export type OpenClawConfig = AgentEventForwardingConfig;
/** @deprecated Use AgentEventForwardingConfigResponse. */
export type OpenClawConfigResponse = AgentEventForwardingConfigResponse;
/** @deprecated Use AgentEventForwardingTestResult. */
export type OpenClawTestResult = AgentEventForwardingTestResult;

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
  channelType?: 'openclaw' | 'mattermost';
  events: {
    notifications: boolean;
    sms: boolean;
    deviceConnected: boolean;
    deviceDisconnected: boolean;
    pairingRequired: boolean;
    incomingCalls?: boolean;
  };
}

export interface AgentEventForwardingConfigResponse {
  config: AgentEventForwardingConfig | null;
  hasSourceToken: boolean;
  sourceTokenPreview: string | null;
}

/** Runtime status, read from ~/.aster/status.json by GET /api/status. */
export interface ServerStatus {
  pid?: number;
  startedAt?: number;
  wsPort: number;
  wsUrl?: string;
  apiPort: number;
  apiUrl?: string;
  mcpUrl?: string;
  dashboardPort?: number;
  dashboardUrl?: string;
  dbPath?: string;
  tailscale?: {
    ip?: string;
    dns?: string;
    wsUrl?: string;
    dashboardUrl?: string;
    mcpUrl?: string;
  } | null;
  serverTime: number;
  uptimeMs: number | null;
}

/** Paged log response. Replaces the bare LogEntry[] the routes used to return. */
export interface LogPage {
  logs: LogEntry[];
  total: number;
  limit: number;
  offset: number;
}

export interface LogFilters {
  limit?: number;
  offset?: number;
  levels?: LogEntry['level'][];
  search?: string;
}

export interface AgentEventForwardingTestResult {
  success: boolean;
  status?: number;
  error?: string;
}

function logQuery(filters: LogFilters): string {
  const params = new URLSearchParams();
  if (filters.limit != null) params.set('limit', String(filters.limit));
  if (filters.offset) params.set('offset', String(filters.offset));
  if (filters.levels?.length) params.set('level', filters.levels.join(','));
  if (filters.search) params.set('search', filters.search);
  const qs = params.toString();
  return qs ? `?${qs}` : '';
}

/**
 * Build a displayable src for an image content block.
 *
 * The server returns bare base64 in `data` with the media type in a separate
 * `mimeType` field. Binding `data` straight to an <img src> — which the old
 * tool explorer did — produces a broken image for every screenshot and photo.
 */
export function toolImageSrc(item: { data?: string; mimeType?: string }): string {
  if (!item.data) return '';
  if (item.data.startsWith('data:')) return item.data;
  return `data:${item.mimeType || 'image/png'};base64,${item.data}`;
}

/** Concatenated text of a tool result, for display or JSON parsing. */
export function toolText(result: ToolResult | null | undefined): string {
  if (!result?.content) return '';
  return result.content
    .filter((c) => c.type === 'text' && c.text)
    .map((c) => c.text)
    .join('\n');
}

/**
 * Parse a tool result whose text payload is JSON.
 *
 * Nearly every device tool answers with a JSON document inside a text block,
 * so each caller was otherwise re-implementing this try/catch.
 */
export function toolJson<T>(result: ToolResult | null | undefined): T | null {
  const text = toolText(result);
  if (!text) return null;
  try {
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
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
    channelType: 'openclaw' | 'mattermost';
    channel: string;
    deliverTo: string;
    events: {
      notifications: boolean;
      sms: boolean;
      deviceConnected: boolean;
      deviceDisconnected: boolean;
      pairingRequired: boolean;
      incomingCalls: boolean;
    };
  }) => fetchJson<{ success: boolean }>('/api/event-forwarding/config', {
    method: 'POST',
    body: JSON.stringify(eventForwardingConfig),
  });
  const testAgentEventForwardingConnection = (
    endpoint: string,
    webhookPath: string,
    token?: string,
    channelType?: 'openclaw' | 'mattermost',
    channel?: string,
  ) =>
    fetchJson<AgentEventForwardingTestResult>('/api/event-forwarding/test', {
      method: 'POST',
      body: JSON.stringify({
        endpoint,
        webhookPath,
        token: token || '',
        channelType,
        channel,
      }),
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
    unrejectDevice: (id: string) =>
      fetchJson<{ success: boolean }>(`/api/devices/${id}/unreject`, { method: 'POST', body: '{}' }),
    deleteDevice: (id: string) =>
      fetchJson<{ success: boolean }>(`/api/devices/${id}`, { method: 'DELETE' }),

    // Logs
    getLogs: (filters: LogFilters = {}) => fetchJson<LogPage>(`/api/logs${logQuery(filters)}`),
    getDeviceLogs: (deviceId: string, filters: LogFilters = {}) =>
      fetchJson<LogPage>(`/api/devices/${deviceId}/logs${logQuery(filters)}`),
    getLogDeviceIds: () => fetchJson<string[]>('/api/logs/devices'),

    // Tools
    getTools: () => fetchJson<ToolDefinition[]>('/api/tools'),
    executeTool: (deviceId: string, name: string, args: Record<string, any>) =>
      fetchJson<ToolResult>(`/api/devices/${deviceId}/execute`, {
        method: 'POST',
        body: JSON.stringify({ name, args }),
      }),

    // Health and runtime status
    getHealth: () => fetchJson<{ status: string; timestamp: number }>('/api/health'),
    getStatus: () => fetchJson<ServerStatus>('/api/status'),

    // Agent event forwarding
    getAgentEventForwardingConfig,
    prefillAgentEventForwardingToken,
    saveAgentEventForwardingConfig,
    testAgentEventForwardingConnection,
  };
}

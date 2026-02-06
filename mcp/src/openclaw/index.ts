import { consola } from 'consola';
import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { homedir } from 'os';

export interface OpenClawConfig {
  enabled: boolean;
  endpoint: string;
  webhookPath: string;
  token: string;
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

let config: OpenClawConfig | null = null;

const OPENCLAW_CONFIG_PATH = join(homedir(), '.aster', 'openclaw.json');
const OPENCLAW_SOURCE_CONFIG_PATH = join(homedir(), '.openclaw', 'openclaw.json');

export function loadOpenClawConfig(): void {
  try {
    if (!existsSync(OPENCLAW_CONFIG_PATH)) {
      consola.debug('No OpenClaw config found, event forwarding disabled');
      return;
    }
    const raw = readFileSync(OPENCLAW_CONFIG_PATH, 'utf-8');
    config = JSON.parse(raw) as OpenClawConfig;
    if (config.enabled) {
      consola.info(`OpenClaw event forwarding enabled → ${config.endpoint}${config.webhookPath}`);
    }
  } catch (err) {
    consola.warn('Failed to load OpenClaw config:', err);
    config = null;
  }
}

export function isOpenClawEnabled(): boolean {
  return config?.enabled === true;
}

/**
 * Device info passed alongside every event for context.
 */
export interface DeviceContext {
  deviceId: string;
  manufacturer: string;
  model: string;
  osVersion: string;
}

/**
 * Serialize a data object into [data-key] value lines.
 * Nested objects become [data-parent-child] value.
 */
function formatData(obj: Record<string, unknown>, prefix = 'data'): string {
  const lines: string[] = [];
  for (const [key, value] of Object.entries(obj)) {
    if (value === null || value === undefined || value === '') continue;
    if (typeof value === 'object' && !Array.isArray(value)) {
      lines.push(formatData(value as Record<string, unknown>, `${prefix}-${key}`));
    } else {
      lines.push(`[${prefix}-${key}] ${value}`);
    }
  }
  return lines.join('\n');
}

/**
 * Build structured event text for the AI agent.
 *
 * Standard format — 4 fixed headers + [data-*] fields:
 *   [skill] aster
 *   [event] <event_name>
 *   [device_id] <id>
 *   [model] <manufacturer model, Android version>
 *   [data-key] value
 *   [data-key] value
 */
function buildEventText(device: DeviceContext, eventType: string, data: Record<string, unknown>): string {
  const model = [device.manufacturer, device.model].filter(Boolean).join(' ') || 'unknown';
  const modelWithOs = device.osVersion ? `${model}, Android ${device.osVersion}` : model;

  // Map internal event types to clean event names
  const eventName =
    eventType === 'sms_received' ? 'sms' :
    eventType === 'device_connected' ? 'device_online' :
    eventType === 'device_disconnected' ? 'device_offline' :
    eventType === 'pairing_required' ? 'pairing' :
    eventType;

  // Standard headers
  const lines = [
    '[skill] aster',
    `[event] ${eventName}`,
    `[device_id] ${device.deviceId}`,
    `[model] ${modelWithOs}`,
  ];

  // Build event-specific data fields
  if (eventType === 'sms_received') {
    lines.push(`[data-sender] ${data.sender ?? 'unknown'}`);
    lines.push(`[data-body] ${data.body ?? ''}`);
  } else if (eventType === 'notification') {
    const pkg = String(data.packageName ?? 'unknown');
    lines.push(`[data-app] ${pkg.split('.').pop() || pkg}`);
    lines.push(`[data-package] ${pkg}`);
    lines.push(`[data-title] ${data.title ?? ''}`);
    lines.push(`[data-text] ${data.text ?? ''}`);
  } else if (eventType === 'device_connected') {
    lines.push('[data-status] connected');
  } else if (eventType === 'device_disconnected') {
    lines.push('[data-status] disconnected');
  } else if (eventType === 'pairing_required') {
    lines.push('[data-status] pending_approval');
    lines.push('[data-action] approve this device from the Aster dashboard or via aster devices approve');
  } else {
    // Generic: serialize all data fields
    const dataLines = formatData(data);
    if (dataLines) lines.push(dataLines);
  }

  return lines.join('\n');
}

export async function forwardEventToOpenClaw(
  device: DeviceContext,
  eventType: string,
  eventData: Record<string, unknown>,
  timestamp: number
): Promise<void> {
  if (!config?.enabled) return;

  // Check if this event type is enabled
  if (eventType === 'sms_received' && !config.events.sms) return;
  if (eventType === 'notification' && !config.events.notifications) return;
  if (eventType === 'device_connected' && !config.events.deviceConnected) return;
  if (eventType === 'device_disconnected' && !config.events.deviceDisconnected) return;
  if (eventType === 'pairing_required' && !config.events.pairingRequired) return;

  const url = `${config.endpoint}${config.webhookPath}`;
  const payload: Record<string, unknown> = {
    message: buildEventText(device, eventType, eventData),
    wakeMode: 'now',
    deliver: true,
  };
  if (config.channel) payload.channel = config.channel;
  if (config.deliverTo) payload.to = config.deliverTo;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (config.token) {
    headers['Authorization'] = `Bearer ${config.token}`;
  }

  consola.info(`OpenClaw POST ${url} →`, JSON.stringify(payload));

  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 10000);

      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      if (response.ok) {
        consola.debug(`OpenClaw event forwarded: ${eventType}`);
        return;
      }

      // No retry on auth errors
      if (response.status === 401 || response.status === 403) {
        consola.error(`OpenClaw auth error (${response.status}) for ${eventType}`);
        return;
      }

      consola.warn(`OpenClaw returned ${response.status} for ${eventType}`);
    } catch (err) {
      consola.warn(`OpenClaw request failed (attempt ${attempt + 1}):`, err);
    }

    // Wait 1s before retry
    if (attempt === 0) {
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }
}

/**
 * Get the current OpenClaw config (for API).
 * Returns the config with the token masked.
 */
export function getOpenClawConfig(): (Omit<OpenClawConfig, 'token'> & { token: string; hasToken: boolean }) | null {
  // Re-read from disk to get latest
  try {
    if (!existsSync(OPENCLAW_CONFIG_PATH)) return null;
    const raw = readFileSync(OPENCLAW_CONFIG_PATH, 'utf-8');
    const cfg = JSON.parse(raw) as OpenClawConfig;
    return {
      ...cfg,
      token: cfg.token ? `${cfg.token.slice(0, 8)}...` : '',
      hasToken: !!cfg.token,
    };
  } catch {
    return null;
  }
}

/**
 * Get the raw saved token from ~/.aster/openclaw.json (for re-saving / testing fallback).
 */
export function getOpenClawSavedToken(): string | null {
  try {
    if (!existsSync(OPENCLAW_CONFIG_PATH)) return null;
    const raw = readFileSync(OPENCLAW_CONFIG_PATH, 'utf-8');
    const cfg = JSON.parse(raw) as OpenClawConfig;
    return cfg.token || null;
  } catch {
    return null;
  }
}

/**
 * Try to read the OpenClaw token from ~/.openclaw/openclaw.json
 */
export function getOpenClawSourceToken(): string | null {
  try {
    if (!existsSync(OPENCLAW_SOURCE_CONFIG_PATH)) return null;
    const raw = readFileSync(OPENCLAW_SOURCE_CONFIG_PATH, 'utf-8');
    const cfg = JSON.parse(raw);
    return cfg?.gateway?.auth?.token || null;
  } catch {
    return null;
  }
}

/**
 * Save OpenClaw config and reload it into memory.
 */
export function saveOpenClawConfig(newConfig: OpenClawConfig): void {
  const dir = dirname(OPENCLAW_CONFIG_PATH);
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
  writeFileSync(OPENCLAW_CONFIG_PATH, JSON.stringify(newConfig, null, 2));
  // Reload into memory
  config = newConfig;
  consola.info(`OpenClaw config saved and reloaded (enabled: ${newConfig.enabled})`);
}

/**
 * Test connectivity to an OpenClaw endpoint.
 */
export async function testOpenClawConnection(
  endpoint: string,
  webhookPath: string,
  token: string
): Promise<{ success: boolean; status?: number; error?: string }> {
  const url = `${endpoint}${webhookPath}`;
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);

    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify({ message: '[skill] aster\n[event] test\n[data]\nstatus: connection test from dashboard', wakeMode: 'now', deliver: false }),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (response.ok) {
      return { success: true, status: response.status };
    }

    // Try to read error details from response body
    let errorDetail = '';
    try {
      const body = await response.text();
      if (body) {
        try {
          const json = JSON.parse(body);
          errorDetail = json.error || json.message || json.detail || body.slice(0, 200);
        } catch {
          errorDetail = body.slice(0, 200);
        }
      }
    } catch { /* ignore body read failures */ }

    const statusText = response.status === 401 ? 'Unauthorized — check your token'
      : response.status === 403 ? 'Forbidden — token lacks permission'
      : response.status === 404 ? 'Not found — check webhook path'
      : response.status >= 500 ? `Server error (${response.status})`
      : `HTTP ${response.status}`;

    return {
      success: false,
      status: response.status,
      error: errorDetail ? `${statusText}: ${errorDetail}` : statusText,
    };
  } catch (err: any) {
    // Provide better messages for common network errors
    const cause = err.cause;
    if (cause?.code === 'ECONNREFUSED') {
      return { success: false, error: `Connection refused — is OpenClaw running at ${endpoint}?` };
    }
    if (cause?.code === 'ENOTFOUND') {
      return { success: false, error: `Host not found — check the endpoint URL` };
    }
    if (cause?.code === 'ETIMEDOUT' || err.name === 'AbortError') {
      return { success: false, error: `Connection timed out after 5s — endpoint may be unreachable` };
    }
    return { success: false, error: err.message || 'Connection failed' };
  }
}

import { consola } from 'consola';
import {
  closeSync,
  existsSync,
  fsyncSync,
  mkdirSync,
  openSync,
  readFileSync,
  renameSync,
  statSync,
  unlinkSync,
  writeFileSync,
} from 'fs';
import { randomUUID } from 'crypto';
import { homedir } from 'os';
import { basename, dirname, join } from 'path';

export {
  getLegacyOpenClawSourceToken,
  LEGACY_OPENCLAW_SOURCE_CONFIG_PATH,
} from './legacy-openclaw-importer.js';

export interface AgentEventForwardingConfig {
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

export interface DeviceContext {
  deviceId: string;
  manufacturer: string;
  model: string;
  osVersion: string;
}

export const AGENT_EVENT_FORWARDING_CONFIG_PATH = join(
  homedir(),
  '.aster',
  'event-forwarding.json',
);

export const LEGACY_OPENCLAW_EVENT_FORWARDING_CONFIG_PATH = join(
  homedir(),
  '.aster',
  'openclaw.json',
);

let config: AgentEventForwardingConfig | null = null;

function syncDirectory(directory: string): void {
  let fd: number | undefined;
  try {
    fd = openSync(directory, 'r');
    fsyncSync(fd);
  } catch {
    // Directory fsync is not available on every supported filesystem.
  } finally {
    if (fd !== undefined) closeSync(fd);
  }
}

function writeAtomically(path: string, contents: string, mode = 0o600): void {
  const directory = dirname(path);
  mkdirSync(directory, { recursive: true });

  const temporaryPath = join(
    directory,
    `.${basename(path)}.${process.pid}.${randomUUID()}.tmp`,
  );
  let fd: number | undefined;

  try {
    fd = openSync(temporaryPath, 'wx', mode);
    writeFileSync(fd, contents, 'utf-8');
    fsyncSync(fd);
    closeSync(fd);
    fd = undefined;
    renameSync(temporaryPath, path);
    syncDirectory(directory);
  } catch (error) {
    if (fd !== undefined) closeSync(fd);
    try { unlinkSync(temporaryPath); } catch { /* best-effort cleanup */ }
    throw error;
  }
}

interface ConfigCandidate {
  path: string;
  raw: string;
  config: AgentEventForwardingConfig;
  modifiedAtMs: number;
}

function readConfigCandidate(path: string): ConfigCandidate | null {
  if (!existsSync(path)) return null;
  try {
    const raw = readFileSync(path, 'utf-8');
    return {
      path,
      raw,
      config: JSON.parse(raw) as AgentEventForwardingConfig,
      modifiedAtMs: statSync(path).mtimeMs,
    };
  } catch (error) {
    consola.warn(`Ignoring unreadable event-forwarding config at ${path}:`, error);
    return null;
  }
}

/**
 * Reconcile the canonical and compatibility files before every read.
 *
 * During the rollback window both files are valid entry points: a current
 * release writes both, while an older binary may update only the legacy path.
 * The newest valid copy wins and atomically repairs the other side. Writing the
 * canonical file first means a process death between the two save writes is
 * also repaired on the next read instead of resurrecting stale credentials.
 */
function resolveReadableConfigPath(): string | null {
  const canonical = readConfigCandidate(AGENT_EVENT_FORWARDING_CONFIG_PATH);
  const legacy = readConfigCandidate(LEGACY_OPENCLAW_EVENT_FORWARDING_CONFIG_PATH);
  if (!canonical && !legacy) return null;

  const winner = canonical && legacy
    ? (legacy.modifiedAtMs > canonical.modifiedAtMs ? legacy : canonical)
    : canonical ?? legacy!;

  try {
    if (!canonical || canonical.raw !== winner.raw) {
      writeAtomically(AGENT_EVENT_FORWARDING_CONFIG_PATH, winner.raw, 0o600);
    }
    if (!legacy || legacy.raw !== winner.raw) {
      writeAtomically(
        LEGACY_OPENCLAW_EVENT_FORWARDING_CONFIG_PATH,
        winner.raw,
        0o600,
      );
    }
    if (!canonical) {
      consola.info('Migrated event-forwarding config to ~/.aster/event-forwarding.json');
    }
    return AGENT_EVENT_FORWARDING_CONFIG_PATH;
  } catch (error) {
    consola.warn('Could not reconcile event-forwarding config copies; using newest valid copy:', error);
    return winner.path;
  }
}

function readSavedConfig(): AgentEventForwardingConfig | null {
  const path = resolveReadableConfigPath();
  if (!path) return null;
  return JSON.parse(readFileSync(path, 'utf-8')) as AgentEventForwardingConfig;
}

export function loadAgentEventForwardingConfig(): void {
  try {
    config = readSavedConfig();
    if (!config) {
      consola.debug('No event-forwarding config found, event forwarding disabled');
      return;
    }
    if (config.enabled) {
      consola.info(`Agent event forwarding enabled → ${config.endpoint}${config.webhookPath}`);
    }
  } catch (error) {
    consola.warn('Failed to load event-forwarding config:', error);
    config = null;
  }
}

export function isAgentEventForwardingEnabled(): boolean {
  return config?.enabled === true;
}

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

function buildEventText(device: DeviceContext, eventType: string, data: Record<string, unknown>): string {
  const model = [device.manufacturer, device.model].filter(Boolean).join(' ') || 'unknown';
  const modelWithOs = device.osVersion ? `${model}, Android ${device.osVersion}` : model;
  const eventName =
    eventType === 'sms_received' ? 'sms' :
    eventType === 'device_connected' ? 'device_online' :
    eventType === 'device_disconnected' ? 'device_offline' :
    eventType === 'pairing_required' ? 'pairing' :
    eventType;

  const lines = [
    '[skill] aster',
    `[event] ${eventName}`,
    `[device_id] ${device.deviceId}`,
    `[model] ${modelWithOs}`,
  ];

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
    const dataLines = formatData(data);
    if (dataLines) lines.push(dataLines);
  }

  return lines.join('\n');
}

export async function forwardAgentEvent(
  device: DeviceContext,
  eventType: string,
  eventData: Record<string, unknown>,
  timestamp: number,
): Promise<void> {
  if (!config?.enabled) return;

  if (eventType === 'sms_received' && !config.events.sms) return;
  if (eventType === 'notification' && !config.events.notifications) return;
  if (eventType === 'device_connected' && !config.events.deviceConnected) return;
  if (eventType === 'device_disconnected' && !config.events.deviceDisconnected) return;
  if (eventType === 'pairing_required' && !config.events.pairingRequired) return;

  // Retained for wire-contract parity; timestamps are supplied by devices but
  // are not part of the upstream webhook body.
  void timestamp;

  const url = `${config.endpoint}${config.webhookPath}`;
  const payload: Record<string, unknown> = {
    message: buildEventText(device, eventType, eventData),
    wakeMode: 'now',
    deliver: true,
  };
  if (config.channel) payload.channel = config.channel;
  if (config.deliverTo) payload.to = config.deliverTo;

  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (config.token) headers.Authorization = `Bearer ${config.token}`;

  consola.info(`Event-forwarding POST ${url} →`, JSON.stringify(payload));

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
        consola.debug(`Agent event forwarded: ${eventType}`);
        return;
      }
      if (response.status === 401 || response.status === 403) {
        consola.error(`Event-forwarding auth error (${response.status}) for ${eventType}`);
        return;
      }
      consola.warn(`Event-forwarding endpoint returned ${response.status} for ${eventType}`);
    } catch (error) {
      consola.warn(`Event-forwarding request failed (attempt ${attempt + 1}):`, error);
    }

    if (attempt === 0) {
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
  }
}

export type MaskedAgentEventForwardingConfig = Omit<AgentEventForwardingConfig, 'token'> & {
  token: string;
  hasToken: boolean;
};

export function getAgentEventForwardingConfig(): MaskedAgentEventForwardingConfig | null {
  try {
    const savedConfig = readSavedConfig();
    if (!savedConfig) return null;
    return {
      ...savedConfig,
      token: savedConfig.token ? `${savedConfig.token.slice(0, 8)}...` : '',
      hasToken: !!savedConfig.token,
    };
  } catch {
    return null;
  }
}

export function getSavedAgentEventForwardingToken(): string | null {
  try {
    return readSavedConfig()?.token || null;
  } catch {
    return null;
  }
}

export function saveAgentEventForwardingConfig(newConfig: AgentEventForwardingConfig): void {
  const raw = JSON.stringify(newConfig, null, 2);
  // Canonical first. If the process dies before the compatibility write, its
  // newer mtime wins and repairs the legacy copy on the next read.
  writeAtomically(AGENT_EVENT_FORWARDING_CONFIG_PATH, raw, 0o600);
  writeAtomically(
    LEGACY_OPENCLAW_EVENT_FORWARDING_CONFIG_PATH,
    raw,
    0o600,
  );
  config = newConfig;
  consola.info(`Event-forwarding config saved and reloaded (enabled: ${newConfig.enabled})`);
}

export async function testAgentEventForwardingConnection(
  endpoint: string,
  webhookPath: string,
  token: string,
): Promise<{ success: boolean; status?: number; error?: string }> {
  const url = `${endpoint}${webhookPath}`;
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = `Bearer ${token}`;

    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify({ message: '[skill] aster\n[event] test\n[data]\nstatus: connection test from dashboard', wakeMode: 'now', deliver: false }),
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (response.ok) return { success: true, status: response.status };

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
  } catch (error: any) {
    const cause = error.cause;
    if (cause?.code === 'ECONNREFUSED') {
      return { success: false, error: `Connection refused — is OpenClaw running at ${endpoint}?` };
    }
    if (cause?.code === 'ENOTFOUND') {
      return { success: false, error: 'Host not found — check the endpoint URL' };
    }
    if (cause?.code === 'ETIMEDOUT' || error.name === 'AbortError') {
      return { success: false, error: 'Connection timed out after 5s — endpoint may be unreachable' };
    }
    return { success: false, error: error.message || 'Connection failed' };
  }
}

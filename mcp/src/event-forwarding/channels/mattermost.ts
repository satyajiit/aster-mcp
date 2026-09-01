import { consola } from 'consola';
import type { EventChannelConfig, EventChannelTestResult } from './types.js';

function webhookUrl(config: EventChannelConfig): string {
  return `${config.endpoint}${config.webhookPath}`;
}

function optionalChannel(channel: string): string | undefined {
  if (!channel || channel === 'whatsapp' || channel === 'telegram') return undefined;
  return channel;
}

export async function send(
  config: EventChannelConfig,
  text: string,
  eventType: string,
): Promise<void> {
  const url = webhookUrl(config);
  const payload: Record<string, unknown> = { text };
  const channel = optionalChannel(config.channel);
  if (channel) payload.channel = channel;

  const headers: Record<string, string> = { 'Content-Type': 'application/json' };

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

export async function testConnection(
  config: EventChannelConfig,
): Promise<EventChannelTestResult> {
  const url = webhookUrl(config);
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: 'Aster connection test' }),
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

    const statusText = response.status === 401 ? 'Unauthorized — check the webhook URL'
      : response.status === 403 ? 'Forbidden — webhook lacks permission'
      : response.status === 404 ? 'Not found — check the incoming webhook URL'
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
      return { success: false, error: `Connection refused — is Mattermost reachable at ${config.endpoint}?` };
    }
    if (cause?.code === 'ENOTFOUND') {
      return { success: false, error: 'Host not found — check the incoming webhook URL' };
    }
    if (cause?.code === 'ETIMEDOUT' || error.name === 'AbortError') {
      return { success: false, error: 'Connection timed out after 5s — webhook may be unreachable' };
    }
    return { success: false, error: error.message || 'Connection failed' };
  }
}

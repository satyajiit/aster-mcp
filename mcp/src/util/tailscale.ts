import { exec, execSync, spawn } from 'child_process';
import { consola } from 'consola';
import { promisify } from 'util';

const execAsync = promisify(exec);

export interface TailscaleStatus {
  isInstalled: boolean;
  isRunning: boolean;
  hostname?: string;
  ipv4?: string;
  ipv6?: string;
  dnsName?: string;
}

export interface TailscaleServeConfig {
  port: number;
  protocol?: 'http' | 'https' | 'tcp';
}

/**
 * Check if Tailscale CLI is installed
 */
export async function isTailscaleInstalled(): Promise<boolean> {
  try {
    await execAsync('tailscale version');
    return true;
  } catch {
    return false;
  }
}

/**
 * Get Tailscale status
 */
export async function getTailscaleStatus(): Promise<TailscaleStatus> {
  const status: TailscaleStatus = {
    isInstalled: false,
    isRunning: false,
  };

  try {
    // Check if installed
    await execAsync('tailscale version');
    status.isInstalled = true;

    // Get status JSON
    const { stdout } = await execAsync('tailscale status --json');
    const statusJson = JSON.parse(stdout);

    if (statusJson.BackendState === 'Running') {
      status.isRunning = true;

      // Get self info
      if (statusJson.Self) {
        status.hostname = statusJson.Self.HostName;
        status.dnsName = statusJson.Self.DNSName;

        // Get IPs
        const ips = statusJson.Self.TailscaleIPs || [];
        for (const ip of ips) {
          if (ip.includes(':')) {
            status.ipv6 = ip;
          } else {
            status.ipv4 = ip;
          }
        }
      }
    }
  } catch (error) {
    // Tailscale not installed or not running
  }

  return status;
}

/**
 * Get the Tailscale IP address
 */
export async function getTailscaleIP(): Promise<string | null> {
  try {
    const { stdout } = await execAsync('tailscale ip -4');
    return stdout.trim();
  } catch {
    return null;
  }
}

/**
 * Get the Tailscale DNS name (e.g., hostname.tailnet-name.ts.net)
 */
export async function getTailscaleDNSName(): Promise<string | null> {
  try {
    const { stdout } = await execAsync('tailscale status --json');
    const status = JSON.parse(stdout);
    return status.Self?.DNSName?.replace(/\.$/, '') || null;
  } catch {
    return null;
  }
}

/**
 * Start Tailscale serve for a port
 * This makes the port accessible to other devices on your Tailnet
 */
export async function startTailscaleServe(config: TailscaleServeConfig): Promise<boolean> {
  const { port, protocol = 'tcp' } = config;

  try {
    // First, reset any existing serve config for this port
    try {
      await execAsync(`tailscale serve reset`);
    } catch {
      // Ignore errors from reset
    }

    // Build the serve command
    const args = [`--bg`, `${protocol}://${port}`];

    consola.info(`Starting Tailscale serve for port ${port}...`);

    // Run tailscale serve in background mode
    const { stdout, stderr } = await execAsync(`tailscale serve ${args.join(' ')}`);

    if (stderr && !stderr.includes('Available')) {
      consola.warn('Tailscale serve warning:', stderr);
    }

    consola.success(`Tailscale serve started for port ${port}`);
    return true;
  } catch (error: any) {
    consola.error('Failed to start Tailscale serve:', error.message);
    return false;
  }
}

/**
 * Stop Tailscale serve
 */
export async function stopTailscaleServe(): Promise<void> {
  try {
    await execAsync('tailscale serve reset');
    consola.info('Tailscale Serve stopped');
  } catch {
    // Ignore errors
  }
}

/**
 * Start Tailscale Serve for WebSocket port with HTTPS/WSS support
 * Serve exposes the port to devices on your Tailnet with automatic TLS
 */
export async function serveTailscalePort(wsPort: number, dashboardPort?: number): Promise<{
  success: boolean;
  tailscaleIp?: string;
  tailscaleDns?: string;
  wsUrl?: string;
  dashboardUrl?: string;
}> {
  const status = await getTailscaleStatus();

  if (!status.isInstalled) {
    consola.warn('Tailscale is not installed. Skipping Tailscale Serve setup.');
    consola.info('Install Tailscale: https://tailscale.com/download');
    return { success: false };
  }

  if (!status.isRunning) {
    consola.warn('Tailscale is not running. Please start Tailscale first.');
    consola.info('Run: sudo tailscale up');
    return { success: false };
  }

  // Get Tailscale info
  const tailscaleIp = status.ipv4;
  const tailscaleDns = status.dnsName?.replace(/\.$/, '');

  // Reset any existing serve config once before adding rules
  try {
    await execAsync('tailscale serve reset');
  } catch {
    // Ignore reset errors
  }

  // Start Serve for the WebSocket port with HTTPS on 443
  const wsServeSuccess = await startTailscaleServeHttps(wsPort, 443);

  // Start Serve for the dashboard/API port with HTTPS on 8443
  let dashboardServeSuccess = false;
  if (dashboardPort) {
    dashboardServeSuccess = await startTailscaleServeHttps(dashboardPort, 8443);
  }

  if (wsServeSuccess && tailscaleDns) {
    return {
      success: true,
      tailscaleIp,
      tailscaleDns,
      wsUrl: `wss://${tailscaleDns}`,
      dashboardUrl: dashboardServeSuccess && tailscaleDns
        ? `https://${tailscaleDns}:8443`
        : undefined,
    };
  }

  // Fallback to Tailnet-only access via IP (no TLS)
  if (tailscaleIp) {
    consola.info('Tailscale Serve HTTPS setup failed, using plain WebSocket');
    return {
      success: true,
      tailscaleIp,
      tailscaleDns,
      wsUrl: `ws://${tailscaleIp}:${wsPort}`,
    };
  }

  return { success: false };
}

/**
 * Start Tailscale Serve with HTTPS for a local port
 * Serve terminates TLS and forwards to local service (Tailnet-only access)
 */
async function startTailscaleServeHttps(localPort: number, httpsPort: number = 443): Promise<boolean> {
  try {
    consola.info(`Starting Tailscale Serve (HTTPS:${httpsPort}) for port ${localPort}...`);

    // Serve command: expose local port via HTTPS on specified port
    // Format: tailscale serve --bg --https=<httpsPort> http://localhost:<port>
    // This terminates TLS at Tailscale and forwards to local HTTP/WS
    const { stderr } = await execAsync(
      `tailscale serve --bg --https=${httpsPort} http://localhost:${localPort}`
    );

    if (stderr && !stderr.includes('Available') && !stderr.includes('Serve')) {
      consola.warn('Tailscale Serve warning:', stderr);
    }

    consola.success(`Tailscale Serve started - port ${localPort} available via HTTPS:${httpsPort} (Tailnet-only)`);
    return true;
  } catch (error: any) {
    consola.error('Failed to start Tailscale Serve:', error.message);
    return false;
  }
}

/**
 * Display Tailscale connection info
 */
export function displayTailscaleInfo(info: {
  tailscaleIp?: string;
  tailscaleDns?: string;
  wsPort: number;
  wsUrl?: string;
  dashboardUrl?: string;
}): void {
  const { tailscaleIp, tailscaleDns, wsPort, wsUrl, dashboardUrl } = info;

  if (tailscaleIp || tailscaleDns) {
    const displayUrl = wsUrl || (tailscaleDns ? `wss://${tailscaleDns}` : `ws://${tailscaleIp}:${wsPort}`);
    const isSecure = displayUrl.startsWith('wss://');

    consola.box({
      title: 'Tailscale Serve (Tailnet)',
      message: [
        '',
        tailscaleIp ? `  Tailscale IP:   ${tailscaleIp}` : '',
        tailscaleDns ? `  Tailscale DNS:  ${tailscaleDns}` : '',
        '',
        `  WebSocket URL:  ${displayUrl}`,
        dashboardUrl ? `  Dashboard URL:  ${dashboardUrl}` : '',
        '',
        isSecure
          ? '  Secure WebSocket (wss://) via Tailscale Serve'
          : '  Plain WebSocket (ws://) via Tailscale IP',
        dashboardUrl
          ? '  Dashboard accessible via HTTPS on your Tailnet.'
          : '',
        '  Accessible from devices on your Tailnet.',
        '',
      ].filter(Boolean).join('\n'),
    });
  }
}

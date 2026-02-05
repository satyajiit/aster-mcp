import { consola } from 'consola';
import { config } from 'dotenv';
import { networkInterfaces } from 'os';
import { existsSync } from 'fs';
import { resolve } from 'path';
import { spawn, type ChildProcess } from 'child_process';
import { initDatabase, closeDatabase } from './db/index.js';
import { createWebSocketServer } from './websocket/index.js';
import { startApiServer } from './server/index.js';
import { startMcpServer } from './mcp/index.js';
import { ServerConfigSchema, type ServerConfig } from './types/index.js';
import {
  serveTailscalePort,
  displayTailscaleInfo,
  stopTailscaleServe,
} from './util/tailscale.js';

config();

let nuxtProcess: ChildProcess | null = null;

function getLocalIP(): string {
  const nets = networkInterfaces();
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] || []) {
      // Skip internal and non-IPv4 addresses
      if (net.family === 'IPv4' && !net.internal) {
        return net.address;
      }
    }
  }
  return '127.0.0.1';
}

const DASHBOARD_SERVER_PORT = 5989;

/**
 * Start the Nuxt production server for the dashboard
 */
function startDashboardServer(apiPort: number): ChildProcess | null {
  const serverPaths = [
    resolve(process.cwd(), 'dashboard/.output/server/index.mjs'),
    resolve(process.cwd(), '../dashboard/.output/server/index.mjs'),
  ];

  const serverEntry = serverPaths.find(p => existsSync(p));

  if (!serverEntry) {
    consola.warn('Dashboard build not found. Run: pnpm --dir dashboard build');
    return null;
  }

  const child = spawn('node', [serverEntry], {
    env: {
      ...process.env,
      PORT: String(DASHBOARD_SERVER_PORT),
      NITRO_PORT: String(DASHBOARD_SERVER_PORT),
      API_PORT: String(apiPort),
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  child.stdout?.on('data', (data: Buffer) => {
    const msg = data.toString().trim();
    if (msg) consola.info(`[Dashboard] ${msg}`);
  });

  child.stderr?.on('data', (data: Buffer) => {
    const msg = data.toString().trim();
    if (msg) consola.warn(`[Dashboard] ${msg}`);
  });

  child.on('error', (err) => {
    consola.error('Failed to start dashboard server:', err.message);
  });

  child.on('exit', (code) => {
    if (code !== null && code !== 0) {
      consola.warn(`Dashboard server exited with code ${code}`);
    }
  });

  consola.success(`Dashboard server starting on port ${DASHBOARD_SERVER_PORT}`);
  return child;
}

export async function startServer(overrides: Partial<ServerConfig> = {}): Promise<void> {
  const serverConfig = ServerConfigSchema.parse({
    wsPort: parseInt(process.env.WS_PORT || '5987', 10),
    dashboardPort: parseInt(process.env.DASHBOARD_PORT || '5988', 10),
    dbPath: process.env.DB_PATH || './aster.db',
    commandTimeout: parseInt(process.env.COMMAND_TIMEOUT || '30000', 10),
    heartbeatInterval: parseInt(process.env.HEARTBEAT_INTERVAL || '30000', 10),
    heartbeatTimeout: parseInt(process.env.HEARTBEAT_TIMEOUT || '90000', 10),
    ...overrides,
  });

  // Initialize database
  initDatabase(serverConfig.dbPath);

  // Start WebSocket server for Android devices
  createWebSocketServer(serverConfig);

  // Start API server for dashboard
  await startApiServer(serverConfig);

  // Start Nuxt dashboard server (SSR)
  nuxtProcess = startDashboardServer(serverConfig.dashboardPort);

  // Display startup info
  const localIP = getLocalIP();

  consola.box({
    title: 'Aster MCP Server',
    message: [
      '',
      `  WebSocket:   ws://${localIP}:${serverConfig.wsPort}`,
      `  API:         http://${localIP}:${serverConfig.dashboardPort}`,
      nuxtProcess ? `  Dashboard:   http://${localIP}:${DASHBOARD_SERVER_PORT}` : '',
      '',
      `  Database:    ${serverConfig.dbPath}`,
      '',
    ].filter(Boolean).join('\n'),
  });

  // Setup Tailscale if available (serve WebSocket + dashboard)
  const tailscaleResult = await serveTailscalePort(
    serverConfig.wsPort,
    nuxtProcess ? DASHBOARD_SERVER_PORT : undefined,
  );
  if (tailscaleResult.success) {
    displayTailscaleInfo({
      tailscaleIp: tailscaleResult.tailscaleIp,
      tailscaleDns: tailscaleResult.tailscaleDns,
      wsPort: serverConfig.wsPort,
      wsUrl: tailscaleResult.wsUrl,
      dashboardUrl: tailscaleResult.dashboardUrl,
    });
  }

  // Handle shutdown
  const shutdown = async () => {
    consola.info('Shutting down...');
    if (nuxtProcess) {
      nuxtProcess.kill('SIGTERM');
      nuxtProcess = null;
    }
    await stopTailscaleServe();
    closeDatabase();
    process.exit(0);
  };

  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}

// Start MCP server (for Claude integration)
export async function startMcp(): Promise<void> {
  const serverConfig = ServerConfigSchema.parse({
    wsPort: parseInt(process.env.WS_PORT || '5987', 10),
    dashboardPort: parseInt(process.env.DASHBOARD_PORT || '5988', 10),
    dbPath: process.env.DB_PATH || './aster.db',
    commandTimeout: parseInt(process.env.COMMAND_TIMEOUT || '30000', 10),
    heartbeatInterval: parseInt(process.env.HEARTBEAT_INTERVAL || '30000', 10),
    heartbeatTimeout: parseInt(process.env.HEARTBEAT_TIMEOUT || '90000', 10),
  });

  // Initialize database
  initDatabase(serverConfig.dbPath);

  // Start WebSocket server
  createWebSocketServer(serverConfig);

  // Setup Tailscale if available (for MCP mode too)
  const tailscaleResult = await serveTailscalePort(serverConfig.wsPort);
  if (tailscaleResult.success) {
    // Log to stderr since stdout is used for MCP communication
    console.error(`[Tailscale] WebSocket available at: ws://${tailscaleResult.tailscaleIp}:${serverConfig.wsPort}`);
    if (tailscaleResult.tailscaleDns) {
      console.error(`[Tailscale] DNS: ${tailscaleResult.tailscaleDns}`);
    }
  }

  // Start MCP server on stdio
  await startMcpServer();
}

// Export for programmatic use
export { createMcpServer, startMcpServer } from './mcp/index.js';
export { createWebSocketServer } from './websocket/index.js';
export { createApiServer, startApiServer } from './server/index.js';
export * from './db/index.js';
export * from './types/index.js';
export * from './util/tailscale.js';

// Auto-start when run directly
import { fileURLToPath } from 'url';

const currentFile = fileURLToPath(import.meta.url);
const mainScript = resolve(process.argv[1]);

if (currentFile === mainScript) {
  startServer().catch((err) => {
    consola.error('Failed to start server:', err);
    process.exit(1);
  });
}

import { consola } from 'consola';
import { config } from 'dotenv';
import { networkInterfaces } from 'os';
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

  // Display startup info
  const localIP = getLocalIP();

  consola.box({
    title: 'Aster MCP Server',
    message: [
      '',
      `  WebSocket:  ws://${localIP}:${serverConfig.wsPort}`,
      `  API:        http://${localIP}:${serverConfig.dashboardPort}`,
      '',
      `  Database:   ${serverConfig.dbPath}`,
      '',
    ].join('\n'),
  });

  // Setup Tailscale if available
  const tailscaleResult = await serveTailscalePort(serverConfig.wsPort);
  if (tailscaleResult.success) {
    displayTailscaleInfo({
      tailscaleIp: tailscaleResult.tailscaleIp,
      tailscaleDns: tailscaleResult.tailscaleDns,
      wsPort: serverConfig.wsPort,
      wsUrl: tailscaleResult.wsUrl,
    });
  }

  // Handle shutdown
  const shutdown = async () => {
    consola.info('Shutting down...');
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
import { resolve } from 'path';

const currentFile = fileURLToPath(import.meta.url);
const mainScript = resolve(process.argv[1]);

if (currentFile === mainScript) {
  startServer().catch((err) => {
    consola.error('Failed to start server:', err);
    process.exit(1);
  });
}

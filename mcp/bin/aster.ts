#!/usr/bin/env node

import { Command } from 'commander';
import chalk from 'chalk';
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { existsSync, writeFileSync, readFileSync, unlinkSync } from 'fs';
import { homedir } from 'os';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const program = new Command();
const PID_FILE = join(homedir(), '.aster.pid');

function openBrowser(url: string): void {
  const platform = process.platform;
  let cmd: string;
  let args: string[];

  if (platform === 'darwin') {
    cmd = 'open';
    args = [url];
  } else if (platform === 'win32') {
    cmd = 'cmd';
    args = ['/c', 'start', '', url];
  } else {
    cmd = 'xdg-open';
    args = [url];
  }

  const child = spawn(cmd, args, { stdio: 'ignore', detached: true });
  child.unref();
}

program
  .name('aster')
  .description('Aster - Android device control via MCP')
  .version('0.1.0');

// Start command - starts everything (WebSocket, API, MCP)
program
  .command('start')
  .description('Start the Aster server')
  .option('-w, --ws-port <port>', 'WebSocket server port', '5987')
  .option('-a, --api-port <port>', 'API server port', '5988')
  .option('-d, --db <path>', 'Database path', './aster.db')
  .action(async (options) => {
    console.log(chalk.cyan.bold(`
╔═══════════════════════════════════════╗
║              ASTER                    ║
║     Android Device Control Bridge     ║
╚═══════════════════════════════════════╝
`));

    process.env.WS_PORT = options.wsPort;
    process.env.DASHBOARD_PORT = options.apiPort;
    process.env.DB_PATH = options.db;

    // Save PID for stop command
    writeFileSync(PID_FILE, process.pid.toString());

    const { startServer } = await import('../src/index.js');
    await startServer();

    console.log(chalk.green(`
┌─────────────────────────────────────────┐
│ Server running:                         │
│   WebSocket: ws://0.0.0.0:${options.wsPort.padEnd(13)}│
│   API:       http://0.0.0.0:${options.apiPort.padEnd(10)}│
│   Dashboard: http://localhost:${options.apiPort.padEnd(8)}│
└─────────────────────────────────────────┘
`));
    console.log(chalk.gray('Press Ctrl+C to stop\n'));
  });

// Stop command - stops all Aster processes
program
  .command('stop')
  .description('Stop the Aster server')
  .action(() => {
    if (!existsSync(PID_FILE)) {
      console.log(chalk.yellow('No Aster server is running.'));
      return;
    }

    try {
      const pid = readFileSync(PID_FILE, 'utf-8').trim();
      process.kill(parseInt(pid, 10), 'SIGTERM');
      unlinkSync(PID_FILE);
      console.log(chalk.green('✓ Aster server stopped.'));
    } catch (err: any) {
      if (err.code === 'ESRCH') {
        // Process doesn't exist, clean up PID file
        unlinkSync(PID_FILE);
        console.log(chalk.yellow('Server was not running. Cleaned up stale PID file.'));
      } else {
        console.log(chalk.red(`Failed to stop server: ${err.message}`));
      }
    }
  });

// Dashboard command - opens dashboard in browser
program
  .command('dashboard')
  .description('Open the dashboard in browser')
  .option('-p, --port <port>', 'API server port', '5988')
  .action((options) => {
    const url = `http://localhost:${options.port}`;
    console.log(chalk.cyan(`Opening dashboard: ${url}`));
    openBrowser(url);
  });

// MCP command - starts as MCP server (stdio) - hidden from help but available
program
  .command('mcp', { hidden: true })
  .description('Start as MCP server (for Claude integration)')
  .option('-w, --ws-port <port>', 'WebSocket server port', '5987')
  .option('-d, --db <path>', 'Database path', './aster.db')
  .action(async (options) => {
    process.env.WS_PORT = options.wsPort;
    process.env.DB_PATH = options.db;

    const { startMcp } = await import('../src/index.js');
    await startMcp();
  });

// Devices command group
const devicesCmd = program.command('devices').description('Manage devices');

devicesCmd
  .command('list')
  .description('List all registered devices')
  .action(async () => {
    process.env.DB_PATH = process.env.DB_PATH || './aster.db';
    const { initDatabase, getAllDevices } = await import('../src/db/index.js');

    initDatabase(process.env.DB_PATH);
    const devices = getAllDevices();

    if (devices.length === 0) {
      console.log(chalk.yellow('No devices registered.'));
      return;
    }

    console.log(chalk.cyan.bold('\nRegistered Devices:\n'));
    console.log(chalk.gray('─'.repeat(60)));

    for (const device of devices) {
      const statusColor = device.status === 'approved'
        ? chalk.green
        : device.status === 'pending'
          ? chalk.yellow
          : chalk.red;

      console.log(`
  ${chalk.white.bold(device.name)} ${chalk.gray(`(${device.id.slice(0, 8)}...)`)}
  ${chalk.gray('Model:')} ${device.manufacturer} ${device.model}
  ${chalk.gray('OS:')} ${device.platform} ${device.osVersion}
  ${chalk.gray('Status:')} ${statusColor(device.status.toUpperCase())}
  ${chalk.gray('Last Seen:')} ${new Date(device.lastSeen).toLocaleString()}
`);
      console.log(chalk.gray('─'.repeat(60)));
    }
  });

devicesCmd
  .command('approve <deviceId>')
  .description('Approve a pending device')
  .action(async (deviceId: string) => {
    process.env.DB_PATH = process.env.DB_PATH || './aster.db';
    const { initDatabase, updateDeviceStatus, getDevice } = await import('../src/db/index.js');

    initDatabase(process.env.DB_PATH);

    const device = getDevice(deviceId);
    if (!device) {
      console.log(chalk.red(`Device ${deviceId} not found.`));
      process.exit(1);
    }

    updateDeviceStatus(deviceId, 'approved');
    console.log(chalk.green(`✓ Device ${device.name} (${deviceId}) approved.`));
  });

devicesCmd
  .command('reject <deviceId>')
  .description('Reject a device')
  .action(async (deviceId: string) => {
    process.env.DB_PATH = process.env.DB_PATH || './aster.db';
    const { initDatabase, updateDeviceStatus, getDevice } = await import('../src/db/index.js');

    initDatabase(process.env.DB_PATH);

    const device = getDevice(deviceId);
    if (!device) {
      console.log(chalk.red(`Device ${deviceId} not found.`));
      process.exit(1);
    }

    updateDeviceStatus(deviceId, 'rejected');
    console.log(chalk.yellow(`✗ Device ${device.name} (${deviceId}) rejected.`));
  });

devicesCmd
  .command('remove <deviceId>')
  .description('Remove a device from the registry')
  .action(async (deviceId: string) => {
    process.env.DB_PATH = process.env.DB_PATH || './aster.db';
    const { initDatabase, deleteDevice, getDevice } = await import('../src/db/index.js');

    initDatabase(process.env.DB_PATH);

    const device = getDevice(deviceId);
    if (!device) {
      console.log(chalk.red(`Device ${deviceId} not found.`));
      process.exit(1);
    }

    deleteDevice(deviceId);
    console.log(chalk.red(`✗ Device ${device.name} (${deviceId}) removed.`));
  });

program.parse();

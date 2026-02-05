#!/usr/bin/env node

import { Command } from 'commander';
import chalk from 'chalk';
import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { existsSync, writeFileSync, readFileSync, unlinkSync, openSync, mkdirSync } from 'fs';
import { homedir } from 'os';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const program = new Command();
const ASTER_DIR = join(homedir(), '.aster');
const PID_FILE = join(ASTER_DIR, 'aster.pid');
const LOG_FILE = join(ASTER_DIR, 'aster.log');
const STATUS_FILE = join(ASTER_DIR, 'status.json');

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function readStatus(): Record<string, any> | null {
  try {
    if (!existsSync(STATUS_FILE)) return null;
    return JSON.parse(readFileSync(STATUS_FILE, 'utf-8'));
  } catch {
    return null;
  }
}

function displayStatus(status: Record<string, any>, pid?: number): void {
  console.log(chalk.cyan.bold(`
╔═══════════════════════════════════════╗
║              ASTER                    ║
║     Android Device Control Bridge     ║
╚═══════════════════════════════════════╝
`));
  console.log(chalk.green(`  Status:     Running (PID ${pid || status.pid})`));
  console.log(chalk.white(`  WebSocket:  ${status.wsUrl}`));
  console.log(chalk.white(`  API:        ${status.apiUrl}`));
  if (status.dashboardUrl) {
    console.log(chalk.white(`  Dashboard:  ${status.dashboardUrl}`));
  }
  if (status.tailscale) {
    console.log('');
    console.log(chalk.magenta(`  Tailscale:`));
    if (status.tailscale.wsUrl) {
      console.log(chalk.white(`    WSS:        ${status.tailscale.wsUrl}`) + chalk.gray('  (Use in Companion App)'));
    }
    if (status.tailscale.dashboardUrl) {
      console.log(chalk.white(`    Dashboard:  ${status.tailscale.dashboardUrl}`));
    }
    if (status.tailscale.dns) {
      console.log(chalk.gray(`    DNS:        ${status.tailscale.dns}`));
    }
  }
  console.log(chalk.gray(`
  Database:   ${status.dbPath}
  Logs:       ${LOG_FILE}
  Started:    ${new Date(status.startedAt).toLocaleString()}
`));
}

// Ensure ~/.aster directory exists
if (!existsSync(ASTER_DIR)) {
  mkdirSync(ASTER_DIR, { recursive: true });
}

function isProcessRunning(pid: number): boolean {
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

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

// Start command - starts everything (WebSocket, API, Dashboard) as a daemon
program
  .command('start')
  .description('Start the Aster server (background daemon)')
  .option('-w, --ws-port <port>', 'WebSocket server port', '5987')
  .option('-a, --api-port <port>', 'API server port', '5988')
  .option('-d, --db <path>', 'Database path', './aster.db')
  .option('-f, --foreground', 'Run in foreground (don\'t daemonize)')
  .action(async (options) => {
    // Check if already running
    if (existsSync(PID_FILE)) {
      const pid = parseInt(readFileSync(PID_FILE, 'utf-8').trim(), 10);
      if (isProcessRunning(pid)) {
        console.log(chalk.yellow(`Aster is already running (PID ${pid}). Use ${chalk.bold('aster stop')} first.`));
        process.exit(1);
      }
      // Stale PID file, clean up
      unlinkSync(PID_FILE);
    }

    if (options.foreground) {
      // Run in foreground (same as before)
      console.log(chalk.cyan.bold(`
╔═══════════════════════════════════════╗
║              ASTER                    ║
║     Android Device Control Bridge     ║
╚═══════════════════════════════════════╝
`));

      process.env.WS_PORT = options.wsPort;
      process.env.DASHBOARD_PORT = options.apiPort;
      process.env.DB_PATH = options.db;

      writeFileSync(PID_FILE, process.pid.toString());

      const { startServer } = await import('../src/index.js');
      await startServer();

      console.log(chalk.gray('Press Ctrl+C to stop\n'));
      return;
    }

    // Daemonize: spawn a detached child process
    const logFd = openSync(LOG_FILE, 'a');
    const packageRoot = join(__dirname, '..');
    const serverScript = join(packageRoot, 'src', 'index.js');

    const child = spawn('node', [serverScript], {
      env: {
        ...process.env,
        WS_PORT: options.wsPort,
        DASHBOARD_PORT: options.apiPort,
        DB_PATH: options.db,
        ASTER_PID_FILE: PID_FILE,
      },
      stdio: ['ignore', logFd, logFd],
      detached: true,
    });

    child.unref();

    if (child.pid) {
      writeFileSync(PID_FILE, child.pid.toString());

      // Wait for the server to write its status file (includes Tailscale info)
      console.log(chalk.gray('  Starting server...'));
      let status: Record<string, any> | null = null;
      for (let i = 0; i < 15; i++) {
        await sleep(1000);
        status = readStatus();
        if (status) break;
      }

      if (status) {
        displayStatus(status, child.pid);
      } else {
        // Fallback if status file not written in time
        console.log(chalk.cyan.bold(`
╔═══════════════════════════════════════╗
║              ASTER                    ║
║     Android Device Control Bridge     ║
╚═══════════════════════════════════════╝
`));
        console.log(chalk.green(`  Server started (PID ${child.pid})`));
        console.log(chalk.gray(`
  WebSocket:  ws://0.0.0.0:${options.wsPort}
  API:        http://0.0.0.0:${options.apiPort}

  Logs:       ${LOG_FILE}
`));
      }
      console.log(chalk.gray(`  Use ${chalk.white('aster status')} for full status`));
      console.log(chalk.gray(`  Use ${chalk.white('aster logs')} to view logs`));
      console.log(chalk.gray(`  Use ${chalk.white('aster stop')} to stop the server\n`));
    } else {
      console.log(chalk.red('Failed to start Aster server.'));
      process.exit(1);
    }
  });

// Stop command - stops the Aster daemon
program
  .command('stop')
  .description('Stop the Aster server')
  .action(() => {
    if (!existsSync(PID_FILE)) {
      console.log(chalk.yellow('No Aster server is running.'));
      return;
    }

    try {
      const pid = parseInt(readFileSync(PID_FILE, 'utf-8').trim(), 10);

      if (!isProcessRunning(pid)) {
        unlinkSync(PID_FILE);
        console.log(chalk.yellow('Server was not running. Cleaned up stale PID file.'));
        return;
      }

      // Kill the process group (- prefix kills the group, catching child processes)
      try {
        process.kill(-pid, 'SIGTERM');
      } catch {
        // Fallback to killing just the main process
        process.kill(pid, 'SIGTERM');
      }

      unlinkSync(PID_FILE);
      console.log(chalk.green(`✓ Aster server stopped (PID ${pid}).`));
    } catch (err: any) {
      if (err.code === 'ESRCH') {
        unlinkSync(PID_FILE);
        console.log(chalk.yellow('Server was not running. Cleaned up stale PID file.'));
      } else {
        console.log(chalk.red(`Failed to stop server: ${err.message}`));
      }
    }
  });

// Logs command - tail the server log file
program
  .command('logs')
  .description('View server logs')
  .option('-f, --follow', 'Follow log output (like tail -f)')
  .option('-n, --lines <lines>', 'Number of lines to show', '50')
  .action((options) => {
    if (!existsSync(LOG_FILE)) {
      console.log(chalk.yellow('No log file found. Start the server first.'));
      return;
    }

    const tailArgs = ['-n', options.lines];
    if (options.follow) tailArgs.push('-f');
    tailArgs.push(LOG_FILE);

    const tail = spawn('tail', tailArgs, { stdio: 'inherit' });
    tail.on('error', () => {
      console.log(chalk.red('Failed to read logs.'));
    });
  });

// Status command - show full server status
program
  .command('status')
  .description('Show full server status and endpoints')
  .action(() => {
    if (!existsSync(PID_FILE)) {
      console.log(chalk.yellow('Aster is not running.'));
      process.exit(1);
    }

    const pid = parseInt(readFileSync(PID_FILE, 'utf-8').trim(), 10);

    if (!isProcessRunning(pid)) {
      unlinkSync(PID_FILE);
      console.log(chalk.yellow('Aster is not running (stale PID file cleaned up).'));
      process.exit(1);
    }

    const status = readStatus();
    if (status) {
      displayStatus(status, pid);
    } else {
      console.log(chalk.green(`Aster is running (PID ${pid}).`));
      console.log(chalk.gray(`  Logs: ${LOG_FILE}`));
      console.log(chalk.gray('  Status file not found. Restart for full status info.'));
    }
  });

// Dashboard command - opens dashboard in browser
program
  .command('dashboard')
  .description('Open the dashboard in browser')
  .option('-p, --port <port>', 'Dashboard server port', '5989')
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

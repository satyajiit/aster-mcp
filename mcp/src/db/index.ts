import Database from 'better-sqlite3';
import { consola } from 'consola';
import type { Device, DeviceStatus, ExtendedDeviceInfo, LogEntry } from '../types/index.js';

let db: Database.Database | null = null;

export function initDatabase(dbPath: string): Database.Database {
  if (db) return db;

  db = new Database(dbPath);
  db.pragma('journal_mode = WAL');

  // Create tables
  db.exec(`
    CREATE TABLE IF NOT EXISTS devices (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      model TEXT NOT NULL,
      manufacturer TEXT NOT NULL,
      platform TEXT NOT NULL CHECK (platform IN ('android', 'ios')),
      os_version TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
      last_seen INTEGER NOT NULL,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS logs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      device_id TEXT NOT NULL,
      level TEXT NOT NULL CHECK (level IN ('debug', 'info', 'warn', 'error')),
      message TEXT NOT NULL,
      data TEXT,
      timestamp INTEGER NOT NULL,
      FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
    );

    CREATE INDEX IF NOT EXISTS idx_logs_device_id ON logs(device_id);
    CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON logs(timestamp);
    CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status);
  `);

  // Migration: Add extended_info column if it doesn't exist
  try {
    db.exec(`ALTER TABLE devices ADD COLUMN extended_info TEXT`);
    consola.info('Added extended_info column to devices table');
  } catch {
    // Column already exists, ignore
  }

  consola.success('Database initialized');
  return db;
}

export function getDatabase(): Database.Database {
  if (!db) {
    throw new Error('Database not initialized. Call initDatabase() first.');
  }
  return db;
}

export function closeDatabase(): void {
  if (db) {
    db.close();
    db = null;
    consola.info('Database closed');
  }
}

// Device operations
export function upsertDevice(device: Omit<Device, 'createdAt'> & { createdAt?: number }): Device {
  const database = getDatabase();
  const now = Date.now();

  const existing = database.prepare('SELECT * FROM devices WHERE id = ?').get(device.id) as Device | undefined;

  if (existing) {
    database.prepare(`
      UPDATE devices SET
        name = ?, model = ?, manufacturer = ?, platform = ?,
        os_version = ?, last_seen = ?
      WHERE id = ?
    `).run(
      device.name,
      device.model,
      device.manufacturer,
      device.platform,
      device.osVersion,
      device.lastSeen,
      device.id
    );

    return {
      ...existing,
      name: device.name,
      model: device.model,
      manufacturer: device.manufacturer,
      platform: device.platform,
      osVersion: device.osVersion,
      lastSeen: device.lastSeen,
    };
  }

  const newDevice: Device = {
    ...device,
    status: device.status || 'pending',
    createdAt: device.createdAt || now,
  };

  database.prepare(`
    INSERT INTO devices (id, name, model, manufacturer, platform, os_version, status, last_seen, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    newDevice.id,
    newDevice.name,
    newDevice.model,
    newDevice.manufacturer,
    newDevice.platform,
    newDevice.osVersion,
    newDevice.status,
    newDevice.lastSeen,
    newDevice.createdAt
  );

  return newDevice;
}

export function getDevice(id: string): Device | null {
  const database = getDatabase();
  const row = database.prepare('SELECT * FROM devices WHERE id = ?').get(id) as DbDevice | undefined;
  return row ? mapDeviceFromDb(row) : null;
}

export function getAllDevices(): Device[] {
  const database = getDatabase();
  const rows = database.prepare('SELECT * FROM devices ORDER BY last_seen DESC').all() as DbDevice[];
  return rows.map(mapDeviceFromDb);
}

export function updateDeviceStatus(id: string, status: DeviceStatus): boolean {
  const database = getDatabase();
  const result = database.prepare('UPDATE devices SET status = ? WHERE id = ?').run(status, id);
  return result.changes > 0;
}

export function updateDeviceLastSeen(id: string, timestamp: number = Date.now()): boolean {
  const database = getDatabase();
  const result = database.prepare('UPDATE devices SET last_seen = ? WHERE id = ?').run(timestamp, id);
  return result.changes > 0;
}

export function updateDeviceExtendedInfo(id: string, info: ExtendedDeviceInfo): boolean {
  const database = getDatabase();
  const result = database.prepare('UPDATE devices SET extended_info = ? WHERE id = ?').run(
    JSON.stringify(info),
    id
  );
  return result.changes > 0;
}

export function deleteDevice(id: string): boolean {
  const database = getDatabase();
  const result = database.prepare('DELETE FROM devices WHERE id = ?').run(id);
  return result.changes > 0;
}

// Log operations
export function addLog(
  deviceId: string,
  level: LogEntry['level'],
  message: string,
  data?: unknown
): void {
  const database = getDatabase();
  database.prepare(`
    INSERT INTO logs (device_id, level, message, data, timestamp)
    VALUES (?, ?, ?, ?, ?)
  `).run(
    deviceId,
    level,
    message,
    data ? JSON.stringify(data) : null,
    Date.now()
  );
}

export function getDeviceLogs(deviceId: string, limit: number = 100): LogEntry[] {
  const database = getDatabase();
  const rows = database.prepare(`
    SELECT * FROM logs WHERE device_id = ?
    ORDER BY timestamp DESC LIMIT ?
  `).all(deviceId, limit) as DbLogEntry[];

  return rows.map(mapLogFromDb);
}

export function getAllLogs(limit: number = 100): LogEntry[] {
  const database = getDatabase();
  const rows = database.prepare(`
    SELECT * FROM logs ORDER BY timestamp DESC LIMIT ?
  `).all(limit) as DbLogEntry[];

  return rows.map(mapLogFromDb);
}

export interface LogQuery {
  deviceId?: string;
  /** Restrict to these levels. Omitted or empty means all levels. */
  levels?: LogEntry['level'][];
  /** Case-insensitive substring match against message and the data payload. */
  search?: string;
  limit?: number;
  offset?: number;
}

export interface LogPage {
  logs: LogEntry[];
  total: number;
  limit: number;
  offset: number;
}

/**
 * Filtered, paginated log query.
 *
 * The dashboard previously had no way to narrow logs at all — `limit` was the
 * only knob, so finding one error meant scrolling a firehose. Levels, device
 * and free text are all pushed down to SQLite rather than filtered client-side.
 */
export function queryLogs(query: LogQuery = {}): LogPage {
  const database = getDatabase();
  const limit = Math.min(Math.max(query.limit ?? 100, 1), 1000);
  const offset = Math.max(query.offset ?? 0, 0);

  const where: string[] = [];
  const params: unknown[] = [];

  if (query.deviceId) {
    where.push('device_id = ?');
    params.push(query.deviceId);
  }

  if (query.levels && query.levels.length > 0) {
    where.push(`level IN (${query.levels.map(() => '?').join(', ')})`);
    params.push(...query.levels);
  }

  if (query.search) {
    where.push(`(message LIKE ? ESCAPE '@' OR data LIKE ? ESCAPE '@')`);
    // Escape LIKE wildcards so a search for "100%" matches a literal percent
    // sign rather than everything. '@' is the escape char; escape it too.
    const term = `%${query.search.replace(/[@%_]/g, (c) => `@${c}`)}%`;
    params.push(term, term);
  }

  const clause = where.length > 0 ? `WHERE ${where.join(' AND ')}` : '';

  const { total } = database
    .prepare(`SELECT COUNT(*) as total FROM logs ${clause}`)
    .get(...params) as { total: number };

  const rows = database
    .prepare(`SELECT * FROM logs ${clause} ORDER BY timestamp DESC LIMIT ? OFFSET ?`)
    .all(...params, limit, offset) as DbLogEntry[];

  return { logs: rows.map(mapLogFromDb), total, limit, offset };
}

/** Distinct device ids present in the log table, for the log filter dropdown. */
export function getLoggedDeviceIds(): string[] {
  const database = getDatabase();
  const rows = database
    .prepare('SELECT DISTINCT device_id FROM logs ORDER BY device_id')
    .all() as { device_id: string }[];
  return rows.map((r) => r.device_id);
}

// Internal types for DB rows
interface DbDevice {
  id: string;
  name: string;
  model: string;
  manufacturer: string;
  platform: 'android' | 'ios';
  os_version: string;
  status: 'pending' | 'approved' | 'rejected';
  last_seen: number;
  created_at: number;
  extended_info: string | null;
}

interface DbLogEntry {
  id: number;
  device_id: string;
  level: 'debug' | 'info' | 'warn' | 'error';
  message: string;
  data: string | null;
  timestamp: number;
}

function mapDeviceFromDb(row: DbDevice): Device {
  let extendedInfo: ExtendedDeviceInfo | undefined;
  if (row.extended_info) {
    try {
      extendedInfo = JSON.parse(row.extended_info) as ExtendedDeviceInfo;
    } catch {
      // Ignore parse errors
    }
  }

  return {
    id: row.id,
    name: row.name,
    model: row.model,
    manufacturer: row.manufacturer,
    platform: row.platform,
    osVersion: row.os_version,
    status: row.status,
    lastSeen: row.last_seen,
    createdAt: row.created_at,
    extendedInfo,
  };
}

function mapLogFromDb(row: DbLogEntry): LogEntry {
  return {
    id: row.id,
    deviceId: row.device_id,
    level: row.level,
    message: row.message,
    data: row.data || undefined,
    timestamp: row.timestamp,
  };
}

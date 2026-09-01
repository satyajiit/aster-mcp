/**
 * One-command screenshot capture.
 *
 * Boots the fixture API, builds and serves the dashboard against it, captures
 * every page in dark and light, then tears everything down. Nothing here talks
 * to a real device or a real server, so the output is reproducible.
 *
 * Reproducibility, measured: 23 of the 24 images are byte-identical on every
 * run. `mcp-tool-explorer.png` occasionally differs at the byte level; the one
 * time two differing copies were compared directly the pixels were identical,
 * so it looks like PNG encoding rather than the page. Compare pixels, not
 * hashes, if you are checking this — a hash diff on that one file alone is not
 * evidence the UI changed.
 *
 *   node run.mjs            build + capture
 *   node run.mjs --no-build reuse an existing .output (faster iteration)
 */
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const DASHBOARD = join(here, '../../mcp/dashboard');
const FIXTURE_PORT = 5998;
const DASHBOARD_PORT = 5989;

const children = [];

function run(cmd, args, opts = {}) {
  const child = spawn(cmd, args, { stdio: 'inherit', ...opts });
  children.push(child);
  return child;
}

function shutdown() {
  for (const c of children) {
    try { c.kill('SIGTERM'); } catch { /* already gone */ }
  }
}
process.on('exit', shutdown);
process.on('SIGINT', () => { shutdown(); process.exit(1); });

async function waitFor(url, timeoutMs = 60_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const res = await fetch(url);
      if (res.ok) return;
    } catch { /* not up yet */ }
    await new Promise((r) => setTimeout(r, 400));
  }
  throw new Error(`Timed out waiting for ${url}`);
}

function once(cmd, args, opts = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { stdio: 'inherit', ...opts });
    child.on('exit', (code) => (code === 0 ? resolve() : reject(new Error(`${cmd} exited ${code}`))));
  });
}

// 1. Fixture API
console.log('→ starting fixture API');
run(process.execPath, [join(here, 'fixture-server.mjs')], {
  env: { ...process.env, FIXTURE_PORT: String(FIXTURE_PORT) },
});
await waitFor(`http://localhost:${FIXTURE_PORT}/api/health`, 15_000);

// 2. Build the dashboard so its /api proxy points at the fixture API.
//    routeRules are baked at build time, hence the rebuild.
if (!process.argv.includes('--no-build')) {
  console.log('→ building dashboard against the fixture API');
  await once('pnpm', ['build'], {
    cwd: DASHBOARD,
    env: { ...process.env, API_PORT: String(FIXTURE_PORT) },
  });
}

// 3. Serve it
console.log('→ serving dashboard');
run(process.execPath, [join(DASHBOARD, '.output/server/index.mjs')], {
  env: {
    ...process.env,
    PORT: String(DASHBOARD_PORT),
    NITRO_PORT: String(DASHBOARD_PORT),
    API_PORT: String(FIXTURE_PORT),
  },
});
await waitFor(`http://localhost:${DASHBOARD_PORT}/`, 30_000);

// 4. Capture
console.log('→ capturing');
await once(process.execPath, [join(here, 'capture.mjs')], {
  env: { ...process.env, DASHBOARD_URL: `http://localhost:${DASHBOARD_PORT}` },
});

// 5. Sync into the docs site
await once(process.execPath, [join(here, 'sync.mjs')]);

shutdown();
process.exit(0);

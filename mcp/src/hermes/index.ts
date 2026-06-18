import { consola } from 'consola';
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { homedir } from 'os';
import { join } from 'path';

/**
 * Hermes Agent integration for Aster.
 *
 * Hermes Agent (by NousResearch) is a self-improving AI agent that supports
 * MCP servers via its config.yaml. This module helps configure Aster as an
 * MCP server within Hermes so Hermes can control an Android device.
 *
 * @see https://github.com/NousResearch/hermes-agent
 * @see https://hermes-agent.nousresearch.com/docs/user-guide/features/mcp
 */

const HERMES_CONFIG_PATHS = [
  join(homedir(), '.hermes', 'config.yaml'),
  join(homedir(), '.hermes', 'config.yml'),
  join(process.cwd(), 'hermes.config.yaml'),
  join(process.cwd(), 'hermes.config.yml'),
];

const ASTER_MCP_SERVER_HOST = process.env.ASTER_MCP_HOST || 'localhost';
const ASTER_MCP_SERVER_PORT = parseInt(process.env.DASHBOARD_PORT || '5988', 10);

export const HERMES_MCP_CONFIG_SNIPPET = `
# ─── Aster — Your AI CoPilot on Mobile ───────────────────────────────────
# Add this to your Hermes config.yaml under mcp_servers:
#
# mcp_servers:
#   aster:
#     type: "http"
#     url: "http://${ASTER_MCP_SERVER_HOST}:${ASTER_MCP_SERVER_PORT}/mcp"
#     tools: "*"
#
# See: https://github.com/satyajiit/aster-mcp
`;

export interface HermesConfigResult {
  found: boolean;
  path: string | null;
  action: 'found' | 'created' | 'not_found';
  snippet: string;
}

/**
 * Find the Hermes config file on disk.
 */
export function findHermesConfig(): string | null {
  for (const p of HERMES_CONFIG_PATHS) {
    if (existsSync(p)) return p;
  }
  return null;
}

/**
 * Generate the MCP server config snippet for Hermes Agent.
 */
export function generateHermesConfigSnippet(host?: string, port?: number): string {
  const h = host || ASTER_MCP_SERVER_HOST;
  const p = port || ASTER_MCP_SERVER_PORT;

  return `
# ─── Aster — Your AI CoPilot on Mobile ───────────────────────────────────
# Give Hermes control over an Android device via MCP
mcp_servers:
  aster:
    type: "http"
    url: "http://${h}:${p}/mcp"
    tools: "*"   # Enable all Aster tools (60+)
`;
}

/**
 * Attempt to configure Hermes for Aster.
 * If a Hermes config exists, prints the snippet to add.
 * If not, offers to create one.
 */
export function configureHermes(options?: { host?: string; port?: number; write?: boolean }): HermesConfigResult {
  const host = options?.host || ASTER_MCP_SERVER_HOST;
  const port = options?.port || ASTER_MCP_SERVER_PORT;
  const snippet = generateHermesConfigSnippet(host, port);
  const existingPath = findHermesConfig();

  if (existingPath) {
    const content = readFileSync(existingPath, 'utf-8');

    if (content.includes('aster:') || content.includes('aster-mcp')) {
      consola.info(`Aster MCP config already found in ${existingPath}`);
      return { found: true, path: existingPath, action: 'found', snippet };
    }

    if (options?.write) {
      writeFileSync(existingPath, content.trimEnd() + '\n' + snippet, 'utf-8');
      consola.success(`Appended Aster MCP config to ${existingPath}`);
      return { found: true, path: existingPath, action: 'found', snippet };
    }

    consola.info(`Hermes config found at: ${existingPath}`);
    consola.info('Add the following to your mcp_servers section:');
    console.log(snippet);
    return { found: true, path: existingPath, action: 'found', snippet };
  }

  // No existing Hermes config — print instructions
  consola.info('No Hermes config.yaml found in standard locations.');
  consola.info('Create ~/.hermes/config.yaml with:');
  console.log(snippet);
  return { found: false, path: null, action: 'not_found', snippet };
}

/**
 * CLI handler for 'aster configure-hermes'
 */
export async function runConfigureHermes(): Promise<void> {
  consola.box({
    title: 'Aster ↔ Hermes Agent',
    message: 'Configure Aster as an MCP server for Hermes Agent',
  });

  const result = configureHermes({ write: false });

  if (result.action === 'found') {
    consola.success(`\n✅ Hermes config located at: ${result.path}`);
    consola.info('After adding the snippet, restart Hermes and run:');
    consola.info('  hermes mcp list   # verify aster tools appear');
  } else {
    consola.info('\n📝 Save the config above, then run:');
    consola.info('  hermes chat       # start chatting — Aster tools are now available');
  }

  consola.info('\n💡 Example Hermes prompts with Aster:');
  consola.info('  • "Take a screenshot of my phone and describe what you see"');
  consola.info('  • "Read my latest SMS messages"');
  consola.info('  • "Open YouTube and search for cooking videos"');
  consola.info('  • "Find photos from last week"');
  consola.info('  • "Get my phone\'s battery status and WiFi info"');
  consola.info('  • "Add a calendar event: dentist tomorrow at 10am"');
}

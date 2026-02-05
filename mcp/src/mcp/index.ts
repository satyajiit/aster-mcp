import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { consola } from 'consola';
import { TOOL_DEFINITIONS } from './tools.js';
import { handleToolCall } from './handler.js';

export function createMcpServer(): Server {
  const server = new Server(
    {
      name: 'aster',
      version: '0.1.0',
    },
    {
      capabilities: {
        tools: {},
      },
    }
  );

  // List available tools
  server.setRequestHandler(ListToolsRequestSchema, async () => {
    return {
      tools: TOOL_DEFINITIONS,
    };
  });

  // Handle tool calls
  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    consola.debug(`Tool call: ${name}`, args);

    const result = await handleToolCall(name, args || {});

    return {
      content: result.content,
      isError: result.isError,
    };
  });

  return server;
}

export async function startMcpServer(): Promise<void> {
  const server = createMcpServer();
  const transport = new StdioServerTransport();

  await server.connect(transport);
  consola.success('MCP server started on stdio');
}

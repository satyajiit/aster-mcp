import type { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import type { JSONRPCRequest, JSONRPCResponse } from '@modelcontextprotocol/sdk/types.js';
import { consola } from 'consola';
import { TOOL_DEFINITIONS } from './tools.js';
import { handleToolCall } from './handler.js';

export function registerMcpHttpRoutes(app: FastifyInstance): void {
  // MCP HTTP endpoint - implements MCP protocol over HTTP
  app.post('/mcp', async (request: FastifyRequest, reply: FastifyReply) => {
    try {
      const jsonrpcRequest = request.body as JSONRPCRequest;

      consola.debug('MCP HTTP request:', {
        method: jsonrpcRequest.method,
        id: jsonrpcRequest.id,
      });

      let response: JSONRPCResponse;

      switch (jsonrpcRequest.method) {
        case 'initialize':
          // Handle MCP initialize handshake
          response = {
            jsonrpc: '2.0',
            id: jsonrpcRequest.id,
            result: {
              protocolVersion: '2024-11-05',
              capabilities: {
                tools: {},
              },
              serverInfo: {
                name: 'aster',
                version: '0.1.0',
              },
            },
          };
          break;

        case 'tools/list':
          // Return list of available tools
          response = {
            jsonrpc: '2.0',
            id: jsonrpcRequest.id,
            result: {
              tools: TOOL_DEFINITIONS,
            },
          };
          break;

        case 'tools/call': {
          // Handle tool execution
          const params = jsonrpcRequest.params as {
            name: string;
            arguments?: Record<string, unknown>;
          };

          const result = await handleToolCall(params.name, params.arguments || {});

          response = {
            jsonrpc: '2.0',
            id: jsonrpcRequest.id,
            result: {
              content: result.content,
              isError: result.isError,
            },
          };
          break;
        }

        case 'ping':
          // Handle ping request
          response = {
            jsonrpc: '2.0',
            id: jsonrpcRequest.id,
            result: {},
          };
          break;

        default:
          // Method not found
          response = {
            jsonrpc: '2.0',
            id: jsonrpcRequest.id,
            error: {
              code: -32601,
              message: `Method not found: ${jsonrpcRequest.method}`,
            },
          };
      }

      reply.header('Content-Type', 'application/json');
      return response;
    } catch (error) {
      consola.error('MCP HTTP error:', error);

      const jsonrpcRequest = request.body as JSONRPCRequest;
      const response: JSONRPCResponse = {
        jsonrpc: '2.0',
        id: jsonrpcRequest?.id,
        error: {
          code: -32603,
          message: error instanceof Error ? error.message : 'Internal error',
          data: error instanceof Error ? error.stack : undefined,
        },
      };

      reply.status(500);
      return response;
    }
  });

  consola.success('MCP HTTP endpoints registered at POST /mcp');
}

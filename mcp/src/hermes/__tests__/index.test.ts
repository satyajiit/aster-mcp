import { describe, it, expect } from 'vitest';
import { generateHermesConfigSnippet } from '../index.js';

describe('Hermes config generation', () => {
  it('generates a valid snippet with default host/port', () => {
    const snippet = generateHermesConfigSnippet();
    expect(snippet).toContain('aster:');
    expect(snippet).toContain('type: "http"');
    expect(snippet).toContain('/mcp');
  });

  it('uses custom host and port', () => {
    const snippet = generateHermesConfigSnippet('192.168.1.100', 9999);
    expect(snippet).toContain('192.168.1.100');
    expect(snippet).toContain('9999');
  });

  it('contains Hermes-specific fields', () => {
    const snippet = generateHermesConfigSnippet();
    expect(snippet).toContain('mcp_servers:');
    expect(snippet).toContain('tools:');
  });
});

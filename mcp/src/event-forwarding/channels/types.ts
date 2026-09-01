export type EventChannelType = 'openclaw' | 'mattermost';

export interface EventChannelConfig {
  endpoint: string;
  webhookPath: string;
  token: string;
  channel: string;
  deliverTo: string;
}

export interface EventChannelTestResult {
  success: boolean;
  status?: number;
  error?: string;
}

export interface EventChannel {
  send(config: EventChannelConfig, text: string, eventType: string): Promise<void>;
  testConnection(config: EventChannelConfig): Promise<EventChannelTestResult>;
}

import * as mattermost from './mattermost.js';
import * as openclaw from './openclaw.js';
import type { EventChannel } from './types.js';

export type {
  EventChannel,
  EventChannelConfig,
  EventChannelTestResult,
  EventChannelType,
} from './types.js';

export function getEventChannel(channelType?: string | null): EventChannel {
  return channelType === 'mattermost' ? mattermost : openclaw;
}

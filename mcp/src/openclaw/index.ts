/**
 * @deprecated Import from `../event-forwarding/index.js` instead.
 *
 * This module is a zero-logic compatibility tombstone. Every legacy symbol is
 * the exact same runtime function as its canonical event-forwarding symbol.
 */
export type {
  AgentEventForwardingConfig as OpenClawConfig,
  DeviceContext,
  MaskedAgentEventForwardingConfig,
} from '../event-forwarding/index.js';

export {
  forwardAgentEvent as forwardEventToOpenClaw,
  getAgentEventForwardingConfig as getOpenClawConfig,
  getLegacyOpenClawSourceToken as getOpenClawSourceToken,
  getSavedAgentEventForwardingToken as getOpenClawSavedToken,
  isAgentEventForwardingEnabled as isOpenClawEnabled,
  loadAgentEventForwardingConfig as loadOpenClawConfig,
  saveAgentEventForwardingConfig as saveOpenClawConfig,
  testAgentEventForwardingConnection as testOpenClawConnection,
} from '../event-forwarding/index.js';

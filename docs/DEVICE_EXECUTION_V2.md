# Original device execution protocol

This is a source implementation description. No build, test suite or device journey was run for the owner UI follow-up.

OpenAlly uses the additive version 2 protocol over the actual Android Binder lane. Before submitting, the host durably records its original request and the chosen companion identity. A companion capability response is required; an unsupported action is refused before submission. The existing outbound companion WebSocket connection is not an OpenAlly command-serving relay. This change does not add that missing remote endpoint.

The request binds the exact UTF-8 `payload_json` and SHA-256 `payload_hash`, protocol version, request ID, workspace ID, optional requested device ID and creation time. Its target binds device ID, adapter incarnation, persistent companion ID and original boot ID. The journal retains that original identity when later transports read its status. Status never runs the command. `sealIfUnstarted` atomically closes a missing or prepared original without dispatch and prevents a delayed submit; a running or uncertain original cannot be sealed into invented completion.

The 17 supported actions are:

| Actions | What a closed execution establishes |
| --- | --- |
| `tap`, `set_text`, `long_press`, `set_toggle`, `perform`, `scroll`, `input_gesture`, `press_key`, `global_action`, `input_text`, `click_by_text`, `click_by_view_id` | The handler and its registered Android gesture children have ended. Inspect the saved result separately; this does not verify a purchase, message delivery or other app outcome. |
| `launch_intent` | The audited package-launch command ended. Arbitrary intent action/data/extras are excluded; downstream app work is not verified. |
| `screen_prompt`, `screen_approve` | The original choice/cancel/timeout result is saved only after the prompt UI closes. Overlay detachment and fallback Activity decor detachment are execution children. Configuration recreation and a delayed Activity launch retain the original child. |
| `screen_signin_wait`, `screen_handoff` | Control-overlay teardown and banner presentation are awaited. The result reports `shown` and always `owner_task_completed:false`. The banner may remain visible until dismissed; sign-in, registration and payment completion are not inferred. |

The first submit awaits an independently owned worker. Losing the Binder or WebSocket waiter does not cancel that worker, detach its gesture/UI children, or release admission. Duplicate submits observe the original row. Terminal proof is persisted after child closure. All legacy control entrances share the same physical admission fence.

The companion keeps one unresolved control execution at a time. Payload and retained result bodies are individually bounded to 256 KiB, with a 64 MiB encrypted result-body budget. Terminal identity/state and result hashes are retained when an encrypted body is compacted; `result_available:false` does not authorize replay. A failure to encrypt an already closed result may also retain hash-only closure. Private result retrieval and hash-only proof remain bound to the original authenticated principal.

After process loss, an unfinished original stays uncertain. A new boot, missing status, transport loss or principal denial is never completion proof. Recovery through a replacement adapter requires the same companion and authenticated principal; switching between IPC and WebSocket principals is not supported. Anonymous legacy executions left unfinished by older callers remain a separate recovery limitation. Moving these four owner UI commands to version 2 prevents newly supported OpenAlly prompt/banner calls from creating such anonymous records; it does not clear existing unknown ones.

Implementation: `ExecutionProtocol`, `ExecutionJournal`, `ExecutionCoordinator` and `ExecutionChildren` under `apps/android/app/src/main/java/com/aster/service/execution/`; the owner UI handlers/controllers and `InteractivePromptActivity` provide their actual UI closure callbacks.

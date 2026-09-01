# Backlog

Work deliberately left out of the dashboard revamp (September 2026), recorded here so it is not rediscovered from scratch. Each item says how it was found, so the finding can be re-derived rather than trusted.

---

## 1. 21 device actions are implemented on the phone but unreachable from the server

The Android app implements **69** command actions across 24 `CommandHandler`s. The MCP server dispatches **48** of them. The remaining **21** have no MCP tool and no REST route, so neither an AI client nor the dashboard can invoke them.

```
companion_overlay_hide      files.list            screen_approve
companion_overlay_recompute files.read            screen_capability
companion_overlay_show      get_now_playing       screen_handoff
companion_overlay_status    hide_all_overlays     screen_prompt
count_sms                   hide_overlay          screen_set_policy
dismiss_all_notifications   list_overlays         screen_signin_wait
dismiss_notification        observe               scroll
```

**How to re-derive:** the phone's surface is every string literal returned by `supportedActions()` under `apps/android/app/src/main/java/com/aster/service/handlers/`; the server's surface is every action passed to `sendCommand(deviceId, '<action>', …)` under `mcp/src/`. The difference is the list above.

**Worth doing first:** `screen_capability` — "is the accessibility service actually granted right now?" is exactly the health signal the dashboard has no way to show, and today it guesses from whether a command failed.

Exposing these is a server feature project (tool schema, handler case, docs) rather than a UI change, which is why the revamp stopped at the existing 49 tools.

---

## 2. The API has no authentication and binds `0.0.0.0`

Every `/api/*` route is unauthenticated, and the server binds all interfaces. `POST /api/event-forwarding/prefill-token` returns the **full plaintext** agent gateway token ([mcp/src/server/index.ts](mcp/src/server/index.ts)).

This predates the revamp, but the revamp raises the stakes: the dashboard now drives SMS, contacts, location and shell through that same open surface.

**Minimum viable fix:** default the bind to `127.0.0.1` with an explicit `--host 0.0.0.0` opt-in, so LAN exposure becomes a choice rather than the default. A shared token in `~/.aster/` checked by a Fastify `preHandler` would be the next step up.

Not done here because it changes how every existing install reaches the dashboard, and that deserves its own release note.

---

## 3. Android screenshots are still captured by hand

`tools/screenshots/` automates the **dashboard** only — a fixture API, a frozen clock and Playwright at 1440×900@2x, dark and light. 23 of the 24 images are byte-identical on every run; `mcp-tool-explorer.png` occasionally differs at the byte level while comparing pixel-identical, so verify that one by pixels rather than by hash.

The Android strip in the README is still real-device capture at 1080×2340. Automating it means Roborazzi (or Paparazzi) plus a screenshot-test source set and golden images in-repo — a real chunk of Gradle work, and the app's most photogenic screens (companion overlay, live device dashboard) depend on runtime state that a screenshot test would have to fake convincingly.

---

## 4. Foreground-service notification icons are launcher icons

All three `setSmallIcon` call sites pass `R.mipmap.ic_launcher`:

- `service/AsterService.kt:481`
- `service/AsterNotificationListenerService.kt:239`
- `service/safety/KillSwitchController.kt:107`

Android expects a monochrome silhouette there and applies its own tint; a full-colour launcher icon renders as a white blob on many devices.

Fix is a dedicated `res/drawable/ic_notification.xml` (single-colour, transparent background) referenced from all three call sites.

---

## 5. Shell tool loses output on a non-zero exit, and its timeout is unreachable

`aster_execute_shell` returns `{command, exitCode, output, success, truncated}` and the panel now
unwraps it correctly. Two things remain on the server side:

- When a command exits non-zero the transport surfaces `Error: Command exited with code N` and
  **drops the captured stdout+stderr**, so the text the shell actually printed never reaches the
  dashboard. The panel falls back to the error string, which is all it has.
- The device accepts a `timeout` param, but the transport's own 30 s timer is identical and always
  fires first, so a long-running command can neither be extended nor report the device's own
  timeout.

Both are fixes in `mcp/src/` (command dispatch and result mapping), not in the UI.

---

## 6. Small `mcp/package.json` inaccuracies

- `"lint": "eslint src"` cannot run: `eslint` is not in `devDependencies` and there is no config file in `mcp/`. The script has apparently never worked.
- `@fastify/static` is a declared dependency that nothing under `mcp/src/` imports.

Left alone because adding a linter mid-revamp would bury the diff under formatting churn. Add ESLint deliberately, as its own change, with the config the repo actually wants.

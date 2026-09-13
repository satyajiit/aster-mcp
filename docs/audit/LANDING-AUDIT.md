# Aster landing page — GEO, content, accessibility and architecture audit

Target: `docs/` (Nuxt 4 static site, deployed to GitHub Pages at `aster.matterwardlabs.com`).

Method: eight independent audit lanes, each adversarially verified by a second agent instructed to refute;
three independent route-map designs judged into one. 20 agents, 0 errors. Findings below are the ones that survived refutation.

**132 verified findings** — 3 critical, 27 high, 44 medium, 58 low.

> **Read the last section first if you want the shipped state.** Everything from here to
> "Route map (adopted)" is the round-1 record — findings as reported and refuted, *before* any code
> was written — and it is kept verbatim. Two of its assertions were later disproved by the
> implementation and are corrected in
> [Round 2 and 3](#round-2-and-3--implementation-corrections-and-the-verified-artifact): the
> on-device action count is **77**, not 67, and the per-mode split is 49 / 77 / 48 shared / 29
> device-only / 1 server-only. Corrected findings carry an inline note.

## Lane summaries

### content-facts

*19 findings — 1 critical, 3 high, 9 medium, 6 low*

Received 20 findings; refuted 2, kept 18, and corrected severity or anchoring on 8 of the survivors. REFUTED: (1) `openally-not-49-tools-over-ipc` — the finding measured the wrong surface. `IpcMode.kt:363` returns `commandHandlers.keys`, which `ModeModule.kt:115-120` builds from every handler's `supportedActions()`; I collected that set (77 actions) and it contains all but one of the 49 server tools' device actions — including make_call_with_voice, record_video, search_media, speak_tts, vibrate, read_notifications, index_media_metadata, find_element and get_screen_hierarchy, every one of which the finding claimed was absent. The only gap is `list_devices` (a server-side registry listing, meaningless on-device), and `aster_click_by_id` maps to the device action `click_by_view_id` at `mcp/src/mcp/handler.ts:523`. The 44-schema `McpToolRegistry.kt` the finding cited belongs to the Ktor on-device MCP mode, not the Binder IPC path the sentence at IntegrationsSection.vue:42 describes. (2) `under-claimed-on-device-mode` — an editorial "say more" request, not a defect: FeaturesGrid.vue:115-116 accurately names all three modes, ScreenshotsSection.vue:118 ships the screenshot, and nothing on the page states or implies the npm server is mandatory; no false statement exists to fix. Severity corrections: `skill-install-url-wrong-repo` critical→high (a broken documented link, not a safety or legal claim); `device-approval-gates-commands-not-connection`, `tailscale-card-hides-plaintext-default` and `llms-txt-android-version-wrong` high→medium; `tool-chips-that-are-not-tools`, `samples-omit-required-deviceid`, `client-roster-inconsistent` and `network-claim-vs-whatsapp-scenarios` medium→low — the last because I confirmed Aster's webhook POSTs to the operator's own agent endpoint and the WhatsApp hop is the agent's, so "no third-party relay" is true of the server itself. One anchoring correction: `app-usage-duration-overclaim` cited UseCasesSection.vue:285 (the baby-monitor card); the copy is at EmbraceSection.vue:285-286 and the finding survives at that path. Held at critical: the Owner-Approved Folders claim, the single verified false security guarantee — FileSystemHandler.kt:198-207 passes any absolute path through under MANAGE_EXTERNAL_STORAGE (AndroidManifest.xml:33) while SecuritySection.vue:202 and FeaturesGrid.vue:110 promise the AI reads only approved folders. The original audit carried no numeric score or overall verdict to preserve; its ToolsShowcase inventory claim did survive independent checking — the 8 categories hold exactly the 49 names in mcp/src/mcp/tools.ts, with zero invented and zero missing, and the only defect there is the stripped aster_ prefix.

### geo-citability

*19 findings — 5 high, 6 medium, 8 low*

Received 22 findings; refuted 3, kept 19 with 11 severity corrections. No overall score or verdict accompanied the original findings list, so there is none to preserve. REFUTED: (1) `no-standalone-definition-of-aster` — its evidence says the page "never names the npm package in prose", but SetupSteps.vue:50 renders the prose sentence "Install the aster-mcp package from npm globally and start the server…" and IntegrationsSection.vue:98-99 renders "Also available on npm: npm install -g aster-mcp"; HeroSection.vue:44 is itself a definitional sentence ("Aster connects Android to AI assistants … via Model Context Protocol") and SetupSteps steps 1+2 convey the two-piece architecture, so the claim rests on a false premise about the file. (2) `aster-not-disambiguated` — same false premise: its evidence enumerates that `aster-mcp` appears "ONLY inside URLs, a code block and a footer link label", which SetupSteps.vue:50 and IntegrationsSection.vue:98 contradict; the JSON-LD also already carries `downloadUrl: 'https://www.npmjs.com/package/aster-mcp'` (nuxt.config.ts:70). (3) `no-comparison-with-alternatives` — the absence of a Tasker/ADB/scrcpy comparison is a content-strategy preference, not a page defect; the rationale ("a competitor's comparison page wins the citation") is unverifiable speculation, and the structural half of the point is already carried by `zero-extraction-structure`. Severity corrections: both "critical" findings downgraded to high (the page still renders 14 other sections and the prefix issue is a precision defect, not a broken page); `observe-fade-ships-opacity-zero` downgraded to medium and its rationale rewritten — Readability-class extractors key on `display:none`/`visibility:hidden`/the `hidden` attribute, not computed opacity, so "recognised discount signal" is folklore; the real, verifiable consequence is that both sections render blank with JS off. `fabricated-tool-names`, `fictional-demo-numbers`, `carousel-variants` moved to medium; `dashboard-screenshot-tab`, `no-price`, `platform-scope`, `local-only-mode`, `h1-contains-no-product-name`, `ownership-stated-two-ways` moved to low. Everything kept below was read in the file at the line cited; the 49 `aster_*` definitions in mcp/src/mcp/tools.ts were counted independently (grep -c "name: 'aster_" → 49) and the three UseCases pill strings were confirmed absent from the entire repo outside docs/app.

### tech-seo

*15 findings — 1 high, 2 medium, 12 low*

Received 22 findings; REFUTED 7, kept 15 (several with severity corrected downward). The original audit contained no overall score or verdict line, so there is none to preserve. Refuted: (1) `global-canonical-hardcoded-to-root` — the alarm rests on a misreading of Nuxt/unhead defaults. `app.head` is Nuxt's documented site-wide-default mechanism, and docs/node_modules/.pnpm/unhead@2.1.2/.../dist/shared/unhead.CYpwL2hc.mjs:20-21 `if (name === "link" && props.rel === "canonical") return "canonical"` proves a route-level `useHead` canonical dedupes against and replaces the global one; title is in `UniqueTags` and `description`/`robots`/`keywords` plus every `og:*` (structured, contains ":") dedupe the same way (lines 3-33 of that file). So the claim "every new page will ship rel=canonical pointing at the homepage" is false — a page that sets its own wins. The only present-tense harm is the two prerendered shells, which is already finding `indexable-empty-200-html-shell`. (9) `twitter-card-missing-site-creator` — self-nullifying: its own fix_direction is "leave this alone", and no X handle exists in the tree. Not a defect. (11) `sitemap-priority-changefreq-inert` — the evidence is correct (docs/public/sitemap.xml:6-7 do carry `weekly`/`1.0`) but the finding itself concludes "No harm"; inert markup is not a defect. (12) `root-url-trailing-slash-consistent-not-a-defect` — explicitly filed as a verified NON-defect with fix_direction "No change needed today". (13) `explicit-ai-crawler-blocks-are-cargo-cult` — also a non-defect; its conclusion is "the file is correct as written, leave it alone". Its substantive recommendation (Disallow the shells) is already carried by the surviving shell finding. (18) `no-itemlist-for-49-tools` — its own evidence says the tool catalogue is fully server-rendered with "0 missing", and its fix is "populate featureList", which is a duplicate of `jsonld-missing-recommended-properties`. (19) `faqpage-howto-would-not-earn-a-google-rich-result` — explicitly a non-recommendation ("Skip FAQPage/HowTo/BreadcrumbList"). Severity corrections: I checked the aster-ui coupling claim and found `grep -rnoE "<A[A-Z][A-Za-z]+" docs/app` returns ZERO hits — docs/app uses no A-prefixed aster-ui component at all, so the CI paths-filter gap cannot cause crawlable-HTML drift; the only real cross-boundary coupling is the tokens.css `@import` in docs/app/assets/css/main.css, which is CSS-only. That finding survives at `low`, not `high`. Two hard confirmations worth calling out: the skill URL genuinely 404s over the network (`satyajiit/Aster` → HTTP 404, `satyajiit/aster-mcp` → HTTP 200), and the github-pages/static preset divergence is provable from the vendored preset source. Note on evidence provenance: docs/.output/ already existed on disk from the prior agent's run and docs/.output/nitro.json records `"preset": "github-pages"`, i.e. the same preset CI uses, so the generated 404.html/200.html/index.html I read are representative of the deployed artifact.

### llm-readability

*9 findings — 3 high, 1 medium, 5 low*

Received 15 findings; refuted 6, kept 9 (three severities corrected down from critical to high, three from high/medium to medium/low). No overall score or verdict was present in the material handed to me, so none is preserved. REFUTED: (1) no-llms-full-txt — llms-full.txt is a convention, not a spec requirement, and no failure mode was demonstrated; the canonical 792-line README.md is publicly fetchable from github.com, so the "only options are 3.6 KB or 252 KB of HTML" premise is false. (2) prose-sections-carry-zero-fetchable-urls — a restatement of the same root defect as h2-sections-are-not-markdown-link-lists (sections are not link lists), counted twice; verified that lines 16-21/23-39/41-48 contain no `http`, but that is the same nonconformance already reported. (3) llms-txt-undiscoverable-from-every-other-surface — the file sits at the conventional well-known path `/llms.txt`, which IS the discovery mechanism; no crawler honours `<link rel="alternate" type="text/markdown">` as an llms.txt discovery signal, sitemaps enumerate HTML pages, and robots.txt:2 `Allow: /` already permits the path. The evidence also miscounts the head links (five entries at docs/nuxt.config.ts:49-55, not "seven at nuxt.config.ts:46-52"). (4) no-per-topic-markdown-twins-and-no-route-to-serve-them — an enhancement proposal for files that do not exist, not a defect in a file that does; the cited evidence (nitro block at docs/nuxt.config.ts:78-84, no docs/app/pages, hash-only NavBar.vue:13-19, `.nojekyll` present in docs/.output/public) is accurate but supports a feature request, and it overlaps no-llms-full-txt. (5) sitemap-omits-every-machine-readable-path — a sitemap lists indexable HTML pages; omitting a .txt and two .json assets has no verified failure mode and rests on the same discovery myth as #3. (6) no-optional-section — `## Optional` is by definition the section a client may drop; its absence cannot itself break a consumer, and no parser was shown to require it. The three surviving high-severity items are all product-fact errors, not formatting: every one of the 49 tool names is published without its `aster_` wire prefix (verified 1:1 by set diff), the Android floor is stated as 7.0 when minSdk is 26, and the per-mode tool namespace divergence is flattened away in both llms.txt and README.md:483.

### a11y-wcag

*26 findings — 1 critical, 7 high, 7 medium, 11 low*

Received 24 findings; 0 fully refuted, 24 survive with corrections. I independently re-derived every contrast number (sRGB relative-luminance formula, Tailwind v4 oklch→sRGB conversion from docs/node_modules/tailwindcss/theme.css, alpha composited in sRGB) and every one matched the original to within rounding — this audit's arithmetic is sound. What I did correct: 11 severities were inflated and are lowered here, and 9 findings carried refuted sub-claims, listed now. REFUTED SUB-CLAIMS: (a) "tabs-state-colour-only-no-aria" — the 2.1.1/arrow-key half is wrong; these are plain `<button>`s, fully keyboard-operable with Tab+Enter, and roving-tabindex is only required for role=tablist/radiogroup, which they never claim. (b) "alpha-modifiers-on-tertiary-text" — the mobile chevrons at HowItWorks.vue:161,174 are NOT the sole phase indicator; the adjacent icon tile at :153/:166/:179 swaps ring and fill on the same `phase >=` condition. (c) "aster-accent-alpha-text" — HowItWorks.vue:236 is `text-green-400/40`, not `text-aster/40`, and :221 is an Icon (3:1 threshold), not text. (d) "accent-eyebrow-labels-below-4-5" — ProactiveSection.vue:254 is a `badgeClass`, not a `labelColor`; the violet labelColor is at :362. (e) "meaningful-state-icons" — two of four cited sites are decorative, not state-bearing: the ScreenshotsSection tab icons sit beside labels that already differ ("Android App"/"Web Dashboard", "Dark"/"Light"), and SecuritySection.vue:124's check sits beside its own full text. (f) "lists-marked-up-as-divs" — the ordinal-semantics claim is wrong; ProactiveSection.vue:225 and SetupStep.vue:6 render the step number as a real text node that AT reads, so order IS conveyed. (g) "scroll-reveal-hides-content" — the unscoped-`querySelectorAll` "silently break" claim is refuted: both observers add the identical `is-visible` class and Vue scoped styles key on the class name, so whichever fires reveals the element correctly; no user-visible consequence. (h) "focus-obscured-by-fixed-navbar" — the hash-link-landing half is wrong; every section carries `py-32` (128px), clearing the 64px bar. (i) "embrace-phone-mock" — the 14px overflow is real but clips the phone frame edge, not the "Accept" button, which is inset behind a centred `gap-8` row. Also noted throughout: docs/nuxt.config.ts:25 forces `htmlAttrs: { 'data-theme': 'dark' }`, so the light-scheme half of finding 1 never renders on this site (it still governs mcp/dashboard), and the two AModal/shared-layer findings affect mcp/dashboard only — `grep -rnoE '<A[A-Z][A-Za-z]*' docs/app` returns zero matches. OVERALL VERDICT PRESERVED: the dominant, unarguable defect is contrast — the tertiary foreground token fails at full opacity across roughly a third of the page's readable text, and every alpha modifier stacked on top of it or on the accent palette compounds that failure. Below that sit one Level A failure (auto-advancing content with no pause) and a thin landmark/ARIA layer.

### ui-ux

*24 findings — 5 high, 13 medium, 6 low*

Received 25 findings; 2 REFUTED, 23 survive (10 with corrected severity, most with rewritten evidence). The original payload carried no overall score or verdict line, so there is none to preserve. REFUTED: (1) `featuresgrid-toolsshowcase-same-49-twice` — false premise. I read all 126 lines of FeaturesGrid.vue: it is a 16-card *capability* grid, not a taxonomy of the 49 tools. Five of its cards ('Companion Face', 'App Automations', 'Safety Rails', 'Three Connection Modes', '"Ask All Together"') name no MCP tool at all and appear nowhere in ToolsShowcase, whose 8 categories do partition exactly 49 tool names (7+5+6+4+6+3+10+8 = 49, ToolsShowcase.vue:51-157). The claim "FeaturesGrid cuts them 16 ways" is not what the file says, and the fix ("Delete FeaturesGrid") would delete unique content. The one real sub-claim (3 cards restate SecuritySection/ProactiveSection) is already covered by the surviving repetition findings. (2) `no-focus-states-anywhere` — the premise "Tailwind's preflight strips the UA outline" is false for Tailwind v4. I grepped the built stylesheet at docs/.output/public/_nuxt/entry.DqDXcy71.css: the only outline rule in the whole file is `:-moz-focusring{outline:auto}` and there are ZERO occurrences of `outline:none` or `outline:0`. Every `<a>` and `<button>` on the page therefore keeps the browser's default focus-visible ring; there is no keyboard-navigation failure. (The underlying fact — zero `focus:`/`focus-visible` in docs/app — is true, but it is a styling-polish gap, not the described defect, and it is folded into the design-system finding.) Severity corrections: scroll-burden critical→high (structure verified, the ~19,000/32,000px figures were unmeasured estimates and are removed from the evidence); embrace-duplicates, proactive/usecases twins, anecdote repetition, livechat-autoplay all high→medium (editorial/degraded-experience, nothing unreachable or broken); design-system adoption kept at high because it carries a real user-facing miss; no-scroll-margin medium→low (py-32 absorbs the whole 64px today, so the defect is latent); hover-only affordances medium→low (group colour still reads from the always-visible group label at UseCasesSection.vue:23-28); observe-fade medium→low, and its "Embrace's observer double-observes Proactive with Embrace's data-delay semantics" clause is wrong — the callback reads each element's own `el.dataset.delay` with an identical threshold, so the duplicate observation is inert. The two strongest verified defects are the 404 install link (curl-confirmed) and the screenshot row that is clipped and unscrollable at every width ≥640px.

### ia-routes

*8 findings — 2 high, 3 medium, 3 low*

Received 51 findings (sec-01…sec-15, data-01…data-25, dep-01…dep-11). REFUTED 43, kept 8 (with 5 severities corrected). The input carried no overall score or verdict to preserve — only the findings array — so none is restated here. The audit's factual spine is largely accurate: I independently reproduced the anchor inventory (9 ids across 9 components; NavBar.vue:13-19 links only 7; `#embrace` and `#screenshots` are orphaned; HeroSection, LiveChatSection, SecuritySection, AuthorSection, FooterSection carry no id), the absence of `pages/`/`layouts/`, the 49-name tool list, and almost every quoted line number. What it mostly lacks is DEFECTS: the `sec-*` block and the entire `data-*` block are an inventory of a deliberately-built one-page site, and their stated harms are conditional on a route split that has not happened. REFUTED, with reasons: sec-01/sec-02/sec-03/sec-05/sec-06/sec-07/sec-10/sec-11/sec-13/sec-14/sec-15 — accurate descriptions of intended single-page design; hash anchors, a `href="#setup"` CTA and an id-less hero all work today. sec-04 — its two "hardcoded" facts are both real: `aster set-openclaw-callbacks` is a registered alias (mcp/bin/aster.ts:409 `.alias('set-openclaw-callbacks')` on `.command('set-event-forwarding')`) and `/settings/openclaw` exists (mcp/dashboard/app/pages/settings/openclaw.vue). data-01…data-14 and data-16…data-25 — "hardcoded data in the component that renders it" is normal Vue, not a defect; specifically data-01's "the two lists do not even agree on which four products they name" is wrong (both name Claude, OpenClaw, MoltBot and the Clawbot product; only spelling differs), data-09's "four features absent from llms.txt" is wrong (llms.txt:44 covers Three Connection Modes; only 3 are absent, and a summary omitting detail is not a defect), data-11's "a missing /light/ file" does not exist — all 6 app and all 12 dashboard light variants ship under public/screenshots/*/light/, data-16's "contradict" is two illustrative demos on two fictional phones, not a fact conflict, data-17's `stepIndex` integers 0-3 map correctly and are in-bounds, data-24's `.mcp.json` ports 5987/5988/5989 match mcp/ and llms.txt:47, data-25's ClawdBot/ClawBot/Clawbot drift originates upstream in the repo's own README.md (lines 325 vs 405 use both spellings) — the page is faithful to an already-inconsistent source. dep-01 — the stated failure mechanism is impossible: BOTH `.observe-fade { opacity: 0 }` rules live in `<style scoped>` (EmbraceSection.vue:330, ProactiveSection.vue:398), so a section can never be left transparent by another section's missing observer; the unscoped selector only double-observes, with no visible effect. dep-02/dep-03/dep-04/dep-06/dep-10 — conditional-on-refactor or pure inventory; in particular dep-04's global canonical/og:url/JSON-LD is CORRECT for a site with exactly one route, and dep-06 is an inventory whose only actionable item duplicates sec-09. dep-11 — llms.txt being a hand-written summary is not a defect, its unprefixed tool list is already dep-07, and its two advertised manifests DO ship (public/runtimes/python/index.json, public/runtimes/whisper/index.json). SURVIVING, strongest first: one confirmed broken URL shipped in the install section (404 verified by request), one confirmed agent-facing contract break (every tool name published without the `aster_` prefix the server actually registers), three real content/IA gaps, and three low-severity duplication/drift items.

### perf-assets

*12 findings — 1 critical, 1 high, 3 medium, 7 low*

Received 17 findings; refuted 5 outright, kept 12 (one substantially rewritten because its stated mechanism was wrong). The original audit carried no numeric score or overall verdict to preserve. General note on provenance: almost every finding cites `docs/.output/public/_nuxt/1K_-OFdg.js`; that file does not exist in the current generate output — the entry chunk is `_nuxt/ZUg0zPNB.js`, though at the identical 282,612 bytes, so the byte figures are sound and only the hash is stale. `entry.DqDXcy71.css` (127,279 B) does match. REFUTED: (1) `no-nuxt-image-installed` — @nuxt/image is genuinely absent (docs/package.json has only fonts/icon; `grep -c @nuxt/image docs/pnpm-lock.yaml` = 0), but "a module is not installed" is a fix direction, not a defect; its entire stated impact is finding `screenshots-are-89pct-of-artifact-unoptimised`, and its one distinguishing claim ("every one of the 18 `<img>` tags in the generated HTML") is false — there are 10. (2) `vue-router-declared-but-not-bundled` — the finding itself concedes "Harmless today", and `docs/node_modules/nuxt/package.json:103` lists `"vue-router": "^4.6.4"` as a direct dependency of `nuxt`, so removing it from docs/package.json changes neither the install graph nor the artifact; no defect. (3) `route-split-regressions` — analyses a refactor nobody has made; nothing in the current tree is wrong (canonical at nuxt.config.ts:54 and the 1-URL sitemap are both correct for a one-page site), and its baseline table cites a chunk filename that no longer exists. (4) `orphan-runtime-manifests-in-public` — its central claim ("a repo-wide grep returns only the two files themselves — no consumer... no in-tree writer") is false: `docs/public/llms.txt:52-53` publishes both manifest URLs, and `docs/scripts/build-python-android.sh:379` is the documented writer, whose step 3 reads "Deploy GitHub Pages to make the updated index.json live". (5) `devtools-verified-not-shipped` — self-declared clean with "No change needed. Leave as-is."; I re-confirmed 0 devtools strings in all 4 emitted JS chunks and index.html, but a verified non-defect is not a finding. Severities corrected downward on 7 of the 12 survivors where the mechanism was real but the impact was asserted rather than measured. The two largest defects — 8.84 MB of unoptimised screenshots at 88.6% of a 9,986,221-byte artifact, and the fact that every @font-face shipped is weight 400 while the token file demands 500/600/700 — both hold up in full against the built output.

## Findings

### content-facts

#### `CRITICAL` "Owner-Approved Folders" constrains nothing on the MCP path — the shipped file tools take any absolute path

**Where:** `docs/app/components/SecuritySection.vue`:202

**Evidence:**

```
SecuritySection.vue:201-202 — title: 'Owner-Approved Folders', description: 'File access outside public storage is limited to directories you\'ve explicitly shared.'
FeaturesGrid.vue:109-110 — 'Owner-Approved Folders' / 'Share exactly the directories you choose — the AI reads and lists only folders you\'ve explicitly approved.'

I read the code path. The owner-approval mechanism lives ONLY in HostDirHandler, whose actions are not MCP tools:
  apps/android/app/src/main/java/com/aster/service/handlers/HostDirHandler.kt:53 — `override fun supportedActions() = listOf("files.read", "files.list")` (its KDoc, lines 16-27, says it exists for the OpenAlly App Builder host-dir feature and takes a `host_dir` handle).
The MCP file tools route to FileSystemHandler, which has no allowlist at all:
  FileSystemHandler.kt:18-23 — `supportedActions() = listOf("list_files","read_file","write_file","delete_file")`
  FileSystemHandler.kt:198-207 — `private fun resolvePath(path: String) { path.startsWith("/") -> path ; "~" -> external storage root ; "internal:" -> context.filesDir ; "cache:" -> context.cacheDir ; else -> external storage root + "/" + path }` — an absolute path is passed straight through.
  mcp/src/mcp/tools.ts:411-457 — aster_list_files / read_file / write_file / delete_file each declare only `{ deviceId, path }`; no host_dir, no scoping.
  apps/android/app/src/main/AndroidManifest.xml:33 — `MANAGE_EXTERNAL_STORAGE`.
Note the narrower SecuritySection wording does not rescue it either: `internal:` and `cache:` in resolvePath are outside public storage and are not owner-shared folders.
```

**Why it matters:** The page's whole topology story is the Remote-WebSocket / MCP-HTTP path, and on that path an approved device's AI reads, writes and deletes any path reachable under MANAGE_EXTERNAL_STORAGE. This is a security guarantee stated on the Security section that the shipped code does not provide.

**Fix:** Scope the claim to the on-device/IPC host-dir surface, and say plainly that the MCP file tools address any readable path on an approved device.

#### `HIGH` Direct skill-install link points at satyajiit/Aster, a repo that does not exist

**Where:** `docs/app/components/IntegrationsSection.vue`:89

**Evidence:**

```
IntegrationsSection.vue:89 — `<span class="text-text-secondary break-all">https://raw.githubusercontent.com/satyajiit/Aster/main/skill/SKILL.md</span>`, under the heading at :80 'Install the skill directly from the GitHub repository raw link for any compatible client.'
`git remote -v` in aster-mcp/ → `origin https://github.com/satyajiit/aster-mcp.git`.
Every other GitHub link on the page uses satyajiit/aster-mcp: FooterSection.vue:17, :26, :35; HeroSection.vue:59; SecuritySection.vue:144.
The file itself exists: `ls skill/` → `SKILL.md`. So only the repo segment of the URL is wrong ('Aster' is a different repo name, not a case variant of 'aster-mcp').
```

**Why it matters:** This is one of only two install paths the Integrations section offers, and it is a hard 404 — the non-ClawHub route fails for every reader who takes it.

**Fix:** Change to https://raw.githubusercontent.com/satyajiit/aster-mcp/main/skill/SKILL.md; add a link check over raw.githubusercontent URLs to the docs build.

#### `HIGH` make_call_with_voice sold as "fully autonomous" / "fully hands-free" with the tool's own acoustic-coupling caveat stripped

**Where:** `docs/app/components/FeaturesGrid.vue`:74

**Evidence:**

```
The tool's own description, mcp/src/mcp/tools.ts:648: `description: 'Make a phone call and speak text via TTS on the loudspeaker. Audio is acoustic coupling (device loudspeaker into the call microphone), not routed to the call audio. Works best in a quiet room; quality is device-dependent.'`
The page, with the caveat absent everywhere:
  FeaturesGrid.vue:74 — '...use make_call_with_voice to dial someone and speak a message via TTS on speakerphone — fully autonomous.'
  LiveChatSection.vue:250 — 'Called Mom with speakerphone and spoke a message via TTS — fully hands-free.'
  HowItWorks.vue:329 — 'Called Mom, spoke message via TTS on speakerphone after 8s wait.'; HowItWorks.vue:349 — same shape for the AI-initiated call.
  EmbraceSection.vue:249 — 'make_call_with_voice — dials you, enables speaker, and delivers a TTS message.'
  UseCasesSection.vue:197-201 and HeroSection.vue:220-223 — same, unqualified.
`grep -rn "acoustic|loudspeaker|quiet room" docs/` returns nothing — the caveat appears on no docs surface.
```

**Why it matters:** The page's most-repeated differentiator rests on a technique the source itself flags as quiet-room-only and device-dependent; 'fully autonomous' and 'fully hands-free' are the opposite of what the maintainers documented.

**Fix:** Carry the tool's own one-line caveat into FeaturesGrid and the two HowItWorks voice-call scenarios; drop 'fully autonomous' / 'fully hands-free'.

#### `HIGH` Baby-monitor card claims audio monitoring the app has no permission and no code for

**Where:** `docs/app/components/UseCasesSection.vue`:297

**Evidence:**

```
UseCasesSection.vue:297-300 —
  prompt: 'Keep the phone in the baby\'s room — if it picks up crying sounds, text me immediately'
  response: 'Audio monitoring active. Detected unusual noise — taking a photo and texting you with status. Everything looks calm.'
  tools: ['take_photo', 'send_sms', 'notification_event']
I grepped the app: `grep -rn "RECORD_AUDIO|MediaRecorder|AudioRecord" apps/android/app/src/main/` returns zero matches, and AndroidManifest.xml's uses-permission block (lines 6-67) contains no audio-capture permission (camera is at :54-55).
No sound-detection tool exists among the 49 in mcp/src/mcp/tools.ts (I enumerated all 49 `name: 'aster_*'` entries; none captures or analyses audio input).
The card's own tool chips (take_photo, send_sms) contradict its response text.
```

**Why it matters:** This is not a stretch of an existing capability, it is one the shipped app cannot perform at all — and it is stated in the safety-adjacent context most likely to be taken literally.

**Fix:** Rewrite the card around what exists (scheduled photo + send_sms, the shape the Proactive baby-monitor card already uses) or remove it.

#### `MEDIUM` "Approved before connecting" is wrong — unapproved devices connect and stay connected; approval gates command dispatch

**Where:** `docs/app/components/SecuritySection.vue`:178

**Evidence:**

```
SecuritySection.vue:177-178 — title: 'Device Approval', description: 'Every new device must be manually approved from your dashboard before connecting.'
I read the server. A new device is admitted and registered on connect:
  mcp/src/websocket/index.ts:377-389 — `// New device - create pending entry` ... `status: 'pending'` ... `addLog(deviceId, 'info', 'New device registered, pending approval');`
  index.ts:401-409 — the connection is stored in `connectedDevices` and a heartbeat interval is started regardless of status.
  index.ts:425-433 — `ws.send(... type: 'auth_result', success: true, status: device.status, message: device.status === 'approved' ? 'Connected and approved' : 'Connected, pending approval')`.
The real gate is at dispatch, index.ts:127-129 — `if (connected.device.status !== 'approved') { throw new Error(\`Device ${deviceId} is not approved\`); }`
README.md:506 states the same: 'Because there's no shared secret, **any client on the network that knows the port can register as `pending`** — so the trust boundary is your network plus your approval tap. Keep the server off untrusted LANs.'
```

**Why it matters:** The page states a stronger property than the code provides, and README treats the weaker real property as a warning worth spelling out. An operator told nothing can attach before approval may reason the port is safe to expose.

**Fix:** Say approval gates command execution — an unapproved device connects and sits inert — and repeat README's warning about keeping :5987/:5988 off untrusted networks.

#### `MEDIUM` Security section presents WireGuard as the transport story and never mentions that the default device link is plaintext ws://

**Where:** `docs/app/components/SecuritySection.vue`:57

**Evidence:**

```
SecuritySection.vue:15 heading — 'Your data never leaves your network'.
SecuritySection.vue:56-58 — 'The MCP server runs on your machine. For remote access, Aster auto-detects Tailscale and routes traffic over an encrypted WireGuard mesh — zero config, no port forwarding.'
SecuritySection.vue:61-84 — the diagram draws exactly one path: 'Your PC' → lock icon labelled 'WireGuard' → 'Android'. There is no second, unencrypted path drawn.
SecuritySection.vue:210-212 tailscalePoints — 'Self-hosted — MCP server runs locally, nothing sent externally' / 'Auto-detect — Tailscale encryption with zero configuration' / 'No port forwarding — VPN mesh, no public exposure'.
README.md:489-493 devotes a section to the opposite framing: '### The Node `ws` server does not terminate TLS' / 'On a trusted LAN the device ↔ server link is an **unencrypted WebSocket** (`ws://`, port `5987`). The Node `ws` server does **not** speak TLS itself'. README.md:669 repeats it in the FAQ: 'Not by default. On a trusted LAN the device link is plain <code>ws://</code>'.
SetupSteps.vue:52-57 itself prints the default as `WebSocket → ws://192.168.1.x:5987`.
```

**Why it matters:** The page's own setup step ships the plaintext default while the security section illustrates only the encrypted remote path under an absolute heading, so a reader forms the belief that the phone↔server link is encrypted out of the box — the exact misreading README was written to prevent.

**Fix:** State the LAN default is ws:// on a trusted network and position Tailscale / a TLS terminator as what you add for remote, mirroring README §'Securing the connection'.

#### `MEDIUM` llms.txt and README state Android 7.0+ while the app pins minSdk 26 = Android 8.0

**Where:** `docs/public/llms.txt`:48

**Evidence:**

```
docs/public/llms.txt:48 — '- Requirements: Node.js >= 20 (server), Android 7.0+ (app)'
docs/public/llms.txt:5 — '...an Android companion app (no root required, Android 7+).'
apps/android/app/build.gradle.kts:16 — `minSdk = 26` (API 26 = Android 8.0 Oreo); :17 `targetSdk = 36`; :19 `versionName = "1.7.1"`. Line 141 of the same file confirms the floor in a comment: 'ML Kit minSdk 21; project is 26'.
README.md:24 — badge `Android_7%2B-supported`; README.md:655 — '- **Android**: Android 7.0+ with Accessibility Service enabled'.
```

**Why it matters:** llms.txt is the fact sheet answer engines and developers read as authoritative, and this is its one hard compatibility number. It is wrong by a full major release; the landing page states no minimum at all, so llms.txt and README are the only places this fact lives and both are wrong.

**Fix:** Change to Android 8.0+ (API 26) at llms.txt:5 and :48 and README.md:24 and :655; derive the badge from build.gradle.kts so it cannot drift again.

#### `MEDIUM` HowItWorks renders two tool-call parameters that do not exist in the tool schemas

**Where:** `docs/app/components/HowItWorks.vue`:306

**Evidence:**

```
HowItWorks.vue:306 — `aiCall: 'search_media({ query: "beach", month: 12 })'`
mcp/src/mcp/tools.ts:793-822 aster_search_media properties, read in full: deviceId, query, path, dateFrom, dateTo, location{city,country,latitude,longitude,radiusKm}, fileTypes, minSizeMB, maxSizeMB, cameraModel, sortBy, limit. There is no `month`.
HowItWorks.vue:336 — `aiCall: 'read_notifications({ filter: "priority" })'`
mcp/src/mcp/tools.ts:323-333 aster_read_notifications: description 'Read active notifications from the device notification shade'; properties are exactly `deviceId` and `limit` (default 20). There is no `filter`.
```

**Why it matters:** These render as literal calls inside a trace panel — the shape a developer or a model copies — and both would fail schema validation. The real date filter (dateFrom/dateTo) is more capable than the invented `month`, so the fabrication also undersells the tool.

**Fix:** Use real params: search_media({ query: "beach", dateFrom: "2024-12-01", dateTo: "2024-12-31" }) and read_notifications({ limit: 20 }).

#### `MEDIUM` Every tool name on the page and in llms.txt is shown with the registered aster_ prefix stripped

**Where:** `docs/app/components/ToolsShowcase.vue`:51

**Evidence:**

```
ToolsShowcase.vue:51-58 (first category) — 'take_screenshot', 'get_screen_hierarchy', 'find_element', 'input_gesture', 'input_text', 'click_by_text', 'click_by_id'; the pattern continues through :157.
Registered names all carry the prefix — mcp/src/mcp/tools.ts, all 49 entries match `name: 'aster_*'` (e.g. :303 'aster_list_devices', :460 'aster_take_screenshot', :647 'aster_make_call_with_voice').
`grep -rn "aster_" docs/app/components/ docs/public/llms.txt` returns ZERO matches — the prefix appears nowhere on any docs surface, including the llms.txt tool inventory at lines 25-39.
Partial mitigation I verified: the on-device Ktor MCP mode registers unprefixed names (apps/android/.../service/mode/McpToolRegistry.kt:21+ keys 'get_device_info', 'list_files', ...), so the stripped forms are valid in that one mode — but not on the npm-server path the page's setup flow documents.
```

**Why it matters:** No name shown can be typed at the MCP client the page tells you to configure, and llms.txt repeats the stripped forms, so an answer engine ingesting either surface will emit tool names that do not resolve.

**Fix:** Render the full aster_* names (or show the prefix once per category header) and apply the same fix to llms.txt:25-39.

#### `MEDIUM` "MIT licensed" asserted on three page surfaces and in JSON-LD, but the repository root has no LICENSE

**Where:** `docs/app/components/SecuritySection.vue`:140

**Evidence:**

```
SecuritySection.vue:139-140 — '100% Open Source' / 'MIT licensed. Read every line, audit every tool, fork and modify freely.', linking to https://github.com/satyajiit/aster-mcp (SecuritySection.vue:144).
HeroSection.vue:21 — 'Open Source · MIT Licensed'.
FooterSection.vue:67 — 'MIT License · A Matterward Labs project'.
docs/nuxt.config.ts:61,69 — SoftwareApplication JSON-LD with `license: 'https://opensource.org/licenses/MIT'`.
`find . -iname 'LICENSE*'` excluding node_modules/.output/build returns exactly one path: `./mcp/LICENSE`. There is no LICENSE at the repo root, so README.md:20's badge link https://github.com/satyajiit/aster-mcp/blob/main/LICENSE is a 404 and apps/android/ — the companion app the page invites you to fork — is covered by no licence file.
```

**Why it matters:** 'Fork and modify freely' is a legal invitation, and the only licence in the tree covers the npm package directory. The Android app, the docs site and packages/aster-ui ship with no stated terms.

**Fix:** Add an MIT LICENSE at the repo root (keeping mcp/LICENSE) so the badge link resolves and the claim covers the whole tree.

#### `MEDIUM` "One command, no extra setup, no plugins" — the real flow requires a configured OpenClaw, is interactive, and needs a restart

**Where:** `docs/app/components/ProactiveSection.vue`:156

**Evidence:**

```
ProactiveSection.vue:156 — 'Enable proactive event forwarding in one command. Your AI agent starts receiving real-time phone events via webhook — no extra setup, no plugins.'
ProactiveSection.vue:175-178 terminal mock — command shown is `aster set-openclaw-callbacks` (the legacy alias; the current name is `set-event-forwarding`, mcp/bin/aster.ts:406-408), then '✓ Token loaded from ~/.openclaw/openclaw.json' and '✓ Webhook connected — events will forward automatically'.
The real command, mcp/bin/aster.ts:411-417 — `const token = getLegacyOpenClawSourceToken(); if (!token) { console.log(chalk.red('Error: No token found in ~/.openclaw/openclaw.json (gateway.auth.token).')); console.log(chalk.gray('Please install and configure OpenClaw first.')); process.exit(1); }` — a hard exit without a pre-existing OpenClaw install.
bin/aster.ts:420-426 — it then prompts interactively for 'OpenClaw endpoint URL' and 'Webhook path' via readline.
bin/aster.ts:467 — the last line it prints is 'Restart Aster for changes to take effect.', not 'events will forward automatically'.
It is also the ONLY forwarding command: `grep -n ".command('" mcp/bin/aster.ts` lists start, stop, logs, status, dashboard, set-event-forwarding, mcp, devices{list,approve,reject,remove} — no non-OpenClaw variant.
```

**Why it matters:** Three prerequisites are elided (OpenClaw already installed and configured, an interactive prompt pair, a server restart), and the mock shows a success line immediately where the real CLI prints the restart instruction instead.

**Fix:** Say it is one command plus a restart, note that it reads an existing OpenClaw config, and show the current command name rather than the legacy alias.

#### `MEDIUM` "For how long" — list_installed_apps returns last-used time, not usage duration (finding's file path was wrong: it is EmbraceSection, not UseCasesSection)

**Where:** `docs/app/components/EmbraceSection.vue`:285

**Evidence:**

```
The original finding named UseCasesSection.vue:285; that line is the baby-monitor card. The copy is actually at EmbraceSection.vue:285-286 —
  scenario: '"What apps did my kid use today and for how long?"'
  detail: 'Reads app usage stats and gives you a clear breakdown. No snooping apps needed.'
The only app-inventory tool, mcp/src/mcp/tools.ts:397-398 — 'List installed apps with full metadata for on-device analysis: package, label, version, install/update time, system flag, on-disk sizes, declared permissions, and last-used time (when Usage access is granted). Paged via cursor.'
The Android side confirms only a timestamp: InstalledAppsHandler.kt:155-163 — `val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager` ... `if (s.lastTimeUsed > 0 && (prev == null || s.lastTimeUsed > prev)) { out[s.packageName] = s.lastTimeUsed }`. `totalTimeInForeground` is never read, and no other tool among the 49 exposes per-app foreground duration.
```

**Why it matters:** The vignette's question has two halves and only the first is answerable; the promised 'clear breakdown' of duration cannot be produced, in a parental-monitoring context where the duration is the point.

**Fix:** Reword to what list_installed_apps returns — which apps were opened and when they were last used — or drop the duration clause.

#### `MEDIUM` Setup section states no Node version, no Android minimum, and omits Camera / Contacts / Overlay from the permission list

**Where:** `docs/app/components/SetupSteps.vue`:65

**Evidence:**

```
SetupSteps.vue:65-70 — the 'Required permissions:' block lists exactly five: Accessibility Service, Notification Access, Location, Storage, SMS & Phone.
Missing, though the page advertises the tools that need them:
  AndroidManifest.xml:54-55 — CAMERA + FOREGROUND_SERVICE_CAMERA, behind take_photo / record_video, sold at FeaturesGrid.vue:83-85 and in five Proactive/UseCase cards.
  AndroidManifest.xml:50-51 — READ_CONTACTS / WRITE_CONTACTS, behind the Contacts tool category in ToolsShowcase.vue.
  AndroidManifest.xml:67 — SYSTEM_ALERT_WINDOW, behind show_overlay and the kill switch that SecuritySection.vue:184-186 presents as a headline safety feature.
SetupSteps.vue:49-52 step 1 says 'Install the aster-mcp package from npm globally' and shows `npm install -g aster-mcp` with no Node floor, though mcp/package.json:86-88 declares `"engines": { "node": ">=20" }`; no step states an Android minimum (build.gradle.kts:16 `minSdk = 26`).
```

**Why it matters:** Both version floors are real install blockers and both are stated in llms.txt:48 — the setup section is the only surface that omits them. The permission block is the page's answer to 'what am I granting?', so omitting Camera understates the ask for the pet-cam and baby-monitor use cases, and omitting SYSTEM_ALERT_WINDOW leaves the advertised kill switch without its enabling grant.

**Fix:** Add Node >= 20 and Android 8.0+ (API 26) to steps 1 and 2, and extend the permission block with Camera, Contacts and Display-over-other-apps.

#### `LOW` Six invented identifiers rendered in the same chip style as real tool names

**Where:** `docs/app/components/UseCasesSection.vue`:259

**Evidence:**

```
UseCasesSection.vue chips: 'event_forwarding' (:259), 'sms_event' (:265), 'notification_event' (:271, :288, :294, :300). HeroSection.vue:221 action string — 'notification_event → delay detected → make_call_with_voice...'.
None is an MCP tool: I enumerated all 49 `name: 'aster_*'` entries in mcp/src/mcp/tools.ts and none of these appears. Nor are they event names — the real event types are in mcp/src/event-forwarding/index.ts:209-238 and :259-264: `sms_received`, `notification`, `device_connected`, `device_disconnected`, `pairing_required`, `incoming_call`.
They render through the identical chip markup as the genuine tool names (UseCasesSection.vue:62-72, `v-for="tool in card.tools"`, same `font-mono` class and `group.tagClass`).
```

**Why it matters:** A reader cannot distinguish the six invented identifiers from the 30+ genuine tool names elsewhere on the page. Severity corrected down from medium: these sit in illustrative scenario cards rather than in the tool inventory or a code sample, and the real event names would serve the same narrative.

**Fix:** Use the real event names (sms_received, notification, incoming_call) and give event chips a visually distinct style from tool chips.

#### `LOW` Every tool-call sample in the HowItWorks trace omits the required deviceId argument

**Where:** `docs/app/components/HowItWorks.vue`:296

**Evidence:**

```
HowItWorks.vue:296 `aiCall: 'take_screenshot()'`; :306 `'search_media({ query: "beach", month: 12 })'`; :316 `'vibrate({ pattern: [0, 500, 200, 500] })'`; :326 and :346 `'make_call_with_voice({ number: ..., text: ..., waitSeconds: 8 })'`; :336 `'read_notifications({ filter: "priority" })'`.
Every one of the 49 schemas makes deviceId mandatory — mcp/src/mcp/tools.ts:466 `required: ['deviceId']` (take_screenshot), :595 `required: ['deviceId', 'pattern']` (vibrate), :657 `required: ['deviceId', 'number', 'text']` (make_call_with_voice), :821 `required: ['deviceId']` (search_media).
The samples are otherwise faithful: the vibrate waveform semantics and the waitSeconds default of 8 (tools.ts:655) both match.
```

**Why it matters:** Composed with the stripped aster_ prefix, no call shown on the page would execute. Severity corrected down from medium: eliding a constant id in a stylised trace panel is a common readability convention, and the invented-parameter finding already covers the two calls that are actually wrong rather than abbreviated.

**Fix:** Add deviceId to the samples, or note once that deviceId is required on every tool and elided in the trace for readability.

#### `LOW` Four AI clients named, one spelled three different ways, with a different roster in each section

**Where:** `docs/app/components/IntegrationsSection.vue`:130

**Evidence:**

```
IntegrationsSection.vue:130 — client card `name: 'ClawBot'`; IntegrationsSection.vue:12 — 'Give Claude, OpenClaw, MoltBot, or ClawBot a CoPilot'; :59 — 'for OpenClaw, MoltBot, and ClawBot'.
HeroSection.vue:149-150 — `clawdbot: { name: 'ClawdBot' ... }`, used at :182 and :210; the Integrations grid has no ClawdBot entry.
ProactiveSection.vue:153 — 'Works out of the box with OpenClaw, ClawdBot & MoltBot' — no ClawBot.
LiveChatSection.vue:52 — 'ClawdBot'.
A third spelling off-page: docs/public/llms.txt:13 and :21 — 'Clawbot'; README.md:405 — '### OpenClaw / Moltbot / Clawbot'.
mcp/package.json:24 keywords contain 'clawdbot' (with 'openclaw', 'moltbot'); there is no 'clawbot' keyword. docs/nuxt.config.ts:29 lists both 'clawdbot' and 'clawbot' in the keywords meta.
```

**Why it matters:** Two sections advertise different rosters and the integrations grid uses a spelling that appears nowhere in package.json, so a reader cannot tell whether ClawBot and ClawdBot are one product or two. Severity corrected down from medium: this is a naming-consistency defect, not a false capability claim (and the nuxt.config keywords meta is inert for ranking regardless).

**Fix:** Pick one spelling per product, align IntegrationsSection / ProactiveSection / HeroSection on the same roster, and reconcile with README and llms.txt.

#### `LOW` Recurring-schedule scenarios imply a scheduler Aster does not have, and the standup card picks a tool that fires immediately

**Where:** `docs/app/components/ProactiveSection.vue`:292

**Evidence:**

```
ProactiveSection.vue:292 — 'Schedule triggers every 30 minutes'; :352 — 'Periodic check every 15 minutes'.
UseCasesSection.vue:256 — 'Left my dog home alone — take a photo every 30 mins and send it to me on WhatsApp'.
UseCasesSection.vue:140-144 — prompt 'Ring my phone at 10 AM tomorrow for the standup meeting' → response 'Done! I'll play a ringtone and show a notification at 10:00 AM' → `tools: ['play_audio', 'post_notification']`.
No scheduling surface exists: none of the 49 tools in mcp/src/mcp/tools.ts schedules future work, and mcp/src/index.ts:125-131 starts only createWebSocketServer, startApiServer and the dashboard child process — no cron, no timer service (`grep -n "cron|schedule|setInterval" mcp/src/index.ts` returns nothing).
The tool that does timed alerts exists and is unused here: mcp/src/mcp/tools.ts:911-923 aster_set_alarm ('Create a new alarm using the device clock app', params hour/minute/message/days), listed in ToolsShowcase.vue under Alarms.
```

**Why it matters:** Timing comes from the calling agent, not from Aster, so these cards attribute the capability to the wrong layer; the standup card names two tools that both fire immediately while set_alarm does exactly what the prompt asked.

**Fix:** Swap the standup card to set_alarm and phrase the recurring cards as the agent checking in, not Aster scheduling.

#### `LOW` Shell Execution card claims package management no tool provides

**Where:** `docs/app/components/FeaturesGrid.vue`:56

**Evidence:**

```
FeaturesGrid.vue:54-56 — title: 'Shell Execution', description: 'Run shell commands within the app sandbox. Launch intents, manage packages, and automate workflows.'
The only package tools are read-only: mcp/src/mcp/tools.ts:385-386 aster_list_packages — 'List installed Android packages/applications'; :397-398 aster_list_installed_apps — list with metadata.
The shell tool rules the rest out in its own description, mcp/src/mcp/tools.ts:374 — 'Execute a shell command within the Android app sandbox. Runs as an unprivileged app process (no root) — restricted to the app's own data directory, standard Android utilities, and user-accessible storage. Cannot modify system files, access other app data, or bypass Android permissions. Commands are subject to a 30-second timeout and 1MB output limit.'
```

**Why it matters:** 'Manage packages' reads as install/uninstall, which an unprivileged unrooted app process cannot do; the card also omits the 30 s / 1 MB limits that bound what 'automate workflows' means here.

**Fix:** Change to 'list installed packages' and mention the 30-second / 1 MB sandbox limits.

#### `LOW` "Nothing sent externally" sits on the same page as repeated WhatsApp-forwarding scenarios

**Where:** `docs/app/components/SecuritySection.vue`:18

**Evidence:**

```
SecuritySection.vue:15-18 — 'Your data never leaves your network' / 'Aster is fully self-hosted. No cloud, no telemetry, no third-party relay'. SecuritySection.vue:210 — 'Self-hosted — MCP server runs locally, nothing sent externally'.
The same page, repeatedly: HeroSection.vue:213-216 'take a photo and send it to my WhatsApp' → 'Sent to your WhatsApp.'; ProactiveSection.vue:294; UseCasesSection.vue:256-257 'send it to me on WhatsApp'; EmbraceSection.vue:252 'forwarded to your WhatsApp in seconds'.
The forwarder does carry a delivery channel — mcp/bin/aster.ts:454-456 `channelType: 'openclaw' as const, channel: 'whatsapp', deliverTo: ''`.
Severity corrected down from medium because I verified the underlying claim is technically true of Aster: the webhook POSTs to the operator's own agent endpoint (default `http://localhost:18789`, aster.ts:425) and the WhatsApp delivery is performed by that agent, not by Aster — so 'no third-party relay' holds for the server itself.
```

**Why it matters:** The absolutes ('never', 'nothing sent externally') read as falsified by the product's most-repeated use case sitting a scroll away, which makes a genuinely strong posture look overstated.

**Fix:** Scope it: Aster itself sends nothing outbound by default; anything your agent forwards travels over that agent's channels, not Aster's.

### geo-citability

#### `HIGH` The LiveChat transcript and its four outcome steps are absent from the prerendered HTML

**Where:** `docs/app/components/LiveChatSection.vue`:201

**Evidence:**

```
Read at LiveChatSection.vue:200-204 — `const chatBody = ref<HTMLElement | null>(null)` / `const visibleMessages = ref<Message[]>([])` / `const visibleSteps = ref<Step[]>([])`. The 13-entry `script` array is at 206-220 and the 4-entry `steps` array at 222-255, but neither is ever assigned at module scope: the only writer is `runScript()` (268-317), invoked solely from `onMounted(() => { runScript() })` at 325-327. The template iterates `visibleMessages` (line 67) and `visibleSteps` (line 141), so `nuxt generate` emits the section header (lines 9-15) and an empty phone shell only.
```

**Why it matters:** The section's own copy (line 14) promises the page's single longest end-to-end worked example, and none of it — the storage breakdown, the duplicate-file numbers, the four outcome steps 'Storage analyzed' / 'Duplicates found' / '5.5 GB freed' / 'Voice call placed' — reaches a crawler that fetches raw HTML without executing JS.

**Fix:** Seed `visibleMessages`/`visibleSteps` with the full script at module scope so SSR emits the transcript, then reset to [] in `onMounted` before re-animating.

#### `HIGH` All 49 MCP tool names on the page are printed without the real `aster_` prefix

**Where:** `docs/app/components/ToolsShowcase.vue`:51

**Evidence:**

```
ToolsShowcase.vue:51-59 — `tools: ['take_screenshot','get_screen_hierarchy','find_element','input_gesture','input_text','click_by_text','click_by_id']`; the eight categories at 45-159 total exactly 49 names, every one unprefixed. In mcp/src/mcp/tools.ts the registered names are prefixed — line 460 `name: 'aster_take_screenshot'`, line 471 `name: 'aster_get_screen_hierarchy'`, line 736 `name: 'aster_click_by_id'`; `grep -c "name: 'aster_" mcp/src/mcp/tools.ts` returns 49 and there is no unprefixed definition. Same stripping in docs/public/llms.txt:25-39, HeroSection.vue:172/193/200, HowItWorks.vue:296/306/316/336, UseCasesSection.vue:230-242.
```

**Why it matters:** The h2 at ToolsShowcase.vue:9 ('49 tools at your AI's fingertips') is the passage an assistant will lift to answer 'what is the tool name to take a screenshot with Aster?'. Every name it can quote is non-callable, and no surface on the domain — page or llms.txt — carries the correction.

**Fix:** Print the real callable names, or add one sentence under the h2: 'All tools are namespaced `aster_*` — e.g. `aster_take_screenshot`.' Fix llms.txt:25-39 in the same pass.

#### `HIGH` The Security section headline makes an unscoped locality claim the described flow does not support

**Where:** `docs/app/components/SecuritySection.vue`:15

**Evidence:**

```
SecuritySection.vue:15 `Your data never leaves your network` and :18 'Aster is fully self-hosted. No cloud, no telemetry, no third-party relay — whether it's your CoPilot on mobile or a dedicated AI device.' The page's own primary flow is a cloud MCP client: SetupSteps.vue:78 'Add the Aster MCP endpoint to Claude Desktop, Claude Code, or any MCP-compatible client'; HowItWorks.vue:86-87 names the first node 'AI Assistant / MCP Client'. Screenshots, the UI hierarchy, SMS bodies and notification text all flow to that client. README.md:513 makes the defensible, narrower claim: 'The **only** outbound call is the *optional* event-forwarding webhook, which is **off** unless you run `aster set-event-forwarding`.'
```

**Why it matters:** This h2 is the most quotable sentence in the section and the one an answer engine repeats verbatim. It is also exactly the unscoped locality claim the workspace claim-discipline rule forbids for a product that talks to third parties.

**Fix:** Scope it to what Aster itself does: 'Aster adds no cloud of its own — no telemetry, no vendor relay, no account. What your AI client sends to its own model is governed by that client.' The six pillars at 158-207 are already accurate and strong.

#### `HIGH` The page contains no list, table, definition list, FAQ or summary block of any kind

**Where:** `docs/app/components/SetupSteps.vue`:16

**Evidence:**

```
`grep -rn "<ul|<ol|<table|<thead|<dl|<dt|<main|<article" docs/app/` returns zero matches across app.vue and all 17 components. The four-step setup is `<SetupStep v-for="(step, i) in steps" :number="i + 1" …>` at SetupSteps.vue:16-40, and SetupStep.vue:6 renders the step number as `<span class="text-sm font-bold text-aster font-mono">{{ number }}</span>` inside a div — not `<ol>`/`<li>`. Every other list-shaped block is the same pattern: ToolsShowcase.vue:18-38 (8 categories × 49 tools), FeaturesGrid.vue:16-22 (16 cards), SecuritySection.vue:24-37 (6 pillars), UseCasesSection.vue:21-75 (21 cards), IntegrationsSection.vue:17-29 (4 clients).
```

**Why it matters:** On a single-page site with no other addressable units, there is no `<ol>` marking the install procedure, no `<dl>` marking definitions, no `<table>` marking a comparison, and no question-shaped passage or TL;DR anywhere — nothing short and self-contained for a model to lift.

**Fix:** Make the setup an `<ol>`, make ToolsShowcase categories `<ul>`s under their h3, and add a 'Quick facts' `<dl>` (platform, requirements, license, price, ports, tool count) plus a small FAQ with h3 questions.

#### `HIGH` Neither the Android version nor the Node version requirement appears anywhere on the page

**Where:** `docs/app/components/SetupSteps.vue`:50

**Evidence:**

```
SetupSteps.vue:50 is the page's most specific install prose — 'Install the aster-mcp package from npm globally and start the server. It launches a WebSocket on port 5987 and an MCP HTTP endpoint on port 5988.' — and states no version floor. `grep -rni "android 7|minsdk|requirement|node 20|>= *20" docs/app/components/` returns only two decorative hits: HowItWorks.vue:100 and :172, both the node-label string 'Node.js · WS Bridge'. The facts exist off-page: mcp/package.json:86-88 `"engines": { "node": ">=20" }` and docs/public/llms.txt:48 'Requirements: Node.js >= 20 (server), Android 7.0+ (app)'.
```

**Why it matters:** 'Will this run on my phone?' and 'what Node version do I need?' are the two highest-intent pre-install questions, and the page cannot be cited for either; a model forced to infer will guess.

**Fix:** Add a requirements line to the SetupSteps intro or a 'Quick facts' dl, and mirror it into the JSON-LD (`softwareRequirements`, `operatingSystem: 'Android 7.0+'` — currently just `'Android'` at nuxt.config.ts:66).

#### `MEDIUM` Three strings rendered in the tool-tag pill are not tools and do not exist as identifiers anywhere in the repo

**Where:** `docs/app/components/UseCasesSection.vue`:259

**Evidence:**

```
UseCasesSection.vue:259 `tools: ['take_photo','send_sms','event_forwarding']`, :265 `tools: ['sms_event','send_sms']`, :271 `tools: ['notification_event','speak_tts']`, plus :288, :294, :300. A repo-wide grep for `event_forwarding|sms_event|notification_event` (excluding node_modules/.git/.output and docs/app) returns zero hits — the CLI/feature is spelled `event-forwarding` with a hyphen (mcp/bin/aster.ts:408). None of the three appear among the 49 `aster_*` definitions in mcp/src/mcp/tools.ts. The template renders `card.tools` at UseCasesSection.vue:63-72 in one monospace pill class with no distinction between tool and event.
```

**Why it matters:** An extractor reading these cards has no signal separating 'MCP tool you can call' from 'event type that fires', so `sms_event` gets reported as part of Aster's tool surface — and the page's tool list stops being verifiable against the repo.

**Fix:** Drop the three from the `tools` arrays, or move them to a separate `events: []` array rendered with an 'event' label.

#### `MEDIUM` Hard-coded latencies and results are wrapped in 'real', 'actual' and 'live' framing

**Where:** `docs/app/components/LiveChatSection.vue`:14

**Evidence:**

```
LiveChatSection.vue:14 — 'Watch a real conversation unfold. Every message triggers actual tools on the Android device' sits above a fully hard-coded `script` array (206-220). HowItWorks.vue:299-300 `result: 'Screenshot captured (1080x2400, 842KB)…', latency: '~320ms'`, with :320 `'~90ms'` and :310 `'~480ms'`; HowItWorks.vue:51-55 renders a pulsing green dot plus `<span …>live</span>` and :15 labels the panel 'Command Trace'. HeroSection.vue:79 'Aster · Live' with its own animate-ping dot at :80-83; ProactiveSection.vue:96-100 'Live Event Stream' with the same pulse.
```

**Why it matters:** '~320ms', '~90ms', 'Freed up 5.5 GB', 'Found 47 duplicate sets' are the number-shaped strings an answer engine lifts as benchmarks. The 'real'/'actual'/'live' framing turns illustrative copy into an asserted measurement; one check against a device run discredits every other specific on the page.

**Fix:** Relabel the panels as illustrative ('Example trace'), drop the 'live' pulse from synthetic panels, and either remove the latency strings or replace them with measured ranges.

#### `MEDIUM` Every content block in EmbraceSection and ProactiveSection ships at opacity:0 and is revealed only by JS

**Where:** `docs/app/components/EmbraceSection.vue`:331

**Evidence:**

```
EmbraceSection.vue:331-336 `.observe-fade { opacity: 0; transform: translateY(24px); transition: … }` with `.observe-fade.is-visible { opacity: 1 }` at 338-341; `is-visible` is added only inside the IntersectionObserver callback registered in `onMounted` (307-327). Identical block in ProactiveSection.vue:399-404 / 406-409 with its observer at 375-395. `grep -rn observe-fade app/components/` shows the class on every content wrapper of both sections: EmbraceSection.vue:11, 28, 59, 96, 108, 218 and ProactiveSection.vue:11, 25, 131, 196.
```

**Why it matters:** With JS disabled or not executed, both sections render as blank space for a human and for any renderer-driven pipeline. (The original claim that Readability-class extractors discount zero-opacity subtrees is overstated — those extractors key on `display:none`/`visibility:hidden`/the `hidden` attribute, not computed opacity — so the harm is the no-JS blank render, not an SEO penalty.)

**Fix:** Invert the default: ship visible and apply the fade only once JS marks the document ready (`.js-ready .observe-fade { opacity: 0 }`), or gate the whole effect behind `@media (prefers-reduced-motion: no-preference)` over a visible baseline.

#### `MEDIUM` 7 of 8 hero conversations, the second h1 variant, and 5 of 6 command-trace bodies never reach the static HTML

**Where:** `docs/app/components/HeroSection.vue`:26

**Evidence:**

```
HeroSection.vue:27-38 — `<Transition name="headline" mode="out-in">` with `<div v-if="headlineIndex === 0" key="copilot">…AI CoPilot / for your Mobile</div>` and `<div v-else key="give">…Give your AI / its own phone</div>`; `headlineIndex = ref(0)` at :229, so only the first variant is in the server-rendered h1. The hero carousel body is keyed `<div :key="currentIndex">` at :90 with `currentIndex = ref(0)` at :234, so `conversations[1..7]` (array at 169-226) are absent. HowItWorks.vue:357 `const activeScenario = computed(() => scenarios[current.value]!)` with `current = ref(0)` at :356 leaves scenarios[1..5] (291-352) out of the trace panel — note the scenario *labels* do prerender via the `v-for` pill list at HowItWorks.vue:26-37, so the loss is the aiCall/serverRoute/deviceExec/result bodies only. Additionally the four log rows at HowItWorks.vue:195-257 carry `opacity-0` while `phase` is -1 (:358), so even scenario 0's trace is invisible pre-JS.
```

**Why it matters:** 'Give your AI its own phone' is the page's most distinctive positioning and never appears in the prerendered h1, and the densest concrete technical detail on the page (`AccessibilityService.takeScreenshot()`, `MediaStore.query()`, `Intent(ACTION_CALL) → speakerphone → TTS.speak()`) is JS-only.

**Fix:** Render all variants into the DOM and toggle visibility with CSS/`hidden` rather than `v-if`/keyed swap.

#### `MEDIUM` The same third-party client is spelled ClawdBot in three sections and ClawBot in another

**Where:** `docs/app/components/IntegrationsSection.vue`:12

**Evidence:**

```
IntegrationsSection.vue:12 'Give Claude, OpenClaw, MoltBot, or ClawBot a CoPilot for your phone'; IntegrationsSection.vue:130 `name: 'ClawBot'` with :134 'Compatible via MCP or ClawHub skill' (the original finding cited line 131 — the `name:` key is on 130); IntegrationsSection.vue:59 'for OpenClaw, MoltBot, and ClawBot'. Against that: HeroSection.vue:149-154 `clawdbot: { name: 'ClawdBot', … }`, ProactiveSection.vue:153 'Works out of the box with OpenClaw, ClawdBot & MoltBot', LiveChatSection.vue:52 'ClawdBot'. docs/public/llms.txt:13 writes 'OpenClaw / Moltbot / Clawbot' — a third casing.
```

**Why it matters:** ProactiveSection's supported-client list (OpenClaw/ClawdBot/MoltBot) and IntegrationsSection's (Claude/OpenClaw/MoltBot/ClawBot) do not agree, so 'what clients does Aster support?' extracts a garbled set with one name that may not exist.

**Fix:** Settle the upstream spellings, define the supported-client list once, and reuse it in ProactiveSection, IntegrationsSection, HeroSection, LiveChatSection and llms.txt.

#### `MEDIUM` The proactive section teaches only the alias command and never says forwarding is off by default

**Where:** `docs/app/components/ProactiveSection.vue`:174

**Evidence:**

```
ProactiveSection.vue:174 `<span class="text-text-primary">aster set-openclaw-callbacks</span>` — and `grep -rn "set-event-forwarding" docs/app/` returns nothing, so the alias is the page's only command for this feature. In the CLI the alias is secondary: mcp/bin/aster.ts:408 `.command('set-event-forwarding')` followed by :409 `.alias('set-openclaw-callbacks')`; aster.ts:292 prints 'Or use the CLI: aster set-event-forwarding'. README.md:513 states the webhook is 'off unless you run `aster set-event-forwarding`' and POSTs 'to an endpoint you specify … never a vendor server'. `grep -rni "off by default|opt-in" docs/app/components/` returns zero hits.
```

**Why it matters:** An assistant citing the page teaches the secondary, OpenClaw-branded name to users of every client; and the page omits the off-by-default, user-specified-endpoint fact that is the strongest defensible privacy claim the product actually has — the one that should replace the overbroad SecuritySection h2.

**Fix:** Show `aster set-event-forwarding` as primary with the alias mentioned secondarily, and add 'off until you enable it; posts only to the endpoint you configure.'

#### `LOW` The Web Dashboard panel sits behind a v-else and is absent from the static HTML

**Where:** `docs/app/components/ScreenshotsSection.vue`:74

**Evidence:**

```
ScreenshotsSection.vue:52 `<div v-if="activeTab === 'app'" key="app">` and :74 `<div v-else key="dashboard">`, with `const activeTab = ref('app')` at :99. The dashboard panel's only text content is the twelve `label` values in `dashboardScreenshots` (123-136: Overview, Device Registry, Device Telemetry, Screen Control, Messages, Apps Inventory, Storage & Media, Logs, Connect, File Browser, MCP Tool Explorer, Event Forwarding), each rendered as an `:alt` and a caption span at :82-88.
```

**Why it matters:** Those twelve labels are the page's only enumeration of what the web dashboard does, so 'what can the Aster dashboard do?' has no extractable answer in the delivered HTML.

**Fix:** Render both tab panels and toggle with `hidden`/CSS instead of `v-if`/`v-else`.

#### `LOW` The prerendered h1 does not contain the product name

**Where:** `docs/app/components/HeroSection.vue`:25

**Evidence:**

```
HeroSection.vue:25-40 — the only `<h1>` on the page wraps the headline slider; at `headlineIndex = 0` (ref at :229) it renders `<span>AI CoPilot</span><br /><span>for your Mobile</span>` (lines 29-31). `grep -rn '<h1' app/` finds no other h1. 'Aster' does appear in the immediately following subhead (:44), in NavBar.vue:9, and in the `<title>`/og:title at nuxt.config.ts:24/37.
```

**Why it matters:** The single h1 on a one-page site is the strongest place to bind the entity to the category claim, and it currently reads as a generic phrase with no brand token. Severity is low, not medium: the brand is one line below and in the title tag.

**Fix:** Anchor the brand in the static variant ('Aster — the AI CoPilot for your Android') and let the rotation carry the second slogan.

#### `LOW` Person-vs-company attribution is split across surfaces and the JSON-LD carries no publisher Organization

**Where:** `docs/nuxt.config.ts`:68

**Evidence:**

```
nuxt.config.ts:68 `author: { '@type': 'Person', name: 'Satyajit Pradhan', url: 'https://github.com/satyajiit' }` — the SoftwareApplication block (59-72) has no `publisher`, no `sameAs`. AuthorSection.vue:13 'Aster is built by Satyajit Pradhan.' FooterSection.vue:67 'MIT License · A <a href="https://matterwardlabs.com">Matterward Labs</a> project'. docs/public/llms.txt:5 'Built by Matterward Labs; part of the OpenAlly platform.'
```

**Why it matters:** Not a contradiction — a person building at a company is coherent — but the company that owns the domain (aster.matterwardlabs.com) appears only in footer fine print and in llms.txt, and is entirely absent from the structured data, so an entity record built from this page attaches to the individual only. Severity corrected from medium to low for that reason.

**Fix:** Add `publisher: { '@type': 'Organization', name: 'Matterward Labs', url: 'https://matterwardlabs.com' }` and `sameAs: [github, npm]` to the JSON-LD, and say it once in body copy.

#### `LOW` The price exists only in JSON-LD; no visible sentence says Aster is free

**Where:** `docs/nuxt.config.ts`:67

**Evidence:**

```
nuxt.config.ts:67 `offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' }`. `grep -rni "free|pricing|price|\$0" docs/app/components/` returns only unrelated hits — 'free up space' (HeroSection.vue:171), 'Are you free?' (UseCasesSection.vue:263), 'GB free' (LiveChatSection.vue:210/216), 'hands-free' (LiveChatSection.vue:250). The nearest visible statements are HeroSection.vue:21 'Open Source · MIT Licensed' and SecuritySection.vue:139 '100% Open Source'.
```

**Why it matters:** MIT-licensed products routinely carry paid tiers, so the implication is not a quotable answer to 'how much does Aster cost?'. Severity corrected to low — the Offer in structured data plus the MIT framing makes this a small gap, not a misleading one.

**Fix:** One visible sentence — 'Aster is free and MIT-licensed: no accounts, no paid tier, no usage limits' — with the Offer kept as corroboration.

#### `LOW` Android is never stated as an exclusive boundary, so 'does it work on iPhone?' has no citable answer

**Where:** `docs/app/components/HeroSection.vue`:44

**Evidence:**

```
HeroSection.vue:44 'Aster connects Android to AI assistants like Claude, OpenClaw, or MoltBot via Model Context Protocol.' `grep -rni "ios|iphone|android.only|android-only" docs/app/components/` returns only false positives on the substring 'ios' inside the word `scenarios` (ProactiveSection.vue:194/281, HowItWorks.vue:27/291/357). nuxt.config.ts:66 sets `operatingSystem: 'Android'` in structured data, but no body sentence states exclusivity.
```

**Why it matters:** The boundary is inferable but never asserted, so an assistant answering 'can I use Aster with my iPhone?' has nothing to quote. Severity corrected to low — 'Android' appears constantly and the structured data carries the OS.

**Fix:** State it once near the requirements: 'Aster is Android-only — the companion app uses the Android Accessibility Service; there is no iOS build.'

#### `LOW` The no-PC operating modes appear in two cards while setup, architecture and security all present a PC server as required

**Where:** `docs/app/components/FeaturesGrid.vue`:116

**Evidence:**

```
FeaturesGrid.vue:114-118 — one card titled 'Three Connection Modes', description 'Remote WebSocket server, on-device MCP server (Ktor), or zero-network Binder IPC for same-device agents like OpenAlly.' IntegrationsSection.vue:40-42 — 'OpenAlly — zero-setup, fully on-device … drives Aster directly over Android Binder IPC: no server, no network, the full 49-tool surface locally.' Against that: SetupSteps.vue:49-58 makes step 1 'Install & start the MCP server' with `npm install -g aster-mcp`; HowItWorks.vue:99-100 and :171-172 render 'Aster Server — Node.js · WS Bridge' as the mandatory middle node of the three-node flow; SecuritySection.vue:56 'The MCP server runs on your machine'.
```

**Why it matters:** 'Can I use Aster without a computer?' is a top install-blocker and the answer is yes, but two short cards are outweighed by the setup steps, the architecture diagram and the security copy. Severity corrected to low — the page does state it, twice, in named cards.

**Fix:** Give the three connection modes an h3 each with one sentence of when to use which, and cross-reference from step 1 ('No PC? Use the on-device MCP server instead').

#### `LOW` Four sections carry no id, and two that do are unreachable from the nav

**Where:** `docs/app/components/LiveChatSection.vue`:2

**Evidence:**

```
`grep -rn 'id="' docs/app/components/*.vue` returns ids on only nine sections: features, embrace, how-it-works, integrations, proactive, use-cases, setup, screenshots, tools. No id on LiveChatSection.vue:2 `<section class="relative py-32 px-6 overflow-hidden">`, SecuritySection.vue:2 (same class string), AuthorSection.vue:2 `<section class="relative py-24 px-6">`, or HeroSection.vue:2. NavBar.vue:13-19 links only #features, #use-cases, #proactive, #how-it-works, #setup, #integrations, #tools — so `#embrace` (EmbraceSection.vue:2) and `#screenshots` (ScreenshotsSection.vue:2) exist but nothing links to them.
```

**Why it matters:** On a one-page site fragments are the only addressable sub-units, and the Security & Privacy section — the most likely citation target for 'Aster's security model' — has no anchor at all.

**Fix:** Add `id="security"`, `id="demo"`, `id="author"`, and surface #security (and optionally #embrace, #screenshots) in NavBar.

#### `LOW` The page has no <main> landmark and no <article> wrapper

**Where:** `docs/app/app.vue`:2

**Evidence:**

```
app.vue:1-18 is the entire root: `<template><div class="noise"><NavBar /><HeroSection /> … <FooterSection /></div></template>`. There is no `app/layouts/` directory (`ls docs/app/` → assets, components, app.vue), so nothing wraps this, and `grep -rn "<main|<article" docs/app/` returns zero matches.
```

**Why it matters:** Boilerplate-stripping extractors use `<main>`/`<article>` to separate primary content from nav and footer; with everything in one undifferentiated div the nav links, the footer link farm (FooterSection.vue:15-102) and the decorative background divs compete with body copy for inclusion.

**Fix:** Wrap the fifteen sections in `<main>` in app.vue, leaving NavBar and FooterSection outside it.

### tech-seo

#### `HIGH` The on-page skill install URL points at a repository that does not exist and returns HTTP 404

**Where:** `docs/app/components/IntegrationsSection.vue`:89

**Evidence:**

```
docs/app/components/IntegrationsSection.vue:89, inside a `<pre class="terminal"><code>` block labelled `# Raw skill link:` — `<span class="text-text-secondary break-all">https://raw.githubusercontent.com/satyajiit/Aster/main/skill/SKILL.md</span>`. I verified this over the network: `curl -o /dev/null -w %{http_code}` returns **404** for that URL and **200** for `https://raw.githubusercontent.com/satyajiit/aster-mcp/main/skill/SKILL.md`. `git remote -v` reports `origin https://github.com/satyajiit/aster-mcp.git`, `ls skill/` shows SKILL.md exists at that path, and a grep for `satyajiit/Aster` across docs/, packages/, skill/ and README.md returns exactly this one line — every other GitHub URL on the site uses `satyajiit/aster-mcp` (NavBar.vue:23, HeroSection.vue:59, SecuritySection.vue:144, SetupSteps.vue:27, FooterSection.vue:17/26/35, docs/public/llms.txt:10/12).
```

**Why it matters:** It is presented as a copy-paste install command and it is dead — confirmed 404, not inferred. It sits in the prerendered HTML, so answer engines quoting the install instructions will repeat a URL that does not resolve, and the site publishes two apparent repository identities for one product.

**Fix:** Change line 89 to `https://raw.githubusercontent.com/satyajiit/aster-mcp/main/skill/SKILL.md` — verified reachable (HTTP 200).

#### `MEDIUM` /200.html ships as a crawlable HTTP-200 zero-content page carrying the homepage's full head and robots: index, follow

**Where:** `docs/.output/public/200.html`:1

**Evidence:**

```
I read docs/.output/public/200.html (3,793 bytes). Its entire body is `<body><div id="__nuxt"></div><div id="teleports"></div>` plus the Nuxt bootstrap scripts, with `data-ssr="false"` on the payload script — no content whatsoever. Its head is byte-identical to 404.html's, which I also read in full: `<title>Aster — Your AI CoPilot on Mobile or Give Your AI Its Own Phone</title>`, `<meta name="robots" content="index, follow">`, `<link rel="canonical" href="https://aster.matterwardlabs.com">`, the complete og:/twitter: set and the SoftwareApplication JSON-LD. docs/public/robots.txt is 4 lines with no Disallow (`User-Agent: *` / `Allow: /` / blank / `Sitemap: ...`) and docs/public/sitemap.xml lists only the root. .github/workflows/deploy-docs.yml:44-46 uploads all of docs/.output/public, so 200.html is deployed and GitHub Pages serves it with status 200 (unlike 404.html, which Pages serves with status 404).
```

**Why it matters:** A live 200-status URL on the host with a duplicate <title>, duplicate og: tags, duplicate JSON-LD and zero body text is a thin/soft-404 page. It is unlinked and absent from the sitemap, which is what keeps the risk moderate rather than acute, but nothing in the config prevents it being crawled if discovered, and the only thing that would stop it being treated as a homepage duplicate is the canonical it inherits.

**Fix:** Add `Disallow: /200.html` to docs/public/robots.txt (404.html is already protected by its 404 status), or drop the SPA fallback from the generate output if it is not needed on GitHub Pages.

#### `MEDIUM` JSON-LD declares operatingSystem 'Android' on the same node whose downloadUrl is an npm package that does not run on Android

**Where:** `docs/nuxt.config.ts`:66

**Evidence:**

```
docs/nuxt.config.ts:66 — `operatingSystem: 'Android',` and docs/nuxt.config.ts:70 — `downloadUrl: 'https://www.npmjs.com/package/aster-mcp',` sit in the same SoftwareApplication node. The product shape is stated plainly in docs/public/llms.txt:5 — "Aster is two pieces: a self-hosted Node.js MCP server (npm: `aster-mcp`) and an Android companion app (no root required, Android 7+)" — and llms.txt:48 — "Requirements: Node.js >= 20 (server), Android 7.0+ (app)". mcp/package.json confirms `"engines": {"node": ">=20"}` with no platform restriction. The APK is distributed from GitHub Releases (docs/app/components/SetupSteps.vue:27, docs/public/llms.txt:12), not from npm.
```

**Why it matters:** The machine-readable claim ('this Android application is downloaded from npm') contradicts the human-readable install instructions on the same page, and it understates reach by asserting the server half is Android-only when it runs on macOS, Linux and Windows. Kept at medium because this is a factual error in the only structured-data block on the site, not a missing-property nicety.

**Fix:** Split docs/nuxt.config.ts:59-72 into an @graph with two SoftwareApplication nodes — the npm server (operatingSystem macOS/Linux/Windows, softwareRequirements Node.js >= 20, downloadUrl npm) and the Android companion (operatingSystem Android, softwareRequirements Android 7.0+, downloadUrl the Releases page).

#### `LOW` The GitHub Pages 404 document is an unrendered SPA shell with no 'not found' content and an index, follow robots meta

**Where:** `docs/app/app.vue`:1

**Evidence:**

```
`find docs/app -type f` returns 19 files: app.vue, main.css and 17 components — there is no docs/app/error.vue and no docs/app/pages/ directory. docs/app/app.vue:1-19 renders 15 sections unconditionally (`<NavBar /> <HeroSection /> ... <FooterSection />`) with no route awareness. The 404 route is forced by the preset, which I read at docs/node_modules/.pnpm/nitropack@2.13.1_supports-color@10.2.2/node_modules/nitropack/dist/presets/_static/preset.mjs — the `githubPages` preset sets `prerender: { routes: ['/', '/404.html'] }`. The resulting docs/.output/public/404.html is 3.8 KB with body `<div id="__nuxt"></div><div id="teleports"></div>`, `data-ssr="false"`, and head carrying `<meta name="robots" content="index, follow">`.
```

**Why it matters:** Any mistyped or stale deep link returns a blank white page that JavaScript then repaints as the full landing page, so a visitor cannot tell they hit a dead URL and there is no branded not-found affordance. Downgraded from medium because the HTTP status is correctly 404, so Google will not index it regardless of the robots meta — this is a UX and hygiene defect, not an indexing one. Note the empty body is Nuxt's deliberate SPA-fallback behaviour (`data-ssr="false"`), so an error.vue alone may not make it server-rendered; verify the generated file after any fix.

**Fix:** Add docs/app/error.vue with real not-found content and a link to `/`, set `useHead({ meta: [{ name: 'robots', content: 'noindex' }] })` there, and re-run generate to confirm the prerendered 404.html actually contains the markup.

#### `LOW` The deploy workflow's paths filter does not cover packages/aster-ui, whose tokens.css is imported directly into the docs stylesheet

**Where:** `.github/workflows/deploy-docs.yml`:6

**Evidence:**

```
.github/workflows/deploy-docs.yml:6 — `paths: [docs/**, .github/workflows/deploy-docs.yml]`, and docs/nuxt.config.ts:10 — `extends: ['../packages/aster-ui']`. I then tested the claimed coupling and it is mostly absent: `grep -rnoE "<A[A-Z][A-Za-z]+" docs/app` returns ZERO matches and a kebab-case search for `<a-card|<a-button|...` also returns nothing, so NO aster-ui component (ACard, AButton, ABadge, ACodeBlock, ASectionLabel, AsterMark, BrandLockup, …) is rendered anywhere on the docs site. The one real cross-boundary dependency is docs/app/assets/css/main.css line 6 — `@import "../../../../packages/aster-ui/app/assets/css/tokens.css";` plus `@source "../../../../packages/aster-ui/app/components";` — so a token change does alter the built CSS with no workflow trigger.
```

**Why it matters:** Severity corrected from high to low, and the stated harm is corrected: a packages/aster-ui change cannot silently alter the crawlable HTML of aster.matterwardlabs.com, because the docs site renders none of that layer's components. What it can do is leave the deployed CSS (colours, surfaces, borders, fonts) behind main with no failure signal, since the workflow is simply never queued. That is a build-freshness defect, not an SEO one.

**Fix:** Add `packages/aster-ui/**` to the `paths:` list in .github/workflows/deploy-docs.yml; checkout@v4 already has the layer on disk.

#### `LOW` The keywords meta tag is inert for search engines and publishes five third-party product brands in the head

**Where:** `docs/nuxt.config.ts`:29

**Evidence:**

```
docs/nuxt.config.ts:29 — `{ name: 'keywords', content: 'aster, android ai copilot, mcp server, model context protocol, ai phone, ai assistant android, claude android, openclaw, clawdbot, moltbot, clawbot, clawhub, ai automation, ai copilot mobile, give ai a phone, ai own phone, natural language android, ai calls you, ai own device' }`. Measured: 276 characters, 19 comma-separated terms. Verified emitted verbatim in docs/.output/public/404.html (and index.html/200.html) as `<meta name="keywords" content="...">`.
```

**Why it matters:** Downgraded from medium to low. The tag does nothing for Google, so removing it costs nothing — but the finding's secondary argument is weaker than stated: the same brands (OpenClaw, MoltBot, Clawbot, ClawHub) appear deliberately in og:description at docs/nuxt.config.ts:38 and in docs/public/llms.txt:13 and :21 as genuine supported-client facts, so their presence is an editorial choice, not accidental stuffing. The residual defect is simply dead markup in the head.

**Fix:** Delete the `keywords` entry from the meta array in docs/nuxt.config.ts.

#### `LOW` The meta description is 201 characters and its differentiators sit past the usual truncation point

**Where:** `docs/nuxt.config.ts`:28

**Evidence:**

```
docs/nuxt.config.ts:28 — I measured the literal string at **201 characters**. The closing clause `49 tools, open source, self-hosted.` begins at character index 166. For comparison I also measured docs/nuxt.config.ts:38 og:description at 211 characters and docs/nuxt.config.ts:46 twitter:description at 137 characters.
```

**Why it matters:** Downgraded from medium to low: Google frequently rewrites descriptions from page content regardless of length, so this is a snippet-quality nudge rather than a defect. The three strongest proof points (tool count, open source, self-hosted) are nonetheless positioned last, where they are least likely to survive any truncation.

**Fix:** Front-load the differentiators and shorten the `description` at docs/nuxt.config.ts:28; leave og:description alone.

#### `LOW` og:image declares no width, height, alt or type, and twitter:image has no alt

**Where:** `docs/nuxt.config.ts`:39

**Evidence:**

```
docs/nuxt.config.ts:39 is the only og:image declaration — `{ property: 'og:image', content: 'https://aster.matterwardlabs.com/og-card.png' }`. Reading the full meta array (lines 26-48) confirms there is no `og:image:width`, `og:image:height`, `og:image:alt`, `og:image:type` or `og:image:secure_url`, and no `twitter:image:alt` next to line 47. I checked the asset: `file docs/public/og-card.png` → `PNG image data, 1200 x 630, 8-bit/color RGB, non-interlaced`, and it is present in docs/.output/public/og-card.png (118 KB).
```

**Why it matters:** Scrapers that do not pre-fetch and measure the bitmap can render a smaller fallback card or defer the image on first unfurl. Every missing value is a known constant here, so the fix is free. Low rather than medium: this affects share-card rendering, not crawling or indexing.

**Fix:** Add og:image:width 1200, og:image:height 630, og:image:type image/png, og:image:alt and twitter:image:alt to the meta array in docs/nuxt.config.ts.

#### `LOW` sitemap.xml is a hand-maintained static asset whose lastmod already trails the content, with no build step to refresh it

**Where:** `docs/public/sitemap.xml`:5

**Evidence:**

```
docs/public/sitemap.xml:5 — `<lastmod>2026-09-01</lastmod>`. I ran `git log -1 --date=short -- docs/app` → `2026-09-02 Align the docs site and READMEs with the rebuilt dashboard`, and `git log -1 --date=short -- docs` → `2026-09-06 Device execution protocol v2 over the Binder lane`. The file lives in docs/public/ and is copied verbatim (it appears unchanged at docs/.output/public/sitemap.xml, 274 bytes). .github/workflows/deploy-docs.yml:38-42 runs only `pnpm nuxt generate` with no sitemap step, and docs/package.json:5-11 lists only build/dev/generate/preview/postinstall with no @nuxtjs/sitemap in dependencies (lines 12-20).
```

**Why it matters:** Downgraded from medium to low. The structural point is right — the date can only ever be correct by hand and already is not — but the impact claim was overstated: this is a one-URL sitemap for a single-page site whose homepage Google will crawl regardless, so a stale lastmod here costs very little. Worth fixing because it will silently widen with every commit.

**Fix:** Stamp `<lastmod>` at build time (a small prerender hook, or a CI step in deploy-docs.yml that rewrites it from the commit timestamp before upload-pages-artifact).

#### `LOW` The SoftwareApplication JSON-LD carries no publisher, sameAs, featureList, softwareVersion or image

**Where:** `docs/nuxt.config.ts`:59

**Evidence:**

```
docs/nuxt.config.ts:59-72 is the whole structured-data surface. I read it in full: present are @context, @type, name, description, url, applicationCategory ('DeveloperApplication'), operatingSystem, offers, author, license, downloadUrl, screenshot. Absent: softwareVersion, datePublished, dateModified, featureList, installUrl, softwareRequirements, image, publisher, sameAs. Every missing value exists in-repo: mcp/package.json gives `"version": "0.1.16"` and `"engines": {"node": ">=20"}`; docs/public/llms.txt:23-39 enumerates the 49 tools across 14 categories and line 48 states `Node.js >= 20 (server), Android 7.0+ (app)`; the GitHub/npm/ClawHub URLs are at FooterSection.vue:17, :79 and :87; the publisher appears at FooterSection.vue:67 (`A <a href="https://matterwardlabs.com">Matterward Labs</a> project`). The finding's note that `license` is valid here is correct — SoftwareApplication inherits it from CreativeWork.
```

**Why it matters:** Downgraded from medium to low: the block already satisfies Google's minimum for SoftwareApplication (name plus offers), so no rich result is being lost. The real cost is entity clarity — nothing machine-readable ties this page to Matterward Labs, the GitHub repo or the npm package.

**Fix:** Extend the block at docs/nuxt.config.ts:59-72 with softwareVersion, softwareRequirements, featureList, installUrl, image and a publisher Organization with sameAs. Do not add aggregateRating or review — there are no real ratings.

#### `LOW` The Offer node declares only price and priceCurrency

**Where:** `docs/nuxt.config.ts`:67

**Evidence:**

```
docs/nuxt.config.ts:67 — `offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD' },`. No `availability`, no `url`, no `category`. The shape that is there is valid: price as a string and an ISO 4217 priceCurrency is the correct form, and a free product legitimately uses '0'.
```

**Why it matters:** Minor. `offers` is the node carrying the 'this is free' claim and it is the property Google reads for software pricing, so filling in availability and url is cheap completeness. No rich result is gated on it.

**Fix:** Add `availability: 'https://schema.org/InStock'` and a `url` to the Offer at docs/nuxt.config.ts:67.

#### `LOW` No WebSite or Organization node exists — Matterward Labs has no machine-readable entity representation

**Where:** `docs/nuxt.config.ts`:56

**Evidence:**

```
docs/nuxt.config.ts:56-74 declares exactly one `application/ld+json` script and it is the SoftwareApplication. I verified against the artifact: a regex count over docs/.output/public/index.html returns `ldjson blocks 1`. There is no WebSite node, no Organization node and no `sameAs` anywhere in the config. The publisher appears only as body copy at docs/app/components/FooterSection.vue:67 with an unmarked link to https://matterwardlabs.com, and the npm and ClawHub links at FooterSection.vue:79 and :87 are likewise unmarked.
```

**Why it matters:** Downgraded from medium to low, consistent with the finding's own admission that the sitelinks searchbox is deprecated and no rich result is on offer. The value is entity disambiguation only — 'Aster' is a heavily overloaded token and nothing structured connects this page to Matterward Labs, github.com/satyajiit/aster-mcp or the npm package.

**Fix:** Add an Organization node for Matterward Labs (url, logo, sameAs for GitHub/npm/site) and point SoftwareApplication.publisher at it by @id, in docs/nuxt.config.ts.

#### `LOW` A local `nuxt generate` without GITHUB_PAGES=true produces a different artifact: no 404.html and no .nojekyll

**Where:** `docs/nuxt.config.ts`:79

**Evidence:**

```
docs/nuxt.config.ts:79 — `preset: isGitHubPages ? 'github-pages' : undefined,` with docs/nuxt.config.ts:3 `const isGitHubPages = process.env.GITHUB_PAGES === 'true';`. The only place that env is set is .github/workflows/deploy-docs.yml:41-42. I read the vendored preset at docs/node_modules/.pnpm/nitropack@2.13.1_supports-color@10.2.2/node_modules/nitropack/dist/presets/_static/preset.mjs and confirmed the difference is real: the `githubPages` preset adds `prerender: { routes: ['/', '/404.html'] }` and a `compiled` hook that does `fsp.writeFile(join(nitro.options.output.publicDir, '.nojekyll'), '')`; the base `static` preset it falls back to has neither. docs/package.json:8 exposes `"generate": "nuxt generate"` with no env, and docs/.output/nitro.json advertises `"deploy": "npx gh-pages --dotfiles -d public"`.
```

**Why it matters:** Two divergent outputs from the same command, separated only by an env var no local script sets. A hand-deploy via the gh-pages path nitro itself suggests would ship a site with no 404 document at all. The missing .nojekyll is harmless through actions/deploy-pages but would strip `_nuxt/` and `_fonts/` on a classic branch-based Pages deploy.

**Fix:** Set `preset: 'github-pages'` unconditionally in docs/nuxt.config.ts, or bake `GITHUB_PAGES=true` into the `generate` script in docs/package.json.

#### `LOW` 30 of the 36 deployed screenshot assets never appear in the prerendered HTML and are in no image sitemap

**Where:** `docs/app/components/ScreenshotsSection.vue`:74

**Evidence:**

```
docs/app/components/ScreenshotsSection.vue:74 — `<div v-else key="dashboard">` (the original finding cited line 76; the v-else is at 74). Both galleries are behind client state: line 52 `v-if="activeTab === 'app'"` against `activeTab = ref('app')` (line 99), and the light variants are computed inline at line 61 `shot.src.replace('/app/', '/app/light/')` and line 82 `shot.src.replace('/dashboard/', '/dashboard/light/')` against `shotTheme = ref('dark')` (line 100). I counted the artifact: docs/.output/public/index.html contains 10 `<img>` tags resolving to 8 unique paths — logo.png (x2), openally-mark.svg (x2) and the 6 dark `/screenshots/app/*.jpg`. `find docs/public/screenshots -type f | wc -l` returns 36 (6 app dark, 6 app light, 12 dashboard dark, 12 dashboard light), all copied into .output/public. None of the 30 non-default variants appear in the HTML, and docs/public/sitemap.xml has no image entries.
```

**Why it matters:** Thirty real product screenshots ship in the bundle but are unreachable to any crawler that does not click the tab and theme toggles, so they cannot surface in image search or be cited as visual evidence. The 6 that do render are correct and all carry alt text, which is why this is low.

**Fix:** Render both galleries in the DOM and toggle visibility with CSS, or add an image sitemap covering the dashboard and light-variant paths.

#### `LOW` No <img> on the page declares width or height, so every image is a layout-shift source

**Where:** `docs/app/components/ScreenshotsSection.vue`:60

**Evidence:**

```
I parsed docs/.output/public/index.html: 10 `<img>` tags, and `width attr count 0` — not one declares width or height. `srcset` occurs 0 times and `<picture` 0 times. Source sites verified by reading each file: ScreenshotsSection.vue:60-65 and :81-86 (`class="w-full h-auto block ..." loading="lazy"`, sized only by Tailwind, with the app cards in a fixed `style="width: 180px;"` wrapper at line 58 whose height is unknown until load), NavBar.vue:8 (`class="w-8 h-8 rounded-lg ..."`), FooterSection.vue:7 and :58, IntegrationsSection.vue:38 (`class="w-10 h-10 flex-shrink-0"`). The screenshots are correctly `loading="lazy"` and the LCP element is hero text, so this is CLS-only.
```

**Why it matters:** Without intrinsic dimensions the browser cannot reserve space before the bitmap arrives, so images pop in and shift content — the CLS half of Core Web Vitals. The screenshot row is the worst case because six images load together. Low because the Tailwind utilities already constrain one axis on the logo marks, limiting real-world shift to the screenshot gallery.

**Fix:** Add explicit width/height attributes (or an aspect-ratio on the wrapper) to the img tags in ScreenshotsSection.vue, NavBar.vue, FooterSection.vue and IntegrationsSection.vue; the Tailwind classes still control rendered size.

### llm-readability

#### `HIGH` All 49 tool names in llms.txt (and README + ToolsShowcase) are published without the aster_ wire prefix

**Where:** `docs/public/llms.txt`:25

**Evidence:**

```
I extracted the 49 names from docs/public/llms.txt:25-39 (e.g. line 25 `- Screen: take_screenshot, get_screen_hierarchy, find_element`) and the 49 from mcp/src/mcp/tools.ts (`grep -o "name: 'aster_[a-z_]*'"`) and diffed the two sorted sets after stripping the prefix: `llms count: 49  tools count: 49` and `diff` returned nothing — IDENTICAL modulo the prefix, zero match as written. mcp/src/mcp/tools.ts:460 `name: 'aster_take_screenshot',`; :471 `name: 'aster_get_screen_hierarchy',`; :711 `name: 'aster_find_element',`. mcp/src/mcp/handler.ts:100 `case 'aster_list_devices':` and the fall-through at the end of the switch: `default:` / `return errorResult(`Unknown tool: ${name}`);`. Same stripping repeats at README.md:438 (`| **Screen** | `take_screenshot`, `get_screen_hierarchy`, `find_element` |`) and docs/app/components/ToolsShowcase.vue:52 `'take_screenshot',`.
```

**Why it matters:** llms.txt is the artifact an answer engine quotes when asked what tools Aster exposes and how to call them. A model answering from the document rather than from a live tools/list gets 49 names that all fail with `Unknown tool`. Severity corrected from critical to high: a connected MCP client still receives the authoritative prefixed list from the server at handshake, so the failure is confined to doc-sourced answers rather than being total for every caller.

**Fix:** Publish the wire names (`aster_take_screenshot`, …) and state the prefix rule once in the section header; generate the section from TOOL_DEFINITIONS in mcp/src/mcp/tools.ts and regenerate README.md:438 and ToolsShowcase.vue from the same source.

#### `HIGH` llms.txt claims Android 7.0+; the APK's minSdk is 26 (Android 8.0)

**Where:** `docs/public/llms.txt`:48

**Evidence:**

```
docs/public/llms.txt:48 `- Requirements: Node.js >= 20 (server), Android 7.0+ (app)` and :5 `an Android companion app (no root required, Android 7+)`. Ground truth read directly: apps/android/app/build.gradle.kts:16 `minSdk = 26` (Android 8.0 Oreo; Android 7.0 is API 24), with :141 confirming `(ML Kit minSdk 21; project is 26)`. README.md contradicts itself in-file: :644 `#   minSdk 26 (Android 8), compileSdk 36, arm64-v8a, com.aster` versus :655 `- **Android**: Android 7.0+ with Accessibility Service enabled` and the badge at :24 `Android_7%2B`. I did not verify the merged-manifest path the original evidence cited; build.gradle.kts:16 is sufficient and authoritative.
```

**Why it matters:** The same line carries a correct fact (Node >= 20 matches mcp/package.json engines), so it reads authoritative and will be restated without hedging. An Android 7.x user is told their device is supported and hits a hard package-manager install refusal after download. Severity corrected from critical to high: it is a documentation error with a post-download failure, not a shipping defect.

**Fix:** Set llms.txt:5 and :48 to "Android 8.0+ (API 26)"; fix README.md:655 and the README.md:24 badge in the same pass so all surfaces agree with build.gradle.kts:16.

#### `HIGH` The tool namespace and tool set differ per connection mode; llms.txt publishes one flat list and README asserts they are identical

**Where:** `docs/public/llms.txt`:44

**Evidence:**

```
docs/public/llms.txt:44 advertises three modes and :23 a single `## Tools (49 total)` flat list. The Node path registers prefixed names (49 × `name: 'aster_*'` in mcp/src/mcp/tools.ts). The on-device Local MCP path registers the RAW action: apps/android/app/src/main/java/com/aster/service/mode/McpToolRegistry.kt:279-282 `server.addTool(` / `name = action,` / `description = def?.description ?: "Execute $action",` / `inputSchema = schema`, fed by McpMode.kt:180 `return ToolCatalog.resolve(commandHandlers.keys)`. The on-device set is larger and different: ToolCatalog.kt has 67 entries, including :33 `"observe" to ToolEntry(`, :74 `"scroll" to ToolEntry("Scroll", …)`, :75 `"tap" to ToolEntry("Tap", "Tap an element by ref or coordinates", "Screen Control")`, :76 `"long_press"`, :101 `"wait_for_idle"`, :193 `"get_now_playing"` — none of which appear among the 49 Node names. README.md:483 states outright: "All three share the same **49 tools** and the same `CommandHandler` registry", which the two registries disprove (67 vs 49, prefixed vs raw).
```

**Why it matters:** An assistant pointed at a phone's Local MCP endpoint (`http://<phone-ip>:8080/mcp`) mis-names every tool relative to the doc AND is blind to the on-device-only screen-control loop (observe/tap/scroll/wait_for_idle). This is the fact llms.txt most needs to carry and the one it flattens. Severity corrected from critical to high for the same reason as the prefix finding: a live client still gets the correct per-mode list at handshake.

> **CORRECTED — the number in this finding is wrong.** The on-device set is **77**, not 67.
> `ToolCatalog.kt` is display metadata, not the registry: `McpToolRegistry.registerTools()` and
> `IpcMode.kt` both read `commandHandlers.keys`, which `ModeModule.kt` `provideCommandHandlers()`
> builds from every handler's `supportedActions()`. The catalogue is missing ten of them. The
> finding's substance — that the namespace and the tool set differ per mode and that llms.txt
> flattens both — stands and was fixed. Only the count was wrong. See
> [The 67 that was actually 77](#the-67-that-was-actually-77).

**Fix:** Split the Tools section by mode — "Remote WebSocket / Node server (`aster_*`, 49 tools)" and "On-device Local MCP + IPC (unprefixed actions, **77** reachable via `commandHandlers.keys`, superset incl. observe/tap/scroll/get_now_playing)" — and correct the false sentence at README.md:483.

#### `MEDIUM` llms.txt enumerates the security posture but omits that /api/* is unauthenticated and binds 0.0.0.0

**Where:** `docs/public/llms.txt`:46

**Evidence:**

```
docs/public/llms.txt:45-46 is the whole security inventory — `- Safety: device approval gate (pending → approved), on-screen kill switch during AI control, fail-closed package policy that blocks control of banking apps by default` and `- Security: plain ws:// on trusted LAN; use Tailscale (auto-detected, `tailscale serve` TLS) for remote access; no telemetry, local SQLite storage`. No mention of API auth. The repo's own BACKLOG.md:29 is headed `## 2. The API has no authentication and binds `0.0.0.0`` and :31 reads "Every `/api/*` route is unauthenticated, and the server binds all interfaces. `POST /api/event-forwarding/prefill-token` returns the **full plaintext** agent gateway token". README.md:504 is partly candid ("**any client on the network that knows the port can register as `pending`**") but carries neither BACKLOG sentence.
```

**Why it matters:** The bullet leads with "Security:" and reads as a complete posture, so an assistant asked whether Aster is safe on an office LAN or behind a reverse proxy answers from an inventory missing the one open surface that returns a plaintext token. Severity corrected high → medium: README.md:504 does state the no-shared-secret trust boundary, so the summary understates rather than contradicts.

**Fix:** Add one Key-facts bullet: "No API authentication — `/api/*` and `/mcp` on :5988 are unauthenticated and bind 0.0.0.0; the trust boundary is your network plus the device-approval tap. Never port-forward 5987/5988."

#### `LOW` No H2 section uses the markdown link-list form; a spec parser extracts zero links

**Where:** `docs/public/llms.txt`:9

**Evidence:**

```
The llmstxt.org section form is `- [name](url): notes`. Actual, read verbatim: docs/public/llms.txt:9 `- Website: https://aster.matterwardlabs.com`, :10 `- GitHub: https://github.com/satyajiit/aster-mcp`, :11 `- npm package: https://www.npmjs.com/package/aster-mcp`, :12-14 same shape; :52 `- Python runtime manifest: https://aster.matterwardlabs.com/runtimes/python/index.json`, :53-54 same. Every one is `Label: bare-URL`, never a markdown link. The H1 at :1 `# Aster`, the blockquote at :3 and the free prose at :5 ARE conformant.
```

**Why it matters:** Tooling that expands an llms.txt index by extracting the per-section link list and fetching each target yields an empty set against this file, so it can only be read as a flat blob. Severity corrected high → low: this affects a narrow class of expander tooling, the URLs remain plainly legible to any model reading the text, and no Google or answer-engine ranking consequence follows from the formatting.

**Fix:** Rewrite the `## Links` and `## Machine-readable endpoints` bullets as `- [Website](https://aster.matterwardlabs.com): marketing and setup page`, etc. Leave the prose paragraph at :5 alone — the spec permits it.

#### `LOW` The two "machine-readable endpoints" describe capabilities outside Aster's tool surface and have no consumer anywhere in the workspace

**Where:** `docs/public/llms.txt`:52

**Evidence:**

```
Both manifests exist and ship: docs/public/runtimes/python/index.json (`"latest": "3.14.3"`, one CPython android-arm64 tarball under release tag `runtimes-python-v3.14.3`, sha256 + `"sizeBytes": 9884777`) and docs/public/runtimes/whisper/index.json (`"latest": "base-v1"`, three whisper.cpp GGML models tiny/base/small under `runtimes-whisper-v1`), and both are copied into docs/.output/public/runtimes/. I grepped the entire aster-platform-root workspace (excluding node_modules/.git/target/.output/build) for `runtimes/python/index.json` and `runtimes/whisper/index.json`: the only hits are docs/public/llms.txt:52-53 and the producer echo at docs/scripts/build-python-android.sh:379. A workspace-wide grep for `aster.matterwardlabs.com` returns no aster-one file at all — aster-one's Python runtime is fetched from `https://cdn.openally.ai/runtimes/python/…` (aster-one/docs/architecture/IOS-ON-DEVICE-PYTHON-PLAN-2026-08-29.md:1230), not from this domain. Neither Python execution nor speech-to-text appears among the 49 names in mcp/src/mcp/tools.ts or in ToolCatalog.kt.
```

**Why it matters:** It is the only section headed "Machine-readable endpoints", so it reads as Aster's programmatic contract and invites the inference that Aster ships a Python runtime and Whisper transcription. Severity corrected medium → low: the files are real and internally valid, so the harm is an inference risk rather than a broken reference.

**Fix:** Annotate each with its actual consumer and scope, or drop them from llms.txt; if they belong to OpenAlly rather than Aster, host them under openally.ai.

#### `LOW` llms.txt pins no version, and the MCP server hardcodes a version 16 patches behind the package

**Where:** `docs/public/llms.txt`:3

**Evidence:**

```
I read all 54 lines of docs/public/llms.txt: there is no version string anywhere in the file. Three values disagree: mcp/package.json:3 `"version": "0.1.16",`; mcp/src/mcp/index.ts:15 `version: '0.1.0',` inside the `new Server({ name: 'aster', … })` Implementation block, i.e. the value every MCP client is told at handshake (the original evidence cited line 17 — it is line 15); apps/android/app/src/main/java/com/aster/service/mode/McpMode.kt:103 `version = "1.1.5"` for `Implementation(name = "aster-mcp-android", …)`.
```

**Why it matters:** The genuinely verified defect is mcp/src/mcp/index.ts:15 — the one place a client can machine-read a version reports 0.1.0 against a published 0.1.16, so even an agent that introspects gets the wrong answer. Severity corrected medium → low: llms.txt carrying no version is a convention gap with no demonstrated failure, and the version drift is a server-code issue rather than a docs one.

**Fix:** Read the version from package.json in mcp/src/mcp/index.ts:15 instead of the literal '0.1.0'; optionally add a build-time `- Current server version:` bullet under Key facts.

#### `LOW` llms.txt omits the health endpoints and states the Tailscale caveat incompletely

**Where:** `docs/public/llms.txt`:46

**Evidence:**

```
Present in README.md, absent from llms.txt: :304-305 `curl http://localhost:5988/api/health     # -> { "status": "ok", "timestamp": ... }` and `curl http://localhost:5988/api/stats      # device counts`, with :308 "The `/api/health` endpoint is the one to poll from scripts and monitors"; :114 "**App Automations** — Record a flow on-device (taps, text, scrolls) … replay it as an automation"; :115 "**Companion Face**"; the `.mcp.json` block at :398 `"type": "http",`. On Tailscale, README.md:572 is emphatic — "Never `https://<magicdns>:8443/mcp`." — and :755 "Serve TLS-terminates the device WebSocket and the dashboard; it does **not** expose `/mcp`", where docs/public/llms.txt:46 says only "use Tailscale (auto-detected, `tailscale serve` TLS) for remote access".
```

**Why it matters:** The Tailscale bullet omits the one caveat the README shouts, so a reader can land on the Serve URL for MCP. Severity corrected medium → low, and the original claim that llms.txt "actively leads an assistant to the exact wrong URL" is overstated: llms.txt:20 already names `http://<server-ip>:5988/mcp` as the MCP endpoint, so the file under-specifies rather than misdirects. The `.mcp.json` omission is likewise soft for the same reason.

**Fix:** Extend llms.txt:46 to "Tailscale Serve TLS-terminates the device WebSocket and the dashboard only — MCP stays on `http://<ts-ip>:5988/mcp`", and add a Key-facts bullet for `/api/health` and `/api/stats`.

#### `LOW` Four different answers to who makes Aster across llms.txt, package.json, README and the JSON-LD

**Where:** `docs/public/llms.txt`:5

**Evidence:**

```
docs/public/llms.txt:5 ends "Built by Matterward Labs; part of the OpenAlly platform." — mcp/package.json:8 `"author": "Aster Team",` — README.md:782 `MIT © [Satyajit Pradhan](https://github.com/satyajiit)` — docs/nuxt.config.ts:30 `{ name: 'author', content: 'Satyajit Pradhan' },` and the SoftwareApplication JSON-LD at docs/nuxt.config.ts:68 `author: { '@type': 'Person', name: 'Satyajit Pradhan', url: 'https://github.com/satyajiit' },`. There is no Organization publisher node in the JSON-LD block (nuxt.config.ts:59-72).
```

**Why it matters:** Authorship and organisational affiliation are the entity facts an answer engine tries to resolve, and the two structured surfaces disagree with the two prose ones. Severity confirmed at low — an inconsistency, with no functional consequence.

**Fix:** Pick one canonical form and make llms.txt:5, mcp/package.json:8, README.md:782 and the nuxt.config.ts:68 author node agree; consider adding an Organization publisher node alongside the Person author.

### a11y-wcag

#### `CRITICAL` --color-fg-muted (#4a5670) is 2.75:1 on the page background and is the site's tertiary prose ramp

**Where:** `packages/aster-ui/app/assets/css/tokens.css`:29

**Evidence:**

```
I read tokens.css:29 `--color-fg-muted: #4a5670;` and main.css:29 `--color-text-tertiary: var(--color-fg-muted);`. Ratios I computed myself (sRGB relative luminance): #4a5670 on #06060c = 2.75:1, on #10101e = 2.56:1, on #161628 = 2.42:1 — all below 4.5:1 and below even 3:1. Call sites I opened: FooterSection.vue:10 `<p class="text-xs text-text-tertiary">Your AI CoPilot on Mobile via MCP</p>`; FooterSection.vue:66 (MIT-licence paragraph); UseCasesSection.vue:56 `<p class="text-xs leading-relaxed text-text-tertiary">` (the AI-response line of every use-case card); SecuritySection.vue:140 `<p class="text-[11px] text-text-tertiary">MIT licensed…`; HowItWorks.vue:87,100,113,159,172,185 node captions and :269 protocol chips; EmbraceSection.vue:46,52,81,126,145,147,196,202,226; ProactiveSection.vue:100,115,169,176,182,227; IntegrationsSection.vue:27,41; ToolsShowcase.vue:33; main.css:143-145 `.terminal .comment { color: var(--color-text-tertiary); }`; SetupSteps.vue:80-87 JSON punctuation. CORRECTIONS: `grep -rl text-text-tertiary docs/app/components/*.vue` returns 12 files, not the 15-of-17 claimed (60 opaque uses + 14 alpha'd). The light-scheme half (#90a1b5 on #f8fafb, which I measure at 2.52:1, not the claimed ~2.2:1) never renders on this site — docs/nuxt.config.ts:25 pins `htmlAttrs: { lang: 'en', 'data-theme': 'dark' }` — but it does govern mcp/dashboard.
```

**Why it matters:** This is the token behind card descriptions, footer credits, terminal comments, diagram captions and protocol chips — roughly a third of the readable text on the page, at 2.42–2.75:1. Because it lives in packages/aster-ui it is a design-system defect shared with the dashboard, not a page defect.

**Fix:** Lift `--color-fg-muted` in the dark block until it clears 4.5:1 on #06060c (≈ #8a93a8 measures 5.9:1), and fix the light value at tokens.css:174/:194 at the same time; re-check mcp/dashboard, which shares the token.

#### `HIGH` Opacity modifiers on text-text-tertiary drive real text as low as 1.22:1

**Where:** `docs/app/components/HowItWorks.vue`:200

**Evidence:**

```
I recomputed the composites over #06060c: /60 → #2f3648 = 1.68:1, /50 → #282e3e = 1.49:1, /40 → #212634 = 1.34:1, /30 → #1a1e2a = 1.22:1. Verified call sites: HowItWorks.vue:49 `text-[9px] font-mono text-text-tertiary/50 … uppercase`; HowItWorks.vue:200,215,230,247 `<span class="text-text-tertiary/40 flex-shrink-0">&rarr;</span>`; HowItWorks.vue:161,174 `:class="phase >= 1 ? 'text-violet-400' : 'text-text-tertiary/30'"`; LiveChatSection.vue:70 `text-[9px] text-text-tertiary/60`; LiveChatSection.vue:77,93 `<span class="text-[7px] text-text-tertiary/50 …">{{ msg.time }}</span>` — over the bubble fills at LiveChatSection.vue:75 (`bg-aster/[0.15]`) and :91 (`bg-white/[0.04]`) these measure 1.41:1 and 1.50:1; LiveChatSection.vue:119 `<span class="text-[10px] text-text-tertiary/40">Message</span>` = 1.37:1 over its `bg-white/[0.04]` field. REFUTED SUB-CLAIM: the chevrons at :161/:174 are not the sole phase indicator — the sibling icon tile at :153/:166/:179 swaps ring width and fill on the same `phase >=` test. Severity lowered from critical to high: the affected content is secondary (timestamps, separator glyphs, one placeholder), not body prose.
```

**Why it matters:** 7px timestamps at 1.41–1.50:1 and a 10px input placeholder at 1.37:1 are below anything a sighted user can resolve off a colour-calibrated screen, and they stack on a token that already fails at full opacity.

**Fix:** Remove opacity modifiers from text tokens; if a second dim step is wanted, define a real token measured against #06060c.

#### `HIGH` The /70 and /60 accent eyebrow labels on every section and card land at 3.08–4.21:1

**Where:** `docs/app/components/ProactiveSection.vue`:208

**Evidence:**

```
Render site verified at ProactiveSection.vue:208 `<span class="text-[10px] font-mono uppercase tracking-[0.12em]" :class="scenario.labelColor">{{ scenario.label }}</span>`. I converted the Tailwind v4 oklch palette from docs/node_modules/tailwindcss/theme.css and got exactly the hexes the audit claimed (violet-400 #a684ff, rose-400 #ff637e, pink-400 #fb64b6, blue-400 #51a2ff, fuchsia-400 #ed6aff). Composited over #06060c: violet-400/70 = 3.89:1, rose-400/70 = 3.85:1, pink-400/70 = 3.99:1, blue-400/70 = 4.15:1, fuchsia-400/70 = 4.21:1; the /60 step falls to 3.08–3.35:1. Verified labelColor lines: ProactiveSection.vue:287 orange/70, :302 rose/70, :317 blue/70, :332 sky/70, :347 pink/70, :362 violet/70; plus :40 `text-fuchsia-400/70`. EmbraceSection.vue:34 `text-rose-400/70` and :53 `<span class="text-rose-400/60">and then you pick up the phone yourself.</span>` = 3.08:1; vignette labels at :268/:284 rendered by :223. HeroSection.vue:147,153,159,165 `nameColor: 'text-…-400/70'` rendered at :116 as the assistant name above every chat reply. CORRECTIONS: the cited ProactiveSection.vue:254 is a `badgeClass`, not a `labelColor` (the violet labelColor is :362); EmbraceSection.vue:42 is an `<Icon>`, a non-text target where 3.08:1 clears the 3:1 bar.
```

**Why it matters:** These 10px uppercase kickers are the only text naming each card's category and the assistant on each hero reply. They are normal-size text, so 4.5:1 applies, and the full-opacity base shades all clear 7:1 — the alpha is the entire cause.

**Fix:** Drop the alpha on label colours; quiet them with weight or size, or step to the -300 shade at full opacity.

#### `HIGH` text-aster/40, /50 and /60 carry substantive text at 2.46–4.34:1

**Where:** `docs/app/components/HowItWorks.vue`:217

**Evidence:**

```
main.css:15 `--color-aster: var(--color-primary);` resolving to tokens.css:31 `--color-primary: #2dd4bf` (10.86:1 at full opacity on #06060c). My composites: /80 = 7.10:1, /70 = 5.57:1, /60 = 4.34:1 (fails 4.5), /50 = 3.29:1 (fails), /40 = 2.46:1 (fails). Text sites I read: HowItWorks.vue:217 `<span class="text-aster/50">ws://</span>`; HowItWorks.vue:256 `<span class="text-[9px] text-aster/40 font-mono">{{ activeScenario.latency }}</span>` — the only place the latency figure appears; HowItWorks.vue:62 `<span class="text-aster/50 font-mono text-sm select-none mt-0.5">&gt;</span>`; LiveChatSection.vue:85 `<span class="text-[8px] font-mono text-aster/50">{{ msg.text }}</span>` — every tool-call chip, which I measure at 3.32:1 over its own `bg-aster/[0.05]` pill on the #0c0c12 phone body; LiveChatSection.vue:155 `text-aster/60` tool tags; ProactiveSection.vue:121 `<span class="text-[10px] font-mono text-aster/60">{{ event.action }}</span>`. REFUTED SUB-CLAIMS: HowItWorks.vue:236 is `text-green-400/40`, not `text-aster/40`; HowItWorks.vue:221 and IntegrationsSection.vue:45 are `<Icon>` elements (3:1 non-text threshold), not text.
```

**Why it matters:** The alpha'd accent carries the WebSocket route, the measured latency and the tool names invoked — the concrete substance of those sections — at 8–10px.

**Fix:** Use --color-aster at full opacity for text; treat /70 (5.57:1) as the dimmest permissible step and reserve /40–/50 for borders and fills.

#### `HIGH` UseCasesSection tool tags measure 2.50–3.49:1 on all 21 cards

**Where:** `docs/app/components/UseCasesSection.vue`:67

**Evidence:**

```
Render site verified at UseCasesSection.vue:64-71: `<span … class="px-2 py-0.5 rounded-md text-[10px] font-mono border transition-colors" :class="[group.tagClass]">{{ tool }}</span>`. Every cited tagClass line is exact — :108 violet, :137 amber, :166 teal, :195 blue, :224 rose, :253 fuchsia, :282 orange, each of the form `border-<c>-500/10 text-<c>-400/50 bg-<c>-500/[0.04]`. Composited over the card fill (`bg-surface-raised/80` at :35 → #0e0e1a) plus the tag's own 4% tint, my measurements are: rose 2.50:1, violet 2.51:1, blue 2.63:1, fuchsia 2.67:1, orange 2.82:1, teal 3.30:1, amber 3.49:1. The `/10` borders measure 1.05–1.09:1 against the tag fill, so they add no boundary.
```

**Why it matters:** These tags name the actual MCP tools (search_media, make_call_with_voice, delete_file) — the verifiable substance a technical reader scans for — at 10px and roughly half the required contrast.

**Fix:** Raise the text alpha to 1.0 (each -400 base clears 7:1 on this ground) and keep the tint fill for visual weight.

#### `HIGH` SecuritySection permission-layer rows fade to 1.48:1

**Where:** `docs/app/components/SecuritySection.vue`:234

**Evidence:**

```
Render site verified at SecuritySection.vue:115-119 (`<span class="text-[10px] font-medium flex-1" :class="layer.textColor">{{ layer.label }}</span>` / `<span class="text-[9px] font-mono" :class="layer.badgeColor">{{ layer.badge }}</span>`). Data lines exact: :222-223 `textColor: 'text-green-300/70'` / `badgeColor: 'text-green-400/40'`; :231-232 `/50` / `/30`; :240-241 `/35` / `/20`. I composited each row against its own tint over the card's real backing, which is `bg-[#0c0c14]` at SecuritySection.vue:99, and get text 6.90 / 4.08 / 2.61 and badge 2.62 / 1.98 / 1.48 — the same conclusion as claimed: the badge fails on all three rows and both text and badge fail on rows 2 and 3. Row tints `bg-green-500/[0.06]`, `/[0.04]`, `/[0.02]` are 1.02–1.18:1 against the card and contribute nothing.
```

**Why it matters:** The faded rows carry the section's own security claims — "Permission-gated access / required" and "Sandboxed execution / isolated". Fading real content to 1.48:1 is a 1.4.3 failure regardless of the depth-effect intent.

**Fix:** Express the depth ladder through background tint and border only; hold all three rows' text and badge at one value clearing 4.5:1 (green-300 at full opacity is 14.4:1 on this ground).

#### `HIGH` Hero carousel dots: <button> with no accessible name, 6x6 px, 1.34:1 inactive state

**Where:** `docs/app/components/HeroSection.vue`:126

**Evidence:**

```
HeroSection.vue:126-132 reads exactly as claimed — a self-closing `<button v-for="(_, i) in conversations" :key="i" class="w-1.5 h-1.5 rounded-full transition-all duration-300 cursor-pointer" :class="i === currentIndex ? 'bg-aster w-4' : 'bg-text-tertiary/40 hover:bg-text-tertiary'" @click="goTo(i)" />` with no text child, no aria-label, no title and no aria-current. `conversations` (HeroSection.vue:169-226) has 8 entries, so eight unnamed buttons. w-1.5/h-1.5 = 6x6 CSS px inside a `gap-1.5` (6px) row at :125, putting adjacent centres ~12px apart, so the 2.5.8 undersized-target spacing exception (24px circles must not intersect) does not apply. I measured the inactive fill `bg-text-tertiary/40` → #212634 = 1.34:1 against #06060c, against the 3:1 required for a control that is its own only visual identifier.
```

**Why it matters:** These are the only manual control over the 5-second auto-advancing carousel: a screen reader hears eight identical unnamed buttons, a motor-impaired user cannot hit a 6px target, and a low-vision user cannot see seven of the eight.

**Fix:** Add `:aria-label` naming the position and `:aria-current`, pad the button to a >=24x24 hit area around the 6px dot, and raise the inactive fill to >=3:1.

#### `HIGH` Auto-playing content with no pause/stop/hide control (2.2.2, Level A)

**Where:** `docs/app/components/HeroSection.vue`:262

**Evidence:**

```
HeroSection.vue:262-271 — `headlineInterval = setInterval(() => { headlineIndex.value = (headlineIndex.value + 1) % 2 … }, 4000)`, and the swapped content is the `<h1>` itself (HeroSection.vue:25-40 wraps a `<Transition>` over the two headline variants). HeroSection.vue:242-247 — `interval = setInterval(() => { currentIndex.value = (currentIndex.value + 1) % conversations.length }, 5000)`. LiveChatSection.vue:325-327 `onMounted(() => { runScript() })` starts the ~14-20s cascade of setTimeouts built at :277-309; the only control is the replay button at :166-173, which is `v-if="finished"` and therefore absent for the whole run. `grep -rn 'pause\|Pause' docs/app` returns only the string literal at HowItWorks.vue:319. Indefinitely looping CSS confirmed at main.css:94 `animation: border-rotate 6s linear infinite`, `animate-ping` at HeroSection.vue:18,81, ProactiveSection.vue:45,97, HowItWorks.vue:52, EmbraceSection.vue:350-357 `call-pulse … infinite`, LiveChatSection.vue:363-365 `typing-bounce … infinite`. CORRECTIONS: HowItWorks.vue:373-375 `play(0)` runs a one-shot 3.8s sequence (timers at :367-370) — under the 5-second threshold, so it is not itself a 2.2.2 failure; and AGlowOrb.vue:39 / AStatusPill.vue:63 are not rendered on this page.
```

**Why it matters:** 2.2.2 is Level A, so this puts the page below the entry conformance bar. Rewriting the document's primary heading every 4 seconds also changes the accessible name a screen reader has just announced. The prefers-reduced-motion guard at tokens.css:251-259 does not help — it only zeroes CSS durations.

**Fix:** Add one visible pause/play control governing the two hero cyclers and the LiveChat demo, stopping the timers when engaged, and gate the setInterval/setTimeout starts on `matchMedia('(prefers-reduced-motion: reduce)').matches`.

#### `MEDIUM` No <main> landmark and no skip link; 9 nav tab stops precede all content

**Where:** `docs/app/app.vue`:2

**Evidence:**

```
docs/app/app.vue:2-18 is `<div class="noise">` wrapping `<NavBar />` and 14 section components with no wrapper element. I ran `grep -rn '<main\|sr-only\|skip-' docs/app` — no matches. The only landmarks in the tree are NavBar.vue:2 `<nav class="fixed top-0 …">` and FooterSection.vue:2 `<footer …>`; every content block is a bare `<section>` (AuthorSection.vue:2, EmbraceSection.vue:2, FeaturesGrid.vue:2, HeroSection.vue:2, HowItWorks.vue:2, IntegrationsSection.vue:2, LiveChatSection.vue:2, ProactiveSection.vue:2, ScreenshotsSection.vue:2, SecuritySection.vue:2, SetupSteps.vue:2, ToolsShowcase.vue:2, UseCasesSection.vue:2 — 13, not the 15 the title says). NavBar.vue:7-30 places 9 tab stops ahead of the hero. SEVERITY LOWERED from high to medium: as the finding itself concedes, 2.4.1 Bypass Blocks is scoped to blocks repeated across pages and this is a single-page site, and a `<main>` element is a strong best practice rather than a normative AA requirement, so this is a real AT-navigation defect without a clean normative hook.
```

**Why it matters:** The document exposes navigation and contentinfo but no main region, so the standard "jump to main content" gesture has no target across 3,400 lines of content, and a keyboard user traverses 9 nav stops on every load.

**Fix:** Wrap the 14 content components in app.vue in `<main id="main">`, add a visually-hidden-until-focused `<a href="#main">` as the page's first child, and give `<nav>` an aria-label.

#### `MEDIUM` Both tab groups expose selected state by colour alone with no aria-selected/aria-pressed

**Where:** `docs/app/components/ScreenshotsSection.vue`:20

**Evidence:**

```
ScreenshotsSection.vue:20-31 (App/Dashboard) and :36-47 (Dark/Light) are plain `<button v-for … :class="activeTab === tab.id ? 'bg-aster/10 border-aster/30 text-aster …' : 'border-border-dim text-text-tertiary …'" @click="activeTab = tab.id">` — no role, no aria-selected, no aria-pressed, no aria-controls. HowItWorks.vue:26-37 is the identical pattern for the six scenario pills. `grep -rn 'aria-pressed\|aria-selected\|aria-current' docs/app` returns nothing. I measured the non-text state cues against #06060c: selected `bg-aster/10` fill = 1.14:1 and `border-aster/30` = 1.86:1; unselected `border-border-dim` (#1a1a30) = 1.19:1 — all below the 3:1 non-text minimum, leaving the label colour (#2dd4bf at 10.86:1 vs #4a5670 at 2.75:1) as the only perceptible difference. REFUTED SUB-CLAIM: the 2.1.1 / arrow-key half is wrong — these are plain `<button>` elements, fully keyboard operable with Tab and Enter; roving-tabindex arrow navigation is only required of role="tablist"/radiogroup, which these never claim. Severity lowered from high to medium accordingly.
```

**Why it matters:** Selecting a tab is the only route to the Web Dashboard and light-theme screenshots. A screen-reader user gets no indication which of the six/two identical buttons is active, and both the selected and unselected borders are below 3:1, so the state is carried by label colour alone.

**Fix:** Add `:aria-pressed="activeTab === tab.id"` (simplest, since these are toggles) or a full tablist with aria-selected/aria-controls; independently raise the selected border to >=3:1 and add a non-colour cue.

#### `MEDIUM` Heading order breaks: an h4 under an h2, an orphan h3 section, and headings marked up as spans

**Where:** `docs/app/components/LiveChatSection.vue`:149

**Evidence:**

```
LiveChatSection.vue:10 is the section's `<h2>`; the next heading in that section is :149 `<h4 class="text-sm font-semibold" :class="step.active ? 'text-text-primary' : 'text-text-tertiary'">{{ step.title }}</h4>` — h2 straight to h4. AuthorSection.vue contains exactly one heading, :9 `<h3 class="text-xl font-bold text-text-primary mb-2">Follow the Author</h3>`, with no h2 anywhere in the file. SecuritySection.vue:139 `<span class="text-sm font-semibold text-text-primary">100% Open Source</span>` is styled and positioned as a card heading, paired with the `<p>` at :140. FooterSection.vue:9 `<span class="text-sm font-semibold text-text-primary">Aster</span>` is the same pattern. I verified by grep that there is exactly one `<h1>` in the tree (HeroSection.vue:25) and that docs/nuxt.config.ts:25 sets `htmlAttrs: { lang: 'en', … }` — both correct, as the audit stated.
```

**Why it matters:** Heading navigation is the primary way a screen-reader user skims this 15-section single-page document, and there are no other landmarks to fall back on. The LiveChat steps appear to belong to a heading that does not exist, and AuthorSection reads as a subsection of ToolsShowcase.

**Fix:** Change LiveChatSection.vue:149 to `<h3>`, promote AuthorSection.vue:9 to `<h2>`, and convert SecuritySection.vue:139 and FooterSection.vue:9 to heading elements with the visual styling kept.

#### `MEDIUM` Footer bottom link row does not wrap and is clipped by overflow-x:hidden at 320px

**Where:** `docs/app/components/FooterSection.vue`:69

**Evidence:**

```
I read FooterSection.vue in full. :65 is `<div class="mt-10 pt-6 border-t border-border-dim flex flex-col sm:flex-row items-center justify-between gap-4">` and its second child at :69 is `<div class="flex items-center gap-4">` with NO flex-wrap — contrast the upper link row at :15, `<div class="flex flex-wrap items-center justify-center gap-4 sm:gap-6 …">`, which wraps correctly. The four children are the links at :70-77 "OpenAlly.ai", :78-85 "npm: aster-mcp", :86-93 "ClawHub" and :94-101 "Model Context Protocol" — 54 characters at text-xs plus three 16px gaps, well over the 272px a 320px viewport leaves after the footer's `px-6` at :2. The overflow is then clipped rather than scrollable by main.css:33-40, which sets `overflow-x: hidden` on both `html` and `body`.
```

**Why it matters:** 1.4.10 Reflow requires content to be presentable at 320 CSS px without loss of content; with overflow-x hidden on both html and body the tail of the row is unreachable by any means, which is loss of functionality rather than awkward layout.

**Fix:** Add `flex-wrap` at FooterSection.vue:69 to match :15, and reconsider the blanket `overflow-x: hidden` in main.css, which converts every reflow bug into silent content loss.

#### `MEDIUM` The prefers-reduced-motion guard covers CSS only; every JS-driven animation keeps running

**Where:** `packages/aster-ui/app/assets/css/tokens.css`:251

**Evidence:**

```
tokens.css:251-259 is exactly as quoted — `@media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation-duration: 0.01ms !important; animation-iteration-count: 1 !important; transition-duration: 0.01ms !important; scroll-behavior: auto !important; } }`. `grep -rn 'matchMedia\|prefers-reduced' docs/app` returns nothing, so nothing in the page consults it. Still running under a reduced-motion preference: HeroSection.vue:244-247 (5s conversation interval) and :262-271 (4s headline interval); LiveChatSection.vue:277-309 setTimeout cascade started at :325-327; HowItWorks.vue:367-370 phase timers started at :373-375; EmbraceSection.vue:307-327 and ProactiveSection.vue:375-395 IntersectionObserver reveal timers. AAnimatedEntrance.vue:5 carries the comment "Honours prefers-reduced-motion via the global reset in tokens.css" — true for that CSS-only component, false for the page's JS animations.
```

**Why it matters:** The CSS guard silences pulses and fades but leaves the content-swapping animations fully active — the ones that move text and rewrite the `<h1>` — so 2.2.2 and 2.3.3 remediation cannot be achieved in CSS alone.

**Fix:** Add a shared `useReducedMotion()` composable in packages/aster-ui/app/composables and gate the setInterval/setTimeout starts in HeroSection, LiveChatSection and HowItWorks on it, rendering the final frame immediately.

#### `MEDIUM` Trace output and chat transcript update with no live region or focus management

**Where:** `docs/app/components/HowItWorks.vue`:191

**Evidence:**

```
HowItWorks.vue:191 is `<div class="rounded-xl bg-surface/60 border border-border-dim p-4 font-mono text-[11px] leading-loose min-h-[140px] overflow-x-auto">` wrapping a `<Transition name="log" mode="out-in">` at :192. Clicking a scenario pill (:26-37, `@click="play(i)"`) rewrites the prompt at :64-66, the three trace rows at :195-237, the result line at :248 and the latency badge at :256 over 3.8 seconds (timers at :367-370), with focus remaining on the pill. LiveChatSection.vue:65-97 is a `<TransitionGroup name="msg">` that appends messages on a timer, with an unlabelled typing indicator at :99-110. ScreenshotsSection.vue:51-93 swaps the whole panel with no aria-controls from the tab buttons at :20-31. `grep -rn 'aria-live\|role="status"\|role="log"' docs/app` returns nothing. The shared layer does it correctly at AToastHost.vue:21 `<div class="toasts" role="region" aria-live="polite">`, so the pattern exists but is unused here.
```

**Why it matters:** 4.1.3 Status Messages (AA) requires the result of a user action to be announced without receiving focus. Pressing the "vibrate" pill produces the result text at HowItWorks.vue:319 and the ~90ms latency at :320 silently, so for a screen-reader user the six pills appear inert.

**Fix:** Put `role="status"` (or aria-live="polite" aria-atomic="true") on HowItWorks.vue:191 and LiveChatSection.vue:65, and add aria-controls from each tab button to the panel it swaps in.

#### `MEDIUM` Shared layer: AModal has no focus trap, no initial focus, no focus restore and no accessible name

**Where:** `packages/aster-ui/app/components/AModal.vue`:26

**Evidence:**

```
AModal.vue:26 is `<div v-if="open" class="modal" role="dialog" aria-modal="true" @click.self="close">` with no aria-labelledby and no aria-label, and :30 `<h2 class="modal__title">{{ title }}</h2>` carries no id to point at. I read the whole file: nothing moves focus into the panel on open, nothing constrains Tab within `.modal__panel`, nothing restores focus on close, and no `inert`/`aria-hidden` is applied to the background despite aria-modal="true". Escape is handled correctly at :15-20 and the close button is labelled at :33 `aria-label="Close"`, as the finding concedes; `.modal__close` at :86-95 has `padding: 0.25rem` and `font-size: 1.125rem` (≈26x26, clearing 2.5.8) but no :focus-visible rule. Scope note confirmed: `grep -rnoE '<A[A-Z][A-Za-z]*' docs/app` returns zero matches, so no A-prefixed component renders on the landing page and this reaches mcp/dashboard only. The component's own comment at :2-3 says it exists to replace CreateFolderModal.vue and DeleteConfirmModal.vue, so every future dialog inherits the gap.
```

**Why it matters:** A modal without a focus trap is the canonical keyboard failure: Tab walks out into the page behind a visually blocking dialog with no route back, and the dialog opens nameless for a screen reader.

**Fix:** Bind aria-labelledby to a generated id on .modal__title, focus the panel on open, cycle Tab/Shift+Tab within .modal__panel, restore focus to the previously-active element on close, and add a :focus-visible ring to .modal__close.

#### `LOW` Zero focus styling in docs/app; the page ships only the UA default ring

**Where:** `docs/app/assets/css/main.css`:1

**Evidence:**

```
`grep -rn 'focus-visible\|:focus\|outline' docs/app` returns no matches — not in main.css (150 lines, which I read in full) and not in any of the 17 components. The only focus rules in the repo are in the shared layer, which this page never instantiates (`grep -rnoE '<A[A-Z][A-Za-z]*' docs/app` returns zero): AButton.vue:72, ACard.vue:73, AToggle.vue:71. I confirmed the finding's own defence of the browser default: docs/node_modules/tailwindcss/preflight.css:173-174 contains only `:-moz-focusring { outline: auto; }` and strips no outlines, so the UA ring survives and 2.4.7 is technically satisfied. SEVERITY LOWERED from medium to low: by the finding's own admission there is no normative failure here — it is a design gap on a #06060c ground, worst on the 6px dots at HeroSection.vue:126-132 and the `border border-transparent` pills at HowItWorks.vue:29-32.
```

**Why it matters:** On a near-black page the UA default is the only keyboard affordance and varies by browser; combined with the missing skip link it makes traversing 15 sections harder than it needs to be.

**Fix:** Add one rule in main.css reusing the layer's own pattern from AButton.vue:72-75: `:where(a, button, [tabindex]):focus-visible { outline: 2px solid var(--color-aster); outline-offset: 2px; }`.

#### `LOW` 64px fixed NavBar with no scroll-padding-top can hide keyboard focus targets (2.4.11)

**Where:** `docs/app/components/NavBar.vue`:2

**Evidence:**

```
NavBar.vue:2-6 is `<nav class="fixed top-0 left-0 right-0 z-50 transition-all duration-300" :class="scrolled ? 'bg-surface/80 backdrop-blur-xl border-b border-border-dim' : ''">` over `<div class="max-w-6xl mx-auto px-6 h-16 …">` — h-16 = 64px, fixed, z-50, opaque once `scrolled` (NavBar.vue:36-44, scrollY > 20). main.css:33-36 is `html { scroll-behavior: smooth; overflow-x: hidden; }` with no scroll-padding-top, and `grep -rn 'scroll-padding\|scroll-margin' docs` returns nothing. REFUTED SUB-CLAIM: the hash-link-landing half of the rationale is wrong — every content section carries `py-32` (128px, e.g. FeaturesGrid.vue:2, HowItWorks.vue:2, UseCasesSection.vue:2), so the seven NavBar anchors land their headings well clear of the 64px bar. Severity lowered from medium to low: the remaining case is the Shift-Tab-upward scroll, which is a genuine 2.4.11 exposure but not directly demonstrable from source.
```

**Why it matters:** 2.4.11 Focus Not Obscured (Minimum) is AA in WCAG 2.2; when focus moves to an element just above the viewport the browser scrolls it flush to the top, underneath the opaque bar.

**Fix:** Add `scroll-padding-top: 5rem` to the `html` rule in main.css.

#### `LOW` EmbraceSection phone mock is a fixed 220px inside 32px padding and overflows at 320px

**Where:** `docs/app/components/EmbraceSection.vue`:117

**Evidence:**

```
EmbraceSection.vue:116-117 reads `<div class="lg:col-span-2 flex items-center justify-center p-8 lg:p-10">` / `<div class="relative w-[220px]">`. At 320px the section's `px-6` (EmbraceSection.vue:2) leaves 272px, the card's `p-8` takes 64px and the border 2px, leaving ~206px for a hard-coded 220px child — a ~14px overflow, clipped by `body { overflow-x: hidden }` at main.css:38-40. The related case LiveChatSection.vue:20-21 `<div class="relative flex-shrink-0 phone-wrapper"><div class="relative w-[270px] sm:w-[285px]">` fits at exactly 320px with 2px to spare and `flex-shrink-0` preventing give. REFUTED SUB-CLAIM: the clipped strip is ~7px off each edge of a `justify-center` child, so it takes the phone frame's border, not the "Accept" call button at :164-169, which sits inset inside a centred `gap-8` row. Severity lowered from medium to low: real 1.4.10 overflow, but the clipped pixels carry no information.
```

**Why it matters:** 1.4.10 is evaluated at 320 CSS px, and a fixed-width child inside fixed padding cannot reflow; the surrounding overflow-x:hidden turns it into clipping rather than a scrollbar.

**Fix:** Change `w-[220px]` to `w-full max-w-[220px]` and step the wrapper padding down at the smallest breakpoint (`p-5 sm:p-8`).

#### `LOW` html font-size pinned to 16px in the shared layer, plus 40+ hard-coded px sizes as small as 7px

**Where:** `packages/aster-ui/app/assets/css/tokens.css`:208

**Evidence:**

```
tokens.css:208-212 is exactly `html { font-size: 16px; line-height: 1.5; -webkit-text-size-adjust: 100%; }`, which overrides the user's browser default-font-size preference for every rem-based utility on the page. Absolute px sizes verified: LiveChatSection.vue:77,93 `text-[7px]`; EmbraceSection.vue:162,168 `text-[8px]` (Decline/Accept); LiveChatSection.vue:84,85 `text-[8px]`; HowItWorks.vue:49,55,87,100,113,159,172,185 `text-[9px]`; SecuritySection.vue:66,82,118 `text-[9px]`; ProactiveSection.vue:169 `text-[9px]`; plus ~30 `text-[10px]`/`text-[11px]` sites. CORRECTIONS: SecuritySection.vue:76 is `text-[8px] text-sky-400/50`, not the `text-[9px]` claimed. Severity lowered from medium to low: as the finding itself concedes, browser zoom still scales the page, so 1.4.4 Resize Text is not cleanly failed — this is a legibility-floor and text-only-enlargement concern, not an AA failure. Note the fix touches the shared layer that mcp/dashboard also consumes.
```

**Why it matters:** 7–9px type is below a practical legibility floor before contrast is considered, and it is exactly the same text that carries the worst contrast ratios in the findings above.

**Fix:** Drop `font-size: 16px` from tokens.css:209 (the browser default is already 16px and honours the user setting) and replace the text-[7px]–text-[11px] arbitrary values with the layer's rem-based label/body steps, raising the floor to ~11px.

#### `LOW` Every repeated collection is a div/span grid rather than a list

**Where:** `docs/app/components/ToolsShowcase.vue`:29

**Evidence:**

```
`grep -rn '<ul\|<ol\|<li' docs/app` returns nothing across all 17 components. Verified cases: ToolsShowcase.vue:29-37, the 49 tool names as `<div class="flex flex-wrap gap-1.5">` + `<span v-for="tool in category.tools">`; FeaturesGrid.vue:16-22, the feature cards as `<div class="grid …">` + `<FeatureCard v-for>`; UseCasesSection.vue:63-72; ProactiveSection.vue:218-229; SetupSteps.vue:15-41 with SetupStep.vue:5-7; FooterSection.vue:15-61 and :69-101; SecuritySection.vue:86-91, :114-120, :122-127; EmbraceSection.vue:39-49, :189-199, :214-228. REFUTED SUB-CLAIM: the ordinal-semantics half is wrong — ProactiveSection.vue:225 `<span class="text-[8px] font-mono font-bold" :class="step.numColor">{{ si + 1 }}</span>` and SetupStep.vue:6 `<span class="text-sm font-bold text-aster font-mono">{{ number }}</span>` render the step number as a real text node that AT reads in order, so the sequence IS conveyed. Severity lowered from medium to low accordingly.
```

**Why it matters:** 1.3.1 asks that visually conveyed structure be programmatically determinable; without list semantics a screen-reader user gets no item count and no list boundary around the 49-tool inventory, which is the page's central factual claim.

**Fix:** Convert the repeated collections to `<ul>`/`<li>` with `list-none`, keeping the existing grid/flex styling.

#### `LOW` HowItWorks completion checkmarks render as empty spans carrying state

**Where:** `docs/app/components/HowItWorks.vue`:205

**Evidence:**

```
I confirmed the mechanism: docs/node_modules/@nuxt/icon/dist/module.mjs:51 sets `$default: "css"` for the render mode, and .../runtime/components/css.js:165 returns `h("span", { class: ["iconify", cssClass.value] })` — an empty span painted by a CSS mask, invisible to AT. The state-bearing sites are HowItWorks.vue:205-206 (`<span v-if="phase === 0" …>...</span>` / `<Icon v-else-if="phase > 0" name="lucide:check" class="ml-auto text-[9px] text-violet-400/40 …" />`), identical at :220-221 and :235-236, plus :245 `<Icon name="lucide:check-circle" class="inline text-[10px]" />` marking the trace result. EmbraceSection.vue:222 `<span class="text-xl" role="img" :aria-label="vignette.emojiLabel">` is the tree's only text alternative. REFUTED SUB-CLAIMS: ScreenshotsSection.vue:29/:45 icons are decorative — the labels beside them already differ ("Android App"/"Web Dashboard" at :103-104, "Dark"/"Light" at :108-109); and SecuritySection.vue:124's check sits beside `{{ item.label }} {{ item.desc }}` at :125, so it is not the sole carrier either. Severity lowered from medium to low, and the finding narrowed to HowItWorks, where it substantially overlaps the live-region finding.
```

**Why it matters:** "This trace step completed" is conveyed only by an empty span plus a colour I measure at 2.46:1, so neither a screen-reader user nor a low-vision user gets the signal.

**Fix:** Add a visually-hidden text node beside the state-bearing checkmarks, or aria-label plus role="img" on a wrapper; leave decorative icons as they are.

#### `LOW` Two sections default to opacity:0 and are revealed only by an IntersectionObserver

**Where:** `docs/app/components/EmbraceSection.vue`:331

**Evidence:**

```
EmbraceSection.vue:330-341 is `.observe-fade { opacity: 0; transform: translateY(24px); transition: … }` / `.observe-fade.is-visible { opacity: 1; transform: translateY(0); }`, revealed only by the observer at :307-327. ProactiveSection.vue:398-409 plus :375-395 is the same construction. The class is on EmbraceSection.vue:11, :28, :59, :96, :108 and :218 and on ProactiveSection.vue:11, :25, :131 and :196 — the entire visible content of two of the fifteen sections. EmbraceSection.vue:324 does query globally: `document.querySelectorAll('.observe-fade')`, where ProactiveSection.vue:392 correctly scopes to `'#proactive .observe-fade'`. REFUTED SUB-CLAIM: the "will silently break if mount order changes" consequence does not hold — both observers add the identical `is-visible` class, and Vue scoped styles key on the class name with a data-v attribute on the element itself, so whichever observer fires reveals the element correctly; there is no user-visible difference. Severity lowered from medium to low: the surviving issue is the no-JS/AT mismatch, which WCAG does not normatively require JS-free rendering for.
```

**Why it matters:** On a prerendered static site on GitHub Pages, a hydration bundle that fails to load leaves two full sections permanently invisible with no fallback; and content at opacity:0 without programmatic hiding is read by AT while invisible on screen.

**Fix:** Scope EmbraceSection.vue:324 to `'#embrace .observe-fade'`, and make the reveal additive — apply `.observe-fade` from script on mount so the no-JS state is fully visible.

#### `LOW` Logo images carry alt text that duplicates the adjacent visible label

**Where:** `docs/app/components/NavBar.vue`:8

**Evidence:**

```
NavBar.vue:7-10 is `<a href="#" class="flex items-center gap-3 group">` / `<img src="/logo.png" alt="Aster" class="w-8 h-8 rounded-lg …" />` / `<span class="text-lg font-semibold tracking-tight text-text-primary">Aster</span>` — the link's accessible name computes to "Aster Aster". Same pattern at FooterSection.vue:7-9 (`alt="Aster"` beside `<span>Aster</span>`), FooterSection.vue:58-59 (`alt="OpenAlly"` immediately followed by the text "OpenAlly"), and IntegrationsSection.vue:38-40 (`alt="OpenAlly"` inside a link whose h3 at :40 already reads "OpenAlly — zero-setup, fully on-device"). I confirmed by `grep -rn '<img' docs/app/components/` that these four plus the two screenshot `<img>` tags at ScreenshotsSection.vue:60 and :81 are the only images in the tree, so the fix is complete and small. NavBar.vue:7 `href="#"` is also an empty fragment.
```

**Why it matters:** 1.1.1 asks for an equivalent, not a duplicate; the logo link is the page's first tab stop, so the doubled announcement is constant friction.

**Fix:** Set `alt=""` on all four decorative logo images, and point NavBar.vue:7 at `#main` once the main landmark exists.

#### `LOW` Thirteen <section> elements and the <nav> carry no accessible name

**Where:** `docs/app/components/SecuritySection.vue`:2

**Evidence:**

```
None of the 13 `<section>` elements carries aria-label or aria-labelledby (`grep -rn 'aria-label' docs/app` returns only EmbraceSection.vue:222, the emoji). AuthorSection.vue:2 `<section class="relative py-24 px-6">` has no id either; HeroSection.vue:2, LiveChatSection.vue:2 and SecuritySection.vue:2 likewise have no id and no label; EmbraceSection.vue:2, FeaturesGrid.vue:2, HowItWorks.vue:2, IntegrationsSection.vue:2, ProactiveSection.vue:2, ScreenshotsSection.vue:2, SetupSteps.vue:2, ToolsShowcase.vue:2 and UseCasesSection.vue:2 have an id (the NavBar hash target) but no accessible name. NavBar.vue:2 has no aria-label and none of its seven links at :13-19 carries aria-current. CORRECTION: the original title said fifteen sections; there are 13 `<section>` elements plus one `<nav>` and one `<footer>`.
```

**Why it matters:** An unnamed `<section>` is not exposed as a region landmark, so it buys nothing over a `<div>`; combined with the missing `<main>` the page offers a screen-reader user essentially no landmark structure.

**Fix:** Give each section aria-labelledby pointing at an id on its own `<h2>`, add aria-label="Main" to the nav, and set aria-current on the active nav link using the scroll position already tracked at NavBar.vue:36-44.

#### `LOW` White captions sit over a fading gradient across 18 arbitrary screenshots, including white light-theme ones

**Where:** `docs/app/components/ScreenshotsSection.vue`:66

**Evidence:**

```
ScreenshotsSection.vue:66-68 and :87-89 are exactly `<div class="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent px-3 py-3">` wrapping `<span class="text-[10px] font-semibold uppercase tracking-[0.12em] text-white/90">{{ shot.label }}</span>`. Because the scrim gradient is on the same py-3 box as the text, the caption sits where the scrim is around 40–50% black. I computed white/90 over that scrim over a white image region: at 40% black the composite is #f5f5f5 on #999999 = 2.61:1 and at 50% it is #f2f2f2 on #808080 = 3.53:1 — both below 4.5:1. There are 18 images cycled here (appScreenshots :112-119, six; dashboardScreenshots :123-136, twelve), each in a dark and a light variant, and the light dashboard captures are predominantly white. The light src is derived by string replacement at :61 and :82 with no existence check, though the comment at :121-122 documents the invariant.
```

**Why it matters:** 1.4.3 applies to text over images and the ratio must hold over the actual background; with the light theme selected via the toggle at :36-47, 10px white captions over a fading scrim on a white UI capture fall well below AA.

**Fix:** Make the caption box itself a near-solid band (e.g. bg-black/75 on the caption box) rather than a gradient that fades through the text, so the ratio is independent of the image.

#### `LOW` Shared layer: toast/copy buttons below 24x24, AToggle off-state at 1.13:1, AStatCard focus ring on the wrong element

**Where:** `packages/aster-ui/app/components/AToastHost.vue`:34

**Evidence:**

```
All four sub-claims verified. AToastHost.vue:34 `<button type="button" class="toast__close" aria-label="Dismiss" @click="dismiss(t.id)">` with `.toast__close` at :93-99 declaring `border: none; background: none;` and no padding — the Icon renders as a 1em span at the inherited 16px root, giving ~16x16, under 2.5.8's 24x24, with no focus-visible rule. ACodeBlock.vue:29-32 `.code__copy` at :61-74 has `padding: 0.125rem 0.25rem` and `font-size: var(--text-label-sm)` (tokens.css:141-142 = 0.6875rem / 0.875rem line-height) → ~18px tall, also under 24; and it flips its own label between "Copy" and "Copied" at :30-31 with no live region (4.1.3). AToggle.vue:37-46 off-state track is `background: var(--color-surface-3)` (#1c1c34) with `border: 1px solid var(--color-border)` (#1a1a30); against an ACard at --color-surface-1 (#10101e) I measure the track at 1.13:1, the border at 1.02:1 against the track, and the thumb (--color-fg-muted #4a5670, AToggle.vue:55) at 2.26:1 against the track — all below the 3:1 of 1.4.11, exactly as claimed; focus at :71-74 and the checked state at :61-69 are handled correctly. AStatCard.vue:17-18 renders `<component :is="to ? resolveComponent('NuxtLink') : 'div'" … class="stat">` wrapping `<ACard :interactive="!!to">`, so the focusable element is the outer `<a class="stat">` while the only focus rule, ACard.vue:73-76 `.card--interactive:focus-visible`, lands on the inner non-focusable div. Scoped to mcp/dashboard; no A-prefixed component is used in docs/app.
```

**Why it matters:** These are the primitives the dashboard is assembled from, so each defect multiplies across every screen. The AToggle off state is the worst: a switch whose unchecked track cannot be distinguished from its container is the component that governs settings.

**Fix:** Give .toast__close and .code__copy `min-width/min-height: 24px` plus a :focus-visible ring, wrap the copy-state label in a polite live region, raise the AToggle off-track fill and border to clear 3:1 against --color-surface-1, and move ACard's focus-visible rule to the host (or add `.stat:focus-visible .card` in AStatCard).

### ui-ux

#### `HIGH` The "Install via direct link" URL is a hard 404 — wrong repo name

**Where:** `docs/app/components/IntegrationsSection.vue`:89

**Evidence:**

```
Line 89 reads verbatim: `<span class="text-text-secondary break-all">https://raw.githubusercontent.com/satyajiit/Aster/main/skill/SKILL.md</span>`. I ran `curl -sI -o /dev/null -w "%{http_code}" -L` on it: **404**. The same curl against `https://raw.githubusercontent.com/satyajiit/aster-mcp/main/skill/SKILL.md` returns **200**, and `ls aster-mcp/skill/` shows `SKILL.md` exists. Every other GitHub link on the page already uses `satyajiit/aster-mcp` (NavBar.vue:23, HeroSection.vue:59, SecuritySection.vue:144, SetupSteps.vue:27, FooterSection.vue:17/26/35).
```

**Why it matters:** One of the two documented install paths is dead. It also sits inside a `<pre>` with no copy control, so a visitor has to hand-type a URL that then 404s.

**Fix:** Change `satyajiit/Aster` to `satyajiit/aster-mcp` at IntegrationsSection.vue:89; add a link check to .github/workflows/deploy-docs.yml.

#### `HIGH` App-screenshot row is 1200px wide inside a ≤1152px container at every width ≥640px, with scrolling disabled and the overflow clipped

**Where:** `docs/app/components/ScreenshotsSection.vue`:53

**Evidence:**

```
Line 53: `<div class="flex justify-start sm:justify-center gap-4 sm:gap-6 overflow-x-auto pb-4 sm:pb-0 sm:overflow-x-visible snap-x snap-mandatory">`. Line 57 gives each card `flex-shrink-0` and line 58 pins `style="width: 180px;"`; `appScreenshots` (lines 112-119) has exactly 6 entries. Intrinsic row width at `sm:` and up = 6×180 + 5×24 = **1200px**. The wrapper is `max-w-6xl mx-auto` (line 6) = 1152px max, and the section is `px-6` (line 2) so it is never wider. At ≥640px the row loses BOTH `overflow-x-auto` and `justify-start`, so it is centred and overflows with no scrollbar — and the section itself carries `overflow-hidden` (line 2), with `html { overflow-x: hidden }` behind it (docs/app/assets/css/main.css:35). Because max-w-6xl caps at 1152 < 1200, this is true at EVERY viewport ≥640px, worst at 768px where the wrapper is 720px and 480px of the row (≈2.5 screenshots) is unreachable.
```

**Why it matters:** On every tablet and small-laptop width, the leftmost and rightmost app screenshots are clipped and cannot be scrolled to. This is the section whose entire job is showing the product.

**Fix:** Drop `sm:overflow-x-visible` and `sm:justify-center` so the row stays a scroller at all widths, or switch to a `grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6` with `w-full` cards.

#### `HIGH` All nav links are hidden below 768px with no mobile menu, no scroll-spy and no back-to-top

**Where:** `docs/app/components/NavBar.vue`:12

**Evidence:**

```
NavBar.vue is 45 lines end to end; I read all of them. Line 12 is `<div class="hidden md:flex items-center gap-8 text-sm text-text-secondary">` wrapping all 7 links (lines 13-19). There is no hamburger, `<details>`, drawer or `v-if` mobile branch anywhere in the file — the only script is a `scrolled` ref for the backdrop (lines 36-44). Line 29 is `<span class="hidden sm:inline">GitHub</span>`, so below 640px the sole nav control is an unlabelled icon. `grep -rni "back-to-top|backToTop|scrollspy" docs/app/` returns nothing (the original's `activeSection` grep hit was a false positive on the filename "ProactiveSection"), and no component contains an IntersectionObserver-driven active-link state — links carry only `hover:text-aster` (lines 13-19).
```

**Why it matters:** The site is one continuous page with 14 sections (docs/app/app.vue:4-17), 11 of them carrying `py-32`. On a phone there is no way to jump to Setup, Tools or Integrations, and no way back to the top from the footer.

**Fix:** Add a mobile drawer toggle in NavBar, an IntersectionObserver scroll-spy for active-link state, and a fixed back-to-top button past ~1 viewport.

#### `HIGH` One page, 14 sections, 11 of them at py-32, with no pagination, no tabs, no mobile nav and exactly one conversion CTA

**Where:** `docs/app/app.vue`:4

**Evidence:**

```
docs/app/app.vue:4-17 renders, in order: HeroSection, EmbraceSection, ProactiveSection, FeaturesGrid, ScreenshotsSection, UseCasesSection, LiveChatSection, SecuritySection, HowItWorks, SetupSteps, IntegrationsSection, ToolsShowcase, AuthorSection, FooterSection — 14 sections behind one NavBar, no `docs/app/pages/` directory. `grep -rn "py-32" docs/app/components/*.vue` returns exactly 11 hits (Embrace:2, FeaturesGrid:2, HowItWorks:2, Integrations:2, LiveChat:2, Proactive:2, Screenshots:2, Security:2, ToolsShowcase:2, SetupSteps:2, UseCases:2) = 256px of vertical padding per section. Verified card counts: FeaturesGrid 16 cards (FeaturesGrid.vue:28-125), UseCasesSection 7 groups × 3 = 21 cards (UseCasesSection.vue:100-304), ProactiveSection 6 scenarios × 3 steps (ProactiveSection.vue:281-372), ToolsShowcase 8 categories / 49 chips (ToolsShowcase.vue:45-159), SecuritySection 6 pillars (SecuritySection.vue:158-207), ScreenshotsSection 6+12 images (112-136). `grep -rn '#setup' docs/app/` returns exactly two hits: NavBar.vue:17 and HeroSection.vue:52 — the hero button is the page's only conversion CTA outside the nav. NOTE: I did NOT reproduce the original's ~19,200px / ~32,000px totals; those are unmeasured estimates and I could not verify them from the files or a browser, so they are removed.
```

**Why it matters:** SetupSteps (`id="setup"`, the conversion target) is section 10 of 14, reachable on desktop only via one nav link and on mobile not at all (see no-mobile-navigation-at-all). Everything between the hero and Setup is restated marketing copy (see the three repetition findings).

**Fix:** Merge the overlapping proof sections, drop `py-32` to `py-20 sm:py-28`, and add a jump affordance plus a repeat CTA before the footer.

#### `HIGH` The shared aster-ui layer is extended but not one A-prefixed component is used — install commands lose ACodeBlock's copy button

**Where:** `docs/nuxt.config.ts`:10

**Evidence:**

```
nuxt.config.ts:10 is `extends: ['../packages/aster-ui'],` and `ls packages/aster-ui/app/components/` returns AAnimatedEntrance, ABadge, AButton, ACard, ACodeBlock, AEmptyState, AGlowOrb, AIconTile, AModal, ASectionLabel, ASpinner, AStatCard, AStatusPill, AToastHost, AToggle. `grep -rnoE "<A(Card|Badge|Button|IconTile|SectionLabel|CodeBlock|StatusPill|AnimatedEntrance|GlowOrb|Toggle|Modal|StatCard|EmptyState|Spinner)" docs/app/` returns **zero matches**. Hand-rolled equivalents I read: card shell at FeatureCard.vue:2, ToolsShowcase.vue:21, IntegrationsSection.vue:21/51/72, SecuritySection.vue:27, EmbraceSection.vue:28/59/218, ProactiveSection.vue:196, UseCasesSection.vue:35, AuthorSection.vue:4; icon tiles at six different sizes — FeatureCard.vue:7 and SecuritySection.vue:31 (`w-10 h-10`), IntegrationsSection.vue:23 and ProactiveSection.vue:37/64/84 (`w-12 h-12`), HowItWorks.vue:79 (`w-14 h-14`), UseCasesSection.vue:24 (`w-7 h-7`), ToolsShowcase.vue:24 (`w-8 h-8`). The sharpest cost: all three install blocks are hand-rolled `<pre class="terminal">` with fake traffic-light dots and **no copy control** — SetupSteps.vue:31-39 (`npm install -g aster-mcp` at line 52 and the `.mcp.json` body at 80-87), IntegrationsSection.vue:61-68 (`clawhub install aster`) and 82-90 — while ACodeBlock exists in the layer. The layer's AButton.vue:72 (`.btn:focus-visible`) and ACard.vue:73 (`.card--interactive:focus-visible`) also carry focus rings the hand-rolled shells lack. Nothing here requires editing packages/aster-ui, so mcp/dashboard is unaffected.
```

**Why it matters:** The site pays the layer's build cost and gets none of its consistency, and the two strings a visitor must actually copy — the npm install line and the `.mcp.json` block — are not copyable.

**Fix:** Start with ACodeBlock for the three terminal blocks, then swap the card/tile/badge shells for ACard/AIconTile/ABadge. Consume the layer only; do not modify it.

#### `MEDIUM` One AI response string is byte-identical across two components, and the storage/duplicate figures disagree between three sections

**Where:** `docs/app/components/UseCasesSection.vue`:199

**Evidence:**

```
Byte-identical: UseCasesSection.vue:199 `response: 'Calling Mom now. Speakerphone on — I\'ll speak your message once she picks up.'` and LiveChatSection.vue:219 `text: 'Calling Mom now. Speakerphone on — I\'ll speak your message once she picks up.'`. Same storage anecdote, three different numbers: HeroSection.vue:202 `'WhatsApp media: 8.2 GB, cached app data: 3.1 GB, old APKs: 1.4 GB...'` and UseCasesSection.vue:176 `'Storage breakdown: WhatsApp media 8.2 GB, cached data 3.1 GB, old APKs 1.4 GB...'` versus LiveChatSection.vue:210 `'4.2 GB free out of 64 GB... WhatsApp media — 12.3 GB / Photos & videos — 18.7 GB / Cached data — 5.1 GB'`. Same duplicate-photos anecdote: HeroSection.vue:174 and UseCasesSection.vue:112 both say `47 duplicate sets`, LiveChatSection.vue:213 says `94 duplicate images (2.1 GB) and 23 duplicate videos (3.4 GB)`. The pet-cam anecdote appears at HeroSection.vue:213, EmbraceSection.vue:243/251/301, ProactiveSection.vue:289, UseCasesSection.vue:256 and FeaturesGrid.vue:86; the flight-delay anecdote at HeroSection.vue:220, EmbraceSection.vue:152, ProactiveSection.vue:256 and :334, UseCasesSection.vue:285 and HowItWorks.vue:345.
```

**Why it matters:** A reader who reads two sections sees the same phone described with two different storage breakdowns and two different duplicate counts. Severity lowered from high: these read as separate fictional devices rather than an outright contradiction, and nothing functional breaks.

**Fix:** Pick one canonical demo device and one canonical set of numbers; keep each anecdote in exactly one section.

#### `MEDIUM` EmbraceSection reprints the hero's rotating headline, gradient and all, one section later

**Where:** `docs/app/components/EmbraceSection.vue`:180

**Evidence:**

```
HeroSection.vue:33-37 (rotating H1 variant 2): `<span class="text-text-primary">Give your AI</span><br /><span class="bg-gradient-to-r from-amber-400 via-orange-400 to-amber-300 bg-clip-text text-transparent">its own phone</span>`. EmbraceSection.vue:180-183 (H3): `Give your AI<br /><span class="bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">its own phone.</span>` — same words, same amber→orange gradient. The same pitch also appears at HeroSection.vue:46 ("The CoPilot for your mobile — or give your AI a dedicated device and let it call, text, and act on its own."), EmbraceSection.vue:185-187, EmbraceSection.vue:203-205 ("You gave it WhatsApp. You gave it Telegram... Now give it a phone. Let it call you back.") and EmbraceSection.vue:102 ("Aster lets it touch your phone — or better yet, its own."). EmbraceSection.vue is 363 lines.
```

**Why it matters:** The product's single biggest idea is stated four times inside the first two sections. Severity lowered from high: this is editorial redundancy, not a functional defect, and the hero and Embrace treatments do play different roles.

**Fix:** Keep the phone-mock incoming-call visual (EmbraceSection.vue:114-211) and cut the before/after cards, blockquote and vignettes, or fold the visual into the hero.

#### `MEDIUM` 5 of ProactiveSection's 6 scenarios have a 1:1 twin card in UseCasesSection

**Where:** `docs/app/components/ProactiveSection.vue`:281

**Evidence:**

```
I read both scenario arrays in full. Pairs: 'Pet Cam' (ProactiveSection.vue:282-296, "Check on your dog while you're at work... sends them to your WhatsApp. Your pup is safe, and you have the pics to prove it.") ↔ UseCasesSection.vue:255-260 ("Left my dog home alone — take a photo every 30 mins and send it to me on WhatsApp" / "...Your pup is safe with me."); 'Smart Doorbell' (:297-311) ↔ UseCasesSection.vue:290-295; 'Flight Tracker' (:327-341) ↔ UseCasesSection.vue:284-289; 'Baby Monitor' (:342-356) ↔ UseCasesSection.vue:296-301; 'Smart Replies' (:357-371) ↔ UseCasesSection.vue:261-266. Only 'Ride Alerts' (:312-326) has no twin. UseCasesSection's last two groups, 'Proactive & Monitoring' (label at :247) and "AI's Own Phone" (label at :276), are entirely ProactiveSection's content re-cut as prompt/response cards.
```

**Why it matters:** Six ideas are told twice in two card shapes, separated by FeaturesGrid and ScreenshotsSection so the repeat reads as padding. Severity lowered from high: editorial redundancy, nothing broken.

**Fix:** Delete UseCasesSection.vue:246-303 (the two duplicate groups) or ProactiveSection's scenario grid (ProactiveSection.vue:191-232), keeping its flow diagram.

#### `MEDIUM` Nav order does not match DOM order, and 4 sections have no nav entry — 2 of them have no id at all

**Where:** `docs/app/components/NavBar.vue`:13

**Evidence:**

```
Nav order (NavBar.vue:13-19): features, use-cases, proactive, how-it-works, setup, integrations, tools. DOM order (app.vue:5-15): Embrace, Proactive, Features, Screenshots, UseCases, LiveChat, Security, HowItWorks, Setup, Integrations, Tools. So 'Proactive' (nav item 3) sits ABOVE 'Features' (nav item 1) in the document — clicking it after Features scrolls the user upward. Orphans: `id="embrace"` (EmbraceSection.vue:2) and `id="screenshots"` (ScreenshotsSection.vue:2) exist but have no nav link; LiveChatSection.vue:2 is `<section class="relative py-32 px-6 overflow-hidden">` and SecuritySection.vue:2 is `<section class="relative py-32 px-6 overflow-hidden">` — **neither carries an id**, so neither can be linked or deep-linked from anywhere.
```

**Why it matters:** The nav misrepresents the page order, and the two sections a buyer would most want to share — the screenshots and the security/privacy story — have no addressable anchor.

**Fix:** Reorder the nav to DOM order and add `id="live-demo"` to LiveChatSection.vue:2 and `id="security"` to SecuritySection.vue:2.

#### `MEDIUM` All screenshot <img> tags lack width/height or aspect-ratio, and 2880px-wide dashboard PNGs are served into ~568px cells

**Where:** `docs/app/components/ScreenshotsSection.vue`:60

**Evidence:**

```
ScreenshotsSection.vue:60-65 — `<img :src="..." :alt="shot.label" class="w-full h-auto block transition-transform duration-500 group-hover:scale-[1.02]" loading="lazy" />` — no `width`, no `height`, no aspect-ratio; identical at lines 81-86. Measured with sips: all six app JPEGs are 1080×2340 rendered into 180px cards (≈390px tall, reserved as 0px until load). All twelve dashboard PNGs are 2880px wide (heights 1800/2680/2888/3336) rendered into a `grid-cols-1 md:grid-cols-2 gap-4` (line 75) inside max-w-6xl → ~568px cells, a 5× overfetch. `stat` on the dark dashboard set totals **3.49 MB** across 12 files (dashboard-overview 474,807 B; device-telemetry 451,229 B; device-screen-control 444,417 B); `du -sh` on the light set is another 3.4 MB. Lines 56 and 78 use `:key="shotTheme + shot.src"`, which forces a full remount and re-download of all 12 files when the Light toggle is pressed, with no skeleton or spinner in the template.
```

**Why it matters:** Zero-height boxes snapping to full height produces CLS on the one section visitors stop at, and pressing 'Light' blanks 12 cards while ~3.4 MB downloads with no placeholder.

**Fix:** Add explicit `width`/`height` (or `aspect-ratio` on the card), downscale the dashboard PNGs to ~1200px WebP, and render a skeleton while a theme's set loads.

#### `MEDIUM` LiveChatSection auto-plays its 15.7s scripted conversation on page load, seven sections above where it is visible, with no pause

**Where:** `docs/app/components/LiveChatSection.vue`:325

**Evidence:**

```
Lines 325-327: `onMounted(() => { runScript() })` — no IntersectionObserver, no visibility gate anywhere in the file. `runScript` (268-317) starts at `let cumulativeDelay = 500` (line 275) and adds each of the 13 script delays exactly once (400+800+1200+1500+1200+1800+1400+1000+1500+1200+1000+800+800 = 14,600), then sets `finished.value = true` at `cumulativeDelay + 600` (line 316) = **15,700 ms**. LiveChatSection is the 7th of 14 sections (app.vue:10), below six sections that each carry `py-32`. The only control is `<button v-if="finished" ... @click="replay">Replay conversation</button>` (lines 166-173) — no pause, no step control, invisible until the animation the user never saw has ended. Same pattern at HowItWorks.vue:373-375 (`onMounted(() => { play(0) })`) whose four phases finish at 3,800 ms (lines 367-370). The reduced-motion reset in packages/aster-ui/app/assets/css/tokens.css:250-259 sets `animation-duration`/`transition-duration` to 0.01ms and `animation-iteration-count: 1` — it cannot touch a `setTimeout` chain.
```

**Why it matters:** The section's own promise is "Watch a real conversation unfold" (line 14) and nobody ever does; they arrive at a finished transcript plus a replay button. Severity lowered from high: all the content is still rendered and a replay control exists.

**Fix:** Gate `runScript()` and HowItWorks `play(0)` behind an IntersectionObserver at ~30% visibility, add a play/pause control, and jump to the end state when `matchMedia('(prefers-reduced-motion: reduce)').matches`.

#### `MEDIUM` Two infinite setInterval loops in the hero with no pause control, immune to prefers-reduced-motion, re-centring the CTA every 5s

**Where:** `docs/app/components/HeroSection.vue`:262

**Evidence:**

```
HeroSection.vue:262-271 — `headlineInterval = setInterval(() => { headlineIndex.value = (headlineIndex.value + 1) % 2; ... }, 4000)`; lines 244-246 — `interval = setInterval(() => { currentIndex.value = (currentIndex.value + 1) % conversations.length }, 5000)` over the 8 entries at lines 169-226. Neither checks `prefers-reduced-motion`, and the tokens.css:250-259 reset only neutralises CSS animation/transition durations — it cannot stop a JS timer. The only controls are the 8 dots at lines 126-132, whose `goTo(i)` (237-240) calls `resetInterval()` — it restarts the timer, it never pauses it. The chat area is `min-h-[140px]` (line 87) while responses run 1-3 lines at `text-[13px] leading-relaxed` (line 117), inside a `min-h-[100dvh] flex items-center justify-center` section (line 2), so the headline and CTA row re-centre vertically on every rotation. `headlineHeight` is measured via double-`nextTick` (lines 253-259 and 264-270) while the `mode="out-in"` leave transition (350ms, lines 304-313) is still running, and there is no resize listener to recompute it.
```

**Why it matters:** Auto-updating content with no pause mechanism is a WCAG 2.2.2 failure, and the periodic re-centring moves the 'Get Started' button under the user's finger every five seconds. Seeing all 8 conversations takes 40s.

**Fix:** Pause both intervals on `prefers-reduced-motion`, on hover/focus and when the tab is hidden; fix the chat block to the tallest response; add a visible pause control.

#### `MEDIUM` Hero carousel dots are 6×6px unlabelled buttons; no interactive control on the page has an accessible name beyond its visible text

**Where:** `docs/app/components/HeroSection.vue`:126

**Evidence:**

```
Lines 126-132: `<button v-for="(_, i) in conversations" :key="i" class="w-1.5 h-1.5 rounded-full transition-all duration-300 cursor-pointer" :class="i === currentIndex ? 'bg-aster w-4' : 'bg-text-tertiary/40 hover:bg-text-tertiary'" @click="goTo(i)" />` — `w-1.5 h-1.5` is 6×6px, the element is self-closing so it has no text content, and there is no `aria-label`, `title` or sr-only span. `grep -rn "aria-" docs/app/` returns exactly one hit in the entire site: EmbraceSection.vue:222 (`:aria-label="vignette.emojiLabel"` on a decorative emoji). The Screenshots tab pair (ScreenshotsSection.vue:20-31) and theme pair (36-47) are visual tabs with no `role="tablist"`/`role="tab"`/`aria-selected`, and the HowItWorks scenario pills (HowItWorks.vue:26-37) are the same.
```

**Why it matters:** Eight 6px targets against the WCAG 2.5.8 24×24 minimum, and eight buttons a screen reader announces as unnamed.

**Fix:** Give each dot an `aria-label` plus `:aria-current`, and wrap the 6px dot in a `p-2` button so the hit area is ≥24px; add tab roles to the two Screenshots switchers.

#### `MEDIUM` 18 Tailwind palette families, 6 raw hex surfaces at 9 sites, 10 hard-coded brand rgba literals and 9 arbitrary px font sizes bypass tokens.css

**Where:** `docs/app/components/EmbraceSection.vue`:119

**Evidence:**

```
I ran the counts myself. Raw hex surfaces (`grep -rno "\[#[0-9a-fA-F]{3,8}\]"` → 9 hits, 6 distinct values): EmbraceSection.vue:5 `via-[#0d0a12]`, :119 and :121 `bg-[#0c0c14]`, :123 `from-[#0f0f1a] to-[#0a0a12]`; ProactiveSection.vue:5 `via-[#0c0811]`; SecuritySection.vue:46 and :99 `bg-[#0c0c14]`; LiveChatSection.vue:33 `bg-[#0c0c12]` — while packages/aster-ui/app/assets/css/tokens.css:21-24 already defines `--color-bg:#06060c`, `--color-surface-1:#10101e`, `--color-surface-2:#161628`, `--color-surface-3:#1c1c34`. Hard-coded brand colour: `grep -rn "rgba(45,212,191"` returns **10** hits across HeroSection.vue (:6, :53), EmbraceSection.vue (:59, :112), ProactiveSection.vue (:6, :29), HowItWorks.vue (:9, :94), ScreenshotsSection.vue (:25) — `45,212,191` is `--color-primary: #2dd4bf` (tokens.css:32) written out longhand. Palette families in use: violet 66, green 61, amber 58, rose 52, sky 35, blue 32, orange 28, fuchsia 20, red 18, pink 15, teal 13, purple 6, cyan 5, zinc 4, yellow 4, lime 4, indigo 3, emerald 1 — **18 families**, against tokens.css:40-51 which provides semantic success/warning/error/info. Arbitrary type sizes outside the 15-step scale at tokens.css:77-144: `text-[7px]` ×2 (LiveChatSection.vue:77, :93), `text-[8px]` ×6 (incl. EmbraceSection.vue:162, ProactiveSection.vue:225), `[9px]` ×23, `[10px]` ×32, `[11px]` ×27, `[12px]` ×4, `[13px]` ×8, `text-[13.5px]` (UseCasesSection.vue:46), `text-[15px]` ×2 (incl. ProactiveSection.vue:212). (I dropped the original's WCAG claim — WCAG has no minimum-font-size criterion — but 7-8px body text is below any practical legibility floor.)
```

**Why it matters:** The brand colour cannot be changed in one place despite tokens.css being the stated single source of truth, and the 18-family rainbow is why the page reads as a different product every two sections.

**Fix:** Replace raw hexes with `bg-surface`/`bg-surface-raised`/`bg-surface-overlay`, replace `rgba(45,212,191,…)` with `color-mix(in oklab, var(--color-primary) N%, transparent)`, cap the palette, and map every `text-[Npx]` onto the `--text-label-*` / `--text-body-*` scale with an 11px floor.

#### `MEDIUM` Four container widths, four eyebrow variants, two h2 variants and three header margins across 14 sections

**Where:** `docs/app/components/HowItWorks.vue`:15

**Evidence:**

```
Verified by grep across all components. Container widths: `max-w-3xl` — SetupSteps.vue:3, AuthorSection.vue:3; `max-w-4xl` — HeroSection.vue:14; `max-w-5xl` — EmbraceSection.vue:9, SecuritySection.vue:10, HowItWorks.vue:12, IntegrationsSection.vue:5, ToolsShowcase.vue:5; `max-w-6xl` — NavBar.vue:6, FeaturesGrid.vue:3, ScreenshotsSection.vue:6, UseCasesSection.vue:7, LiveChatSection.vue:6, ProactiveSection.vue:9, FooterSection.vue:3. Eyebrow, 4 variants: (a) `text-xs font-semibold uppercase tracking-[0.2em] text-aster mb-4 block` in 8 sections (FeaturesGrid.vue:6, ScreenshotsSection.vue:9, UseCasesSection.vue:10, LiveChatSection.vue:9, SecuritySection.vue:13, SetupSteps.vue:5, IntegrationsSection.vue:7, ToolsShowcase.vue:7); (b) same + `font-mono` + violet at EmbraceSection.vue:12; (c) same + `font-mono` + fuchsia at ProactiveSection.vue:12; (d) `text-[10px] font-mono uppercase tracking-[0.3em] text-aster/70` at HowItWorks.vue:15 — different size AND tracking. H2, 2 variants: `text-3xl sm:text-4xl font-bold tracking-tight text-text-primary` in 9 sections vs `text-3xl sm:text-4xl lg:text-5xl ... leading-[1.15]` at EmbraceSection.vue:13 and ProactiveSection.vue:13. Header margin: `mb-16` ×8, `mb-20` (EmbraceSection.vue:11, UseCasesSection.vue:9), `mb-14` (HowItWorks.vue:14). Section padding: `py-32` ×11, `py-24` (AuthorSection.vue:2), `py-16` (FooterSection.vue:2).
```

**Why it matters:** Content edges jump between consecutive sections (max-w-4xl hero → max-w-5xl embrace → max-w-6xl proactive → max-w-6xl features → max-w-5xl security) and the eyebrow changes typeface, size and tracking mid-page.

**Fix:** Pick one container, extract a single `<SectionHeader eyebrow title subtitle>` (or use ASectionLabel), and one `py-24` rhythm.

#### `MEDIUM` The page's only conversion CTA is in the hero; the last content block before the footer is a YouTube subscribe card

**Where:** `docs/app/components/AuthorSection.vue`:17

**Evidence:**

```
`grep -rno 'href="#[a-z-]*"' docs/app/` returns 9 hits total: 7 nav links (NavBar.vue:13-19), the logo's `href="#"` (NavBar.vue:7), and exactly one content CTA — HeroSection.vue:52 `href="#setup"`. AuthorSection is section 13 of 14 (app.vue:16) and its only action is `<a href="https://youtube.com/@GamesPatch" ...>Subscribe to @GamesPatch</a>` (AuthorSection.vue:17-26). SetupSteps (`id="setup"`, SetupSteps.vue:2) is section 10, and its install commands sit in hand-rolled `<pre class="terminal">` blocks (SetupSteps.vue:38, IntegrationsSection.vue:67 and :88) with no copy control.
```

**Why it matters:** A visitor who scrolls the whole page is handed a YouTube subscribe button rather than an install path; one who does not scroll past the hero never sees the install path at all.

**Fix:** Add a compact install band (npm command + APK link, both with ACodeBlock copy buttons) immediately before the footer and demote AuthorSection to a footer line.

#### `MEDIUM` 81 <Icon> call sites resolve from api.iconify.design after hydration — confirmed against the built output, which ships zero inline SVG

**Where:** `docs/nuxt.config.ts`:14

**Evidence:**

```
nuxt.config.ts:14 is `modules: ['@nuxt/fonts', '@nuxt/icon']` with no `icon: {}` block in docs/nuxt.config.ts or packages/aster-ui/nuxt.config.ts (grep for `icon:` in both returns nothing). `@iconify-json/lucide` and `@iconify-json/mdi` are **devDependencies** in docs/package.json. The build is `nitro.preset: isGitHubPages ? 'github-pages' : undefined` with `prerender.routes: ['/']` (nuxt.config.ts:78-84). `grep -ro "<Icon" docs/app/ | wc -l` = **81**. I then inspected the committed build at docs/.output/public: `grep -o "<svg" index.html | wc -l` = **0**; the markup is `<span class="iconify i-lucide:bot text-xs text-amber-400" aria-hidden="true" style="">` with no glyph; `grep -c "i-lucide" _nuxt/*.css` = 0 in all three stylesheets, so no CSS mask fallback exists either; there is no `_nuxt_icon` or `api` directory in the output; and `_nuxt/ZUg0zPNB.js` (283 KB) contains the literal `https://api.iconify.design`.
```

**Why it matters:** Every icon on the page is an empty span until a third-party request to api.iconify.design resolves post-hydration — visible pop-in throughout, and a hard external dependency on a site whose pitch at SecuritySection.vue:18 is "No cloud, no telemetry, no third-party relay" and whose pillar at SecuritySection.vue:169 is 'Zero Telemetry'.

**Fix:** Add `icon: { clientBundle: { scan: true, sizeLimitKb: 512 }, provider: 'none' }` to docs/nuxt.config.ts and promote the @iconify-json packages to dependencies.

#### `MEDIUM` The 'Live Event Stream' truncates every message to one line and hides the tool column below 640px

**Where:** `docs/app/components/ProactiveSection.vue`:117

**Evidence:**

```
Line 117: `<p class="text-xs text-text-secondary mt-0.5 truncate">{{ event.message }}</p>` — Tailwind's `truncate` is `overflow:hidden; text-overflow:ellipsis; white-space:nowrap`, so the message can never wrap. The messages are `Mom: "Running late, can you pick up groceries on the way?"` (line 246, 56 chars) and `Samsung Galaxy S25 connected — Android 16, battery 87%` (line 276, 53 chars), inside a row whose inner column at 375px is roughly 275px wide at 12px text. Line 119: `<div class="hidden sm:flex items-center gap-1.5">` wraps the arrow and `{{ event.action }}` (line 121) — the `send_sms → reply sent` / `make_call_with_voice` half is removed entirely below 640px.
```

**Why it matters:** The widget exists to show 'event in → tool out'. On a phone the event text is cut mid-sentence and the tool half is gone, so it conveys nothing.

**Fix:** Replace `truncate` with `line-clamp-2` and move `event.action` below the message as a chip instead of hiding it at `sm`.

#### `LOW` No scroll-padding-top anywhere — latent, currently masked by py-32

**Where:** `docs/app/assets/css/main.css`:33

**Evidence:**

```
main.css:33-36 is `html { scroll-behavior: smooth; overflow-x: hidden; }` with no `scroll-padding-top`. `grep -rn "scroll-margin|scroll-padding|scroll-mt" docs/app/ packages/aster-ui/app/` returns **zero hits**. NavBar.vue:2-4 is `fixed top-0 left-0 right-0 z-50` and its inner container is `h-16` (line 6) = 64px, turning opaque (`bg-surface/80 backdrop-blur-xl border-b`) once `window.scrollY > 20` (lines 39-41). Every hash target starts with `py-32` (128px top padding), so the 64px the nav covers is currently empty padding and no heading is occluded today. Severity lowered from medium to low on that basis.
```

**Why it matters:** Purely latent now, but any reduction of `py-32` (which several other findings recommend) turns the missing offset into visible occlusion of every section heading.

**Fix:** Add `scroll-padding-top: 5rem;` to the `html` rule at main.css:33 before touching the section padding.

#### `LOW` Card accent lines and lifts are hover-only, and the 49 tool chips advertise a hover affordance while declaring cursor-default

**Where:** `docs/app/components/ToolsShowcase.vue`:33

**Evidence:**

```
ToolsShowcase.vue:33: `class="px-2.5 py-1 rounded-md bg-surface border border-border-dim text-xs font-mono text-text-tertiary hover:text-aster hover:border-aster/20 transition-colors cursor-default"` — all 49 chips brighten and change border on hover while `cursor-default` declares them non-interactive; on touch they do nothing. Accent lines that only exist on hover: UseCasesSection.vue:38 `opacity-0 group-hover:opacity-100`, ProactiveSection.vue:200, FeatureCard.vue:4, SecuritySection.vue:29. Hover-only lifts: UseCasesSection.vue:313-316 (`translateY(-2px)`) and ScreenshotsSection.vue:145-148 (`translateY(-3px)`). Severity lowered from medium: the per-group colour is NOT lost on touch — UseCasesSection.vue:23-28 renders an always-visible coloured icon tile and label above each group's card row, and each card's response icon at line 54 carries `group.color`.
```

**Why it matters:** Touch users get a flatter page than desktop users, and the tool chips promise interactivity they do not have.

**Fix:** Show the accent line at low opacity by default, or gate hover-only treatments behind `@media (hover: hover)`. Remove the hover colour change from the `cursor-default` chips.

#### `LOW` EmbraceSection and ProactiveSection render at opacity:0 until JS runs; Embrace's observer selector is unscoped

**Where:** `docs/app/components/EmbraceSection.vue`:331

**Evidence:**

```
EmbraceSection.vue:331-336 (scoped style): `.observe-fade { opacity: 0; transform: translateY(24px); transition: ... }`, with `is-visible` added only by JS at line 315 (`el.classList.add('is-visible')`). Identical block at ProactiveSection.vue:399-404 / :383. EmbraceSection.vue:324 is `document.querySelectorAll('.observe-fade').forEach((el) => observer.observe(el))` — no `#embrace` scope — whereas ProactiveSection.vue:392 correctly uses `'#proactive .observe-fade'`. CORRECTION to the original: the duplicate observation is **inert**, not a mis-delay. Both callbacks read `el.dataset.delay` from the element itself (EmbraceSection.vue:313, ProactiveSection.vue:381) and both observers use the identical `{ threshold: 0.15, rootMargin: '0px 0px -40px 0px' }`, and the `.observe-fade` CSS is Vue-scoped so there is no cross-component style leak. Severity lowered from medium to low accordingly.
```

**Why it matters:** On a static GitHub Pages build with JS blocked or IntersectionObserver unavailable, two of fourteen sections — including the whole 'Give your AI its own phone' pitch — render blank while every other section renders normally. The unscoped selector is a latent trap for any future section adopting the class.

**Fix:** Scope EmbraceSection.vue:324 to `'#embrace .observe-fade'`, and start at `opacity: 1`, applying the fade only once a JS-set root class is present.

#### `LOW` A sun/moon Dark/Light toggle sits mid-page but only swaps image paths; the site is hard-locked to dark

**Where:** `docs/app/components/ScreenshotsSection.vue`:35

**Evidence:**

```
ScreenshotsSection.vue:36-47 renders a `Dark | Light` pill pair from `themes` (lines 107-110, icons `lucide:moon` / `lucide:sun`) styled with the same `bg-aster/10 border-aster/30 text-aster` active treatment as the 'Android App | Web Dashboard' tabs directly above at lines 20-31. It only rewrites paths: line 61 `:src="shotTheme === 'light' ? shot.src.replace('/app/', '/app/light/') : shot.src"` and line 82 for the dashboard. Meanwhile nuxt.config.ts:25 hard-codes `htmlAttrs: { lang: 'en', 'data-theme': 'dark' }`, which permanently defeats both light paths in tokens.css — `:root[data-theme='light']` (line 166) and `@media (prefers-color-scheme: light) { :root:not([data-theme='dark']) }` (lines 185-186). `AToggle.vue` exists in packages/aster-ui/app/components/ and is unused. The section comment at line 34 even says "both the app and the dashboard ship in both themes".
```

**Why it matters:** A sun/moon control reads as a site theme switch; pressing it changes no page chrome and instead blanks 12 cards while ~3.4 MB of light PNGs download, so it reads as a bug.

**Fix:** Label it 'Screenshot theme' and restyle it distinctly from the tabs, or make it a real site theme toggle by removing the hard-coded `data-theme: 'dark'`.

#### `LOW` HowItWorks connection beams are percentage-positioned on an overlay that does not track the 3-column grid, and never reach the left node

**Where:** `docs/app/components/HowItWorks.vue`:119

**Evidence:**

```
Line 118-119: `<div class="absolute top-7 left-0 right-0 pointer-events-none"><div class="relative mx-auto" style="width: 66.66%; left: 0%;">`; the beams are `absolute left-[8%] right-[54%]` (line 121) and `absolute left-[54%] right-[8%]` (line 133), overlaid on a `grid grid-cols-3 gap-4` (line 75) whose node tiles are a fixed `w-14 h-14` = 56px (lines 79, 92, 105). Working the geometry from those values: with grid width W, node centres are at (W−32)/6, W/2 and W−(W−32)/6, while the beam endpoints are fixed fractions of a 0.6666W box centred in W. At the max-w-5xl cap (HowItWorks.vue:12 → W ≈ 976 after `px-6` and `sm:p-6`) the left beam starts ≈30px to the right of the first tile's edge; at the 768px `md:` breakpoint the gap is ≈13px. It undershoots the left node at every width (the original's "at 768-900px they overshoot" is wrong). `left: 0%` on a `relative` box is also a no-op.
```

**Why it matters:** The diagram is the section's entire content and its connectors visibly fail to meet the left-hand node at every desktop width.

**Fix:** Make each beam a grid item spanning the gap between columns, or draw one `<svg preserveAspectRatio="none">` sized to the grid, instead of percentage offsets on a separately-sized overlay.

#### `LOW` A registered-custom-property conic gradient repaints forever, alongside 5 large blurred blobs and a fixed full-viewport noise overlay

**Where:** `docs/app/assets/css/main.css`:94

**Evidence:**

```
main.css:85-95 — `.gradient-border::before { ... background: conic-gradient(from var(--angle), transparent 40%, var(--color-aster) 50%, transparent 60%); mask: ...; animation: border-rotate 6s linear infinite; }` with `@property --angle { syntax: "<angle>"; initial-value: 0deg; inherits: false; }` at lines 72-76 — animating a registered custom property that drives a conic gradient forces a repaint of that element every frame, indefinitely, on or off screen. It wraps the hero card at HeroSection.vue:71. Infinite `animate-ping` at HeroSection.vue:18 and :81, ProactiveSection.vue:45 and :97, HowItWorks.vue:52. Infinite `call-pulse` / `call-pulse-delayed` at EmbraceSection.vue:138-139 with keyframes at EmbraceSection.vue:344-357. Blur layers: EmbraceSection.vue:6 `w-[800px] h-[800px] ... blur-[40px]`, ProactiveSection.vue:6 `w-[900px] h-[700px] ... blur-[60px]`, UseCasesSection.vue:5 `w-[800px] h-[800px] ... blur-[120px]`, LiveChatSection.vue:4 `w-[600px] h-[600px] ... blur-[100px]`, LiveChatSection.vue:23 `blur-3xl`, plus `backdrop-blur-xl` on the fixed nav (NavBar.vue:4). `.noise::before` (main.css:43-53) is a fixed full-viewport SVG-turbulence overlay at `z-index: 9999`, applied to the page root at app.vue:2. CORRECTION to the original: the reduced-motion block at tokens.css:250-259 sets `animation-iteration-count: 1 !important` alongside the 0.01ms duration, so these CSS animations DO stop under reduced motion — the JS timer findings are the ones it cannot reach.
```

**Why it matters:** A permanently repainting conic gradient plus five multi-hundred-pixel blur layers is a measurable battery and scroll-smoothness cost on the mid-range Android this product targets.

**Fix:** Toggle `animation-play-state: paused` on `.gradient-border` via an IntersectionObserver when the hero leaves the viewport, and cut the blob count.

### ia-routes

#### `HIGH` The "Install via direct link" recipe ships a raw URL that 404s — wrong repo name

**Where:** `docs/app/components/IntegrationsSection.vue`:89

**Evidence:**

```
IntegrationsSection.vue:89 renders, inside a copy-me terminal block: `<span class="text-text-secondary break-all">https://raw.githubusercontent.com/satyajiit/Aster/main/skill/SKILL.md</span>`. I fetched both forms: `curl -o /dev/null -w %{http_code} https://raw.githubusercontent.com/satyajiit/Aster/main/skill/SKILL.md` → **404**; the same path under the real repo name → **200**. `git remote -v` in aster-mcp/ is `https://github.com/satyajiit/aster-mcp.git`, and `skill/SKILL.md` exists at the repo root. A repo-wide grep shows line 89 is the ONLY occurrence of `satyajiit/Aster` anywhere in the tree — every other raw link (mcp/README.md:2, :8, :53, :67…) uses `satyajiit/aster-mcp`.
```

**Why it matters:** This is not an IA observation, it is a live broken install instruction on the section that carries the site's near-transactional intent. Anyone (or any agent) who copies the displayed URL to install the skill for OpenClaw/MoltBot/ClawBot gets a 404, with no fallback — the string is plain text in a `<pre>`, not a link, so there is not even a redirect chance. The original finding buried this inside a "medium" IA-classification entry; the 404 is the only defect in that entry and it outranks everything else in the lane after dep-07.

**Fix:** Change `satyajiit/Aster` to `satyajiit/aster-mcp` at IntegrationsSection.vue:89; the rest of the path (`main/skill/SKILL.md`) is already correct.

#### `HIGH` All 49 tool names are published without the `aster_` prefix the server actually registers

**Where:** `docs/app/components/ToolsShowcase.vue`:51

**Evidence:**

```
I extracted every tool string from ToolsShowcase.vue (8 categories, first `tools: [` at line 51) and diffed it against `name: '…'` in mcp/src/mcp/tools.ts: 49 page names, 49 server names, and EVERY server name is exactly `aster_` + the page name — zero unmatched on either side. mcp/src/mcp/tools.ts:303 `name: 'aster_list_devices'`, :460 `name: 'aster_take_screenshot'`, :647 `name: 'aster_make_call_with_voice'`; mcp/src/mcp/handler.ts dispatches on the same prefixed strings (`case 'aster_take_screenshot':` at line 139). No prefix-stripping or alias layer exists anywhere in mcp/src. docs/public/llms.txt lines 25-39 repeat the same 49 unprefixed names (`grep aster_ llms.txt` → no match). Same unprefixed form in HowItWorks.vue:296 `aiCall: 'take_screenshot()'`, :326 `make_call_with_voice({…})`, EmbraceSection.vue:249 and every UseCasesSection `tools:` tag.
```

**Why it matters:** llms.txt exists specifically so an agent can read the tool surface; an agent that follows it and calls `take_screenshot` gets tool-not-found, because the MCP server only ever registered `aster_take_screenshot`. The counts agree exactly (49 everywhere, matching tools.ts), which is precisely what makes the drift invisible on inspection. Five surfaces transcribe the names, so there is no single edit that fixes it.

**Fix:** Emit one tool catalogue from mcp/src/mcp/tools.ts carrying the registered id plus an optional display label, and have ToolsShowcase, the use-case tags, the HowItWorks traces and llms.txt read it; at minimum, llms.txt must publish the registered `aster_*` ids since that is the agent-facing artifact.

#### `MEDIUM` SecuritySection has no id and no nav entry — the trust content cannot be linked or navigated to

**Where:** `docs/app/components/SecuritySection.vue`:2

**Evidence:**

```
SecuritySection.vue:2 — `<section class="relative py-32 px-6 overflow-hidden">`, no `id` attribute (confirmed by dumping line 2 of all 17 components: only 9 carry ids). NavBar.vue:13-19 links `#features`, `#use-cases`, `#proactive`, `#how-it-works`, `#setup`, `#integrations`, `#tools` — no security entry. The section itself is substantial: H2 at line 15 "Your data never leaves your network", 6 pillars at line 158 (Self-Hosted, Zero Telemetry, Device Approval, Kill Switch, Fail-Closed App Policy, Owner-Approved Folders), 3 tailscalePoints at 209, 3 permissionLayers at 215, 3 safetyPoints at 245.
```

**Why it matters:** On a site whose only navigation is hash anchors, the section answering "does it send telemetry / does it need root / can it touch my banking app" is the one section with no way to reach or cite it — no nav link, no fragment, nothing. Two far less consequential sections (`#embrace`, `#screenshots`) do own ids. This is fixable today in two lines and does not depend on any route split. Severity corrected from high: nothing is broken, the content is simply unaddressable.

**Fix:** Add `id="security"` to SecuritySection.vue:2 and a matching nav entry in NavBar.vue alongside the existing seven.

#### `MEDIUM` LiveChatSection's 13-beat transcript never reaches the prerendered HTML

**Where:** `docs/app/components/LiveChatSection.vue`:201

**Evidence:**

```
LiveChatSection.vue:201-202 `const visibleMessages = ref<Message[]>([])` / `const visibleSteps = ref<Step[]>([])` — both start EMPTY. The template renders only from those refs: line 67 `<template v-for="(msg, i) in visibleMessages">`, line 141 `v-for="(step, i) in visibleSteps"`. They are populated exclusively by `runScript()` (line 268), which pushes each of the 13 `script` entries (line 206) through `setTimeout` chains, and `runScript()` is called only from `onMounted` (line 325). nuxt.config.ts:80-83 prerenders `routes: ['/']` and deploy-docs.yml:38 runs `pnpm nuxt generate`, so the shipped HTML contains an empty phone frame. The lost content is real product copy — line 210 "Your phone has 4.2 GB free out of 64 GB… WhatsApp media — 12.3 GB", line 216 "Found 94 duplicate images (2.1 GB)", line 219 make_call_with_voice narration.
```

**Why it matters:** 397 lines — the site's only full four-turn transcript, the most answer-shaped content it has — yield zero bytes of text to any non-JS fetcher, which on a static GitHub Pages build is the only artifact that exists. The section also has no `id` (line 2), so it cannot be linked either. This is verifiable from the file today, independent of any route split.

**Fix:** Render the first beat (or a `<noscript>`/visually-hidden static transcript) from `script` at build time so the text is in the generated HTML, then let the timers take over.

#### `MEDIUM` Six use-case cards tag event types as if they were callable tools

**Where:** `docs/app/components/UseCasesSection.vue`:259

**Evidence:**

```
UseCasesSection.vue:259 `tools: ['take_photo', 'send_sms', 'event_forwarding']`, :265 `tools: ['sms_event', 'send_sms']`, :271 `tools: ['notification_event', 'speak_tts']`, :288 and :294 `tools: ['notification_event', …]`, :300 `tools: ['take_photo', 'send_sms', 'notification_event']`. I diffed every tool string on the page against mcp/src/mcp/tools.ts: `aster_event_forwarding`, `aster_sms_event` and `aster_notification_event` do not exist — the server registers exactly 49 `aster_*` tools and none of them is these. The template renders every entry of `tools[]` in identical monospace chip styling, so the three phantoms are visually indistinguishable from the 15 real tool names in the same array positions.
```

**Why it matters:** Six of the 21 cards present a feature name (`event_forwarding`, described at llms.txt:43 as a webhook feature, not a tool) and two event types as members of the tool surface. Severity corrected from high because the agent-facing artifact is clean — llms.txt lists only the 49 real names — so the blast radius is the rendered page and anything generated from it, not a live MCP client.

**Fix:** Make the tag arrays references into the real tool catalogue so a non-existent id cannot be typed, and render event types in a visually distinct chip.

#### `LOW` The '49 tools' literal is hand-typed in 8 places across 3 files

**Where:** `docs/app/components/ToolsShowcase.vue`:9

**Evidence:**

```
All eight verified by reading each line: ToolsShowcase.vue:9 "49 tools at your AI's fingertips"; FeaturesGrid.vue:8 "49 tools. Your phone or theirs."; IntegrationsSection.vue:42 "the full 49-tool surface locally"; nuxt.config.ts:28 (description) "49 tools, open source, self-hosted", :38 (og:description) "49 MCP tools", :46 (twitter:description) "49 tools"; llms.txt:3 "49 MCP tools" and :23 "## Tools (49 total)". The count is CORRECT today — mcp/src/mcp/tools.ts defines exactly 49 `aster_*` tools, and I counted 49 names in both ToolsShowcase and llms.txt.
```

**Why it matters:** Nothing is wrong right now, which is exactly why this is low and not medium: eight copies of a number owned by a file in a different package, edited by three different workflows (component, build config, hand-written static asset). The same transcribe-by-hand pattern is what produced the real dep-07 prefix drift, so the mechanism is demonstrated rather than hypothetical.

**Fix:** Interpolate the count from the extracted tool catalogue's length into copy, head meta and a generated llms.txt.

#### `LOW` The shared aster-ui layer is consumed for tokens only; its components are unused and their chrome is re-implemented

**Where:** `docs/app/assets/css/main.css`:10

**Evidence:**

```
nuxt.config.ts:10 `extends: ['../packages/aster-ui']`; main.css:6 imports tokens.css, :8 `@source "…/aster-ui/app/components"`, and :10-13 carries the comment "This site's components were written against the older token names. Rather than rewrite 17 components, alias the old names…" followed by the alias block at :14-30. `grep -rnoE '<A[A-Z][A-Za-z]+' docs/app/` returns ZERO matches, while packages/aster-ui/app/components ships 15 components (ACard, AButton, ABadge, ACodeBlock, AEmptyState, AGlowOrb, AIconTile, AModal, ASectionLabel, ASpinner, AStatCard, AStatusPill, AToastHost, AToggle, AAnimatedEntrance) and mcp/dashboard/app/components uses them throughout. I read each cited card shell: FeatureCard.vue:2, SecuritySection.vue:27, IntegrationsSection.vue:21/51/72, ToolsShowcase.vue:21, UseCasesSection.vue:35 all repeat `rounded-2xl … bg-surface-raised … border border-border-dim` verbatim. ACodeBlock.vue exists (with label + copy button) while the three-dot terminal chrome is hand-built at SetupSteps.vue:31-39, IntegrationsSection.vue:61-68 and :82-90, and ProactiveSection.vue:162-181.
```

**Why it matters:** Severity corrected from medium: the tokens ARE shared (the alias block is a deliberate, documented decision), so the "design system unused" framing overstates it — the real, verified cost is four hand-built copies of a code-block chrome that a shared component already provides, plus a back-compat alias layer that stays load-bearing. Any fix must stay inside docs/ — changing packages/aster-ui would hit mcp/dashboard, which consumes the same layer.

**Fix:** Adopt ACodeBlock for the four terminal blocks and ACard for the repeated shell, inside docs/ only; leave packages/aster-ui untouched.

#### `LOW` sitemap.xml is hand-written and no build step touches it — its lastmod is already stale

**Where:** `docs/public/sitemap.xml`:5

**Evidence:**

```
docs/public/sitemap.xml is 9 lines with one `<loc>https://aster.matterwardlabs.com</loc>` and `<lastmod>2026-09-01</lastmod>` (line 5). `git log -1 --format=%ci -- docs/` → 2026-09-06; `git log -1 --format=%ci -- docs/public/sitemap.xml` → 2026-09-01. .github/workflows/deploy-docs.yml:37-46 runs only `pnpm nuxt generate` and uploads `docs/.output/public` — there is no sitemap generation step, and nuxt.config.ts registers no sitemap module. robots.txt:4 points at this file.
```

**Why it matters:** Severity corrected from high: the single `<loc>` is CORRECT today — the site genuinely has one route — so the "new routes are invisible" half of the original claim is conditional on a split that has not happened, and `changefreq`/`priority` on lines 6-7 are inert either way. What is a present, verified defect is that the only URL's `lastmod` is already five days behind the content it describes and will drift further with every docs commit, because nothing regenerates it. An inaccurate lastmod is worse than none.

**Fix:** Either drop `lastmod` (and the inert `changefreq`/`priority`) or emit sitemap.xml from the build so the timestamp tracks the generated output.

### perf-assets

#### `CRITICAL` 8.84 MB of unoptimised PNG/JPEG screenshots are 88.6% of the deployed artifact, served 2.5-6x larger than any slot that displays them

**Where:** `docs/app/components/ScreenshotsSection.vue`:58

**Evidence:**

```
Re-measured independently against the committed tree and the current .output/public.

Artifact total: `find docs/.output/public -type f -exec stat -f %z {} \;` => 9,986,221 bytes over 83 files. Screenshots: `find docs/public/screenshots -type f` => 36 files, 8,844,115 bytes = 88.56%. Both figures match the original finding exactly.

Dimensions read with `sips -g pixelWidth -g pixelHeight` (not estimated):
  dashboard/*.png — all 2880 wide: dashboard-overview 2880x2680 474807 B | device-telemetry 2880x3336 451229 | device-screen-control 2880x1800 444417 | connect 2880x2888 352222 | logs 2880x1800 273956 | event-forwarding 242639 | panel-messages 238476 | panel-storage 237379 | panel-apps 215739 | device-registry 196938 | file-browser 194224 | mcp-tool-explorer 170907
  app/*.jpg — all 1080x2340: device-dashboard 191695 | permissions 167892 | connection-setup 155726 | on-device-mcp 149835 | services-logs 139418 | companion-overlay 70512

The over-delivery is structural and I read both call sites:
  ScreenshotsSection.vue:58  `style="width: 180px;"` — a 1080px-wide JPEG in a 180px box (6x at 1x DPR, 3x at 2x).
  ScreenshotsSection.vue:6   `class="relative max-w-6xl mx-auto"` (72rem = 1152px) and ScreenshotsSection.vue:75 `class="grid grid-cols-1 md:grid-cols-2 gap-4"` => (1152-16)/2 = 568 CSS px per cell, fed a 2880px-wide PNG (5x at 1x, 2.5x at 2x).

I re-ran the audit's cwebp claims rather than trusting them (`cwebp` is at /opt/homebrew/bin/cwebp) and got byte-for-byte the same outputs:
  dashboard/dashboard-overview.png 474807 -> `-q 80` 160102 -> `-q 80 -resize 1152 0` 45006
  app/device-dashboard.jpg 191695 -> `-q 80 -resize 540 0` 38388
The three spot checks reproduce the reported numbers exactly, so the whole-set 8,844,115 -> ~990,274 sweep is credible.

PNG is also the wrong container for 24 flat-UI captures: it is lossless, and GitHub Pages' gzip does nothing for already-entropy-coded PNG/JPEG.
```

**Why it matters:** On the only page the product has, a visitor who reaches the Screenshots section pulls 875 KB of phone JPEGs on the default tab and another 3.49 MB on one tab click, at 2.5-6x the resolution any slot can display. That is a direct LCP/INP cost on mobile data, and it is 88.6% of everything the deploy uploads.

**Fix:** Either add @nuxt/image with `provider: 'ipxStatic'` (derivatives are written into .output/public at generate time, so it stays server-free) and swap the two raw <img> for <NuxtImg format="avif,webp" sizes=...>, or pre-convert the 36 files to WebP at 1152w (dashboard) / 540w (app) in the capture tool and commit those.

#### `HIGH` Every @font-face in the shipped stylesheet is weight 400, while the design tokens declare 500/600/700 and 81 utilities request them — all bold text is synthesised

**Where:** `docs/nuxt.config.ts`:14

**Evidence:**

```
docs/nuxt.config.ts:14 is `modules: ['@nuxt/fonts', '@nuxt/icon'],` and there is no `fonts:` key anywhere in docs/nuxt.config.ts; packages/aster-ui/nuxt.config.ts (read in full) sets only `components:` and `imports:` and its comment says "@nuxt/fonts (which auto-provisions Instrument Sans / JetBrains Mono from the font-family declarations in tokens.css)". So the module runs on its default `weights: [400]`.

I parsed docs/.output/public/_nuxt/entry.DqDXcy71.css myself: 28 @font-face rules. 8 are metric-override fallbacks (size-adjust, no weight). The remaining 20 are real faces and every single one is `font-weight: 400` — 6 Instrument Sans (3 normal, 3 italic) and 14 JetBrains Mono. There is no 500, 600 or 700 face in the artifact. Dumped src per face: the latin woff2 for Instrument Sans is `_fonts/Wn2eY81…woff2` (17,410 B), for JetBrains Mono `_fonts/sqxfSu-14…woff2` (21,190 B).

What the source asks for, verified:
  packages/aster-ui/app/assets/css/tokens.css:80,85,95 `--text-display-lg/-md--font-weight: 700` and `--text-headline-lg--font-weight: 700`; :90,100,105,109 = 600; :114,119,134,139,144 = 500.
  `grep -ro` across docs/app + packages/aster-ui/app: font-semibold 44, font-medium 21, font-bold 16 = 81 call sites.
  tokens.css:74 `--font-display: 'Instrument Sans', …` and tokens.css:218 `font-family: var(--font-display);` put it on the base layer.
```

**Why it matters:** Every h1/h2/h3, nav item, badge and button label on the site is browser-synthesised bold over a 400 face — smeared stems, no optical compensation, and text metrics that no longer match the metric-override fallbacks @nuxt/fonts generated. The token file specifies 700 and the page can never render 700. Instrument Sans is a 400-700 variable font, so the correct weights are nearly free.

**Fix:** Add `fonts: { defaults: { weights: [400, 500, 600, 700], styles: ['normal', 'italic'], subsets: ['latin'] } }` to docs/nuxt.config.ts. This is a docs-only config key and cannot affect mcp/dashboard.

#### `MEDIUM` deploy-docs.yml triggers only on docs/**, so every change to packages/aster-ui — the layer the site extends and imports its tokens from — silently fails to redeploy

**Where:** `.github/workflows/deploy-docs.yml`:6

**Evidence:**

```
.github/workflows/deploy-docs.yml, read in full. Line 6 verbatim:
    paths: [docs/**, .github/workflows/deploy-docs.yml]
under `on: push: branches: [main]` (lines 3-5), plus `workflow_dispatch:` at line 7.

The layer is genuinely outside that path and genuinely load-bearing:
  docs/nuxt.config.ts:10  `extends: ['../packages/aster-ui'],`
  docs/app/assets/css/main.css:6  `@import "../../../../packages/aster-ui/app/assets/css/tokens.css";`
  docs/app/assets/css/main.css:8  `@source "../../../../packages/aster-ui/app/components";`
  packages/aster-ui/app/assets/css/tokens.css:74-75 hold --font-display / --font-mono, :80-144 the twelve --text-*--font-weight values, :166 the `:root[data-theme='light']` block and :218 the base font-family — i.e. the whole visual foundation the deployed page renders with.

The workflow's only other trigger is manual dispatch, so nothing catches the miss.
```

**Why it matters:** A design-token or A-component change lands green with the site still serving the old CSS and no failure anywhere. The next unrelated docs/** push then ships that change attributed to the wrong commit — the exact drift the shared layer was introduced to prevent.

**Fix:** Add `packages/aster-ui/**` to the `paths:` list at .github/workflows/deploy-docs.yml:6.

#### `MEDIUM` 4.48 MB of light-theme screenshots — 44.8% of the artifact — are addressed only by a runtime .replace(), invisible to every build-time optimiser and to the prerenderer

**Where:** `docs/app/components/ScreenshotsSection.vue`:61

**Evidence:**

```
The light tree is real and large: `find docs/public/screenshots -path "*light*" -type f` => 18 files, 4,476,104 bytes = 44.82% of the 9,986,221-byte artifact. All 18 are copied into .output/public (36 files land there).

They are never named as a literal path. Both references are string surgery, which I read at both sites:
  ScreenshotsSection.vue:61  :src="shotTheme === 'light' ? shot.src.replace('/app/', '/app/light/') : shot.src"
  ScreenshotsSection.vue:82  :src="shotTheme === 'light' ? shot.src.replace('/dashboard/', '/dashboard/light/') : shot.src"
Default state is dark — ScreenshotsSection.vue:100 `const shotTheme = ref('dark')`; the two buttons are at :36-47. The literal arrays at ScreenshotsSection.vue:112-136 contain only dark paths.

Consequences I verified rather than assumed:
  - `grep -c "light/" docs/.output/public/index.html` => 0. No light path is in the prerendered HTML, so `nitro.prerender.crawlLinks: true` (docs/nuxt.config.ts:81) cannot reach them.
  - Only the bare fragments survive into the bundle: `grep -o "app/light/\|dashboard/light/" _nuxt/*.js` => one occurrence each in ZUg0zPNB.js. No full filename exists anywhere a bundler or image pipeline could enumerate.

The theme-lock half of the original claim also checks out — docs/nuxt.config.ts:25 `htmlAttrs: { lang: 'en', 'data-theme': 'dark' }`, and `grep -rn "useColorScheme" docs/app` returns nothing, so packages/aster-ui/app/composables/useColorScheme.ts (the only mutator) is never called from the site. I am not treating a light screenshot inside a dark gallery as a defect in itself, only noting the toggle drives nothing but the image URL.

Severity lowered from high: the byte cost overlaps the screenshots finding, and shipping both themes of a product gallery is a defensible product choice. What is a defect is that the addressing scheme guarantees any future optimiser fixes exactly half the images.
```

**Why it matters:** Whoever installs an image pipeline will optimise the 18 dark files and leave the 18 light ones untouched, because no static tool can see them — so the two halves drift permanently, and the deploy keeps uploading 4.48 MB nothing can crawl.

**Fix:** Replace the two `.replace()` calls with an explicit `{ dark, light }` pair per entry in the arrays at ScreenshotsSection.vue:112-136 so both paths are statically visible, then decide separately whether the light tree earns its 4.48 MB.

#### `MEDIUM` The six screenshot <img> in the prerendered HTML carry loading="lazy" with no width/height and class "w-full h-auto" — zero reserved box, guaranteed layout shift

**Where:** `docs/app/components/ScreenshotsSection.vue`:60

**Evidence:**

```
CORRECTION to the original finding, which claimed "All 18 <img> elements in the prerendered HTML". `grep -o '<img[^>]*>' docs/.output/public/index.html` returns 10, not 18, and only 6 of those are unsized. The source has 6 <img> total (`grep -ro "<img" docs/app` => 6); 18 is the number ever rendered by ScreenshotsSection across both tabs, not the number in the HTML. packages/aster-ui/app has zero <img>.

What is genuinely unsized — the six app screenshots, emitted verbatim as:
  <img src="/screenshots/app/connection-setup.jpg" alt="Connection Setup" class="w-full h-auto block transition-transform duration-500 group-hover:scale-[1.02]" loading="lazy" data-v-2e83a94a>
source at ScreenshotsSection.vue:60-65, dashboard twin at :81-86.

The other four ARE sized, by Tailwind classes, so they do not shift: NavBar's `/logo.png` `class="w-8 h-8 rounded-lg…"`, FooterSection's `/logo.png` `w-8 h-8`, FooterSection's `/openally-mark.svg` `w-4 h-4`, IntegrationsSection's `/openally-mark.svg` `w-10 h-10 flex-shrink-0`.

Confirmed absent across the whole document: `grep -o '<img[^>]*\(width\|height\)=' index.html | wc -l` => 0; `grep -c fetchpriority` => 0; `grep -c 'decoding='` => 0; `grep -o 'aspect-[a-z0-9/[]*'` => empty.

Shift magnitude, from the real intrinsic sizes: the app card is fixed at 180px (ScreenshotsSection.vue:58) on a 1080x2340 source => 0 -> 390px per card, 6 cards. The dashboard cells are 568px on 2880x1800 => 0 -> 355px, and on connect.png (2880x2888) => 0 -> 570px.

Severity lowered from high: 6 shifts below the fold, not 18 across the page.
```

**Why it matters:** loading="lazy" plus a box with no intrinsic height is the textbook CLS generator — the fetch starts as the user scrolls toward it and the page reflows under their thumb, and scroll is not an excluded input for CLS. It also denies the browser any chance to lay the page out during the window in which these 70-475 KB files are still in flight.

**Fix:** Add `width`/`height` with the intrinsic pixels (1080x2340 for app, 2880x{1800|2680|2888|3336} for dashboard) to the two <img> at ScreenshotsSection.vue:60 and :81, or wrap each card in an `aspect-[1080/2340]` / `aspect-[2880/1800]` box. <NuxtImg> would emit them automatically.

#### `LOW` Two icons are missing from the prerender and will be fetched from api.iconify.design at runtime — but the claimed /api/_nuxt_icon 404 does not happen

**Where:** `docs/nuxt.config.ts`:14

**Evidence:**

```
REFUTED AS STATED, kept in corrected form. The original claim — "The resolved value is visible in the build output — docs/.nuxt/app.config.mjs:6-7 … \"provider\": \"server\"" — is false in the current build. I read docs/.nuxt/app.config.mjs lines 4-7 directly:
  "icon": { "provider": "iconify", "class": "", "aliases": {}, "iconifyApiEndpoint": "https://api.iconify.design", "localApiEndpoint": "/api/_nuxt_icon", "fallbackToApi": true, …
and the shipped bundle agrees — `grep -o 'icon:{provider:"[^"]*"' docs/.output/public/_nuxt/ZUg0zPNB.js` => `icon:{provider:"iconify"`. So the `_generate` guard at docs/node_modules/@nuxt/icon/dist/module.mjs:934-936 DOES fire, and the `/api/_nuxt_icon` string that appears once in the chunk sits inside the dead `if (options.provider === "server")` branch of runtime/plugin.js:9-13. No request to a nonexistent Pages endpoint will ever be made.

What survives, and I verified it myself: with provider "iconify" the plugin's else-branch does `resources.push(options.iconifyApiEndpoint)` — the single resource is https://api.iconify.design. And two icons are genuinely not prerendered: 89 distinct `lucide:`/`mdi:` names appear across docs/app (`grep -roh … | sort -u | wc -l` => 89) but only 87 `:where(.i-…)` rules are inlined in docs/.output/public/index.html (55,855 bytes across 87 rules). `comm` on the two sorted lists gives exactly: `lucide:rotate-ccw`, `lucide:search`.

Severity lowered from medium to low: no 404, no double round-trip — just two icons behind interaction that resolve from a third party.
```

**Why it matters:** When a visitor triggers whichever control renders lucide:search or lucide:rotate-ccw, the page contacts api.iconify.design — a third-party host, on a site whose pitch is self-hosted and private. The icon also pops in a round-trip late, and the failure path is a console.warn, so CI will never see it.

**Fix:** Set `icon: { provider: 'none', clientBundle: { scan: true }, fallbackToApi: false }` in docs/nuxt.config.ts so every icon found in source is bundled locally and no request can leave the page.

#### `LOW` The 220-entry Iconify collection-prefix list is serialised into the entry chunk for a site that uses two collections and fully-qualified names everywhere

**Where:** `docs/nuxt.config.ts`:14

**Evidence:**

```
I counted the array in docs/.nuxt/app.config.mjs myself: 220 entries ("academicons", "akar-icons", "ant-design", "arcticons", "basil", "bi", "bitcoin-icons", "bpmn", …). It reaches the client — `grep -c "academicons" docs/.output/public/_nuxt/ZUg0zPNB.js` => 1, and 0 in the other three chunks.

SIZE CORRECTED: the original said 4,138 bytes; re-serialising the 220 names as a JSON array gives 2,797 bytes. The 4,138 figure counts the pretty-printed indentation in .nuxt/app.config.mjs, which is not what ships.

The sole consumer is docs/node_modules/@nuxt/icon/dist/runtime/components/shared.js:32-43 (`useResolvedName`), which I read: `const collections = (options.collections || []).sort((a, b) => b.length - a.length)` runs once per Icon setup, and the prefix search only executes inside `if (!resolved.includes(":"))`. Every icon name in docs/app is fully qualified — all 89 distinct names carry a colon — so that branch can never fire. There are 81 <Icon> instances (`grep -roh "<Icon" docs/app` => 81).

Severity lowered from medium to low: ~2.8 KB of highly-compressible short strings in a 282,612-byte chunk, plus 81 sorts of a 220-element array. Real, but trivial.
```

**Why it matters:** Dead string data plus a per-instance sort on the critical-path bundle for a resolution path that cannot execute. One config line removes it.

**Fix:** Add `icon: { collections: ['lucide', 'mdi'] }` to docs/nuxt.config.ts — the module option exists to trim exactly this.

#### `LOW` No font preload — real text is three round-trips deep — and 156,592 bytes of non-subsetted .woff that no browser will ever request ship on every deploy

**Where:** `docs/nuxt.config.ts`:49

**Evidence:**

```
I listed every <link> in docs/.output/public/index.html: the entry stylesheet, a `preload as=fetch` for /_payload.json, a `modulepreload` for /_nuxt/ZUg0zPNB.js, three icons, one apple-touch-icon and the canonical. No `rel="preload" as="font"` and no `rel="preconnect"`. The `link:` array at docs/nuxt.config.ts:49-55 holds only the five icon/canonical entries. Chain confirmed: index.html -> entry.DqDXcy71.css (127,279 B) -> @font-face -> _fonts/Wn2eY81…woff2 (17,410 B, Instrument Sans latin) and _fonts/sqxfSu-14…woff2 (21,190 B, JetBrains Mono latin). All faces are font-display:swap, so FOUT not FOIT.

The superseded-.woff claim holds, and I checked the mechanism rather than the count. Dumping src + unicode-range per face from entry.DqDXcy71.css: the four .woff faces are declared FIRST (rule indices 0, 6, 11, 15) with NO unicode-range at all, and the woff2 latin subsets for the same family/weight/style are declared later (indices 7-10, 21, 26-27). Later-declared faces win the match for covered codepoints, so the .woff are unreachable. Exact sizes: 50,096 + 27,276 + 53,536 + 25,684 = 156,592 B.

TOTALS CORRECTED: _fonts/ is 20 files / 294,488 bytes, not the "336 KB" claimed. The remainder are cyrillic/greek/vietnamese woff2 subsets the site has no content for.

The audit's own mitigating note is also correct and should not be "fixed" — entry.DqDXcy71.css does carry 8 metric-override fallback faces with size-adjust/ascent-override, wired into --font-display, so the swap costs little CLS.

Severity lowered from medium to low: the 156,592 B is upload/storage cost only (never sent to a visitor), and the preload saves one hop on an LCP element that already has metric-matched fallbacks.
```

**Why it matters:** The LCP element is the hero <h1> (HeroSection.vue:25), which is text, so collapsing the font chain from three sequential fetches to one is the cheapest LCP move available here. The 156,592 bytes of unreachable .woff plus the unused non-latin subsets are re-uploaded on every deploy for nothing.

**Fix:** Add `fonts: { defaults: { subsets: ['latin'] } }` to docs/nuxt.config.ts to drop the non-latin faces, and add two `{ rel: 'preload', as: 'font', type: 'font/woff2', crossorigin: '' }` entries to the app.head.link array at nuxt.config.ts:49 for the two latin woff2.

#### `LOW` 24 prerendered content blocks ship at opacity:0 revealed only by JS, and EmbraceSection's observer query is unscoped so it double-observes ProactiveSection

**Where:** `docs/app/components/EmbraceSection.vue`:324

**Evidence:**

```
Both halves verified against the real artifact. `grep -o "observe-fade" docs/.output/public/index.html | wc -l` => 24; `grep -o "is-visible"` => 2, and those two are the CSS rule selectors, not applied classes. The shipped scoped CSS, read out of index.html verbatim:
  .observe-fade[data-v-db148901]{opacity:0;transform:translateY(24px);transition:opacity .7s cubic-bezier(.22,1,.36,1),transform .7s cubic-bezier(.22,1,.36,1)}
  .observe-fade[data-v-035356e6]{opacity:0;transform:translateY(24px);…}
So all 24 blocks of prerendered marketing copy are invisible until the entry chunk runs.

LINE NUMBERS CORRECTED (original said 325 / 393; both are off by one):
  docs/app/components/EmbraceSection.vue:324  document.querySelectorAll('.observe-fade').forEach((el) => {
  docs/app/components/ProactiveSection.vue:392  document.querySelectorAll('#proactive .observe-fade').forEach((el) => {
Only these two components use IntersectionObserver (`grep -c IntersectionObserver` across all 17 components in docs/app/components => 1 for each of these, 0 for the other 15), so EmbraceSection's unscoped query does pick up every ProactiveSection block as well and arms a second setTimeout per element.
```

**Why it matters:** If the 282,612-byte entry chunk is blocked, fails, or is merely slow, the majority of the page's body copy stays at opacity 0 — a hard content-visibility failure on a static site whose entire job is to be readable. The duplicate observation is wasted work and makes Proactive's reveal timing nondeterministic.

**Fix:** Scope EmbraceSection.vue:324 to its own section root the way ProactiveSection.vue:392 does, and add a `<noscript><style>.observe-fade{opacity:1;transform:none}</style></noscript>` (plus a prefers-reduced-motion rule) so the copy is visible without JS.

#### `LOW` LiveChatSection arms a ~15.7-second setTimeout sequence at hydration for a section six components below the fold, with no IntersectionObserver gate

**Where:** `docs/app/components/LiveChatSection.vue`:325

**Evidence:**

```
docs/app/components/LiveChatSection.vue:325-327 verbatim:
  onMounted(() => {
    runScript()
  })
`runScript()` at :268-315 walks the script array pushing setTimeouts at :283, :294 and :312 into `timeoutIds` (declared :258). I summed the script's delay values rather than trusting the figure: 0, 0, 400, 800, 1200, 1500, 1200, 1800, 1400, 1000, 1500, 1200, 1000, 800, 800 = 14,600 ms — so with the 500 ms base (`let cumulativeDelay = 500`) and the +600 ms finish timer the real tail is 15,700 ms, slightly longer than the 14,600 claimed. Each callback mutates visibleMessages/visibleSteps/isTyping and calls scrollToBottom() (:260-266), which reads `chatBody.scrollHeight` then writes scrollTop — a forced layout per message.

Position confirmed in docs/app/app.vue: NavBar(3), HeroSection(4), EmbraceSection(5), ProactiveSection(6), FeaturesGrid(7), ScreenshotsSection(8), UseCasesSection(9), LiveChatSection(10). HeroSection.vue:2 is `min-h-[100dvh]`, so LiveChat cannot be in the initial viewport.

No gate: `grep -c IntersectionObserver docs/app/components/LiveChatSection.vue` => 0, against 1 each for EmbraceSection and ProactiveSection. HowItWorks.vue:367-370 has the same unconditional shape (four setTimeouts at 250/1400/2600/3800 ms armed in onMounted at :373).

Severity lowered from medium to low: 31 timers whose per-callback work is an array push plus one scrollTop write is small, and only the first two or three land inside the hydration window.
```

**Why it matters:** Render, patch and forced layout for an invisible chat animation compete with the work that determines LCP and INP for content the user can actually see, and it runs to completion for every visitor who never scrolls that far.

**Fix:** Gate runScript() behind an IntersectionObserver on the section root, mirroring ProactiveSection.vue:375-393, and do the same for HowItWorks.vue:367-370.

#### `LOW` HeroSection runs two setIntervals for the life of the tab, one forcing an offsetHeight read every 4 s, and holds the LCP <h1> at opacity 0 for the first 100 ms

**Where:** `docs/app/components/HeroSection.vue`:262

**Evidence:**

```
docs/app/components/HeroSection.vue:242-247:
  function resetInterval() { if (interval) clearInterval(interval); interval = setInterval(() => { currentIndex.value = (currentIndex.value + 1) % conversations.length }, 5000) }
and :262-271:
  headlineInterval = setInterval(() => {
    headlineIndex.value = (headlineIndex.value + 1) % 2
    nextTick(() => { nextTick(() => { if (headlineRef.value) { headlineHeight.value = headlineRef.value.offsetHeight + 'px' } }) })
  }, 4000)
`offsetHeight` is a forced synchronous layout read, run every 4 s right after the <Transition name="headline"> at :26-38 swaps the <h1> children. Neither interval consults document.visibilityState or an IntersectionObserver; the only clears are in onUnmounted at :274-277, which on a single-page site never runs.

The LCP half also checks out. HeroSection.vue:25 is `<h1 class="animate-fade-up delay-100 …">`, and docs/app/assets/css/main.css:108-117 defines `@keyframes fade-up { from { opacity: 0; transform: translateY(24px) } … }` with :119-121 `.animate-fade-up { animation: fade-up 0.7s ease-out both; }` and :123 `.delay-100 { animation-delay: 100ms; }`. The `both` fill pins opacity at 0 for the first 100 ms of the element's life.

Severity lowered from medium to low: two timers and one small layout read every 4 s is a modest steady-state cost, and the LCP delay is bounded at ~100 ms. Both are real, neither is dramatic.
```

**Why it matters:** An <h1> re-render plus a forced layout every 4 seconds, forever, on every open tab keeps the main thread awake long after the user has scrolled past the hero. Separately, the LCP element is deliberately invisible for its first 100 ms for no visual benefit.

**Fix:** Pause both intervals on `document.visibilitychange` and when the hero leaves the viewport; cache the measured headline height once instead of re-reading offsetHeight each cycle; drop `delay-100` from the h1 at HeroSection.vue:25.

#### `LOW` 34 backdrop-blur elements, a continuously-animating @property conic-gradient and a fixed full-viewport feTurbulence overlay all render on one 1,718-element page

**Where:** `docs/app/assets/css/main.css`:43

**Evidence:**

```
Counts taken from the real docs/.output/public/index.html: backdrop-blur 34, blur-3xl 1, animate-ping 5, call-pulse 7, and 1,718 element open-tags. All four match the original finding exactly.

docs/app/assets/css/main.css:43-53, read verbatim:
  .noise::before { content: ""; position: fixed; inset: 0; z-index: 9999; pointer-events: none; opacity: 0.025; background-image: url("data:image/svg+xml,…feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4'…"); background-repeat: repeat; background-size: 256px 256px; }
applied at docs/app/app.vue:2 `<div class="noise">` — the root wrapper.
main.css:85-95 is `.gradient-border::before` with `background: conic-gradient(from var(--angle), …)` and `animation: border-rotate 6s linear infinite`, driven by the `@property --angle` registration at :76-80 — a main-thread, non-compositable repaint loop.
main.css:33-40 does put `overflow-x: hidden` on both html and body with `scroll-behavior: smooth` on html.

Six of the 34 blurs are the screenshot cards: ScreenshotsSection.vue:57 and :79 both carry `backdrop-blur-sm`, and :53 is the `overflow-x-auto` scroll container they sit in.

Severity lowered from medium to low: the element counts are solid, but the claimed consequence ("defeats the compositor's ability to promote and cache scrolling content, so each scroll frame can re-rasterise") is mechanism reasoning — I did not trace or profile it, and no measurement was offered.
```

**Why it matters:** backdrop-filter forces a snapshot-and-blur of everything painted behind the element, and an @property-driven conic-gradient repaints on the main thread every frame for 6 s on loop. On a mid-range Android — this product's audience — that is avoidable cost on a page that is otherwise static text and images.

**Fix:** Drop `backdrop-blur-sm` from ScreenshotsSection.vue:57 and :79 — those cards sit behind an opaque screenshot, so the blur is invisible. Replace the runtime feTurbulence data URI at main.css:50 with a small pre-rendered tiling PNG, and consider making .gradient-border static.

## Route map (adopted)

Chosen by a judge over three independent proposals (SEO-first 27 routes, user-first 6, docs-first 10).

| Route | Title | Primary query intent | Schema | Text twin |
|---|---|---|---|---|
| `/` | Aster — AI copilot for Android, or give your AI its own phone | what is aster mcp / android ai copilot / connect android phone to claude | SoftwareApplication, WebSite, Organization | `/index.md` |
| `/use-cases` | Aster use cases — what people actually ask their AI to do | what can an ai assistant do on my android phone / ai phone automation examples | CollectionPage, ItemList, BreadcrumbList | `/use-cases.md` |
| `/ai-phone` | Give your AI its own phone — dedicated Android, proactive events | give ai its own phone / can an ai call me / forward android notifications to an ai agent webhook | TechArticle, HowTo, ItemList, BreadcrumbList | `/ai-phone.md` |
| `/tools` | All 49 Aster MCP tools for Android | aster mcp tools list / android mcp server tools / aster_make_call_with_voice | ItemList, TechArticle, BreadcrumbList | `/tools.md` |
| `/security` | Is Aster safe? No root, self-hosted, zero telemetry | is aster mcp safe / does android mcp need root / why does aster need SMS permission / BIND_ACCESSIBILITY_SERVICE | TechArticle, FAQPage, BreadcrumbList | `/security.md` |
| `/setup` | Set up Aster — MCP server, Android app, AI client | aster mcp install / npm install -g aster-mcp / connect claude code to android / clawhub install aster | HowTo, SoftwareSourceCode, BreadcrumbList | `/setup.md` |
| `/architecture` | Architecture — how a command reaches the phone | how does aster work / how does mcp control an android phone / on-device mcp server ktor / mcp over binder ipc | TechArticle, BreadcrumbList | `/architecture.md` |
| `/faq` | FAQ and troubleshooting | aster device pending commands timeout / does aster need ssl / aster which ports / make_call_with_voice no audio / scrcpy vs mcp | FAQPage, BreadcrumbList | `/faq.md` |

**Nav order:** Use cases · AI phone · Tools · Architecture · Security · Setup · FAQ

### Rationale

BASE: the user-first proposal (6 routes). It is the only one of the three that answers the owner's actual complaint. I measured the landing page by section, not by line count: the current `/` is roughly 23 viewports of scroll (Hero 100dvh, EmbraceSection ~3, ProactiveSection ~3, UseCasesSection 21 cards ~3.5, LiveChatSection ~1.5, SecuritySection ~2, HowItWorks ~1.5, FeaturesGrid 16 cards ~1.5, ToolsShowcase ~1.5, SetupSteps ~1.5, IntegrationsSection ~1.5, ScreenshotsSection ~1, AuthorSection+Footer ~1). user-first's home lands at roughly 4.3 viewports and gets there by DELETING verified duplicates rather than by authoring replacement teaser copy. seo-first's home is a similar length but reaches it with nine newly-written teaser blocks — that is a new maintenance surface and a worse landing page.

I verified every duplication claim user-first made. The load-bearing ones are real: "lost phone / vibrate" appears at EmbraceSection.vue:264-271, HeroSection.vue:178-183 and UseCasesSection.vue:145-150; "find photos with Mom / Goa" at EmbraceSection.vue:288-295, HeroSection.vue:184-190 and UseCasesSection.vue:116-121; "pet cam" at EmbraceSection.vue:296-303, ProactiveSection.vue:282-296 and UseCasesSection.vue:255-260. FeaturesGrid.vue:101-106 ("Safety Rails") really is a summary of SecuritySection.vue:183-198, and FeaturesGrid.vue:107-112 is near-verbatim SecuritySection.vue:199-206. HowItWorks.vue:342-351 ("AI calls you") really is mechanically identical to :322-331 ("voice call") — same `aiCall` (make_call_with_voice), same `serverRoute` (MAKE_CALL_WITH_VOICE), same `deviceExec` (Intent(ACTION_CALL)), same ~10s latency. Those deletions are free.

GRAFTED FROM docs-first (the strongest technical proposal, and the only one whose README citations survive checking):
1. `/faq`. README.md:660-706 holds exactly 7 `<details>` troubleshooting entries and README.md:622-634 holds the scrcpy/ADB comparison table. None of it is published on the site. docs-first cited these ranges correctly; seo-first cited 697-737 and 739-748, which are wrong by 39 and 117 lines respectively. user-first omitted the route entirely. This is the single highest-value graft in the whole exercise and it is the only route that earns a real FAQPage.
2. `/architecture` as its own route rather than folded into `/setup`. HowItWorks.vue is 434 lines with six worked traces carrying real latencies; burying it under a HowTo about installing npm packages mismatches the intent. Pair it with README.md:454-471 (ASCII diagram) and README.md:473-483 (the three-transport table), neither of which the site publishes.
3. NO `/tools/<category>` sub-routes. docs-first's argument is correct and I confirmed the reason it matters: the tool taxonomy is not stable. README.md:436-452 groups the 49 tools into 15 categories, ToolsShowcase.vue:45-159 into 8, and apps/android/.../ToolCatalog.kt into 12 (categoryOrder at lines 16-29). Any URL keyed to a display category renames when the grouping does. Per-tool and per-category anchors on one `/tools` page give deeper deep-linking at zero route cost.
4. FeaturesGrid merged into `/tools` rather than a separate `/features`. I read both: FeaturesGrid.vue:29-34 "Screen Control" describes exactly ToolsShowcase.vue:46-60 "Screen & Input". Two URLs, one query intent. seo-first's `/features` and `/tools` compete directly.
5. The README security grafts onto `/security`: the honest TLS disclosure at README.md:489-496, the approval-gate trust boundary at README.md:498-506, and the 13-row permission-to-reason table at README.md:519-531. user-first's `/security` was SecuritySection alone, which was its thinnest route.
6. All four of docs-first's verified defect findings (see risks).

GRAFTED FROM seo-first:
1. The data-extraction prerequisite. Every product fact is trapped in `<script setup>` const arrays — ToolsShowcase.vue:45-159, FeaturesGrid.vue:28-125, UseCasesSection.vue:100-304, ProactiveSection.vue:238-372, SecuritySection.vue:158-249, HowItWorks.vue:291-352, SetupSteps.vue:47-89. No ItemList JSON-LD and no .md twin can be generated without hand-duplicating it. Lift to `docs/app/data/*.ts` first.
2. The `aster_` prefix defect, the 67-vs-49 surface split (**the real split is 77-vs-49** — see [The 67 that was actually 77](#the-67-that-was-actually-77)), the minSdk fact conflict, and the prerender opacity:0 defect — all four confirmed, all four in risks.
3. `/ai-phone` as a first-class route. docs-first loses this and it is its worst call: the headline itself alternates to it (HeroSection.vue:33-37), EmbraceSection.vue:107-211 is a 100-line block on it, UseCasesSection.vue:275-303 is a group literally named "AI's Own Phone", and README.md:113 is a Features bullet. It is the least contested positioning the product has.

REJECTED:
- seo-first's 27 routes. Thin at scale: 3,416 lines of components across 27 routes averages 126 source lines per route. Three routes are thin on inspection — `/integrations/openally` is IntegrationsSection.vue:32-46 (a 15-line card, ~60 words) plus one README table row; `/use-cases/spare-phone-camera` is three ProactiveSection cards plus one vignette plus three UseCases cards, ~500 words; `/tools/media-and-camera` is 4 tools. seo-first flags all three itself, which is the tell. Its README line citations are also systematically wrong (CLI cited at 352-366, actual 281-296; health checks at 370-377, actual 298-308; Claude config at 428-440, actual 390-403; scrcpy at 739-748, actual 622-634), so the four "new content" routes were designed against ranges that do not exist.
- seo-first's `/vs-scrcpy` as a standalone route. README.md:622-634 is a 5-row table plus one paragraph, ~250 words. It is a section of `/faq`, not a page.
- docs-first's `/screenshots` as a standalone route. ScreenshotsSection.vue is 164 lines of which ~25 are data arrays and ~25 are style; the indexable text is 18 image labels and a two-sentence header. It is a gallery, not an answer target — and removing it from `/` strips the only proof-of-existence from the landing page. It stays on `/`.
- seo-first's and user-first's deletion of AuthorSection.vue without qualification. user-first claims its payload is "already in FooterSection.vue:43-51" — I checked: the footer carries the YouTube LINK but NOT the author name. "Satyajit Pradhan" appears only in docs/nuxt.config.ts:30 and :68. docs-first has this right. The component goes, but the subscribe CTA is an audience decision, not a redundancy one, and needs owner sign-off.
- All three proposals' text-twin build mechanism except user-first's. See risk #2: the CI workflow does not run the package.json `generate` script.

### Section placement

**`/`** → docs/app/pages/index.vue

- NavBar.vue — rewritten: 7 NuxtLink routes replacing the hash links at NavBar.vue:13-19, logo href="#" at :7 becomes NuxtLink to="/", PLUS a mobile menu (NavBar.vue:12 is hidden md:flex with no fallback in 45 lines)
- HeroSection.vue intact (315 lines) — the 8-conversation carousel at :169-226 is the best asset on the site and is above the fold; only the CTA at :52 changes from href="#setup" to a NuxtLink
- EmbraceSection.vue REDUCED to :10-105 — section intro, the before/after card pair at :25-93, the pull-quote at :96-105. The 'own phone' block at :107-211 moves to /ai-phone; the six vignettes at :213-228 (data :255-304) are deleted as verified triplicates
- FeaturesGrid.vue TRIMMED to 8 of 16 cards — delete :77-82 (Proactive Events, a 3-line summary of the 410-line ProactiveSection), :101-106 (Safety Rails, duplicates SecuritySection.vue:183-198), :107-112 (Owner-Approved Folders, near-verbatim SecuritySection.vue:199-206); move :53-58, :89-94, :95-100 to /tools; move :113-118, :119-124 to /setup. Keeps Screen Control, File Management, Media Intelligence, Notifications & SMS, Audio & Haptics, Location & Battery, Calls/SMS/Voice, Camera & Video
- ScreenshotsSection.vue intact — the only proof the product exists; keeping it on / is why this landing beats a page of teasers
- FooterSection.vue plus one absorbed author sentence (the footer has the @GamesPatch link at :43-51 but NOT the author name, which lives only at docs/nuxt.config.ts:30 and :68)
- NEW: a compact 'Start here' link grid to the seven child routes — also guarantees crawlLinks reachability

**`/use-cases`** → docs/app/pages/use-cases.vue

- LiveChatSection.vue moved here intact and opens the page (397 lines; has NO id at :2 today, so it is unlinkable — this route gives it a URL). Script at :206-220, step summary at :222-255
- UseCasesSection.vue groups 1-6 (:100-274), minus the three cards the LiveChat script already tells beat for beat: :110-115 (duplicate photos), :174-179 (storage breakdown), :197-202 (call Mom). Group 7 'AI's Own Phone' (:275-303) moves to /ai-phone
- NEW: the Example Prompts block from README.md:310-332, which the site does not publish

**`/ai-phone`** → docs/app/pages/ai-phone.vue

- NEW AiOwnPhoneHero.vue extracted from EmbraceSection.vue:107-211 (the phone mockup with the incoming call) plus ownPhoneItems at :248-253 — this block is the page thesis
- ProactiveSection.vue moved here intact (410 lines): header + event-flow visualiser :10-128, live event stream data :238-279, the `aster set-openclaw-callbacks` terminal callout :130-189, all 6 scenario cards :281-372
- UseCasesSection.vue group 7 'AI's Own Phone' (:275-303) relocated here — flight call, doorbell clip, baby monitor
- NEW: the event table, Mattermost incoming-webhook steps and sample OpenClaw hook JSON from README.md:334-386, none of which the site publishes

**`/tools`** → docs/app/pages/tools.vue

- FeaturesGrid.vue merged in as the intro band, keeping id="features" so the old anchor resolves
- FeatureCard.vue
- ToolsShowcase.vue (keeps id="tools"; 8 categories, 49 names, verified to sum to 49: 7+5+6+4+6+3+10+8). Add :id to the category h3 at :27 and to each tool span at :30-36 so /tools#aster_make_call_with_voice and /tools#screen-input are permanent deep links
- The three FeaturesGrid cards demoted from home as a 'beyond the tool surface' strip: Shell Execution (:53-58), Companion Face (:89-94), App Automations (:95-100)
- NEW: the 15-row tool table from README.md:436-452, which carries the per-tool 'what it does' prose the chips lack — including the record_video max-8s cap (README.md:447) and the execute_shell 30s/1MB unprivileged sandbox (README.md:452)
- NEW: a one-line surface disclaimer distinguishing the MCP server's 49 tools from ToolCatalog.kt's 67-entry on-device set

**`/security`** → docs/app/pages/security.vue

- SecuritySection.vue moved here intact (272 lines): 6 pillars :22-38 with data at :158-207, the Tailscale/WireGuard card :42-93, the no-root card :95-129, the MIT open-source callout :132-152
- NEW: the honest ws:// TLS disclosure from README.md:489-496 — the Node ws server does not terminate TLS and pointing wss:// at :5987 fails with a parse error. The current section omits this entirely and the site is weaker for it
- NEW: the status-based approval gate and its trust boundary from README.md:498-506, including the fact that any client on the network can register as pending
- NEW: the 13-row permission-to-reason table from README.md:519-531 — each permission name is its own query and this is the objection that stops installs
- NEW: the on-device safety rails detail from README.md:533-538 (Kill Switch, PackagePolicyGuard fail-closed denylist, read-only actions always permitted)

**`/setup`** → docs/app/pages/setup.vue

- SetupSteps.vue moved here intact (keeps id="setup"; 4 steps at :47-89 with the terminal block at :52-58, the permissions list at :65-70, the APK link at :27 and the .mcp.json block at :80-87)
- SetupStep.vue
- IntegrationsSection.vue moved here (keeps id="integrations"): 4 client cards :17-29 with data :107-136, the OpenAlly Binder-IPC highlight :32-46, ClawHub install :50-69, direct raw-skill install :71-91. DROP the npm badge at :94-101 — SetupSteps.vue:52 already prints `npm install -g aster-mcp` on the same route
- The two FeaturesGrid cards demoted from home, both install-time decisions: Three Connection Modes (:113-118) and 'Ask All Together' (:119-124)
- NEW: the CLI command reference from README.md:281-296 and the health-check surfaces from README.md:298-308
- NEW: the topology table from README.md:419-430 — which URL goes where, and the rule of thumb that :5987 is for the phone, :5988/mcp for the AI client, :5989 for the browser

**`/architecture`** → docs/app/pages/architecture.vue

- HowItWorks.vue moved here (keeps id="how-it-works"; 434 lines): three-node flow :71-188, the live trace log :190-260, the protocol strip :264-274. DELETE the 'AI calls you' scenario at :342-351 — it is mechanically identical to 'voice call' at :322-331 (same aiCall, serverRoute, deviceExec and ~10s latency). 6 scenarios become 5
- NEW: the ASCII topology diagram from README.md:456-471
- NEW: the three-transport table from README.md:477-481 plus the shared-registry paragraph at :483 — remote WebSocket, on-device Ktor MCP server on :8080, and Binder IPC with 32-char token auth. 'On-device MCP server android' is a genuinely uncontested query and the site has never published this

**`/faq`** → docs/app/pages/faq.vue

- NEW FaqAccordion.vue rendering README.md:660-706 as native <details>/<summary> so the answers sit in the prerendered HTML rather than behind JS. Seven entries, verified: device pending / commands time out (:660-664), is the connection encrypted, do I need SSL (:666-670), disconnects on Wi-Fi switch (:672-676), make_call_with_voice dials but no audio (:678-682), firewall and which ports (:684-694), why no chat screen (:696-700), programmatic health check (:702-706)
- NEW: the 'Why not just scrcpy / ADB?' 5-row table and closing paragraph from README.md:622-634 — a section here, not a route of its own
- NEW: a Requirements block from README.md:652-656, with the Android minimum CORRECTED against apps/android/app/build.gradle.kts:16 (minSdk = 26, Android 8.0) rather than repeating the false 'Android 7.0+'

### Anchor compatibility

- #features -> / (NO shim; FeaturesGrid.vue:2 keeps id="features" on the trimmed 8-card grid that stays on /)
- #embrace -> / (NO shim; EmbraceSection.vue:2 keeps id="embrace" on the reduced before/after + pull-quote block that stays on /)
- #screenshots -> / (NO shim; ScreenshotsSection.vue:2 keeps id="screenshots", stays on /)
- #use-cases -> /use-cases (UseCasesSection.vue:2 keeps its id for in-page scroll)
- #proactive -> /ai-phone#proactive (ProactiveSection.vue:2 keeps id="proactive")
- #how-it-works -> /architecture#how-it-works (HowItWorks.vue:2 keeps id="how-it-works")
- #setup -> /setup#setup (SetupSteps.vue:2 keeps id="setup"; HeroSection.vue:52 href="#setup" becomes <NuxtLink to="/setup">)
- #integrations -> /setup#integrations (IntegrationsSection.vue:2 keeps id="integrations")
- #tools -> /tools#tools (ToolsShowcase.vue:2 keeps id="tools")
- MECHANISM: a fragment is never transmitted to the host, so no GitHub Pages rule could ever have handled these. One frozen const map in docs/app/pages/index.vue, executed in onMounted via router.replace() when location.pathname === '/'. Use replace, not assign, so Back does not bounce.
- EVIDENCE THE RISK IS LOW: I grepped every reference to aster.matterwardlabs.com across the repo — README.md:17, README.md:43, skill/SKILL.md:5, :13, :17, :335, mcp/README.md:44, :394, mcp/package.json:14, mcp/dashboard/app/layouts/default.vue:73, docs/public/llms.txt:9. ZERO carry a fragment; every one points at the bare origin. The only hash consumers are internal: NavBar.vue:13-19 and HeroSection.vue:52, both of which become NuxtLinks in this change. The shim is insurance for links pasted into chats and issues, not a load-bearing migration.

### Blockers and risks

- BLOCKER 1 — THE SOFT-404 SHELL POISONS ANY UNPRERENDERED ROUTE. Verified on disk: docs/.output/public/404.html and 200.html are 3,807 bytes each against index.html at 251,839 bytes, and both carry the global head — `rel="canonical" href="https://aster.matterwardlabs.com"` and `<meta name="robots" content="index, follow">`. Any of the eight routes not listed in docs/nuxt.config.ts:82 `prerender.routes` and not reachable by a crawled <a href> serves that empty shell under HTTP 404 with a canonical claiming to be the homepage. FIX: enumerate all eight paths explicitly in prerender.routes; do not trust `crawlLinks: true` at docs/nuxt.config.ts:81.

- BLOCKER 2 — THE CI WORKFLOW DOES NOT RUN THE package.json `generate` SCRIPT. .github/workflows/deploy-docs.yml:39 runs `pnpm nuxt generate`, not `pnpm generate`. All three proposals wire their .md-twin and sitemap generator into 'the generate script' (docs/package.json:8 is a bare `nuxt generate` with no pre/post hook). A script chained there would run locally and silently never run in CI, shipping a 1-URL sitemap and zero twins to production. FIX: put the generator in a `nitro:prerender` / `close` hook inside docs/nuxt.config.ts so it fires regardless of how generate is invoked, or change the workflow line — do not rely on an npm-script wrapper.

- BLOCKER 3 — NO MOBILE NAVIGATION EXISTS. docs/app/components/NavBar.vue:12 wraps the entire link set in `hidden md:flex`, and there is no hamburger, drawer, or fallback anywhere in the file (45 lines total). Today that is survivable because every target is a hash on one scrolling page. After the split a phone visitor sees only the logo and the GitHub button and has NO route to /setup, /tools or /faq. A mobile menu must ship in the same commit, not after it.

- BLOCKER 4 — EVERY HEAD TAG IS GLOBAL AND WILL BE INHERITED. docs/nuxt.config.ts:23-76 holds title (:24), description (:28), keywords (:29), all OG/Twitter tags, `og:url` (:40), `canonical` (:54) and the lone SoftwareApplication JSON-LD (:56-76). The moment docs/app/pages/ exists, all eight routes inherit a canonical and an og:url pointing at the homepage, which instructs Google to drop the seven new routes. The canonical and og:url lines must be DELETED from app.head and re-declared per route via useSeoMeta, not merely overridden. Note also docs/app/app.vue:22, which reads `// Head/SEO meta lives in nuxt.config.ts (single source of truth).` — rewrite that comment in the same change or the next editor restores the bug. Add `app.head.titleTemplate` so per-page titles do not lose the site name; only favicons (:50-53), theme-color (:32), og:site_name (:36) and htmlAttrs (:25) stay global.

- BLOCKER 5 — app.vue HAS NO <NuxtPage/> AND THE NOISE WRAPPER IS IN IT. docs/app/app.vue is 23 lines: a `.noise` div wrapping all 15 components directly. main.css:43 defines `.noise::before` as a full-viewport fixed overlay. Adding docs/app/pages/ enables the router, so app.vue must become `<NuxtLayout><NuxtPage/></NuxtLayout>` with the .noise wrapper, NavBar and FooterSection lifted into docs/app/layouts/default.vue — otherwise the overlay either disappears on the new routes or is duplicated eight times. vue-router@4.6.4 is already in docs/package.json:19, so no new dependency, but this is a hard cutover, not additive.

- PREREQUISITE — EVERY PRODUCT FACT IS TRAPPED IN A <script setup> CONST. ToolsShowcase.vue:45-159, FeaturesGrid.vue:28-125, UseCasesSection.vue:100-304, ProactiveSection.vue:238-372, SecuritySection.vue:158-249, HowItWorks.vue:291-352, SetupSteps.vue:47-89, ScreenshotsSection.vue:112-136, EmbraceSection.vue:234-304. Neither the ItemList/HowTo JSON-LD nor the .md twins can be generated without hand-duplicating this copy, which guarantees drift — docs/public/llms.txt:23-39 is already a second hand-maintained copy of ToolsShowcase.vue:45-159. Lift to docs/app/data/*.ts and import from both the .vue and the generator BEFORE splitting.

- NAME MISMATCH, SHIPPED SITE-WIDE — docs/app/components/ToolsShowcase.vue:52-157 lists all 49 tools UNPREFIXED (`take_screenshot`, `make_call_with_voice`), but mcp/src/mcp/tools.ts registers every one of the 49 with the `aster_` prefix (verified: `grep -c "name: 'aster_"` returns exactly 49; first is `aster_list_devices` at :303, last `aster_record_video` at :963). The name an MCP client actually shows a user is the exact-match query, and the site contains it zero times. docs/public/llms.txt:25-39 and README.md:438-452 have the identical defect. Every /tools heading must carry `aster_<name>` with the bare name as a visible alias.

- TOOL-COUNT CLAIM IS SURFACE-SPECIFIC — apps/android/app/src/main/java/com/aster/service/mode/ToolCatalog.kt holds exactly 67 unique tool names against the MCP server's 49. It exposes 23 the server never does (observe, perform, tap, scroll, long_press, set_text, press_key, wait_for, wait_for_idle, set_toggle, get_now_playing, companion_overlay_* ×4, screen_set_policy, dismiss_notification, dismiss_all_notifications, count_sms, hide_overlay, hide_all_overlays, list_overlays, click_by_view_id) and omits 5 the server does (list_devices, click_by_id, list_installed_apps, list_contacts_full, delete_contacts). README.md:122 already advertises `get_now_playing` as though it were in the set, and README.md:483 asserts all three connection modes 'share the same 49 tools'. '49' is true of the MCP server surface only. /tools and /architecture must say which surface they count or the claim is false in Local-MCP and IPC modes.

- FACT CONFLICT, ALREADY SHIPPING — apps/android/app/build.gradle.kts:16 sets `minSdk = 26` (Android 8.0), and README.md:644 itself says `minSdk 26 (Android 8)`. But README.md:655 says 'Android 7.0+', docs/public/llms.txt:5 says 'Android 7+' and llms.txt:48 says 'Android 7.0+'. The README contradicts itself. /faq and /setup must resolve against build.gradle.kts or they ship a false minimum-version requirement to people about to sideload an APK.

- PRERENDER VISIBILITY — CONTENT SHIPS AT opacity: 0. EmbraceSection.vue:331-335 and ProactiveSection.vue:399-403 both define `.observe-fade { opacity: 0 }`, and only an IntersectionObserver (EmbraceSection.vue:307-327, ProactiveSection.vue:375-395) adds `.is-visible`. The prerendered HTML of /ai-phone therefore ships its entire above-the-fold content invisible. Googlebot renders JS; several answer-engine fetchers read raw HTML. Render visible by default and let the observer add the animation, or gate the initial state on a `.js` class. Also scope EmbraceSection.vue:324 — it is `document.querySelectorAll('.observe-fade')` with no scope, while ProactiveSection.vue:392 correctly scopes to `#proactive .observe-fade`; lift both to docs/app/composables/useObserveFade.ts, NOT into packages/aster-ui, which mcp/dashboard consumes.

- SCREENSHOTS ARE LARGELY ABSENT FROM THE STATIC HTML. ScreenshotsSection.vue:61 and :82 derive light-theme paths with a runtime String.replace from a `shotTheme` ref defaulting to 'dark' (:100), images are `loading="lazy"` (:64, :86), and the inactive tab is behind a v-else inside a <Transition> (:74). Of the 36 image files on disk (6 app + 12 dashboard, each with a light twin under public/screenshots/*/light/), only the 6 dark app shots appear in the prerendered HTML. Keeping this on / is still the right call, but do not expect the images to carry any indexing weight, and note the alt text is just the label (`alt="Overview"`), which is not descriptive. A missing light variant also 404s silently with no fallback.

- SITEMAP AND llms.txt WILL UNDER-REPORT ON DAY ONE. docs/public/sitemap.xml holds exactly one <url> with `lastmod 2026-09-01` (lines 3-8) and is the only sitemap robots.txt:4 points Google at. docs/public/llms.txt is 54 lines with a hand-written 49-tool block at :23-39. Both must be regenerated from docs/app/data/*.ts in the same build step as the twins. Decide the trailing-slash form ONCE — GitHub Pages serves a prerendered route from setup/index.html, so /setup and /setup/ are both reachable — and use the same form in the sitemap, the canonicals and the nav, or the three disagree.

- GITHUB PAGES SERVES .md AS text/markdown. A human hitting /tools.md downloads the file rather than reading it. Fetchers and LLM crawlers are unaffected. There is no server, so no Content-Type override is possible — this is a naming decision, not a config one. Consider emitting .txt alongside .md if a human-readable plain-text twin matters.

- ONE OG IMAGE FOR EIGHT ROUTES. docs/public/og-card.png is referenced at docs/nuxt.config.ts:39 and :47. Eight routes sharing it means every shared link previews as the homepage. Either produce per-route cards or accept the collapse deliberately — do not leave og:image silently inherited while every other tag is per-route.

- OVER-CLAIM AUDIT BEFORE /ai-phone SHIPS. ProactiveSection.vue:292 ('Schedule triggers every 30 minutes') and :352 ('Periodic check every 15 minutes') describe scheduling Aster does not implement — the agent schedules, Aster exposes tools. UseCasesSection.vue:297-298 ('if it picks up crying sounds' / 'Audio monitoring active') implies audio monitoring; there is no audio-listening tool among the 49 in mcp/src/mcp/tools.ts. Attribute the scheduling to the AI client and cut or rewrite the crying-sounds card, or /ai-phone ships capability claims the product does not back.

- AuthorSection DELETION IS AN AUDIENCE DECISION, NOT A REDUNDANCY ONE. docs/app/components/AuthorSection.vue is 30 lines and its @GamesPatch href at :18 is duplicated at FooterSection.vue:43-51 — but the author NAME at :13 is not in the footer at all; it exists only at docs/nuxt.config.ts:30 and :68. Removing the component removes the only YouTube subscribe CTA above the footer. Confirm with the owner before cutting, and fold the author sentence into FooterSection.vue:6-12 in the same change.

- THE HASH SHIM ONLY RUNS AFTER HYDRATION. A crawler, a curl, or a JS-disabled browser hitting /#setup lands on / and scrolls nowhere. Unavoidable — fragments are never sent to any host. Do not attempt a meta-refresh on /, which would break the landing page for everyone. Mitigation is already structural: #features, #embrace and #screenshots keep working natively because those sections stay on /, and no external property links a fragment at all.

- ORPHAN — docs/DEVICE_EXECUTION_V2.md is neither under docs/public/ nor referenced by any component, so it is written but unpublished and will not be part of any route in this map. Either give it a home or move it out of docs/.

- NO CHANGE TO packages/aster-ui IS REQUIRED OR PROPOSED. I confirmed docs/app/ uses ZERO A-prefixed components from the shared layer — every section is hand-rolled Tailwind against the alias tokens at docs/app/assets/css/main.css:14-30. The per-route head/JSON-LD helper and useObserveFade must live in docs/app/composables/, never in the layer that mcp/dashboard consumes.

- SCOPE DISCIPLINE ON GEO CLAIMS. Nothing in this map treats llms.txt, the .md twins or any markup as a Google ranking input. The twins and the rewritten llms.txt are for non-Google answer engines and developer ergonomics. The Google-facing work is entirely per-route title, description, canonical, og tags and schema, plus getting eight real prerendered documents to exist where one exists today.

---

# Round 2 and 3 — implementation, corrections, and the verified artifact

Everything above is the **round-1 record**: findings as they were reported and refuted, before any
code was written. It is kept verbatim as a record. Two of its assertions were later disproved by the
implementation and are corrected below — see "The 67 that was actually 77". Read this section for the
state that shipped.

## The 67 that was actually 77

Round 1 reported the on-device action set as **67**, citing `ToolCatalog.kt` (67 entries). Findings
`llms-txt-flattens-per-mode-namespace` (line 929) and its fix (line 934) both state that number, and
the route map's "67-vs-49 surface split" repeats it. **All three are wrong.**

`ToolCatalog.kt` is **display metadata** — a lookup of human labels and categories used to decorate a
tool listing. It is not the registry. The registry is the handler map:

- `ModeModule.kt` `provideCommandHandlers()` builds the map from every handler's `supportedActions()`
- `McpToolRegistry.registerTools(commandHandlers.keys)` registers from *that* map, not from the catalogue
- `IpcMode.kt` returns `commandHandlers.keys` for the Binder path

Collecting `supportedActions()` across every handler yields **77** actions. The catalogue is missing
ten of them, which is exactly why counting it under-reports. Every audit lane in round 1 read the
catalogue, so every lane agreed on a wrong number — agreement across independent lanes is not
verification when they all read the same wrong file.

Getting to 77 took four passes, and each intermediate number was a parser defect worth naming:

| Count | Why it was wrong |
|---|---|
| 67 | Read `ToolCatalog.kt` (display metadata) instead of the handler map |
| 69 | A `sed` one-liner stopped at the first `)`, truncating `AccessibilityHandler`'s 18-name list |
| 73, 75 | Line-oriented regex truncated multi-line `listOf(...)` blocks |
| **77** | Paren-balanced scan; `[a-z0-9_.]+` so dotted actions (`files.read`, `files.list`) survive |

The 69 trap is the dangerous one because 69 looks plausible. Do not reproduce this count with a
shell one-liner. `docs/scripts/verify-facts.ts` re-derives it with a paren-balanced scanner and the
build fails if it drifts.

**The published arithmetic now closes**, which is the property that makes it checkable at all:

```
shared (48) + onDeviceOnly (29) === onDevice   (77)
shared (48) + serverOnly   (1)  === mcpServer  (49)
```

`shared: 48` is counted by **reachability, not by name**: 47 actions carry identical names on both
sides, plus `click_by_view_id`, which an MCP client reaches as `aster_click_by_id`
(`mcp/src/mcp/handler.ts`). `serverOnly: 1` is `aster_list_devices` — a server-side registry listing
that is meaningless on a device. Both assertions are enforced in `verify-facts.ts`; neither can drift
silently again.

## What was built

The single page became eight prerendered routes on a host with no server, no redirects and no headers.

| Area | Shipped |
|---|---|
| Routes | 8 (`/`, `/use-cases`, `/ai-phone`, `/tools`, `/architecture`, `/security`, `/setup`, `/faq`) plus a branded `/404` |
| Fact layer | `app/data/*.ts` — 12 modules; every product fact lifted out of `<script setup>` so twins and JSON-LD generate rather than duplicate |
| Machine-readable | `sitemap.xml`, `robots.txt`, `llms.txt`, `llms-full.txt`, 8 markdown twins — 12 files, all generated |
| Build gate | `scripts/verify-facts.ts` re-derives every published number from source and fails the build on drift |
| Structured data | SoftwareApplication, MobileApplication, WebSite, Organization, ItemList, TechArticle, HowTo, FAQPage, CollectionPage, BreadcrumbList, SoftwareSourceCode |

### Host defects the split exposed

Four traps, each of which would have shipped silently:

1. **GitHub Pages 301s a directory path without a trailing slash.** Verified empirically against other
   Pages sites (`microsoft.github.io/monaco-editor/typedoc` → 301 → `/typedoc/`). `/tools` is not a URL
   this site serves — it is a redirect to one. Declaring it canonical pointed every canonical, sitemap
   `<loc>` and `og:url` at a redirect. `href()` in `app/data/routes.ts` is the fix: router paths for
   matching and prerendering, slashed hrefs for anything published.
2. **The github-pages preset writes `200.html` and `404.html` *after* prerendering.** Prerendering
   straight to `/404.html` does not work — the fallback write lands last and overwrites it. The route is
   prerendered at `/404` and the `nitro:build:public-assets` hook copies `404/index.html` over the
   shell. That the hook's `rm 200.html` works is the proof it runs after the preset.
3. **The 404 fetched a payload the hook deletes.** Three failed requests and two console errors per
   404 view, and the router rewrote the address bar to `/404`, erasing the URL the visitor mistyped.
   The hook now strips every `<script>` and modulepreload/preload/prefetch from the 404 and only the
   404, making it a plain document that renders identically and navigates correctly.
4. **CI never runs package.json pre/post scripts.** `deploy-docs.yml` runs `pnpm nuxt generate`
   directly, so a generator chained to a npm script would run locally and silently never run in
   production. Both generators are wired to the nitro hook instead.

## Regressions introduced during the repair, and fixed

Round-2 verification ran against the built artifact, not the source, and caught sixteen defects this
work had introduced. The ones worth keeping as lessons:

| Regression | Cause |
|---|---|
| Nav brand overlapped links at 768–799px | No `shrink-0` on the brand; only visible in a 32px window |
| Drawer scroll lock never released | Closed on route change but not on the breakpoint; `matchMedia` listener added |
| `shared: 47` did not close | Counted by name; `click_by_view_id` is reachable as `aster_click_by_id`. Closure assertions added so arithmetic that does not add up fails the build |
| Event forwarding under-disclosed | Named some event kinds; now names all six and states `incomingCalls` defaults **ON** |
| `<pre>` blocks lost their copy control | `ComparisonTable.vue` used `withDefaults(...)` unassigned, so `props.label` was `undefined` |
| "Five steps" over six hardening steps | Hand-written count over a generated list; now derived |
| `/tools` meta description 163 chars | Over the 160 budget |

A `v-show` "fix" for the no-JS drawer was written and then **reverted**: `v-show` SSRs as
`style="display:none"`, so it helps a no-JS visitor exactly as little as `v-if`. The footer carries
every route and is the real no-JS path; `<details>` is the fix if it ever matters. The comment in
`NavBar.vue` says so.

## Findings assessed and rejected, with reasons

Not everything reported was real. These were checked against the shipped artifact and dropped:

- **"`<pre>` has no role and no accessible name."** The emitted HTML already carries
  `role="group" aria-label="Code sample: …"`. The finding was written against the source, not the build.
- **"llms.txt omits five tools."** It omits **one** (`aster_list_devices`), plus one rename. The
  finding's set diff was run against a stale list.
- **Design-system drift (`AButton`, `ASectionLabel`, `AStatusPill` unused).** Adopting them would be a
  regression, not a fix: `AButton` renders a `<button>` and every landing CTA is a navigation link —
  swapping would break keyboard and screen-reader semantics; `ASectionLabel` hardcodes `<h2>` and the
  eyebrows sit *above* real headings, so it would inject bogus headings into the outline;
  `AStatusPill` is a device-status enum with no landing-page analogue. `ACodeBlock` *was* adopted.

### The shared layer *was* touched — deliberately, and verified against the dashboard

The round-1 route map asserted "NO CHANGE TO `packages/aster-ui` IS REQUIRED OR PROPOSED". Two files
were changed anyway, because the defects were *in the layer* and fixing them in `docs/` would have
left `mcp/dashboard` broken:

| File | Change | Why it could not live in `docs/` |
|---|---|---|
| `tokens.css` | `--color-fg-muted` — dark `#4a5670` → `#8a93a8`, light `#90a1b5` → `#5c6b80` | The old values measure **2.75:1** (dark) and **2.52:1** (light) against `--color-bg`. WCAG 1.4.3 AA needs 4.5:1. It was a token-level failure, so every consumer was failing. New values: 6.56:1 and 5.18:1 |
| `ACodeBlock.vue` | `tabindex="0" role="group" :aria-label` on the scrollable `<pre>`; focus rings; `min-width: 0`; 24px min target on the copy button | A horizontal scroll container needs a keyboard handle (2.1.1) and therefore a name. `aria-label` is ignored on a bare `<pre>` (`role=generic` forbids naming), hence `group` — it takes a name without minting a landmark the way `region` would, once per code block |

Both are additive: no prop, slot or class name changed. **`mcp/dashboard` was rebuilt against the
modified layer and builds green** (`pnpm build`, 4.18 MB, no errors), and its emitted CSS carries
`fg-muted:#8a93a8` / `#5c6b80` while its `/connect` route carries `role="group"` — so the dashboard
inherits both fixes rather than being broken by them.

No other file in the layer was touched, and `AButton` / `ASectionLabel` / `AStatusPill` were left
alone for the reasons above.

## Verified on the artifact

Measured against `.output/public` from a `GITHUB_PAGES=true nuxt generate`, not against source:

```
pages checked      : 8          sitemap <loc>      : 8
JSON-LD fragments  : 167        dangling           : 0
broken internal links : 0       links that would 301 : 0
defects            : 0          (one title/description/canonical/og:url/h1 per page,
                                 canonical === og:url === sitemap <loc>, all trailing-slash,
                                 every description ≤ 160 chars, banner landmark on every page)
200.html present   : False
404.html           : 21,197 bytes, 0 scripts, noindex/follow, no canonical
[verify-facts] 4/4 source checks ran and passed
image refs: 20, missing: 0
```

Markdown twins, after the coverage pass that hoisted page-local prose into `app/data`:

| Twin | Words | | Twin | Words |
|---|---|---|---|---|
| `index.md` | 1,261 | | `architecture.md` | 1,221 |
| `use-cases.md` | 1,263 | | `security.md` | 3,486 |
| `ai-phone.md` | 1,875 | | `setup.md` | 1,030 |
| `tools.md` | 3,257 | | `faq.md` | 2,501 |

`ai-phone.md` was 977 words before that pass — the page's lede, needs, changes, limit and setup steps
lived only in the `.vue` and never reached the twin. The disambiguation sentence (naming Aster DM
Healthcare, Aster Data Systems and the NASA Terra ASTER instrument) now appears in all eight twin
front matters, in `llms.txt` and in `llms-full.txt`.

**Side-effect worth knowing:** `@nuxt/icon`'s `clientBundle` scanner globs `.vue` only, so hoisting
arrays out of components took six icon names out of its view. `nuxt.config.ts` sets
`provider: 'none'`, which means a name missing from the `ICONS` list fails **loudly** at build time
rather than silently reaching for `api.iconify.design` — the list carries scanner comments naming the
hoisted icons. If you move data out of a `.vue`, check `ICONS`.

## GEO trajectory

| Stage | Score |
|---|---|
| Pre-split (single page) | 47 / 100 |
| Post-split (8 routes) | 87 / 100 |
| After round-1 repairs | 88 / 100 |

Scored with the `blog-geo` heuristic. It is an internal editorial heuristic, **not** a calibrated
probability of citation, and the final figure was measured before the round-2 fixes and the twin
coverage pass landed — so it understates the shipped state rather than overstating it.

Per Google's guidance, none of the llms.txt / twin work is treated as a Google ranking input. The
Google-facing work is entirely per-route title, description, canonical, og tags and schema, plus
eight real prerendered documents existing where one existed before. The twins and `llms.txt` are for
non-Google answer engines and for developer ergonomics.

## Standing rules for this site

- **Add a route** → add a row to `app/data/routes.ts` *and* a file in `app/pages/`. A route missing
  from the registry is not prerendered, and GitHub Pages serves the soft-404 shell in its place.
- **Publish a number** → derive it in `app/data/`, and add a probe to `verify-facts.ts` if it comes
  from outside `docs/`. Hand-written counts drift; that is what produced the 67.
- **Never put a canonical or `og:url` in `app.head`.** A global canonical pointing at the origin tells
  search engines every other route is a duplicate of the homepage.
- **Anything published — canonical, `og:url`, sitemap, JSON-LD item URLs, nav hrefs — goes through
  `href()`.** Router paths are for matching and prerendering only.

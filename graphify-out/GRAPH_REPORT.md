# Graph Report - aster-mcp  (2026-08-27)

## Corpus Check
- 220 files · ~1,010,503 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1878 nodes · 4314 edges · 158 communities (118 shown, 40 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 334 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `10b8045d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]
- [[_COMMUNITY_Community 44|Community 44]]
- [[_COMMUNITY_Community 45|Community 45]]
- [[_COMMUNITY_Community 46|Community 46]]
- [[_COMMUNITY_Community 47|Community 47]]
- [[_COMMUNITY_Community 48|Community 48]]
- [[_COMMUNITY_Community 49|Community 49]]
- [[_COMMUNITY_Community 50|Community 50]]
- [[_COMMUNITY_Community 51|Community 51]]
- [[_COMMUNITY_Community 52|Community 52]]
- [[_COMMUNITY_Community 53|Community 53]]
- [[_COMMUNITY_Community 54|Community 54]]
- [[_COMMUNITY_Community 55|Community 55]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Community 57|Community 57]]
- [[_COMMUNITY_Community 58|Community 58]]
- [[_COMMUNITY_Community 59|Community 59]]
- [[_COMMUNITY_Community 60|Community 60]]
- [[_COMMUNITY_Community 61|Community 61]]
- [[_COMMUNITY_Community 62|Community 62]]
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 65|Community 65]]
- [[_COMMUNITY_Community 66|Community 66]]
- [[_COMMUNITY_Community 67|Community 67]]
- [[_COMMUNITY_Community 68|Community 68]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 70|Community 70]]
- [[_COMMUNITY_Community 71|Community 71]]
- [[_COMMUNITY_Community 72|Community 72]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]
- [[_COMMUNITY_Community 75|Community 75]]
- [[_COMMUNITY_Community 76|Community 76]]
- [[_COMMUNITY_Community 77|Community 77]]
- [[_COMMUNITY_Community 78|Community 78]]
- [[_COMMUNITY_Community 79|Community 79]]
- [[_COMMUNITY_Community 80|Community 80]]
- [[_COMMUNITY_Community 81|Community 81]]
- [[_COMMUNITY_Community 82|Community 82]]
- [[_COMMUNITY_Community 83|Community 83]]
- [[_COMMUNITY_Community 84|Community 84]]
- [[_COMMUNITY_Community 85|Community 85]]
- [[_COMMUNITY_Community 86|Community 86]]
- [[_COMMUNITY_Community 87|Community 87]]
- [[_COMMUNITY_Community 88|Community 88]]
- [[_COMMUNITY_Community 89|Community 89]]
- [[_COMMUNITY_Community 90|Community 90]]
- [[_COMMUNITY_Community 91|Community 91]]
- [[_COMMUNITY_Community 92|Community 92]]
- [[_COMMUNITY_Community 93|Community 93]]
- [[_COMMUNITY_Community 94|Community 94]]
- [[_COMMUNITY_Community 96|Community 96]]
- [[_COMMUNITY_Community 97|Community 97]]
- [[_COMMUNITY_Community 98|Community 98]]
- [[_COMMUNITY_Community 99|Community 99]]
- [[_COMMUNITY_Community 100|Community 100]]
- [[_COMMUNITY_Community 101|Community 101]]
- [[_COMMUNITY_Community 103|Community 103]]
- [[_COMMUNITY_Community 105|Community 105]]
- [[_COMMUNITY_Community 107|Community 107]]
- [[_COMMUNITY_Community 108|Community 108]]
- [[_COMMUNITY_Community 109|Community 109]]
- [[_COMMUNITY_Community 110|Community 110]]
- [[_COMMUNITY_Community 112|Community 112]]

## God Nodes (most connected - your core abstractions)
1. `CommandResult` - 104 edges
2. `AsterAccessibilityService` - 94 edges
3. `handleToolCall()` - 55 edges
4. `sendCommand()` - 51 edges
5. `jsonResult()` - 49 edges
6. `AccessibilityHandler` - 39 edges
7. `InteractiveOverlayController` - 38 edges
8. `AsterWebSocketClient` - 34 edges
9. `AsterService` - 33 edges
10. `CommandHandler` - 32 edges

## Surprising Connections (you probably didn't know these)
- `PermissionRow()` --calls--> `AsterButton()`  [INFERRED]
  apps/android/app/src/main/java/com/aster/ui/IpcApprovalActivity.kt → apps/android/app/src/main/java/com/aster/ui/components/AsterComponents.kt
- `AsterNavHost()` --calls--> `HomeScreen()`  [INFERRED]
  apps/android/app/src/main/java/com/aster/ui/MainActivity.kt → apps/android/app/src/main/java/com/aster/ui/screens/home/HomeScreen.kt
- `AsterNavHost()` --calls--> `IpcDashboardScreen()`  [INFERRED]
  apps/android/app/src/main/java/com/aster/ui/MainActivity.kt → apps/android/app/src/main/java/com/aster/ui/screens/ipc/IpcDashboardScreen.kt
- `AsterNavHost()` --calls--> `McpDashboardScreen()`  [INFERRED]
  apps/android/app/src/main/java/com/aster/ui/MainActivity.kt → apps/android/app/src/main/java/com/aster/ui/screens/mcp/McpDashboardScreen.kt
- `AsterNavHost()` --calls--> `OnboardingScreen()`  [INFERRED]
  apps/android/app/src/main/java/com/aster/ui/MainActivity.kt → apps/android/app/src/main/java/com/aster/ui/screens/onboarding/OnboardingScreen.kt

## Import Cycles
- None detected.

## Communities (158 total, 40 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (22): CompletableDeferred, CoroutineScope, EditText, FrameLayout, GradientDrawable, LinearLayout, CappedScrollView, InteractiveOverlayController (+14 more)

### Community 1 - "Community 1"
Cohesion: 0.06
Nodes (16): ElementFilter, NodeFacts, ObserveMode, ObserveBudget, ObserveBudgetTest, RoleMapper, Boolean, Context (+8 more)

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (21): Address, ByteArray, Double, ExifInterface, File, HostDirHandler, DirectoryInfo, FileInfo (+13 more)

### Community 3 - "Community 3"
Cohesion: 0.06
Nodes (15): Bounds, ElementState, fromLTRB(), ObservedElement, ObserveResult, ScreenContext, Scrollable, WindowInfo (+7 more)

### Community 4 - "Community 4"
Cohesion: 0.07
Nodes (50): ToolResult, AnalyzeStorageSchema, ClickByIdSchema, ClickByTextSchema, DeleteAlarmSchema, DeleteContactsSchema, DeleteFileSchema, DismissAlarmSchema (+42 more)

### Community 5 - "Community 5"
Cohesion: 0.12
Nodes (6): AccessibilityEvent, AccessibilityService, AccessibilityHandler, PreAct, JsonElement, AsterAccessibilityService

### Community 6 - "Community 6"
Cohesion: 0.10
Nodes (7): Command, AlarmHandler, FileSystemHandler, NotificationHandler, CommandResult, failure(), success()

### Community 7 - "Community 7"
Cohesion: 0.12
Nodes (47): handleAnalyzeStorage(), handleClickById(), handleClickByText(), handleDeleteAlarm(), handleDeleteContacts(), handleDeleteFile(), handleDismissAlarm(), handleExecuteShell() (+39 more)

### Community 8 - "Community 8"
Cohesion: 0.10
Nodes (14): InputModule, AccessibilityInputBackend, AccessibilityKeyRoute, routeFor(), BackendOutcome, Declined, Failed, InputBackend (+6 more)

### Community 9 - "Community 9"
Cohesion: 0.07
Nodes (15): AuthResult, DeviceStatus, kotlinx, AuthMessage, AuthResult, Command, CommandResponse, DeviceStatus (+7 more)

### Community 10 - "Community 10"
Cohesion: 0.05
Nodes (38): 1. Install the MCP Server, 2. Install the Android App, 3. Start the Server, 4. Connect Your Device, 5. Configure Your AI Client, Adding a new MCP tool, Android app, Android permissions — and why each one (+30 more)

### Community 11 - "Community 11"
Cohesion: 0.09
Nodes (8): LabelAggregator, Keys, ServerConfig, SettingsDataStore, EventDeduplicator, SettingsViewModel, String, WireParams

### Community 12 - "Community 12"
Cohesion: 0.09
Nodes (15): body, BroadcastReceiver, IBinder, Intent, BootReceiver, KillSwitchReceiver, NotificationActionReceiver, SmsBroadcastReceiver (+7 more)

### Community 13 - "Community 13"
Cohesion: 0.09
Nodes (11): DataStore, AsterDatabase, migrate(), ToolCallLog, ToolCallLogDao, AppModule, Flow, OkHttpClient (+3 more)

### Community 14 - "Community 14"
Cohesion: 0.10
Nodes (27): updateDeviceExtendedInfo(), AuthMessage, AuthMessageSchema, AuthResult, AuthResultSchema, Command, CommandResponse, CommandResponseSchema (+19 more)

### Community 15 - "Community 15"
Cohesion: 0.12
Nodes (4): PackagePolicyGuard, targetPackageOf(), PackagePolicyGuardTest, Set

### Community 16 - "Community 16"
Cohesion: 0.16
Nodes (23): AsterButton(), AsterButtonVariant, AsterCard(), AsterStatCard(), AsterTextField(), AsterTopBar(), InfoRow(), ToolListItem() (+15 more)

### Community 17 - "Community 17"
Cohesion: 0.12
Nodes (26): addLog(), closeDatabase(), DbDevice, DbLogEntry, deleteDevice(), getAllDevices(), getAllLogs(), getDatabase() (+18 more)

### Community 18 - "Community 18"
Cohesion: 0.08
Nodes (23): dependencies, nuxt, @nuxt/fonts, @nuxt/icon, tailwindcss, @tailwindcss/vite, vue, vue-router (+15 more)

### Community 19 - "Community 19"
Cohesion: 0.13
Nodes (6): Closeable, com, OcrBlock, OcrEngine, ScreenObserveSupportTest, TextRecognizer

### Community 20 - "Community 20"
Cohesion: 0.18
Nodes (4): Notification, PowerManager, AsterService, WifiManager

### Community 21 - "Community 21"
Cohesion: 0.13
Nodes (7): NotificationListenerService, AsterNotificationListenerService, getInstance(), isServiceEnabled(), NotificationAction, NotificationActionCallback, StatusBarNotification

### Community 22 - "Community 22"
Cohesion: 0.09
Nodes (21): dependencies, nuxt, @nuxt/fonts, devDependencies, @iconify-json/lucide, @iconify-json/mdi, @iconify-json/ph, @nuxt/devtools (+13 more)

### Community 23 - "Community 23"
Cohesion: 0.19
Nodes (18): AgentEventForwardingConfig, buildEventText(), ConfigCandidate, DeviceContext, formatData(), getAgentEventForwardingConfig(), getSavedAgentEventForwardingToken(), isAgentEventForwardingEnabled() (+10 more)

### Community 24 - "Community 24"
Cohesion: 0.16
Nodes (19): initDatabase(), loadAgentEventForwardingConfig(), registerMcpHttpRoutes(), createMcpServer(), startMcpServer(), createApiServer(), startApiServer(), ASTER_DIR (+11 more)

### Community 25 - "Community 25"
Cohesion: 0.12
Nodes (6): IpcDashboardViewModel, LogViewModel, McpDashboardViewModel, OnboardingViewModel, StateFlow, ViewModel

### Community 26 - "Community 26"
Cohesion: 0.14
Nodes (9): A11yObservation, getInstance(), isServiceEnabled(), keyNameToKeycode(), normalizeScrollDirection(), pinchStrokePoints(), ScreenshotCapture, StrokePts (+1 more)

### Community 27 - "Community 27"
Cohesion: 0.11
Nodes (11): ASTER_DIR, devicesCmd, __dirname, __filename, LOG_FILE, PID_FILE, program, PROGRESS_STEPS (+3 more)

### Community 28 - "Community 28"
Cohesion: 0.15
Nodes (6): Collection, List, ToolCatalog, ToolEntry, ToolInfo, RemoteViewModel

### Community 29 - "Community 29"
Cohesion: 0.16
Nodes (6): IAsterService, Job, ModeStatus, IpcMode, RemoteWsMode, ConnectionState

### Community 30 - "Community 30"
Cohesion: 0.11
Nodes (18): compilerOptions, declaration, declarationMap, esModuleInterop, forceConsistentCasingInFileNames, lib, module, moduleResolution (+10 more)

### Community 31 - "Community 31"
Cohesion: 0.11
Nodes (18): Apps & System, Aster - Your AI CoPilot on Mobile, Available Tools, Commands, Device Features, Device & Screen, Event Format, Event Types (+10 more)

### Community 33 - "Community 33"
Cohesion: 0.18
Nodes (6): CameraSelector, CameraHandler, ServiceLifecycleOwner, Lifecycle, LifecycleOwner, Recording

### Community 34 - "Community 34"
Cohesion: 0.12
Nodes (16): AgentEventForwardingConfig, AgentEventForwardingConfigResponse, AgentEventForwardingTestResult, Device, DeviceWithLiveInfo, ExtendedDeviceInfo, FileContentResult, FileEntry (+8 more)

### Community 36 - "Community 36"
Cohesion: 0.18
Nodes (12): Color, StatusBadge(), GlowOrb(), TokenDisplay(), getCategoryIcon(), ToolRow(), ToolsSection(), Dp (+4 more)

### Community 37 - "Community 37"
Cohesion: 0.12
Nodes (16): author, bin, aster, description, exports, files, homepage, import (+8 more)

### Community 39 - "Community 39"
Cohesion: 0.18
Nodes (8): ActionMapper, AnimatedEntrance(), Int, AboutStep(), BottomNavigation(), FeatureItem(), OnboardingScreen(), PermissionsStep()

### Community 41 - "Community 41"
Cohesion: 0.23
Nodes (4): ExoPlayer, MediaHandler, MediaSession, TextToSpeech

### Community 42 - "Community 42"
Cohesion: 0.27
Nodes (4): Float, GestureDescription, Pair, dragStrokePoints()

### Community 43 - "Community 43"
Cohesion: 0.13
Nodes (14): Any MCP-Compatible Client, Available MCP Tools, Claude Code / Claude Desktop, Commands, Connect Your AI Assistant, Installation, License, OpenClaw / Moltbot / Clawbot (Recommended) (+6 more)

### Community 44 - "Community 44"
Cohesion: 0.23
Nodes (4): AccessibilityPulseClassifier, Input, Pulse, AccessibilityPulseClassifierTest

### Community 45 - "Community 45"
Cohesion: 0.22
Nodes (9): Bundle, ComponentActivity, AsterColorScheme, AsterTheme(), ApprovalBody(), BrandHeader(), ChooserBody(), InteractivePromptActivity (+1 more)

### Community 47 - "Community 47"
Cohesion: 0.21
Nodes (8): HomeViewModel, ConnectionMode, IpcConfig, McpConfig, ModeConfig, ModeState, ModeType, RemoteConfig

### Community 51 - "Community 51"
Cohesion: 0.19
Nodes (5): CIOApplicationEngine, EmbeddedServer, McpMode, Server, StreamableHttpServerTransport

### Community 52 - "Community 52"
Cohesion: 0.26
Nodes (4): Cand, Walk, Drawable, IpcApprovalActivity

### Community 53 - "Community 53"
Cohesion: 0.30
Nodes (10): AsterSectionHeader(), CodeBlock(), ConnectionMethodHint(), McpConfigSection(), McpDashboardScreen(), ServerEndpointsSection(), SetupGuideSection(), SetupStep() (+2 more)

### Community 54 - "Community 54"
Cohesion: 0.32
Nodes (11): InstallationSection(), NumberedStep(), OpenClawSection(), OptionHeader(), PortInfoRow(), ProtocolChip(), RemoteConnectScreen(), ServerAddressCard() (+3 more)

### Community 55 - "Community 55"
Cohesion: 0.32
Nodes (11): Home, IpcDashboard, Logs, McpDashboard, Onboarding, PermissionAlert, Permissions, RemoteConnect (+3 more)

### Community 56 - "Community 56"
Cohesion: 0.32
Nodes (11): execAsync, getTailscaleDNSName(), getTailscaleIP(), getTailscaleStatus(), isTailscaleInstalled(), serveTailscalePort(), startTailscaleServe(), startTailscaleServeHttps() (+3 more)

### Community 58 - "Community 58"
Cohesion: 0.20
Nodes (3): android, WindowDescriptor, Triple

### Community 59 - "Community 59"
Cohesion: 0.18
Nodes (4): AudioManager, InteractiveOverlayHandler, ShellHandler, VolumeHandler

### Community 60 - "Community 60"
Cohesion: 0.27
Nodes (5): Bitmap, Rect, ScreenshotFile, Mark, ScreenAnnotator

### Community 63 - "Community 63"
Cohesion: 0.18
Nodes (11): dependencies, better-sqlite3, chalk, commander, consola, dotenv, fastify, @fastify/static (+3 more)

### Community 64 - "Community 64"
Cohesion: 0.18
Nodes (11): scripts, build, build:all, dev, dev:all, dev:dashboard, lint, postinstall (+3 more)

### Community 65 - "Community 65"
Cohesion: 0.25
Nodes (3): normalizeScrollAmount(), ScrollAmount, ScrollAxis

### Community 68 - "Community 68"
Cohesion: 0.24
Nodes (3): DescriptorBounds, fromLTRB(), NodeDescriptor

### Community 70 - "Community 70"
Cohesion: 0.27
Nodes (3): ResolvedRef, ResolvedBy, ScreenActionResult

### Community 74 - "Community 74"
Cohesion: 0.43
Nodes (5): Completed, Started, ToolCallLogger, ToolEvent, SharedFlow

### Community 75 - "Community 75"
Cohesion: 0.43
Nodes (3): ModeModule, Map, CommandHandler

### Community 76 - "Community 76"
Cohesion: 0.39
Nodes (7): handleSearchMedia(), formatDate(), ParsedQuery, parseFileTypes(), parseLocation(), parseNaturalLanguageQuery(), parseTimeExpression()

### Community 77 - "Community 77"
Cohesion: 0.25
Nodes (8): devDependencies, concurrently, tsx, @types/better-sqlite3, @types/node, @types/ws, typescript, vitest

### Community 82 - "Community 82"
Cohesion: 0.53
Nodes (4): BadgeItem, ModeCard(), HomeScreen(), toDisplayName()

### Community 84 - "Community 84"
Cohesion: 0.60
Nodes (5): ImageVector, AboutInfoRow(), ModeOption(), SettingsScreen(), ThemeChip()

### Community 85 - "Community 85"
Cohesion: 0.33
Nodes (6): errorResult(), handleGetDeviceInfo(), handleTakePhoto(), handleTakeScreenshot(), imageResult(), textResult()

### Community 87 - "Community 87"
Cohesion: 0.47
Nodes (5): analyzeMediaResults(), generateSuggestions(), generateSummary(), MediaInsights, MediaSearchResult

### Community 91 - "Community 91"
Cohesion: 0.50
Nodes (4): repository, directory, type, url

### Community 92 - "Community 92"
Cohesion: 0.50
Nodes (3): McpToolRegistry, PropDef, ToolDef

### Community 93 - "Community 93"
Cohesion: 0.50
Nodes (3): AsterDarkColors, AsterLightColors, SemanticColors

## Knowledge Gaps
- **234 isolated node(s):** `aster`, `Keys`, `AuthMessage`, `CommandResponse`, `AuthResult` (+229 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **40 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `packageManager` connect `Community 105` to `Community 37`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Why does `AsterAccessibilityService` connect `Community 5` to `Community 1`, `Community 65`, `Community 35`, `Community 58`, `Community 68`, `Community 69`, `Community 39`, `Community 8`, `Community 70`, `Community 42`, `Community 101`, `Community 57`, `Community 26`, `Community 60`?**
  _High betweenness centrality (0.040) - this node is a cross-community bridge._
- **Why does `AsterWebSocketClient` connect `Community 9` to `Community 6`, `Community 74`, `Community 75`, `Community 11`, `Community 13`, `Community 20`, `Community 25`, `Community 29`?**
  _High betweenness centrality (0.025) - this node is a cross-community bridge._
- **What connects `aster`, `Keys`, `AuthMessage` to the rest of the system?**
  _234 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05554311310190369 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.055087719298245616 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.05794556628621598 - nodes in this community are weakly interconnected._
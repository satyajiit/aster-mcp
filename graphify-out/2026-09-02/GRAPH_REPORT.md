# Graph Report - aster-mcp  (2026-09-02)

## Corpus Check
- 291 files · ~682,361 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3095 nodes · 5944 edges · 229 communities (157 shown, 46 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 259 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b848de04`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- GradientDrawable
- InputRouter
- StorageHandler
- PermissionUtils
- handler.ts
- AsterAccessibilityService.kt
- ToolExecutionOverlay
- handleToolCall
- AccessibilityHandler
- AsterWebSocketClient
- README.md
- .put
- SmsBroadcastReceiver
- AppModule.kt
- PackagePolicyGuardTest
- devDependencies
- MainActivity.kt
- FileBrowser.vue
- dependencies
- ScreenObserveSupportTest
- AsterService
- AsterNotificationListenerService
- src/index.ts
- useApi.ts
- [id]/index.vue
- server/index.ts
- ElementFilterTest
- types/index.ts
- ToolInfo
- IpcMode
- compilerOptions
- Aster - Your AI CoPilot on Mobile
- event-forwarding/index.ts
- CameraHandler
- control.vue
- ModeStatus
- Composable
- mcp/package.json
- ScreenSyncTracker
- CompanionFaceView.kt
- CompanionNativeAnimator
- MediaHandler
- AsterAccessibilityService
- mcp/README.md
- CompanionFaceView
- event-forwarding.vue
- ToolCallLog
- AsterCard
- aster.ts
- CallStateMonitor
- CommandResult
- McpMode
- IpcApprovalActivity.kt
- CompanionFaceState.kt
- CutoutBounds
- RemoteConnectScreen.kt
- ScreenActionResultTest
- WireContractTest
- Canvas
- screen.vue
- AccessibilityPulseClassifier
- SettingsDataStore
- HostDirHandler
- dependencies
- scripts
- CompanionFaceOverlay
- keywords
- ConnectionState
- SnapshotCache
- fixtures.mjs
- TailscaleUtils
- IntentHandler
- SmartLiftClassifier
- LiveChatSection.vue
- pages/index.vue
- .sampleResult
- websocket/index.ts
- devDependencies
- ContactHandler
- CompanionReaction
- McpDashboardScreen.kt
- ActionMapperTest
- RoleMapperTest
- AsterButton
- WireParamsTest
- FocusedNodeOutcome
- EventDeduplicator
- InstalledAppsHandler
- ModeType
- AsterApplication
- parseCompanionStatus
- repository
- CommandHandler
- Screen
- docs/tsconfig.json
- SmsHandler
- LabelAggregatorTest
- AsterWebSocketClient.kt
- devices/index.vue
- DeviceInfoHandler
- logs.vue
- ScreenAnnotator.kt
- parseCompanionPulseConfiguration
- ObserveRankTest
- ScreenWaitMatcherTest
- HeroSection.vue
- HowItWorks.vue
- .mcp.json
- InteractiveOverlayModel
- KillSwitchController
- queryParser.ts
- LabelAggregator
- InteractiveOverlayController
- WireParams
- OnboardingViewModel
- CompanionSystemPulseMonitor
- SettingsViewModel
- Rect
- ContactsPanel.vue
- channels/index.ts
- handleGetDeviceInfo
- BootReceiver.kt
- StoragePanel.vue
- .isSatisfied
- AudioPanel.vue
- ScreenAnnotatorGeometryTest
- SignInWaitOverlay
- mediaAnalyzer.ts
- WebSocketListener
- GuidedPermissionFlow
- BoundsTest
- EmbraceSection.vue
- ScreenshotsSection.vue
- SecuritySection.vue
- files
- AppsPanel.vue
- gradlew
- UseCasesSection.vue
- UtilitiesPanel.vue
- ConnectionFailureMapperTest
- ProactiveSection.vue
- screenshots/package.json
- ScreenObserver
- dashboard/tsconfig.json
- FeaturesGrid.vue
- IntegrationsSection.vue
- NavBar.vue
- SetupSteps.vue
- ToolsShowcase.vue
- build-python-android.sh
- AlarmsPanel.vue
- ObserveBudget
- dashboard/nuxt.config.ts
- installedApps.test.ts
- dashboard/app/app.vue
- FileSystemHandler
- MessagesPanel.vue
- AsterTheme
- connect.vue
- .performGesture
- .resolveRef
- ReconnectPolicyTest
- TextWatcher
- OverlayPermissionActivity.kt
- SmartLiftClassifierTest
- ToolCallLogDao
- CompanionViseme
- panels.vue
- AsterIpcService
- VolumeHandler
- LocationPanel.vue
- run.mjs
- ConnectionFailureMapper
- McpDashboardViewModel.kt
- AccessibilityHandlerActionsTest
- Backlog
- aster-ui/package.json
- CompanionStatusModel
- ShellPanel.vue
- default.vue
- useColorScheme
- usePolling
- Quick Start
- Security & Privacy
- WsUrlPolicy
- AStatusPill.vue
- useToast
- Development
- Integrations
- .sendAuthMessage
- StarRepoCard.vue
- MetricGauge.vue
- ACodeBlock.vue
- AModal.vue
- AToastHost.vue
- Usage
- Securing the connection
- sync.mjs
- ThemeToggle.vue
- aster-ui/nuxt.config.ts
- Proactive Event Forwarding
- ABadge.vue
- AToggle.vue

## God Nodes (most connected - your core abstractions)
1. `CommandResult` - 130 edges
2. `AsterAccessibilityService` - 108 edges
3. `CommandHandler` - 63 edges
4. `handleToolCall()` - 58 edges
5. `sendCommand()` - 54 edges
6. `jsonResult()` - 49 edges
7. `AsterService` - 46 edges
8. `CompanionFaceOverlay` - 45 edges
9. `AsterWebSocketClient` - 43 edges
10. `AccessibilityHandler` - 41 edges

## Surprising Connections (you probably didn't know these)
- `AsterNotificationListenerService` --references--> `title`  [EXTRACTED]
  apps/android/app/src/main/java/com/aster/service/AsterNotificationListenerService.kt → mcp/dashboard/app/components/panels/NotificationsPanel.vue
- `AsterAccessibilityService` --calls--> `OcrEngine`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/AsterAccessibilityService.kt → apps/android/app/src/main/java/com/aster/service/OcrEngine.kt
- `AsterAccessibilityService` --calls--> `ScreenSyncTracker`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/AsterAccessibilityService.kt → apps/android/app/src/main/java/com/aster/service/ScreenSyncTracker.kt
- `CompanionFaceOverlay` --calls--> `CompanionSystemPulseMonitor`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/overlay/CompanionFaceOverlay.kt → apps/android/app/src/main/java/com/aster/service/overlay/CompanionSystemPulseMonitor.kt
- `CompanionFaceView` --calls--> `CompanionNativeAnimator`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/overlay/CompanionFaceView.kt → apps/android/app/src/main/java/com/aster/service/overlay/CompanionNativeAnimator.kt

## Import Cycles
- None detected.

## Communities (229 total, 46 thin omitted)

### Community 0 - "GradientDrawable"
Cohesion: 0.31
Nodes (5): TextView, View, EditText, GradientDrawable, LinearLayout

### Community 1 - "InputRouter"
Cohesion: 0.07
Nodes (22): InputModule, AccessibilityInputBackend, AccessibilityKeyRoute, GLOBAL_BACK, GLOBAL_HOME, GLOBAL_RECENTS, IME_ENTER, NODE_COPY (+14 more)

### Community 2 - "StorageHandler"
Cohesion: 0.07
Nodes (23): Address, CameraInfo, ImageDimensions, LocationInfo, PhotoMetadata, LocationFilter, SearchFilters, SortOption (+15 more)

### Community 3 - "PermissionUtils"
Cohesion: 0.15
Nodes (15): Context, Intent, PermissionCheckResult, PermissionType, ACCESSIBILITY, BATTERY, CAMERA, CONTACTS (+7 more)

### Community 4 - "handler.ts"
Cohesion: 0.07
Nodes (50): ToolResult, AnalyzeStorageSchema, ClickByIdSchema, ClickByTextSchema, DeleteAlarmSchema, DeleteContactsSchema, DeleteFileSchema, DismissAlarmSchema (+42 more)

### Community 5 - "AsterAccessibilityService.kt"
Cohesion: 0.09
Nodes (10): AccessibilityService, ActionMapper, RoleMapper, dragStrokePoints(), keyNameToKeycode(), normalizeScrollDirection(), pinchStrokePoints(), StrokePts (+2 more)

### Community 6 - "ToolExecutionOverlay"
Cohesion: 0.15
Nodes (10): Started, ToolEvent, Context, CoroutineScope, FrameLayout, Job, TextView, WindowManager (+2 more)

### Community 7 - "handleToolCall"
Cohesion: 0.12
Nodes (48): handleAnalyzeStorage(), handleClickById(), handleClickByText(), handleDeleteAlarm(), handleDeleteContacts(), handleDeleteFile(), handleDismissAlarm(), handleExecuteShell() (+40 more)

### Community 8 - "AccessibilityHandler"
Cohesion: 0.15
Nodes (7): AccessibilityHandler, Command, JsonArray, JsonElement, JsonObject, PreAct, Mark

### Community 9 - "AsterWebSocketClient"
Cohesion: 0.15
Nodes (6): AsterWebSocketClient, NetworkCallback, Command, AuthResult, DeviceStatus, Network

### Community 10 - "README.md"
Cohesion: 0.12
Nodes (15): Architecture, Backlog, Connection Modes, Features, License, MCP Tools, OpenAlly.ai, Project Structure (+7 more)

### Community 11 - ".put"
Cohesion: 0.09
Nodes (8): AlarmHandler, Command, Command, Command, Command, NotificationHandler, Command, InteractiveOverlayModelTest

### Community 12 - "SmsBroadcastReceiver"
Cohesion: 0.31
Nodes (7): BroadcastReceiver, Context, Intent, SmsBroadcastReceiver, body, sender, timestamp

### Community 13 - "AppModule.kt"
Cohesion: 0.23
Nodes (7): AsterDatabase, AppModule, Context, DataStore, OkHttpClient, Preferences, RoomDatabase

### Community 14 - "PackagePolicyGuardTest"
Cohesion: 0.12
Nodes (3): Command, PackagePolicyGuard, PackagePolicyGuardTest

### Community 15 - "devDependencies"
Cohesion: 0.06
Nodes (31): @iconify-json/ph, dependencies, nuxt, @nuxt/fonts, devDependencies, @iconify-json/lucide, @iconify-json/mdi, @iconify-json/ph (+23 more)

### Community 16 - "MainActivity.kt"
Cohesion: 0.25
Nodes (12): AsterNavHost(), Bundle, ComponentActivity, Modifier, MainActivity, AboutInfoRow(), ImageVector, Modifier (+4 more)

### Community 17 - "FileBrowser.vue"
Cohesion: 0.08
Nodes (31): api, busy, confirmDelete(), createFolder(), creatingFolder, crumbs, deleting, download() (+23 more)

### Community 18 - "dependencies"
Cohesion: 0.06
Nodes (35): dependencies, nuxt, @nuxt/fonts, @nuxt/icon, tailwindcss, @tailwindcss/vite, vue, vue-router (+27 more)

### Community 19 - "ScreenObserveSupportTest"
Cohesion: 0.10
Nodes (11): Bitmap, com, OcrBlock, OcrEngine, JsonArray, JsonObject, MergeResult, ScreenObserveSupport (+3 more)

### Community 20 - "AsterService"
Cohesion: 0.13
Nodes (10): AsterService, com, Context, IBinder, Intent, Job, JsonElement, Notification (+2 more)

### Community 21 - "AsterNotificationListenerService"
Cohesion: 0.06
Nodes (20): BroadcastReceiver, Context, Intent, NotificationActionReceiver, AsterNotificationListenerService, JsonArray, JsonObject, NotificationAction (+12 more)

### Community 22 - "src/index.ts"
Cohesion: 0.13
Nodes (27): initDatabase(), ASTER_DIR, currentFile, getLocalIP(), mainScript, removeStatusFile(), startDashboardServer(), startMcp() (+19 more)

### Community 23 - "useApi.ts"
Cohesion: 0.09
Nodes (19): PLATFORM_ICON, AgentEventForwardingConfigResponse, AgentEventForwardingTestResult, Device, DeviceWithLiveInfo, ExtendedDeviceInfo, FileContentResult, FileEntry (+11 more)

### Community 24 - "[id]/index.vue"
Cohesion: 0.09
Nodes (25): InfoItem, api, approve(), battery, canControl, device, deviceId, displayInfo (+17 more)

### Community 25 - "server/index.ts"
Cohesion: 0.12
Nodes (32): DbDevice, DbLogEntry, deleteDevice(), getAllDevices(), getAllLogs(), getDatabase(), getDeviceLogs(), getLoggedDeviceIds() (+24 more)

### Community 26 - "ElementFilterTest"
Cohesion: 0.14
Nodes (4): ElementFilter, NodeFacts, ObserveMode, ElementFilterTest

### Community 27 - "types/index.ts"
Cohesion: 0.10
Nodes (20): AuthMessage, AuthMessageSchema, AuthResult, AuthResultSchema, Command, CommandResponse, CommandResponseSchema, CommandSchema (+12 more)

### Community 28 - "ToolInfo"
Cohesion: 0.15
Nodes (6): ToolCatalog, ToolEntry, ToolInfo, IpcDashboardViewModel, StateFlow, ViewModel

### Community 29 - "IpcMode"
Cohesion: 0.27
Nodes (3): IpcMode, StateFlow, IAsterService

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (24): compilerOptions, declaration, declarationMap, esModuleInterop, forceConsistentCasingInFileNames, lib, module, moduleResolution (+16 more)

### Community 31 - "Aster - Your AI CoPilot on Mobile"
Cohesion: 0.11
Nodes (18): Apps & System, Aster - Your AI CoPilot on Mobile, Available Tools, Commands, Device Features, Device & Screen, Event Format, Event Types (+10 more)

### Community 32 - "event-forwarding/index.ts"
Cohesion: 0.17
Nodes (17): AgentEventForwardingConfig, buildEventText(), ConfigCandidate, DeviceContext, formatData(), getAgentEventForwardingConfig(), getSavedAgentEventForwardingToken(), isAgentEventForwardingEnabled() (+9 more)

### Community 33 - "CameraHandler"
Cohesion: 0.18
Nodes (9): CameraHandler, OnImageCapturedCallback, Command, ServiceLifecycleOwner, CameraSelector, ImageCaptureException, ImageProxy, LifecycleOwner (+1 more)

### Community 34 - "control.vue"
Cohesion: 0.09
Nodes (22): ToolDefinition, api, args, coerce(), deviceId, execute(), expanded, filtered (+14 more)

### Community 35 - "ModeStatus"
Cohesion: 0.15
Nodes (11): ConnectionMode, IpcConfig, StateFlow, McpConfig, ModeConfig, ModeStatus, RemoteConfig, Command (+3 more)

### Community 36 - "Composable"
Cohesion: 0.15
Nodes (21): Modifier, BrandLockup(), Modifier, Modifier, GlowOrb(), Color, Modifier, BadgeItem (+13 more)

### Community 37 - "mcp/package.json"
Cohesion: 0.11
Nodes (18): author, bin, aster, bugs, url, description, engines, node (+10 more)

### Community 38 - "ScreenSyncTracker"
Cohesion: 0.14
Nodes (3): SharedFlow, ScreenSyncTracker, ScreenSyncTrackerTest

### Community 39 - "CompanionFaceView.kt"
Cohesion: 0.22
Nodes (20): capOf(), CompanionFaceModel, FaceBlush, FaceCircle, FaceCookie, FaceCup, faceGroups(), FaceHeadphones (+12 more)

### Community 40 - "CompanionNativeAnimator"
Cohesion: 0.19
Nodes (3): CompanionMotionFrame, CompanionNativeAnimator, CompanionNativeAnimatorTest

### Community 41 - "MediaHandler"
Cohesion: 0.18
Nodes (7): Command, TextToSpeech, UtteranceProgressListener, MediaHandler, UtteranceProgressListener, ExoPlayer, MediaSession

### Community 42 - "AsterAccessibilityService"
Cohesion: 0.07
Nodes (9): AsterAccessibilityService, TakeScreenshotCallback, android, JsonElement, JsonObject, ScreenshotCapture, ScreenshotFile, WindowDescriptor (+1 more)

### Community 43 - "mcp/README.md"
Cohesion: 0.11
Nodes (17): Any MCP-Compatible Client, Available MCP Tools, Claude Code / Claude Desktop, Commands, Companion App, Connect Your AI Assistant, Installation, License (+9 more)

### Community 44 - "CompanionFaceView"
Cohesion: 0.15
Nodes (3): CompanionFaceView, View, FrameCallback

### Community 45 - "event-forwarding.vue"
Cohesion: 0.12
Nodes (15): AgentEventForwardingConfig, api, editing, enabledEvents, EVENTS, form, hasSourceToken, load() (+7 more)

### Community 46 - "ToolCallLog"
Cohesion: 0.27
Nodes (5): ToolCallLog, Completed, StateFlow, ViewModel, LogViewModel

### Community 47 - "AsterCard"
Cohesion: 0.23
Nodes (19): AsterCard(), AsterStatCard(), AsterTextField(), AsterTopBar(), InfoRow(), Color, ImageVector, Modifier (+11 more)

### Community 48 - "aster.ts"
Cohesion: 0.11
Nodes (17): ASTER_DIR, daemonDeviceAction(), devicesCmd, __dirname, displayStatus(), __filename, isProcessRunning(), lanMcpUrl() (+9 more)

### Community 49 - "CallStateMonitor"
Cohesion: 0.18
Nodes (8): Api31Callback, CallStateMonitor, CallStateSnapshot, StateFlow, LegacyListener, CallStateListener, PhoneStateListener, TelephonyCallback

### Community 50 - "CommandResult"
Cohesion: 0.09
Nodes (13): CommandResult, Command, JsonElement, Command, Command, NowPlayingHandler, Command, View (+5 more)

### Community 51 - "McpMode"
Cohesion: 0.26
Nodes (5): StateFlow, McpMode, CIOApplicationEngine, EmbeddedServer, StreamableHttpServerTransport

### Community 52 - "IpcApprovalActivity.kt"
Cohesion: 0.16
Nodes (14): ModeState, ERROR, IDLE, RUNNING, STARTING, STOPPING, IpcApprovalActivity, Bitmap (+6 more)

### Community 53 - "CompanionFaceState.kt"
Cohesion: 0.20
Nodes (11): aggregateCompanionSpeech(), CompanionFaceStateSnapshot, CompanionSpeechAggregate, CompanionSpeechSource, AUDIO, STREAM, CompanionSpeechStream, parseCompanionFaceState() (+3 more)

### Community 54 - "CutoutBounds"
Cohesion: 0.22
Nodes (5): CompanionOverlayGeometry, CutoutBounds, OverlayGeometry, SafeBounds, CompanionOverlayGeometryTest

### Community 55 - "RemoteConnectScreen.kt"
Cohesion: 0.27
Nodes (14): AboutRemoteSection(), InstallationSection(), Color, ImageVector, NumberedStep(), OpenClawSection(), OptionHeader(), PortInfoRow() (+6 more)

### Community 58 - "Canvas"
Cohesion: 0.38
Nodes (3): FaceGroup, Canvas, RectF

### Community 59 - "screen.vue"
Cohesion: 0.08
Nodes (31): toolImageSrc(), acting, api, autoRefresh, capture(), capturing, clickables, clickByText() (+23 more)

### Community 60 - "AccessibilityPulseClassifier"
Cohesion: 0.20
Nodes (5): AccessibilityEvent, AccessibilityPulseClassifier, Input, Pulse, AccessibilityPulseClassifierTest

### Community 61 - "SettingsDataStore"
Cohesion: 0.13
Nodes (4): Keys, Flow, ServerConfig, SettingsDataStore

### Community 62 - "HostDirHandler"
Cohesion: 0.25
Nodes (4): HostDirHandler, ByteArray, Command, Uri

### Community 63 - "dependencies"
Cohesion: 0.10
Nodes (21): better-sqlite3, chalk, commander, consola, dotenv, fastify, @fastify/static, dependencies (+13 more)

### Community 64 - "scripts"
Cohesion: 0.18
Nodes (11): scripts, build, build:all, dev, dev:all, dev:dashboard, lint, postinstall (+3 more)

### Community 65 - "CompanionFaceOverlay"
Cohesion: 0.12
Nodes (8): CompanionFaceOverlay, ByteArray, FrameLayout, Rect, View, WindowManager, ReceivedFaceState, DisplayManager

### Community 66 - "keywords"
Cohesion: 0.12
Nodes (16): keywords, accessibility, ai, ai-phone, android, automation, claude, clawdbot (+8 more)

### Community 67 - "ConnectionState"
Cohesion: 0.15
Nodes (12): ConnectionState, APPROVED, CONNECTED, CONNECTING, DISCONNECTED, ERROR, PENDING_APPROVAL, RECONNECTING (+4 more)

### Community 69 - "fixtures.mjs"
Cohesion: 0.07
Nodes (23): here, OUT, SHOTS, VIEWPORT, APPS, CONTACTS, here, HIERARCHY (+15 more)

### Community 70 - "TailscaleUtils"
Cohesion: 0.28
Nodes (5): Context, Intent, TailscaleStatus, TailscaleUtils, ConnectivityManager

### Community 71 - "IntentHandler"
Cohesion: 0.17
Nodes (6): IntentHandler, UtteranceProgressListener, AudioManager, Command, TextToSpeech, UtteranceProgressListener

### Community 72 - "SmartLiftClassifier"
Cohesion: 0.12
Nodes (6): Edge, LANDED, LIFTED, REFIRE, SmartLiftClassifier, DoubleArray

### Community 73 - "LiveChatSection.vue"
Cohesion: 0.16
Nodes (13): chatBody, finished, isTyping, Message, replay(), runScript(), script, scrollToBottom() (+5 more)

### Community 74 - "pages/index.vue"
Cohesion: 0.09
Nodes (18): expanded, LEVELS, meta, payload, props, LogEntry, Stats, api (+10 more)

### Community 75 - ".sampleResult"
Cohesion: 0.12
Nodes (10): Bounds, ElementState, JsonObject, ObservedElement, ObserveResult, ScreenContext, Scrollable, WindowInfo (+2 more)

### Community 76 - "websocket/index.ts"
Cohesion: 0.15
Nodes (25): addLog(), closeDatabase(), getDevice(), updateDeviceLastSeen(), upsertDevice(), forwardAgentEvent(), Device, ServerConfig (+17 more)

### Community 77 - "devDependencies"
Cohesion: 0.13
Nodes (15): concurrently, devDependencies, concurrently, tsx, @types/better-sqlite3, @types/node, @types/ws, typescript (+7 more)

### Community 78 - "ContactHandler"
Cohesion: 0.34
Nodes (3): ContactHandler, Command, JsonObject

### Community 79 - "CompanionReaction"
Cohesion: 0.15
Nodes (12): CompanionReaction, BOOP, CHARGE, CURIOUS, LAND, LIFT, LOW_BATTERY, PING (+4 more)

### Community 80 - "McpDashboardScreen.kt"
Cohesion: 0.23
Nodes (17): AsterSectionHeader(), CodeBlock(), getCategoryIcon(), Color, ImageVector, Modifier, ToolRow(), ToolsSection() (+9 more)

### Community 83 - "AsterButton"
Cohesion: 0.18
Nodes (20): AnimatedEntrance(), AsterButton(), AsterButtonVariant, DANGER, PRIMARY, SECONDARY, AboutStep(), BottomNavigation() (+12 more)

### Community 85 - "FocusedNodeOutcome"
Cohesion: 0.33
Nodes (5): FocusedNodeOutcome, NO_FOCUSED_FIELD, PERFORMED, REFUSED, UNSUPPORTED_API

### Community 86 - "EventDeduplicator"
Cohesion: 0.20
Nodes (3): EventDeduplicator, CoroutineScope, Job

### Community 87 - "InstalledAppsHandler"
Cohesion: 0.18
Nodes (5): InstalledAppsHandler, Command, Command, PackageHandler, PackageManager

### Community 88 - "ModeType"
Cohesion: 0.23
Nodes (7): ModeType, IPC, LOCAL_MCP, REMOTE_WS, HomeViewModel, StateFlow, ViewModel

### Community 90 - "parseCompanionStatus"
Cohesion: 0.29
Nodes (4): CompanionStatusParseResult, parseCompanionStatus(), string(), CompanionStatusModelTest

### Community 91 - "repository"
Cohesion: 0.50
Nodes (4): repository, directory, type, url

### Community 92 - "CommandHandler"
Cohesion: 0.08
Nodes (20): SharedFlow, ToolCallLogger, Command, Context, ModeModule, CommandHandler, CapabilityHandler, ClipboardHandler (+12 more)

### Community 93 - "Screen"
Cohesion: 0.18
Nodes (11): Home, IpcDashboard, Logs, McpDashboard, Onboarding, PermissionAlert, Permissions, RemoteConnect (+3 more)

### Community 95 - "SmsHandler"
Cohesion: 0.31
Nodes (4): Command, JsonArray, Uri, SmsHandler

### Community 97 - "AsterWebSocketClient.kt"
Cohesion: 0.13
Nodes (14): AuthResult, CommandResponse, DeviceStatus, APPROVED, PENDING, REJECTED, EventMessage, HeartbeatAck (+6 more)

### Community 98 - "devices/index.vue"
Cohesion: 0.11
Nodes (11): api, counts, devices, filter, loading, { refresh }, removing, search (+3 more)

### Community 100 - "logs.vue"
Cohesion: 0.11
Nodes (17): api, deviceFilter, deviceIds, hasFilters, levels, live, load(), loading (+9 more)

### Community 101 - "ScreenAnnotator.kt"
Cohesion: 0.39
Nodes (4): Bitmap, Rect, ScreenAnnotator, Paint

### Community 102 - "parseCompanionPulseConfiguration"
Cohesion: 0.31
Nodes (3): CompanionPulseConfiguration, parseCompanionPulseConfiguration(), CompanionPulseConfigurationTest

### Community 105 - "HeroSection.vue"
Cohesion: 0.25
Nodes (8): assistants, conversations, currentIndex, goTo(), headlineHeight, headlineIndex, headlineRef, resetInterval()

### Community 106 - "HowItWorks.vue"
Cohesion: 0.22
Nodes (7): activeScenario, current, phase, protocols, Scenario, scenarios, timers

### Community 108 - "InteractiveOverlayModel"
Cohesion: 0.16
Nodes (8): Approval, ChoiceOption, Chooser, DraftVariant, InteractiveOverlayModel, JsonElement, JsonObject, TextField

### Community 109 - "KillSwitchController"
Cohesion: 0.19
Nodes (7): KillSwitchReceiver, BroadcastReceiver, Context, Intent, KillSwitchController, Context, Notification

### Community 110 - "queryParser.ts"
Cohesion: 0.39
Nodes (7): handleSearchMedia(), formatDate(), ParsedQuery, parseFileTypes(), parseLocation(), parseNaturalLanguageQuery(), parseTimeExpression()

### Community 112 - "InteractiveOverlayController"
Cohesion: 0.21
Nodes (7): CappedScrollView, InteractiveOverlayController, JsonObject, WindowManager, InteractivePrompt, CompletableDeferred, ScrollView

### Community 114 - "OnboardingViewModel"
Cohesion: 0.38
Nodes (3): StateFlow, ViewModel, OnboardingViewModel

### Community 115 - "CompanionSystemPulseMonitor"
Cohesion: 0.18
Nodes (6): CompanionSystemPulseMonitor, FloatArray, PowerManager, Sensor, SensorEvent, SensorEventListener

### Community 116 - "SettingsViewModel"
Cohesion: 0.38
Nodes (3): StateFlow, ViewModel, SettingsViewModel

### Community 117 - "Rect"
Cohesion: 0.15
Nodes (11): Rect, normalizeScrollAmount(), ScrollAmount, HALF_PAGE, PAGE, TO_EDGE, ScrollAxis, HORIZONTAL_BACKWARD (+3 more)

### Community 118 - "ContactsPanel.vue"
Cohesion: 0.14
Nodes (13): all, confirming, Contact, cursor, { data, loading, error, run, toast }, extract(), loadPage(), nextCursor (+5 more)

### Community 119 - "channels/index.ts"
Cohesion: 0.21
Nodes (9): getEventChannel(), optionalChannel(), send(), testConnection(), webhookUrl(), EventChannel, EventChannelConfig, EventChannelTestResult (+1 more)

### Community 120 - "handleGetDeviceInfo"
Cohesion: 0.33
Nodes (6): errorResult(), handleGetDeviceInfo(), handleTakePhoto(), handleTakeScreenshot(), imageResult(), textResult()

### Community 121 - "BootReceiver.kt"
Cohesion: 0.53
Nodes (4): BootReceiver, BroadcastReceiver, Context, Intent

### Community 122 - "StoragePanel.vue"
Cohesion: 0.14
Nodes (16): analysis, analyze(), breakdown, { data, loading, error, run }, findLarge(), LargeFile, largeFiles, media (+8 more)

### Community 123 - ".isSatisfied"
Cohesion: 0.53
Nodes (3): JsonArray, JsonObject, ScreenWaitMatcher

### Community 124 - "AudioPanel.vue"
Cohesion: 0.15
Nodes (11): audioSource, { data, loading, error, run, toast }, level, loadVolume(), mute(), props, setVolume(), stream (+3 more)

### Community 126 - "SignInWaitOverlay"
Cohesion: 0.28
Nodes (4): FrameLayout, TextView, WindowManager, SignInWaitOverlay

### Community 127 - "mediaAnalyzer.ts"
Cohesion: 0.47
Nodes (5): analyzeMediaResults(), generateSuggestions(), generateSummary(), MediaInsights, MediaSearchResult

### Community 128 - "WebSocketListener"
Cohesion: 0.26
Nodes (4): WebSocketListener, ReconnectPolicy, Response, WebSocket

### Community 129 - "GuidedPermissionFlow"
Cohesion: 0.27
Nodes (5): CompanionApprovalActivity, Activity, Bundle, GuidedPermissionFlow, Context

### Community 131 - "EmbraceSection.vue"
Cohesion: 0.40
Nodes (4): afterItems, beforeItems, ownPhoneItems, vignettes

### Community 132 - "ScreenshotsSection.vue"
Cohesion: 0.29
Nodes (6): activeTab, appScreenshots, dashboardScreenshots, shotTheme, tabs, themes

### Community 133 - "SecuritySection.vue"
Cohesion: 0.40
Nodes (4): permissionLayers, pillars, safetyPoints, tailscalePoints

### Community 134 - "files"
Cohesion: 0.40
Nodes (5): files, dist, dashboard/.output, LICENSE, README.md

### Community 135 - "AppsPanel.vue"
Cohesion: 0.16
Nodes (13): App, apps, cursor, { data, loading, error, run, toast }, detail, extract(), includeSystem, launch() (+5 more)

### Community 136 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 137 - "UseCasesSection.vue"
Cohesion: 0.50
Nodes (3): Group, groups, UseCase

### Community 138 - "UtilitiesPanel.vue"
Cohesion: 0.14
Nodes (8): callNumber, callScript, clipboardIn, clipboardOut, { data, loading, error, run, toast }, overlayHtml, props, toastMsg

### Community 141 - "screenshots/package.json"
Cohesion: 0.14
Nodes (13): playwright, description, devDependencies, playwright, name, private, scripts, capture (+5 more)

### Community 143 - "ScreenObserver"
Cohesion: 0.36
Nodes (4): Cand, AccessibilityNodeInfo, ScreenObserver, Walk

### Community 151 - "AlarmsPanel.vue"
Cohesion: 0.19
Nodes (11): Alarm, alarms, create(), { data, loading, error, run, toast }, dismiss(), hour, load(), message (+3 more)

### Community 166 - "MessagesPanel.vue"
Cohesion: 0.20
Nodes (10): body, { data, loading, error, run, toast }, limit, load(), messages, props, send(), sending (+2 more)

### Community 168 - "AsterTheme"
Cohesion: 0.20
Nodes (11): ApprovalBody(), BrandHeader(), ChooserBody(), InteractivePromptActivity, Bundle, ComponentActivity, AsterDarkColors, AsterLightColors (+3 more)

### Community 169 - "connect.vue"
Cohesion: 0.18
Nodes (9): ServerStatus, api, claudeCli, endpoints, loading, mcpConfig, mcpUrl, status (+1 more)

### Community 170 - ".performGesture"
Cohesion: 0.15
Nodes (8): GestureResultCallback, ResolvedBy, CENTER_TAP, NEAREST_BOUNDS, TEXT_ROLE, VIEW_ID, ScreenActionResult, GestureDescription

### Community 171 - ".resolveRef"
Cohesion: 0.21
Nodes (4): DescriptorBounds, NodeDescriptor, AccessibilityNodeInfo, ResolvedRef

### Community 174 - "OverlayPermissionActivity.kt"
Cohesion: 0.60
Nodes (3): Activity, Bundle, OverlayPermissionActivity

### Community 177 - "CompanionViseme"
Cohesion: 0.22
Nodes (9): CompanionViseme, CLOSED, NEUTRAL, OPEN, REST, ROUND, TEETH, TONGUE (+1 more)

### Community 178 - "panels.vue"
Cohesion: 0.22
Nodes (8): active, api, current, device, deviceId, PANELS, route, router

### Community 179 - "AsterIpcService"
Cohesion: 0.43
Nodes (4): AsterIpcService, IBinder, Intent, Service

### Community 180 - "VolumeHandler"
Cohesion: 0.39
Nodes (3): AudioManager, Command, VolumeHandler

### Community 181 - "LocationPanel.vue"
Cohesion: 0.25
Nodes (6): { data, loading, error, run }, hasFix, Loc, mapUrl, osmLink, props

### Community 182 - "run.mjs"
Cohesion: 0.25
Nodes (3): children, DASHBOARD, here

### Community 184 - "McpDashboardViewModel.kt"
Cohesion: 0.38
Nodes (3): StateFlow, ViewModel, McpDashboardViewModel

### Community 186 - "Backlog"
Cohesion: 0.29
Nodes (6): 1. 21 device actions are implemented on the phone but unreachable from the server, 2. The API has no authentication and binds `0.0.0.0`, 3. Android screenshots are still captured by hand, 4. Foreground-service notification icons are launcher icons, 5. Small `mcp/package.json` inaccuracies, Backlog

### Community 187 - "aster-ui/package.json"
Cohesion: 0.29
Nodes (6): description, main, name, private, type, version

### Community 189 - "ShellPanel.vue"
Cohesion: 0.33
Nodes (4): command, history, props, { raw, loading, error, run }

### Community 190 - "default.vue"
Cohesion: 0.33
Nodes (4): config, mobileNavOpen, NAV, route

### Community 191 - "useColorScheme"
Cohesion: 0.47
Nodes (5): ColorScheme, useColorScheme(), apply(), init(), set()

### Community 192 - "usePolling"
Cohesion: 0.60
Nodes (5): usePolling(), onVisibility(), start(), stop(), tick()

### Community 194 - "Quick Start"
Cohesion: 0.33
Nodes (6): 1. Install the MCP Server, 2. Install the Android App, 3. Start the Server, 4. Connect Your Device, 5. Configure Your AI Client, Quick Start

### Community 195 - "Security & Privacy"
Cohesion: 0.33
Nodes (6): Android permissions — and why each one, Device approval gate (status-based, no shared secret), Nothing leaves your network by default, On-device safety rails, Security & Privacy, The Node `ws` server does not terminate TLS

### Community 197 - "AStatusPill.vue"
Cohesion: 0.40
Nodes (4): MAP, meta, props, shouldPulse

### Community 198 - "useToast"
Cohesion: 0.50
Nodes (4): Toast, useToast(), dismiss(), push()

### Community 199 - "Development"
Cohesion: 0.40
Nodes (5): Adding a new MCP tool, Android app, Development, MCP server, Prerequisites

### Community 200 - "Integrations"
Cohesion: 0.40
Nodes (5): Any MCP-Compatible Client, Claude Code / Claude Desktop, Integrations, OpenClaw / Moltbot / Clawbot, Which URL goes where (topologies)

### Community 203 - "MetricGauge.vue"
Cohesion: 0.50
Nodes (3): clamped, dash, props

### Community 205 - "AModal.vue"
Cohesion: 0.67
Nodes (3): close(), onKeydown(), open

### Community 206 - "AToastHost.vue"
Cohesion: 0.50
Nodes (3): ICONS, { toasts, dismiss }, TONES

### Community 207 - "Usage"
Cohesion: 0.50
Nodes (4): CLI Commands, Example Prompts, Status & Health Checks, Usage

### Community 208 - "Securing the connection"
Cohesion: 0.50
Nodes (4): Reverse proxy (Traefik / Caddy), Securing the connection, Tailscale Serve — `wss://` for the app, Trusted LAN — `ws://`

### Community 209 - "sync.mjs"
Cohesion: 0.50
Nodes (3): DEST, here, SRC

### Community 212 - "Proactive Event Forwarding"
Cohesion: 0.67
Nodes (3): Mattermost incoming webhooks, Proactive Event Forwarding, Sample OpenClaw hooks

## Knowledge Gaps
- **710 isolated node(s):** `aster`, `Keys`, `DATE_ASC`, `DATE_DESC`, `SIZE_ASC` (+705 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1115 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **46 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CompanionFaceOverlay` connect `CompanionFaceOverlay` to `CompanionFaceView`, `CommandHandler`, `CompanionSystemPulseMonitor`, `CompanionStatusModel`, `IpcMode`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Why does `AsterAccessibilityService` connect `AsterAccessibilityService` to `InputRouter`, `SnapshotCache`, `AsterAccessibilityService.kt`, `ScreenSyncTracker`, `AccessibilityHandler`, `.performGesture`, `.sampleResult`, `.resolveRef`, `PackagePolicyGuardTest`, `CommandHandler`, `ScreenObserveSupportTest`, `FocusedNodeOutcome`, `Rect`, `AccessibilityPulseClassifier`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Why does `AsterWebSocketClient` connect `AsterWebSocketClient` to `WebSocketListener`, `AsterWebSocketClient.kt`, `ConnectionState`, `WsUrlPolicy`, `ModeStatus`, `TailscaleUtils`, `.sendAuthMessage`, `AppModule.kt`, `AsterService`, `CommandHandler`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `AsterAccessibilityService` (e.g. with `OcrEngine` and `ScreenSyncTracker`) actually correct?**
  _`AsterAccessibilityService` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `aster`, `Keys`, `DATE_ASC` to the rest of the system?**
  _710 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InputRouter` be split into smaller, more focused modules?**
  _Cohesion score 0.06980392156862746 - nodes in this community are weakly interconnected._
- **Should `StorageHandler` be split into smaller, more focused modules?**
  _Cohesion score 0.06954887218045112 - nodes in this community are weakly interconnected._
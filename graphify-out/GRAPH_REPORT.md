# Graph Report - aster-mcp  (2026-09-01)

## Corpus Check
- 246 files · ~571,215 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2767 nodes · 5580 edges · 176 communities (118 shown, 43 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 237 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ff27ac75`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- InteractiveOverlayController
- InputRouter
- StorageHandler
- PermissionUtils
- handler.ts
- AsterAccessibilityService.kt
- ToolExecutionOverlay
- handleToolCall
- CommandResult
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
- websocket/index.ts
- ToolInfo
- IpcMode
- compilerOptions
- Aster - Your AI CoPilot on Mobile
- event-forwarding/index.ts
- CameraHandler
- control.vue
- ModeStatus
- Dp
- mcp/package.json
- ScreenSyncTracker
- CompanionFaceView.kt
- CompanionNativeAnimator
- MediaHandler
- AsterAccessibilityService
- mcp/README.md
- CompanionFaceView
- event-forwarding.vue
- ToolCallLogDao
- AsterCard
- aster.ts
- CallStateMonitor
- OverlayHandler
- McpMode
- IpcApprovalActivity.kt
- CompanionViseme
- CutoutBounds
- RemoteConnectScreen.kt
- ScreenActionResultTest
- WireContractTest
- Canvas
- FileRow.vue
- AccessibilityPulseClassifier
- SettingsDataStore
- HostDirHandler
- dependencies
- scripts
- CompanionFaceOverlay
- keywords
- ConnectionState
- SnapshotCache
- IpcDashboardScreen.kt
- TailscaleUtils
- IntentHandler
- SmartLiftClassifier
- LiveChatSection.vue
- pages/index.vue
- ScreenObserver
- AlarmHandler
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
- FilePreview.vue
- devices/index.vue
- DeviceInfoHandler
- .runOnMain
- ScreenAnnotator.kt
- parseCompanionPulseConfiguration
- ObserveRankTest
- ScreenWaitMatcherTest
- HeroSection.vue
- HowItWorks.vue
- .mcp.json
- InteractiveOverlayModel
- files.vue
- queryParser.ts
- LabelAggregator
- InteractivePrompt
- WireParams
- OnboardingViewModel
- SettingsScreen.kt
- SettingsViewModel
- AsterTheme
- CreateFolderModal.vue
- channels/index.ts
- handleGetDeviceInfo
- BootReceiver.kt
- IpcDashboardViewModel.kt
- .isSatisfied
- ClipboardHandler
- ScreenAnnotatorGeometryTest
- SignInWaitOverlay
- mediaAnalyzer.ts
- AsterDatabase
- GuidedPermissionFlow
- BoundsTest
- EmbraceSection.vue
- ScreenshotsSection.vue
- SecuritySection.vue
- files
- TakeScreenshotCallback
- gradlew
- UseCasesSection.vue
- QuickAccessBar.vue
- ConnectionFailureMapperTest
- ProactiveSection.vue
- AsciiHeader.vue
- TerminalPageHeader.vue
- dashboard/tsconfig.json
- FeaturesGrid.vue
- IntegrationsSection.vue
- NavBar.vue
- SetupSteps.vue
- ToolsShowcase.vue
- build-python-android.sh
- StatusBadge.vue
- TerminalDetailHeader.vue
- installedApps.test.ts
- InteractivePromptActivity.kt
- .setSafeGeometry
- ResolvedBy
- NodeDescriptor.kt
- ReconnectPolicyTest
- TextWatcher
- OverlayPermissionActivity.kt
- .executeShell

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
- `AsterAccessibilityService` --calls--> `OcrEngine`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/AsterAccessibilityService.kt → apps/android/app/src/main/java/com/aster/service/OcrEngine.kt
- `AsterAccessibilityService` --calls--> `ScreenSyncTracker`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/AsterAccessibilityService.kt → apps/android/app/src/main/java/com/aster/service/ScreenSyncTracker.kt
- `CompanionFaceOverlay` --calls--> `CompanionSystemPulseMonitor`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/overlay/CompanionFaceOverlay.kt → apps/android/app/src/main/java/com/aster/service/overlay/CompanionSystemPulseMonitor.kt
- `CompanionFaceView` --calls--> `CompanionNativeAnimator`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/overlay/CompanionFaceView.kt → apps/android/app/src/main/java/com/aster/service/overlay/CompanionNativeAnimator.kt
- `CompanionSystemPulseMonitor` --calls--> `SmartLiftClassifier`  [INFERRED]
  apps/android/app/src/main/java/com/aster/service/overlay/CompanionSystemPulseMonitor.kt → apps/android/app/src/main/java/com/aster/service/overlay/SmartLiftClassifier.kt

## Import Cycles
- None detected.

## Communities (176 total, 43 thin omitted)

### Community 0 - "InteractiveOverlayController"
Cohesion: 0.29
Nodes (6): InteractiveOverlayController, TextView, View, EditText, GradientDrawable, LinearLayout

### Community 1 - "InputRouter"
Cohesion: 0.06
Nodes (23): InputModule, AccessibilityInputBackend, AccessibilityKeyRoute, GLOBAL_BACK, GLOBAL_HOME, GLOBAL_RECENTS, IME_ENTER, NODE_COPY (+15 more)

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
Cohesion: 0.08
Nodes (19): AccessibilityService, ActionMapper, RoleMapper, dragStrokePoints(), keyNameToKeycode(), normalizeScrollAmount(), normalizeScrollDirection(), pinchStrokePoints() (+11 more)

### Community 6 - "ToolExecutionOverlay"
Cohesion: 0.08
Nodes (17): Started, ToolEvent, KillSwitchReceiver, BroadcastReceiver, Context, Intent, Context, CoroutineScope (+9 more)

### Community 7 - "handleToolCall"
Cohesion: 0.12
Nodes (48): handleAnalyzeStorage(), handleClickById(), handleClickByText(), handleDeleteAlarm(), handleDeleteContacts(), handleDeleteFile(), handleDismissAlarm(), handleExecuteShell() (+40 more)

### Community 8 - "CommandResult"
Cohesion: 0.17
Nodes (8): CommandResult, AccessibilityHandler, Command, JsonArray, JsonElement, JsonObject, PreAct, Mark

### Community 9 - "AsterWebSocketClient"
Cohesion: 0.05
Nodes (28): AuthMessage, AuthResult, CommandResponse, DeviceStatus, APPROVED, PENDING, REJECTED, EventMessage (+20 more)

### Community 10 - "README.md"
Cohesion: 0.04
Nodes (46): 1. Install the MCP Server, 2. Install the Android App, 3. Start the Server, 4. Connect Your Device, 5. Configure Your AI Client, Adding a new MCP tool, Android app, Android permissions — and why each one (+38 more)

### Community 11 - ".put"
Cohesion: 0.08
Nodes (8): Command, Command, FileSystemHandler, Command, Command, Command, Command, InteractiveOverlayModelTest

### Community 12 - "SmsBroadcastReceiver"
Cohesion: 0.31
Nodes (7): BroadcastReceiver, Context, Intent, SmsBroadcastReceiver, body, sender, timestamp

### Community 13 - "AppModule.kt"
Cohesion: 0.35
Nodes (5): AppModule, Context, DataStore, OkHttpClient, Preferences

### Community 14 - "PackagePolicyGuardTest"
Cohesion: 0.09
Nodes (4): Command, PackagePolicyGuard, AccessibilityHandlerActionsTest, PackagePolicyGuardTest

### Community 15 - "devDependencies"
Cohesion: 0.06
Nodes (31): @iconify-json/ph, dependencies, nuxt, @nuxt/fonts, devDependencies, @iconify-json/lucide, @iconify-json/mdi, @iconify-json/ph (+23 more)

### Community 16 - "MainActivity.kt"
Cohesion: 0.18
Nodes (16): AnimatedEntrance(), Modifier, AsterNavHost(), Bundle, ComponentActivity, Modifier, MainActivity, AboutStep() (+8 more)

### Community 17 - "FileBrowser.vue"
Cohesion: 0.09
Nodes (31): api, confirmDelete(), createFolder(), creatingFolder, currentPath, deletingFile, downloadFile(), error (+23 more)

### Community 18 - "dependencies"
Cohesion: 0.06
Nodes (35): dependencies, nuxt, @nuxt/fonts, @nuxt/icon, tailwindcss, @tailwindcss/vite, vue, vue-router (+27 more)

### Community 19 - "ScreenObserveSupportTest"
Cohesion: 0.10
Nodes (11): Bitmap, com, OcrBlock, OcrEngine, JsonArray, JsonObject, MergeResult, ScreenObserveSupport (+3 more)

### Community 20 - "AsterService"
Cohesion: 0.12
Nodes (11): AsterService, com, Context, IBinder, Intent, Job, JsonElement, Notification (+3 more)

### Community 21 - "AsterNotificationListenerService"
Cohesion: 0.08
Nodes (16): BroadcastReceiver, Context, Intent, NotificationActionReceiver, AsterNotificationListenerService, JsonArray, JsonObject, NotificationAction (+8 more)

### Community 22 - "src/index.ts"
Cohesion: 0.12
Nodes (31): closeDatabase(), initDatabase(), loadAgentEventForwardingConfig(), ASTER_DIR, currentFile, getLocalIP(), mainScript, removeStatusFile() (+23 more)

### Community 23 - "useApi.ts"
Cohesion: 0.09
Nodes (19): levelIcons, emit, platformIcons, props, AgentEventForwardingConfig, AgentEventForwardingConfigResponse, AgentEventForwardingTestResult, Device (+11 more)

### Community 24 - "[id]/index.vue"
Cohesion: 0.08
Nodes (19): api, autoScroll, device, deviceId, error, fetchData(), fetchLiveInfo(), handleApprove() (+11 more)

### Community 25 - "server/index.ts"
Cohesion: 0.16
Nodes (26): addLog(), DbDevice, DbLogEntry, deleteDevice(), getAllDevices(), getAllLogs(), getDatabase(), getDevice() (+18 more)

### Community 26 - "ElementFilterTest"
Cohesion: 0.14
Nodes (4): ElementFilter, NodeFacts, ObserveMode, ElementFilterTest

### Community 27 - "websocket/index.ts"
Cohesion: 0.08
Nodes (36): AuthMessage, AuthMessageSchema, AuthResult, AuthResultSchema, Command, CommandResponse, CommandResponseSchema, CommandSchema (+28 more)

### Community 28 - "ToolInfo"
Cohesion: 0.14
Nodes (6): ToolCatalog, ToolEntry, ToolInfo, StateFlow, ViewModel, McpDashboardViewModel

### Community 29 - "IpcMode"
Cohesion: 0.17
Nodes (7): AsterIpcService, IBinder, Intent, Service, IpcMode, StateFlow, IAsterService

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (24): compilerOptions, declaration, declarationMap, esModuleInterop, forceConsistentCasingInFileNames, lib, module, moduleResolution (+16 more)

### Community 31 - "Aster - Your AI CoPilot on Mobile"
Cohesion: 0.11
Nodes (18): Apps & System, Aster - Your AI CoPilot on Mobile, Available Tools, Commands, Device Features, Device & Screen, Event Format, Event Types (+10 more)

### Community 32 - "event-forwarding/index.ts"
Cohesion: 0.16
Nodes (20): getEventChannel(), AgentEventForwardingConfig, buildEventText(), ConfigCandidate, DeviceContext, formatData(), forwardAgentEvent(), getAgentEventForwardingConfig() (+12 more)

### Community 33 - "CameraHandler"
Cohesion: 0.18
Nodes (9): CameraHandler, OnImageCapturedCallback, Command, ServiceLifecycleOwner, CameraSelector, ImageCaptureException, ImageProxy, LifecycleOwner (+1 more)

### Community 34 - "control.vue"
Cohesion: 0.09
Nodes (18): api, commandHistory, device, deviceId, executing, executionResult, filteredTools, isVibrateTool (+10 more)

### Community 35 - "ModeStatus"
Cohesion: 0.17
Nodes (11): ConnectionMode, IpcConfig, StateFlow, McpConfig, ModeConfig, ModeStatus, RemoteConfig, Command (+3 more)

### Community 36 - "Dp"
Cohesion: 0.30
Nodes (11): GlowOrb(), Color, Modifier, BadgeItem, Color, ImageVector, Modifier, ModeCard() (+3 more)

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
Cohesion: 0.05
Nodes (15): NodeDescriptor, A11yObservation, AsterAccessibilityService, GestureResultCallback, AccessibilityNodeInfo, android, JsonElement, JsonObject (+7 more)

### Community 43 - "mcp/README.md"
Cohesion: 0.12
Nodes (15): Any MCP-Compatible Client, Available MCP Tools, Claude Code / Claude Desktop, Commands, Connect Your AI Assistant, Installation, License, OpenAlly (+7 more)

### Community 44 - "CompanionFaceView"
Cohesion: 0.15
Nodes (3): CompanionFaceView, View, FrameCallback

### Community 45 - "event-forwarding.vue"
Cohesion: 0.09
Nodes (18): api, cancelEditing(), configuredAt, defaultEvents, form, hasExistingConfig, hasSourceToken, isEditing (+10 more)

### Community 46 - "ToolCallLogDao"
Cohesion: 0.16
Nodes (7): ToolCallLog, Flow, ToolCallLogDao, Completed, StateFlow, ViewModel, LogViewModel

### Community 47 - "AsterCard"
Cohesion: 0.28
Nodes (16): AsterCard(), AsterSectionHeader(), AsterStatCard(), AsterTopBar(), InfoRow(), Color, ImageVector, Modifier (+8 more)

### Community 48 - "aster.ts"
Cohesion: 0.11
Nodes (17): ASTER_DIR, daemonDeviceAction(), devicesCmd, __dirname, displayStatus(), __filename, isProcessRunning(), lanMcpUrl() (+9 more)

### Community 49 - "CallStateMonitor"
Cohesion: 0.10
Nodes (13): CompanionSystemPulseMonitor, Api31Callback, CallStateMonitor, CallStateSnapshot, StateFlow, LegacyListener, CallStateListener, FloatArray (+5 more)

### Community 50 - "OverlayHandler"
Cohesion: 0.22
Nodes (5): Command, View, WindowManager, OverlayHandler, OverlayInstance

### Community 51 - "McpMode"
Cohesion: 0.23
Nodes (5): StateFlow, McpMode, CIOApplicationEngine, EmbeddedServer, StreamableHttpServerTransport

### Community 52 - "IpcApprovalActivity.kt"
Cohesion: 0.16
Nodes (14): ModeState, ERROR, IDLE, RUNNING, STARTING, STOPPING, IpcApprovalActivity, Bitmap (+6 more)

### Community 53 - "CompanionViseme"
Cohesion: 0.11
Nodes (20): aggregateCompanionSpeech(), CompanionFaceStateSnapshot, CompanionSpeechAggregate, CompanionSpeechSource, AUDIO, STREAM, CompanionSpeechStream, CompanionViseme (+12 more)

### Community 55 - "RemoteConnectScreen.kt"
Cohesion: 0.24
Nodes (16): AsterTextField(), AboutRemoteSection(), InstallationSection(), Color, ImageVector, NumberedStep(), OpenClawSection(), OptionHeader() (+8 more)

### Community 58 - "Canvas"
Cohesion: 0.38
Nodes (3): FaceGroup, Canvas, RectF

### Community 59 - "FileRow.vue"
Cohesion: 0.13
Nodes (8): emit, handleKeydown(), props, emit, emit, handleClick(), props, FileEntry

### Community 60 - "AccessibilityPulseClassifier"
Cohesion: 0.20
Nodes (5): AccessibilityEvent, AccessibilityPulseClassifier, Input, Pulse, AccessibilityPulseClassifierTest

### Community 61 - "SettingsDataStore"
Cohesion: 0.14
Nodes (4): Keys, Flow, ServerConfig, SettingsDataStore

### Community 62 - "HostDirHandler"
Cohesion: 0.27
Nodes (4): HostDirHandler, ByteArray, Command, Uri

### Community 63 - "dependencies"
Cohesion: 0.10
Nodes (21): better-sqlite3, chalk, commander, consola, dotenv, fastify, @fastify/static, dependencies (+13 more)

### Community 64 - "scripts"
Cohesion: 0.18
Nodes (11): scripts, build, build:all, dev, dev:all, dev:dashboard, lint, postinstall (+3 more)

### Community 65 - "CompanionFaceOverlay"
Cohesion: 0.13
Nodes (9): CompanionFaceOverlay, FrameLayout, Rect, View, WindowManager, PowerSnapshot, ReceivedFaceState, CompanionStatusModel (+1 more)

### Community 66 - "keywords"
Cohesion: 0.12
Nodes (16): keywords, accessibility, ai, ai-phone, android, automation, claude, clawdbot (+8 more)

### Community 67 - "ConnectionState"
Cohesion: 0.15
Nodes (12): ConnectionState, APPROVED, CONNECTED, CONNECTING, DISCONNECTED, ERROR, PENDING_APPROVAL, RECONNECTING (+4 more)

### Community 69 - "IpcDashboardScreen.kt"
Cohesion: 0.26
Nodes (12): Modifier, TokenDisplay(), getCategoryIcon(), Color, ImageVector, Modifier, ToolRow(), ToolsSection() (+4 more)

### Community 70 - "TailscaleUtils"
Cohesion: 0.30
Nodes (4): Context, Intent, TailscaleStatus, TailscaleUtils

### Community 71 - "IntentHandler"
Cohesion: 0.17
Nodes (6): IntentHandler, UtteranceProgressListener, AudioManager, Command, TextToSpeech, UtteranceProgressListener

### Community 72 - "SmartLiftClassifier"
Cohesion: 0.11
Nodes (7): Edge, LANDED, LIFTED, REFIRE, SmartLiftClassifier, SmartLiftClassifierTest, DoubleArray

### Community 73 - "LiveChatSection.vue"
Cohesion: 0.16
Nodes (13): chatBody, finished, isTyping, Message, replay(), runScript(), script, scrollToBottom() (+5 more)

### Community 74 - "pages/index.vue"
Cohesion: 0.16
Nodes (12): Stats, api, currentTime, devices, eventForwardingEnabled, fetchData(), handleApprove(), handleReject() (+4 more)

### Community 75 - "ScreenObserver"
Cohesion: 0.08
Nodes (15): ObserveBudget, Bounds, ElementState, JsonObject, ObservedElement, ObserveResult, ScreenContext, Scrollable (+7 more)

### Community 77 - "devDependencies"
Cohesion: 0.13
Nodes (15): concurrently, devDependencies, concurrently, tsx, @types/better-sqlite3, @types/node, @types/ws, typescript (+7 more)

### Community 78 - "ContactHandler"
Cohesion: 0.38
Nodes (3): ContactHandler, Command, JsonObject

### Community 79 - "CompanionReaction"
Cohesion: 0.15
Nodes (12): CompanionReaction, BOOP, CHARGE, CURIOUS, LAND, LIFT, LOW_BATTERY, PING (+4 more)

### Community 80 - "McpDashboardScreen.kt"
Cohesion: 0.29
Nodes (11): CodeBlock(), Modifier, ConnectionMethodHint(), Color, McpConfigSection(), McpDashboardScreen(), ServerEndpointsSection(), SetupGuideSection() (+3 more)

### Community 83 - "AsterButton"
Cohesion: 0.29
Nodes (11): AsterButton(), AsterButtonVariant, DANGER, PRIMARY, SECONDARY, Color, ImageVector, Modifier (+3 more)

### Community 85 - "FocusedNodeOutcome"
Cohesion: 0.33
Nodes (5): FocusedNodeOutcome, NO_FOCUSED_FIELD, PERFORMED, REFUSED, UNSUPPORTED_API

### Community 86 - "EventDeduplicator"
Cohesion: 0.20
Nodes (3): EventDeduplicator, CoroutineScope, Job

### Community 88 - "ModeType"
Cohesion: 0.25
Nodes (7): ModeType, IPC, LOCAL_MCP, REMOTE_WS, HomeViewModel, StateFlow, ViewModel

### Community 90 - "parseCompanionStatus"
Cohesion: 0.29
Nodes (4): CompanionStatusParseResult, parseCompanionStatus(), string(), CompanionStatusModelTest

### Community 91 - "repository"
Cohesion: 0.50
Nodes (4): repository, directory, type, url

### Community 92 - "CommandHandler"
Cohesion: 0.05
Nodes (28): SharedFlow, ToolCallLogger, Command, Context, ModeModule, CommandHandler, Command, JsonElement (+20 more)

### Community 93 - "Screen"
Cohesion: 0.18
Nodes (11): Home, IpcDashboard, Logs, McpDashboard, Onboarding, PermissionAlert, Permissions, RemoteConnect (+3 more)

### Community 95 - "SmsHandler"
Cohesion: 0.33
Nodes (4): Command, JsonArray, Uri, SmsHandler

### Community 97 - "FilePreview.vue"
Cohesion: 0.29
Nodes (6): emit, getFileExtension(), imageDataUrl, isImageFile(), isTextFile(), props

### Community 98 - "devices/index.vue"
Cohesion: 0.24
Nodes (9): api, counts, devices, fetchDevices(), filter, filteredDevices, handleApprove(), handleReject() (+1 more)

### Community 101 - "ScreenAnnotator.kt"
Cohesion: 0.39
Nodes (4): Bitmap, Rect, ScreenAnnotator, Paint

### Community 102 - "parseCompanionPulseConfiguration"
Cohesion: 0.25
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

### Community 109 - "files.vue"
Cohesion: 0.25
Nodes (6): api, device, deviceId, error, loading, route

### Community 110 - "queryParser.ts"
Cohesion: 0.39
Nodes (7): handleSearchMedia(), formatDate(), ParsedQuery, parseFileTypes(), parseLocation(), parseNaturalLanguageQuery(), parseTimeExpression()

### Community 112 - "InteractivePrompt"
Cohesion: 0.19
Nodes (6): CappedScrollView, JsonObject, WindowManager, InteractivePrompt, CompletableDeferred, ScrollView

### Community 114 - "OnboardingViewModel"
Cohesion: 0.38
Nodes (3): StateFlow, ViewModel, OnboardingViewModel

### Community 115 - "SettingsScreen.kt"
Cohesion: 0.57
Nodes (6): AboutInfoRow(), ImageVector, Modifier, ModeOption(), SettingsScreen(), ThemeChip()

### Community 116 - "SettingsViewModel"
Cohesion: 0.38
Nodes (3): StateFlow, ViewModel, SettingsViewModel

### Community 117 - "AsterTheme"
Cohesion: 0.33
Nodes (5): AsterDarkColors, AsterLightColors, SemanticColors, AsterColorScheme, AsterTheme()

### Community 118 - "CreateFolderModal.vue"
Cohesion: 0.38
Nodes (6): emit, folderName, handleKeydown(), handleSubmit(), inputRef, props

### Community 119 - "channels/index.ts"
Cohesion: 0.23
Nodes (8): optionalChannel(), send(), testConnection(), webhookUrl(), EventChannel, EventChannelConfig, EventChannelTestResult, EventChannelType

### Community 120 - "handleGetDeviceInfo"
Cohesion: 0.33
Nodes (6): errorResult(), handleGetDeviceInfo(), handleTakePhoto(), handleTakeScreenshot(), imageResult(), textResult()

### Community 121 - "BootReceiver.kt"
Cohesion: 0.53
Nodes (4): BootReceiver, BroadcastReceiver, Context, Intent

### Community 122 - "IpcDashboardViewModel.kt"
Cohesion: 0.38
Nodes (3): IpcDashboardViewModel, StateFlow, ViewModel

### Community 123 - ".isSatisfied"
Cohesion: 0.53
Nodes (3): JsonArray, JsonObject, ScreenWaitMatcher

### Community 126 - "SignInWaitOverlay"
Cohesion: 0.28
Nodes (4): FrameLayout, TextView, WindowManager, SignInWaitOverlay

### Community 127 - "mediaAnalyzer.ts"
Cohesion: 0.47
Nodes (5): analyzeMediaResults(), generateSuggestions(), generateSummary(), MediaInsights, MediaSearchResult

### Community 129 - "GuidedPermissionFlow"
Cohesion: 0.27
Nodes (5): CompanionApprovalActivity, Activity, Bundle, GuidedPermissionFlow, Context

### Community 131 - "EmbraceSection.vue"
Cohesion: 0.40
Nodes (4): afterItems, beforeItems, ownPhoneItems, vignettes

### Community 132 - "ScreenshotsSection.vue"
Cohesion: 0.29
Nodes (6): activeTab, appScreenshots, appTheme, dashboardScreenshots, tabs, themes

### Community 133 - "SecuritySection.vue"
Cohesion: 0.40
Nodes (4): permissionLayers, pillars, safetyPoints, tailscalePoints

### Community 134 - "files"
Cohesion: 0.40
Nodes (5): files, dist, dashboard/.output, LICENSE, README.md

### Community 136 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 137 - "UseCasesSection.vue"
Cohesion: 0.50
Nodes (3): Group, groups, UseCase

### Community 168 - "InteractivePromptActivity.kt"
Cohesion: 0.39
Nodes (6): ApprovalBody(), BrandHeader(), ChooserBody(), InteractivePromptActivity, Bundle, ComponentActivity

### Community 169 - ".setSafeGeometry"
Cohesion: 0.47
Nodes (3): CompanionOverlayGeometry, OverlayGeometry, SafeBounds

### Community 170 - "ResolvedBy"
Cohesion: 0.33
Nodes (5): ResolvedBy, CENTER_TAP, NEAREST_BOUNDS, TEXT_ROLE, VIEW_ID

### Community 174 - "OverlayPermissionActivity.kt"
Cohesion: 0.60
Nodes (3): Activity, Bundle, OverlayPermissionActivity

## Knowledge Gaps
- **507 isolated node(s):** `aster`, `Keys`, `DATE_ASC`, `DATE_DESC`, `SIZE_ASC` (+502 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 875 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **43 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CompanionFaceOverlay` connect `CompanionFaceOverlay` to `.runOnMain`, `parseCompanionPulseConfiguration`, `CompanionFaceView`, `CallStateMonitor`, `CommandHandler`, `IpcMode`?**
  _High betweenness centrality (0.091) - this node is a cross-community bridge._
- **Why does `AsterAccessibilityService` connect `AsterAccessibilityService` to `InputRouter`, `SnapshotCache`, `AsterAccessibilityService.kt`, `ScreenSyncTracker`, `CommandResult`, `PackagePolicyGuardTest`, `CommandHandler`, `ScreenObserveSupportTest`, `FocusedNodeOutcome`, `AccessibilityPulseClassifier`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **Why does `AsterWebSocketClient` connect `AsterWebSocketClient` to `ModeStatus`, `ConnectionState`, `AppModule.kt`, `AsterService`, `CommandHandler`?**
  _High betweenness centrality (0.051) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `AsterAccessibilityService` (e.g. with `OcrEngine` and `ScreenSyncTracker`) actually correct?**
  _`AsterAccessibilityService` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `aster`, `Keys`, `DATE_ASC` to the rest of the system?**
  _507 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `InputRouter` be split into smaller, more focused modules?**
  _Cohesion score 0.0632996632996633 - nodes in this community are weakly interconnected._
- **Should `StorageHandler` be split into smaller, more focused modules?**
  _Cohesion score 0.06954887218045112 - nodes in this community are weakly interconnected._
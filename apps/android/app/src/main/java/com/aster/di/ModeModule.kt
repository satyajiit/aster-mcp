package com.aster.di

import android.content.Context
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.CommandHandler
import com.aster.service.handlers.AccessibilityHandler
import com.aster.service.handlers.AlarmHandler
import com.aster.service.handlers.CameraHandler
import com.aster.service.handlers.CapabilityHandler
import com.aster.service.handlers.ClipboardHandler
import com.aster.service.handlers.CompanionOverlayHandler
import com.aster.service.handlers.ContactHandler
import com.aster.service.handlers.DeviceInfoHandler
import com.aster.service.handlers.FileSystemHandler
import com.aster.service.handlers.HostDirHandler
import com.aster.service.handlers.InstalledAppsHandler
import com.aster.service.handlers.IntentHandler
import com.aster.service.handlers.InteractiveOverlayHandler
import com.aster.service.handlers.MediaHandler
import com.aster.service.handlers.NotificationHandler
import com.aster.service.handlers.NowPlayingHandler
import com.aster.service.handlers.OverlayHandler
import com.aster.service.handlers.PackageHandler
import com.aster.service.handlers.PolicyHandler
import com.aster.service.handlers.ShellHandler
import com.aster.service.handlers.SignInWaitHandler
import com.aster.service.handlers.SmsHandler
import com.aster.service.handlers.StorageHandler
import com.aster.service.handlers.VolumeHandler
import com.aster.service.mode.IpcMode
import com.aster.service.mode.McpMode
import com.aster.service.mode.RemoteWsMode
import com.aster.service.overlay.CompanionFaceOverlay
import com.aster.service.overlay.InteractiveOverlayController
import com.aster.service.overlay.SignInWaitOverlay
import com.aster.service.overlay.ToolExecutionOverlay
import com.aster.service.safety.GuardedCommandHandler
import com.aster.service.safety.PackagePolicyGuard
import com.aster.service.telephony.CallStateMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CommandHandlerMap

@Module
@InstallIn(SingletonComponent::class)
object ModeModule {

    @Provides
    @Singleton
    @CommandHandlerMap
    fun provideCommandHandlers(
        @ApplicationContext context: Context,
        packagePolicyGuard: PackagePolicyGuard,
        interactiveOverlayController: InteractiveOverlayController,
        toolExecutionOverlay: ToolExecutionOverlay,
        companionFaceOverlay: CompanionFaceOverlay,
        callStateMonitor: CallStateMonitor
    ): Map<String, @JvmSuppressWildcards CommandHandler> {
        val handlers = mutableMapOf<String, CommandHandler>()

        val allHandlers = listOf(
            DeviceInfoHandler(context),
            FileSystemHandler(context),
            HostDirHandler(context),
            PackageHandler(context),
            InstalledAppsHandler(context),
            ClipboardHandler(context),
            MediaHandler(context),
            NowPlayingHandler(context),
            ShellHandler(),
            IntentHandler(context, callStateMonitor),
            AccessibilityHandler(),
            NotificationHandler(),
            SmsHandler(context),
            OverlayHandler(context),
            StorageHandler(context),
            VolumeHandler(context),
            ContactHandler(context),
            AlarmHandler(context),
            CameraHandler(context),
            InteractiveOverlayHandler(interactiveOverlayController),
            SignInWaitHandler(SignInWaitOverlay(context), toolExecutionOverlay),
            // The ambient OpenAlly companion face. Only the LIFECYCLE verbs live in the
            // handler map — the frames ride the dedicated `oneway` lane in IpcMode.
            // Action names are `companion_overlay_*`, deliberately distinct from
            // OverlayHandler's `*_overlay`: a duplicate key here would silently
            // overwrite the earlier handler in the map built below.
            CompanionOverlayHandler(companionFaceOverlay),
            CapabilityHandler(),
            // The kernel's owner allow/deny push. Without it `updatePolicy` had
            // no caller at all, so an owner override never reached the guard.
            PolicyHandler(packagePolicyGuard)
        )

        // Every handler is wrapped, so the denylist gate applies on ALL THREE
        // transports (IPC / local MCP / remote WS) and to every verb that drives
        // another app — not just the accessibility ones, which is where the
        // check used to live. `GuardedCommandHandler.supportedActions()`
        // delegates, so the map is keyed exactly as before.
        allHandlers.forEach { handler ->
            val guarded = GuardedCommandHandler(handler, packagePolicyGuard)
            handler.supportedActions().forEach { action ->
                handlers[action] = guarded
            }
        }

        return handlers
    }

    @Provides
    @Singleton
    fun provideIpcMode(
        @CommandHandlerMap commandHandlers: Map<String, @JvmSuppressWildcards CommandHandler>,
        toolCallLogger: ToolCallLogger,
        companionFaceOverlay: CompanionFaceOverlay
    ): IpcMode {
        return IpcMode(commandHandlers, toolCallLogger, companionFaceOverlay)
    }

    @Provides
    @Singleton
    fun provideMcpMode(
        @CommandHandlerMap commandHandlers: Map<String, @JvmSuppressWildcards CommandHandler>,
        @ApplicationContext context: Context,
        toolCallLogger: ToolCallLogger
    ): McpMode {
        return McpMode(commandHandlers, context, toolCallLogger)
    }

    @Provides
    @Singleton
    fun provideRemoteWsMode(
        webSocketClient: AsterWebSocketClient,
        @CommandHandlerMap commandHandlers: Map<String, @JvmSuppressWildcards CommandHandler>,
        toolCallLogger: ToolCallLogger
    ): RemoteWsMode {
        return RemoteWsMode(webSocketClient, commandHandlers, toolCallLogger)
    }
}

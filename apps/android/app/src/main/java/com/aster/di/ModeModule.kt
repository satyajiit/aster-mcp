package com.aster.di

import android.content.Context
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.CommandHandler
import com.aster.service.handlers.AccessibilityHandler
import com.aster.service.handlers.AlarmHandler
import com.aster.service.handlers.CameraHandler
import com.aster.service.handlers.ClipboardHandler
import com.aster.service.handlers.ContactHandler
import com.aster.service.handlers.DeviceInfoHandler
import com.aster.service.handlers.FileSystemHandler
import com.aster.service.handlers.IntentHandler
import com.aster.service.handlers.MediaHandler
import com.aster.service.handlers.NotificationHandler
import com.aster.service.handlers.OverlayHandler
import com.aster.service.handlers.PackageHandler
import com.aster.service.handlers.ShellHandler
import com.aster.service.handlers.SmsHandler
import com.aster.service.handlers.StorageHandler
import com.aster.service.handlers.VolumeHandler
import com.aster.service.mode.IpcMode
import com.aster.service.mode.McpMode
import com.aster.service.mode.RemoteWsMode
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
        @ApplicationContext context: Context
    ): Map<String, @JvmSuppressWildcards CommandHandler> {
        val handlers = mutableMapOf<String, CommandHandler>()

        val allHandlers = listOf(
            DeviceInfoHandler(context),
            FileSystemHandler(context),
            PackageHandler(context),
            ClipboardHandler(context),
            MediaHandler(context),
            ShellHandler(),
            IntentHandler(context),
            AccessibilityHandler(),
            NotificationHandler(),
            SmsHandler(context),
            OverlayHandler(context),
            StorageHandler(context),
            VolumeHandler(context),
            ContactHandler(context),
            AlarmHandler(context),
            CameraHandler(context)
        )

        allHandlers.forEach { handler ->
            handler.supportedActions().forEach { action ->
                handlers[action] = handler
            }
        }

        return handlers
    }

    @Provides
    @Singleton
    fun provideIpcMode(
        @CommandHandlerMap commandHandlers: Map<String, @JvmSuppressWildcards CommandHandler>,
        toolCallLogger: ToolCallLogger
    ): IpcMode {
        return IpcMode(commandHandlers, toolCallLogger)
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

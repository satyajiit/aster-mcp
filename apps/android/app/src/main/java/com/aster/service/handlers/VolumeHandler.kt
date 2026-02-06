package com.aster.service.handlers

import android.content.Context
import android.media.AudioManager
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

class VolumeHandler(
    private val context: Context
) : CommandHandler {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun supportedActions() = listOf(
        "get_volume",
        "set_volume"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "get_volume" -> getVolume()
            "set_volume" -> setVolume(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun getVolume(): CommandResult {
        val streams = mapOf(
            "media" to AudioManager.STREAM_MUSIC,
            "ring" to AudioManager.STREAM_RING,
            "notification" to AudioManager.STREAM_NOTIFICATION,
            "alarm" to AudioManager.STREAM_ALARM,
            "call" to AudioManager.STREAM_VOICE_CALL,
            "system" to AudioManager.STREAM_SYSTEM
        )

        val ringerMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "silent"
            AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
            AudioManager.RINGER_MODE_NORMAL -> "normal"
            else -> "unknown"
        }

        val data = buildJsonObject {
            put("ringerMode", ringerMode)
            put("streams", buildJsonObject {
                streams.forEach { (name, streamType) ->
                    put(name, buildJsonObject {
                        put("current", audioManager.getStreamVolume(streamType))
                        put("max", audioManager.getStreamMaxVolume(streamType))
                        put("min", audioManager.getStreamMinVolume(streamType))
                    })
                }
            })
        }

        return CommandResult.success(data)
    }

    private fun setVolume(command: Command): CommandResult {
        val streamName = command.params?.get("stream")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'stream' parameter")

        val streamType = when (streamName) {
            "media" -> AudioManager.STREAM_MUSIC
            "ring" -> AudioManager.STREAM_RING
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "alarm" -> AudioManager.STREAM_ALARM
            "call" -> AudioManager.STREAM_VOICE_CALL
            "system" -> AudioManager.STREAM_SYSTEM
            else -> return CommandResult.failure("Unknown stream: $streamName. Use: media, ring, notification, alarm, call, system")
        }

        val level = command.params?.get("level")?.jsonPrimitive?.intOrNull
        val mute = command.params?.get("mute")?.jsonPrimitive?.booleanOrNull

        if (level == null && mute == null) {
            return CommandResult.failure("Provide 'level' (number) and/or 'mute' (boolean)")
        }

        if (mute != null) {
            audioManager.adjustStreamVolume(
                streamType,
                if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )
        }

        if (level != null) {
            val maxVol = audioManager.getStreamMaxVolume(streamType)
            val minVol = audioManager.getStreamMinVolume(streamType)
            val clampedLevel = level.coerceIn(minVol, maxVol)
            audioManager.setStreamVolume(streamType, clampedLevel, 0)
        }

        val data = buildJsonObject {
            put("stream", streamName)
            put("current", audioManager.getStreamVolume(streamType))
            put("max", audioManager.getStreamMaxVolume(streamType))
            put("min", audioManager.getStreamMinVolume(streamType))
            if (mute != null) put("muted", mute)
        }

        return CommandResult.success(data)
    }
}

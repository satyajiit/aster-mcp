package com.aster.service.handlers

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import java.io.File
import java.util.*
import kotlin.coroutines.resume

class MediaHandler(
    private val context: Context
) : CommandHandler {

    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    override fun supportedActions() = listOf(
        "vibrate",
        "speak_tts",
        "play_audio"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "vibrate" -> vibrate(command)
            "speak_tts" -> speakTts(command)
            "play_audio" -> playAudio(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun vibrate(command: Command): CommandResult {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) {
            return CommandResult.failure("Device does not have a vibrator")
        }

        // Get pattern from params
        val patternJson = command.params?.get("pattern")
        val pattern: LongArray = if (patternJson != null && patternJson is JsonArray) {
            patternJson.map { it.jsonPrimitive.long }.toLongArray()
        } else {
            // Default: single vibration of 500ms
            longArrayOf(0, 500)
        }

        val repeat = command.params?.get("repeat")?.jsonPrimitive?.intOrNull ?: -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, repeat)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, repeat)
        }

        val data = buildJsonObject {
            put("success", true)
            put("pattern", buildJsonArray { pattern.forEach { add(it) } })
        }

        return CommandResult.success(data)
    }

    private suspend fun speakTts(command: Command): CommandResult {
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'text' parameter")

        // Initialize TTS if needed
        if (tts == null) {
            val initResult = initTts()
            if (!initResult) {
                return CommandResult.failure("Failed to initialize TTS")
            }
        }

        if (!ttsInitialized) {
            return CommandResult.failure("TTS not initialized")
        }

        val utteranceId = UUID.randomUUID().toString()

        return suspendCancellableCoroutine { continuation ->
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}

                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        val data = buildJsonObject {
                            put("success", true)
                            put("text", text)
                        }
                        continuation.resume(CommandResult.success(data))
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(CommandResult.failure("TTS error"))
                    }
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(CommandResult.failure("TTS error: $errorCode"))
                    }
                }
            })

            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                if (continuation.isActive) {
                    continuation.resume(CommandResult.failure("Failed to start TTS"))
                }
            }
        }
    }

    private suspend fun initTts(): Boolean = suspendCancellableCoroutine { continuation ->
        tts = TextToSpeech(context) { status ->
            ttsInitialized = status == TextToSpeech.SUCCESS
            if (ttsInitialized) {
                tts?.language = Locale.getDefault()
            }
            if (continuation.isActive) {
                continuation.resume(ttsInitialized)
            }
        }
    }

    private fun playAudio(command: Command): CommandResult {
        val source = command.params?.get("source")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'source' parameter")

        // Stop any currently playing audio
        mediaPlayer?.release()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                when {
                    source.startsWith("http://") || source.startsWith("https://") -> {
                        setDataSource(source)
                    }
                    source.startsWith("data:") || source.length > 1000 -> {
                        // Base64 encoded audio
                        val base64Data = if (source.startsWith("data:")) {
                            source.substringAfter("base64,")
                        } else {
                            source
                        }
                        val audioData = Base64.decode(base64Data, Base64.DEFAULT)
                        val tempFile = File.createTempFile("audio", ".mp3", context.cacheDir)
                        tempFile.writeBytes(audioData)
                        setDataSource(tempFile.absolutePath)
                        tempFile.deleteOnExit()
                    }
                    else -> {
                        // Local file path
                        setDataSource(source)
                    }
                }

                prepare()
                start()
            }

            val data = buildJsonObject {
                put("success", true)
                put("source", source.take(100)) // Truncate for response
                put("duration", mediaPlayer?.duration ?: 0)
            }

            return CommandResult.success(data)
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            return CommandResult.failure("Failed to play audio: ${e.message}")
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

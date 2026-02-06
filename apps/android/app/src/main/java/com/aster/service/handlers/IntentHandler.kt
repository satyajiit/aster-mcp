package com.aster.service.handlers

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.telecom.TelecomManager
import android.widget.Toast
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.*
import kotlin.coroutines.resume

class IntentHandler(
    private val context: Context
) : CommandHandler {

    private var tts: TextToSpeech? = null
    private var ttsInitialized = false

    override fun supportedActions() = listOf(
        "launch_intent",
        "show_toast",
        "make_call",
        "make_call_with_voice"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "launch_intent" -> launchIntent(command)
            "show_toast" -> showToast(command)
            "make_call" -> makeCall(command)
            "make_call_with_voice" -> makeCallWithVoice(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun launchIntent(command: Command): CommandResult {
        val packageName = command.params?.get("package")?.jsonPrimitive?.contentOrNull
        val action = command.params?.get("action")?.jsonPrimitive?.contentOrNull
        val data = command.params?.get("data")?.jsonPrimitive?.contentOrNull

        // If only package name provided, launch the app
        if (packageName != null && action == null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return CommandResult.success(buildJsonObject {
                    put("launched", true)
                    put("package", packageName)
                })
            } else {
                return CommandResult.failure("Cannot launch package: $packageName")
            }
        }

        // Build custom intent
        val intent = Intent().apply {
            action?.let { setAction(it) }
            data?.let { setData(Uri.parse(it)) }
            packageName?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Add extras if provided
            command.params?.get("extras")?.jsonObject?.forEach { (key, value) ->
                when (value) {
                    is JsonPrimitive -> {
                        when {
                            value.isString -> putExtra(key, value.content)
                            value.booleanOrNull != null -> putExtra(key, value.boolean)
                            value.intOrNull != null -> putExtra(key, value.int)
                            value.longOrNull != null -> putExtra(key, value.long)
                            value.floatOrNull != null -> putExtra(key, value.float)
                        }
                    }
                    else -> putExtra(key, value.toString())
                }
            }
        }

        return try {
            context.startActivity(intent)
            CommandResult.success(buildJsonObject {
                put("launched", true)
                put("action", action)
                put("data", data)
                put("package", packageName)
            })
        } catch (e: Exception) {
            CommandResult.failure("Failed to launch intent: ${e.message}")
        }
    }

    private suspend fun showToast(command: Command): CommandResult = withContext(Dispatchers.Main) {
        val message = command.params?.get("message")?.jsonPrimitive?.contentOrNull
            ?: return@withContext CommandResult.failure("Missing 'message' parameter")

        val duration = when (command.params?.get("duration")?.jsonPrimitive?.contentOrNull) {
            "long" -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }

        Toast.makeText(context, message, duration).show()

        CommandResult.success(buildJsonObject {
            put("shown", true)
            put("message", message)
        })
    }

    private fun makeCall(command: Command): CommandResult {
        val number = command.params?.get("number")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'number' parameter")

        // Clean the phone number
        val cleanNumber = number.replace(Regex("[^0-9+*#]"), "")

        if (cleanNumber.isEmpty()) {
            return CommandResult.failure("Invalid phone number")
        }

        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$cleanNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            CommandResult.success(buildJsonObject {
                put("calling", true)
                put("number", cleanNumber)
            })
        } catch (e: SecurityException) {
            CommandResult.failure("CALL_PHONE permission not granted")
        } catch (e: Exception) {
            CommandResult.failure("Failed to make call: ${e.message}")
        }
    }

    /**
     * Makes a phone call, enables speakerphone, waits for the call to connect,
     * then uses TTS to speak the provided text into the call.
     */
    private suspend fun makeCallWithVoice(command: Command): CommandResult {
        val number = command.params?.get("number")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'number' parameter")
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'text' parameter")
        val delaySeconds = command.params?.get("waitSeconds")?.jsonPrimitive?.intOrNull ?: 8

        // Clean the phone number
        val cleanNumber = number.replace(Regex("[^0-9+*#]"), "")
        if (cleanNumber.isEmpty()) {
            return CommandResult.failure("Invalid phone number")
        }

        // Initialize TTS if needed
        if (tts == null) {
            val initResult = initTts()
            if (!initResult) {
                return CommandResult.failure("Failed to initialize TTS")
            }
        }

        // Step 1: Make the call
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$cleanNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(callIntent)
        } catch (e: SecurityException) {
            return CommandResult.failure("CALL_PHONE permission not granted")
        } catch (e: Exception) {
            return CommandResult.failure("Failed to make call: ${e.message}")
        }

        // Step 2: Wait for the call to connect, then enable speaker
        delay(2000) // Wait for dialer to start

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = true
        delay(500) // Allow audio routing to stabilize

        // Step 3: Wait for the call to be answered
        delay(delaySeconds.toLong() * 1000)

        // Step 4: Speak the text using TTS
        val ttsResult = speakText(text)

        // Step 5: Restore audio (speakerphone stays on during the call, user can end manually)
        return if (ttsResult) {
            CommandResult.success(buildJsonObject {
                put("success", true)
                put("number", cleanNumber)
                put("textSpoken", text)
                put("speakerphone", true)
                put("waitSeconds", delaySeconds)
            })
        } else {
            CommandResult.success(buildJsonObject {
                put("callMade", true)
                put("number", cleanNumber)
                put("ttsError", "TTS failed to speak, but call is active on speakerphone")
                put("speakerphone", true)
            })
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

    private suspend fun speakText(text: String): Boolean = suspendCancellableCoroutine { continuation ->
        val utteranceId = UUID.randomUUID().toString()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId && continuation.isActive) {
                    continuation.resume(true)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId && continuation.isActive) {
                    continuation.resume(false)
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId && continuation.isActive) {
                    continuation.resume(false)
                }
            }
        })

        // Use STREAM_VOICE_CALL to route TTS audio through the call audio path
        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            if (continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

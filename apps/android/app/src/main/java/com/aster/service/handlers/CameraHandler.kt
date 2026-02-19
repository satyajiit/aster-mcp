package com.aster.service.handlers

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.aster.BuildConfig
import com.aster.data.model.Command
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

class CameraHandler(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "CameraHandler"
        private const val MAX_VIDEO_DURATION_SEC = 8
        private const val PHOTO_WARMUP_MS = 1000L
        private const val TARGET_WIDTH = 1280
        private const val TARGET_HEIGHT = 720
    }

    private val lifecycleOwner = ServiceLifecycleOwner()
    private var activeRecording: Recording? = null

    override fun supportedActions() = listOf(
        "take_photo",
        "record_video"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "take_photo" -> takePhoto(command)
            "record_video" -> recordVideo(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private suspend fun takePhoto(command: Command): CommandResult {
        val cameraParam = command.params?.get("camera")?.jsonPrimitive?.contentOrNull ?: "back"
        val quality = command.params?.get("quality")?.jsonPrimitive?.intOrNull ?: 75

        val cameraSelector = if (cameraParam == "front") {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        return try {
            val result = withTimeoutOrNull(30000L) {
                capturePhoto(cameraSelector, quality)
            } ?: return CommandResult.failure("Photo capture timed out after 30s")

            result
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Photo capture failed", e)
            CommandResult.failure("Photo capture failed: ${e.message}")
        }
    }

    private suspend fun capturePhoto(
        cameraSelector: CameraSelector,
        quality: Int
    ): CommandResult = suspendCancellableCoroutine { continuation ->
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetResolution(android.util.Size(TARGET_WIDTH, TARGET_HEIGHT))
                    .setJpegQuality(quality)
                    .build()

                lifecycleOwner.start()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)

                // Warmup delay for autofocus/exposure
                mainExecutor.execute {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        imageCapture.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                try {
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    val width = image.width
                                    val height = image.height
                                    image.close()

                                    cameraProvider.unbindAll()
                                    lifecycleOwner.stop()

                                    val mediaDir = getAsterMediaDir("photos")
                                    val file = File(mediaDir, "photo_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { it.write(bytes) }

                                    if (continuation.isActive) {
                                        continuation.resume(CommandResult.success(buildJsonObject {
                                            put("filePath", file.absolutePath)
                                            put("format", "jpeg")
                                            put("width", width)
                                            put("height", height)
                                            put("sizeKB", bytes.size / 1024)
                                        }))
                                    }
                                } catch (e: Exception) {
                                    image.close()
                                    cameraProvider.unbindAll()
                                    lifecycleOwner.stop()
                                    if (continuation.isActive) {
                                        continuation.resume(CommandResult.failure("Failed to save photo: ${e.message}"))
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                cameraProvider.unbindAll()
                                lifecycleOwner.stop()
                                if (continuation.isActive) {
                                    continuation.resume(CommandResult.failure("Capture error: ${exception.message}"))
                                }
                            }
                        })
                    }, PHOTO_WARMUP_MS)
                }
            } catch (e: Exception) {
                lifecycleOwner.stop()
                if (continuation.isActive) {
                    continuation.resume(CommandResult.failure("Camera init failed: ${e.message}"))
                }
            }
        }, mainExecutor)
    }

    private suspend fun recordVideo(command: Command): CommandResult {
        val cameraParam = command.params?.get("camera")?.jsonPrimitive?.contentOrNull ?: "back"
        val maxDuration = (command.params?.get("maxDuration")?.jsonPrimitive?.intOrNull ?: MAX_VIDEO_DURATION_SEC)
            .coerceIn(1, MAX_VIDEO_DURATION_SEC)

        val cameraSelector = if (cameraParam == "front") {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val timeoutMs = (maxDuration + 12) * 1000L

        return try {
            val result = withTimeoutOrNull(timeoutMs) {
                captureVideo(cameraSelector, maxDuration)
            } ?: return CommandResult.failure("Video recording timed out")

            result
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Video recording failed", e)
            CommandResult.failure("Video recording failed: ${e.message}")
        }
    }

    @androidx.annotation.OptIn(androidx.camera.video.ExperimentalPersistentRecording::class)
    private suspend fun captureVideo(
        cameraSelector: CameraSelector,
        maxDurationSec: Int
    ): CommandResult = suspendCancellableCoroutine { continuation ->
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD))
                    .build()

                val videoCapture = VideoCapture.withOutput(recorder)

                lifecycleOwner.start()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, videoCapture)

                val videoDir = getAsterMediaDir("videos")
                val outputFile = File(videoDir, "video_${System.currentTimeMillis()}.mp4")
                val outputOptions = FileOutputOptions.Builder(outputFile).build()

                // Start recording (no audio)
                val recording = recorder.prepareRecording(context, outputOptions)
                    .start(mainExecutor) { event ->
                        when (event) {
                            is VideoRecordEvent.Finalize -> {
                                cameraProvider.unbindAll()
                                lifecycleOwner.stop()
                                activeRecording = null

                                if (event.hasError()) {
                                    outputFile.delete()
                                    if (continuation.isActive) {
                                        continuation.resume(CommandResult.failure("Recording error: ${event.cause?.message}"))
                                    }
                                    return@start
                                }

                                if (continuation.isActive) {
                                    val fileSizeKB = outputFile.length() / 1024

                                    continuation.resume(CommandResult.success(buildJsonObject {
                                        put("filePath", outputFile.absolutePath)
                                        put("format", "mp4")
                                        put("sizeKB", fileSizeKB)
                                        put("durationSeconds", maxDurationSec)
                                    }))
                                }
                            }
                        }
                    }

                activeRecording = recording

                // Auto-stop after maxDuration
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        recording.stop()
                    } catch (_: Exception) {}
                }, maxDurationSec * 1000L)

                continuation.invokeOnCancellation {
                    try {
                        recording.stop()
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                lifecycleOwner.stop()
                if (continuation.isActive) {
                    continuation.resume(CommandResult.failure("Camera init failed: ${e.message}"))
                }
            }
        }, mainExecutor)
    }

    fun release() {
        try {
            activeRecording?.stop()
        } catch (_: Exception) {}
        activeRecording = null
        lifecycleOwner.stop()
    }

    private fun getAsterMediaDir(subDir: String): File {
        val dir = File(context.getExternalFilesDir(null), "aster_media/$subDir")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}

/**
 * A LifecycleOwner for use in Services where CameraX requires a lifecycle.
 */
class ServiceLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    fun start() {
        registry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.CREATED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

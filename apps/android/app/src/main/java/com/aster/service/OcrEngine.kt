package com.aster.service

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.aster.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import kotlin.coroutines.resume

/**
 * One recognized text block, in the PIXEL SPACE of the bitmap passed to [recognize].
 * When the caller passes the downscaled 1280px screenshot, [bounds] is in screenshot pixels;
 * the caller is responsible for mapping back to real screen pixels via the screenshot `scale`.
 */
data class OcrBlock(
    val text: String,
    val bounds: Rect,
    val confidence: Float
)

/**
 * On-device OCR via ML Kit Text Recognition v2 (bundled Latin model — no network).
 *
 * Owns a single [TextRecognizer] (expensive to create; leaks if not closed). The recognizer's
 * [TextRecognizer.process] returns a Task; we bridge it to a coroutine with
 * [suspendCancellableCoroutine] exactly like AsterAccessibilityService.performGesture /
 * takeScreenshot. Fail-closed: any exception (or a null-text result) yields an empty list so the
 * `observe` path degrades to a11y-only rather than crashing.
 */
class OcrEngine : Closeable {

    companion object {
        private const val TAG = "AsterOcr"
    }

    @Volatile
    private var recognizer: TextRecognizer? = null

    private fun obtainRecognizer(): TextRecognizer {
        return recognizer ?: synchronized(this) {
            recognizer ?: TextRecognition
                .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .also { recognizer = it }
        }
    }

    /**
     * Recognize text in [bitmap]. [rotationDegrees] must be 0, 90, 180 or 270 (ML Kit contract).
     * Returns blocks with non-null bounding boxes and non-blank text, in [bitmap] pixel space.
     */
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int): List<OcrBlock> {
        return try {
            val image = InputImage.fromBitmap(bitmap, rotationDegrees)
            suspendCancellableCoroutine { continuation ->
                obtainRecognizer().process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            continuation.resume(toBlocks(visionText.textBlocks))
                        }
                    }
                    .addOnFailureListener { e ->
                        if (BuildConfig.DEBUG) Log.w(TAG, "OCR process failed", e)
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "OCR setup failed", e)
            emptyList()
        }
    }

    private fun toBlocks(
        textBlocks: List<com.google.mlkit.vision.text.Text.TextBlock>
    ): List<OcrBlock> {
        val out = ArrayList<OcrBlock>(textBlocks.size)
        for (block in textBlocks) {
            val box = block.boundingBox ?: continue
            val text = block.text.trim()
            if (text.isEmpty()) continue
            // ML Kit text-recognition does not surface per-block confidence; report 1.0 as the
            // "recognized" sentinel rather than fabricating a score (project policy: no fabrication).
            out.add(OcrBlock(text = text, bounds = Rect(box), confidence = 1.0f))
        }
        return out
    }

    override fun close() {
        synchronized(this) {
            try {
                recognizer?.close()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "OCR recognizer close failed", e)
            }
            recognizer = null
        }
    }
}

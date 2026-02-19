package com.aster.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aster.R
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.local.db.ToolEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutionOverlay @Inject constructor(
    private val toolCallLogger: ToolCallLogger
) {
    companion object {
        private const val TAG = "ToolExecOverlay"
        private const val DISMISS_DELAY_MS = 1500L

        // Colors
        private const val BG_COLOR = 0xE6_10_10_1E.toInt()     // dark semi-transparent
        private const val TEXT_COLOR = 0xFF_EC_F0_F6.toInt()    // light text
        private const val DOT_TEAL = 0xFF_2D_D4_BF.toInt()     // running
        private const val DOT_GREEN = 0xFF_22_C5_5E.toInt()    // success
        private const val DOT_RED = 0xFF_EF_44_44.toInt()      // error
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var pillView: LinearLayout? = null
    private var dotView: View? = null
    private var labelView: TextView? = null
    private var context: Context? = null
    private var collectJob: Job? = null
    private var dismissRunnable: Runnable? = null
    private var isAttachedToWindow = false

    fun attach(context: Context, scope: CoroutineScope) {
        this.context = context
        this.windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        collectJob = toolCallLogger.toolEvents
            .onEach { event -> mainHandler.post { handleEvent(event) } }
            .launchIn(scope)

        Log.d(TAG, "Attached")
    }

    fun detach() {
        collectJob?.cancel()
        collectJob = null
        mainHandler.post { removePill() }
        windowManager = null
        context = null
        Log.d(TAG, "Detached")
    }

    private fun handleEvent(event: ToolEvent) {
        when (event) {
            is ToolEvent.Started -> {
                cancelPendingDismiss()
                showOrUpdate(event.toolName, DOT_TEAL)
            }
            is ToolEvent.Completed -> {
                val dotColor = if (event.success) DOT_GREEN else DOT_RED
                showOrUpdate(event.toolName, dotColor)
                scheduleDismiss()
            }
        }
    }

    private fun showOrUpdate(toolName: String, dotColor: Int) {
        val ctx = context ?: return
        if (!Settings.canDrawOverlays(ctx)) {
            Log.w(TAG, "Overlay permission not granted, skipping")
            return
        }

        if (pillView == null) {
            createPill(ctx)
        }

        setDotColor(dotColor)
        labelView?.text = toolName

        if (!isAttachedToWindow) {
            try {
                windowManager?.addView(pillView, createLayoutParams(ctx))
                isAttachedToWindow = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add overlay view", e)
            }
        }
    }

    private fun createPill(ctx: Context) {
        val density = ctx.resources.displayMetrics.density

        // Dot: 8dp circle
        dotView = View(ctx).apply {
            val size = (8 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (6 * density).toInt()
                gravity = Gravity.CENTER_VERTICAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(DOT_TEAL)
            }
        }

        // Label
        labelView = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(TEXT_COLOR)
            maxLines = 1
            includeFontPadding = false
            try {
                typeface = ResourcesCompat.getFont(ctx, R.font.instrument_sans_medium)
            } catch (_: Exception) { /* fall back to default */ }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        // Pill container
        val hPad = (12 * density).toInt()
        val vPad = (6 * density).toInt()
        pillView = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
            background = GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(BG_COLOR)
            }
            addView(dotView)
            addView(labelView)
        }
    }

    private fun createLayoutParams(ctx: Context): WindowManager.LayoutParams {
        val density = ctx.resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (12 * density).toInt()
            y = (48 * density).toInt()
        }
    }

    private fun setDotColor(color: Int) {
        (dotView?.background as? GradientDrawable)?.setColor(color)
    }

    private fun scheduleDismiss() {
        cancelPendingDismiss()
        dismissRunnable = Runnable { removePill() }
        mainHandler.postDelayed(dismissRunnable!!, DISMISS_DELAY_MS)
    }

    private fun cancelPendingDismiss() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissRunnable = null
    }

    private fun removePill() {
        cancelPendingDismiss()
        if (isAttachedToWindow) {
            try {
                windowManager?.removeView(pillView)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove overlay view", e)
            }
            isAttachedToWindow = false
        }
        pillView = null
        dotView = null
        labelView = null
    }
}

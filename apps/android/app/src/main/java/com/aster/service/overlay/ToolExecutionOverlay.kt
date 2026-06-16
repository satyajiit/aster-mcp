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
import com.aster.service.safety.KillSwitchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolExecutionOverlay @Inject constructor(
    private val toolCallLogger: ToolCallLogger,
    private val killSwitchController: KillSwitchController
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

        /** Screen-control actions that drive the kill-switch + STOP affordance. */
        private val SCREEN_CONTROL_ACTIONS = setOf(
            "tap", "set_text", "long_press", "set_toggle", "perform", "scroll",
            "input_gesture", "press_key", "global_action", "input_text",
            "click_by_text", "click_by_view_id", "launch_intent"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var pillView: LinearLayout? = null
    private var dotView: View? = null
    private var labelView: TextView? = null
    private var stopView: TextView? = null
    private var context: Context? = null
    private var collectJob: Job? = null
    private var dismissRunnable: Runnable? = null
    private var isAttachedToWindow = false
    private var stopAttached = false

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

    /**
     * Screen Control /goal P7 — immediately tear down the active pill + STOP
     * overlay WITHOUT cancelling the event collector (unlike [detach]). Invoked
     * by the kill switch on STOP so the activity overlay clears right away; the
     * collector keeps running so a later tool still surfaces normally.
     */
    fun clearActive() {
        mainHandler.post { removePill() }
    }

    private fun handleEvent(event: ToolEvent) {
        when (event) {
            is ToolEvent.Started -> {
                cancelPendingDismiss()
                val label = event.target?.let { "${friendly(event.toolName)} '$it'" }
                    ?: friendly(event.toolName)
                val screenControl = isScreenControl(event.toolName)
                showOrUpdate(label, DOT_TEAL, showStop = screenControl)
                if (screenControl) {
                    // Persistent STOP notification — the reliable fallback when
                    // draw-overlays is not granted (P7).
                    killSwitchController.showControlActive(event.target)
                }
            }
            is ToolEvent.Completed -> {
                val dotColor = if (event.success) DOT_GREEN else DOT_RED
                val label = event.target?.let { "${friendly(event.toolName)} '$it'" }
                    ?: friendly(event.toolName)
                showOrUpdate(label, dotColor, showStop = false)
                scheduleDismiss()
            }
        }
    }

    private fun isScreenControl(action: String) = action in SCREEN_CONTROL_ACTIONS

    private fun friendly(action: String) = when (action) {
        "tap" -> "Tapping"
        "set_text", "input_text" -> "Typing"
        "long_press" -> "Long-pressing"
        "set_toggle" -> "Toggling"
        "scroll" -> "Scrolling"
        "perform" -> "Acting on"
        "global_action" -> "System action"
        "press_key" -> "Pressing"
        "launch_intent" -> "Opening"
        else -> action
    }

    private fun showOrUpdate(label: String, dotColor: Int, showStop: Boolean) {
        val ctx = context ?: return
        if (!Settings.canDrawOverlays(ctx)) {
            Log.w(TAG, "Overlay permission not granted, skipping")
            return
        }

        if (pillView == null) {
            createPill(ctx)
        }

        setDotColor(dotColor)
        labelView?.text = label

        if (!isAttachedToWindow) {
            try {
                windowManager?.addView(pillView, createLayoutParams(ctx))
                isAttachedToWindow = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add overlay view", e)
            }
        }

        // Touchable STOP button — its OWN overlay view (the pill stays
        // FLAG_NOT_TOUCHABLE; only the STOP view receives taps). P7.
        if (showStop) {
            ensureStopView(ctx)
            if (!stopAttached) {
                try {
                    windowManager?.addView(stopView, stopLayoutParams(ctx))
                    stopAttached = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to add STOP overlay view", e)
                }
            }
        } else {
            removeStopView()
        }
    }

    private fun ensureStopView(ctx: Context) {
        if (stopView != null) return
        val density = ctx.resources.displayMetrics.density
        stopView = TextView(ctx).apply {
            text = "STOP"
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val h = (10 * density).toInt(); val v = (6 * density).toInt()
            setPadding(h, v, h, v)
            background = GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(DOT_RED)
            }
            setOnClickListener { killSwitchController.stop() }
        }
    }

    private fun stopLayoutParams(ctx: Context): WindowManager.LayoutParams {
        val density = ctx.resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Touchable (NO FLAG_NOT_TOUCHABLE) so the STOP button receives taps.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (12 * density).toInt()
            y = (96 * density).toInt()
        }
    }

    private fun removeStopView() {
        if (stopAttached) {
            try {
                windowManager?.removeView(stopView)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove STOP overlay view", e)
            }
            stopAttached = false
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
        removeStopView()
        stopView = null
        pillView = null
        dotView = null
        labelView = null
        // Control session ended — drop the persistent STOP notification (P7).
        killSwitchController.hide()
    }
}
